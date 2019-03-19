package gui;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import libs.OccupancyGrid;
import libs.OccupancyGrid.GridCell;

import java.awt.Font;
import javax.swing.JTextPane;

public class GUIInfoBox {
    private OccupancyGrid.GridCell cell;
    private JFrame frame;
    /**
     * Create the application.
     * @param cell 
     */
    public GUIInfoBox(GridCell cell) {
        this.cell = cell;
        initialize();
        frame.setVisible(true);
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 321, 198);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);
        
        JLabel boxTitleLabel = new JLabel("GridCell C(" + cell.x + ", " + cell.y + ")" );
        boxTitleLabel.setFont(new Font("Dialog", Font.PLAIN, 30));
        boxTitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        boxTitleLabel.setBounds(10, 11, 285, 31);
        frame.getContentPane().add(boxTitleLabel);
        
        JTextPane textPane = new JTextPane();
        textPane.setBounds(10, 53, 285, 95);
        frame.getContentPane().add(textPane);
        
        textPane.setText(
                "Block Probability: "  + cell.getP() + "\n" +
                "Seen count: "		   + cell.getC() + "\n" +
                "Seen blocked count: " + cell.getM() + "\n" +
                "isVisited: " 		   + cell.isVisited() + "\n" +
                "isNextToWall: " 	   + cell.isNextToWall() + "\n"
        );
    }
}
