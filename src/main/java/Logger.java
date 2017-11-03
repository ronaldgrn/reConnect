import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private PrintWriter writer;
    private Boolean filelog;    // determines if we log to file or screen

    Logger(String filename){
        try {
            if(new File("logs").mkdir()){
                System.out.println("Log directory created");
            }
            writer = new PrintWriter(new FileWriter("logs/" +filename, true), true);
            writer.println(getDate() + ": Logger started" );
            filelog = true;
        } catch (IOException e){
            System.out.println("Unable to create log file, falling back to screen log");
            filelog = false;
        }
    }


    void log(String msg){
        msg = getDate() + ": " + msg;
        if(filelog){
            writer.println(msg);
            writer.flush();
        } else {
            System.out.println(msg);
        }
    }

    private static String getDate(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
}
