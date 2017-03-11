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
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;

/**
 * Common hibernate queries for intelligence.
 * 
 * @author Emily
 *
 */
public class IntelHibernateManager {
	
	public static IntelAttribute getAttribute(String keyId, ConservationArea ca, Session session){ 
		IntelAttribute attribute = (IntelAttribute)session.createCriteria(IntelAttribute.class)
			.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
			.add(Restrictions.eq("keyId", keyId)) //$NON-NLS-1$
			.uniqueResult();
		return attribute;
	}
	
	
	public static IntelAttributeListItem getAttributeListItem(IntelAttribute attribute, String keyId, Session session){ 
		if (attribute == null) return null;
		IntelAttributeListItem listitem = (IntelAttributeListItem)session.createCriteria(IntelAttributeListItem.class)
			.add(Restrictions.eq("attribute", attribute)) //$NON-NLS-1$
			.add(Restrictions.eq("keyId", keyId)) //$NON-NLS-1$
			.uniqueResult();
		return listitem;
	}
	
	public static IntelEntityType getEntityType(String keyId, ConservationArea ca, Session session){ 
		IntelEntityType type = (IntelEntityType)session.createCriteria(IntelEntityType.class)
			.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
			.add(Restrictions.eq("keyId", keyId)) //$NON-NLS-1$
			.uniqueResult();
		return type;
	}
	
	public static IntelEntity getEntity(UUID entityUuid, Session session){
		return (IntelEntity) session.get(IntelEntity.class, entityUuid);
	}
	
	/**
	 * Gets all attributes sorted by name. Does not lazily load list items, translations etc.
	 * 
	 * @param session
	 * @param ca
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<IntelAttribute> getAttributes(Session session, ConservationArea ca){
		List<IntelAttribute> types = session.createCriteria(IntelAttribute.class)
			.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
			.list();
		types.sort((IntelAttribute a, IntelAttribute b) -> Collator.getInstance().compare(a.getName(), b.getName()));
		return types;
	}
	
	/**
	 * converts an attribute type to an sql type
	 * @param type
	 * @return
	 */
	public static int getAttributeSqlType(AttributeType type){
		switch(type){
		case BOOLEAN:
			return java.sql.Types.BOOLEAN;
		case DATE:
			return java.sql.Types.DATE;
		case NUMERIC:
			return java.sql.Types.DOUBLE;
		case POSITION:
			return java.sql.Types.VARCHAR;
		case LIST:
		case TEXT:
			return java.sql.Types.VARCHAR;
		};
		return -1;
	}
}
