package org.wcs.smart.datamodelmatcher.ui;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class DataModelMatcher {

	  public static void main(String[] args) {
			Display display = new Display();
			Shell shell = new Shell(display);
			
			shell.setText("Data Model Matcher");
			
			DataModelMatcherDialog main = new DataModelMatcherDialog(shell);

			shell.pack();
			shell.open();
			while (!shell.isDisposed ()) {
				if (!display.readAndDispatch ()) display.sleep ();
			}
			display.dispose ();
	    }
}
