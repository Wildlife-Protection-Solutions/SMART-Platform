package org.wcs.smart.datamodelmatcher.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class DataModelMatcher {

	  public static void main(String[] args) {
			Display display = new Display();
			Shell shell = new Shell(display);
			
			Point size = shell.computeSize(-1, -1);
	        shell.setBounds(50, 50, size.x, size.y);    
			
			
			GridLayout layout = new GridLayout(1, true);
			shell.setLayout(layout);
		    
		    GridData gridData = new GridData(SWT.LEFT,SWT.TOP, false, false);
		    shell.setLayoutData(gridData);
			
			shell.setText("Data Model Matcher");
			new DataModelMatcherDialog(shell);

			shell.pack();
			shell.open();
			while (!shell.isDisposed ()) {
				if (!display.readAndDispatch ()) display.sleep ();
			}
			display.dispose();
	    }
}
