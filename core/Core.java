package core;
import java.io.IOException;

import behaviours.Collision;
import behaviours.Drive;
import behaviours.GyroReset;
import behaviours.Scan;
import behaviours.Stop;
import lejos.hardware.Button;
import lejos.robotics.subsumption.Arbitrator;
import lejos.robotics.subsumption.Behavior;
import network.Server;

public class Core { 
    // Constants
    public static final int MONITOR_DELAY = 1000;
    public static final int PORT		  = 1234;
    
    /**
     * Execution start point of the Ev3 Brick program.
     * Initialises:
     * 		- Robot, Monitor, Server and Arbitrator instances
     * @param args
     */
    public static void main(String[] args) {
        Server server   = null;
        Robot robot 	= new Robot();		
        Monitor monitor = new Monitor(robot, MONITOR_DELAY);	
        
        robot.installMonitor(monitor);
        
        try {
              server = new Server(PORT, robot);
              robot.installServer(server);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // Set up the behaviours for the Arbitrator and construct it. (higher the index, higher the priority)
        Behavior b4 = new Stop(robot);
        Behavior b3 = new Collision(robot);
        Behavior b2 = new Scan(robot);
        Behavior b1 = new GyroReset(robot);
        Behavior b0 = new Drive(robot);
        
        Behavior [] bArray = {b0, b1, b2, b3, b4};
        Arbitrator arby    = new Arbitrator(bArray);

        // Clear arbitrator spew
        for (int i=0; i<8; i++)
            System.out.println("");
   
        // Wait for the user to press start
        monitor.print("Press Start!");				
        Button.waitForAnyPress();
        
        // Start the Pilot Monitor
           monitor.start();
           
        // Start the Arbitrator
        arby.go();
    }

}