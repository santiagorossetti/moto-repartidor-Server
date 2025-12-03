package red;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;


public class hiloServidor extends Thread{

    private DatagramSocket conexion;
    private boolean fin = false;
    private ArrayList<direccionRed> clientes = new ArrayList<direccionRed>();
    private int cantidadClientes = 0;
    private final int MAX_CLIENTES = 2;

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
        System.out.println(msg);
        if (msg.equals("Conexion")) {

            if (cantidadClientes < MAX_CLIENTES) {

                cantidadClientes++;
                direccionRed usuario = new direccionRed(dp.getAddress(), dp.getPort());
                clientes.add(usuario);
                enviarMensaje("OK", dp.getAddress(), dp.getPort());

                if (cantidadClientes == MAX_CLIENTES) {
                    for (direccionRed cliente : clientes){
                        enviarMensaje("Comienza" ,  cliente.getIp() , cliente.getPort());
                    }
                }
            }


        }
    }


}
