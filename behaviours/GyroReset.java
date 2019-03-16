package behaviours;

import java.util.ArrayList;

import core.Robot;
import lejos.hardware.Sound;
import lejos.robotics.subsumption.Behavior;
import libs.OccupancyGrid;
import libs.OccupancyGrid.GridCell;
import libs.Utility;

public class GyroReset implements Behavior {
	private final float BACK_DISTANCE = 3.5f;
	

	private Robot robot;
	private int countResets;
	private boolean suppressed;
	private OccupancyGrid.GridCell lastResetCell;
	
	/**
	 * Constructor of the behaviour "GyroReset"
	 * @param robot
	 */
	public GyroReset(Robot robot) {
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
		OccupancyGrid grid   = robot.getGrid();
		GridCell currentCell = robot.getCurrentCell();
		GridCell previousCell= robot.getPreviousCell();
		return robot.isNeedingGyroReset() 
					&& ( lastResetCell != previousCell )
					&& ( currentCell.isNextToWall() || !grid.getBlockNeighbours(currentCell).isEmpty());
	}
	
	/**
	 * This method performs the movement concerning reseting the gyro.
	 * It consists in moving against a wall/block to force the car into a perpendicular.
	 * Then the gyro is reset and the offset updated.
	 * Then it restores the robot movement and rotation back to the previous state.
	 * @param map_ang
	 * @param rot_ang
	 */
	private void performMovement(float map_ang, float rot_ang) {
		countResets += 1;
		log("Attempting to perform gyro reset movement");
		
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
				
		float dis		 = robot.getDistance();
		float travel_dis = dis * 100;
		
		final float EXTREME_DIS = 0.3f;
		if ( dis >= EXTREME_DIS ) { // problem found! situation where we read a corner of a block		
			log("INVALID DISTANCE READING ON GYRO RESET (returning)");
			return;
		}
		
		log("Gyro reset distance: " + travel_dis);
		
		robot.getPilot().travel(travel_dis, true);
		
	    while(robot.getPilot().isMoving() && !suppressed)
	    	Thread.yield();
	    
	    robot.resetGyro(map_ang);
	    
	    robot.getPilot().travel(-BACK_DISTANCE, true); // make sure you go back the distance from contact to center and not travel dis!
		
	    while(robot.getPilot().isMoving() && !suppressed)
	    	Thread.yield();
	    
	    log("Rotating back with ang: " + -rot_ang);
	    map_ang = Utility.ShortestRotationAngle(map_ang);
	    robot.getPilot().rotate(-rot_ang, true); // rotate back to scan cell
	    
	    while(robot.getPilot().isMoving() && !suppressed)
	    	Thread.yield();
	    
	    log("Gyro reset movement completed!");
	}
	
	/**
	 * Logic conerning in finding if there is a block to use for reset!
	 * If there is calculate how to rotate towards it!
	 * If there are two blocks, then use both! but never more than two! (only need x and y offset centering correction)
	 */
	private void blockReset() {
		OccupancyGrid.GridCell currentCell = robot.getCurrentCell();
		ArrayList<OccupancyGrid.GridCell> neighbours = robot.getGrid().getBlockNeighbours(currentCell);
		
		if ( neighbours.isEmpty() ) {
			log("No blocks to perform a gyro reset were found!");
			return;
		}
		
		log("BLOCK RESET SEQUENCE STARTED!");
		
		for ( OccupancyGrid.GridCell blockCell : neighbours ) {
			if (countResets >= 2 ){
				log("No more adjustments needed! Already done two!");
				return;
			}
			
			Sound.beepSequenceUp();
			
			float sta_ang = robot.getAngle();
			float map_ang = 0;
			float rot_ang = 0;
			
			robot.rotateTowardsCell(blockCell);
			
			map_ang = robot.getGrid().getAngleToCell(currentCell, blockCell);
			rot_ang = -(sta_ang - robot.getAngle());
			
			log("Using nearby block: " + blockCell.x + ", " + blockCell.y + " map ang: " + map_ang + " rot ang" + rot_ang);
			
			while (robot.getPilot().isMoving() && !suppressed)
				Thread.yield();
			
			performMovement(map_ang, rot_ang);
		}
	}
	
	/**
	 * Applies the same logic as the method above
	 * but concerns itself with locating walls!
	 * Specifically if the robot is next to a wall and where it is.
	 */
	private void wallReset() {
		OccupancyGrid.GridCell currentCell = robot.getCurrentCell();
		
		if( !currentCell.isNextToWall() ) {
			log("No wall to perform a gyro reset was found!");
			return;
		}
		
		log("WALL RESET SEQUENCE STARTED!");
		
		ArrayList<Float> map_angs = new ArrayList<Float>();
		
		if ( currentCell.x == 0 )
			map_angs.add(270f);
		
		if ( currentCell.x == robot.getGrid().WIDTH-1 )
			map_angs.add(90f);
		
		if ( currentCell.y == 0 )
			map_angs.add(180f);
		
		if ( currentCell.y == robot.getGrid().HEIGHT-1 )
			map_angs.add(0f);
		
		for( Float map_ang : map_angs ) {
			if (countResets >= 2 ){
				log("No more adjustments needed! Already done two!");
				return;
			}
			
			Sound.beepSequenceUp();
			
			float sta_ang = robot.getAngle();
			float rot_ang = 0;
			
			rot_ang = map_ang - sta_ang;
			
			rot_ang = Utility.ShortestRotationAngle(rot_ang);
			
			log("Using a wall to reset gyro: Map ang:" + map_ang + " rot ang: " + rot_ang );
			
			robot.getPilot().rotate(rot_ang, true);
			
		    while(robot.getPilot().isMoving() && !suppressed)
		    	Thread.yield();
		    
		    performMovement(map_ang, rot_ang);
		}
	}
	
	/**
	 * Action method of the class.
	 * Gyro reseting sequence, the logic is simple:
	 * Lower the current angular and linear speed (for percision)
	 * Find if there is a block to reset or a wall. Rotate towards it and perform the gyro reset and cell centering logic.
	 */
	@Override
	public void action() {
		suppressed = false;
		
		log("===== RESETING GYRO SEQUENCE =====");
		countResets = 0;
		
		// to increase percsion of centering
		robot.getPilot().setAngularSpeed(robot.DEFAULT_ANGULAR_SPEED/1.25);
		robot.getPilot().setLinearSpeed(robot.DEFAULT_LINEAR_SPEED/1.5);
		
		// Always try to perform both if possible. (Like that the robot gets centered perfectly!)
	    blockReset();
	    wallReset();
	    
	    robot.getPilot().setAngularSpeed(robot.DEFAULT_ANGULAR_SPEED);
	    robot.getPilot().setLinearSpeed(robot.DEFAULT_LINEAR_SPEED);
	    
	    lastResetCell = robot.getCurrentCell();
		robot.setNeedingGyroReset(false);
		
		log("===== GYRO RESET SEQUENCE END =====");
	}
	
	/**
	 * Suppression method. (Sets a boolean flag)
	 */
	@Override
	public void suppress() {
		// TODO Auto-generated method stub

	}

}
