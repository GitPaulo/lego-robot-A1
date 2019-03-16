package behaviours;
import core.Robot;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.robotics.subsumption.Behavior;

public class Stop implements Behavior {
	
	private Robot robot;
	
	/**
	 * Constructor of the behaviour "Stop"
	 * @param robot
	 */
	public Stop( Robot robot ){
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
		return Button.ESCAPE.isDown() || robot.isLapCompleted();
	}

	/**
	 * Action method of the class.
	 * This behaviour is concerned with preemptive exiting of the robot and
	 * when the robot completes a lap!
	 */
	@Override
	public void action() {
		Sound.beepSequence();
		robot.getPilot().stop();
		
		if ( !robot.isLapCompleted() ){
			robot.getServerSocket().sendMapState();
			robot.getServerSocket().close();
			robot.closeRobot();
			System.exit(1);
			return;
		}
		
		String s1 = "############## LAP COMPLETED ##############";
		String s2 = "ELAPSED TIME: " + robot.getElapsedTime() + "ms";
		
		log(s1);
		log(s2);
		
		robot.getServerSocket().sendMapState();
		robot.getServerSocket().sendStateObject();
		
		robot.getMonitor().print("== LAP COMPLETED ==");
		robot.getMonitor().print(s2);
		
		Button.waitForAnyPress();
		robot.getServerSocket().close();
		robot.closeRobot();
		
		System.exit(1);
	}
	
	@Override
	public void suppress() {
		// no need, highest priority!
	}

}