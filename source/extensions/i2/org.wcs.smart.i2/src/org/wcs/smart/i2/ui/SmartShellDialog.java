package org.wcs.smart.i2.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public abstract class SmartShellDialog implements Listener {

	protected Shell shell;
	private Shell hiddenParent;

	public SmartShellDialog(Display ownerDisplay) {
		this(ownerDisplay, SWT.NO_TRIM);
	}
	
	public SmartShellDialog(Display ownerDisplay, int style) {

		hiddenParent = new Shell(ownerDisplay);

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
		if (shell.isDisposed() || shell == null) return;
		shell.close();
	}
	public void open(Point position){
		
		createContents(shell);
		shell.setLocation(position);
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
