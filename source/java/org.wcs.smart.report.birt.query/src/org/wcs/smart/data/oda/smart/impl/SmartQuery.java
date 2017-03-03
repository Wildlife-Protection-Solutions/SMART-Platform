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
package org.wcs.smart.data.oda.smart.impl;

import java.text.MessageFormat;
import java.util.HashMap;

import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.data.oda.smart.internal.Messages;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.model.IQueryType;


/**
 * Implementation class of IQuery for the SMART ODA runtime driver. <br>
 * This wraps around any smart query (including summaries, patrol, waypoint
 * queries).
 */
public class SmartQuery extends AbstractSmartBirtQuery {

	//the query uuid and type
	private IQueryType queryType;

	/**
	 * Creates a new smart query
	 */
	public SmartQuery(SmartConnection connection) {
		super(connection);
		this.queryType = null;
		
	}

	
	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#prepare(java.lang.String)
	 * <p>Here the queryText contains the hex encoded uuid
	 * of the query.  The query is loaded from the database and
	 * parsed to ensure it is valid.
	 * </p>
	 */
	public void prepare(String queryText) throws OdaException {
		parameters = new HashMap<Object, Object>();
		
		ParsedQuery parsed = null;
		try{
			parsed = parseQueryText(queryText);
		} catch (Exception e1) {
			throw new OdaException(e1);
		}
		
		this.uuid = parsed.getUuid();
		
		String[] bits = queryText.split(":"); //$NON-NLS-1$
		this.queryType = QueryTypeManager.INSTANCE.findQueryType(parsed.getType());
			
		/* for historic support */
		if (this.queryType == null){
			this.queryType = QueryTypeManager.INSTANCE.findDeprecatedQueryType(parsed.getType());
		}
			
		if (this.queryType == null){
			throw new OdaException(MessageFormat.format(Messages.SmartQuery_QueryTypeNotSupported, new Object[]{bits[0]}));
		}
		
		try {
			this.wrapperObject = QueryDatasetExtensionManager.getInstance().getDatasetHandler(queryType.getKey());
		} catch (Exception e1) {
			throw new OdaException(e1);
		}
		
		if (this.wrapperObject == null){
			throw new OdaException(MessageFormat.format(Messages.SmartQuery_QueryTypeNotSupportedReports, new Object[]{queryType.getGuiName()}));
		}			

		if (smartQuery == null) {
			smartQuery = QueryHibernateManager.getInstance().findQuery(connection.getSession(),  uuid, queryType);
			if (smartQuery == null){
				throw new OdaException("SMART Query not found");
			}
		}
	}
	
}
