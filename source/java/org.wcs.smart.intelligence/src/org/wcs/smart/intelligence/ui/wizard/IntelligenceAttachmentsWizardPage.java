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
package org.wcs.smart.intelligence.ui.wizard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.common.attachment.AttachmentComposite;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligenceAttachment;

/**
 * Intelligence Wizard page for collecting the intelligence attachments information
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceAttachmentsWizardPage extends IntelligenceWizardPage {

	private AttachmentComposite<IntelligenceAttachment> attachmentComposite;
	
	/**
	 * @param pageName
	 */
	public IntelligenceAttachmentsWizardPage() {
		super(Messages.IntelligenceAttachmentsWizardPage_PageTitle);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
        attachmentComposite = new AttachmentComposite<IntelligenceAttachment>(parent, SWT.NONE) {
			@Override
			protected ISmartAttachment createNewAttachement() {
				return new IntelligenceAttachment();
			}
		};

        setControl(attachmentComposite);
        setMessage(Messages.IntelligenceAttachmentsWizardPage_Message);
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.intelligence.ui.wizard.IntelligenceWizardPage#updateModel(org.wcs.smart.intelligence.model.Intelligence)
	 */
	@Override
	protected boolean updateModel(Intelligence intelligence) {
		//Update the attachments
		List<IntelligenceAttachment> attachments = attachmentComposite.getAttchments();
		if (intelligence.getAttachments() == null) {
			intelligence.setAttachments(new ArrayList<IntelligenceAttachment>());
		}
		
		for (Iterator<IntelligenceAttachment> iterator = intelligence.getAttachments().iterator(); iterator.hasNext();) {
			IntelligenceAttachment att = iterator.next();
			if (!attachments.remove(att)){
				iterator.remove();
			}
		}
		
		//add reminaing; these should all be new attachments
		for (Iterator<IntelligenceAttachment> iterator = attachments.iterator(); iterator.hasNext();) {
			IntelligenceAttachment att = iterator.next();
			att.setIntelligence(intelligence);
			intelligence.getAttachments().add(att);
		}
		return true;
	}

}
