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
package org.wcs.smart.intelligence;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligenceSource;

/**
 * Validates that the intelligence source can be removed.  Source
 * objects can only be removed if they are not associated with any
 * intelligence objects.
 * 
 * @author Emily
 *
 */
public class SourceDeleteAdvisor implements IDeleteAdvisor {

	public SourceDeleteAdvisor() {
	}

	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof IntelligenceSource)){
			return Messages.SourceDeleteAdvisor_InvalidObjectType;
		}
		IntelligenceSource source = (IntelligenceSource)object;
		Long cnt = QueryFactory.buildCountQuery(session, Intelligence.class, new Object[] {"source", source}); //$NON-NLS-1$
		
		if (cnt > 0){
			return MessageFormat.format(
					Messages.SourceDeleteAdvisor_IntelligenceDeleteError, new Object[]{source.getName(), cnt}); 
		}
		return null;
	}

}
