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
package org.wcs.smart.observation.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.AttachmentTagManager;
import org.wcs.smart.ca.AttachmentTag;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.ui.input.AttachmentPreviewTagComposite;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for displaying attachments associated 
 * with a given waypoint.  This dialog displays
 * both waypoint attachments and attachments associated
 * with any of the waypoint observations.  However
 * only the waypoint attachments can be modified.  The others 
 * are view only
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttachmentDialog extends SmartStyledTitleDialog{

	private Waypoint waypoint;
	private List<ISmartAttachment> attachments;
	
	private Link moveLink;
	private boolean isMoved = false;
	private AttachmentPreviewTagComposite preview;
	
	/**
	 * @param parentShell
	 */
	public AttachmentDialog(Shell parentShell, Waypoint wp) {
		super(parentShell);
		this.waypoint = wp;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	private void addAttachment() {
		preview.addAttachment(()->new WaypointAttachment(), attachments);
		modified();
	}
	
	private void deleteAttachment() {
		preview.deleteAttachments(attachments);
		modified();
	}
	
	private void modified() {
		preview.refresh();
		moveLink.setEnabled(false);
		getButton(IDialogConstants.OK_ID).setEnabled(true);
	}
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite)super.createDialogArea(parent);
		
		Composite spacer = new Composite(composite, SWT.NONE);
		spacer.setLayout(new GridLayout());
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		List<AttachmentTag> tags = null;
		try(Session session = HibernateManager.openSession()){
			tags = AttachmentTagManager.INSTANCE.getTags(session, waypoint.getConservationArea());
		}
		
		preview = new AttachmentPreviewTagComposite(spacer,
				tags,
				e->addAttachment(),
				e->deleteAttachment(),
				a->(a instanceof WaypointAttachment));
		preview.addListener(SWT.Modify, e->modified());
		
		attachments = new ArrayList<>();
		attachments.addAll(waypoint.getAttachments());
		for (WaypointObservation o : waypoint.getAllObservations()) {
			if (o.getAttachments() != null) attachments.addAll(o.getAttachments());
		}
		preview.setInput(attachments);
		
		Composite c = new Composite(spacer, SWT.NONE);
		c.setLayout(new GridLayout());
		
		Label l = new Label(c, SWT.NONE);
		l.setText(Messages.AttachmentDialog_ObservationAttachmentLbl);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		
		moveLink = new Link(c,SWT.NONE);
		moveLink.setText("<a>" + Messages.AttachmentDialog_moveattachments + "</a>");  //$NON-NLS-1$ //$NON-NLS-2$
		moveLink.setToolTipText(Messages.AttachmentDialog_moveattachmentstooltip);
		moveLink.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		moveLink.addListener(SWT.Selection, e->{
			if (getButton(IDialogConstants.OK_ID).isEnabled()) {
				//shouldn't happen -must save changes before you can edit 
				return;
			}
			
			cancelPressed();
			if (getShell() != null) return;
			MoveAttachmentDialog d = new MoveAttachmentDialog(Display.getDefault().getActiveShell(), waypoint);
			if (d.open() == Window.OK) isMoved = true;
		});
		
		
		setMessage(Messages.AttachmentDialog_DialogMessage);
		getShell().setText(Messages.AttachmentDialog_DialogTitle);
		setTitle(Messages.AttachmentDialog_DialogTitle);
		return composite; 
	}

	public boolean hasMoved() {
		return this.isMoved;
	}
	
	/**
	 * @return all attachments selected by the user
	 */
	public List<WaypointAttachment> getAttchments() {
		if (this.attachments != null) {
			List<WaypointAttachment> ws = new ArrayList<>();
			for (ISmartAttachment a : attachments) {
				if (a instanceof WaypointAttachment wa) ws.add(wa);
			}
			return ws;
		}
		return Collections.emptyList();
	}

	/** dialog is resizable
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	protected boolean isResizable() {
		return true;
	}
}
