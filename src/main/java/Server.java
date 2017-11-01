import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    // A RELIABLE Emergency communication platform
    private static int PORT = 9876;
    private static ArrayList<String> bannedIps;

    public static void main(String args[]) {
        bannedIps = new ArrayList<>();

        DisasterRoom maria = new DisasterRoom();

        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

        try {
            ServerSocket welcomeSocket = new ServerSocket(PORT);

            // create DisasterRooms
            (new Thread(maria)).start();

            /* Runnable which allocates connected clients to Disaster rooms */
            Runnable channelAllocator = new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            // Special Feature: create a new thread for each connecting client
                            Socket socket = welcomeSocket.accept();
                            if(bannedIps.contains(socket.getInetAddress().getHostAddress() ) ){
                                // User banned, close socket
                                System.out.println("Banned User detected: " + socket.getInetAddress().getHostAddress());
                                socket.close();
                                continue;
                            }

                            maria.addClient(socket);
                            System.out.println("Adding new client to Maria");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            (new Thread(channelAllocator)).start();

            // Parse data entered on the server
            String adminInput;
            while (true) {
                while ((adminInput = stdIn.readLine()) != null) {
                    if (adminInput.equals("#exit")){
                        // close program
                        return;
                    }else if(adminInput.startsWith("#banuser")){
                        /*
                         * Kicks a specific user from all groups
                         * Usage: #banuser 56789
                         */
                        String targetUser = adminInput.substring(8).trim();
                        if(maria.kickClient(Integer.parseInt(targetUser))){
                            maria.messageClients("User Kicked");
                        }else{
                            System.out.println("Kick Failed");
                        }
                        return;
                    }else if(adminInput.startsWith("#banip")){
                        /*
                         * Ban a specific IP
                         * Usage: #banip 127.0.0.1
                         */
                        String targetIp = adminInput.substring(6).trim();
                        bannedIps.add(targetIp);    // ban ip from connecting
                        maria.banIP(targetIp);      // close current connections
                        System.out.println("IP successfully banned");
                        return;
                    }

                    System.out.println(">> " + adminInput);
                    maria.messageClients(adminInput, "ADMIN");
                    // System.out.println("echo: " + in.readLine());
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


class DisasterRoom implements Runnable {
    private CopyOnWriteArrayList<Socket> sockets;   // for ConcurrentModificationException
    private ConcurrentHashMap<Integer, String> nameMap;

    DisasterRoom() {
        this.sockets = new CopyOnWriteArrayList<>();
        this.nameMap = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        /* main task of class, iterates over Sockets and checks for new messages */
        while (true) {
            try {
                for (Socket socket : sockets) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String line;

                    while (in.ready()) {
                        line = in.readLine();
                        Integer identifier = socket.getPort();

                        // Detect and handle special commands #setname etc
                        if(line.charAt(0) == '#'){
                            if(line.substring(0, 8).equals("#setname")){
                                String name = line.substring(8).trim();
                                this.messageClients("User " + identifier.toString() + " has set name to " + name);
                                this.nameMap.put(identifier, name);
                                continue;
                            }
                        }

                        this.messageClients(line, identifier);
                    }
                }
            } catch (SocketException se) {
                System.out.println("Warning: All active sockets closed");
                se.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void addClient(Socket clientSocket) {
        this.sockets.add(clientSocket);
    }

    boolean kickClient(Integer identifier){
        // TODO: remove from nameMap if exists
        for (Socket socket: sockets) {
            if(socket.getPort() == identifier){
                sockets.remove(socket);
                return true;
            }
        }
        return false;
    }

    void banIP(String ip){
        try {
            // Search for all instances of socket then close and remove from list
            for (Socket socket : sockets) {
                if (socket.getInetAddress().getHostAddress().equals(ip)) {
                    sockets.remove(socket);
                    socket.close();
                }
//                System.out.println(socket.getInetAddress());
//                System.out.println(socket.getLocalAddress());
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    
    void messageClients(String msg) {
        /* Send message to all clients in channel */
        try {
            for (Socket socket : sockets) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void messageClients(String msg, String tag) {
        this.messageClients(tag + ": " + msg);
    }

    void messageClients(String msg, Integer port) {
        String tag = this.nameMap.getOrDefault(port, String.valueOf(port));
        this.messageClients(msg, tag);
    }
}