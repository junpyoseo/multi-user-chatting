package com.muc;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;


public class ServerWorker extends Thread {
    private final Socket clientSocket;
    private final Server server;
    private String login = null;
    private OutputStream outputStream;
    private HashSet<String> topicSet = new HashSet<>();

    public ServerWorker(Server server, Socket clientSocket){

        this.server = server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            handleClientSocket();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void handleClientSocket() throws IOException, InterruptedException {
        InputStream inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while((line = reader.readLine()) != null) {
            String[] tokens = StringUtils.split(line);
            if (tokens != null && tokens.length > 0) {
                String cmd = tokens[0];
                if ("quit".equalsIgnoreCase(cmd) || "logoff".equalsIgnoreCase(cmd)) {
                    handleLogOff();
                    break;
                } else if ("login".equalsIgnoreCase(cmd)){
                    handleLogin(outputStream, tokens);
                }
                else if ("msg".equalsIgnoreCase(cmd)){
                    String[] tokensMsg = StringUtils.split(line, null, 3);
                    handleMessage(tokensMsg);
                }
                else if ("join".equalsIgnoreCase(cmd)){
                    handleJoin(tokens);
                }
                else if ("leave".equalsIgnoreCase(cmd)){
                    handleLeave(tokens);
                }
                else {
                    String msg = "unknown " + cmd + "\n";
                    outputStream.write(msg.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        clientSocket.close();
    }

    private void handleLeave(String[] tokens) {
        if(tokens.length > 1) {
            String topic = tokens[1];
            if (topicSet.contains(topic)) {
                topicSet.remove(topic);
            }
        }
    }

    public boolean isMemberOfTopic(String topic){
        return topicSet.contains(topic);
    }

    private void handleJoin(String[] tokens) {
        if(tokens.length > 1) {
            String topic = tokens[1];
            topicSet.add(topic);
        }
    }

    private void handleMessage(String[] tokens) throws IOException{
        String sendTo = tokens[1];
        String content = tokens[2];
        boolean isTopic = sendTo.charAt(0) == '#';

        for(ServerWorker worker: server.getWorkerList()) {
            if (isTopic) {
                if (worker.isMemberOfTopic(sendTo)) {
                    String outMsg = "msg " + sendTo + " " + getLogin() + " " + content + "\n";
                    worker.send(outMsg);
                }
            } else {
                if (sendTo.equalsIgnoreCase(worker.getLogin())) {
                    String outMsg = "msg " + login + " " + content + "\n";
                    worker.send(outMsg);
                }
            }
        }
    }

    private void handleLogOff() throws IOException{
        server.removeWorkerList(this);
        String onlinStatus = "Offline "  + getLogin() + "\n";
        for(ServerWorker worker: server.getWorkerList()){
            if(!login.equals(worker.getLogin())) {
                worker.send(onlinStatus);
            }
        }
        clientSocket.close();
    }

    public String getLogin(){
        return login;
    }

    private void send(String msg) throws IOException {
        if(login != null) {
            outputStream.write(msg.getBytes());
        }

    }
    private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
        if(tokens.length == 3) {
            String login = tokens[1];
            String password = tokens[2];

            if (login.equals("guest") && password.equals("guest")
            || (login.equals("peter") && password.equals("peter"))) {
                String msg = "Okay, sign in\n";
                outputStream.write(msg.getBytes());
                this.login = login;
                System.out.println("User " + login + " successfully logged in");
                ArrayList <ServerWorker> workArrayList = server.getWorkerList();
                // send current user all other online logins
                for(ServerWorker worker: workArrayList){
                        if (worker.getLogin() != null) {
                            if(!login.equals(worker.getLogin())) {
                                String msg2 = "Online " + worker.getLogin() + "\n";
                                send(msg2);
                            }
                        }
                }
                // send other online users current user's status
                String loginStatus = "Online " + getLogin() + "\n";
                for(ServerWorker worker: workArrayList){
                    if(!login.equals(worker.getLogin())) {
                        worker.send(loginStatus);
                    }
                }
            }
            else {
                String msg = "error in signing in";
                outputStream.write(msg.getBytes());
                System.err.println("Login failed for " + login);
            }
        }

    }
}
