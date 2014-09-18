package org.wcs.smart.er.xml;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesign.State;
import org.wcs.smart.er.model.SurveyDesignProperty;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.xml.model.NamesType;
import org.wcs.smart.hibernate.SmartDB;


/**
 * Converts an xml Survey Design schema to an SMART SurveyDesign object.
 * 
 * @author Jeff
 *
 */

public class SurveyDesignFromXmlConverter {
	public static SurveyDesign fromXml(org.wcs.smart.er.xml.model.SurveyDesign xml, Session session) throws ParseException{
		
		
		
		//Read all mission properties and check if they exist, if not load them into the CA
		for (org.wcs.smart.er.xml.model.MissionProperty xmlprop : xml.getMissionProperty()){
			org.wcs.smart.er.xml.model.MissionAttribute xmlAttr = xmlprop.getMissionAttribute();
			//Test if this Mission Attribute exists, if not add it.
			
			//Probably should test all the list items as well to ensure they exist.
		}

		
		

		//create the Survey Java object 
		SurveyDesign surveyDesign = new SurveyDesign();
		 
		importNames(xml.getNames(), surveyDesign, session, true);
		//create the survey design properties
		ArrayList<SurveyDesignProperty> sdProperties = new ArrayList<SurveyDesignProperty>();
		for (org.wcs.smart.er.xml.model.SurveyDesignProperty xmlSDproperty : xml.getSurveyDesignProperty()){
			SurveyDesignProperty sdp = new SurveyDesignProperty();
			sdp.setName(xmlSDproperty.getName());
			sdp.setValue(xmlSDproperty.getValue());
			sdp.setDesign(surveyDesign);
			sdProperties.add(sdp);
		}
		surveyDesign.setProperties(sdProperties); //survey design properties		
		
		
		
		ArrayList<MissionProperty> missionProperties = new ArrayList<MissionProperty>();
		for (org.wcs.smart.er.xml.model.MissionProperty xmlproperty : xml.getMissionProperty()){
			MissionProperty mp = new MissionProperty();
			mp.setSurveyDesign(surveyDesign);
			mp.setOrder(xmlproperty.getOrder());
			missionProperties.add(mp);
			
			org.wcs.smart.er.xml.model.MissionAttribute xmlattr = xmlproperty.getMissionAttribute();
			
			MissionAttribute attr = new MissionAttribute();
			

			attr.setAttributeList(new ArrayList<MissionAttributeListItem>());
			
			for(org.wcs.smart.er.xml.model.MissionAttributeListItem xmlmali : xmlattr.getMissionAttributeListItem()){
				MissionAttributeListItem mali = new MissionAttributeListItem();
				mali.setAttribute(attr);
				mali.setKeyId(xmlmali.getKeyid());
				mali.setListOrder(xmlmali.getListOrder());
				mali.setName(xmlmali.getName());
				mali.setUuid(null);
				
				importNames(xmlmali.getNames(), mali, session, false);
				
				attr.getAttributeList().add(mali);
			}
			
			attr.setConservationArea(SmartDB.getCurrentConservationArea());
			attr.setKeyId(xmlattr.getKeyId());
			attr.setName(xmlattr.getName());
			
			importNames(xmlattr.getNames(), attr, session, false);
			
			String type = xmlattr.getAttributeType();
			if( type.equals(AttributeType.DATE.toString()) ){ 
				attr.setType(AttributeType.DATE);
			}else if( type.equals(AttributeType.BOOLEAN.toString()) ){
				attr.setType(AttributeType.BOOLEAN);
			}else if( type.equals(AttributeType.LIST.toString()) ){
				attr.setType(AttributeType.LIST);
			}else if( type.equals(AttributeType.NUMERIC.toString()) ){
				attr.setType(AttributeType.NUMERIC);
			}else if( type.equals(AttributeType.TEXT.toString()) ){
				attr.setType(AttributeType.TEXT);
			}else if( type.equals(AttributeType.TREE.toString()) ){
				attr.setType(AttributeType.TREE);
			}

			mp.setAttribute(attr);
			
		}
		surveyDesign.setMissionProperties(missionProperties);
		
		
		surveyDesign.setConservationArea(SmartDB.getCurrentConservationArea());

		XMLGregorianCalendar temp = xml.getStartDate();
		if(temp != null){
				surveyDesign.setStartDate(temp.toGregorianCalendar().getTime());
		}else{
			surveyDesign.setStartDate(null);
		}
		
		temp = xml.getEndDate();
		if(temp != null){
				surveyDesign.setEndDate(temp.toGregorianCalendar().getTime());
		}else{
			surveyDesign.setEndDate(null);
		}
		surveyDesign.setTrackDistanceDirection(xml.isTrackDistanceDirection());
		surveyDesign.setDescription(xml.getDescription());
		surveyDesign.setKeyId(xml.getKeyid());
		
		//
		String cmName = xml.getConfigurableModelName();
		boolean success = false;
		for(ConfigurableModel currentCM : DataentryHibernateManager.getConfigurableModels(SmartDB.getCurrentConservationArea(), session)){
			if(currentCM.getName().equals(cmName) ){
				surveyDesign.setConfigurableModel(currentCM);
				success = true;
			}
		}
		if(success == false){
			//TODO
			//no CM model was found, show a list and use the one the user selects.
		}
		
		
		if(xml.getState().equals("Active")){
			surveyDesign.setState(State.ACTIVE);
		}else if(xml.getState().equals("Inactive") ){
			surveyDesign.setState(State.INACTIVE);
		}

		ArrayList<SurveyDesignSamplingUnitAttribute> suAttributes = new ArrayList<SurveyDesignSamplingUnitAttribute>();
		for (org.wcs.smart.er.xml.model.SurveyDesignSamplingUnitAttribute suAttribute : xml.getSurveyDesignSamplingUnitAttribute()){
			SurveyDesignSamplingUnitAttribute s = new SurveyDesignSamplingUnitAttribute();
			suAttributes.add(s);
		}
		surveyDesign.setSamplingUnitAttributes(suAttributes);
		
		return surveyDesign;
	}
	
	/**
	 * Imports the query names from the xml query type to the SMART query object.
	 * @param query
	 * @param qt
	 * @throws Exception
	 */
	private static void importNames(List<NamesType> names, NamedItem toUpdate, Session session, boolean required) {
		String xmlDefaultName = null;
		for (NamesType label : names){
			List<?> values = session.createCriteria(Language.class).
				add(Restrictions.eq("ca", SmartDB.getCurrentConservationArea())). //$NON-NLS-1$ 
				add(Restrictions.eq("code",label.getLanguage()) ).list(); //$NON-NLS-1$
				
			if (values.size() > 0){
				for (Object l : values){
					toUpdate.updateName((Language)l, label.getValue());
				}
			}
		}
			
		//ensure a default exists
		String defaultName = toUpdate.findNameNull(SmartDB.getCurrentConservationArea().getDefaultLanguage());
		if (defaultName == null){
			if (xmlDefaultName != null){
				toUpdate.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(),
						xmlDefaultName);
			}else{
				if (required){
					toUpdate.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), 
						names.get(0).getValue());
				}
			}
		}
		//update cached name
		String name = toUpdate.findNameNull(SmartDB.getCurrentLanguage());
		if (name == null){
			name = toUpdate.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
		}
		toUpdate.setName(name);
		
	}
}
