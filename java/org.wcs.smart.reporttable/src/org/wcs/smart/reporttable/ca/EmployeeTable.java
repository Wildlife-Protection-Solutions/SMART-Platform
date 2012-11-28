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

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.reporttable.internal.Messages;

/**
 * Wrapper to convert employee objects
 * to a BIRT table data source.
 * 
 * @author Emily
 *
 */
public class EmployeeTable extends SmartBirtTable {

	private enum EmployeeColumn{
		
		ID(Messages.EmployeeTable_Id_FieldName,java.sql.Types.VARCHAR),
		GIVENNAME(Messages.EmployeeTable_GiveName_FieldName, java.sql.Types.VARCHAR),
		FAMILYNAME(Messages.EmployeeTable_FamilyName_FieldName, java.sql.Types.VARCHAR),
		START_DATE(Messages.EmployeeTable_StartDate_FieldName, java.sql.Types.DATE),
		END_DATE(Messages.EmployeeTable_EndDate_FieldName, java.sql.Types.DATE),
		BIRTH_DATE(Messages.EmployeeTable_BirthDate_FieldName, java.sql.Types.DATE),
		GENDER(Messages.EmployeeTable_Gender_FieldName, java.sql.Types.CHAR),
		SMARTUSERID(Messages.EmployeeTable_UserId_FieldName, java.sql.Types.VARCHAR),
		SMARTUSERLEVEL(Messages.EmployeeTable_UserLevel_FieldName, java.sql.Types.VARCHAR),
		AGENCY(Messages.EmployeeTable_Agency_FieldName, java.sql.Types.VARCHAR),
		RANK(Messages.EmployeeTable_Rank_FieldName, java.sql.Types.VARCHAR);
		
		private String name;
		private int type;
		
		private EmployeeColumn(String name, int type){
			this.name = name;
			this.type = type;
		}
		public String getName(){
			return this.name;
		}
		public int getType(){
			return this.type;
		}
		
		public Object getValue(Employee e){
			switch(this){
			case ID:
				return e.getId();
			case GIVENNAME:
				return e.getGivenName();
			case FAMILYNAME:
				return e.getFamilyName();
			case START_DATE:
				return e.getStartEmploymentDate();
			case END_DATE:
				return e.getEndEmploymentDate();
			case BIRTH_DATE:
				return e.getBirthDate();
			case GENDER:
				return e.getGender();
			case SMARTUSERID:
				return e.getSmartUserId();
			case SMARTUSERLEVEL:
				if (e.getSmartUserLevel() != null){
					return e.getSmartUserLevel().name();
				}
				return ""; //$NON-NLS-1$
			case AGENCY:
				if (e.getAgency() == null){
					return ""; //$NON-NLS-1$
				}
				return e.getAgency().getName();
			case RANK:
				if (e.getRank() == null){
					return ""; //$NON-NLS-1$
				}
				return e.getRank().getName();
			}
			return null;
		}
	}
	
	private Session session = null;
	
	/**
	 * Create a new employee table
	 */
	public EmployeeTable() {
		super(Messages.EmployeeTable_TableName);
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		String[] name = new String[EmployeeColumn.values().length];
		for (int i = 0; i < EmployeeColumn.values().length; i ++){
			name[i] = EmployeeColumn.values()[i].getName();
		}
		return name;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getColumnTypes()
	 */
	@Override
	public int[] getColumnTypes() {
		int[] name = new int[EmployeeColumn.values().length];
		for (int i = 0; i < EmployeeColumn.values().length; i ++){
			name[i] = EmployeeColumn.values()[i].getType();
		}
		return name;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValues(org.wcs.smart.ca.ConservationArea)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Object> getValues(ConservationArea ca) {
		return session.createCriteria(Employee.class).add(Restrictions.eq("conservationArea", ca)).list(); //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValue(java.lang.Object, int)
	 */
	@Override
	public Object getValue(Object object, int index) {
		return EmployeeColumn.values()[index].getValue((Employee)object);
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
