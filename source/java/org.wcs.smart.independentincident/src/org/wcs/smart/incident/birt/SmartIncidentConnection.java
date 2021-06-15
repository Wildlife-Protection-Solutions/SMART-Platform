/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.incident.birt;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;

import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IDataSetMetaData;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.hibernate.Session;
import org.wcs.smart.birt.BirtConstants;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.birt.attachments.IncidentAttachmentDataset;
import org.wcs.smart.incident.birt.details.IncidentDataset;
import org.wcs.smart.incident.birt.observations.IncidentObservationAttributeDataset;
import org.wcs.smart.incident.birt.observations.IncidentObservationDataset;

import com.ibm.icu.util.ULocale;

/**
 * Connection for the SMART incident driver.
 * 
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class SmartIncidentConnection implements IConnection {
	
	/**
	 * App context locale variable
	 */
	public static final String LOCALE_CONTEXT_VAR = "org.wcs.smart.report.locale"; //$NON-NLS-1$
	/**
	 * App context projection provider variable
	 */
	public static final String PROJECTION_PROVIDER_CONTEXT_VAR = "org.wcs.smart.report.crs"; //$NON-NLS-1$
	
	
	protected Map<Object,Object> appContext;
	protected boolean m_isOpen = false;
	protected Session localSession;
	private boolean cleanup;

	public Session getSession() {
		return this.localSession;
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#getMetaData(java.lang.String)
	 */
	@Override
	public IDataSetMetaData getMetaData(String dataSetType) throws OdaException {
		if (dataSetType.equals(IncidentDataset.DATASET_TYPE) ||
				dataSetType.equals(IncidentAttachmentDataset.DATASET_TYPE) ||
				dataSetType.equals(IncidentObservationAttributeDataset.DATASET_TYPE) ||
				dataSetType.equals(IncidentObservationDataset.DATASET_TYPE)) {
			return new IncidentDatasetMetadata(this);
		}
		
		throw new OdaException(
				MessageFormat.format("Unsupported Dataset", //$NON-NLS-1$
						new Object[]{dataSetType}));
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#newQuery(java.lang.String)
	 */
	@Override
	public IQuery newQuery(String dataSetType) throws OdaException {
		try {
			if (dataSetType.equals(IncidentDataset.DATASET_TYPE)) {
				return new IncidentDataset(this);
			}else if (dataSetType.equals(IncidentAttachmentDataset.DATASET_TYPE)){
				return new IncidentAttachmentDataset(this);
			}else if (dataSetType.equals(IncidentObservationDataset.DATASET_TYPE)){
				return new IncidentObservationDataset(this);
			}else if (dataSetType.equals(IncidentObservationAttributeDataset.DATASET_TYPE)){
				return new IncidentObservationAttributeDataset(this);
			}
			throw new OdaException(MessageFormat.format("Unsupported Dataset",new Object[]{dataSetType})); //$NON-NLS-1$
		} catch (Exception e) {
			throw new OdaException(e);
		}
	}

	

	@Override
	public void close() throws OdaException {
		closeSession();
		m_isOpen = false;
		//we cannot cleanup attachments until after the report is rendered
		//if we run BIRT as separate run and render tasks in html
//		if (appContext == null) return;
//		Object randrtask = appContext.get("EngineTask"); //$NON-NLS-1$
//		if (randrtask == null || !(randrtask instanceof EngineTask)) return;
//		EngineTask task = (EngineTask)randrtask;
//		if (task.getRenderOption() == null) return;
//		Object x = task.getRenderOption().getOption(HTMLRenderOption.IMAGE_DIRECTROY);
//		if ( x == null ) cleanUpAttachmentFiles(attachmentFiles);
	}

	@Override
	public void commit() throws OdaException {
		// do nothing; assumes no transaction support needed		
	}

	@Override
	public int getMaxQueries() throws OdaException {
		return 0; //no limit
	}

	@Override
	public boolean isOpen() throws OdaException {
		return m_isOpen;
	}

	@Override
	public void open(Properties arg0) throws OdaException {
		openSession();
		m_isOpen = true;
//		attachmentFiles = new ArrayList<>();		
	}

	@Override
	public void rollback() throws OdaException {
		// do nothing; assumes no transaction support needed
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setAppContext(Object context) throws OdaException {
		if (context instanceof Map){
			this.appContext = (Map<Object,Object>)context;
			this.appContext.put(SmartIncidentConnection.class.getCanonicalName(), this);
		}		
	}

	@Override
	public void setLocale(ULocale arg0) throws OdaException {
		// do nothing; assumes no locale support
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
}
