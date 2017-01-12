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

import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelAttribute;
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
	
	public static IntelAttribute getAttribute(String keyId, Session session){ 
		IntelAttribute attribute = (IntelAttribute)session.createCriteria(IntelAttribute.class)
			.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
			.add(Restrictions.eq("keyId", keyId))
			.uniqueResult();
		return attribute;
	}
	
	
	public static IntelAttributeListItem getAttributeListItem(IntelAttribute attribute, String keyId, Session session){ 
		if (attribute == null) return null;
		IntelAttributeListItem listitem = (IntelAttributeListItem)session.createCriteria(IntelAttributeListItem.class)
			.add(Restrictions.eq("attribute", attribute))
			.add(Restrictions.eq("keyId", keyId))
			.uniqueResult();
		return listitem;
	}
	
	public static IntelEntityType getEntityType(String keyId, Session session){ 
		IntelEntityType type = (IntelEntityType)session.createCriteria(IntelEntityType.class)
			.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
			.add(Restrictions.eq("keyId", keyId))
			.uniqueResult();
		return type;
	}
	
	public static IntelEntity getEntity(UUID entityUuid, Session session){
		return (IntelEntity) session.get(IntelEntity.class, entityUuid);
	}
}
