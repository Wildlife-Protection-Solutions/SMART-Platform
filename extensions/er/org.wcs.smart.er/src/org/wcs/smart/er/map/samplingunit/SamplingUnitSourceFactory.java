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
package org.wcs.smart.er.map.samplingunit;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.wcs.smart.er.internal.Messages;

/**
 * Smart query data source factor.  
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SamplingUnitSourceFactory implements DataStoreFactorySpi{

	/**
	 * query uuid parameter
	 */
	public static final Param SD_UUID = new Param("surveydesignuuid", UUID.class, Messages.SamplingUnitSourceFactory_SurveyDesignParamName, true);  //$NON-NLS-1$
	  
	/**
	 * @see org.geotools.data.DataAccessFactory#canProcess(java.util.Map)
	 */
	@Override
	public boolean canProcess(Map<String, Serializable> params) {
		if (params.containsKey(SD_UUID.key)){
			return true;
		}
		return false;
	}

	/**
	 * @see org.geotools.data.DataAccessFactory#getDescription()
	 */
	@Override
	public String getDescription() {
		return Messages.SamplingUnitSourceFactory_Description;
	}

	/**
	 * @see org.geotools.data.DataAccessFactory#getDisplayName()
	 */
	@Override
	public String getDisplayName() {
		return Messages.SamplingUnitSourceFactory_Name;
	}

	/**
	 * @see org.geotools.data.DataAccessFactory#getParametersInfo()
	 */
	@Override
	public Param[] getParametersInfo() {
		return new Param[]{SD_UUID };
	}

	/**
	 * @see org.geotools.data.DataAccessFactory#isAvailable()
	 */
	@Override
	public boolean isAvailable() {
		return true;
	}

	/**
	 * @see org.geotools.factory.Factory#getImplementationHints()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Map<Key, ?> getImplementationHints() {
		return Collections.EMPTY_MAP;
	}

	/**
	 * @see org.geotools.data.DataStoreFactorySpi#createDataStore(java.util.Map)
	 */
	@Override
	public DataStore createDataStore(Map<String, Serializable> params)
			throws IOException {
		return null;
	}

	/**
	 * @see org.geotools.data.DataStoreFactorySpi#createNewDataStore(java.util.Map)
	 */
	@Override
	public DataStore createNewDataStore(Map<String, Serializable> arg0)
			throws IOException {
		throw new UnsupportedOperationException("This is a read-only data store."); //$NON-NLS-1$
	}

}
