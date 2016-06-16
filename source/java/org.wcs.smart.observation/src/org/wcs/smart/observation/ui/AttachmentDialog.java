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
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.common.attachment.AttachmentComposite;
import org.wcs.smart.common.attachment.IAttachmentsChangeListener;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
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
public class AttachmentDialog extends TitleAreaDialog {

	private Waypoint waypoint;
	private AttachmentComposite<WaypointAttachment> attachmentComposite;
	
	/**
	 * @param parentShell
	 */
	public AttachmentDialog(Shell parentShell, Waypoint wp) {
		super(parentShell);
		waypoint = wp;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite)super.createDialogArea(parent);
		
		attachmentComposite = new AttachmentComposite<WaypointAttachment>(composite, SWT.NONE) {
			@Override
			protected WaypointAttachment createNewAttachement() {
				return new WaypointAttachment();
			}
		};
		attachmentComposite.addAttachmentsChangeListener(new IAttachmentsChangeListener() {
			@Override
			public void attachmentsChanged() {
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
		});
		if (waypoint.getAttachments() != null){
			attachmentComposite.initAttachments(waypoint.getAttachments());
		}
		
		List<ISmartAttachment> obs = new ArrayList<ISmartAttachment>();
		if (waypoint.getObservations() != null) {
			for (WaypointObservation o : waypoint.getObservations()){
				if (o.getAttachments() != null){
					obs.addAll(o.getAttachments());
				}
			}
		}
		attachmentComposite.initOtherAttachments(obs);
		
		Composite c = new Composite(composite, SWT.NONE);
		c.setLayout(new GridLayout());
		
		Label l = new Label(c, SWT.NONE);
		l.setText(Messages.AttachmentDialog_ObservationAttachmentLbl);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		setMessage(Messages.AttachmentDialog_DialogMessage);
		getShell().setText(Messages.AttachmentDialog_DialogTitle);
		setTitle(Messages.AttachmentDialog_DialogTitle);
		return composite; 
	}

	/**
	 * @return all attachments selected by the user
	 */
	public List<WaypointAttachment> getAttchments() {
		if (attachmentComposite != null) {
			return attachmentComposite.getAttchments();
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
