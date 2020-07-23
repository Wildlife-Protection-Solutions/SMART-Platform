/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.data.oda.smart.query.common;

import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Attachment query column for BIRT attachment queries.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class AttachmentFilenameQueryColumn extends QueryColumn{

	public static final AttachmentFilenameQueryColumn INSTANCE = new AttachmentFilenameQueryColumn();
	
	private AttachmentFilenameQueryColumn() {
		super("Attachment Filename", "attachment:filename", ColumnType.STRING); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public Object getValue(IResultItem item) {
		if (item instanceof IAttachmentResultItem) {
			ISmartAttachment att = ((IAttachmentResultItem) item).getAttachment();
			return att.getFilename();
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public QueryColumn clone() {
		throw new UnsupportedOperationException();
	}

}
