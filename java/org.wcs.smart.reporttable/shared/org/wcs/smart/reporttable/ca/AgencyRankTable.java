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

import java.util.List;
import java.util.Locale;

import org.hibernate.Query;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;

/**
 * Wrapper to convert agency/rank objects
 * to a BIRT table data source.
 * 
 * @author Emily
 *
 */
public class AgencyRankTable  extends SmartBirtTable {

	private enum Column{
		CA(ICoreLabelProvider.CA_NAME_KEY, "Conservation Area", java.sql.Types.VARCHAR), //$NON-NLS-1$
		AGENCY(ICoreLabelProvider.AGENCY_NAME_KEY, "Agency Name", java.sql.Types.VARCHAR), //$NON-NLS-1$
		RANK(ICoreLabelProvider.RANK_NAME_KEY, "Rank", java.sql.Types.VARCHAR); //$NON-NLS-1$
		
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
		
		public Object getValue(Rank rank){
			switch(this){
			case AGENCY:
				return rank.getAgency().getName();
			case RANK:
				return rank.getName();
			case CA:
				return rank.getAgency().getConservationArea().getNameLabel();
			}
			return null;
		}
	}
	
	private Column[] activeColumns;
	
	/**
	 * Creates a new agency/rank birt table source
	 */
	public AgencyRankTable() {
		super("Agencies and Rank"); //$NON-NLS-1$
	}

	private void initColumns(SmartConnection connection){
		if (activeColumns == null){
			if (connection.getConservationAreas().size() > 1){
				this.activeColumns = Column.values();	
			}else{
				this.activeColumns = new Column[]{Column.AGENCY, Column.RANK};	
			}
		}
	}
	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getColumnNames()
	 */
	@Override
	public String[] getColumnNames(SmartConnection connection) {
		initColumns(connection);
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
		initColumns(connection);
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
		initColumns(connection);
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
	public List<Object> getValues (SmartConnection connection) {
		String sql = "FROM Rank r WHERE r.agency.conservationArea in (:ca)"; //$NON-NLS-1$
		Query q  = connection.getSession().createQuery(sql);
		q.setParameterList("ca", connection.getConservationAreas()); //$NON-NLS-1$
		
		return q.list();
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValue(java.lang.Object, int)
	 */
	@Override
	public Object getValue(Object object, int index, SmartConnection connection) {
		return activeColumns[index].getValue((Rank)object);
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
		return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(ICoreLabelProvider.AGENCY_RANK_TABLENAME_KEY, l);
	}

	@Override
	public String getTableShortName(Locale l) {
		return getTableFullName(l);
	}

}
