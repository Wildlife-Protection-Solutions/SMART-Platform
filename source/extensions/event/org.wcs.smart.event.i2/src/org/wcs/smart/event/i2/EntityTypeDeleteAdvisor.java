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
package org.wcs.smart.event.i2;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.event.i2.entity.CreateEntityActionType;
import org.wcs.smart.event.i2.entity.EntityTypeParameter;
import org.wcs.smart.event.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntityType;

/**
 * Delete advisor for deleting entity type configurations.
 * 
 * @author Emily
 *
 */
public class EntityTypeDeleteAdvisor implements IDeleteAdvisor {

	public EntityTypeDeleteAdvisor() {
	}

	@Override
	public String canDelete(Object object, Session session) {
		
		if (!(object instanceof IntelEntityType)) return Messages.EntityTypeDeleteAdvisor_InvalidType;
		IntelEntityType entityType = (IntelEntityType)object;
		
		StringBuilder sb = new StringBuilder("SELECT count(*) FROM EAction ea join ea.parameters p "); //$NON-NLS-1$
		sb.append("WHERE ea.actionTypeKey = :actionid AND p.id.parameterKey = :pkey "); //$NON-NLS-1$
		sb.append("AND p.parameterValue = :value AND ea.conservationArea = :ca"); //$NON-NLS-1$
		
		Long used = session.createQuery(sb.toString(), Long.class)
		.setParameter("actionid", CreateEntityActionType.KEY) //$NON-NLS-1$
		.setParameter("pkey", EntityTypeParameter.INSTANCE.getKey()) //$NON-NLS-1$
		.setParameter("value", entityType.getKeyId()) //$NON-NLS-1$
		.setParameter("ca", entityType.getConservationArea()) //$NON-NLS-1$
		.uniqueResult();
		
		if (used > 0) {
			return MessageFormat.format(Messages.EntityTypeDeleteAdvisor_DeleteErrorMsg, entityType.getName());
		}
		
		return null;
	}

}
