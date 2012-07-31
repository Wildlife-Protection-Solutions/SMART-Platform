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
package org.wcs.smart;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Station;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.hibernate.HibernateManager;
/**
 * Wrapper to convert station objects
 * to a BIRT table data source.
 * 
 * @author Emily
 *
 */
public class StationTable  extends SmartBirtTable {

	private enum Column{
		NAME("Station Name",java.sql.Types.VARCHAR),
		DESCRIPTION("Description", java.sql.Types.VARCHAR),
		ACTIVE("Active", java.sql.Types.BOOLEAN);
		
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
		
		public Object getValue(Station e){
			switch(this){
			case NAME:
				return e.getName();
			case DESCRIPTION:
				return e.getDescription();
			case ACTIVE:
				return e.getIsActive();
			}
			return null;
		}
	}
	
	private Session session = null;
	
	/**
	 * Creates a new station table
	 */
	public StationTable() {
		super("Stations");
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		String[] name = new String[Column.values().length];
		for (int i = 0; i < Column.values().length; i ++){
			name[i] = Column.values()[i].getName();
		}
		return name;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getColumnTypes()
	 */
	@Override
	public int[] getColumnTypes() {
		int[] name = new int[Column.values().length];
		for (int i = 0; i < Column.values().length; i ++){
			name[i] = Column.values()[i].getType();
		}
		return name;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValues(org.wcs.smart.ca.ConservationArea)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Object> getValues(ConservationArea ca) {
		return session.createCriteria(Station.class).add(Restrictions.eq("conservationArea", ca)).list();
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValue(java.lang.Object, int)
	 */
	@Override
	public Object getValue(Object object, int index) {
		return Column.values()[index].getValue((Station)object);
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#openQuery()
	 */
	@Override
	public void openQuery() {
		session = HibernateManager.openSession();
		session.beginTransaction();
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#closeQuery()
	 */
	@Override
	public void closeQuery() {
		session.getTransaction().commit();
		session.close();

	}

}

