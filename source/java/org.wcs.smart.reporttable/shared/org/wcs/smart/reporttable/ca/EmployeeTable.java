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

import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;

/**
 * Wrapper to convert employee objects
 * to a BIRT table data source.
 * 
 * @author Emily
 *
 */
public class EmployeeTable extends SmartBirtTable {

	private enum EmployeeColumn{
		CA(ICoreLabelProvider.CA_NAME_KEY, "Conservation Area", java.sql.Types.VARCHAR),	 //$NON-NLS-1$
		ID(ICoreLabelProvider.EMP_ID_KEY, "Id", java.sql.Types.VARCHAR), //$NON-NLS-1$
		GIVENNAME(ICoreLabelProvider.EMP_GIVEN_NAME_KEY, "Given Name", java.sql.Types.VARCHAR), //$NON-NLS-1$
		FAMILYNAME(ICoreLabelProvider.EMP_FAMILY_NAME_KEY, "Family Name", java.sql.Types.VARCHAR), //$NON-NLS-1$
		START_DATE(ICoreLabelProvider.EMP_EMPLOYEMENT_DATE_KEY, "Start Employment Date",java.sql.Types.DATE), //$NON-NLS-1$
		END_DATE(ICoreLabelProvider.EMP_EMPLOYEMENT_ENDDATE_KEY, "End Employment Date",java.sql.Types.DATE), //$NON-NLS-1$
		BIRTH_DATE(ICoreLabelProvider.EMP_BIRTHDATE_KEY, "Birth Date", java.sql.Types.DATE), //$NON-NLS-1$
		GENDER(ICoreLabelProvider.EMP_GENDER_KEY,"Gender",java.sql.Types.CHAR), //$NON-NLS-1$
		SMARTUSERID(ICoreLabelProvider.EMP_SMART_USER_KEY, "SMART User Id", java.sql.Types.VARCHAR), //$NON-NLS-1$
		SMARTUSERLEVEL(ICoreLabelProvider.EMP_SMART_USER_LEVEL_KEY, "SMART User Levels", java.sql.Types.VARCHAR), //$NON-NLS-1$
		AGENCY(ICoreLabelProvider.EMP_AGENCY_KEY, "Agency", java.sql.Types.VARCHAR), //$NON-NLS-1$
		RANK(ICoreLabelProvider.EMP_RANK_KEY, "Rank", java.sql.Types.VARCHAR); //$NON-NLS-1$
		
		private String name;
		private int type;
		private String key;
		
		private EmployeeColumn(String key, String name, int type){
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
				if (e.getSmartUserLevelKeys() != null){
					return e.getSmartUserLevelKeys();
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
		super("Employees"); //$NON-NLS-1$

		
	}

	private void initColumns(SmartConnection connection){
		if (activeColumns != null) return;
		if (connection.getConservationAreas().size() > 1){
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
		return connection.getSession().createCriteria(Employee.class)
				.add(Restrictions.in("conservationArea", connection.getConservationAreas())) //$NON-NLS-1$
				.list(); 
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValue(java.lang.Object, int)
	 */
	@Override
	public Object getValue(Object object, int index, SmartConnection connection) {
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

	@Override
	public String getTableFullName(Locale l) {
		return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(ICoreLabelProvider.EMPLOYEE_TABLENAME_KEY, l);
	}

	@Override
	public String getTableShortName(Locale l) {
		return getTableFullName(l);
	}
}
