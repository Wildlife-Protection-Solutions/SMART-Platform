package org.wcs.smart.datamodelmatcher.ui;



import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class ProcessingDialog extends Dialog{
  Display display;
  Shell shell;

  public ProcessingDialog(Shell parentShell, Rectangle location) {
	super(parentShell);
	this.shell = parentShell;
	this.display = shell.getDisplay(); 

	Point p = Display.getCurrent().getCursorLocation();
	Point start = new Point(p.x - 100, p.y -30);
    shell.setLocation(start);

  }

  public int open(){
	  
    GridLayout layout = new GridLayout(1, false);
	shell.setLayout(layout);

	Label wait0 = new Label(shell, SWT.NONE);
    wait0.setText("----------------------------------");

	Label wait1 = new Label(shell, SWT.NONE);
    wait1.setText("Please Wait, Loading MIST Data.");
    
	Label wait2 = new Label(shell, SWT.NONE);
    wait2.setText("----------------------------------");

    //ProgressBar pb1 = new ProgressBar(shell, SWT.INDETERMINATE);
    //pb1.setSelection(100);
    

	//shell.setSize(250 , 250);
	shell.pack();
    shell.open();


    // Set up the event loop.
//    while (!shell.isDisposed()) {
//      if (!display.readAndDispatch()) {
//        // If no more entries in event queue
//        display.sleep();
//      }
//    }

//    display.dispose();

    return 1;
  }

  public void update(){
  	display.readAndDispatch();
  }

}