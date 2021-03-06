import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

class ClientGUI {
    private static final int LATENCY = 100;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private BufferedReader stdIn;

    private BasicWindow window = null;

    private Label chatDisplayBox;
    private TextBox chatInputBox;

    private StringBuilder chatLines;

    ClientGUI() {
        this("localhost");
    }

    ClientGUI(String ip) {
        try {
            socket = new Socket(ip, 9876);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            stdIn = new BufferedReader(new InputStreamReader(System.in));

        } catch (UnknownHostException e) {
            System.out.println("Error: Unknown Host. \nExiting");
            // e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Error: Unable to connect to server. \nExiting");
            // e.printStackTrace();
            System.exit(2);
        }
    }


    /*
     * Sends message to the server.
     * Handles client-side tags such as #clear
     */
    private Runnable sendMessage = new Runnable() {
        @Override
        public void run() {
            String line = chatInputBox.getText();
            chatInputBox.setText("");
            window.setFocusedInteractable(chatInputBox);

            /*
             * Give user ability to clear chat with #clear
             */
            if(line.equals("#clear") && (chatDisplayBox != null)){
                chatLines = new StringBuilder();
                chatDisplayBox.setText(chatLines.toString());
                return;
            }

            if(line.equals("")){
                return;
            }

            // System.out.println(line);
            out.println(line);
        }
    };

    /*
     * When all elements are ready, get input from Socket
     */
    private Runnable getMessages = new Runnable() {
        @Override
        public void run() {
            try {
                chatLines = new StringBuilder();

                while (true) {
                    if(in.ready() && (chatDisplayBox != null) ) {   // ensure all elements are ready
                        // System.out.println("echo " + in.readLine());

                        String line = in.readLine();

                        chatLines.append(line);
                        chatLines.append("\n");

                        int chatBoxSize = chatDisplayBox.getSize().getRows();
                        String[] chatArray = chatLines.toString().split("\n");

                        // calculate where to start printing
                        int start_position = 0;
                        if(chatBoxSize > 0 && chatArray.length > chatBoxSize){  // chatBoxSize = 0 when first loaded
                            start_position = chatArray.length - chatBoxSize;
                        }

                        // only display as many lines as we can
                        String subArray[] = Arrays.copyOfRange(chatArray, start_position, chatArray.length);
                        chatDisplayBox.setText(String.join("\n", subArray));
                    }

                    try {
                        Thread.sleep(LATENCY);
                    } catch (InterruptedException e) {
                        System.out.println("Interrupt Detected, Closing thread");
                        return;
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    };

    /*
     * Main Layout. Consists of 2 BorderLayouts
     * topPanel - consists of the Message Display window
     * bottomPanel - consists of the inputbox & submit button
     */
    void start(){
        DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
        Terminal terminal = null;
        Screen screen = null;

        Thread chatBoxThread = new Thread(getMessages);
        chatBoxThread.start();

        try {
            terminal = defaultTerminalFactory.createTerminal();
            screen = new TerminalScreen(terminal);
            screen.startScreen();

            // Root Component
            Panel rootPanel = new Panel();
            rootPanel.setLayoutManager(new BorderLayout());

            // Inner Top Panel
            Panel topPanel = new Panel();
            topPanel.setLayoutManager(new BorderLayout());
            topPanel.setLayoutData(BorderLayout.Location.CENTER);
            rootPanel.addComponent(topPanel.withBorder(Borders.singleLine("Output")));

            // Inner Bottom Panel
            Panel bottomPanel = new Panel();
            bottomPanel.setLayoutManager(new BorderLayout());
            bottomPanel.setLayoutData(BorderLayout.Location.BOTTOM);
            rootPanel.addComponent(bottomPanel.withBorder(Borders.singleLine("Chat")) );


            // Label box for displaying chats
            chatDisplayBox = new Label("");
            chatDisplayBox.setLayoutData(BorderLayout.Location.CENTER);
            topPanel.addComponent(chatDisplayBox);

            // Input box for typing data
            chatInputBox = new TextBox("", TextBox.Style.SINGLE_LINE);
            chatInputBox.setLayoutData(BorderLayout.Location.CENTER);
            bottomPanel.addComponent(chatInputBox);

            // Submit button to send data
            Button submitButton = new Button("Submit", sendMessage);
            submitButton.setLayoutData(BorderLayout.Location.RIGHT);
            bottomPanel.addComponent(submitButton);

            // Create window to hold the panel
            window = new BasicWindow();
            window.setHints(Arrays.asList(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
            window.setComponent(rootPanel);

            // Create gui and start gui
            MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));
            gui.addWindowAndWait(window);

        } catch (IOException e) {
            System.out.println("Error creating terminal");
        } finally {
            if(terminal != null){
                try {
                    terminal.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                chatBoxThread.interrupt();
            }
        }
    }
}
