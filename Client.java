package com.nikolahitek;

import javafx.util.Pair;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client extends Thread {

    @Override
    public void run() {
        try {
            Socket mainServer = new Socket("localhost", 3001);
            ObjectInputStream mainIn = new ObjectInputStream(mainServer.getInputStream());
            Pair<String, Integer> kvs = (Pair<String, Integer>) mainIn.readObject();
            mainIn.close();
            mainServer.close();
            System.out.println("Will communicate with KVS Server: " + kvs);

            Socket kvsServer = new Socket(kvs.getKey(), kvs.getValue());
            ObjectOutputStream out = new ObjectOutputStream(kvsServer.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(kvsServer.getInputStream());

            out.writeObject("GET KVP");
            out.flush();
            out.writeObject("key1");
            out.flush();
            KeyValuePair kvp1 = (KeyValuePair) in.readObject();
            System.out.println(kvp1);

            out.writeObject("GET KVP");
            out.flush();
            out.writeObject("key2");
            out.flush();
            KeyValuePair kvp2 = (KeyValuePair) in.readObject();
            System.out.println(kvp2);

            out.writeObject("ADD KVP");
            out.flush();
            out.writeObject(new KeyValuePair("key2", "test"));
            out.flush();

            out.writeObject("END");
            out.flush();
            out.close();
            in.close();
            kvsServer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }
}
