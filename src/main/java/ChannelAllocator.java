import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

/*
 * Takes in a socket and list of rooms and handles the tasks involved
 *  in placing the user in the appropriate room
 */
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