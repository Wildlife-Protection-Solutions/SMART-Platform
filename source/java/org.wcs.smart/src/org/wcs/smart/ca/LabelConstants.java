package org.wcs.smart.ca;

import java.text.MessageFormat;
import java.util.UUID;

import javax.persistence.Transient;

import org.hibernate.Session;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

public class LabelConstants {
	
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
	
	/**
	 * 
	 * @return the employee long name label which includes given name, family name and id
	 */
	public static String getFullLabel(Employee e){
		return formatName(e.getGivenName(), e.getFamilyName(), e.getId());
	}
	
	/**
	 * 
	 * @return the employee short label which does not include the id; only the given and family names
	 */
	public static String getShortLabel(Employee e){
		return formatName(e.getGivenName(), e.getFamilyName());
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
	 * Loads the label for the given element from the database.  Looks
	 * for the language in the conservation area than matches the code
	 * of the current language.  If not found uses the default
	 * language of the provided ca.
	 * 
	 * @param elementuuid the element uuid
	 * @param cauuid the uuid the element is associated with
	 * @return
	 */
	@Transient
	public static synchronized String getDescription(UUID elementuuid, UUID cauuid) {
		String description = ""; //$NON-NLS-1$
		if (elementuuid == null || cauuid == null){
			return description;
		}
		
		Label.LabelItemPK id = new Label.LabelItemPK();
		UuidItem h = new UuidItem();
		h.setUuid(elementuuid);
		id.setElement(h);
		
		for (ConservationArea c : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
//			if (Arrays.equals(c.getUuid(), cauuid)){
			if (c.getUuid().equals(cauuid)){
				Language l = SmartUtils.findLanguageMatch(c.getLanguages());
				if (l != null){
					id.setLanguage(l);
				}else{
					id.setLanguage(c.getDefaultLanguage());
				}
				break;
			}
		}

		if (id.getLanguage() == null){
			return description;
		}
		
		Session s = HibernateManager.openSession();
		Label lbl = (Label) s.get(Label.class, id);
		if (lbl != null) {
			description = lbl.getValue();
		}
		return description;
	}
}
