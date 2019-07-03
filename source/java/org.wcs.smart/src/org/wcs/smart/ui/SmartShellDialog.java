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
package org.wcs.smart.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
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
		this(parentShell, SWT.NO_TRIM | SWT.ON_TOP);
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
	
	/**
	 * Opens the shell, providing additional features if the shell
	 * ends up posited right to left (for menus that run off
	 * the screen).  See ticket #2781. 
	 * 
	 * @param position1 display position of the upper left corner of the shell
	 * @param position2 the amount to shift the start point by if the shell 
	 * ends up getting shifted left to right or top to bottom
	 * @param flipr2l if the position of the direct children of the
	 * shell should be flipped if the shell gets posited right to left 
	 */
	public void open(Point position1, Point position2, boolean flipr2l){
		createContents(shell);
	
		int x = position1.x;
		int y = position1.y;
		
		Monitor on = shell.getMonitor();
		for (Monitor m : Display.getDefault().getMonitors()) {
			if (m.getBounds().x <= x && m.getBounds().x + m.getBounds().width >= x && 
					m.getBounds().y <= y && m.getBounds().y + m.getBounds().height >= y) {
				on = m;
				break;
			}
		}
		int dx = x - on.getBounds().x;
		int width = on.getBounds().width;
		if (dx + shell.getSize().x > width) {
			x = x - shell.getSize().x - position2.x;;
			
			if (flipr2l) {
				//flip the order of the widgets in the core
				for (int i = 0; i < shell.getChildren().length-1; i ++) {
					shell.getChildren()[i].moveBelow(shell.getChildren()[i+1]);
				}
			}
		}
		
		int dy = y - on.getBounds().y;
		int height = on.getBounds().height;
		if (dy + shell.getSize().y > height) {
			y = y - shell.getSize().y - position2.y;
		}
		
		shell.setLocation(x, y);
		shell.open();
	}
	
	/**
	 * Display the shell at the given point.  By default this is the top left corner 
	 * of the shell, but if the shell runs off 
	 * the right of the monitor, the shell will be positioned to the left of this point.
	 * Same if the shell runs off the bottom of the monior the shell will be position 
	 * at the above this point
	 * 
	 * @param position
	 */
	public void open(Point position){

		createContents(shell);

		int x = position.x;
		int y = position.y;
		
		//figure out which monitor this control is on
		Monitor on = shell.getMonitor();
		for (Monitor m : Display.getDefault().getMonitors()) {
			if (m.getBounds().x <= x && m.getBounds().x + m.getBounds().width >= x && 
					m.getBounds().y <= y && m.getBounds().y + m.getBounds().height >= y) {
				on = m;
				break;
			}
		}
	
		int dx = x - on.getBounds().x;
		int width = on.getBounds().width;
		if (dx + shell.getSize().x > width) {
			x = x - shell.getSize().x;
		}
		
		int dy = y - on.getBounds().y;
		int height = on.getBounds().height;
		if (dy + shell.getSize().y > height) {
			y = y - shell.getSize().y;
		}
		
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
