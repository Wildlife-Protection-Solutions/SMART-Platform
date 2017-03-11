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
package org.wcs.smart.i2;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelRecordAttachment;

/**
 * Intelligence attachment manager 
 * @author Emily
 *
 */
public enum AttachmentManager {
	
	INSTANCE;
	
	private AttachmentManager(){
		
	}

	
	/**
	 * Determines if all links to the attachment are removed.
	 * 
	 * @param type
	 * @param session
	 * @throws Exception
	 */
	public boolean canDelete(IntelAttachment attachment, Session session) throws Exception{
		//attachments are linked to:
		//entities; records
		Long recordCnt = (Long)session.createCriteria(IntelRecordAttachment.class)
			.add(Restrictions.eq("id.attachment", attachment)) //$NON-NLS-1$
			.setProjection(Projections.rowCount())
			.uniqueResult();
		if (recordCnt != 0) return false;
		
		recordCnt = (Long)session.createCriteria(IntelEntityAttachment.class)
				.add(Restrictions.eq("id.attachment", attachment)) //$NON-NLS-1$
				.setProjection(Projections.rowCount())
				.uniqueResult();
		if (recordCnt != 0) return false;
		
		return true;
	}
}
