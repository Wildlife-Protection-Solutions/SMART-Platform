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
package org.wcs.smart.ui;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;


/**
 * Inactivity timeout manager
 * 
 * @author Emily
 *
 */
//https://www.eclipse.org/forums/index.php/t/1074589/
public enum InactivityTimeoutHandler implements Listener{


	INSTANCE;
	
	public static final String SMART_INACTIVITY_PROPERTIES_FILE = "smart_security.properties"; //$NON-NLS-1$
	public static final String PROP_INACTIVITY_ENABLED = "isenabled"; //$NON-NLS-1$
	public static final String PROP_INACTIVITY_TIMEOUT = "timeout"; //$NON-NLS-1$
	
    private long lastTime;
    
    private long offsetTime;
    
    private InactivityTimeoutHandler() {
    	Display.getDefault().addFilter(SWT.KeyDown, this);
    	Display.getDefault().addFilter(SWT.KeyUp, this);
    	Display.getDefault().addFilter(SWT.MouseDown, this);
    	Display.getDefault().addFilter(SWT.MouseUp, this);
    	Display.getDefault().addFilter(SWT.MouseDoubleClick, this);
    	Display.getDefault().addFilter(SWT.Move, this);
    	Display.getDefault().addFilter(SWT.Resize, this);
    	Display.getDefault().addFilter(SWT.Activate, this);
    	Display.getDefault().addFilter(SWT.MouseVerticalWheel, this);
    	Display.getDefault().addFilter(SWT.MouseHorizontalWheel, this);

    	lastTime = System.currentTimeMillis();
    }

	
	/**
	 * Reads the properties file for auto-backup config
	 * 
	 * @return a Properties object 
	 * @throws IOException if file not found
	 */
	public Properties getProperties(){
		Properties properties = new Properties();
		try {
			Path location = Paths.get(SmartProperties.getInstance().getProperty(SmartProperties.PROP_FILESTORE))
					.resolve(SMART_INACTIVITY_PROPERTIES_FILE);
			
			if (!Files.exists(location)){
				properties.setProperty(PROP_INACTIVITY_TIMEOUT, "5"); //$NON-NLS-1$
				properties.setProperty(PROP_INACTIVITY_ENABLED, Boolean.FALSE.toString());
				return properties;
			}
			try(InputStream fis = Files.newInputStream(location)){
				properties.load(fis);
			}
		} catch (IOException e) {
			SmartPlugIn.log("Error reading security settings" + "\n\n" + e.getLocalizedMessage(), e); //$NON-NLS-1$ //$NON-NLS-2$
			return properties;
		}
		return properties;
	}
	public Properties setProperties(Properties properties){
		
		try {
			Path location = Paths.get(SmartProperties.getInstance().getProperty(SmartProperties.PROP_FILESTORE))
					.resolve(SMART_INACTIVITY_PROPERTIES_FILE);
			try(OutputStream fout = Files.newOutputStream(location)){
				properties.store(fout, null);
			}
		} catch (IOException e) {
			SmartPlugIn.log("Error reading security settings" + "\n\n" + e.getLocalizedMessage(), e); //$NON-NLS-1$ //$NON-NLS-2$
			return properties;
		}
		return properties;
	}
    public void reset() {
    	Integer minutes = 5;
    	try {
    		minutes = Integer.valueOf(getProperties().getProperty(PROP_INACTIVITY_TIMEOUT));
    	}catch (Exception ex) {
    		
    	}
    	offsetTime = minutes * 60 * 1000;
    	if (Boolean.valueOf(getProperties().getProperty(PROP_INACTIVITY_ENABLED))){
    		lastTime = System.currentTimeMillis();
    		checkActivityJob.schedule(60 * 1000);
    	}else {
    		checkActivityJob.cancel();	
    	}
    }

	@Override
	public void handleEvent(Event event) {
		lastTime = System.currentTimeMillis();
	}
	
