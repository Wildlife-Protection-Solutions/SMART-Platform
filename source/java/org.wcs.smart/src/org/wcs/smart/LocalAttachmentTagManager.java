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
package org.wcs.smart;

import org.hibernate.Session;
import org.wcs.smart.ca.AttachmentTag;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * For managing attachment tags
 * 
 * @author Emily
 *
 */
public class LocalAttachmentTagManager extends AttachmentTagManager{

	public static LocalAttachmentTagManager INSTANCE = new LocalAttachmentTagManager();
	
	private LocalAttachmentTagManager() {
		super();
	}
	
	
	/**
	 * Delete a tag from the Conservation Area
	 * 
	 * @param type
	 * @param session
	 * @throws Exception if the attachment tag cannot be deleted
	 */
	public void deleteTag(AttachmentTag tag, Session session) throws Exception {
		
		if (!DeleteManager.canDelete(tag, session)) {
			throw new Exception("Cannot delete attachment tag.");
		}
		
		if (tag.getUuid() == null) return;
		
		String sql = "DELETE FROM AttachmentTagLink WHERE tag = :tag";  //$NON-NLS-1$
		session.createMutationQuery(sql).setParameter("tag", tag).executeUpdate(); //$NON-NLS-1$
		
		session.remove(tag);
	}
		
	/**
	 * Saves the attachment tag to the database 
	 * @param type
	 * @param session
	 */
	public AttachmentTag saveTag(AttachmentTag type, Session session) {
		return HibernateManager.saveOrMerge(session, type);
	}
	
	
	/**
	 * Creates a new tag with the given name
	 * @param ca
	 * @param name
	 * @return
	 */
	public AttachmentTag createTag(ConservationArea ca, String name) {
		AttachmentTag newTag = new AttachmentTag();
		newTag.setConservationArea(ca);
		newTag.updateName(ca.getDefaultLanguage(), name);
		newTag.setName(name);
		newTag.updateName(SmartDB.getCurrentLanguage(), name);
		return newTag;
	}
}
