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
package org.wcs.smart.reporttable.patrol;

import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.ui.LabelConstants;
import org.wcs.smart.reporttable.internal.Messages;

/**
 * Wrapper to convert patrol transport type objects
 * to a BIRT table data source.
 * 
 * @author Emily
 *
 */
public class PatrolTransportTable extends SmartBirtTable {

	private enum Column{
		CA(Messages.PatrolTransportTable_Ca_FieldName,"Conservation Area",java.sql.Types.VARCHAR), //$NON-NLS-1$
		NAME(Messages.PatrolTransportTable_TransportType_FieldName,"Transport Type",java.sql.Types.VARCHAR), //$NON-NLS-1$
		ACTIVE(Messages.PatrolTransportTable_IsActive_FieldName, "Active", java.sql.Types.BOOLEAN), //$NON-NLS-1$
		PATROL_TYPE(Messages.PatrolTransportTable_PatrolType_FieldName, "Patrol Type", java.sql.Types.VARCHAR); //$NON-NLS-1$
		
		private String name;
		private int type;
		private String label;
		
		private Column(String label, String name, int type){
			this.name = name;
			this.type = type;
			this.label = label;
		}
		public String getName(){
			return this.name;
		}
		public String getLabel(){
			return this.label;
		}
		public int getType(){
			return this.type;
		}
		
		public Object getValue(PatrolTransportType e){
			switch(this){
			case NAME:
				return e.getName();
			case ACTIVE:
				return e.getIsActive();
			case PATROL_TYPE:
				return LabelConstants.getLabel(e.getPatrolType());
			case CA:
				return e.getConservationArea().getNameLabel();
			}
			return null;
		}
	}
	
//	private Session session = null;
	private Column[] activeColumns;
	
	/**
	 * Creates a nw patrol transport type table
	 */
	public PatrolTransportTable() {
		super(Messages.PatrolTransportTable_TableName, "Patrol Transport Types"); //$NON-NLS-1$
		if (SmartDB.isMultipleAnalysis()){
			this.activeColumns = Column.values();
		}else{
			this.activeColumns = new Column[]{Column.NAME, Column.PATROL_TYPE, Column.ACTIVE};
		}
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
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getColumnLabels()
	 */
	@Override
	public String[] getColumnLabels() {
		String[] name = new String[activeColumns.length];
		for (int i = 0; i < activeColumns.length; i ++){
			name[i] = activeColumns[i].getLabel();
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
	@SuppressWarnings("unchecked")
	@Override
	public List<Object> getValues (Collection<ConservationArea> cas, Session session) {
		return session.createCriteria(PatrolTransportType.class).add(Restrictions.in("conservationArea", cas)).list(); //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValue(java.lang.Object, int)
	 */
	@Override
	public Object getValue(Object object, int index) {
		return activeColumns[index].getValue((PatrolTransportType)object);
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
