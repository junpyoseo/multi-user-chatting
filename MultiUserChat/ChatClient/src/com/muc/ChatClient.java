package com.muc;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ChatClient {
    private final String serverName;
    private final int serverPort;
    private Socket socket;
    private OutputStream serverOut;
    private InputStream serverIn;
    private BufferedReader bufferReadIn;

    private ArrayList<UserStatusListener> userStatusListenerArrayList = new ArrayList<>();
    private ArrayList<MessageListener> messageListenerArrayList = new ArrayList<>();
    public ChatClient(String serverName, int serverPort) {
        this.serverName = serverName;
        this.serverPort = serverPort;
    }

    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient("localhost", 9090);
        client.addUserStatusListener(new UserStatusListener() {
            @Override
            public void online(String login) {
                System.out.println("ONLINE: " + login);
            }
            @Override
            public void offline(String login) {
                System.out.println("OFFLINE: " + login);
            }
        });

        client.addMessageListener(new MessageListener() {
            @Override
            public void onMessage(String fromLogin, String message) {
                System.out.println("You got a message from " + fromLogin + " ====> " + message);
            }
        });
        if (!client.connect()){
            System.err.print("Connection failed...");
        }
        else {
            System.out.println("Connection Successful!");
            if (client.login("guest", "guest")){
                System.out.println("Login successful!");

                String message = "Hello World!\n";
                String sendTo = "Peter";
                client.sendMessage(sendTo, message);
            }
            else {
                System.err.println("Login failed");
            }
        }
//        client.logoff();
    }

    public void sendMessage(String sendTo, String message) throws IOException {
        String cmd = "msg " + sendTo + " " + message + "\n";
        serverOut.write(cmd.getBytes());
    }

    public void logoff() throws IOException {
        String cmd = "logoff\n";
        serverOut.write(cmd.getBytes());
    }

    public boolean login(String id, String pwd) throws IOException{
        String cmd = "login " + id + " " + pwd + "\n";
        serverOut.write(cmd.getBytes());

        String response = bufferReadIn.readLine();
        System.out.println("Response Line: " + response);

        if ("Okay, sign in".equalsIgnoreCase(response)){
            startMessageReader();
            return true;
        }
        else {
            return false;
        }
    }

    private void startMessageReader() {
        Thread t = new Thread() {
            public void run () {
                try {
                    readMessageLoop();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }
    private void readMessageLoop() throws IOException {
        try {
            String line;
            while (((line = bufferReadIn.readLine()) != null)){
                String[] tokens = StringUtils.split(line);
                if (tokens != null && tokens.length > 0) {
                    String cmd = tokens[0];
                    if ("Online".equalsIgnoreCase(cmd)) {
                        handleOnline(tokens);
                    }
                    else if ("Offline".equalsIgnoreCase(cmd)){
                        handleOffline(tokens);
                    } else if ("msg".equalsIgnoreCase(cmd)) {
                        String[] tokensMsg = StringUtils.split(line, null, 3);
                        handleMessage(tokensMsg);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            socket.close();
        }
    }

    private void handleMessage(String[] tokens) {
        String login = tokens[1];
        String msg = tokens[2];

        for (MessageListener listener: messageListenerArrayList){
            listener.onMessage(login, msg);
        }
    }

    private void handleOffline(String[] tokens) {
        String login = tokens[1];
        for(UserStatusListener listener: userStatusListenerArrayList){
            listener.offline(login);
        }
    }

    private void handleOnline(String[] tokens) {
        String login = tokens[1];
        for(UserStatusListener listener: userStatusListenerArrayList){
            listener.online(login);
        }
    }

    public boolean connect() {
        try {
            this.socket = new Socket(serverName, serverPort);
            System.out.println("Client port is " + socket.getLocalPort());
            this.serverOut = socket.getOutputStream();
            this.serverIn = socket.getInputStream();
            this.bufferReadIn = new BufferedReader(new InputStreamReader(serverIn));
            return true;
        } catch (IOException e){
            e.printStackTrace();
        }
        return false;
    }

    public void addUserStatusListener(UserStatusListener listener){
        userStatusListenerArrayList.add(listener);
    }
    public void removeStatusListener(UserStatusListener listener){
        userStatusListenerArrayList.remove(listener);
    }

    public void addMessageListener(MessageListener messageListener){
        messageListenerArrayList.add(messageListener);
    }
    public void removeMessageListener(MessageListener messageListener){
        messageListenerArrayList.remove(messageListener);
    }
}
