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
import java.util.Collections;
import java.util.List;

import org.wcs.smart.data.oda.smart.impl.table.IDynamicSmartTables;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.entity.EntityHibernateManager;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

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
	public List<SmartBirtTable> getTables() {
		if (SmartDB.isMultipleAnalysis()){
			return Collections.emptyList();
		}
		//find all entities
		List<SmartBirtTable> tables = new ArrayList<SmartBirtTable>();
		for (EntityType e : EntityHibernateManager.getEntityTypes(HibernateManager.openSession())){
			SmartBirtTable table = new EntityTable(e);
			tables.add(table);
		}
		
		return tables;
	}

}
