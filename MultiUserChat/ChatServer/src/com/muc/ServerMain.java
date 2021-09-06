package com.muc;



public class ServerMain {

    private static int PORT = 9090;
    public static void main(String[] args) {
        Server server = new Server(PORT);
        server.start();
    }



}
