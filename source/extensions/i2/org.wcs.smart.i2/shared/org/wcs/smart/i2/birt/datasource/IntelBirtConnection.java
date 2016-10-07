package org.wcs.smart.i2.birt.datasource;

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
import org.wcs.smart.ProjectionUtils;
import org.wcs.smart.birt.BirtConstants;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.birt.entity.EntityDataset;
import org.wcs.smart.i2.birt.entity.EntityDatasetMetadata;
import org.wcs.smart.i2.birt.entity.location.EntityLocationDataset;
import org.wcs.smart.i2.birt.entity.records.EntityRecordDataset;
import org.wcs.smart.util.GeometryUtils;

import com.ibm.icu.util.ULocale;

public class IntelBirtConnection implements IConnection {
	
	/**
	 * App context locale variable
	 */
	public static final String LOCALE_CONTEXT_VAR = "org.wcs.smart.report.locale"; //$NON-NLS-1$
	/**
	 * App context projection provider variable
	 */
	public static final String PROJECTION_PROVIDER_CONTEXT_VAR = "org.wcs.smart.report.crs"; //$NON-NLS-1$
	
	
	protected boolean m_isOpen = false;
	protected Session localSession;
	protected Map<Object,Object> appContext;
	
	private boolean cleanup;
	
	private final static IProjectionProvider defaultProjectionProvider = new IProjectionProvider() {
		private Projection p; 
		@Override
		public Projection getProjection() {
			if (p == null){
				p = new Projection();
				p.setParsedCoordinateReferenceSystem(GeometryUtils.SMART_CRS);
				p.setName(GeometryUtils.SMART_CRS.getName().toString());
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

	
	public void openSession(){
		cleanup = false;
		if (appContext != null){
			localSession = (Session) appContext.get(BirtConstants.SESSION_PARAM);
		}
		if (localSession == null){
			cleanup = true;
			localSession = HibernateManager.openSession();
			localSession.beginTransaction();
		}
	}
	
	public void closeSession(){
		if (cleanup){
			localSession.getTransaction().rollback();
			localSession.close();
		}
	}

	public Collection<ConservationArea> getConservationAreas() {
		return SmartDB.getConservationAreaConfiguration().getConservationAreas();
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public void setAppContext(Object context) throws OdaException {
		if (context instanceof Map){
			this.appContext = (Map<Object,Object>)context;
			this.appContext.put(IntelBirtConnection.class.getCanonicalName(), this);
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
				ConservationArea reportca = (ConservationArea) appContext.get(BirtConstants.CA_PARAM);
				if (reportca != null){
					value = ProjectionUtils.INSTANCE.createProjectionProvider(getSession(), reportca);
					appContext.put(PROJECTION_PROVIDER_CONTEXT_VAR, value);
				}
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
		return new EntityDatasetMetadata(this);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#newQuery(java.lang.String)
	 */
	public IQuery newQuery(String dataSetType) throws OdaException {
		try {
			if (dataSetType.equals(EntityDataset.DATASET_TYPE)) {
				return new EntityDataset(this);
			}else if (dataSetType.equals(EntityLocationDataset.DATASET_TYPE)) {
				return new EntityLocationDataset(this);
			}else if (dataSetType.equals(EntityRecordDataset.DATASET_TYPE)) {
				return new EntityRecordDataset(this);
			}
			throw new OdaException(
					MessageFormat.format("Dataset {0} not supported by SMART", //$NON-NLS-1$
							new Object[]{dataSetType}));
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

	/**
	 * Returns the object that represents this extension's manifest.
	 * 
	 * @throws OdaException
	 */
	public static ExtensionManifest getManifest() throws OdaException {
		return ManifestExplorer.getInstance().getExtensionManifest(IntelBirtDataSource.ODA_DATA_SOURCE_ID);
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
		DataTypeMapping typeMapping = getManifest().getDataSetType(dataSetType)
				.getDataTypeMapping(nativeDataTypeCode);
		if (typeMapping != null)
			return typeMapping.getNativeType();
		return "Undefined mapping type"; //$NON-NLS-1$
	}

}