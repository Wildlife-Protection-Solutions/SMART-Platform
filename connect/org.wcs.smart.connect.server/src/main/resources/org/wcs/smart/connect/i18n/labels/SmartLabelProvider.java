/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.i18n.labels;

import java.text.MessageFormat;
import java.util.Locale;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.connect.i18n.Messages;

/**
 * The SMART label provide must provide implementations for:
 * Boolean objects(both true and false) and for
 * Employee object (the full name and id).
 * 
 * @author Emily
 *
 */
public class SmartLabelProvider implements ICoreLabelProvider {

	@Override
	public String getLabel(Object value, Locale l) {
		if (value instanceof Boolean){
			if ((Boolean)value){
				return Messages.getString("SmartLabelProvider.BooleanYesOp",l); //$NON-NLS-1$
			}else{
				return Messages.getString("SmartLabelProvider.BooleanNoOp",l); //$NON-NLS-1$
			}
		}else if (value instanceof Employee){
			return getFullName((Employee) value, l);
		}
		
		if (value.equals(AGENCY_NAME_KEY)) return "Agency";
		if (value.equals(RANK_NAME_KEY)) return "Rank";
		if (value.equals(EMP_AGENCY_KEY)) return "Agency";
		if (value.equals(EMP_RANK_KEY)) return "Rank";
		if (value.equals(EMP_GENDER_KEY)) return "Gender";
		if (value.equals(EMP_ID_KEY)) return "ID";
		if (value.equals(EMP_BIRTHDATE_KEY)) return "Birth Date";
		if (value.equals(EMP_EMPLOYEMENT_DATE_KEY)) return "Conservation Area Start";
		if (value.equals(EMP_EMPLOYEMENT_ENDDATE_KEY)) return "Employment End";
		if (value.equals(EMP_SMART_USER_KEY)) return "SMART Username";
		if (value.equals(EMP_SMART_USER_LEVEL_KEY)) return "SMART User Level";
		if (value.equals(EMP_DATE_CREATED_KEY)) return "Date Created";
		if (value.equals(EMP_IS_ACTIVE_KEY)) return "Active";
		if (value.equals(EMP_FAMILY_NAME_KEY)) return "Family Name(s)";
		if (value.equals(EMP_GIVEN_NAME_KEY)) return "Given Name(s)";
		if (value.equals(STATION_ID_KEY)) return "ID";
		if (value.equals(STATION_NAME_KEY)) return "Name";
		if (value.equals(STATION_ACTIVE_KEY)) return "Active";
		if (value.equals(STATION_DESCRIPTION_KEY)) return "Description";
		if (value.equals(CA_NAME_KEY)) return "Conservation Area";	
		if (value.equals(AGENCY_RANK_TABLENAME_KEY)) return "Agencies and Ranks";
		if (value.equals(CA_ID_KEY)) return "ID";
		if (value.equals(CA_DESCRIPTION_KEY)) return "Description";
		if (value.equals(CA_DESIGNATION_KEY)) return "Designation";
		if (value.equals(CA_TABLENAME_KEY)) return "Conservation Areas";
		if (value.equals(EMPLOYEE_TABLENAME_KEY)) return "Employees";
		if (value.equals(STATION_TABLENAME_KEY)) return "Stations";
		return null;
	}
	
	public static String getFullName(Employee e, Locale l){
		return MessageFormat.format(Messages.getString("SmartLabelProvider.EmployeeNameFormat_0Give_1Family",l), e.getGivenName(), e.getFamilyName()); //$NON-NLS-1$
	}
}
