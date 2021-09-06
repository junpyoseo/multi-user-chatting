package com.muc;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

public class Server extends Thread {
    private final int serverPort;

    private ArrayList<ServerWorker> workerList = new ArrayList<>();
    public Server(int serverPort){
        this.serverPort = serverPort;
    }
    public ArrayList<ServerWorker> getWorkerList() {
        return workerList;
    }

    @Override
    public void run() {

        try {
            var serverSocket = new ServerSocket(serverPort);
            while(true){
                System.out.println("About to accept client conection....");
                var clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from: " + clientSocket);
                ServerWorker worker = new ServerWorker(this, clientSocket);
                workerList.add(worker);
                worker.start();
            }
        }
        catch(IOException e)  {
            e.printStackTrace();
        }
    }

    public void removeWorkerList(ServerWorker serverWorker) {
        workerList.remove(serverWorker);
    }
}
