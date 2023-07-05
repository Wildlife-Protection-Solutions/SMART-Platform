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

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.ui.AttachmentPropertiesDialog;
import org.wcs.smart.ui.AttachmentViewDialog;
import org.wcs.smart.ui.Thumbnail;


/**
 * Image viewer for patrol presentation view 
 * 
 * @author Emily
 *
 */
public class PatrolPresentationImageViewer extends Composite {

	private Waypoint wpsource;
	private WaypointObservation obssource;
	private Composite image = null;
	
	private Label lblBack, lblNext, lblInfo;
	private ToolItem lblExpand;
		
	private List<ISmartAttachment> attachments;
	
	private int currentIndex = -1;

	
	public PatrolPresentationImageViewer(Composite parent, int style) {
		super(parent, style);
		createControl();
	}

	/**
	 * Sets the current source of images - 
	 * can either be Waypoint or WaypointObservation.
	 * 
	 * @param source
	 */
	public void setSource(Object source) {
		this.wpsource = null;
		this.obssource = null;
		this.attachments = new ArrayList<>();
		this.currentIndex = -1;
		
		if (source instanceof Waypoint) {
			this.wpsource = (Waypoint)source;
		}else if (source instanceof WaypointObservation) {
			this.obssource = (WaypointObservation)source;	
		}
		updateControls();
		loadAttachments.schedule();
	}
	
	private void expandImage() {
		if (currentIndex >= 0) {
			ISmartAttachment attachment = attachments.get(currentIndex);
			Image rawImage = null;
			
			try {
				Path temp = EncryptUtils.decryptAttachment(attachment);
				try {
					rawImage = new Image(Display.getDefault(), temp.toString());
				}finally {
					if (temp != null) {
						try {
							Files.delete(temp);
						}catch (Exception ex) {}
					}
				}
			}catch (Exception ex) {
				//eatme
				rawImage = null;
			}
			if (rawImage == null) {
				MessageDialog.openInformation(getShell(), Messages.PatrolPresentationImageViewer_InvalidDialogTitle, Messages.PatrolPresentationImageViewer_InvalidDialogMessage);
				return;
			}
			AttachmentViewDialog dialog = new AttachmentViewDialog(getShell(), attachment, rawImage);
			dialog.open();
		}
	}
	

	
	private void updateImage() {
		if (currentIndex == -1) return;
		
		for(Control c : image.getChildren()) c.dispose();
		int size = Math.min(image.getSize().x, image.getSize().y);
		Thumbnail thub = new Thumbnail(attachments.get(currentIndex), size, true);
		Composite c = thub.createThumbnail(image, SWT.NONE);
		c.setLocation((image.getSize().x - size) / 2, (image.getSize().y - size) / 2);
		
		Menu attachmentMenu = new Menu(c);
		c.setMenu(attachmentMenu);
		
		MenuItem mnuOpen = new MenuItem(attachmentMenu, SWT.PUSH);
		mnuOpen.setText(Messages.PatrolPresentationImageViewer_ExpandMenuItem);
		mnuOpen.addListener(SWT.Selection, e->expandImage());
		
		MenuItem mnuOpenSystem = new MenuItem(attachmentMenu, SWT.PUSH);
		mnuOpenSystem.setText(Messages.PatrolPresentationImageViewer_OpenSystemMenuItem);
		mnuOpenSystem.addListener(SWT.Selection, e->{
			if (currentIndex >= 0) {
				AttachmentUtil.openAttachment(attachments.get(currentIndex));
			}
		});
		
		new MenuItem(attachmentMenu, SWT.SEPARATOR);
		
		MenuItem mnuDetails = new MenuItem(attachmentMenu, SWT.PUSH);
		mnuDetails.setText(Messages.PatrolPresentationImageViewer_PropertiesMenuItem);
		mnuDetails.addListener(SWT.Selection, e->{
			if (currentIndex >= 0) {
				AttachmentPropertiesDialog dialog = new AttachmentPropertiesDialog(getShell(), attachments.get(currentIndex));
				dialog.open();
			}
		});
		
		mnuDetails.setEnabled(currentIndex >= 0);
		mnuOpenSystem.setEnabled(currentIndex >= 0);
		mnuOpen.setEnabled(lblExpand.getEnabled());
		
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
		lblInfo.setText(MessageFormat.format(Messages.PatrolPresentationImageViewer_AttachmentsSummary, currentIndex+1, attachments.size()));
		
		boolean isImage = false;
		if (currentIndex >= 0 && !attachments.isEmpty()) {
			ISmartAttachment a = attachments.get(currentIndex);
			try {
				if (Files.probeContentType(a.getAttachmentFile()).startsWith("image")) { //$NON-NLS-1$
					isImage = true;
				}
			}catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		lblExpand.setEnabled(isImage);
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
		lblBack.setToolTipText(Messages.PatrolPresentationImageViewer_previousTooltip);
		
		image = new Composite(this, SWT.BORDER);
		image.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		image.addListener(SWT.Resize, e->updateImage());
		
		Composite temp = new Composite(this, SWT.NONE);
		temp.setLayout(new GridLayout());
		temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		((GridLayout)temp.getLayout()).marginWidth = 0;
		((GridLayout)temp.getLayout()).marginHeight = 0;
		((GridLayout)temp.getLayout()).verticalSpacing = 0;
		
		lblNext = new Label(temp, SWT.NONE);
		lblNext.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.BROWSER_FORWARD64));
		lblNext.addListener(SWT.MouseEnter, e->getShell().setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND)));
		lblNext.addListener(SWT.MouseExit, e->getShell().setCursor(null));
		lblNext.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, true));
		lblNext.addListener(SWT.MouseUp, e->nextImage());
		lblNext.setToolTipText(Messages.PatrolPresentationImageViewer_nextTooltip);
		((GridData)lblNext.getLayoutData()).verticalIndent = 21;
		
		ToolBar tb = new ToolBar(temp, SWT.FLAT);
		tb.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));
		
		lblExpand = new ToolItem(tb, SWT.PUSH);
		lblExpand.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ICON_EXPAND));
		lblExpand.addListener(SWT.Selection, e->expandImage());
		lblExpand.setToolTipText(Messages.PatrolPresentationImageViewer_exapndTooltip);
		
	}
	
	Job loadAttachments = new Job(Messages.PatrolPresentationImageViewer_loadingjobname) {

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
					attachments.addAll(wo.getWaypoint().getAttachments());
					
				}
				
				for (ISmartAttachment a : attachments) {
					try {
						a.computeFileLocation(session);
					} catch (Exception e) {
						SmartPatrolPlugIn.log(e.getMessage(),e);
					}
				}
			}
			
			getDisplay().syncExec(()->{
				for(Control c : image.getChildren()) c.dispose();
				
				if (attachments.isEmpty()) {
					currentIndex = -1;
					lblBack.setEnabled(false);
					lblNext.setEnabled(false);
					lblInfo.setText(""); //$NON-NLS-1$
					if (obssource != null || wpsource != null) {
						Label l = new Label(image, SWT.NONE);
						l.setText(Messages.PatrolPresentationImageViewer_NoAttachmentsText);
						l.setSize(l.computeSize(SWT.DEFAULT, SWT.DEFAULT));
						l.setLocation((image.getSize().x - l.getSize().x) / 2, (image.getSize().y - l.getSize().y) / 2 );
					}

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
