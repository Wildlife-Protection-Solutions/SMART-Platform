/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Displays a message on the screen; the disposes of the message when the user clicks somewhere.
 * 
 * @author Emily
 *
 */
public class TransparentInfoDialog  {
	
	Shell shell;

	public TransparentInfoDialog(Shell parent, String message) {
		shell = new Shell(parent.getDisplay(), SWT.NO_TRIM);
		Rectangle size = populate(message);
		
		Rectangle bnds = parent.getBounds();
		int x = (bnds.width / 2 ) - (size.width / 2) + bnds.x;
		int y = (bnds.height / 2 ) - (size.height / 2) + bnds.y;
		
		shell.setLocation(x, y);
	}

	public void open(){	
		shell.open();
	}
	
	  
	private Rectangle populate(String message) {
		Label l = new Label(shell, SWT.NONE);
		l.setText(message);
		l.pack();		
		l.setLocation(10, 10);
		Point size = l.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		
		Rectangle shellSize = new Rectangle(0, 0, size.x + 20, size.y + 20);
		
		Region region = new Region();
		region.add(shellSize);
		region.subtract(0, 0, 1, 1);
		region.subtract(shellSize.width-1, 0, 1, 1);
		region.subtract(0, shellSize.height-1, 1, 1);
		region.subtract(shellSize.width-1, shellSize.height-1, 1, 1);
		
		shell.setRegion(region);
		shell.setSize(shellSize.width, shellSize.height);
		
		shell.addListener(SWT.Paint, (e)->{
			e.gc.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
			e.gc.fillRoundRectangle(0, 0, shellSize.width-1, shellSize.height-1, 9, 9);
			
			e.gc.setForeground(shell.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
			e.gc.drawRoundRectangle(0, 0, shellSize.width-1, shellSize.height-1, 9, 9);
		});
		
		shell.addListener(SWT.FocusOut, e->{
			shell.setVisible(false);
			Display.getDefault().asyncExec(()->shell.close());	
		});
		
		
		shell.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		l.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		
		return shellSize;
	}
}
