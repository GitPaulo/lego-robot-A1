package libs;
import java.util.ArrayList;
import java.util.List;

/**
 * This class uses the abstract AstarSearch/AstarNode classes
 * and converts them into something useful for the robot.
 */
public class WaypointResolver {
    /**
     * Class representing a node of the occupancy grid as a graph.
     */
    public class Node extends AStarNode {
        private OccupancyGrid.GridCell cell;
        private ArrayList<AStarNode> neighbours;
        
        public Node ( OccupancyGrid.GridCell cell ){
            this.cell = cell;
            this.neighbours = new ArrayList<AStarNode>();
        }

        public float getCost( AStarNode node ){
            return 1;
        }

        public float getEstimatedCost( AStarNode node ){
            int x1 = this.cell.x;
            int y1 = this.cell.y;
            int x2 = ((WaypointResolver.Node)node).getCell().x;
            int y2 = ((WaypointResolver.Node)node).getCell().y;

            return Math.abs(y2-y1) + Math.abs(x2-x1);
        }

        public void addNeighbour( AStarNode node ){
            this.neighbours.add(node);
        }

        public List getNeighbours(){
            return this.neighbours;
        }

        public OccupancyGrid.GridCell getCell(){
            return this.cell;
        }
    }

    private OccupancyGrid grid;
    private WaypointResolver.Node[] map;
    
    public WaypointResolver( OccupancyGrid grid ){
        final int GRID_SIZE = grid.HEIGHT * grid.WIDTH;
        this.map            = new Node[GRID_SIZE];
        this.grid           = grid;

        // convert grid to map
        OccupancyGrid.GridCell[][] cells = grid.getCells();
        
        // instantiate map nodes
        for(int i=0; i < grid.WIDTH; i++){
            for(int j=0; j < grid.HEIGHT; j++){
                this.map[(i * grid.HEIGHT) + j] = new WaypointResolver.Node(cells[i][j]);
            }
        }

        // add neighbours to map nodes
        for(int i=0; i < grid.WIDTH; i++){
            for(int j=0; j < grid.HEIGHT; j++){
                for (int ni = Math.max(0, i - 1); ni <= Math.min(i + 1, grid.WIDTH - 1); ++ni){
                    for (int nj = Math.max(0, j - 1); nj <= Math.min(j + 1, grid.HEIGHT - 1); ++nj){
                        if (!(ni==i && nj==j) && !(Math.abs(ni-i) > 0 && Math.abs(nj-j) > 0)){  // don't process itself or consider diagonals 
                            WaypointResolver.Node neighbour = this.map[ni * grid.HEIGHT + nj];
                            if (neighbour.getCell().getP() < OccupancyGrid.FREE_CELL_DETERMINANT && neighbour.getCell().getC() > 0){ // nodes only through cells we have scanned! (safety first) 
                                this.map[i * grid.HEIGHT + j].addNeighbour(neighbour); // In othere words, only path find on current map!
                            }
                        }
                    }
                } 
            }
        }
    }
    
    /** 
     * Calculates the shortest path through the current given occupancy grid from a start cell
     * to a goal cell.
     * Returns a lsit of nodes.
     * @param startCell
     * @param goalCell
     * @return
     */
    public List calculatePath( OccupancyGrid.GridCell startCell, OccupancyGrid.GridCell goalCell ){
        AStarSearch search = new AStarSearch();
        
        int pos1 = startCell.x * grid.HEIGHT + startCell.y;
        int pos2 = goalCell.x * grid.HEIGHT + goalCell.y;

        return search.findPath(this.map[pos1], this.map[pos2]);
    }
    
    /**
     * Returns the map.
     * @return
     */
    public WaypointResolver.Node[] getMap(){
        return map;
    }
}