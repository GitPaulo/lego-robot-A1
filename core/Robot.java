package core;
import java.io.Serializable;

import lejos.hardware.Brick;
import lejos.hardware.BrickFinder;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.SampleProvider;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.geometry.Point;
import lejos.robotics.localization.OdometryPoseProvider;
import lejos.robotics.navigation.MovePilot;
import lejos.robotics.navigation.Pose;
import libs.OccupancyGrid;
import libs.Utility;
import network.Server;

/**
 * Robot class - this is where all the state of the robot is represented.
 * This class also includes control over certain robot behaviour (such as rotations)
 */
public class Robot implements Serializable {
    //  ID
    private static final long serialVersionUID 	  = -4723713075904647944L;
    // Constants
    public final int DEFAULT_LINEAR_SPEED    	  = 15;
    public final int DEFAULT_ANGULAR_SPEED   	  = 90;
    public final int DEFAULT_ANGULAR_ACCELARATION = 100;
    public final int MAP_CELL_SIZE_X 		 	  = 6;
    public final int MAP_CELL_SIZE_Y 		 	  = 7;
    
    public final float WHEEL_DIAMETER = 4.2f;
    public final float CHASSIS_OFFSET = 5.32f;
     
    private final long START_TIME;
    
    // Sensors
    private EV3TouchSensor lTouchSensor, rTouchSensor;	
    private EV3UltrasonicSensor uSonicSensor;
    private EV3GyroSensor gyroSensor;
    
    // GyroReset Offset
    private float gyro_offset;
    
    // Sample arrays
    private SampleProvider leftSP, rightSP, distSP, gyroSP;	
    private float[] leftSample, rightSample, distSample, angleSample; 
    
    // Move Pilot
    private MovePilot pilot;

    // Occupancy Grid
    private OccupancyGrid grid;
    
    // Motors
    private EV3MediumRegulatedMotor motorC;
    private EV3LargeRegulatedMotor motorL, motorR;
    
    // Robot State
    private boolean needingGyroReset;
    private boolean lapCompleted;
    private boolean scanning;
    private OccupancyGrid.GridCell currentCell;
    private OccupancyGrid.GridCell previousCell;
    private int cellsMoved;
    
    // Odametry
    OdometryPoseProvider opp;
    Point currentLocation;
    Point previousLocation;
    
    // SocketServer and Monitor (not initialised on constructor!)
    private Server server;
    private Monitor monitor;
    
    public Robot() {
        Brick myEV3 = BrickFinder.getDefault();
        
        // Set up sensors to their respective ports
        lTouchSensor = new EV3TouchSensor(myEV3.getPort("S1"));
        gyroSensor   = new EV3GyroSensor(myEV3.getPort("S2"));
        uSonicSensor = new EV3UltrasonicSensor(myEV3.getPort("S3"));
        rTouchSensor = new EV3TouchSensor(myEV3.getPort("S4"));

        leftSP  = lTouchSensor.getTouchMode();
        rightSP = rTouchSensor.getTouchMode();
        distSP  = uSonicSensor.getDistanceMode();
        gyroSP  = gyroSensor.getAngleMode();
        
        leftSample  = new float[leftSP.sampleSize()];	// Size is 1
        rightSample = new float[rightSP.sampleSize()];	// Size is 1
        distSample  = new float[distSP.sampleSize()];	// Size is 1
        angleSample = new float[gyroSP.sampleSize()];	// Size is 1
        
        motorC = new EV3MediumRegulatedMotor(myEV3.getPort("C"));
        motorL = new EV3LargeRegulatedMotor(myEV3.getPort("B"));
        motorR = new EV3LargeRegulatedMotor(myEV3.getPort("D"));
        
        // Set up chasis for the pilot
        Wheel leftWheel   = WheeledChassis.modelWheel(motorL, WHEEL_DIAMETER).offset(-CHASSIS_OFFSET);
        Wheel rightWheel  = WheeledChassis.modelWheel(motorR, WHEEL_DIAMETER).offset(CHASSIS_OFFSET);
        Chassis myChassis = new WheeledChassis(new Wheel[]{leftWheel, rightWheel}, WheeledChassis.TYPE_DIFFERENTIAL);

        pilot = new MovePilot(myChassis);
        pilot.setLinearSpeed(DEFAULT_LINEAR_SPEED); // cm per second
        pilot.setAngularSpeed(DEFAULT_ANGULAR_SPEED);
        pilot.setAngularAcceleration(DEFAULT_ANGULAR_ACCELARATION); 
        
        // Odometry
        opp = new OdometryPoseProvider(pilot);
        Point p 		 = opp.getPose().getLocation();
        currentLocation  = p;
        previousLocation = p;
        
        // Init Occupancy Grid and mark the starting cell as unoccupied
        grid 		= new OccupancyGrid(MAP_CELL_SIZE_X, MAP_CELL_SIZE_Y);
        currentCell = grid.getCell(0, 0);
        currentCell.setVisited(true);
        currentCell.unoccupied();
        
        // Reset the value of the gyroscope to zero
        gyro_offset = 0;
        gyroSensor.reset();
        
        // Benchmark and start scanning!
        START_TIME = System.currentTimeMillis();
        
        // Set flags to start behaviour
        setScanning(true);
    }
    
