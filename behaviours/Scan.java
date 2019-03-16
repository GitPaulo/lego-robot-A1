package behaviours;
import java.util.ArrayList;

import core.Robot;
import lejos.hardware.Sound;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.robotics.subsumption.Behavior;
import libs.OccupancyGrid;
import libs.Utility;

public class Scan implements Behavior {
	
	private final float DISTANCE_LIMIT = 0.135f;
	private Robot robot;
	private boolean suppressed;
	
	/**
	 * Constructor of the behaviour "Scan"
	 * @param robot
	 */
	public Scan(Robot robot) {
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
		return robot.isScanning();
	}
	
	
	/*
	 * 
	 */
	private float scanNeighbours(OccupancyGrid grid, OccupancyGrid.GridCell targetCell) {
		EV3MediumRegulatedMotor motor = robot.getUSMotor();
				
		float map_ang = grid.getAngleToCell(robot.getCurrentCell(), targetCell);
		float rot_ang = map_ang - robot.getAngle();

		log("**** SCAN NEIGHBOR ROTATION CALCULATIONS ****");
		log("Gyro Angle:" + robot.getAngle());
		log("Mapped Angle:" + map_ang);
		
		rot_ang = Utility.ShortestRotationAngle(rot_ang);
		
		log("USRotation Angle: -" + rot_ang);
		motor.rotate((int)-rot_ang, true); // because +90 is to the left on motorC 
		
		while(motor.isMoving() && !suppressed)
			Thread.yield();
		
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		float dis = robot.getDistance();
		log("Reseting pos: " + rot_ang);
		motor.rotate((int)rot_ang, true); // rest motor C to center
		
		while(motor.isMoving() && !suppressed)
			Thread.yield();
		
		return dis;
	}

	@Override
	/**
	 * Action method for scan.
	 * Robot scans neighbouring cells using the ultra-sound and calculates if there may
	 * be a block on one of the neighbour cells.
	 */
	public void action() {
		suppressed = false;
		 
		// Get gird instance, and current cell.
		OccupancyGrid.GridCell currentCell = robot.getCurrentCell();
		OccupancyGrid grid 			       = robot.getGrid();
		
		// Find, rotate-to and scan neighbour cells
		ArrayList<OccupancyGrid.GridCell> currentCellNeighbours = grid.getScanNeighbours(currentCell);
		
		for( OccupancyGrid.GridCell scanningCell : currentCellNeighbours ) {
			robot.getServerSocket().sendString("===> Scanning Cell: " + scanningCell.x + ", " + scanningCell.y);
			
			try { // Scan and update grid
				float scan_dis = scanNeighbours(grid, scanningCell);
				robot.getServerSocket().sendString("Scanned Distance:" + scan_dis);
				
				if ( scan_dis <= DISTANCE_LIMIT ) {
					scanningCell.occupied();
					Sound.twoBeeps();
					robot.getServerSocket().sendString("Cell Occupied!");
				}else {
					scanningCell.unoccupied();
					robot.getServerSocket().sendString("Cell Unoccupied!");
				}
				
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if ( robot.getPreviousCell() != null && currentCell != robot.getPreviousCell() )
			robot.setNeedingGyroReset(true);
		
		robot.setScanning(false); // scan is over (only need to scan once per visit)
	}
	
	/**
	 * Suppression method. (Sets a boolean flag)
	 */
	@Override
	public void suppress() {
		suppressed = true;
	}

}