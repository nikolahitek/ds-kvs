package com.nikolahitek;

import javafx.util.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


public class KVSServer {
    static ServerSocket serverSocket;
    static List<KeyValuePair> keyValuePairList;

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        serverSocket = new ServerSocket(scanner.nextInt());

        keyValuePairList = new LinkedList<>();
        keyValuePairList.add(new KeyValuePair("key1", "good"));

        ServerThread serverThread = new ServerThread();
        MainThread mainThread = new MainThread();
        serverThread.start();
        mainThread.start();
    }
}

class MainThread extends Thread {

    static void requestKeyValuePairs(Pair<String, Integer> friend) throws IOException, ClassNotFoundException {
        if (friend == null) {
            return;
        }

        Socket conn = new Socket(friend.getKey(), friend.getValue());
        ObjectOutputStream out = new ObjectOutputStream(conn.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(conn.getInputStream());

        out.writeObject("REQ KVL");
        out.flush();
        int n = (Integer) in.readObject();
        for (int i=0; i<n; i++) {
            KeyValuePair keyValuePair = (KeyValuePair) in.readObject();
            KVSServer.keyValuePairList.add(keyValuePair);
        }
        System.out.println("Total KVPs: " + KVSServer.keyValuePairList.size());

        out.writeObject("END");
        out.flush();
        out.close();
        in.close();
        conn.close();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket conn = KVSServer.serverSocket.accept();
                Thread thread = new Thread(new ServeRunnable(conn));
                thread.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

class ServeRunnable implements Runnable {

    private Socket conn;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private boolean working;

    ServeRunnable(Socket conn) throws IOException {
        this.conn = conn;
        in = new ObjectInputStream(conn.getInputStream());
        out = new ObjectOutputStream(conn.getOutputStream());
        working = true;
    }

    @Override
    public void run() {
        while (working) {
            try {
                String req = (String) in.readObject();
                System.out.println(req);

                switch (req) {
                    case "REQ KVL":
                        out.writeObject(KVSServer.keyValuePairList.size());
                        out.flush();
                        for (KeyValuePair keyValuePair : KVSServer.keyValuePairList) {
                            out.writeObject(keyValuePair);
                            out.flush();
                        }
                        System.out.println("Total KVPs: " + KVSServer.keyValuePairList.size());
                        break;

                    case "END":
                        working = false;
                        out.close();
                        in.close();
                        conn.close();
                        break;

                    case "GET KVP":
                        Object key = in.readObject();
                        KeyValuePair keyValuePair = KVSServer.keyValuePairList
                                .stream()
                                .filter(o -> o.key.equals(key))
                                .findFirst()
                                .orElse(null);
                        out.writeObject(keyValuePair);
                        out.flush();

                        break;

                    case "ADD KVP":
                        KeyValuePair kv = (KeyValuePair) in.readObject();
                        KVSServer.keyValuePairList.add(kv);
                        ServerThread.sendNewKeyValuePair(kv);
                        break;
                }

            } catch (Exception e) {
                e.printStackTrace();
                working = false;
            }
        }
    }
}

class ServerThread extends Thread {

    private static ObjectOutputStream out;
    private static ObjectInputStream in;

    ServerThread() throws IOException {

        Socket serverConn = new Socket("localhost", 3000);
        out = new ObjectOutputStream(serverConn.getOutputStream());
        in = new ObjectInputStream(serverConn.getInputStream());
    }

    static void sendNewKeyValuePair(KeyValuePair keyValuePair) throws IOException {
        out.writeObject("NEW KVP");
        out.flush();
        out.writeObject(keyValuePair);
        out.flush();
    }

    @Override
    public void run() {
        try {
            out.writeObject("REGISTER");
            out.writeObject(new Pair<>(KVSServer.serverSocket.getInetAddress().getHostName(), KVSServer.serverSocket.getLocalPort()));
            out.flush();

            out.writeObject("REQ KVS");
            Pair<String, Integer> friend = (Pair<String, Integer>) in.readObject();
            System.out.println("Got friend: " + friend);
            MainThread.requestKeyValuePairs(friend);

            while (true) {
                String req = (String) in.readObject();
                System.out.println(req);

                switch (req) {
                    case "NEW KVP":
                        KeyValuePair keyValuePair = (KeyValuePair) in.readObject();
                        KVSServer.keyValuePairList.add(keyValuePair);
                        System.out.println("Received new KVP from server: " + keyValuePair);
                        break;
                }
            }
            //serverOut.writeObject("END");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
