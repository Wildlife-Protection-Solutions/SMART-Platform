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
package org.wcs.smart.geotools.data.smart;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;

/**
 * Smart area data source factory.  This is a read only data source
 * @author Emily
 * @since 1.0.0
 */
public class SmartDataSourceFactory implements DataStoreFactorySpi{

	public static final Param CA_UUID = new Param("cauuid", UUID.class, Messages.SmartDataSourceFactory_CA_ParameterName, true);  //$NON-NLS-1$
	  
	/* (non-Javadoc)
	 * @see org.geotools.data.DataAccessFactory#canProcess(java.util.Map)
	 */
	@Override
	public boolean canProcess(Map<String, Serializable> params) {
		if (params.containsKey(CA_UUID.key)){
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.DataAccessFactory#getDescription()
	 */
	@Override
	public String getDescription() {
		return Messages.SmartDataSourceFactory_SmartDataSourceDescription;
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.DataAccessFactory#getDisplayName()
	 */
	@Override
	public String getDisplayName() {
		return Messages.SmartDataSourceFactory_SmartDataSourceName;
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.DataAccessFactory#getParametersInfo()
	 */
	@Override
	public Param[] getParametersInfo() {
		return new Param[]{CA_UUID };
	}

	/**
	 * @see org.geotools.data.DataAccessFactory#isAvailable()
	 */
	@Override
	public boolean isAvailable() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.geotools.factory.Factory#getImplementationHints()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Map<Key, ?> getImplementationHints() {
		return Collections.EMPTY_MAP;
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.DataStoreFactorySpi#createDataStore(java.util.Map)
	 */
	@Override
	public DataStore createDataStore(Map<String, Serializable> params)
			throws IOException {
		
		Session session = HibernateManager.openSession();
		ConservationArea ca = null;
		try{
			ca = (ConservationArea)session.load(ConservationArea.class, ((UUID)params.get(CA_UUID.key)));	
		
		}finally{
			session.close();
		}
		if (ca == null ){
			throw new IOException(Messages.SmartDataSourceFactory_Error_ReadingSmartDataSource);
		}
		return new SmartDataSource(ca);
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.DataStoreFactorySpi#createNewDataStore(java.util.Map)
	 */
	@Override
	public DataStore createNewDataStore(Map<String, Serializable> arg0)
			throws IOException {
		throw new UnsupportedOperationException(Messages.SmartDataSourceFactory_Error_ReadOnly);
	}

}
