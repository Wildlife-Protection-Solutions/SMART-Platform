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


/**
 * Label provider for core SMART items.Must provide values
 * for Boolean.TRUE and Boolean.FALSE and Employee
 * 
 * @author Emily
 *
 */
public interface ICoreLabelProvider extends ISharedLabelProvider {

	public static final String CA_NAME_KEY = "conservationareanamekey"; //$NON-NLS-1$
	public static final String AGENCY_NAME_KEY = "agencynamekey"; //$NON-NLS-1$
	public static final String RANK_NAME_KEY = "ranknamekey"; //$NON-NLS-1$
	public static final String EMP_AGENCY_KEY = "employeeagencynamekey"; //$NON-NLS-1$
	public static final String EMP_RANK_KEY = "employeeranknamekey"; //$NON-NLS-1$
	public static final String EMP_GENDER_KEY = "employeegendernamekey"; //$NON-NLS-1$
	public static final String EMP_ID_KEY = "employeeidnamekey"; //$NON-NLS-1$
	public static final String EMP_BIRTHDATE_KEY = "employeebirthdatenamekey"; //$NON-NLS-1$
	public static final String EMP_EMPLOYEMENT_DATE_KEY = "employeedatekey"; //$NON-NLS-1$
	public static final String EMP_EMPLOYEMENT_ENDDATE_KEY = "employeeenddatekey"; //$NON-NLS-1$
	public static final String EMP_SMART_USER_KEY = "employeesmartuserkey"; //$NON-NLS-1$
	public static final String EMP_SMART_USER_LEVEL_KEY = "employeesmartuserlevelkey"; //$NON-NLS-1$
	public static final String EMP_DATE_CREATED_KEY = "employeedatecreatedkey"; //$NON-NLS-1$
	public static final String EMP_IS_ACTIVE_KEY = "employeeisactivekey"; //$NON-NLS-1$
	public static final String EMP_FAMILY_NAME_KEY = "employeefamilynamekey"; //$NON-NLS-1$
	public static final String EMP_GIVEN_NAME_KEY = "employeegivennamekey"; //$NON-NLS-1$
	public static final String STATION_ID_KEY = "employeestationidkey"; //$NON-NLS-1$
	public static final String STATION_NAME_KEY = "employeestationnamekey"; //$NON-NLS-1$
	public static final String STATION_ACTIVE_KEY = "employeestationnamekey"; //$NON-NLS-1$
	public static final String STATION_DESCRIPTION_KEY = "employeedescriptionkey"; //$NON-NLS-1$
	public static final String AGENCY_RANK_TABLENAME_KEY = "agencyranktablenamekey"; //$NON-NLS-1$
	public static final String CA_ID_KEY = "caidnamekey";
	public static final String CA_DESCRIPTION_KEY = "cadescriptionnamekey";
	public static final String CA_DESIGNATION_KEY = "cadesignationnamekey";
	public static final String CA_TABLENAME_KEY = "catablenamekey";
	public static final String EMPLOYEE_TABLENAME_KEY = "employeetablenamekey";
	public static final String STATION_TABLENAME_KEY = "stationtablenamekey";
}