	private Job checkActivityJob = new Job("check activity") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Long now = (new Date()).getTime();
			Long diff = now - lastTime;
			if (diff > offsetTime) {				
				Display.getDefault().syncExec(()->{					
					List<Shell> temp = new ArrayList<>();
					for (Shell sh : Display.getDefault().getShells()) {
						if (!sh.isVisible()) continue;
						Shell s = new Shell(sh, SWT.NO_TRIM);
						s.setLayout(new FillLayout());
						s.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
						
						Canvas c = new Canvas(s, SWT.BORDER);
						c.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_TRANSPARENT));
						
						FontData fd = c.getFont().getFontData()[0];
						fd.height = fd.height *2;
						fd.setStyle(SWT.BOLD);
						Font f = new Font(c.getDisplay(), fd);
						
						final Color blue = new Color(c.getDisplay(), 50, 74,115);
						c.setForeground(blue);
						c.setFont(f);
						Transform tr = new Transform(c.getDisplay());
						tr.rotate(-30);
						
						c.addListener(SWT.Dispose, e->{
							blue.dispose();
							f.dispose();
							tr.dispose();
							Image img = (Image)c.getData();
							if (img != null) img.dispose();
						});
						
						
						c.addListener(SWT.Paint, e->{
							
							if (c.getData() == null) {
								int size = Math.min(c.getBounds().width, c.getBounds().height)-150;
								Image img = SmartUtils.getSvgLogoNoText(c.getDisplay(), size);
								c.setData(img);
							}
							
							Image img = (Image)c.getData();
							e.gc.setBackground(c.getDisplay().getSystemColor(SWT.COLOR_WHITE));
							
							int xoff = (int)((c.getBounds().width - img.getBounds().width) / 2.0);
							int yoff = (int)((c.getBounds().height - img.getBounds().height) / 2.0);
							if (xoff < 0) xoff = 0;
							if (yoff < 0) yoff = 0;
							
							e.gc.drawImage(img, 0, 0, img.getBounds().width, img.getBounds().height, xoff+1,yoff+1,img.getBounds().width-2, img.getBounds().height-2);		
							e.gc.setFont(f);

							String txt = Messages.InactivityTimeoutHandler_InactiveTimeoutText;
							Point pnt = e.gc.textExtent(txt);
							int x = (int)((c.getBounds().width - pnt.x) / 2.0);
							e.gc.drawText(txt, x,  c.getBounds().height - pnt.y - 10);
						});
						
						Rectangle bb = sh.getBounds();
						s.setSize(bb.width, bb.height);
						s.setLocation(sh.getLocation());
						temp.add(s);
						
					}
					
					temp.forEach(tmp->tmp.open());
					Shell parent = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					LoginDialog loginShell = new LoginDialog(parent, temp, this);
					loginShell.open();
				});
				
			}else {				
				schedule(1000);
			}
			return Status.OK_STATUS;
		}};
		
	
	public class LoginDialog extends UserNamePasswordDialog{

		private List<Shell> temp;
		private Job j;
		public LoginDialog(Shell parentShell, List<Shell> shells, Job j) {
			super(parentShell, Messages.InactivityTimeoutHandler_LoginDialogTitle, Messages.InactivityTimeoutHandler_LoginDialogMessage,
					Messages.InactivityTimeoutHandler_LoginButton);
			setShellStyle(SWT.TITLE | SWT.APPLICATION_MODAL);
			this.temp = shells;
			this.j = j;
		}
		
		@Override
		protected Point getInitialLocation(Point initialSize) {
			Shell parent = getParentShell();
			if (parent == null) {
				return 	super.getInitialLocation(initialSize);	
			}
			Point pnt = parent.getLocation();
			
			Rectangle b2 = parent.getBounds();
			
			int x = pnt.x + (int)((b2.width - initialSize.x) / 2.0);
			int y = pnt.y + (int)((b2.height - initialSize.y) / 2.0);
			
			return new Point(x,y);
			
		}
		protected Control createDialogArea(Composite parent) {
			
			Control c = super.createDialogArea(parent);
			
			super.txtUsername.setEditable(false);
			super.txtUsername.setEnabled(false);
			
			super.txtUsername.setText(SmartDB.getCurrentEmployee().getSmartUserId());
			return c;
		}
		
		@Override
		protected Control createButtonBar(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(4, false));
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			// create OK and Cancel buttons by default
			
			Link h = new Link(composite,  SWT.NONE);
			h.setText("<a>" + Messages.InactivityTimeoutHandler_LogoutButton + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
			h.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
			h.addListener(SWT.Selection, e->{
				if (!MessageDialog.openQuestion(getShell(), Messages.InactivityTimeoutHandler_LogoutTitle, Messages.InactivityTimeoutHandler_LogoutMessage)) return;
				for (IWorkbenchWindow ww : PlatformUI.getWorkbench().getWorkbenchWindows()) {
					for (IWorkbenchPage pp : ww.getPages()) {
						pp.closeAllEditors(false);
					}
				}
				PlatformUI.getWorkbench().restart();
			});
			
			h = new Link(composite,  SWT.NONE);
			h.setText("<a>" + Messages.InactivityTimeoutHandler_ShutdownButton + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
			h.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
			h.addListener(SWT.Selection, e->{
				if (!MessageDialog.openQuestion(getShell(), Messages.InactivityTimeoutHandler_ShutdownTitle, Messages.InactivityTimeoutHandler_ShutdownMessage)) return;
				for (IWorkbenchWindow ww : PlatformUI.getWorkbench().getWorkbenchWindows()) {
					for (IWorkbenchPage pp : ww.getPages()) {
						pp.closeAllEditors(false);
					}
				}
				PlatformUI.getWorkbench().close();
			});
			
			
			Label l = new Label(composite, SWT.NONE);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			createButton(composite, IDialogConstants.OK_ID, okText, true);

			return composite;
		}

		@Override
		public void okPressed() {
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			if (HibernateManager.validatePassword(txtPassword.getText(), SmartDB.getCurrentEmployee())) {
				temp.forEach(tmp->tmp.dispose());
				j.schedule(1000);
				super.okPressed();
			}else {
				MessageDialog.openError(getShell(), Messages.InactivityTimeoutHandler_InvalidPasswordTitle, Messages.InactivityTimeoutHandler_InvalidPasswordMsg);
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}			
		}
		
	}
}
