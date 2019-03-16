package behaviours;
import java.util.ArrayList;
import java.util.List;
import core.Robot;
import lejos.robotics.subsumption.Behavior;
import libs.OccupancyGrid;
import libs.WaypointResolver;

public class Drive implements Behavior {
	private final int CELL_DISTANCE = 25; // cm
	
	private Robot robot;
	private boolean suppressed;
	
	/**
	 * Constructor of the behaviour "Collision"
	 * @param robot
	 */
	public Drive( Robot robot ){
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
		return !robot.isScanning();
	}

	/**
	 * Action method for Drive.
	 * Begin by checking if there are any FREE, UNVISITED, SCANNED neighbours.
	 * If there are pick one and move to it. 
	 * Else path find to a FREE, UNVISITED SCANNED cell. 
	 */
	@Override
	public void action() {
	   suppressed = false;
	   
	   // Calculate next destination, drive to it, update robot state, start scan again
	   OccupancyGrid.GridCell 			 currentCell = robot.getCurrentCell();
	   OccupancyGrid.GridCell			 nextCell    = null;
	   ArrayList<OccupancyGrid.GridCell> neighbours  = robot.getGrid().getFreeUnvisitedNeighbours(currentCell);
	   
	   log("CurrentCell: " + currentCell.x + "," + currentCell.y);
	   
	   if ( !neighbours.isEmpty() ) { // there are free, scanned, unvisited neighbour cells
		   // Randomly pick a cell or pick first so that we go around in a circle? **IF THE BOT DOESNT GO AROUND IN THE SQUARE BORDER CHECK THIS CODE!**
		   //int rnd = new Random().nextInt(neighbours.size());
		   nextCell = neighbours.get(0);
		   
		   log("Free unvisited neighbours found - picked: " + nextCell.x + "," + nextCell.y);
		   log("[rotating towards picked next cell]");
		   
		   robot.rotateTowardsCell(nextCell);
		   
		   while( robot.getPilot().isMoving() && !suppressed )  // Does rotation trigger isMoving? probably.
		         Thread.yield();
		   
		   log("[traveling towards picked next cell]");
		   
		   robot.getPilot().travel(CELL_DISTANCE, true);
		   
	       while( robot.getPilot().isMoving() && !suppressed )
	    	   Thread.yield();
	       
		   robot.setCurrentCell(nextCell);
	   } else { 												// must pathfind to a free, unvisited, scanned neighbour cell
		   log("No free, unvisted neighbour cells found! [Attempting to pathfind]...");
		  
		   OccupancyGrid current_grid 					   = robot.getGrid();
		   ArrayList<OccupancyGrid.GridCell> possibleCells = current_grid.calculateScannedUnvisited();	
		   
		   if ( !possibleCells.isEmpty() ) {
			   log("Found free, scanned unvisted cells!");
			   
			   List path = null;
			   for( OccupancyGrid.GridCell goalCell : possibleCells ) {
				   WaypointResolver wr = new WaypointResolver(current_grid);
				   path = wr.calculatePath(currentCell, goalCell);
				   
				   if( path != null ) { // avoid calculating any more
					   log("Found a path to cell: " + goalCell.x + ", " + goalCell.y);
					   break;
				   }
			   }
			   
			   if( path != null ) {
				   log(">> Beggining path following:");
				   for( Object o : path ) {
					   WaypointResolver.Node  node = (WaypointResolver.Node)o;
					   OccupancyGrid.GridCell pathCell = node.getCell();
					   
					   log("(Cell on path) Rotating & Traveling to cell: " + pathCell.x + ", " + pathCell.y);
					   robot.rotateTowardsCell(pathCell);
	
					   while( robot.getPilot().isMoving() && !suppressed )  // Does rotation trigger isMoving? probably.
					         Thread.yield();
					   
					   robot.getPilot().travel(CELL_DISTANCE-1, true);
					   
					   while( robot.getPilot().isMoving() && !suppressed )  // Does rotation trigger isMoving? probably.
					         Thread.yield();
					   
					   robot.getPilot().stop();
					   robot.setCurrentCell(pathCell);
				   }
				   log(">> Path completed!");
			   } else { // completed!
				   log("No paths were found! (possible end of lap?)");
				   robot.setLapCompleted(true);
			   }
		   } else {
			   log("No scanned unvisted cells! Robot is blocked!");
			   robot.setLapCompleted(true);
		   }
	   }
	   
	   robot.getPilot().stop();
	   robot.setScanning(true);
	}
	
	/**
	 * Suppression method. (Sets a boolean flag)
	 */
	@Override
	public void suppress() {
		suppressed = true; // can be suppressed by stop, scan, collision
	}

}