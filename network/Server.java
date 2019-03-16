package network;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import core.Robot;
import lejos.hardware.Sound;
import libs.OccupancyGrid;

/**
 * Server class that represents the endpoint of connection.
 * This is where the log-client attempts to connect to.
 * The robot keeps an instance of this class so that it can print out it's
 * state to it. Which is then networked to the client program.
 */
public class Server {
	private Robot robot;
	private ServerSocket server;
	private Socket client;
	private OutputStream os;
	private InputStream is;
	private ObjectInputStream  in;
	private ObjectOutputStream pr;
	
	public Server ( int port, Robot robot ) throws IOException {
		this.robot  = robot;
		this.server = new ServerSocket(port);
		
		Sound.beep();
		robot.getMonitor().print("Awaiting client..");
		
		this.client = server.accept();
		
		Sound.beep();
		robot.getMonitor().print("CONNECTED!");
		
		this.os = client.getOutputStream();
		this.is = client.getInputStream();
	
		this.pr   = new ObjectOutputStream(os);
        this.in   = new ObjectInputStream(is);
		
		pr.writeObject("===[ CONNECTED TO EV3 BRICK ]===");
	}
	
	/**
	 * Writes the robot instance object to the stream.
	 */
	public void sendStateObject () {
		Thread t = new Thread() {
		    public void run() {
		    	try {    	
		    		RobotState rs = new RobotState(new OccupancyGrid(robot.getGrid()), robot.getCurrentCell(), robot.isLapCompleted(), robot.getElapsedTime(), robot.getCellsMoved());
		    		pr.reset();
		    		pr.writeUnshared(rs);
					pr.reset();
				} catch (IOException e) {
					e.printStackTrace();
				}
		    }
		};
		t.setPriority(t.MAX_PRIORITY);
		t.start();
	}
	
	/**
	 * Sends the current occupancy grid state to the client.
	 * and also sends the visits grid.
	 */
	public void sendMapState () {
		if(true) return; // (was used for debugging)
		try {
			pr.writeObject("**** OCCUPANCY GRID *****");
			
			for ( String s : robot.getGrid().getPrintString() )
				pr.writeObject(s);
			
			pr.writeObject("**** VISITS GRID *****");
			
			for ( String s : robot.getGrid().getPrintString2() )
				pr.writeObject(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Method used to print out to the client.
	 * @param msg
	 */
	public void sendString ( String msg ) {
		try {
			if( "%CLOSE%".equals(msg) )
				pr.writeObject(msg);
			
			if(true) 
				return; // (was used for debugging)
			
			pr.writeObject(msg);
			pr.reset();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Clean up method.
	 */
	public void close() {
		try {
			sendString("%CLOSE%");
			client.close();
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
