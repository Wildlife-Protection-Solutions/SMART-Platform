package org.wcs.smart.data.oda.smart.impl;

import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

public class QueryDatasetExtensionManager {

	private HashMap<String, ISmartQuery> datasets;
	
	private QueryDatasetExtensionManager(){
		
	}
	
	public static QueryDatasetExtensionManager instance = null;
	
	public static QueryDatasetExtensionManager  getInstance(){
		if (instance == null){
			instance = new QueryDatasetExtensionManager();
		}
		return instance;
	}
	
	public ISmartQuery getDatasetHandler(String queryType) throws Exception{
		if (datasets == null){
			getDatasets();
		}
		return datasets.get(queryType);
	}
	
	private synchronized void getDatasets() throws CoreException{
		if (datasets != null){
			return;
		}
		datasets = new HashMap<String, ISmartQuery>();
		IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(ISmartQuery.SMART_QUERY_EXTENSION_ID);
		for (IConfigurationElement e : config) {
			datasets.put(e.getAttribute("queryTypeKey"), (ISmartQuery) e.createExecutableExtension("class"));
		}
	}
}
