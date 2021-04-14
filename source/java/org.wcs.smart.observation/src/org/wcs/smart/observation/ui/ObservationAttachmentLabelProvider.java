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
package org.wcs.smart.observation.ui;

import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.common.attachment.SmartAttachmentLabelProvider;
import org.wcs.smart.observation.model.WaypointAttachment;

/**
 * Label provider for observation attachments; adds signature
 * details if applicable to labels for waypoint attachments
 * 
 * @author Emily
 *
 */
public class ObservationAttachmentLabelProvider extends SmartAttachmentLabelProvider {
	
	public String getText(Object element) {
		if (element instanceof ISmartAttachment){
			StringBuilder sb = new StringBuilder();
			sb.append(super.getText(element));
			if (element instanceof WaypointAttachment) {
				WaypointAttachment wa = (WaypointAttachment)element;
				if (wa.getSignatureType() != null) {
					sb.append(" (Signature: " );
					sb.append(wa.getSignatureType().getName());
					sb.append(")");
				}
			}
			return sb.toString();
		}
		return super.getText(element);
	}
}
