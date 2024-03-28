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

import org.hibernate.Session;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.DataModelMergeAndUpdater;
import org.wcs.smart.ca.datamodel.DataModelMerger;
import org.wcs.smart.ca.datamodel.SimpleDataModel;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.internal.ca.datamodel.xml.IXmlToDataModelConverter;
import org.wcs.smart.internal.ca.datamodel.xml.XmlDataModelImporter;
import org.wcs.smart.internal.ca.datamodel.xml.XmlDataModelValidator;
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
	public static final String KEY_NAME = Messages.SmartLabelProvider_KeyName;
	public static final String RANK_NAME = Messages.Rank_Label;
	public static final String EMP_AGENCY = Messages.Employee_Agency_Label;
	public static final String EMP_RANK = Messages.Employee_Rank_Label;
	public static final String EMP_TEAMS = Messages.SmartLabelProvider_TeamsColumnLabel;
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
	
	public static final String POLYGON_ATTRIBUTE_LAYER = Messages.SmartLabelProvider_PolygonLayer;
	public static final String LINESTRING_ATTRIBUTE_LAYER = Messages.SmartLabelProvider_LineStringLayer;

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
		}else if (value instanceof Attribute.GeometrySource src) {
			switch(src) {
				case GPS_MANUAL: return Messages.SmartLabelProvider_GeometrySourceGPS;
				case GPS_AUTO: return Messages.SmartLabelProvider_GeometrySourceGPSAuto;
				case MANUAL_DRAW: return Messages.SmartLabelProvider_GeometrySourceManualDraw;
				case MANUAL_POINT: return Messages.SmartLabelProvider_GeometrySourceManualPoint;
				case UNKNOWN: return Messages.SmartLabelProvider_GeometrySourceUnknown;		
			}
		}
		if (value.equals(GEOMETRY_LABEL)) return Messages.SmartLabelProvider_GeometryLabel;
		if (value.equals(AGENCY_NAME_KEY)) return Messages.Agency_AgencyName;
		if (value.equals(AGENCY_KEY_KEY)) return KEY_NAME;
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
		
		if (value.equals(CA_ORGANIZATION_KEY)) return Messages.SmartLabelProvider_Organizationcolumnname;
		if (value.equals(CA_POINTOFCONTACT_KEY)) return Messages.SmartLabelProvider_pointofcontactcolumnname;
		if (value.equals(CA_COUNTRY_KEY)) return Messages.SmartLabelProvider_countrycolumnname;
		if (value.equals(CA_OWNER_KEY)) return Messages.SmartLabelProvider_ownercolumnname;
		
		if (value.equals(AREA_KM2_KEY)) return Messages.SmartLabelProvider_Area;
		if (value.equals(PERIMETER_KM_KEY)) return Messages.SmartLabelProvider_Perimeter;
		if (value.equals(POLYGON_KEY)) return Messages.SmartLabelProvider_Polygon;
		if (value.equals(LINESTRING_KEY)) return Messages.SmartLabelProvider_LineString;
		
		if (value.equals(POLYGON_GEOM_ATTRIBUTE_LABEL)) return Messages.SmartLabelProvider_PolygonLabel;
		if (value.equals(LINESTRING_GEOM_ATTRIBUTE_LABEL)) return Messages.SmartLabelProvider_LineStringLabel;

		if (value.equals(DataModelMergeAndUpdater.I18NMessages.ATT_NOT_FOUND)) return Messages.SmartLabelProvider_MergeDmAttNotFound;
		if (value.equals(DataModelMergeAndUpdater.I18NMessages.ATT_TYPE_MISMATCH)) return Messages.SmartLabelProvider_MergeDmTypeMismatch;
		if (value.equals(DataModelMergeAndUpdater.I18NMessages.MERGE_ATT_TASKNAME)) return Messages.SmartLabelProvider_MergeDmProcessingAtt;
		if (value.equals(DataModelMergeAndUpdater.I18NMessages.MERGE_CAT_TASKNAME)) return Messages.SmartLabelProvider_MergeDmProcessingCats;
		if (value.equals(DataModelMergeAndUpdater.I18NMessages.MERGE_TASKNAME)) return Messages.SmartLabelProvider_MergeDmProcessingDms;
		
		if (value.equals(IXmlToDataModelConverter.I18NMessages.ATTRIBUTE_NOT_FOUND_ERROR)) return Messages.SmartLabelProvider_AttributeNotFound;
		if (value.equals(IXmlToDataModelConverter.I18NMessages.ATTRIBUTE_TYPE_NOT_SUPPORTED)) return Messages.SmartLabelProvider_AttributeTypeNotFound;
		
		if (value.equals(SimpleDataModel.I18nMessages.KEY_INVALID_CHARS)) return Messages.DataModel_Error_Key_InvalidCharacters;
		if (value.equals(SimpleDataModel.I18nMessages.KEY_KEYWORD)) return Messages.DataModel_KeywordKeyError;
		if (value.equals(SimpleDataModel.I18nMessages.KEY_NOT_UNIQUE)) return Messages.DataModel_Error_Key_NotUnique;
		if (value.equals(SimpleDataModel.I18nMessages.KEY_REQUIRED)) return Messages.DataModel_Error_Key_NotEmpty;
		if (value.equals(SimpleDataModel.I18nMessages.KEY_TO_LONG)) return Messages.DataModel_Error_Key_ToLong;
		if (value.equals(SimpleDataModel.I18nMessages.NAME_INVALID)) return Messages.SmartLabelProvider_DmNameRequired;
		if (value.equals(SimpleDataModel.I18nMessages.NAME_REQUIRED)) return  Messages.DataModel_NameRequired;
		
		if (value.equals(XmlDataModelValidator.I18NMessages.INVALID_KEY)) return Messages.XmlDataModelValidator_InvalidKey;
		if (value.equals(XmlDataModelValidator.I18NMessages.INVALID_NAME)) return Messages.XmlDataModelValidator_InvalidName;
		
		if (value.equals(XmlDataModelImporter.I18NMessages.NO_DM_XML_FOUND)) return Messages.SmartLabelProvider_DataModelXmlNotFound;
		if (value.equals(XmlDataModelImporter.I18NMessages.INVALID_XML)) return Messages.SmartLabelProvider_InvalidXmlFile;
		
		if (value.equals(DataModelMerger.ProgressMessages.LOADING)) return Messages.DataModelMerger_SubTask1;
		if (value.equals(DataModelMerger.ProgressMessages.TASKNAME)) return Messages.DataModelMerger_TaskName;
		if (value.equals(DataModelMerger.ProgressMessages.MERGINGATTRIBUTES)) return Messages.DataModelMerger_SubTask2;
		if (value.equals(DataModelMerger.ProgressMessages.MERGINGCATEGORIES)) return Messages.DataModelMerger_SubTask3;
						
		if (value.equals(AREA_TABLENAME_KEY)) return Messages.SmartLabelProvider_AreasTableName;
		
		if (value.equals(AREATABLE_CAID_KEY)) return Messages.SmartLabelProvider_areacaidcolumnname;
		if (value.equals(AREATABLE_CANAME_KEY)) return Messages.SmartLabelProvider_areacanamecolumnname;
		if (value.equals(AREATABLE_NAME_KEY)) return Messages.SmartLabelProvider_areaareaname;
		if (value.equals(AREATABLE_KEY_KEY)) return Messages.SmartLabelProvider_areakeyname;
		if (value.equals(AREATABLE_AREA_KEY)) return Messages.SmartLabelProvider_areaareamname;
		if (value.equals(AREATABLE_GEOMETRY_KEY)) return Messages.SmartLabelProvider_areageometryname;
		
		if (value == Operator.AND) return Messages.SmartLabelProvider_AndOperator;
		if (value == Operator.BETWEEN) return Messages.SmartLabelProvider_BetweenOperator;
		if (value == Operator.BRACKETS) return "(  )"; //$NON-NLS-1$
		if (value == Operator.EQUALS) return "="; //$NON-NLS-1$
		if (value == Operator.GREATERTHAN) return ">"; //$NON-NLS-1$
		if (value == Operator.GREATERTHANEQUALS) return ">="; //$NON-NLS-1$
		if (value == Operator.LESSTHAN) return "<"; //$NON-NLS-1$
		if (value == Operator.LESSTHANEQUALS) return "<="; //$NON-NLS-1$
		if (value == Operator.NOT) return Messages.SmartLabelProvider_NotOperator;
		if (value == Operator.NOTEQUALS) return "!="; //$NON-NLS-1$
		if (value == Operator.NOT_BETWEEN) return Messages.SmartLabelProvider_NotBetweenOperator;
		if (value == Operator.OR) return Messages.SmartLabelProvider_OrOperator;
		if (value == Operator.STR_CONTAINS) return Messages.SmartLabelProvider_ContainsOperator;
		if (value == Operator.STR_EQUALS) return Messages.SmartLabelProvider_EqualsOperator;
		if (value == Operator.STR_NOTCONTAINS) return Messages.SmartLabelProvider_NotContainsOperator;
		if (value == Operator.EXACT) return Messages.SmartLabelProvider_ExactOperator;
		
		if (value == IFilter.NULL_OP) return Messages.SmartLabelProvider_NoneFilterOption;
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
	public static synchronized String getDescription(UUID elementuuid,
			UUID cauuid, Session session) {
		String description = ""; //$NON-NLS-1$
		if (elementuuid == null || cauuid == null) {
			return description;
		}

		Label.LabelItemPK id = new Label.LabelItemPK();
		id.setElement(new UuidItem(elementuuid));
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

		Label lbl = (Label) session.get(Label.class, id);
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
	@Override
	public String getEmployeeShortLabel(Employee e, Locale l){
		return getShortLabel(e);
	}

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

	public String getAttributeTypeLabel(Attribute.AttributeType type, Locale l) {
		switch(type) {
			case BOOLEAN: return Messages.SmartLabelProvider_BooleanAttributeType;
			case DATE: return Messages.SmartLabelProvider_DateAttributeType;
			case LIST: return Messages.SmartLabelProvider_ListAttributeType;
			case MLIST: return Messages.SmartLabelProvider_MultiListAttributeType;
			case NUMERIC: return Messages.SmartLabelProvider_NumericAttributeType;
			case TEXT: return Messages.SmartLabelProvider_TextAttributeType;
			case TREE: return Messages.SmartLabelProvider_TreeAttributeType;
			case POLYGON: return Messages.SmartLabelProvider_PolygonAttributeType;
			case LINE: return Messages.SmartLabelProvider_LineAttributeType;
		}
		return ""; //$NON-NLS-1$
	}
}
