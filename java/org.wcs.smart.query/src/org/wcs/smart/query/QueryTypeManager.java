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
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.ui.definition.DefinitionPanelManager;
/**
 * Manages the query type extension. Loads all query types
 * and provides support functions for finding query types.
 * @author Emily
 *
 */
public class QueryTypeManager {

	private static QueryTypeManager instance = null;
	private IQueryType[] supportedTypes;
	private List<QueryTypeWrapper> types;
	
	private QueryTypeManager(){
		
	}
	
	/**
	 * 
	 * @return the query type manager instance
	 */
	public static QueryTypeManager getInstance(){
		if (instance == null){
			instance = new QueryTypeManager();
		}
		return instance;
	}

	/**
	 * Finds the query type for the given type key.
	 * @param typeKey
	 * @return
	 */
	public IQueryType findQueryType(String typeKey){
		for (IQueryType type : getSupportedQueryTypes()){
			if (type.getKey().equals(typeKey)){
				return type;
			}
		}
		return null;
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
		if (supportedTypes == null){
			getSupportedQueryTypes();
		}
		for (QueryTypeWrapper q : types){
			if (q.queryType.equals(qType)){
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
		if (supportedTypes == null){
			getSupportedQueryTypes();
		}
		for (QueryTypeWrapper q : types){
			if (q.queryType.equals(qType)){
				return q.definitionPanels.toArray(new String[q.definitionPanels.size()]);
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @return array of all supported query types
	 */
	public IQueryType[] getSupportedQueryTypes() {
		if (this.supportedTypes != null){
			return this.supportedTypes;
		}
	
		boolean isSingle = !SmartDB.isMultipleAnalysis();
		
		List<IQueryType> sTypes = new ArrayList<IQueryType>();
		types = new ArrayList<QueryTypeWrapper>();
		
		IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(IQueryType.EXTENSION_ID);
		for (IConfigurationElement e : config) {
			IQueryType qType;
			try {
				qType = (IQueryType) e.createExecutableExtension("class");
								
				boolean add = false;
				if (!isSingle && qType.supportsCrossCaQueries()){
					add = true;
				}else if (isSingle && qType.supportsSingleCaQueries()){
					add = true;
				}
				if (add){
					QueryTypeWrapper wrapper = new QueryTypeWrapper(qType);
					IConfigurationElement defs[] = e.getChildren("DefinitionPanel");
					for (IConfigurationElement def : defs){
						String id = def.getAttribute("id");

						if (DefinitionPanelManager.getInstance().isValid(id)){
							wrapper.definitionPanels.add(id);
						
							String pid = def.getAttribute("itemPanelId");
							wrapper.itemPanels.put(id, pid);
						}
					}
					sTypes.add(qType);
					types.add(wrapper);
				}
			} catch (CoreException e1) {
				QueryPlugIn.log("Error reading query type extension points", e1);
			}
			
		}
		this.supportedTypes = sTypes.toArray(new IQueryType[sTypes.size()]);
		return this.supportedTypes;

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
