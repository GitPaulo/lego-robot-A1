package libs;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * A class used to define the Occupancy grid data structure. 
 * This grid holds all the probabilities of each individual cell as well as whether the robot has visited them or not.
 * Additionally, it contains many helper methods to return exactly what the robot needs for it's calculations.
 */
public class OccupancyGrid implements Serializable {
	// ID
	private static final long serialVersionUID = -1787008957389378530L;
	public static final double FREE_CELL_DETERMINANT = 0.5; // Probability determining whether a block is blocked or not!
	// Size of the grid
	public final int WIDTH;
	public final int HEIGHT;
	// Hard coded directional points. (For orthogonal movement)
	private final int[] DIRECTIONS;
	
	public class GridCell implements Serializable {
		private static final long serialVersionUID = -1249206336466467891L;
		public final int x;
		public final int y;
		
		private boolean visited; // If a cell has been visited or not by the robot.
		
		private int M;    // meaningful observation count
		private int C;    // overall observation count
		private double P; // probability of occupancy
		
		public GridCell(int x, int y) {
			this.x = x;
			this.y = y;
			this.M = 0;
			this.C = 0;
			this.P = 0;
			this.visited = false;
		}
		
		private void CalculateOccupancyProbability() {
			int dn = 2 * this.C;

			if (dn == 0) {
				this.P = 0;
				return;
			}

			double v = (double) (this.M + this.C) / dn;
			v = v > 1 ? 1 : v;
			v = v < 0 ? 0 : v;

			this.P = v;
		}

		private void seen() {
			this.C = this.C + 1;
		}

		public void occupied() {
			seen();
			this.M = this.M + 1;
		}

		public void unoccupied() {
			seen();
			this.M = this.M - 1;
		}
		
		public void setVisited(boolean b) {
			this.visited = b;
		}
		
		public int getM() {
			return this.M;
		}

		public int getC() {
			return this.C;
		}

		public double getP() {
			CalculateOccupancyProbability();
			return this.P;
		}
		
		public boolean isVisited() {
			return this.visited;
		}
		
		public boolean isNextToWall() {
			return this.x == 0 || this.y == 0 || this.x == WIDTH-1 || this.y == HEIGHT-1;
		}

		public void print() {
			System.out.println("[" + this.x + "][" + this.y + "] = " + "{ M=" + getM() + ", C=" + getC() + " } with Pr("
					+ getP() + ")");
		}
	}
		
	private GridCell[][] grid;

	public OccupancyGrid(int w, int h) {
		this.WIDTH  = w;
		this.HEIGHT = h;
		
		this.DIRECTIONS = new int[4];
		this.grid 		= new GridCell[w][h];
		
		DIRECTIONS[0] = 0;
		DIRECTIONS[1] = 180;  
		DIRECTIONS[2] = 270; 
		DIRECTIONS[3] = 90; 
		
		// Initialise grid with grid cells objects
		for (int row = 0; row < this.WIDTH; row++) {
			for (int col = 0; col < this.HEIGHT; col++) {
				grid[row][col] = new GridCell(row, col);
			}
		}
	}
	
	public OccupancyGrid(OccupancyGrid cgrid) {
		this.WIDTH  = cgrid.WIDTH;
		this.HEIGHT = cgrid.HEIGHT;
		this.DIRECTIONS = cgrid.DIRECTIONS;
		this.grid = cgrid.grid;
	}
	
	/**
	 * Returns a grid cell object at a specified position.
	 * @param x
	 * @param y
	 * @return
	 */
	public GridCell getCell(int x, int y) {
		return this.grid[x][y];
	}
	
	/**
	 * Returns the Grid Cell array
	 * @return
	 */
	public GridCell[][] getCells() {
		return this.grid;
	}
	
