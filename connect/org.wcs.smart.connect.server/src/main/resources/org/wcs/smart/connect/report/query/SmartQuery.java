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
package org.wcs.smart.connect.report.query;

import java.text.MessageFormat;
import java.util.HashMap;

import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.data.oda.smart.impl.AbstractSmartBirtQuery;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.util.UuidUtils;


/**
 * Implementation class of IQuery for the SMART ODA runtime driver. <br>
 * This wraps around any smart query (including summaries, patrol, waypoint
 * queries).
 */
public class SmartQuery extends AbstractSmartBirtQuery {


	/**
	 * Creates a new smart query
	 */
	public SmartQuery(SmartConnection connection) {
		super(connection);
		
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
		
		String[] bits = queryText.split(":"); //$NON-NLS-1$
		String queryTypeKey = bits[0];
			
		/* for historic support */
		if (queryTypeKey.equals("OBSERVATION")){ //$NON-NLS-1$
			queryTypeKey = "patrolobservation"; //$NON-NLS-1$
		}else if (queryTypeKey.equals("PATROL")){ //$NON-NLS-1$
			queryTypeKey = "patrolquery"; //$NON-NLS-1$
		}else if (queryTypeKey.equals("GRIDDED")){ //$NON-NLS-1$
			queryTypeKey = "patrolgrid"; //$NON-NLS-1$
		}else if (queryTypeKey.equals("SUMMARY")){ //$NON-NLS-1$
			queryTypeKey = "patrolsummary"; //$NON-NLS-1$
		}else if (queryTypeKey.equals("WAYPOINT")){ //$NON-NLS-1$
			queryTypeKey = "patrolwaypoint"; //$NON-NLS-1$
		}

		try {
			this.uuid = UuidUtils.stringToUuid(bits[1]);
			this.wrapperObject = QueryDatasetExtensionManager.getInstance().getDatasetHandler(queryTypeKey);
		} catch (Exception e1) {
			throw new OdaException(e1);
		}
		
		if (this.wrapperObject == null){
			throw new OdaException(MessageFormat.format(Messages.getString("SmartQuery.QuerytypeNotSupported",connection.getCurrentLocale()), bits[0])); //$NON-NLS-1$
		}			

		if (smartQuery == null) {
			smartQuery = QueryManager.INSTANCE.findQuery(uuid, connection.getSession());
		}
	}
	
}
