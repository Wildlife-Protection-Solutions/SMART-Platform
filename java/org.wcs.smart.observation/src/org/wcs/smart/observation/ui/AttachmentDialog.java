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

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.common.attachment.AttachmentComposite;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;

/**
 * Dialog for displaying attachments associated 
 * with a given waypoint.
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
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite)super.createDialogArea(parent);
		
		attachmentComposite = new AttachmentComposite<WaypointAttachment>(composite, SWT.NONE) {
			@Override
			protected WaypointAttachment createNewAttachement() {
				return new WaypointAttachment();
			}
		};
		if (waypoint.getAttachments() != null){
			attachmentComposite.initAttachments(waypoint.getAttachments());
		}
		
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
