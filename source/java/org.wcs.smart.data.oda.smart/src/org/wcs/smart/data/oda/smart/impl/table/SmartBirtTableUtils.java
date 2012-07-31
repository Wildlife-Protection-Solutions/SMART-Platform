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
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

/**
 * Utilities for reading extension point information
 * for the SmartBirtTable extension point.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SmartBirtTableUtils {
	/**
	 * Extension point id
	 */
	public static final String REPORT_MAPPING_ID = "org.wcs.smart.data.ods.smart.table";
	
	/**
	 * @return list of all SmartBirtTable extension point implementations
	 * @throws Exception
	 */
	public static final List<SmartBirtTable> getBirtTables() throws Exception{
		List<SmartBirtTable> items = new ArrayList<SmartBirtTable>();
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(REPORT_MAPPING_ID);
		for (int i = 0; i < config.length; i ++){
			items.add((SmartBirtTable)config[i].createExecutableExtension("SmartBirtTable"));
		}
		return items;
	}
	
	/**
	 * For a given table name finds the associated extension point.
	 * 
	 * @param tableName tablename to find.
	 * @return SmartBirtTable with given tablename
	 * @throws Exception
	 */
	public static final SmartBirtTable findTable(String tableName) throws Exception{
		if (Platform.getExtensionRegistry() == null) return null;
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(REPORT_MAPPING_ID);
		for (int i = 0; i < config.length; i ++){
			
			SmartBirtTable table = (SmartBirtTable)config[i].createExecutableExtension("SmartBirtTable");
			if (table.getTableName().equals(tableName)){
				return table;
			}
		}
		return null;
		
	}
	
}
