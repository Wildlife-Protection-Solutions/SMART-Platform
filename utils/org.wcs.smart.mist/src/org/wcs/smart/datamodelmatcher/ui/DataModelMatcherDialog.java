package org.wcs.smart.datamodelmatcher.ui;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class DataModelMatcherDialog extends JFrame {

	
	public DataModelMatcherDialog(){
		super("Data Model Matcher");
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		createDialog();
	}
	
	 /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private void createDialog() {
         //Add the ubiquitous "Hello World" label.
        JLabel label = new JLabel("Hello World");
        getContentPane().add(label);
    }
    
}
