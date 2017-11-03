public class Client {
    public static void main(String args[]){
        // If ip supplied, use this IP to connect to server
        if(args.length == 1){
            ClientGUI client = new ClientGUI(args[0]);
            client.start();
        } else {
            ClientGUI client = new ClientGUI();
            client.start();
        }
    }
}