    /**
     * Returns the occupancy grid instance used by the robot.
     * @return
     */
    public OccupancyGrid getGrid() {
        return grid;
    }
    
    /**
     * Log function - Prints out to the server socket. (attaches class name)
     * @param str
     */
    private void log( String str ) {
        String prefix = "[" + this.getClass().getSimpleName() + "]";
        getServerSocket().sendString(prefix+str);
    }
    
    /**
     * Clean up robot method.
     */
    public void closeRobot() {
        lTouchSensor.close();
        rTouchSensor.close();
        uSonicSensor.close();
        gyroSensor  .close();
    }
    
    /**
     * Sets a flag that the robot is needing a gyro reset.
     * @param reset
     */
    public void setNeedingGyroReset( boolean reset ) {
        this.needingGyroReset = reset;
    }
    
    /**
     * returns the "needingGyroReset" flag
     * @return
     */
    public boolean isNeedingGyroReset() {
        return this.needingGyroReset;
    }
    
    /**
     * returns if the left bumper is currently pressed
     * @return
     */
    public boolean isLeftBumpPressed() {
        leftSP.fetchSample(leftSample, 0);
        return (leftSample[0] == 1.0);
    }
    
    /**
     * returns if the right bumper is currently pressed
     * @return
     */
    public boolean isRightBumpPressed() {
        rightSP.fetchSample(rightSample, 0);
        return (rightSample[0] == 1.0);
    }
    
    /**
     * returns the distance measured by the ultra-sound
     * @return
     */
    public float getDistance() {
        distSP.fetchSample(distSample, 0);
        return distSample[0];
    }
    
    /**
     * returns the angle measured by the gyro sensor
     * @return
     */
    public float getAngle() {
        gyroSP.fetchSample(angleSample, 0);
        
        float ang_inc = angleSample[0];
        
        return (ang_inc+gyro_offset)%360;
    }
    
    /**
     * returns the move pilot attached to the robot
     * @return
     */
    public MovePilot getPilot() {
        return pilot;
    }
    
    /**
     * returns if the robot is scanning
     * @return
     */
    public boolean isScanning() {
        return scanning;
    }
    
    /**
     * sets the robot flag that he should be scanning
     * @param s
     */
    public void setScanning( boolean s ) {
        scanning = s;
    }
    
    /**
     * installs the server socket to the robot
     * @param server
     */
    public void installServer(Server server) {
        this.server = server;
    }
    
    /**
     * returns the server socket instance
     * @return
     */
    public Server getServerSocket() {
        return server;
    }
    
    /**
     * returns the number of cells moved by the robot since the start of the run
     * @return
     */
    public int getCellsMoved() {
        return cellsMoved;
    }
    
    /**
     * returns the current cell of the robot (where it currently is on the grid)
     * @return
     */
    public OccupancyGrid.GridCell getCurrentCell(){
        return currentCell;
    }
    
    /**
     * returns the last cell of the robot (where he was one drive movement ago)
     * @return
     */
    public OccupancyGrid.GridCell getPreviousCell(){
        return previousCell;
    }
    
