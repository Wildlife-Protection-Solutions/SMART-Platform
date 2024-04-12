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
package org.wcs.smart.intelligence.report.oda;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;

import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IDataSetMetaData;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.hibernate.Session;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.ProjectionUtils;
import org.wcs.smart.birt.BirtConstants;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.internal.Messages;

import com.ibm.icu.util.ULocale;

/**
 * Connection for the SMART intelligence driver
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class SmartIntelligenceConnection implements IConnection {

	/**
	 * App context projection provider variable
	 */
	public static final String PROJECTION_PROVIDER_CONTEXT_VAR = "org.wcs.smart.report.crs"; //$NON-NLS-1$
	
	private boolean m_isOpen = false;
	protected Session localSession;
	protected Map<Object,Object> appContext;
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#open(java.util.Properties)
	 */
	public void open(Properties connProperties) throws OdaException {
		m_isOpen = true;
		localSession = (Session) appContext.get(BirtConstants.SESSION_PARAM);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#setAppContext(java.lang.Object)
	 */
	public void setAppContext(Object context) throws OdaException {
		if (context instanceof Map){
			this.appContext = (Map<Object,Object>)context;
		}
	}


	public Session getSession(){
		return this.localSession;
	}
	
	/**
	 * 
	 * @return the projection provider for the current app context
	 * @throws Exception
	 */
	public IProjectionProvider getProjectionProvider() throws Exception{
		IProjectionProvider value = null;
		if (appContext != null){
			value = (IProjectionProvider) appContext.get(PROJECTION_PROVIDER_CONTEXT_VAR);	
		}
		if (value == null){
			value = ProjectionUtils.INSTANCE.createProjectionProvider(getSession(), SmartDB.getCurrentConservationArea());
			if (appContext != null) appContext.put(PROJECTION_PROVIDER_CONTEXT_VAR, value);
		}
		return value;		
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#close()
	 */
	public void close() throws OdaException {
		m_isOpen = false;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#isOpen()
	 */
	public boolean isOpen() throws OdaException {
		return m_isOpen;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#getMetaData(java.lang.String)
	 */
	public IDataSetMetaData getMetaData(String dataSetType) throws OdaException {
		return new SmartIntelligenceDatasetMetadata(this);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#newQuery(java.lang.String)
	 */
	public IQuery newQuery(String dataSetType) throws OdaException {
		try {
			if (dataSetType.equals(IntelligencePointsQuery.ID)) {
				return new IntelligencePointsQuery(this);
			}
			throw new OdaException(MessageFormat.format(Messages.SmartIntelligenceConnection_UnsupportedDataset, dataSetType));
		} catch (Exception e) {
			throw new OdaException(e);
		}
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#getMaxQueries()
	 */
	public int getMaxQueries() throws OdaException {
		return 0; // no limit
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#commit()
	 */
	public void commit() throws OdaException {
		// do nothing; assumes no transaction support needed
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#rollback()
	 */
	public void rollback() throws OdaException {
		// do nothing; assumes no transaction support needed
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#setLocale(com.ibm.icu.util.ULocale)
	 */
	public void setLocale(ULocale locale) throws OdaException {
		// do nothing; assumes no locale support
	}
	
}
