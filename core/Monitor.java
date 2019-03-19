package core;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.Font;
import lejos.hardware.lcd.GraphicsLCD;

/*
 * A class ran on a separate thread that prints out the 
 * robot's current state to the LCD screen.
 */
public class Monitor extends Thread {	
   
  private final GraphicsLCD lcd = LocalEV3.get().getGraphicsLCD();
  private final long PRINT_TIME = 2000;
  private final String TITLE    = "G16";	

  private boolean suppress;
  private int delay;
  public Robot robot;
  
    public Monitor(Robot r, int d){
      this.setDaemon(true);

      this.delay    = d;
      this.robot 	  = r;
      this.suppress = false;
    }
    
    /**
     * Helper method used to access the LCD.
     * Prints out a string and suppresses the state display for a few seconds.
     * @param txt
     */
    public void print( String txt ) {
      suppress = true;
      
      lcd.clear();
      lcd.setFont(Font.getDefaultFont());
      lcd.drawString("> " + txt, 0, lcd.getHeight()/2, 0);
      
      try{
      sleep(PRINT_TIME);
      suppress = false;
    }catch(Exception e) {
      e.printStackTrace();
    }
    }

    /**
     * Run method of the thread controlling the monitor!
     */
    public void run(){
      while(true){
        if ( suppress ) 
          continue;
        
        lcd.clear();
        lcd.setFont(Font.getSmallFont());
        
        int _y = 0;
          for ( String o : robot.getGrid().getPrintString() ) {
            lcd.drawString(o, 0, _y, 0);
            _y += 20;
          }
        
          lcd.drawString(
              TITLE + 
              "|C:" + robot.getCurrentCell().x + "," + robot.getCurrentCell().y + 
              "|S:" + Boolean.toString(robot.isScanning()).substring(0,1).toUpperCase() + 
              "|M:" + Boolean.toString(robot.getPilot().isMoving()).substring(0,1).toUpperCase() +
              "|#C:"+ Integer.toString(robot.getCellsMoved()) +
              "|LC:"+ Boolean.toString(robot.isLapCompleted()).substring(0,1).toUpperCase()
              
          , 0, _y, 0);
        
          try{
          sleep(delay);
        }catch(Exception e) {
          e.printStackTrace();
        }
      }
    }
}