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
package org.wcs.smart.entity.report;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.impl.table.IDynamicSmartTables;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.entity.model.EntityType;

/**
 * SMART Report Tables Extension that provides all entity types
 * as tables to the BIRT report interface. 
 * 
 * @author Emily
 *
 */
public class DynamicSmartTables implements IDynamicSmartTables {

	public DynamicSmartTables() {
	}

	@Override
	public List<SmartBirtTable> getTables(SmartConnection connection) {
		
		//TODO: CCAA MERGE ENTITIES -> for connect so cannot use this code
//		if (conservationArea.getIsCcaa()){
//			return EntityTypeCcaaManager.getInstance().getAllEntityTypes();
//		}
		Query q = connection.getSession().createQuery("FROM EntityType WHERE conservationArea IN ( :ca )"); //$NON-NLS-1$
		q.setParameterList("ca", connection.getConservationAreas()); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<EntityType> items = q.list();
		
		List<SmartBirtTable> tables = new ArrayList<SmartBirtTable>();
		for (EntityType et : items){
			SmartBirtTable table = new EntityTable(et);
			tables.add(table);
		}
		return tables;
	}

}
