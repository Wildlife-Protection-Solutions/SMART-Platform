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
package org.wcs.smart.conversion.csv;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.conversion.csv.ui.CsvMatcherDialog;
import org.wcs.smart.conversion.util.ConnectionUtil;

/**
 * Launcher for CSV to SMART matching tool
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class Csv2SmartMatcher {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		
		GridLayout layout = new GridLayout(1, true);
		shell.setLayout(layout);
	    
	    GridData gridData = new GridData(SWT.LEFT,SWT.TOP, false, false);
	    shell.setLayoutData(gridData);
		
	    Image img = new Image(display, ClassLoader.getSystemClassLoader().getSystemResourceAsStream("csvsmart16.gif"));
	    shell.setImage(img);
		shell.setText("CSV to SMART - Conversion Tool");
		new CsvMatcherDialog(shell);

		shell.pack();
		
		Point size = shell.computeSize(-1, -1);
        shell.setBounds(120, 50, (int)(size.x*1.5), size.y);    
        
		shell.open();
		
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose();
		ConnectionUtil.closeConnection();
	}
}
