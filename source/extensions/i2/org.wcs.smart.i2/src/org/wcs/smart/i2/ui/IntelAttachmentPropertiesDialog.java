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
package org.wcs.smart.i2.ui;


import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.ui.AttachmentPropertiesDialog;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Extension of attachment properties to include intel specific items
 * @author Emily
 *
 */
public class IntelAttachmentPropertiesDialog extends AttachmentPropertiesDialog{
	
	public IntelAttachmentPropertiesDialog(Shell parent, ISmartAttachment attachment) {
		super(parent, attachment);
	}

	@Override
	protected List<Entry> findAdditionalDetails(ISmartAttachment attachment){
		IntelAttachment a = (IntelAttachment)attachment;
		
		List<Entry> details = new ArrayList<>();
		details.add(new Entry(Messages.AttachmentPropertiesDialog_CreatedByLabel,
				a.getCreatedBy() == null ? "" : SmartLabelProvider.getShortLabel(a.getCreatedBy()))); //$NON-NLS-1$
		details.add(new Entry(Messages.AttachmentPropertiesDialog_CreatedOnLabel, 
				DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(a.getDateCreated())));
		return details;
	}

}
