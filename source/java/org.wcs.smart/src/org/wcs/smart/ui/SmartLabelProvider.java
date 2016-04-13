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
package org.wcs.smart.ui;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.UUID;

import javax.persistence.Transient;

import org.hibernate.Session;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * The SMART label provide must provide implementations for:
 * Boolean objects(both true and false) and for
 * Employee object (the full name and id).
 * 
 * @author Emily
 *
 */
public class SmartLabelProvider implements ICoreLabelProvider {

	
	public static final String BOOLEAN_TRUE_LABEL = Messages.Attribute_BooleanAttribute_True_Label;
	public static final String BOOLEAN_FALSE_LABEL = Messages.Attribute_BooleanAttribute_False_Label;
	public static final String AGENCY_NAME = Messages.Agency_AgencyName;
	public static final String RANK_NAME = Messages.Rank_Label;
	public static final String EMP_AGENCY = Messages.Employee_Agency_Label;
	public static final String EMP_RANK = Messages.Employee_Rank_Label;
	public static final String EMP_GENDER = Messages.Employee_Gender_Label;
	public static final String EMP_ID = Messages.Employee_Id_Label;
	public static final String EMP_BIRTHDATE = Messages.Employee_Birthdate_Label;
	public static final String EMP_EMPLOYEMENT_DATE = Messages.Employee_CAStartDate_Label;
	public static final String EMP_EMPLOYEMENT_ENDDATE = Messages.Employee_EndDate_Label;
	public static final String EMP_SMART_USER = Messages.Employee_SmartUser_Label;
	public static final String EMP_SMART_USER_LEVEL = Messages.Employee_SmartUserLevel_Label;
	public static final String EMP_DATE_CREATED = Messages.Employee_DateCreated_Label;
	public static final String EMP_IS_ACTIVE = Messages.Employee_IsActive_Label;
	public static final String EMP_FAMILY_NAME = Messages.Employee_FamilyName_Label;
	public static final String EMP_GIVEN_NAME = Messages.Employee_GiveName_Label;
	public static final String STATION_ID = Messages.Station_Id_Label;
	public static final String STATION_NAME = Messages.Station_Name_Label;
	public static final String STATION_DESCRIPTION = Messages.Station_Description_Label;

	@Override
	public String getLabel(Object value, Locale l) {
		if (value instanceof Boolean){
			if ((Boolean)value){
				return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
			}else{
				return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
			}
		}else if (value instanceof Employee){
			return SmartLabelProvider.getFullLabel((Employee)value);
		}else if (value instanceof AreaType){
			return getAreaTypeName((AreaType)value);
		}
		if (value.equals(GEOMETRY_LABEL)) return "Geometry";
		if (value.equals(AGENCY_NAME_KEY)) return Messages.Agency_AgencyName;
		if (value.equals(RANK_NAME_KEY)) return Messages.Rank_Label;
		if (value.equals(EMP_AGENCY_KEY)) return Messages.Employee_Agency_Label;
		if (value.equals(EMP_RANK_KEY)) return Messages.Employee_Rank_Label;
		if (value.equals(EMP_GENDER_KEY)) return Messages.Employee_Gender_Label;
		if (value.equals(EMP_ID_KEY)) return Messages.Employee_Id_Label;
		if (value.equals(EMP_BIRTHDATE_KEY)) return Messages.Employee_Birthdate_Label;
		if (value.equals(EMP_EMPLOYEMENT_DATE_KEY)) return Messages.Employee_CAStartDate_Label;
		if (value.equals(EMP_EMPLOYEMENT_ENDDATE_KEY)) return Messages.Employee_EndDate_Label;
		if (value.equals(EMP_SMART_USER_KEY)) return Messages.Employee_SmartUser_Label;
		if (value.equals(EMP_SMART_USER_LEVEL_KEY)) return Messages.Employee_SmartUserLevel_Label;
		if (value.equals(EMP_DATE_CREATED_KEY)) return Messages.Employee_DateCreated_Label;
		if (value.equals(EMP_IS_ACTIVE_KEY)) return Messages.Employee_IsActive_Label;
		if (value.equals(EMP_FAMILY_NAME_KEY)) return Messages.Employee_FamilyName_Label;
		if (value.equals(EMP_GIVEN_NAME_KEY)) return Messages.Employee_GiveName_Label;
		if (value.equals(STATION_ID_KEY)) return Messages.Station_Id_Label;
		if (value.equals(STATION_NAME_KEY)) return Messages.Station_Name_Label;
		if (value.equals(STATION_DESCRIPTION_KEY)) return Messages.Station_Description_Label;
		if (value.equals(STATION_ACTIVE_KEY)) return Messages.SmartLabelProvider_StationActiveColumnName;
		if (value.equals(CA_NAME_KEY)) return Messages.SmartLabelProvider_CaColumnName;
		if (value.equals(AGENCY_RANK_TABLENAME_KEY)) return Messages.SmartLabelProvider_AgencyRankColumnName;
		if (value.equals(CA_ID_KEY)) return Messages.SmartLabelProvider_CaIdColumnName;
		if (value.equals(CA_DESCRIPTION_KEY)) return Messages.SmartLabelProvider_DescriptionColumnName;
		if (value.equals(CA_DESIGNATION_KEY)) return Messages.SmartLabelProvider_DesignationColumnName;
		if (value.equals(CA_TABLENAME_KEY)) return Messages.SmartLabelProvider_CaTableName;
		if (value.equals(EMPLOYEE_TABLENAME_KEY)) return Messages.SmartLabelProvider_EmployeeTableName;
		if (value.equals(STATION_TABLENAME_KEY)) return Messages.SmartLabelProvider_StationsTableName;
		return null;
	}

