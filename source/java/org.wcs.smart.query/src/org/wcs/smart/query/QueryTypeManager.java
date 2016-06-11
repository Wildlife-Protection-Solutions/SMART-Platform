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
package org.wcs.smart.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.QueryCategory;
import org.wcs.smart.query.ui.definition.DefinitionPanelManager;
/**
 * Manages the query type extension. Loads all query types
 * and provides support functions for finding query types.
 * @author Emily
 *
 */
public enum QueryTypeManager {

	INSTANCE;
	
	private static final String QUERYTYPE_EXTNAME = "QueryType"; //$NON-NLS-1$

	private static final String QUERYTYPEGROUP_EXTNAME = "QueryCategory"; //$NON-NLS-1$

	
	private IQueryType[] allTypes;
	private IQueryType[] supportedTypes;
	private List<QueryTypeWrapper> types;
	private List<QueryCategory> groups;
	private Map<IQueryType, Class<? extends IQueryEngine>> executors;

	public IQueryEngine getQueryEngine(IQueryType type) throws Exception{
		return executors.get(type).newInstance();
	}
	
	/**
	 * Finds the query type for the given type key.
	 * @param typeKey
	 * @return
	 */
	public IQueryType findQueryType(String typeKey){
		for (IQueryType type : getAllQueryTypes()){
			if (type.getKey().equals(typeKey)){
				return type;
			}
		}
		return findDeprecatedQueryType(typeKey);
	}
		
	/**
	 * Provides support for deprecated query types from
	 * pre 3.0.0 systems
	 * 
	 * @param depricatedTypeString
	 * @return
	 */
	public IQueryType findDeprecatedQueryType(String deprecatedTypeString){
		String newTypeString = findDeprecatedQueryTypeString(deprecatedTypeString);
		if (newTypeString.equals(deprecatedTypeString)) return null;
		return findQueryType(newTypeString);
	}
	