    /**
     * Sets the robot current cell and updates other tied states.
     * @param cell
     */
    public void setCurrentCell(OccupancyGrid.GridCell cell) { 
        this.previousCell = this.currentCell;
        this.currentCell  = cell;
        
        setCurrentLocation(opp.getPose().getLocation());
        cell.setVisited(true);
        
        cellsMoved = cellsMoved + 1;
        
        getServerSocket().sendStateObject();
    }
    
    /**
     * Returns odometry pose instance tied to the robot
     * @return
     */
    public Pose getPose() {
        return this.opp.getPose();
    }
    
    /**
     * returns the current location of the robot (regulated by internal odometry)
     * @return
     */
    public Point getCurentLocation() {
        return this.currentLocation;
    }
    
    /**
     * returns the current previous of the robot (regulated by internal odometry)
     * @return
     */
    public Point getPreviousLocaiton() {
        return this.previousLocation;
    }
    
    /**
     * setter for the pose location
     * @param p
     */
    public void setCurrentLocation(Point p) {
        this.previousLocation = currentLocation;
        this.currentLocation  = p;
    }
    
    /**
     * returns if a lap has been completed by the robot.
     * @return
     */
    public boolean isLapCompleted() {
        return lapCompleted;
    }

    /**
     * setter for the lap completion flag
     * @param lapCompleted
     */
    public void setLapCompleted(boolean lapCompleted) {
        this.lapCompleted = lapCompleted;
    }
    
    /**
     * returns the amount of time since the start of the run.
     * @return
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - START_TIME;
    }
    
    /**
     * installs the monitor instance tied to the robot
     * @param monitor
     */
    public void installMonitor(Monitor monitor) {
        this.monitor = monitor;
    }
    
    /**
     * returns the monitor instance
     * @return
     */
    public Monitor getMonitor() {
        return this.monitor;
    }
    
    /**
     * returns the motor controling the ultrasonic sensor
     * @return
     */
    public EV3MediumRegulatedMotor getUSMotor() {
        return this.motorC;
    }
    
    private int overflow_prevention = 1; // exists because sensors cant be trusted
    /**
     * Rotates the robot towards a given cell.
     * @param targetCell
     */
    public void rotateTowardsCell(OccupancyGrid.GridCell targetCell) {
        float map_ang = grid.getAngleToCell(currentCell, targetCell);
        float sta_ang = getAngle();
        float rot_ang = map_ang - sta_ang;
        
        log("**** DRIVE ROTATION CALCULATIONS ****");
        log("Gyro Angle:" + getAngle());
        log("Mapped Angle:" + map_ang);
        
        if ( Math.round(rot_ang) == 0 ) 
            return;
        
        rot_ang = Utility.ShortestRotationAngle(rot_ang);
        
        log("Rotation Angle:" + rot_ang);
        
        pilot.rotate(rot_ang, true);
        
        while(pilot.isMoving())
            Thread.yield();
        
        float end_ang = getAngle();
        float da 	  = Math.abs(sta_ang - end_ang);
        float dr	  = Math.round(Math.abs(rot_ang - da));
    
        if ( dr > 2 && overflow_prevention%33 != 0 ) { 
            overflow_prevention += 1;
            rotateTowardsCell(targetCell);
        }
    }
    
    private int overflow_prevention2 = 1;
    /**
     * Rotates the robot forward.
     */
    public void rotateForward() {
        getServerSocket().sendString("Rotating forward!"); 
        float sta_ang = getAngle();
        float rot_ang = 0 - getAngle();
        
        if ( Math.round(rot_ang) == 0 ) 
            return;
        
        rot_ang = Utility.ShortestRotationAngle(rot_ang);
        
        pilot.rotate(rot_ang, true);
        
        while(pilot.isMoving())
            Thread.yield();
        
        float end_ang = getAngle();
        float da 	  = Math.abs(sta_ang - end_ang);
        float dr	  = Math.round(Math.abs(rot_ang - da));
    
        if ( dr > 2 && overflow_prevention2%33 != 0 ) { 
            overflow_prevention2 += 1; 
            rotateForward();
        }
    }
    
    /**
     * Resets gyro and updates robot state. Also updates gyro offset.
     * @param map_ang
     */
    public void resetGyro(float map_ang) {
        map_ang 	= Utility.ShortestRotationAngle(map_ang);
        gyro_offset = map_ang;
        
        gyroSensor.reset();
        getServerSocket().sendString("Reset gyro with offset: " + map_ang);
    }
}