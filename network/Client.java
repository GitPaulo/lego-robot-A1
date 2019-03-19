package network;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import gui.GUIOutput;

/**
 * Client class used to connect to the server (hosted on the Ev3 brick)
 * This is where most of the output concerning what the robot is doing is logged to.
 */
public class Client {
    public static final String IP = "192.168.70.173"; // 173 default bot 20
    public static final int  PORT = 1234;
    
    /**
     * Wrap function for print.
     * @param txt
     */
    public static void print( String txt ) {
        System.out.println("[CLIENT] " + txt);
    }
    
    /**
     * Initialises the client and attempts to connect to the server.
     * From there, it awaits for messages and prints them out to the console screen.
     * @param args
     * @throws UnknownHostException
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    public static void main (String[] args) throws UnknownHostException, IOException, ClassNotFoundException {
        Scanner input = new Scanner(System.in);
        
        print("Input IP (press ENTER to use default):");
        String ip = input.nextLine();
        
        if( ip.length() <= 0 )
            ip = IP;
        
        print("Input PORT (press ENTER to use default):");
        int port = -1;
        
        try {
            port = Integer.parseInt(input.nextLine());
        }catch(NumberFormatException e) {
            port = PORT;
        }
        
        
        Socket echoSocket 	   = null;
        ObjectInputStream in   = null;
        ObjectOutputStream out = null;
     
        try {
            echoSocket = new Socket(ip, port);
            out 	   = new ObjectOutputStream(echoSocket.getOutputStream());
            in 		   = new ObjectInputStream(echoSocket.getInputStream());
        } catch (UnknownHostException e) {
            print("Unknown host: " + IP);
            System.exit(1);
        } catch (IOException e) {
            print("Unable to get streams from server!");
            System.exit(1);
        }
        
        GUIOutput gui = new GUIOutput();
        gui.frame.setVisible(true);
        gui.frame.toFront();
        
        while (!echoSocket.isClosed() ) {
            Object o = in.readObject();
            
            if ( o instanceof String ) {
                String msg = (String) o;
                if ( "%CLOSE%".equals(msg) )
                    break;
                System.out.println("[EV3 Brick] " + msg);
            } else if ( o instanceof RobotState ) {
                gui.updateGUI((RobotState) o);
            }
        }
        
        print("Connection closed by EV3 Brick.");
        
        /** Closing all the resources */
        out			.close();
        in			.close();
        echoSocket	.close();
        input		.close();
    }
}