	/**
	 * Returns the current valid query type string
	 * @param deprecatedTypeString
	 * @return
	 */
	public String findDeprecatedQueryTypeString(String queryTypeString){
		if (queryTypeString.equals("OBSERVATION")){ //$NON-NLS-1$
			return "patrolobservation"; //$NON-NLS-1$
		}else if (queryTypeString.equals("PATROL")){ //$NON-NLS-1$
			return "patrolquery"; //$NON-NLS-1$
		}else if (queryTypeString.equals("GRIDDED")){ //$NON-NLS-1$
			return "patrolgrid"; //$NON-NLS-1$
		}else if (queryTypeString.equals("SUMMARY")){ //$NON-NLS-1$
			return "patrolsummary"; //$NON-NLS-1$
		}else if (queryTypeString.equals("WAYPOINT")){ //$NON-NLS-1$
			return "patrolwaypoint"; //$NON-NLS-1$
		}
		return queryTypeString;
	}
	/**
	 * Gets the query item panel that is associated with the given query
	 * type and definition panel.  There should only be one item panel
	 * for a given definition panel and query type.  This panel is the list
	 * of items that can be added to the definition panel.
	 * 
	 * @param qType
	 * @param definitionId
	 * @return
	 */
	public String getQueryItemPanel(IQueryType qType, String definitionId){
		for (QueryTypeWrapper q : getTypeConfigurations()){
			if (q.queryType.getKey().equals(qType.getKey())){
				return q.itemPanels.get(definitionId);
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param qType
	 * @return all definition panels that are supported for the query type
	 */
	public String[] getQueryDefinitionPanelIds(IQueryType qType){
		for (QueryTypeWrapper q : getTypeConfigurations()){
			if (q.queryType.getKey().equals(qType.getKey())){
				return q.definitionPanels.toArray(new String[q.definitionPanels.size()]);
			}
		}
		return null;
	}
	
	/**
	 * <p>This includes only the query types
	 * that support the current system configuration.  If
	 * the system is performing cross conservation area 
	 * analysis this will only return query types that support
	 * cross ca analysis.  The same is true for
	 * single analysis.</p>
	 * This function should only be called once you are logged
	 * into a conservation area; otherwise it will
	 * throw an exception
	 * 
	 * @return array of all supported query types
	 * for the current system configuration
	 */
	public IQueryType[] getSupportedQueryTypes() {
		if (this.supportedTypes == null){
			findSupportedType();
		}
		return this.supportedTypes;
	}
	
	/**
	 * 
	 * @return array of all supported query types
	 * irregardless of system configuration
	 */
	public IQueryType[] getAllQueryTypes() {
		if (this.allTypes == null){
			readTypesOnly();
		}
		return this.allTypes;
	}
	
	
	public List<QueryCategory> getQueryGroups(){
		if (this.groups == null){
			readTypesOnly();
		}
		return this.groups;
	}
	
	
	private synchronized void readTypesOnly(){
		if (allTypes != null){
			return;
		}
		List<IQueryType> aTypes = new ArrayList<IQueryType>();
		groups = new ArrayList<QueryCategory>();
		executors = new HashMap<IQueryType, Class<? extends IQueryEngine>>();
		IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(IQueryType.EXTENSION_ID);
		for (IConfigurationElement e : config) {	
			if (e.getName().equals(QUERYTYPEGROUP_EXTNAME)){
				String id = e.getAttribute("id"); //$NON-NLS-1$
				String name = e.getAttribute("name"); //$NON-NLS-1$
				String help = e.getAttribute("html_description"); //$NON-NLS-1$
				QueryCategory g = new QueryCategory(id, name, help);
				groups.add(g);
			}else if (e.getName().equals(QUERYTYPE_EXTNAME)){
				try {
					IQueryType qType = (IQueryType) e.createExecutableExtension("class"); //$NON-NLS-1$
					
					//check ccaa status
					boolean isValid = false;
					if (SmartDB.getCurrentConservationArea() != null && SmartDB.isMultipleAnalysis()){
						if (qType.supportsCrossCaQueries()){
							isValid = true;
									
						}
					}else{
						if (qType.supportsSingleCaQueries()){
							isValid = true;
						}
					}
					executors.put(qType, (Class<? extends IQueryEngine>)e.createExecutableExtension("executor").getClass()); //$NON-NLS-1$
					if (isValid){
						aTypes.add(qType);
					
						String id = e.getAttribute(QUERYTYPEGROUP_EXTNAME);
						for (QueryCategory g : groups){
							if (g.getId().equals(id)){
								g.addQueryType(qType);
							}
						}
					}
				} catch (CoreException e1) {
					QueryPlugIn.log(Messages.QueryTypeManager_QueryTypeError, e1);
				}
			}			
		}
		//remove any groups without any types
		for (Iterator<QueryCategory> iterator = groups.iterator(); iterator.hasNext();) {
			QueryCategory category = (QueryCategory) iterator.next();
			if (category.getTypes() == null || category.getTypes().size() == 0){
				iterator.remove();
			}
			
		}
		this.allTypes = aTypes.toArray(new IQueryType[aTypes.size()]);
	}
	
	
	/**
	 * Reads the query type extension point
	 * and loads all query types
	 * 
	 * @return
	 */
	private void readTypeConfigurations() {
		getAllQueryTypes();
		
		List<QueryTypeWrapper> ltypes = new ArrayList<QueryTypeWrapper>();
		IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(IQueryType.EXTENSION_ID);
		
		for (IConfigurationElement e : config) {
			 if (!e.getName().equals(QUERYTYPE_EXTNAME)){
				 //not a query type so we don't care
				 continue;
			 }
			try {
				IQueryType tmp = (IQueryType) e.createExecutableExtension("class"); //$NON-NLS-1$
				IQueryType qType = null;
				for (IQueryType qt: allTypes ){
					if (qt.getKey().equals(tmp.getKey())){
						qType = qt;
						break;
					}
				}
				
				if (qType == null){
					//only log message here if it makes sense
					if (SmartDB.isMultipleAnalysis() && tmp.supportsCrossCaQueries()){
						QueryPlugIn.log("Query type " + tmp.getKey() + " not found.", null); //$NON-NLS-1$ //$NON-NLS-2$
					}else if (!SmartDB.isMultipleAnalysis() && tmp.supportsSingleCaQueries()){
						QueryPlugIn.log("Query type " + tmp.getKey() + " not found.", null); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}else{
				
					QueryTypeWrapper wrapper = new QueryTypeWrapper(qType);
					IConfigurationElement defs[] = e.getChildren("DefinitionPanel"); //$NON-NLS-1$
					for (IConfigurationElement def : defs){
						String id = def.getAttribute("id"); //$NON-NLS-1$

						if (DefinitionPanelManager.isValid(id)){
							wrapper.definitionPanels.add(id);
					
							String pid = def.getAttribute("itemPanelId"); //$NON-NLS-1$
							wrapper.itemPanels.put(id, pid);
						}
					}
					ltypes.add(wrapper);
				}
			} catch (CoreException e1) {
				QueryPlugIn.log(Messages.QueryTypeManager_QueryTypeError, e1);
			}
		}
		this.types = ltypes;
	}
	
	private List<QueryTypeWrapper> getTypeConfigurations(){
		if (types == null){
			readTypeConfigurations();
		}
		return types;
	}
	/*
	 * Determines which query types
	 * are allowed for the current configuration.
	 * 
	 */
	private void findSupportedType(){	
		boolean isSingle = SmartDB.getCurrentConservationArea() == null || !SmartDB.isMultipleAnalysis();
		List<IQueryType> sTypes = new ArrayList<IQueryType>();
		for (IQueryType qType: getAllQueryTypes()){
			boolean add = false;
			if (!isSingle && qType.supportsCrossCaQueries()){
				add = true;
			}else if (isSingle && qType.supportsSingleCaQueries()){
				add = true;
			}
			if (add){
				sTypes.add(qType);
			}
		}
		this.supportedTypes = sTypes.toArray(new IQueryType[sTypes.size()]);
	}


	private class QueryTypeWrapper{
		
		IQueryType queryType;
		List<String> definitionPanels;
		Map<String,String> itemPanels;
		
		public QueryTypeWrapper(IQueryType type){
			this.queryType = type;
			definitionPanels= new ArrayList<String>();
			itemPanels = new HashMap<String,String>();
		}
	}
}
