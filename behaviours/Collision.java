package behaviours;
import core.Robot;
import lejos.robotics.subsumption.Behavior;

public class Collision implements Behavior {
    private Robot robot;
    private boolean suppressed;
    
    public Collision( Robot robot ){
        this.robot = robot;
    }
    
    /**
     * Log function - Prints out to the server socket. (attaches class name)
     * @param str
     */
    private void log( String str ) {
        String prefix = "[" + this.getClass().getSimpleName() + "]";
        robot.getServerSocket().sendString(prefix+str);
    }
    
    /**
     * Method used to determine action by the the arbitrator.
     */
    @Override
    public boolean takeControl() {
        return robot.isLeftBumpPressed() || robot.isRightBumpPressed();
    }
    
    /*
     * Action method of the class.
     * The robot will collide with a block and use internal
     * odometry to update its position.
     * Then it try to correct its position using the ultrasound
     * by measuring the offset to the block it collided with.
     */
    @Override
    public void action() {
        suppressed = false;
        
        log("----> ROBOT HAS COLLIDED");
        
        // update grid, calculate displacement, move to previous cell.
        float dis = robot.getPose().distanceTo(robot.getPreviousLocaiton());
        log("Traveling using pose locations. Distance calculated:" + dis);
        
        robot.getPilot().travel(-dis, true);
        
        while(robot.getPilot().isMoving() && !suppressed)
            Thread.yield();
        
        robot.getPilot().stop();
        
        float dis2 = robot.getDistance()*100;
        float cell_half_distance = 25/2;
        
        if ( dis2 > cell_half_distance ) {
            robot.getPilot().travel(dis2-cell_half_distance, true);
            
            while(robot.getPilot().isMoving() && !suppressed)
                Thread.yield();
        } 
    }
    
    /**
     * Suppression method. (Sets a boolean flag)
     */
    @Override
    public void suppress() {
        suppressed = true; // can be suppressed by stop and scanning behaviours
    }

}