	/**
	 * Loads the label for the given element from the database. Looks for the
	 * language in the conservation area than matches the code of the current
	 * language. If not found uses the default language of the provided ca.
	 * 
	 * @param elementuuid
	 *            the element uuid
	 * @param cauuid
	 *            the uuid the element is associated with
	 * @return
	 */
	@Transient
	public static synchronized String getDescription(UUID elementuuid,
			UUID cauuid) {
		String description = ""; //$NON-NLS-1$
		if (elementuuid == null || cauuid == null) {
			return description;
		}

		Label.LabelItemPK id = new Label.LabelItemPK();
		UuidItem h = new UuidItem();
		h.setUuid(elementuuid);
		id.setElement(h);

		for (ConservationArea c : SmartDB.getConservationAreaConfiguration().getConservationAreas()) {
			if (c.getUuid().equals(cauuid)) {
				Language l = SmartUtils.findLanguageMatch(c.getLanguages());
				if (l != null) {
					id.setLanguage(l);
				} else {
					id.setLanguage(c.getDefaultLanguage());
				}
				break;
			}
		}

		if (id.getLanguage() == null) {
			return description;
		}

		Session s = HibernateManager.openSession();
		Label lbl = (Label) s.get(Label.class, id);
		if (lbl != null) {
			description = lbl.getValue();
		}
		return description;
	}

	public static String getAreaTypeName(AreaType areatype){
		switch(areatype){
			case CA: return Messages.Area_CaBoundaryArea_Name; 
			case BA: return Messages.Area_BufferedManagementArea_Name;
			case ADMIN: return Messages.Area_AdminArea_Name;
			case MNGT: return Messages.Area_ManagementArea_Name;
			case PATRL: return Messages.Area_PatrolSectorArea_Name;
		}
		return null;
	}

	/**
	 * Formats a name & id for display on the UI.
	 * 
	 * @param givenName given name
	 * @param familyName family name
	 * @param id id
	 * @return
	 */
	public static String formatName(String givenName, String familyName, String id){
		return MessageFormat.format(Messages.Employee_LongLabel_0Given_1Family_2Id, new Object[]{givenName, familyName, id});
	}

	/**
	 * Formats a name for display on the UI.
	 * 
	 * @param givenName given name
	 * @param familyName family name
	 * @return
	 */
	public static String formatName(String givenName, String familyName){
		return MessageFormat.format(Messages.Employee_ShortLabel_0Given_1Family, new Object[]{givenName, familyName});
	}

	/**
	 * 
	 * @return the employee short label which does not include the id; only the given and family names
	 */
	public static String getShortLabel(Employee e){
		return SmartLabelProvider.formatName(e.getGivenName(), e.getFamilyName());
	}

	/**
	 * 
	 * @return the employee long name label which includes given name, family name and id
	 */
	public static String getFullLabel(Employee e){
		return SmartLabelProvider.formatName(e.getGivenName(), e.getFamilyName(), e.getId());
	}

}