	public int getNumVisits() {
		int total = 0;
		for (int row = 0; row < this.WIDTH; row++)
			for (int col = 0; col < this.HEIGHT; col++)
				if ( grid[row][col].isVisited() )
					total += 1;
		return total;
	}
	/**
	 * Calculates the (mapped angle) from one cell to another based on their position.
	 * Orthogonal movement only.
	 * @param c1
	 * @param c2
	 * @return
	 */
	public float getAngleToCell( GridCell c1, GridCell c2 ) { 
		int dy       = c2.y - c1.y;
		int dx       = c2.x - c1.x;
		int map_ang  = 0;
		
		if ( dy > 0 ) // up
			map_ang = DIRECTIONS[0];
		
		if ( dy < 0 ) // down
			map_ang = DIRECTIONS[1];
		
		if ( dx < 0 ) // left
			map_ang = DIRECTIONS[2];
		
		if ( dx > 0 ) // right
			map_ang = DIRECTIONS[3];
		
		return map_ang;
	}
	
	/**
	 * Returns a string of the state of the occupancy grid!
	 * @return
	 */
	public ArrayList<String> getPrintString() {
		ArrayList<String> rstring = new ArrayList<String>();
		for (int row = 0; row < this.WIDTH; row++) {
			String istring = "";
			for (int col = 0; col < this.HEIGHT; col++) {
				GridCell gc = grid[row][col];
				String prefix = col == 0 ? "[" : "";
				String postfix = col + 1 == this.HEIGHT ? "]" : "|";
				istring = istring + String.format("%s%.1f%s", prefix, gc.getP(), postfix);
			}
			rstring.add(istring);
		}
		return rstring;
	}
	
	/**
	 * Returns a string of the state of visits of the occupancy grid!
	 * @return
	 */
	public ArrayList<String> getPrintString2() {
		ArrayList<String> rstring = new ArrayList<String>();
		for (int row = 0; row < this.WIDTH; row++) {
			String istring = "";
			for (int col = 0; col < this.HEIGHT; col++) {
				GridCell gc = grid[row][col];
				String prefix = col == 0 ? "[" : "";
				String postfix = col + 1 == this.HEIGHT ? "]" : "|";
				istring = istring + String.format("%s%s%s", prefix, Boolean.toString(gc.isVisited()).substring(0,1).toUpperCase(), postfix);
			}
			rstring.add(istring);
		}
		return rstring;
	}
	
	/**
	 * Returns an array list of GridCells that neighbour the parameter.
	 * @param cell
	 * @return
	 */
	public ArrayList<GridCell> getNeighbours(GridCell cell) {
		ArrayList<GridCell> neighbours = new ArrayList<GridCell>();

		int up    = cell.y + 1;
		int down  = cell.y - 1;
		int left  = cell.x - 1;
		int right = cell.x + 1;

		if (up >= 0 && up < HEIGHT)
			neighbours.add(grid[cell.x][up]);

		if (down >= 0 && down < HEIGHT)
			neighbours.add(grid[cell.x][down]);

		if (left >= 0 && left < WIDTH)
			neighbours.add(grid[left][cell.y]);

		if (right >= 0 && right < WIDTH)
			neighbours.add(grid[right][cell.y]);

		return neighbours;
	}
	
	/**
	 * Returns an array list of blocked GridCells that neighbour the parameter.
	 * @param cell
	 * @return
	 */
	public ArrayList<GridCell> getBlockNeighbours(GridCell cell) {
		ArrayList<GridCell> neighbours = new ArrayList<GridCell>();
		
		int up    = cell.y + 1;
		int down  = cell.y - 1;
		int left  = cell.x - 1;
		int right = cell.x + 1;
		
		boolean  c1;
		if (up >= 0 && up < HEIGHT)
			c1 = grid[cell.x][up].getP() >= FREE_CELL_DETERMINANT ? neighbours.add(grid[cell.x][up]) : false;
		
		boolean c2;
		if (down >= 0 && down < HEIGHT)
			c2 = grid[cell.x][down].getP() >= FREE_CELL_DETERMINANT ? neighbours.add(grid[cell.x][down]) : false;
		
		boolean c3;
		if (left >= 0 && left < WIDTH)
			c3 = grid[left][cell.y].getP() >= FREE_CELL_DETERMINANT ? neighbours.add(grid[left][cell.y]) : false;
		
		boolean c4;
		if (right >= 0 && right < WIDTH)
			c4 = grid[right][cell.y].getP() >= FREE_CELL_DETERMINANT ? neighbours.add(grid[right][cell.y]) : false;

		return neighbours;
	}
	
