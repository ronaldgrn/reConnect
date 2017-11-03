import java.io.*;
import java.net.*;
import java.util.ArrayList;

/*
 * A RELIABLE Emergency communication platform
  */
public class Server {
    private static Logger logger;
    private static final int PORT = 9876;
    private static ArrayList<DisasterRoom> rooms;
    private static ArrayList<String> bannedIps;


    public static void main(String args[]) {
        logger = new Logger("reConnect.log");

        rooms = new ArrayList<>();
        bannedIps = new ArrayList<>();

        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

        try {
            ServerSocket welcomeSocket = new ServerSocket(PORT);

            /* Create default DisasterRoom. Useful for testing */
            //DisasterRoom defaultRoom = new DisasterRoom("Default");
            //rooms.add(defaultRoom);
            //new Thread(defaultRoom).start();

            Runnable detectConnections = new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            // .accept is blocking so we do not need to throttle thread
                            Socket socket = welcomeSocket.accept();

                            if(bannedIps.contains(socket.getInetAddress().getHostAddress() ) ){
                                // User banned, close socket
                                System.out.println("Banned User detected: " + socket.getInetAddress().getHostAddress());
                                socket.close();
                                continue;
                            }

                            new Thread(new ChannelAllocator(socket, rooms)).start();
                        }
                    } catch (IOException e) {
                        logger.log("IO Exception during manage runnable" );
                        e.printStackTrace();
                    }
                }
            };
            Thread detectConnectionsThread = new Thread(detectConnections);
            detectConnectionsThread.start();

            /* Parse data entered on the server */
            String adminInput;
            while (true) {
                while ((adminInput = stdIn.readLine()) != null) {
                    if (adminInput.equals("#exit")){
                        /*
                         * Ends server service
                         * Usage: #exit
                         */
                        System.exit(0);
                        return;
                    }else if(adminInput.startsWith("#banuser")){
                        /*
                         * Kicks a specific user from all groups
                         * Usage: #banuser 56789
                         */
                        String targetUser = adminInput.substring(8).trim();
                        try {
                            for(DisasterRoom d: rooms) {
                                if (d.kickClient(Integer.parseInt(targetUser))) {
                                    d.messageClients("User Kicked");
                                }
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid User, please enter identifier");
                        }
                        continue;
                    }else if(adminInput.startsWith("#banip")){
                        /*
                         * Ban a specific IP
                         * Usage: #banip 127.0.0.1
                         */
                        String targetIp = adminInput.substring(6).trim();
                        bannedIps.add(targetIp);    // ban ip from connecting
                        for(DisasterRoom d: rooms) {
                            d.banIP(targetIp);      // close current connections
                        }

                        System.out.println("IP successfully banned");
                        continue;
                    }else if(adminInput.startsWith("#create")){
                        /*
                         * Create a new room and add to room array
                         * Usage: #create Maria
                         */
                        String roomName = adminInput.substring(7).trim();
                        DisasterRoom room = new DisasterRoom(roomName);
                        rooms.add(room);
                        (new Thread(room)).start();
                        System.out.println("Room created");
                        logger.log("Room created: " + roomName);
                        continue;
                    }


                    System.out.println(">> " + adminInput);
                    // all admin messages go to all rooms
                    for(DisasterRoom d: rooms) {
                        d.messageClients(adminInput, "ADMIN");
                    }

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

