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
package org.wcs.smart.reporttable.ca;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.reporttable.internal.Messages;

/**
 * BIRT Table extensions that access the current conservation
 * areas in the database.
 * 
 * @author Emily
 *
 */
public class CaTable extends SmartBirtTable {

	private enum Column{
		ID(Messages.CaTable_IDColumnName, java.sql.Types.VARCHAR),
		NAME(Messages.CaTable_NameColumnName,java.sql.Types.VARCHAR),
		DESCRIPTION(Messages.CaTable_DescriptionColumnName, java.sql.Types.VARCHAR),
		DESIGNATION(Messages.CaTable_DesignationColumnName, java.sql.Types.VARCHAR);
		
		private String name;
		private int type;
		
		private Column(String name, int type){
			this.name = name;
			this.type = type;
		}
		public String getName(){
			return this.name;
		}
		public int getType(){
			return this.type;
		}
		
		public Object getValue(ConservationArea e){
			switch(this){
			case NAME:
				return e.getName();
			case ID:
				return e.getId();
			case DESCRIPTION:
				return e.getDescription();
			case DESIGNATION:
				return e.getDesignation();
			}
			return null;
		}
	}
	
	private Column[] activeColumns;
	
	
	/**
	 * Creates a new station table
	 */
	public CaTable() {
		super(Messages.CaTable_TableName);
		this.activeColumns = Column.values();
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		String[] name = new String[activeColumns.length];
		for (int i = 0; i < activeColumns.length; i ++){
			name[i] = activeColumns[i].getName();
		}
		return name;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getColumnTypes()
	 */
	@Override
	public int[] getColumnTypes() {
		int[] name = new int[activeColumns.length];
		for (int i = 0; i < activeColumns.length; i ++){
			name[i] = activeColumns[i].getType();
		}
		return name;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValues(org.wcs.smart.ca.ConservationArea)
	 */
	@Override
	public List<Object> getValues (Collection<ConservationArea> cas) {
		List<Object> values = new ArrayList<Object>();
		values.addAll(cas);
		return values;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValue(java.lang.Object, int)
	 */
	@Override
	public Object getValue(Object object, int index) {
		return activeColumns[index].getValue((ConservationArea)object);
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#openQuery()
	 */
	@Override
	public void openQuery() {
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#closeQuery()
	 */
	@Override
	public void closeQuery() {

	}

}
