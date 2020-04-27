package com.example.demo.backLogic;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.*;

public class NameServer implements Runnable{
    Map<Integer, Integer> dataBase = new HashMap();
    Map<Integer, String> nodes = new HashMap<>();
    Integer highest = 0;
    String eigenIP = "192.168.1.1";
    public NameServer() throws IOException {
        readNodeMap();
        readDatabase();
        sendUDPMessage("shutdown","230.0.0.0",4321);
    }
    private int hashfunction(String name, boolean node) {
        int hash=0;
        int temp = 0;
        int i;
        for (i = 0; i<name.length();i++) {
            hash = 3 * hash + name.charAt(i);
            temp = temp+ name.charAt(i);
        }
        hash = hash/(temp/7);

        if (node) {
                hash = (hash) / (5);
        }
        else
            hash = hash/53;
        return hash;
    }
    private void addNodeToMap(String name, String ip) throws IOException {
        BufferedWriter writer = new BufferedWriter(
                new FileWriter("src\\main\\java\\com\\example\\NodeMap.txt", true)  //Set true for append mode
        );
        writer.newLine();   //Add new line
        writer.write(name);
        writer.newLine();
        writer.write(ip);
        writer.close();
        readNodeMap();
        readDatabase();
    }
    private int requestFile(String filename){
        Integer hash = hashfunction(filename, false);
        if(dataBase.get(hash)!=null)
            return dataBase.get(hash);
        else
            return -1;
    }
    private void removeNodeFromMap(Integer node) throws IOException {
        nodes.clear();
        File file = new File("src\\main\\java\\com\\example\\NodeMap.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        nodes.clear();
        ArrayList<String> nameToAdd = new ArrayList<>();
        ArrayList<String> ipToAdd = new ArrayList<>();
        while ((st = br.readLine()) != null){
            String ip = br.readLine();
            int hash = hashfunction(st,true);
            if (hash!= node) {
                nodes.put(hash, ip);
                nameToAdd.add(st);
                ipToAdd.add(ip);
            }else
                System.out.println("removed "+st);
        }
        int i = 0;
        BufferedWriter writer = new BufferedWriter(
                new FileWriter("src\\main\\java\\com\\example\\NodeMap.txt", false)  //Set true for append mode
        );
        while (i<nameToAdd.size()){
            if (i>=1)
                writer.newLine();
            writer.write(nameToAdd.get(i));
            writer.newLine();
            writer.write(ipToAdd.get(i));
            i++;
        }
        writer.close();
        highest = 0;
        readNodeMap();
        readDatabase();
    }
    private void readDatabase() throws IOException {
        File file2 = new File("src\\main\\java\\com\\example\\Database2.txt");
        BufferedReader br2 = new BufferedReader(new FileReader(file2));
        String st2;
        dataBase.clear();
        while ((st2 = br2.readLine()) != null){
            Integer tempfile = hashfunction(st2,false);
            Integer temp = tempfile-1;
            while (nodes.get(temp)==null && temp != 0){
                temp--;
            }
            if (temp == 0)
                dataBase.put(tempfile,highest);
            dataBase.put(tempfile,highest);
        }
        System.out.println(dataBase.toString());
    }
    private void readNodeMap() throws IOException {
        File file = new File("src\\main\\java\\com\\example\\NodeMap.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        nodes.clear();
        while ((st = br.readLine()) != null){
            String ip = br.readLine();
            int hash = hashfunction(st, true);
            System.out.println("node "+st+" heeft hashwaarde "+ hash);
            nodes.put(hash, ip);
            if (hash>highest)
                highest = hash;
        }
    }
    /*
    public Main() throws IOException {
        sendUDPMessage("This is a multicast messge", "230.0.0.0",
                4321);
        sendUDPMessage("OK", "230.0.0.0",
                4321);
    }

     */
    public static void main(String[] args) throws IOException {
        Thread t = new Thread(new NameServer());
        t.start();
    }
    public static void sendUDPMessage(String message,
                                      String ipAddress, int port) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        InetAddress group = InetAddress.getByName(ipAddress);
        byte[] msg = message.getBytes();
        DatagramPacket packet = new DatagramPacket(msg, msg.length,
                group, port);
        socket.send(packet);
        socket.close();
    }
    public void receiveUDPMessage(String ip, int port) throws
            IOException {
        byte[] buffer = new byte[1024];
        MulticastSocket socket = new MulticastSocket(4321);
        InetAddress group = InetAddress.getByName("230.0.0.0");
        socket.joinGroup(group);
        while (true) {
            System.out.println("Waiting for multicast message...");
            DatagramPacket packet = new DatagramPacket(buffer,
                    buffer.length);
            socket.receive(packet);
            String msg = new String(packet.getData(),
                    packet.getOffset(), packet.getLength());
            getNameAndIp(msg);
            if ("OK".equals(msg)) {
                System.out.println("No more message. Exiting : " + msg);
                break;
            }
        }
        socket.leaveGroup(group);
        socket.close();
    }

    // DEES INLEZE EN HASHE
    private ArrayList<String> getNameAndIp(String msg) throws IOException {
        ArrayList<String> temp = new ArrayList<>();
        if (msg.contains("newNode")) {
            String haha = msg.replace("newNode ","");
            if (!haha.isEmpty()) {
                String[] tokens = haha.split("::");
                for (String t : tokens)
                    temp.add(t);
            }
            addNodeToMap(temp.get(0),temp.get(1));
            System.out.println(temp.toString());
            System.out.println("Node added");
            sendUDPMessage("nodeCount "+Integer.toString(nodes.size()),temp.get(1),5000);
        }
        if (msg.contains("remNode")) {
            String haha = msg.replace("remNode ","");
            if (!haha.isEmpty()) {
                String[] tokens = haha.split("::");
                for (String t : tokens)
                    temp.add(t);
            }
            // Hier wordt terug gegeve de hoeveelste node hij was dus ist mogenlijk de hash te fixe
            removeNodeFromMap(hashfunction(temp.get(0),true));
            System.out.println(temp.toString());
            System.out.println("Node removed");
        }
        return temp;
    }
    @Override
    public void run() {
        try {
            receiveUDPMessage("230.0.0.0", 4321);
            receiveUDPMessage(eigenIP, 5000);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
