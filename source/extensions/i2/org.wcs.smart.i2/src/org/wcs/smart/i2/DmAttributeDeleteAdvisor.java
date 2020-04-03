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
package org.wcs.smart.i2;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntityType;

/**
 * Ensure that data model attributes that are removed are not 
 * link to entity type.
 * 
 * @author Emily
 *
 */
public class DmAttributeDeleteAdvisor implements IDeleteAdvisor {

	public DmAttributeDeleteAdvisor() {
	}

	@Override
	public String canDelete(Object object, Session session) {
		Attribute attribute = (Attribute)object;
		
		IntelEntityType type = QueryFactory.buildQuery(session, IntelEntityType.class, 
				new Object[] {"dmAttribute", attribute}).uniqueResult(); //$NON-NLS-1$
		if (type == null) return null;
		
		return MessageFormat.format(Messages.DmAttributeDeleteAdvisor_LinkedAttributeMsg, attribute.getName(), type.getName());
	}

}
