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
package org.wcs.smart.query.common.engine;

import org.wcs.smart.common.attachment.ISmartAttachment;

/**
 * Query result item and include a single attachment.
 * 
 * The associated attachment object should have it's
 * filepath computed before setting (attachment.computeFileLocation(session)
 * should be called).
 * 
 * @author Emily
 *
 */
public class AttachmentResultItemImpl implements IAttachmentResultItem {

	private ISmartAttachment attachment;
	
	public AttachmentResultItemImpl(ISmartAttachment attachment) {
		setAttachment(attachment);
	}
	
	@Override
	public ISmartAttachment getAttachment() {
		return this.attachment;
	}

	@Override
	public void setAttachment(ISmartAttachment attachment) {
		this.attachment = attachment;
	}

}
