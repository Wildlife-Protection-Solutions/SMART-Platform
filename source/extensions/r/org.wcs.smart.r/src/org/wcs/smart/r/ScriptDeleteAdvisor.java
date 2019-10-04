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
package org.wcs.smart.r;

import java.text.MessageFormat;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.r.internal.Messages;
import org.wcs.smart.r.model.RQuery;
import org.wcs.smart.r.model.RScript;


/**
 * 
 * Ensures that there are no R queries associated with the
 * script before it is deleted
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class ScriptDeleteAdvisor implements IDeleteAdvisor {

	public ScriptDeleteAdvisor() {
	}

	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof RScript)) return null;
		List<RQuery> queries = QueryFactory.buildQuery(session, RQuery.class, 
				new Object[] {"script", object}).list(); //$NON-NLS-1$
		
		if (queries.size() > 0) {
			StringBuilder sb = new StringBuilder();
			queries.forEach(s->sb.append(s.getName() + " ")); //$NON-NLS-1$
			return MessageFormat.format(Messages.ScriptDeleteAdvisor_RQueryDeleteError, ((RScript)object).getName(), sb.toString());
		}
		return null;
	}

}
