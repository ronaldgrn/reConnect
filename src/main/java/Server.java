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

        DisasterRoom home = new DisasterRoom("Default");
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

        try {
            ServerSocket welcomeSocket = new ServerSocket(PORT);

            // create default DisasterRoom
            new Thread(home).start();

            Runnable manage = new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
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
                        e.printStackTrace();
                    }
                }
            };
            new Thread(manage).start();

            /* Parse data entered on the server */
            String adminInput;
            while (true) {
                while ((adminInput = stdIn.readLine()) != null) {
                    if (adminInput.equals("#exit")){
                        /*
                         * Ends server service
                         * Usage: #exit
                         */
                        return;
                    }else if(adminInput.startsWith("#banuser")){
                        /*
                         * Kicks a specific user from all groups
                         * Usage: #banuser 56789
                         */
                        String targetUser = adminInput.substring(8).trim();
                        try {
                            if (home.kickClient(Integer.parseInt(targetUser))) {
                                home.messageClients("User Kicked");
                            } else {
                                System.out.println("Kick Failed");
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
                        home.banIP(targetIp);      // close current connections
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
                    home.messageClients(adminInput, "ADMIN");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


/* Class which allocates connected clients to a specified disaster room */
class ChannelAllocator implements Runnable {
    private ArrayList<DisasterRoom> rooms;
    private Socket socket;

    ChannelAllocator(Socket socket, ArrayList<DisasterRoom> rooms) {
        this.socket = socket;
        this.rooms = rooms;
    }

    @Override
    public void run() {
        try {
            /*
             * New client joined, look for #join message and allocate to channel/room
             */
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
            Boolean allocated = false;

            pw.println("Welcome to reConnect");
            pw.println("Available Channels " + this.rooms.toString());
            while (!allocated) {
                if (in.ready()) {
                    String line = in.readLine();

                    if (line.startsWith("#join")) {
                        /*
                         * Join new room
                         * Usage #join roomName
                         */
                        String targetChannel = line.substring(5).trim();

                        for (DisasterRoom room : rooms) {
                            if (room.getName().equals(targetChannel)) {
                                room.addClient(socket);
                                allocated = true;
                                break;
                            }
                        }
                        if (!allocated) {
                            System.out.printf("Requested room '%s' not found \n", targetChannel);
                            pw.println("Room not found");
                            pw.println("Available Channels " + this.rooms.toString());
                        }
                    } else if (line.startsWith("#rooms")) {
                        /*
                         * Show list of available rooms
                         */
                        pw.println("Available Channels " + this.rooms.toString());
                    } else {
                        pw.println("Usage: #join channelName");
                        // pw.close();
                        // in.close()
                    }
                }
            }

//            home.addClient(socket);
//            System.out.println("Adding new client to " + home.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}