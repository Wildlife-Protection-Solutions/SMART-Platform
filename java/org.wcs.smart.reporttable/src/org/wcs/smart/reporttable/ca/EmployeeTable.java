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

import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.hibernate.SmartDB;
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
		CA(Messages.EmployeeTable_Ca_FieldName, "Conservation Area", java.sql.Types.VARCHAR),	 //$NON-NLS-1$
		ID(Messages.EmployeeTable_Id_FieldName, "Id", java.sql.Types.VARCHAR), //$NON-NLS-1$
		GIVENNAME(Messages.EmployeeTable_GiveName_FieldName, "Given Name", java.sql.Types.VARCHAR), //$NON-NLS-1$
		FAMILYNAME(Messages.EmployeeTable_FamilyName_FieldName, "Family Name", java.sql.Types.VARCHAR), //$NON-NLS-1$
		START_DATE(Messages.EmployeeTable_StartDate_FieldName, "Start Employment Date",java.sql.Types.DATE), //$NON-NLS-1$
		END_DATE(Messages.EmployeeTable_EndDate_FieldName, "End Employment Date",java.sql.Types.DATE), //$NON-NLS-1$
		BIRTH_DATE(Messages.EmployeeTable_BirthDate_FieldName, "Birth Date", java.sql.Types.DATE), //$NON-NLS-1$
		GENDER(Messages.EmployeeTable_Gender_FieldName,"Gender",java.sql.Types.CHAR), //$NON-NLS-1$
		SMARTUSERID(Messages.EmployeeTable_UserId_FieldName, "SMART User Id", java.sql.Types.VARCHAR), //$NON-NLS-1$
		SMARTUSERLEVEL(Messages.EmployeeTable_UserLevel_FieldName, "SMART User Level", java.sql.Types.VARCHAR), //$NON-NLS-1$
		AGENCY(Messages.EmployeeTable_Agency_FieldName, "Agency", java.sql.Types.VARCHAR), //$NON-NLS-1$
		RANK(Messages.EmployeeTable_Rank_FieldName, "Rank", java.sql.Types.VARCHAR); //$NON-NLS-1$
		
		private String name;
		private int type;
		private String label;
		
		private EmployeeColumn(String label, String name, int type){
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
		
		public Object getValue(Employee e){
			switch(this){
			case CA:
				return e.getConservationArea().getNameLabel();
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
	
	private EmployeeColumn[] activeColumns;
	
	/**
	 * Create a new employee table
	 */
	public EmployeeTable() {
		super(Messages.EmployeeTable_TableName, "Employees", //$NON-NLS-1$
			SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EMPLOYEE_ICON));
		if (SmartDB.isMultipleAnalysis()){
			this.activeColumns = EmployeeColumn.values();
		}else{
			this.activeColumns = new EmployeeColumn[]{EmployeeColumn.ID,
					EmployeeColumn.GIVENNAME,
					EmployeeColumn.FAMILYNAME,
					EmployeeColumn.START_DATE,
					EmployeeColumn.END_DATE,
					EmployeeColumn.BIRTH_DATE,
					EmployeeColumn.GENDER,
					EmployeeColumn.SMARTUSERID,
					EmployeeColumn.SMARTUSERLEVEL,
					EmployeeColumn.AGENCY,
					EmployeeColumn.RANK};
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
		return session.createCriteria(Employee.class).add(Restrictions.in("conservationArea", cas)).list(); //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValue(java.lang.Object, int)
	 */
	@Override
	public Object getValue(Object object, int index) {
		return activeColumns[index].getValue((Employee)object);
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
