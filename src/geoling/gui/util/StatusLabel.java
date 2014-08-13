package geoling.gui.util;

import java.awt.Color;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JLabel;

/**
 * Modified <code>JLabel</code> which shows a status message with some background color.
 * <li>status 0: empty label (no color)</li>
 * <li>status 1: todo (white background color)</li>
 * <li>status 2: started (yellow background color)</li>
 * <li>status 3: finished (green background color)</li>
 * <li>status 4: error (red background color)</li>
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class StatusLabel extends JLabel {

	private static final long serialVersionUID = 1L;

	private int status;

	/** Creates object with initial status. */
	public StatusLabel(int status) {
		this.status = status;
		this.setPreferredSize(new Dimension(150, 30));

		switch (status) {
		case 0: this.setText(""); this.setOpaque(false); break;
		case 1: this.setText("TODO"); this.setBackground(Color.WHITE); this.setOpaque(true); break;
		case 2: this.setText("Started at: " + Calendar.getInstance().getTime().toString()); this.setBackground(Color.YELLOW); this.setOpaque(true); break;
		case 3: this.setText("Finished at: " + Calendar.getInstance().getTime().toString()); this.setBackground(Color.GREEN); this.setOpaque(true); break;
		case 4: this.setText("Error occured at: " + Calendar.getInstance().getTime().toString()); this.setBackground(Color.RED); this.setOpaque(true); break;
		default: throw new IllegalArgumentException("Unknown status for StatusLabel!");
		}
	}

	/** Changes status, i.e. change text and background color.
	 * <li>status 0: empty label</li>
	 * <li>status 1: todo </li>
	 * <li>status 2: started </li>
	 * <li>status 3: finished </li>
	 * <li>status 4: error </li>
	 * 
	 * @param status the new status
	 */
	public void changeStatus(int status) {
		this.status = status;

		switch (status) {
		case 0: this.setText(""); this.setOpaque(false); break;
		case 1: this.setText("TODO"); this.setBackground(Color.WHITE); this.setOpaque(true); break;
		case 2: this.setText("Started at: " + getTime()); this.setBackground(Color.YELLOW); this.setOpaque(true); break;
		case 3: this.setText("Finished at: " + getTime()); this.setBackground(Color.GREEN); this.setOpaque(true); break;
		case 4: this.setText("Error occured at: " + getTime()); this.setBackground(Color.RED); this.setOpaque(true); break;
		default: throw new IllegalArgumentException("Unknown status for StatusLabel!");
		}
	}

	/** Returns current status. */
	public int getStatus() {
		return status;
	}

	private static String getTime() {
		SimpleDateFormat formatter = new SimpleDateFormat ("HH:mm:ss");
		Date currentTime = new Date();
		return formatter.format(currentTime);
	}
}