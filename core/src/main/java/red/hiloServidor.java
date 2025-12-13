package red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.io.IOException;
import java.net.*;

public class hiloServidor extends Thread {

    private DatagramSocket conexion;
    private boolean fin = false;

    private static final int PORT = 6767;
    private static final int MAX_CLIENTES = 2;

    // ✅ IDs estables: 0 y 1 siempre
    public final direccionRed[] clientes = new direccionRed[MAX_CLIENTES];
    private int cantidadClientes = 0;

    private gameController gameController;

    public hiloServidor() {
        try {
            conexion = new DatagramSocket(PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void setGameController(gameController gameController) {
        this.gameController = gameController;
    }

    public void enviarMensaje(String msg, InetAddress ip, int port) {
        byte[] data = msg.getBytes();
        DatagramPacket dp = new DatagramPacket(data, data.length, ip, port);
        try {
            conexion.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (!fin) {
            byte[] data = new byte[1024];
            DatagramPacket dp = new DatagramPacket(data, data.length);
            try {
                conexion.receive(dp);
                procesarMensaje(dp);
            } catch (IOException e) {
                if (!fin) e.printStackTrace();
            }
        }
    }

    public void terminarServidor() {
        fin = true;
        try {
            if (conexion != null && !conexion.isClosed()) conexion.close();
        } catch (Exception ignored) {}
        interrupt();
    }

    // =========================================================
    // ✅ Conexiones (slots estables)
    // =========================================================

    private int findSlotLibre() {
        for (int i = 0; i < MAX_CLIENTES; i++) {
            if (clientes[i] == null) return i;
        }
        return -1;
    }

    private int findSlotPorDireccion(InetAddress ip, int port) {
        for (int i = 0; i < MAX_CLIENTES; i++) {
            direccionRed c = clientes[i];
            if (c != null && c.getIp().equals(ip) && c.getPort() == port) return i;
        }
        return -1;
    }

    private void conectarCliente(DatagramPacket dp) {
        InetAddress ip = dp.getAddress();
        int port = dp.getPort();

        // ✅ si ya estaba conectado (mismo ip/puerto), re-envío ID (idempotente)
        int existente = findSlotPorDireccion(ip, port);
        if (existente != -1) {
            enviarMensaje("OK", ip, port);
            enviarMensaje("ID:" + existente, ip, port);
            if (cantidadClientes == MAX_CLIENTES) enviarGlobal("Comienza");
            return;
        }

        int slot = findSlotLibre();
        if (slot == -1) {
            enviarMensaje("FULL", ip, port);
            return;
        }

        clientes[slot] = new direccionRed(ip, port);
        cantidadClientes++;

        enviarMensaje("OK", ip, port);
        enviarMensaje("ID:" + slot, ip, port);

        if (cantidadClientes == MAX_CLIENTES) {
            enviarGlobal("Comienza");
        }
    }

    private void desconectarCliente(int id, DatagramPacket dp) {
        if (id < 0 || id >= MAX_CLIENTES) return;

        direccionRed c = clientes[id];
        if (c == null) return;

        // seguridad: solo el mismo ip/port puede desconectar su slot
        if (!c.getIp().equals(dp.getAddress()) || c.getPort() != dp.getPort()) return;

        clientes[id] = null;
        cantidadClientes = Math.max(0, cantidadClientes - 1);

        // opcional: avisar al otro
        // enviarGlobal("JugadorSalio:" + id);
    }

    // =========================================================
    // ✅ Mensajes entrantes
    // =========================================================

    private void procesarMensaje(DatagramPacket dp) {
        String msg = (new String(dp.getData())).trim();
        if (msg.length() == 0) return;

        String[] letras = msg.split(":");

        if (msg.equals("Conexion")) {
            conectarCliente(dp);
            return;
        }

        if ("Disconnect".equals(letras[0])) {
            if (letras.length >= 2) {
                int id = Integer.parseInt(letras[1]);
                desconectarCliente(id, dp);
            }
            return;
        }

        if ("Input".equals(letras[0])) {
            // Formato: Input:id:tecla
            if (letras.length >= 3) {
                final int id = Integer.parseInt(letras[1]);
                final int keycode = Integer.parseInt(letras[2]);

                Gdx.app.postRunnable(new Runnable() {
                    @Override public void run() {
                        if (gameController != null) {
                            gameController.interactuar(id, keycode);
                        }
                    }
                });
            }
        }
    }

    // =========================================================
    // ✅ Envíos a clientes
    // =========================================================

    public void enviarGlobal(String msg) {
        for (int i = 0; i < MAX_CLIENTES; i++) {
            direccionRed c = clientes[i];
            if (c != null) {
                enviarMensaje(msg, c.getIp(), c.getPort());
            }
        }
    }

    private direccionRed safeGetCliente(int id) {
        if (id < 0 || id >= MAX_CLIENTES) return null;
        return clientes[id];
    }

    public void enviarPosicion(Vector2 posJugador1, Vector2 posJugador2, float angulo1, float angulo2) {
        String pos1 = posJugador1.toString();
        String pos2 = posJugador2.toString();
        enviarGlobal("Movimiento:" + pos1 + ":" + pos2 + ":" + angulo1 + ":" + angulo2);
    }

    public void enviarGameOver(int winnerIndex) {
        enviarGlobal("GameOver:" + winnerIndex);
    }

    public void enviarFinDelivery(int idJugador) {
        direccionRed c = safeGetCliente(idJugador);
        if (c == null) return;
        enviarMensaje("DeliveryFin:" + idJugador, c.getIp(), c.getPort());
    }

    public void enviarGas(float gas, int idJugador, InetAddress ip, int port) {
        enviarMensaje("Gas:" + gas + ":" + idJugador, ip, port);
    }

    public void enviarDinero(int dinero, int idJugador, InetAddress ip, int port) {
        enviarMensaje("Dinero:" + dinero + ":" + idJugador, ip, port);
    }

    public void enviarVida(int vida, int idJugador, InetAddress ip, int port) {
        enviarMensaje("Vida:" + vida + ":" + idJugador, ip, port);
    }

    public void enviarHint(int idJugador, int hintTipo) {
        direccionRed c = safeGetCliente(idJugador);
        if (c == null) return;
        enviarMensaje("Hint:" + idJugador + ":" + hintTipo, c.getIp(), c.getPort());
    }

    public void enviarUbicacionDelivery(Rectangle target, boolean dangerous, int reward, int idJugador) {
        if (target == null) return;

        direccionRed c = safeGetCliente(idJugador);
        if (c == null) return;

        String rect = target.x + "," + target.y + "," + target.width + "," + target.height;
        String msg = "Delivery:" + rect + ":" + (dangerous ? "1" : "0") + ":" + reward + ":" + idJugador;
        enviarMensaje(msg, c.getIp(), c.getPort());
    }
}
