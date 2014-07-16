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
package org.wcs.smart.intelligence.ui.panel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.common.attachment.AttachmentComposite;
import org.wcs.smart.common.attachment.IAttachmentsChangeListener;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligenceAttachment;

/**
 * Composite for collecting the intelligence attachments information
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceAttachmentsComposite extends IntelligenceComposite implements IAttachmentsChangeListener {

	private AttachmentComposite<IntelligenceAttachment> attachmentComposite;

	/**
	 * @param parent
	 * @param style
	 */
	public IntelligenceAttachmentsComposite(Composite parent, int style) {
		super(parent, style);
		setMessage(Messages.IntelligenceAttachments_Message);
		createControls();
	}

	private void createControls() {
        this.setLayout(new GridLayout(1, false));
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        attachmentComposite = new AttachmentComposite<IntelligenceAttachment>(this, SWT.NONE) {
			@Override
			protected IntelligenceAttachment createNewAttachement() {
				return new IntelligenceAttachment();
			}
		};
		attachmentComposite.addAttachmentsChangeListener(this);
	}

	@Override
	protected void updateModelInternal(Intelligence intelligence) {
		//create a copy of attachments array 
		//(we don't want to remove from original array as this will effect gui)
		List<IntelligenceAttachment> attachments = new ArrayList<IntelligenceAttachment>(attachmentComposite.getAttchments());
		//Update the attachments
		if (intelligence.getAttachments() == null) {
			intelligence.setAttachments(new ArrayList<IntelligenceAttachment>());
		}
		
		for (Iterator<IntelligenceAttachment> iterator = intelligence.getAttachments().iterator(); iterator.hasNext();) {
			IntelligenceAttachment att = iterator.next();
			if (!attachments.remove(att)){
				iterator.remove();
			}
		}
		
		//add remaining; these should all be new attachments
		for (Iterator<IntelligenceAttachment> iterator = attachments.iterator(); iterator.hasNext();) {
			IntelligenceAttachment att = iterator.next();
			att.setIntelligence(intelligence);
			intelligence.getAttachments().add(att);
		}
	}

	@Override
	public void initFromModel(Intelligence intelligence) {		
		attachmentComposite.getAttchments().clear();
		attachmentComposite.initAttachments(intelligence.getAttachments());
	}

	@Override
	public void attachmentsChanged() {
		fireInputChangeListeners();
	}

}
