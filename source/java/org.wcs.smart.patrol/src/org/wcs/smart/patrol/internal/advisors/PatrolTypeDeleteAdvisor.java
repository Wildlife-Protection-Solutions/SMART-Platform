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
package org.wcs.smart.patrol.internal.advisors;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolType;

/**
 * @sinze 8.0.1
 */
public class PatrolTypeDeleteAdvisor implements IDeleteAdvisor {

	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof PatrolType)){
			return "Object not of type PatrolType. Can not delete."; //$NON-NLS-1$
		}
		PatrolType type = (PatrolType)object;
		
		if (type.getKeyId().equalsIgnoreCase(PatrolType.DefaultType.MIXED.getKeyId())) {
			return MessageFormat.format(Messages.PatrolTypeDeleteAdvisor_systemtracktypenotdelete, PatrolType.DefaultType.MIXED.getKeyId());
		}
		Long cnt = QueryFactory.buildCountQuery(session, Patrol.class,new Object[] {"patrolType", object}); //$NON-NLS-1$
		if (cnt != 0){
			return MessageFormat.format(
					Messages.PatrolTypeDeleteAdvisor_patrolswithtypeexist,
					new Object[]{cnt, type.getName()});
		}
		
		return null;
		
	}

}
