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
package org.wcs.smart.ct2smart.ui;

import java.io.FileOutputStream;
import java.io.PrintStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class Ct2SmartMatcher {

	public static void main(String[] args) throws Exception {
		if (args.length > 0) {
			FileOutputStream fout = new FileOutputStream(args[0]);
			FileOutputStream ferr = (args.length > 1) ? new FileOutputStream(args[1]) : fout;
			
			MultiOutputStream multiOut = new MultiOutputStream(System.out, fout);
			MultiOutputStream multiErr = new MultiOutputStream(System.err, ferr);
			
			PrintStream stdout = new PrintStream(multiOut);
			PrintStream stderr = new PrintStream(multiErr);
			
			System.setOut(stdout);
			System.setErr(stderr);
		}
		
		Display display = new Display();
		Shell shell = new Shell(display);
		
		Point size = shell.computeSize(-1, -1);
        shell.setBounds(50, 50, size.x, size.y);    
		
		
		GridLayout layout = new GridLayout(1, true);
		shell.setLayout(layout);
	    
	    GridData gridData = new GridData(SWT.LEFT,SWT.TOP, false, false);
	    shell.setLayoutData(gridData);
		
		shell.setText("CyberTracker to SMART - Data Model Matcher");
		new SourceDialog(shell);

		shell.pack();
		shell.open();
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose();
	}

}