	/**
	 * Returns an array list of to-scan GridCells that neighbour the parameter.
	 * @param cell
	 * @return
	 */
	public ArrayList<GridCell> getScanNeighbours(GridCell cell) {
		ArrayList<GridCell> neighbours = new ArrayList<GridCell>();

		int up    = cell.y + 1;
		int down  = cell.y - 1;
		int left  = cell.x - 1;
		int right = cell.x + 1;

		boolean  c1;
		if (up >= 0 && up < HEIGHT)
			c1 = !grid[cell.x][up].isVisited() ? neighbours.add(grid[cell.x][up]) : false;
		
		boolean c2;
		if (down >= 0 && down < HEIGHT)
			c2 = !grid[cell.x][down].isVisited() ? neighbours.add(grid[cell.x][down]) : false;
		
		boolean c3;
		if (left >= 0 && left < WIDTH)
			c3 = !grid[left][cell.y].isVisited() ? neighbours.add(grid[left][cell.y]) : false;
		
		boolean c4;
		if (right >= 0 && right < WIDTH)
			c4 = !grid[right][cell.y].isVisited() ? neighbours.add(grid[right][cell.y]) : false;

		return neighbours;
	}
	
	/**
	 * Returns an array list of GridCells that are free and scanned but not visited that neighbour the parameter.
	 * @param cell
	 * @return
	 */
	public ArrayList<GridCell> getFreeUnvisitedNeighbours(GridCell cell) {
		ArrayList<GridCell> neighbours = new ArrayList<GridCell>();

		int up    = cell.y + 1;
		int down  = cell.y - 1;
		int left  = cell.x - 1;
		int right = cell.x + 1;
		
		boolean  c1;
		if (up >= 0 && up < HEIGHT)
			c1 = grid[cell.x][up].getP() < FREE_CELL_DETERMINANT && !grid[cell.x][up].isVisited() ? neighbours.add(grid[cell.x][up]) : false;
		
		boolean c2;
		if (down >= 0 && down < HEIGHT)
			c2 = grid[cell.x][down].getP() < FREE_CELL_DETERMINANT && !grid[cell.x][down].isVisited() ? neighbours.add(grid[cell.x][down]) : false;
		
		boolean c3;
		if (left >= 0 && left < WIDTH)
			c3 = grid[left][cell.y].getP() < FREE_CELL_DETERMINANT && !grid[left][cell.y].isVisited() ? neighbours.add(grid[left][cell.y]) : false;
		
		boolean c4;
		if (right >= 0 && right < WIDTH)
			c4 = grid[right][cell.y].getP() < FREE_CELL_DETERMINANT && !grid[right][cell.y].isVisited() ? neighbours.add(grid[right][cell.y]) : false;

		return neighbours;
	}
	
	/**
	 * Calculates and returns across the whole grid free, scanned, unvisited cells.
	 * Used for path-finding.
	 * @return
	 */
	public ArrayList<GridCell> calculateScannedUnvisited() {
		ArrayList<GridCell> scanned_unvisited = new ArrayList<GridCell>();
		
		for (int row = 0; row < this.WIDTH; row++) {
			for (int col = 0; col < this.HEIGHT; col++) {
				GridCell gc = grid[row][col];
				if ( gc.getP() < FREE_CELL_DETERMINANT && !gc.isVisited() && gc.getC() > 0 ) {
					scanned_unvisited.add(gc);
				}
			}
		}
		
		return scanned_unvisited;
	}
}