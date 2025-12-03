package red;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class direccionRed {
    private InetAddress ip;
    private int port;

    public direccionRed (InetAddress ip ,  int port){

        this.ip = ip;
        this.port = port;

    }

    public InetAddress getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
