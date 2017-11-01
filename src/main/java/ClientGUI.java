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

public class ClientGUI {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private BufferedReader stdIn;


    private BasicWindow window = null;


    private Label chatDisplayBox;
    private TextBox chatInputBox;

    public ClientGUI() {
        try {
            socket = new Socket("localhost", 9876);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            stdIn = new BufferedReader(new InputStreamReader(System.in));

//        ) {
//            String userInput;
//            while ((userInput = stdIn.readLine()) != null) {
//                out.println(userInput);
//                System.out.println("echo " + in.readLine());
//            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("IO Error");
            e.printStackTrace();
        }
    }


    private Runnable sendMessage = new Runnable() {
        @Override
        public void run() {
            String line = chatInputBox.getText();
            chatInputBox.setText("");
            window.setFocusedInteractable(chatInputBox);

            // System.out.println(line);
            out.println(line);
        }
    };

    private Runnable getMessages = new Runnable() {
        @Override
        public void run() {
            try {
                String chatLines = "";

                while (true) {
                    if(in.ready() && (chatDisplayBox != null) ) {   // ensure all elements are ready
                        // System.out.println("echo " + in.readLine());

                        String line = in.readLine();
                        chatLines += line + "\n";
                        chatDisplayBox.setText(chatLines);
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    };

    public void start(){
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

//            if(terminal != null){
//                try {
//                    terminal.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
        }
    }
}
