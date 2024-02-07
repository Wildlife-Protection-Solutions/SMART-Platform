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
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.DataModelMergeAndUpdater;
import org.wcs.smart.ca.datamodel.DataModelMerger;
import org.wcs.smart.ca.datamodel.SimpleDataModel;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.internal.ca.datamodel.xml.IXmlToDataModelConverter;
import org.wcs.smart.internal.ca.datamodel.xml.XmlDataModelImporter;
import org.wcs.smart.internal.ca.datamodel.xml.XmlDataModelValidator;

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
		if (value instanceof AreaType){
			switch((AreaType)value){
				case CA: return Messages.getString("SmartLabelProvider.CaAreaName", l);  //$NON-NLS-1$
				case BA: return Messages.getString("SmartLabelProvider.BufferedAreaName", l); //$NON-NLS-1$
				case ADMIN: return Messages.getString("SmartLabelProvider.AdminAreaName", l); //$NON-NLS-1$
				case MNGT: return Messages.getString("SmartLabelProvider.MgtAreaName", l); //$NON-NLS-1$
				case PATRL: return Messages.getString("SmartLabelProvider.PatrolAreaName", l); //$NON-NLS-1$
				default: return ((AreaType) value).name();
			}
		}
		if (value instanceof Attribute.GeometrySource src) {
			switch(src) {
				case GPS: return Messages.getString("SmartLabelProvider.SourceGPS", l); //$NON-NLS-1$
				case MANUAL_DRAW: return Messages.getString("SmartLabelProvider.SourceDrawnMap", l); //$NON-NLS-1$
				case MANUAL_POINT: return Messages.getString("SmartLabelProvider.SourceInput", l); //$NON-NLS-1$
				case UNKNOWN: return Messages.getString("SmartLabelProvider.SourceUnknown", l);		 //$NON-NLS-1$
			}
		}
		
		if (value.equals(GEOMETRY_LABEL)) return Messages.getString("SmartLabelProvider.GeometryColumnLabel", l); //$NON-NLS-1$
		if (value.equals(AGENCY_NAME_KEY)) return Messages.getString("SmartLabelProvider.AgencyName", l); //$NON-NLS-1$
		if (value.equals(AGENCY_KEY_KEY)) return Messages.getString("SmartLabelProvider.KeyName", l); //$NON-NLS-1$
		if (value.equals(RANK_NAME_KEY)) return Messages.getString("SmartLabelProvider.RankName", l); //$NON-NLS-1$
		if (value.equals(EMP_AGENCY_KEY)) return Messages.getString("SmartLabelProvider.EmployeeAgencyName", l); //$NON-NLS-1$
		if (value.equals(EMP_RANK_KEY)) return Messages.getString("SmartLabelProvider.EmployeeRankName", l); //$NON-NLS-1$
		if (value.equals(EMP_GENDER_KEY)) return Messages.getString("SmartLabelProvider.EmployeeGender", l); //$NON-NLS-1$
		if (value.equals(EMP_ID_KEY)) return Messages.getString("SmartLabelProvider.EmployeeId", l); //$NON-NLS-1$
		if (value.equals(EMP_BIRTHDATE_KEY)) return Messages.getString("SmartLabelProvider.EmployeeBirthdate", l); //$NON-NLS-1$
		if (value.equals(EMP_EMPLOYEMENT_DATE_KEY)) return Messages.getString("SmartLabelProvider.EmployeeCaStart", l); //$NON-NLS-1$
		if (value.equals(EMP_EMPLOYEMENT_ENDDATE_KEY)) return Messages.getString("SmartLabelProvider.EmployeeCaEnd", l); //$NON-NLS-1$
		if (value.equals(EMP_SMART_USER_KEY)) return Messages.getString("SmartLabelProvider.Employeeusername", l); //$NON-NLS-1$
		if (value.equals(EMP_SMART_USER_LEVEL_KEY)) return Messages.getString("SmartLabelProvider.EmployeeUserlevel", l); //$NON-NLS-1$
		if (value.equals(EMP_DATE_CREATED_KEY)) return Messages.getString("SmartLabelProvider.EmployeeDateCreated", l); //$NON-NLS-1$
		if (value.equals(EMP_IS_ACTIVE_KEY)) return Messages.getString("SmartLabelProvider.EmployeeActive", l); //$NON-NLS-1$
		if (value.equals(EMP_FAMILY_NAME_KEY)) return Messages.getString("SmartLabelProvider.EmployeeFamily", l); //$NON-NLS-1$
		if (value.equals(EMP_GIVEN_NAME_KEY)) return Messages.getString("SmartLabelProvider.EmployeeGiven", l); //$NON-NLS-1$
		if (value.equals(STATION_ID_KEY)) return Messages.getString("SmartLabelProvider.StationId", l); //$NON-NLS-1$
		if (value.equals(STATION_NAME_KEY)) return Messages.getString("SmartLabelProvider.StationName", l); //$NON-NLS-1$
		if (value.equals(STATION_ACTIVE_KEY)) return Messages.getString("SmartLabelProvider.StationActive", l); //$NON-NLS-1$
		if (value.equals(STATION_DESCRIPTION_KEY)) return Messages.getString("SmartLabelProvider.StationDescription", l); //$NON-NLS-1$
		if (value.equals(CA_NAME_KEY)) return Messages.getString("SmartLabelProvider.CaName", l);	 //$NON-NLS-1$
		if (value.equals(AGENCY_RANK_TABLENAME_KEY)) return Messages.getString("SmartLabelProvider.AgenciesAndRanksTable", l); //$NON-NLS-1$
		if (value.equals(CA_ID_KEY)) return Messages.getString("SmartLabelProvider.CaId", l); //$NON-NLS-1$
		if (value.equals(CA_DESCRIPTION_KEY)) return Messages.getString("SmartLabelProvider.CaDescription", l); //$NON-NLS-1$
		if (value.equals(CA_DESIGNATION_KEY)) return Messages.getString("SmartLabelProvider.CaDesignation", l); //$NON-NLS-1$
		if (value.equals(CA_TABLENAME_KEY)) return Messages.getString("SmartLabelProvider.CaTableName", l); //$NON-NLS-1$
		if (value.equals(EMPLOYEE_TABLENAME_KEY)) return Messages.getString("SmartLabelProvider.EmployeeTableName", l); //$NON-NLS-1$
		if (value.equals(STATION_TABLENAME_KEY)) return Messages.getString("SmartLabelProvider.StationsTableName", l); //$NON-NLS-1$
		
		if (value.equals(CA_ORGANIZATION_KEY)) return Messages.getString("SmartLabelProvider.CaOrganization", l); //$NON-NLS-1$
		if (value.equals(CA_POINTOFCONTACT_KEY)) return Messages.getString("SmartLabelProvider.CaPointOfContact", l); //$NON-NLS-1$
		if (value.equals(CA_COUNTRY_KEY)) return Messages.getString("SmartLabelProvider.CaCountry", l); //$NON-NLS-1$
		if (value.equals(CA_OWNER_KEY)) return Messages.getString("SmartLabelProvider.CaOwner", l); //$NON-NLS-1$
		
		if (value.equals(AREA_KM2_KEY)) return Messages.getString("SmartLabelProvider.Area", l); //$NON-NLS-1$
		if (value.equals(PERIMETER_KM_KEY)) return Messages.getString("SmartLabelProvider.Perimeter", l); //$NON-NLS-1$
		if (value.equals(POLYGON_KEY)) return Messages.getString("SmartLabelProvider.Polygon", l); //$NON-NLS-1$
		if (value.equals(LINESTRING_KEY)) return Messages.getString("SmartLabelProvider.LineString", l); //$NON-NLS-1$
		
		if (value.equals(POLYGON_GEOM_ATTRIBUTE_LABEL)) return Messages.getString("SmartLabelProvider.PolygonLabel", l); //$NON-NLS-1$
		if (value.equals(LINESTRING_GEOM_ATTRIBUTE_LABEL)) return Messages.getString("SmartLabelProvider.LineStringLabel", l); //$NON-NLS-1$

		if (value.equals(DataModelMergeAndUpdater.I18NMessages.ATT_NOT_FOUND)) return Messages.getString("SmartLabelProvider.MergeDmAttributeNotFound", l); //$NON-NLS-1$
		if (value.equals(DataModelMergeAndUpdater.I18NMessages.ATT_TYPE_MISMATCH)) return Messages.getString("SmartLabelProvider.MergeDmTypeDifference",l ); //$NON-NLS-1$
		if (value.equals(DataModelMergeAndUpdater.I18NMessages.MERGE_ATT_TASKNAME)) return Messages.getString("SmartLabelProvider.MergeDmAttributesProgress", l); //$NON-NLS-1$
		if (value.equals(DataModelMergeAndUpdater.I18NMessages.MERGE_CAT_TASKNAME)) return Messages.getString("SmartLabelProvider.MergeDmCategoriesProgress", l); //$NON-NLS-1$
		if (value.equals(DataModelMergeAndUpdater.I18NMessages.MERGE_TASKNAME)) return Messages.getString("SmartLabelProvider.MergeDmProgress", l); //$NON-NLS-1$
		
				
		if (value.equals(IXmlToDataModelConverter.I18NMessages.ATTRIBUTE_NOT_FOUND_ERROR)) return Messages.getString("SmartLabelProvider.DmXmlAttributeNotFound", l); //$NON-NLS-1$
		if (value.equals(IXmlToDataModelConverter.I18NMessages.ATTRIBUTE_TYPE_NOT_SUPPORTED)) return Messages.getString("SmartLabelProvider.DmXmlAttributeTypeNotSupported", l); //$NON-NLS-1$
		
		if (value.equals(XmlDataModelImporter.I18NMessages.NO_DM_XML_FOUND)) return Messages.getString("SmartLabelProvider.DataModelXmlNotFound", l); //$NON-NLS-1$
		if (value.equals(XmlDataModelImporter.I18NMessages.INVALID_XML)) return Messages.getString("SmartLabelProvider.InvalidFile", l); //$NON-NLS-1$
		
		
		if (value.equals(SimpleDataModel.I18nMessages.KEY_INVALID_CHARS)) return Messages.getString("SmartLabelProvider.DmValidateInvalidChars", l); //$NON-NLS-1$
		if (value.equals(SimpleDataModel.I18nMessages.KEY_KEYWORD)) return Messages.getString("SmartLabelProvider.DmValidateCannotContainKeyword", l); //$NON-NLS-1$
		if (value.equals(SimpleDataModel.I18nMessages.KEY_NOT_UNIQUE)) return Messages.getString("SmartLabelProvider.DmValidateKeyNotUnique", l); //$NON-NLS-1$
		if (value.equals(SimpleDataModel.I18nMessages.KEY_REQUIRED)) return Messages.getString("SmartLabelProvider.DmValidateKeyRequired", l); //$NON-NLS-1$
		if (value.equals(SimpleDataModel.I18nMessages.KEY_TO_LONG)) return Messages.getString("SmartLabelProvider.DmValidateKeyToLong", l); //$NON-NLS-1$
		if (value.equals(SimpleDataModel.I18nMessages.NAME_INVALID)) return Messages.getString("SmartLabelProvider.DmValidateInvalidName", l); //$NON-NLS-1$
		if (value.equals(SimpleDataModel.I18nMessages.NAME_REQUIRED)) return Messages.getString("SmartLabelProvider.DmValidateNameRequired", l); //$NON-NLS-1$
		
		if (value.equals(XmlDataModelValidator.I18NMessages.INVALID_KEY)) return Messages.getString("SmartLabelProvider.XmlDmValidatorInvalidKey", l); //$NON-NLS-1$
		if (value.equals(XmlDataModelValidator.I18NMessages.INVALID_NAME)) return Messages.getString("SmartLabelProvider.XmlDmValidatorInvalidName", l); //$NON-NLS-1$
		
		if (value.equals(XmlDataModelImporter.I18NMessages.NO_DM_XML_FOUND)) return Messages.getString("SmartLabelProvider.XmlNotFound", l); //$NON-NLS-1$
		if (value.equals(XmlDataModelImporter.I18NMessages.INVALID_XML)) return Messages.getString("SmartLabelProvider.InvalidXml", l); //$NON-NLS-1$
		
		if (value.equals(DataModelMerger.ProgressMessages.LOADING)) return Messages.getString("SmartLabelProvider.LoadingStatus", l); //$NON-NLS-1$
		if (value.equals(DataModelMerger.ProgressMessages.TASKNAME)) return Messages.getString("SmartLabelProvider.MergingStatus", l); //$NON-NLS-1$
		if (value.equals(DataModelMerger.ProgressMessages.MERGINGATTRIBUTES)) return Messages.getString("SmartLabelProvider.MergingAttributesStatus", l); //$NON-NLS-1$
		if (value.equals(DataModelMerger.ProgressMessages.MERGINGCATEGORIES)) return Messages.getString("SmartLabelProvider.MergingCategoryStatus", l); //$NON-NLS-1$
		
		if (value.equals(AREA_TABLENAME_KEY)) return Messages.getString("SmartLabelProvider.AreaTable", l); //$NON-NLS-1$
		
		if (value.equals(AREATABLE_CAID_KEY)) return Messages.getString("SmartLabelProvider.AreaCaIdColumn", l); //$NON-NLS-1$
		if (value.equals(AREATABLE_CANAME_KEY)) return Messages.getString("SmartLabelProvider.AreaCaNameColumn", l); //$NON-NLS-1$
		if (value.equals(AREATABLE_NAME_KEY)) return Messages.getString("SmartLabelProvider.AreaNameColumn", l); //$NON-NLS-1$
		if (value.equals(AREATABLE_KEY_KEY)) return Messages.getString("SmartLabelProvider.AreaKeyColumn", l); //$NON-NLS-1$
		if (value.equals(AREATABLE_AREA_KEY)) return Messages.getString("SmartLabelProvider.AreaAreamColumn", l); //$NON-NLS-1$
		if (value.equals(AREATABLE_GEOMETRY_KEY)) return Messages.getString("SmartLabelProvider.AreaGeometryColumn", l); //$NON-NLS-1$
		
		if (value instanceof Operator){
			switch((Operator)value){
				case EQUALS:{ return "=";} //$NON-NLS-1$
				case LESSTHAN:{ return "<";} //$NON-NLS-1$
				case LESSTHANEQUALS:{ return "<=";} //$NON-NLS-1$
				case GREATERTHAN:{ return ">";} //$NON-NLS-1$
				case GREATERTHANEQUALS:{ return ">=";} //$NON-NLS-1$
				case NOTEQUALS:{ return "!=";} //$NON-NLS-1$
				case STR_EQUALS:{ return Messages.getString("OperatorLabelProvider.equalsLabel", l);} //$NON-NLS-1$
				case STR_CONTAINS:{ return Messages.getString("OperatorLabelProvider.containsLabel", l);} //$NON-NLS-1$
				case STR_NOTCONTAINS:{ return Messages.getString("OperatorLabelProvider.notContains", l);} //$NON-NLS-1$
				case BETWEEN:{ return Messages.getString("OperatorLabelProvider.BetweenLabel", l);} //$NON-NLS-1$
				case NOT_BETWEEN:{ return Messages.getString("OperatorLabelProvider.notBetweenLabel", l);} //$NON-NLS-1$
				case AND:{ return Messages.getString("OperatorLabelProvider.AndLabel", l);} //$NON-NLS-1$
				case OR:{ return Messages.getString("OperatorLabelProvider.OrLabel", l);} //$NON-NLS-1$
				case NOT:{ return Messages.getString("OperatorLabelProvider.NotLabel", l);} //$NON-NLS-1$
				case BRACKETS:{ return "( )"; } //$NON-NLS-1$
				case EXACT: return Messages.getString("OperatorLabelProvider.ExactOperator", l); //$NON-NLS-1$
			}
		}
		
		if (value == IFilter.NULL_OP) return Messages.getString("SmartLabelProvider.NoneOp", l); //$NON-NLS-1$
		
		return null;
	}
	
	public static String getFullName(Employee e, Locale l){
		return MessageFormat.format(Messages.getString("SmartLabelProvider.EmployeeNameFormat_0Give_1Family",l), e.getGivenName(), e.getFamilyName()); //$NON-NLS-1$
	}

	@Override
	public String getEmployeeShortLabel(Employee e, Locale l) {
		return getFullName(e, l);
	}

	@Override
	public String getAttributeTypeLabel(AttributeType type, Locale l) {
		switch(type) {
			case BOOLEAN: return Messages.getString("SmartLabelProvider.BooleanAttType",l); //$NON-NLS-1$
			case DATE: return Messages.getString("SmartLabelProvider.DateAttType",l); //$NON-NLS-1$
			case LIST: return Messages.getString("SmartLabelProvider.ListAttType",l); //$NON-NLS-1$
			case MLIST: return Messages.getString("SmartLabelProvider.MultiListAttType",l); //$NON-NLS-1$
			case NUMERIC: return Messages.getString("SmartLabelProvider.NumericAttType",l); //$NON-NLS-1$
			case TEXT: return Messages.getString("SmartLabelProvider.TextAttType",l); //$NON-NLS-1$
			case TREE: return Messages.getString("SmartLabelProvider.TreeAttType",l); //$NON-NLS-1$
			case POLYGON: return Messages.getString("SmartLabelProvider.PolygonAttType", l); //$NON-NLS-1$
			case LINE: return Messages.getString("SmartLabelProvider.LineAttType", l); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}
}
