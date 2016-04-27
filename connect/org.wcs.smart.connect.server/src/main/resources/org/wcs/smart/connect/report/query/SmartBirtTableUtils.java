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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.impl.table.IDynamicSmartTables;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;

/**
 * Utilities for reading extension point information
 * for the SmartBirtTable extension point.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SmartBirtTableUtils {
	
	
	private List<SmartBirtTable> tables;
	/**
	 * Extension point id
	 */
	public static final String REPORT_MAPPING_ID = "org.wcs.smart.data.oda.smart.table"; //$NON-NLS-1$
	
	public SmartBirtTableUtils(){
		
	}
	
	/**
	 * For a given table name finds the associated extension point.
	 * 
	 * @param tableName tablename to find.
	 * @return SmartBirtTable with given tablename
	 * @throws Exception
	 */
	public SmartBirtTable findTable(String tableName, SmartConnection connection) throws Exception{
		if (tables == null){
			initTables(connection);
		}
		for (SmartBirtTable t : tables){
			if (t.getTableKey().equals(tableName)){
				return t;
			}
		}
		return null;
	}
		
	private void initTables(SmartConnection connection) throws Exception{
		
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtensionPoint pnt = registry.getExtensionPoint(REPORT_MAPPING_ID);
		IConfigurationElement[] config = pnt.getConfigurationElements();
		tables = new ArrayList<SmartBirtTable>();
		
		for (IConfigurationElement e : config) {
			if (e.getName().equals("StaticTable")) { //$NON-NLS-1$
				
				SmartBirtTable table = (SmartBirtTable) e.createExecutableExtension("class"); //$NON-NLS-1$
				tables.add(table);
			} else if (e.getName().equals("DynamicTable")) { //$NON-NLS-1$
				IDynamicSmartTables dtables = (IDynamicSmartTables) e.createExecutableExtension("class"); //$NON-NLS-1$
				for (SmartBirtTable table : dtables.getTables(connection)){
					tables.add(table);
				}
			}
		}
	}

}
