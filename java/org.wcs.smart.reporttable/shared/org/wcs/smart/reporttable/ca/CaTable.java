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
import java.util.List;
import java.util.Locale;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;

/**
 * BIRT Table extensions that access the current conservation
 * areas in the database.
 * 
 * @author Emily
 *
 */
public class CaTable extends SmartBirtTable {

	private enum Column{
		ID(ICoreLabelProvider.CA_ID_KEY, "ID", java.sql.Types.VARCHAR), //$NON-NLS-1$
		NAME(ICoreLabelProvider.CA_NAME_KEY, "Name", java.sql.Types.VARCHAR), //$NON-NLS-1$
		DESCRIPTION(ICoreLabelProvider.CA_DESCRIPTION_KEY, "Description", java.sql.Types.VARCHAR), //$NON-NLS-1$
		DESIGNATION(ICoreLabelProvider.CA_DESIGNATION_KEY, "Designation", java.sql.Types.VARCHAR); //$NON-NLS-1$
		
		private String name;
		private int type;
		private String key;
		
		private Column(String key, String name, int type){
			this.name = name;
			this.type = type;
			this.key = key;
		}
		public String getName(){
			return this.name;
		}
		public String getLabel(Locale l){
			return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(key, l);
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
		super("Conservation Areas"); //$NON-NLS-1$
		this.activeColumns = Column.values();
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getColumnNames()
	 */
	@Override
	public String[] getColumnNames(SmartConnection connection) {
		String[] name = new String[activeColumns.length];
		for (int i = 0; i < activeColumns.length; i ++){
			name[i] = activeColumns[i].getName();
		}
		return name;
	}
	
	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getColumnLabels()
	 */
	@Override
	public String[] getColumnLabels(SmartConnection connection) {
		String[] name = new String[activeColumns.length];
		for (int i = 0; i < activeColumns.length; i ++){
			name[i] = activeColumns[i].getLabel(connection.getCurrentLocale());
		}
		return name;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getColumnTypes()
	 */
	@Override
	public int[] getColumnTypes(SmartConnection connection) {
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
	public List<Object> getValues (SmartConnection connection) {
		List<Object> values = new ArrayList<Object>();
		values.addAll(connection.getConservationAreas());
		return values;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValue(java.lang.Object, int)
	 */
	@Override
	public Object getValue(Object object, int index, SmartConnection connection) {
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

	@Override
	public String getTableFullName(Locale l) {
		return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(ICoreLabelProvider.CA_TABLENAME_KEY, l);
	}

	@Override
	public String getTableShortName(Locale l) {
		return getTableFullName(l);
	}
}
