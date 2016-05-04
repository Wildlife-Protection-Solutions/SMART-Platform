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
import java.util.Collection;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTableUtils;
import org.wcs.smart.data.oda.smart.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.QueryExecutor;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.report.execute.SmartReportRunner;

/**
 * Implementation class of IConnection for SMART ODA runtime driver.
 */
public class DesktopSmartConnection extends SmartConnection {

	private boolean cleanup;
	@Override
	public void openSession(){
		cleanup = false;
		if (appContext != null){
			localSession = (Session) appContext.get(SmartReportRunner.SESSION_PARAM);
		}else{
			cleanup = true;
			localSession = HibernateManager.openSession();
			localSession.beginTransaction();
		}
	}
	
	@Override
	public void closeSession(){
		if (cleanup){
			localSession.getTransaction().rollback();
			localSession.close();
		}
	}

	@Override
	public AbstractSmartBirtQuery createQuery(){
		return new SmartQuery(this);
	}

	@Override
	public IQueryResult executeQuery(Query query) throws Exception {
		return QueryExecutor.INSTANCE.executeQuery(query, getSession(), new NullProgressMonitor());
	}

	@Override
	public Collection<ConservationArea> getConservationAreas() {
		return SmartDB.getConservationAreaConfiguration().getConservationAreas();
	}

	@Override
	public SmartBirtTable findSmartBirtTable(String queryText) throws OdaException{
		try{
			SmartBirtTable table = SmartBirtTableUtils.getInstance().findTable(queryText, this);
			if (table == null){
				throw new OdaException(
						MessageFormat.format("Could not find SMART data table {0}.", new Object[]{queryText})); //$NON-NLS-1$
			}
			return table;
		}catch (Exception ex){
			throw new OdaException (ex);
		}
	}

	@Override
	public String getDataSourceProductName() {
		return Messages.SmartDatasetMetadata_SmartDataSourceName;
	}

}
