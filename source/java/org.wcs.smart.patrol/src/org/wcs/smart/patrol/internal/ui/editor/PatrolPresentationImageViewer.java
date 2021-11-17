/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.patrol.internal.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.ui.Thumbnail;

import com.ibm.icu.text.MessageFormat;

/**
 * Image viewer for patrol presentation view 
 * 
 * @author Emily
 *
 */
public class PatrolPresentationImageViewer extends Composite {

	public PatrolPresentationImageViewer(Composite parent, int style) {
		super(parent, style);
		createControl();
	}

	private Waypoint wpsource;
	private WaypointObservation obssource;
	private Composite image = null;
	
	private Label lblBack, lblNext, lblInfo;
	
	private List<ISmartAttachment> attachments;
	
	private int currentIndex = -1;
	
	public void setSource(Object source) {
		this.wpsource = null;
		this.obssource = null;
		this.attachments = new ArrayList<>();
		
		if (source instanceof Waypoint) {
			this.wpsource = (Waypoint)source;
		}else if (source instanceof WaypointObservation) {
			this.obssource = (WaypointObservation)source;	
		}
		updateControls();
		loadAttachments.schedule();
	}
	
	private void updateImage() {
		if (currentIndex == -1) return;
		
		for(Control c : image.getChildren()) c.dispose();
		int size = Math.min(image.getSize().x, image.getSize().y);
		Thumbnail thub = new Thumbnail(attachments.get(currentIndex), size, true);
		Composite c = thub.createThumbnail(image, SWT.NONE);
		c.setLocation((image.getSize().x - size) / 2, (image.getSize().y - size) / 2);
	}
	
	
	private void nextImage() {
		currentIndex ++;
		if (currentIndex >= attachments.size()) currentIndex = attachments.size() - 1;
		updateControls();
		updateImage();
	}
	
	private void previousImage() {
		currentIndex --;
		if (currentIndex < 0) currentIndex = 0;
		updateControls();
		updateImage();
		
	}
	
	private void updateControls() {
		lblBack.setEnabled(currentIndex > 0);
		lblNext.setEnabled(currentIndex < attachments.size() -1);
		lblInfo.setText(MessageFormat.format("Attachment {0} of {1}", currentIndex+1, attachments.size()));
	}
	
	private void createControl() {
		setLayout(new GridLayout(3, false));

		lblInfo = new Label(this, SWT.NONE);
		lblInfo.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, 3, 1));
		
		lblBack = new Label(this, SWT.NONE);
		lblBack.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.BROWSER_BACKWARD64));
		lblBack.addListener(SWT.MouseEnter, e->getShell().setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND)));
		lblBack.addListener(SWT.MouseExit, e->getShell().setCursor(null));
		lblBack.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));
		lblBack.addListener(SWT.MouseUp, e->previousImage());
		
		image = new Composite(this, SWT.BORDER);
		image.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		image.addListener(SWT.Resize, e->updateImage());
		
		lblNext = new Label(this, SWT.NONE);
		lblNext.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.BROWSER_FORWARD64));
		lblNext.addListener(SWT.MouseEnter, e->getShell().setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND)));
		lblNext.addListener(SWT.MouseExit, e->getShell().setCursor(null));
		lblNext.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, true));
		lblNext.addListener(SWT.MouseUp, e->nextImage());
	
	}
	
	Job loadAttachments = new Job("loading attachments") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try(Session session = HibernateManager.openSession()){
				if (wpsource != null) {
					Waypoint wp = session.get(Waypoint.class, wpsource.getUuid());
					attachments.addAll(wp.getAttachments());
					for (WaypointObservation wo : wp.getAllObservations()) {
						attachments.addAll(wo.getAttachments());
					}
				}
				if (obssource != null) {
					WaypointObservation wo = session.get(WaypointObservation.class, obssource.getUuid());
					attachments.addAll(wo.getAttachments());
					//TODO: figure out what to do here 
					attachments.addAll(wo.getWaypoint().getAttachments());
					
				}
				
				for (ISmartAttachment a : attachments) {
					try {
						a.computeFileLocation(session);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
			getDisplay().syncExec(()->{
				for(Control c : image.getChildren()) c.dispose();
				
				if (attachments.isEmpty()) {
					currentIndex = -1;
					lblBack.setEnabled(false);
					lblNext.setEnabled(false);
					lblInfo.setText("");
					
					Label l = new Label(image, SWT.NONE);
					l.setText("No Attachments");
					l.setSize(l.computeSize(SWT.DEFAULT, SWT.DEFAULT));
					l.setLocation((image.getSize().x - l.getSize().x) / 2, (image.getSize().y - l.getSize().y) / 2 );
				}else {
					currentIndex = 0;
					updateControls();
					updateImage();
				}
				layout(true);
			});
			return Status.OK_STATUS;
		}
		
	};
}
