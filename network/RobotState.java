package network;

import java.io.Serializable;

import libs.OccupancyGrid;

/**
 * Class used for networking.
 * Instances of this class are networked to the client.
 */
public class RobotState implements Serializable {
	private static final long serialVersionUID = -4175009697112583344L;
	public OccupancyGrid grid;
	public OccupancyGrid.GridCell currentCell;
	public boolean isLapCompleted;
	public float elapsedTime;
	public int numberOfMovements;
	
	public RobotState ( OccupancyGrid grid, OccupancyGrid.GridCell currentCell, boolean isLapCompleted, float elapsedTime, int numberOfMovements ) {
		this.numberOfMovements = numberOfMovements;
		this.grid 			   = grid;
		this.currentCell 	   = currentCell;
		this.isLapCompleted    = isLapCompleted;
		this.elapsedTime       = elapsedTime;
	}
}
