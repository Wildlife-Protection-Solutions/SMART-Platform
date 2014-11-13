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
package org.wcs.smart.data.oda.smart.impl.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Utilities for reading extension point information
 * for the SmartBirtTable extension point.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SmartBirtTableUtils {
	
	private static SmartBirtTableUtils instance = null;
	
	/**
	 * Extension point id
	 */
	public static final String REPORT_MAPPING_ID = "org.wcs.smart.data.oda.smart.table"; //$NON-NLS-1$
	
	private Map<TableCategory, List<SmartBirtTable>> cachedStatic = null;
	private Map<TableCategory, IDynamicSmartTables> cachedDynamic = null;
	
	public static SmartBirtTableUtils getInstance(){
		if (instance == null){
			instance = new SmartBirtTableUtils();
		}
		return instance;
	}
	/**
	 * @return list of all SmartBirtTable extension point implementations
	 * @throws Exception
	 */
	public Map<TableCategory, List<SmartBirtTable>> getBirtTables() throws Exception{
		if (Platform.getExtensionRegistry() == null) return Collections.emptyMap();
		if (cachedDynamic == null){
			populateCache();
		}
		
		HashMap<TableCategory, List<SmartBirtTable>> items = new HashMap<TableCategory, List<SmartBirtTable>>();
		for (Entry<TableCategory, IDynamicSmartTables> e : cachedDynamic.entrySet()){
			items.put(e.getKey(), e.getValue().getTables());
		}
		items.putAll(cachedStatic);
		
		return items;
	}
	
	/**
	 * For a given table name finds the associated extension point.
	 * 
	 * @param tableName tablename to find.
	 * @return SmartBirtTable with given tablename
	 * @throws Exception
	 */
	public SmartBirtTable findTable(String tableName) throws Exception{
		if (cachedDynamic == null){
			populateCache();
		}
		
		for (List<SmartBirtTable> values : cachedStatic.values()){
			for (SmartBirtTable bt : values){
				if (bt.getTableKey().equals(tableName)){
					return bt;
				}
			}
		}
		for (IDynamicSmartTables dt : cachedDynamic.values()){
			for (SmartBirtTable bt : dt.getTables()){
				if (bt.getTableKey().equals(tableName)){
					return bt;
				}
			}
		}
		return null;
		
	}
	
	private void populateCache() throws CoreException{
		// read categories first
		HashMap<String, TableCategory> categories = new HashMap<String, TableCategory>();
		IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(REPORT_MAPPING_ID);
		for (int i = 0; i < config.length; i++) {
			if (config[i].getName().equals("TableCategory")) { //$NON-NLS-1$
				String id = config[i].getAttribute("id"); //$NON-NLS-1$
				String name = config[i].getAttribute("name"); //$NON-NLS-1$
				String icon = config[i].getAttribute("icon"); //$NON-NLS-1$

				ImageDescriptor idd  = null;
				if (icon != null){
					idd = AbstractUIPlugin.imageDescriptorFromPlugin(config[i].getNamespaceIdentifier(), icon);
				}
				TableCategory tc = new TableCategory(id, name, idd);
				categories.put(id, tc);
			}
		}

		// read static and dynamic tables
		cachedDynamic = new HashMap<TableCategory, IDynamicSmartTables>();
		cachedStatic = new HashMap<TableCategory, List<SmartBirtTable>>();
		for (int i = 0; i < config.length; i++) {
			boolean canAdd = true;
			if (SmartDB.isMultipleAnalysis()){
				canAdd = Boolean.valueOf(config[i].getAttribute("supportsCcaa")); //$NON-NLS-1$
			}
			if (!canAdd) continue;
			
			if (config[i].getName().equals("StaticTable")) { //$NON-NLS-1$
				
				TableCategory tc = categories.get(config[i].getAttribute("category")); //$NON-NLS-1$
				SmartBirtTable table = (SmartBirtTable) config[i].createExecutableExtension("class"); //$NON-NLS-1$

				List<SmartBirtTable> tt = (List<SmartBirtTable>) cachedStatic.get(tc);
				if (tt == null) {
					tt = new ArrayList<SmartBirtTable>();
					cachedStatic.put(tc, tt);
				}
				tt.add(table);

			} else if (config[i].getName().equals("DynamicTable")) { //$NON-NLS-1$
				TableCategory tc = categories.get(config[i].getAttribute("category")); //$NON-NLS-1$
				IDynamicSmartTables dtables = (IDynamicSmartTables) config[i].createExecutableExtension("class"); //$NON-NLS-1$
				cachedDynamic.put(tc, dtables);
			}
		}
	}
	
}
