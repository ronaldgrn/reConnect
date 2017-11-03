import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class DisasterRoom implements Runnable {
    private static final int LATENCY = 100;   // sleep time on threads

    private String name;
    private Logger logger;

    private CopyOnWriteArrayList<Socket> sockets;   // for ConcurrentModificationException
    private ConcurrentHashMap<Integer, String> nameMap;

    DisasterRoom(String name) {
        this.name = name;
        logger = new Logger(name + ".log");

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
                            if (line.startsWith("#setname")) {
                                /*
                                 * Allow user to set nickname. Uses hashmap for mapping.
                                 * Usage: #setname Ronald
                                 */
                                String name = line.substring(8).trim();
                                String msg = "User " + identifier.toString() + " has set name to " + name;
                                logger.log(msg);
                                this.messageClients(msg);
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

            try {
                // Prevent server from using 100% CPU when room created
                Thread.sleep(LATENCY);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    void addClient(Socket clientSocket) {
        /* Add client to Room */
        this.sockets.add(clientSocket);
        System.out.println("Client added to " + getName());
        messageClients(clientSocket.getPort() + " joined room " + getName());
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
        /* Search for all instances of socket then close and remove from list */
        try {
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

    String getName(){
        return this.name;
    }

    @Override
    public String toString() {
        return name + '(' + sockets.size() + ')';
    }

}