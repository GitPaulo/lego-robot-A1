package gui;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import network.RobotState;

import java.awt.Font;
import javax.swing.JEditorPane;

public class GUILapCompleted {

    private RobotState robotState;
    private JFrame frame;

    /**
     * Create the application.
     */
    public GUILapCompleted(RobotState robotState) {
        this.robotState = robotState;
        initialize();
        frame.toFront();
        frame.setVisible(true);
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 363, 207);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);
        
        JLabel lblTitle = new JLabel("Lap completed!!");
        lblTitle.setFont(new Font("Microsoft JhengHei", Font.BOLD, 40));
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.setBounds(10, 11, 327, 43);
        frame.getContentPane().add(lblTitle);
        
        JEditorPane editorPane = new JEditorPane();
        editorPane.setBounds(10, 65, 327, 92);
        editorPane.setText(
                "Robot has completed the lap!" + "\n" +
                "Time elapsed: " + robotState.elapsedTime + "(ms)\n" +
                "Number of visited cells: " + robotState.grid.getNumVisits() + "\n" +
                "Number of movements: " + robotState.numberOfMovements
        );
        frame.getContentPane().add(editorPane);
    }
}
