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

import java.util.HashMap;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.wcs.smart.data.oda.smart.impl.AbstractSmartQuery;

/**
 * Query dataset extension manager.
 * 
 * @author Emily
 *
 */
public class QueryDatasetExtensionManager {

	private static QueryDatasetExtensionManager instance = null;
	private static final Object LOCK = new Object();
	/**
	 * Mapping from query type key to smart dataset
	 */
	private HashMap<String, AbstractSmartQuery> datasets;
	
	private QueryDatasetExtensionManager(){
		
	}
	
	
	/**
	 * 
	 * @return manager instance
	 */
	public static QueryDatasetExtensionManager getInstance(){
		if (instance == null){
			synchronized (LOCK) {
				if (instance == null){
					instance = new QueryDatasetExtensionManager();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Gets the dataset handler for the given query type.
	 * @param queryType
	 * @return
	 * @throws Exception
	 */
	public AbstractSmartQuery getDatasetHandler(String queryType) throws Exception{
		if (datasets == null){
			getDatasets();
		}
		return datasets.get(queryType);
	}
	
	/*
	 * reads the datasets
	 */
	private synchronized void getDatasets() throws Exception{
		if (datasets != null){
			return;
		}
		datasets = new HashMap<String, AbstractSmartQuery>();
		
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtensionPoint pnt = registry.getExtensionPoint(AbstractSmartQuery.SMART_QUERY_EXTENSION_ID);
		IConfigurationElement[] config = pnt.getConfigurationElements();
		for (IConfigurationElement e : config) {
			AbstractSmartQuery query = (AbstractSmartQuery) e.createExecutableExtension("class"); //$NON-NLS-1$
			query.setMetadataProvider(ServerQueryMetadataProvider.INSTANCE);
			datasets.put(e.getAttribute("queryTypeKey"), query); //$NON-NLS-1$
		}
	}
}
