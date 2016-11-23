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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Abstract class for shell dialog. 
 * 
 * @author Emily
 *
 */
public abstract class SmartShellDialog implements Listener {

	protected Shell shell;
	protected Shell hiddenParent;
	protected Shell parentShell;
	
	public SmartShellDialog(Shell parentShell) {
		this(parentShell, SWT.NO_TRIM);
	}
	
	public SmartShellDialog(Shell parentShell, int style) {
		this.parentShell = parentShell;
		hiddenParent = new Shell(parentShell.getDisplay());

		shell = new Shell(hiddenParent, style);

		shell.setLayout(new GridLayout());
		((GridLayout) shell.getLayout()).marginWidth = 0;
		((GridLayout) shell.getLayout()).marginHeight = 0;
		shell.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

		shell.addListener(SWT.Dispose, this);
		shell.addListener(SWT.Deactivate, this);		
		
		shell.setSize(400, 200);
	}
	

	public abstract void createContents(Composite parent);
	
	public void close(){
		if (shell == null || shell.isDisposed()) return;
		shell.close();
	}
	
	public void open(Point position){
		Point sizea = shell.getSize();
		createContents(shell);
		Point sizeb = shell.getSize();
		int y = sizea.y - sizeb.y + position.y;
		int x = sizea.x - sizeb.x + position.x;
		shell.setLocation(x, y);
		shell.open();
	}
	
	public void addListener(int eventType, Listener listener){
		shell.addListener(eventType, listener);
	}
	
	public Point getSize(){
		return shell.getSize();
	}
	public boolean isDisposed(){
		return shell == null || shell.isDisposed();
	}

	@Override
	public void handleEvent(Event event) {
		if (event.type == SWT.Dispose) {
			hiddenParent.dispose();
			return;
		}
		if (event.type == SWT.Deactivate) {
			shell.close();
			return;
		}
	}

}
