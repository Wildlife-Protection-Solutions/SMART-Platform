package org.wcs.smart.datamodelmatcher.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class DataModelMatcher {

	  public static void main(String[] args) {
			Display display = new Display();
			Shell shell = new Shell(display);
			GridLayout layout = new GridLayout(1, true);
			shell.setLayout(layout);
		    
		    GridData gridData = new GridData(SWT.LEFT,SWT.TOP, false, false);
		    shell.setLayoutData(gridData);
			
			shell.setText("Data Model Matcher");
			DataModelMatcherDialog main = new DataModelMatcherDialog(shell);

			shell.pack();
			shell.open();
			while (!shell.isDisposed ()) {
				if (!display.readAndDispatch ()) display.sleep ();
			}
			display.dispose();
	    }
}
