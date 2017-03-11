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
package org.wcs.smart.i2.handlers;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.model.IntelRelationshipTypeAttribute;

/**
 * Checks for attribute relationships that prevent deletion.
 * 
 * @author Emily
 *
 */
public class DeleteIntelAttributeAdvisor implements IDeleteAdvisor {

	public DeleteIntelAttributeAdvisor() {
	}

	@SuppressWarnings("unchecked")
	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof IntelAttribute)){
			return Messages.DeleteIntelAttributeAdvisor_InvalidObject;
		}
		IntelAttribute attribute = (IntelAttribute) object;
		List<IntelEntityTypeAttribute> links = 
				session.createCriteria(IntelEntityTypeAttribute.class)
			.add(Restrictions.eq("id.attribute", attribute)) //$NON-NLS-1$
			.list();
		
		if (!links.isEmpty()){
			StringBuilder sb = new StringBuilder();
			sb.append(Messages.DeleteIntelAttributeAdvisor_EntityTypeError);
			for (IntelEntityTypeAttribute a : links){
				sb.append(a.getEntityType().getName());
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			sb.append("."); //$NON-NLS-1$
			return sb.toString();
		}
		
		List<IntelRelationshipTypeAttribute> links2 = 
				session.createCriteria(IntelRelationshipTypeAttribute.class)
			.add(Restrictions.eq("id.attribute", attribute)) //$NON-NLS-1$
			.list();
		
		if (!links2.isEmpty()){
			StringBuilder sb = new StringBuilder();
			sb.append(Messages.DeleteIntelAttributeAdvisor_RelationshipTypeError);
			for (IntelRelationshipTypeAttribute a : links2){
				sb.append(a.getRelationshipType().getName());
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			sb.append("."); //$NON-NLS-1$
			return sb.toString();
		}
		
		List<IntelRecordSourceAttribute> links3 = 
				session.createCriteria(IntelRecordSourceAttribute.class)
			.add(Restrictions.eq("attribute", attribute)) //$NON-NLS-1$
			.list();
		
		if (!links3.isEmpty()){
			StringBuilder sb = new StringBuilder();
			sb.append(Messages.DeleteIntelAttributeAdvisor_SourceError);
			for (IntelRecordSourceAttribute a : links3){
				sb.append(a.getSource().getName());
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			sb.append("."); //$NON-NLS-1$
			return sb.toString();
		}
		
		return null;
	}

}
