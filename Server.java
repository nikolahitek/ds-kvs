package com.nikolahitek;

import javafx.util.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {

    public static void main(String[] args) throws IOException {
        ServersThread serversThread = new ServersThread(new ServerSocket(3000));
        serversThread.start();
        ClientsThread clientsThread = new ClientsThread(new ServerSocket(3001));
        clientsThread.start();
    }
}

class ClientsThread extends Thread {

    private ServerSocket serverSocket;

    ClientsThread(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket conn = serverSocket.accept();
                Thread thread = new Thread(new ClientRunnable(conn));
                thread.start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

class ClientRunnable implements Runnable {

    private Socket conn;
    private ObjectOutputStream out;

    ClientRunnable(Socket conn) throws IOException {
        this.conn = conn;
        out = new ObjectOutputStream(conn.getOutputStream());
    }

    @Override
    public void run() {
        try {
            out.writeObject(ServersThread.getKVSServer(null));
            out.flush();
            out.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class ServersThread extends Thread {

    private ServerSocket serverSocket;
    private static HashMap<Pair<String, Integer>, KVSServerRunnable> kvsServers;

    ServersThread(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        kvsServers = new HashMap<>();
    }

    static void addKVSServer(Pair<String, Integer> kvs, KVSServerRunnable runnable) {
        kvsServers.put(kvs, runnable);
        System.out.println("New KVS Server: " + kvs);
    }

    static void removeKVSServer(Pair<String, Integer> kvs) {
        kvsServers.remove(kvs);
        System.out.println("Removed KVS Server: " + kvs);
    }

    static Pair<String, Integer> getKVSServer(Pair<String, Integer> requestingKVS) {
        if (kvsServers.size() < 2) {
            return null;
        }
        Pair<String, Integer> randomKVS;

        while ((randomKVS =
                (Pair<String, Integer>) kvsServers.keySet().toArray()[new Random().nextInt(kvsServers.size())])
                .equals(requestingKVS)) {
        }

        return randomKVS;
    }

    static void shareNewKeyValuePair(KeyValuePair keyValuePair, Pair<String, Integer> from) {
        System.out.println("Received new KVP from: " + from);
        kvsServers.keySet().stream()
                .filter(key -> !key.equals(from))
                .map(kvsServers::get)
                .forEach(r -> {
                    try {
                        r.sendNewKeyValuePair(keyValuePair);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket conn = serverSocket.accept();

                Thread thread = new Thread(new KVSServerRunnable(conn));
                thread.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class KVSServerRunnable implements Runnable {

    private Socket conn;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean working;
    private Pair<String, Integer> kvs;

    KVSServerRunnable(Socket conn) throws IOException {
        this.conn = conn;
        in = new ObjectInputStream(conn.getInputStream());
        out = new ObjectOutputStream(conn.getOutputStream());
        working = true;
    }

    void sendNewKeyValuePair(KeyValuePair keyValuePair) throws IOException {
        out.writeObject("NEW KVP");
        out.flush();
        out.writeObject(keyValuePair);
        out.flush();
    }

    @Override
    public void run() {
        while (working) {
            try {
                String req = (String) in.readObject();
                System.out.println(req);

                switch (req) {
                    case "REGISTER":
                        kvs = (Pair<String, Integer>) in.readObject();
                        ServersThread.addKVSServer(kvs, this);
                        break;
                    case "END":
                        working = false;
                        ServersThread.removeKVSServer(kvs);
                        break;
                    case "REQ KVS":
                        out.writeObject(ServersThread.getKVSServer(kvs));
                        out.flush();
                        break;
                    case "NEW KVP":
                        KeyValuePair keyValuePair = (KeyValuePair) in.readObject();
                        ServersThread.shareNewKeyValuePair(keyValuePair, kvs);
                        break;
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }
}