package gui;

import javax.swing.JFrame;
import java.awt.GridLayout;
import javax.swing.JPanel;

import libs.OccupancyGrid;
import network.RobotState;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Font;

public class GUIOutput {
	// constants
	private final int WIDTH = 6;
	private final int HEIGHT = 7;
	// private members
	private RobotState robotState;
	private JButton[][] gridButtons;
	// public frame
	public JFrame frame;

	/**
	 * Create the application.
	 */
	public GUIOutput() {
		initialize();
	}
	
	/**
	 * Method to set the default colors of the cell
	 * @param b
	 * @param i
	 * @param j
	 */
	private void setColors ( JButton b, int i, int j ) {
		b.setBackground(Color.WHITE);

		if (i == 0 && j == 0)
			b.setBackground(Color.BLUE);

		if (i == 0 && j == HEIGHT - 1)
			b.setBackground(Color.YELLOW);

		if (i == WIDTH - 1 && j == 0)
			b.setBackground(Color.GREEN);

		if (i == WIDTH - 1 && j == HEIGHT - 1)
			b.setBackground(Color.RED);
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 900, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		JPanel headerPanel = new JPanel();
		headerPanel.setBounds(0, 0, 884, 81);
		frame.getContentPane().add(headerPanel);
		headerPanel.setLayout(null);

		Label title = new Label("Group 16 - COMP329");
		title.setAlignment(Label.CENTER);
		title.setFont(new Font("Dialog", Font.PLAIN, 60));
		title.setBounds(151, 10, 585, 61);
		headerPanel.add(title);

		JPanel gridPanel = new JPanel();
		gridPanel.setBounds(0, 81, 884, 480);
		frame.getContentPane().add(gridPanel);
		gridPanel.setLayout(new GridLayout(WIDTH, HEIGHT, 0, 0));

		gridButtons = new JButton[WIDTH][HEIGHT];

		for (int i = 0; i < WIDTH; i++) {
			for (int j = 0; j < HEIGHT; j++) {
				gridButtons[i][j] = new JButton("V("+i+","+j+") - P(0.00)");

				setColors(gridButtons[i][j], i, j);

				gridPanel.add(gridButtons[i][j]);
			}
		}
	}
	
	/**
	 * Method used to update the gui state.
	 */
	private void updateGUI() {
		for(int i=0; i<WIDTH; i++) {
			for(int j=0; j<HEIGHT; j++) {
				final OccupancyGrid.GridCell cell = robotState.grid.getCell(i, j);
				gridButtons[i][j].setText("V("+i+","+j+") - P("+cell.getP()+")");
				
				setColors(gridButtons[i][j], i, j);
				
				if( cell.isVisited() )
					gridButtons[i][j].setBackground(Color.GRAY);
				
				if( cell.getP() > 0.5 )
					gridButtons[i][j].setBackground(Color.BLACK);
				
				if ( cell.x == robotState.currentCell.x && cell.y ==  robotState.currentCell.y )
					gridButtons[i][j].setBackground(Color.PINK);
				
				
				ActionListener buttonClick = new ActionListener() {
				    @Override
				    public void actionPerformed(ActionEvent e) {
				    	GUIInfoBox infoBox = new GUIInfoBox(cell);
				    }
				};
				
				ActionListener[] als = gridButtons[i][j].getActionListeners();
				if ( als.length <= 0 ) {
					gridButtons[i][j].addActionListener(buttonClick);
				}else {
					als[0] = buttonClick;
				}
			}
		}
	}
	
	/**
	 * Method that starts the update state.
	 * @param o
	 */
	public void updateGUI(RobotState o) {
		this.robotState = o;
		updateGUI();
		
		if ( o.isLapCompleted )
			new GUILapCompleted(o);
	}
}
