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
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IDataSetMetaData;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.util.manifest.DataTypeMapping;
import org.eclipse.datatools.connectivity.oda.util.manifest.ExtensionManifest;
import org.eclipse.datatools.connectivity.oda.util.manifest.ManifestExplorer;
import org.hibernate.Session;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.data.oda.smart.impl.table.SmartTableQuery;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.model.Query;

import com.ibm.icu.util.ULocale;

/**
 * Implementation class of IConnection for SMART ODA runtime driver.
 */
public abstract class SmartConnection implements IConnection {
	
	/**
	 * App context locale variable
	 */
	public static final String LOCALE_CONTEXT_VAR = "org.wcs.smart.report.locale"; //$NON-NLS-1$
	/**
	 * App context projection provider variable
	 */
	public static final String PROJECTION_PROVIDER_CONTEXT_VAR = "org.wcs.smart.report.crs"; //$NON-NLS-1$
	
	public static final String ODA_DATA_SOURCE_ID = "org.wcs.smart.data.oda.smart"; //$NON-NLS-1$
	
	protected boolean m_isOpen = false;
	protected Session localSession;
	protected Map<Object,Object> appContext;
	
	private final static IProjectionProvider defaultProjectionProvider = new IProjectionProvider() {
		private Projection p; 
		@Override
		public Projection getProjection() {
			if (p == null){
				p = new Projection();
				p.setParsedCoordinateReferenceSystem(SmartDB.DATABASE_CRS);
				p.setName(SmartDB.DATABASE_CRS.getName().toString());
			}
			return p;
		}
	};
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#open(java.util.Properties)
	 */
	public void open(Properties connProperties) throws OdaException{
		openSession();
		m_isOpen = true;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#close()
	 */
	public void close() throws OdaException {
		closeSession();
		m_isOpen = false;
	}

	public abstract String getDataSourceProductName();
	
	public abstract void openSession();
	
	public abstract void closeSession();
	
	public abstract Collection<ConservationArea> getConservationAreas();
	
	public abstract IQueryResult executeQuery(Query query) throws Exception;

	protected abstract AbstractSmartBirtQuery createQuery();
	
	public abstract SmartBirtTable findSmartBirtTable(String queryText) throws OdaException;
	
	@SuppressWarnings("unchecked")
	@Override
	public void setAppContext(Object context) throws OdaException {
		if (context instanceof Map){
			this.appContext = (Map<Object,Object>)context;
			this.appContext.put(SmartConnection.class.getCanonicalName(), this);
		}
	}
	
	/**
	 * Gets the context locale from the appContext 
	 * variable LOCAL_CONTEXT_VAR or the default 
	 * context.
	 * @return
	 */
	public Locale getCurrentLocale(){
		if (appContext == null) return Locale.getDefault();
		Locale l = (Locale) appContext.get(LOCALE_CONTEXT_VAR);
		if (l == null) return Locale.getDefault();
		return l;
	}
	
	/**
	 * 
	 * @return the projection provider for the current app context
	 * @throws Exception
	 */
	public IProjectionProvider getProjectionProvider() throws Exception{
		IProjectionProvider value = defaultProjectionProvider;
		if (appContext != null){
			value = (IProjectionProvider) appContext.get(PROJECTION_PROVIDER_CONTEXT_VAR);	
			if (value == null){
				value = ObservationHibernateManager.createProjectionProvider(getSession());
				appContext.put(PROJECTION_PROVIDER_CONTEXT_VAR, value);
			}
		}
		return value;		
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#isOpen()
	 */
	public boolean isOpen() throws OdaException {
		return m_isOpen;
	}
	
	public Session getSession(){
		return this.localSession;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#getMetaData(java.lang.String)
	 */
	public IDataSetMetaData getMetaData(String dataSetType) throws OdaException {
		return new SmartDatasetMetadata(this);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#newQuery(java.lang.String)
	 */
	public IQuery newQuery(String dataSetType) throws OdaException {
		try {
			if (dataSetType.equals(AbstractSmartBirtQuery.SMART_DATASET_TYPE)) {
				return createQuery();
			}else if (dataSetType.equals(SmartTableQuery.SMART_DATASET_TYPE)){
				return createTableQuery();
			}
			throw new OdaException(
					MessageFormat.format("Dataset {0} not supported by SMART", //$NON-NLS-1$
							new Object[]{dataSetType}));
		} catch (Exception e) {
			throw new OdaException(e);
		}
	}

	
	protected SmartTableQuery createTableQuery(){
		return new SmartTableQuery(this);
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

	/**
	 * Returns the object that represents this extension's manifest.
	 * 
	 * @throws OdaException
	 */
	public static ExtensionManifest getManifest() throws OdaException {
		return ManifestExplorer.getInstance().getExtensionManifest(
				ODA_DATA_SOURCE_ID);
	}

	/**
	 * Returns the native data type name of the specified code, as defined in
	 * this data source extension's manifest.
	 * 
	 * @param nativeTypeCode
	 *            the native data type code
	 * @return corresponding native data type name
	 * @throws OdaException
	 *             if lookup fails
	 */
	public static String getNativeDataTypeName(int nativeDataTypeCode, String dataSetType)
			throws OdaException {
		DataTypeMapping typeMapping = SmartConnection.getManifest().getDataSetType(dataSetType)
				.getDataTypeMapping(nativeDataTypeCode);
		if (typeMapping != null)
			return typeMapping.getNativeType();
		return "Undefined mapping type"; //$NON-NLS-1$
	}

}
