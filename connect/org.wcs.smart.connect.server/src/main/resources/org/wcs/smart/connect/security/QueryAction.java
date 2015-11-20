/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.connect.query.QueryProxy;

/**
 * Query actions.  This actions controls which users can view/run Queries
 * 
 * 
 * @author Jeff
 *
 */
public class QueryAction implements ISmartConnectAction{

	public static final String RUNQUERY_KEY = "runquery"; //$NON-NLS-1$  //Also implies they can view it in the Connect listing.
	
	@Override
	public String getActionName(String actionKey, Locale l) {
		if (actionKey.equals(RUNQUERY_KEY)){
			return Messages.getString("QueryAction.RunQueryPermission", l); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public String[] getActionKeys() {
		return new String[]{RUNQUERY_KEY};
	}

	@Override
	public List<ResourceOption> getResourceOptions(String actionKey, Session s, Locale l) {

		List<ResourceOption> ops = new ArrayList<ResourceOption>();
		ResourceOption ro = new ResourceOption(Messages.getString("QueryAction.AllQueries", l), null); //$NON-NLS-1$
		ops.add(ro);
		
		List<QueryProxy> info = QueryManager.INSTANCE.getQueries(s, l);
		for (QueryProxy i : info){
			ro = new ResourceOption(i.getName(), i.getUuid());
			ops.add(ro);
		}
		
		return ops;
	}

	@Override
	public String getResourceName(UUID resource, Session s, Locale l) {
		if (resource == null) return Messages.getString("QueryAction.AllQueries", l); //$NON-NLS-1$
		QueryProxy info = (QueryProxy) s.createCriteria(QueryProxy.class)
				.add(Restrictions.eq("uuid", resource)) //$NON-NLS-1$
				.uniqueResult();
		if (info == null) return resource.toString();
		return info.getName();
	}

}
