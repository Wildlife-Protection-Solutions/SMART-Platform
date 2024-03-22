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
package org.wcs.smart.data.oda.smart.query.common;

import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.observation.model.AttachmentTagLink;
import org.wcs.smart.observation.model.ISignatureAttachment;
import org.wcs.smart.observation.model.ITaggedAttachment;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Attachment signature type query column for BIRT attachment queries.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class SignatureTypeQueryColumn extends QueryColumn{

	public static final SignatureTypeQueryColumn KEY_COLUMN = new SignatureTypeQueryColumn(Type.KEY);
	public static final SignatureTypeQueryColumn NAME_COLUMN = new SignatureTypeQueryColumn(Type.NAME);
	
	public static final SignatureTypeQueryColumn TAGS_COLUMN = new SignatureTypeQueryColumn(Type.TAGS);
	
	public static enum Type{
		KEY,NAME, TAGS;
		
		public String getName() {
			switch(this) {
			case KEY: return "Signature Type Key"; //$NON-NLS-1$
			case NAME: return "Signature Type Name"; //$NON-NLS-1$
			case TAGS: return "Attachment Tags";
			}
			return ""; //$NON-NLS-1$
		}
		
		public String getKey() {
			switch(this) {
			case KEY: return "attachment:signaturetypekey"; //$NON-NLS-1$
			case NAME: return "attachment:signaturetypename"; //$NON-NLS-1$
			case TAGS: return "attachment:tags";
			}
			return ""; //$NON-NLS-1$
		}
	}
	
	private Type type;
	
	private SignatureTypeQueryColumn(Type type) {
		super(type.getName(), type.getKey(), ColumnType.STRING); 
		this.type = type;
	}

	@Override
	public Object getValue(IResultItem item) {
		if (item instanceof IAttachmentResultItem) {
			ISmartAttachment att = ((IAttachmentResultItem) item).getAttachment();
			if (att instanceof ISignatureAttachment) {
				if (((ISignatureAttachment) att).getSignatureType() != null) {
					if (type == Type.KEY) return ((ISignatureAttachment) att).getSignatureType().getKeyId();
					if (type == Type.NAME) return ((ISignatureAttachment) att).getSignatureType().getName();
				}
			}
			if (att instanceof ITaggedAttachment tagged) {
				if (type == Type.TAGS) {
					
					if (tagged.getAttachmentTags() != null && !tagged.getAttachmentTags().isEmpty()) {
						StringBuilder sb = new StringBuilder();
						sb.append("|");
						for (AttachmentTagLink tag : tagged.getAttachmentTags()) {
							sb.append(tag.getTag().getKeyId());
							sb.append("|");
						}
						return sb.toString();
					}
					
				}
			}
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public QueryColumn clone() {
		throw new UnsupportedOperationException();
	}

}
