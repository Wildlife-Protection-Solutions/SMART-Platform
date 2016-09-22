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

import java.text.Collator;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.i2.model.IntelAttribute;

/**
 * Tools for mangaing intelligence attributes
 * @author Emily
 *
 */
public enum AttributeManager {
	INSTANCE;
	
	private AttributeManager(){
		
	}
	
	/**
	 * Gets all attributes sorted by name. Does not lazily load list items, translations etc.
	 * 
	 * @param session
	 * @param ca
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<IntelAttribute> getAttributes(Session session, ConservationArea ca){
		List<IntelAttribute> types = session.createCriteria(IntelAttribute.class)
			.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
			.list();
		types.sort((IntelAttribute a, IntelAttribute b) -> Collator.getInstance().compare(a.getName(), b.getName()));
		return types;
	}
	
	public void canDelete(IntelAttribute type, Session session) throws Exception{
		if (!DeleteManager.canDelete(type, session)){
			throw new Exception("Unknown error occurrs while deleteing entity type.");
		}
	}
	
	/**
	 * Deletes an intelligence attribute from the system.  This will fail if there are any links
	 * to this attribute.
	 * 
	 * @param type
	 * @param session
	 * @throws Exception
	 */
	public void deleteAttribute(IntelAttribute type, Session session) throws Exception{
		canDelete(type, session);
		session.delete(type);
	}
}
