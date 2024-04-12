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

import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.intelligence.internal.Messages;


/**
 * IntelligenceCompositeFactory
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceCompositeFactory {

	private static IntelligenceCompositeFactory instance;
	
	private IntelligenceCompositeFactory() {}

	public static IntelligenceCompositeFactory getInstance() {
		if (instance == null) {
			instance = new IntelligenceCompositeFactory();
		}
		return instance;
	}

	public IntelligenceComposite createComposite(Composite parent, int style, PanelType type) {
		switch (type) {
		case RECIEVED:    return new IntelligenceReceivedComposite(parent, style);
		case SOURCE:      return new IntelligenceSourceComposite(parent, style);
		case DATES:       return new IntelligenceDatesComposite(parent, style);
		case DESCRIPTION: return new IntelligenceDescComposite(parent, style);
		case LOCATION:    return new IntelligenceLocationComposite(parent, style);
		case ATTACHMENTS: return new IntelligenceAttachmentsComposite(parent, style);
		default: throw new UnsupportedOperationException(type + "is not supported"); //$NON-NLS-1$
		}
	}

	public String getTitle(PanelType type) {
		switch (type) {
		case RECIEVED:    return Messages.IntelligenceReceived_PageTitle;
		case SOURCE:      return Messages.IntelligenceSource_PageTitle;
		case DATES:       return Messages.IntelligenceDates_PageTitle;
		case DESCRIPTION: return Messages.IntelligenceDesc_PageTitle;
		case LOCATION:    return Messages.IntelligenceLocation_PageTitle;
		case ATTACHMENTS: return Messages.IntelligenceAttachments_PageTitle;
		default: throw new UnsupportedOperationException(type + "is not supported"); //$NON-NLS-1$
		}
	}

	/**
	 * The supported panels.
	 */
	public enum PanelType {
		RECIEVED,
		SOURCE,
		DATES,
		DESCRIPTION,
		LOCATION,
		ATTACHMENTS;
	}

}
