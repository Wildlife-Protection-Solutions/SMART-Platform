/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.wcs.smart.datamodelmatcher.ui;



import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
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

	//Label wait0 = new Label(shell, SWT.NONE);
    //wait0.setText("----------------------------------");

	Label wait1 = new Label(shell, SWT.NONE);
    wait1.setText("Please Wait, Loading MIST Data.");

    ProgressBar pb1 = new ProgressBar(shell, SWT.INDETERMINATE);
    pb1.setSelection(100);
    
	//Label wait2 = new Label(shell, SWT.NONE);
    //wait2.setText("----------------------------------");

    

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