package red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;


public class hiloServidor extends Thread{

    private DatagramSocket conexion;
    private boolean fin = false;
    public ArrayList<direccionRed> clientes = new ArrayList<direccionRed>();
    private int cantidadClientes = 0;
    private final int MAX_CLIENTES = 2;
    private gameController gameController;


    public hiloServidor(){
        try {
            conexion = new DatagramSocket(6767);
        } catch (SocketException e){
            e.printStackTrace();
        }

    }

    public void enviarMensaje(String msg , InetAddress ip , int port) {
        byte[] data = msg.getBytes();
        DatagramPacket dp = new DatagramPacket(data , data.length, ip , port);
        try {
            conexion.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        do {
            byte[] data = new byte[1024];
            DatagramPacket dp  = new DatagramPacket(data , data.length);
            try {
                conexion.receive(dp);
            } catch (IOException e) {
                e.printStackTrace();
            }
            procesarMensaje(dp);
        }while (!fin);
    }

    public void terminarServidor(){
        this.fin = true;
        conexion.close();
        this.interrupt();
    }

    private void procesarMensaje(DatagramPacket dp) {
        String msg = (new String(dp.getData())).trim();
        //System.out.println(msg);

        String letras [] = msg.split(":");


        if (msg.equals("Conexion")) {


                if (cantidadClientes < MAX_CLIENTES) {

                    cantidadClientes++;
                    direccionRed usuario = new direccionRed(dp.getAddress(), dp.getPort());
                    clientes.add(usuario);
                    enviarMensaje("OK", dp.getAddress(), dp.getPort());

                    int id = clientes.size() - 1;
                    enviarMensaje("ID:" + id, dp.getAddress(), dp.getPort());

                    if (cantidadClientes == MAX_CLIENTES) {
                        enviarGlobal("Comienza");
                    }
            }



        }
        switch (letras[0]){
            case "Input":
                int idJugador = Integer.parseInt(letras[1]);
                int keycode = Integer.parseInt(letras[2]);

                Gdx.app.postRunnable(() -> {
                    if (gameController != null)
                        gameController.interactuar(idJugador, keycode);
                });
                break;


        }

    }

    public void setGameController(gameController gameController) {
        this.gameController = gameController;
    }

    public void enviarPosicion(Vector2 posJugador1 , Vector2 posJugador2 , float angulo1 , float angulo2) {

        String pos1 = posJugador1.toString();
        String pos2 = posJugador2.toString();



        enviarGlobal("Movimiento:" + pos1 + ":" + pos2 + ":" + angulo1 + ":" + angulo2 );

    }

    public void enviarFinDelivery(int idJugador) {
        if (idJugador < 0 || idJugador >= clientes.size()) return;
        direccionRed cliente = clientes.get(idJugador);
        enviarMensaje("DeliveryFin:" + idJugador, cliente.getIp(), cliente.getPort());
    }

    public void enviarGas(float gas , int id , InetAddress ip , int port ){

        enviarMensaje("Gas:" + gas + ":" + id , ip , port );

    }

    public void enviarDinero (int dinero , int id , InetAddress ip , int port){

        enviarMensaje("Dinero:" + dinero + ":" + id , ip , port);

    }

    public void enviarVida (int vida , int id , InetAddress ip , int port){

        enviarMensaje("Vida:" + vida + ":" + id , ip , port);

    }



    public void enviarHint(int idJugador, int hintTipo) {
        if (idJugador < 0 || idJugador >= clientes.size()) return;

        direccionRed cliente = clientes.get(idJugador);
        InetAddress ip = cliente.getIp();
        int port = cliente.getPort();

        // Formato: Hint:id:tipo
        // ej: Hint:0:1  -> jugador 0, "Aceptar pedido"
        enviarMensaje("Hint:" + idJugador + ":" + hintTipo, ip, port);
    }


    public void enviarUbicacionDelivery (Rectangle target, boolean dangerous, int reward, int idJugador ){

        if (target == null) return;
        if (idJugador < 0 || idJugador >= clientes.size()) return;
        direccionRed cliente = clientes.get(idJugador);
        InetAddress ip = cliente.getIp();
        int port = cliente.getPort();

        String rect = target.x + "," + target.y + "," + target.width + "," + target.height;

        String msg = "Delivery:" + rect + ":" + (dangerous ? "1" : "0") + ":" + reward + ":" + idJugador;

        enviarMensaje(msg, ip, port);

    }

    public void enviarGlobal (String msg) {
        for (direccionRed cliente : clientes){
            enviarMensaje(msg  ,  cliente.getIp() , cliente.getPort());
        }
    }

}
