package org.wcs.smart.er.xml;

import java.text.MessageFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignProperty;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;


/**
 * Converts an SMART SurveyDesign object to its xml representation.
 * 
 * @author Jeff
 *
 */

public class SurveyDesignToXmlConverter {

	/**
	 * Converts a SurveyDesign to the XML representation
	 * of the xml type.
	 * 
	 * @param entityType
	 * @return
	 * @throws DatatypeConfigurationException 
	 */
	public static org.wcs.smart.er.xml.model.SurveyDesign toXml(SurveyDesign surveyDesign, Session s, IProgressMonitor monitor) throws DatatypeConfigurationException{
		monitor.beginTask(MessageFormat.format("Exporting {0} to xml.", new Object[]{surveyDesign.getName()}), surveyDesign.getProperties().size() + 1);
		org.wcs.smart.er.xml.model.SurveyDesign xml = new org.wcs.smart.er.xml.model.SurveyDesign();

		
		//Names
		for (org.wcs.smart.ca.Label label : surveyDesign.getNames() ){ 
			org.wcs.smart.er.xml.model.NamesType xmlpair = new org.wcs.smart.er.xml.model.NamesType();
			
			xmlpair.setLanguage( label.getId().getLanguage().getCode() );
			xmlpair.setValue(label.getValue());
			xml.getNames().add(xmlpair);
		}

		//start date
		GregorianCalendar c = new GregorianCalendar();
		Date start = surveyDesign.getStartDate();
		if(start != null){
			c.setTime(start);
			xml.setStartDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
		}
		xml.setStartDate(null);

		//end date
		Date end = surveyDesign.getEndDate();
		if(end != null){
			c.setTime(end);
			xml.setEndDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
		}else{
			xml.setEndDate(null);
		}
		
		//use distance and direction
		xml.setTrackDistanceDirection(surveyDesign.getTrackDistanceDirection());
		
		//description
		xml.setDescription(surveyDesign.getDescription());
		
		//state
		xml.setState(surveyDesign.getState().getGuiName());
		
		//keyid
		xml.setKeyid(surveyDesign.getKeyId());
		
		//configurable model
		ConfigurableModel cm = surveyDesign.getConfigurableModel();
		if(cm != null){
			xml.setConfigurableModelName(cm.getName());
		}else{
			xml.setConfigurableModelName("");
		}
		

		//Survey design properties 
		for (SurveyDesignProperty sdp : surveyDesign.getProperties() ){
			org.wcs.smart.er.xml.model.SurveyDesignProperty xmlsdp = new org.wcs.smart.er.xml.model.SurveyDesignProperty();
			xmlsdp.setName(sdp.getName());
			xmlsdp.setValue(sdp.getValue());
			
			xml.getSurveyDesignProperty().add(xmlsdp);
			monitor.worked(1);
		}
		
		
		//mission attributes and their attribteListItems
		for(MissionProperty mp : surveyDesign.getMissionProperties()){
			org.wcs.smart.er.xml.model.MissionProperty xmlmp = new org.wcs.smart.er.xml.model.MissionProperty();
		
			xmlmp.setOrder(mp.getOrder());
		
			org.wcs.smart.er.model.MissionAttribute attr = mp.getAttribute(); 
			org.wcs.smart.er.xml.model.MissionAttribute xmlma = new org.wcs.smart.er.xml.model.MissionAttribute();

			xmlma.setAttributeType(attr.getType().toString());
			xmlma.setDefaultName(attr.getDefaultName());
			xmlma.setKeyId(attr.getKeyId());
			xmlma.setName(attr.getName());
			 
			//all the names for a mission_Attribute
			for (org.wcs.smart.ca.Label label : attr.getNames() ){ 
				org.wcs.smart.er.xml.model.NamesType xmlpair = new org.wcs.smart.er.xml.model.NamesType();
				
				xmlpair.setLanguage( label.getId().getLanguage().getCode() );
				xmlpair.setValue(label.getValue());
				xmlma.getNames().add(xmlpair);
			}
			
			//All the missionAttributeListItems
			for(MissionAttributeListItem mali : attr.getAttributeList()){
				org.wcs.smart.er.xml.model.MissionAttributeListItem xmlmali = new org.wcs.smart.er.xml.model.MissionAttributeListItem();
				
				xmlmali.setDefaultName(mali.getDefaultName());
				xmlmali.setKeyid(mali.getKeyId());
				xmlmali.setName(mali.getName());
				xmlmali.setListOrder(mali.getListOrder());

				
				//all the names for a single mission_Attribute list item
				for (org.wcs.smart.ca.Label label : mali.getNames() ){ 
					org.wcs.smart.er.xml.model.NamesType xmlpair = new org.wcs.smart.er.xml.model.NamesType();
					
					xmlpair.setLanguage(label.getId().getLanguage().getCode());
					xmlpair.setValue(label.getValue());
					xmlmali.getNames().add(xmlpair);
				}
				
				xmlma.getMissionAttributeListItem().add(xmlmali);
			}
			
			xmlmp.setMissionAttribute(xmlma);
			
			xml.getMissionProperty().add(xmlmp);
		}

		
		//sampling Unit Attributes
		for(SurveyDesignSamplingUnitAttribute attr : surveyDesign.getSamplingUnitAttributes()){
			SamplingUnitAttribute sua = attr.getSamplingUnitAttribute();
			org.wcs.smart.er.xml.model.SamplingUnitAttribute xmlsua = new org.wcs.smart.er.xml.model.SamplingUnitAttribute();
			
			xmlsua.setAttributeType(sua.getType().toString());
			xmlsua.setDefaultName(sua.getDefaultName());
			xmlsua.setKeyId(sua.getKeyId());
			xmlsua.setName(sua.getName());
			//all the names for the Attribute
			for (org.wcs.smart.ca.Label label : sua.getNames() ){ 
				org.wcs.smart.er.xml.model.NamesType xmlpair = new org.wcs.smart.er.xml.model.NamesType();
				
				xmlpair.setLanguage( label.getId().getLanguage().getCode() );
				xmlpair.setValue(label.getValue());
				xmlsua.getNames().add(xmlpair);
			}
			
			
		}

		//All Sampling Units
		List<SamplingUnit> units = s.createCriteria(SamplingUnit.class).add(Restrictions.eq("surveyDesign", surveyDesign )).list();
		for(SamplingUnit su :  units) {
			org.wcs.smart.er.xml.model.SamplingUnit xmlsu = new org.wcs.smart.er.xml.model.SamplingUnit();
			xmlsu.setBuffer(su.getBuffer());
			xmlsu.setGeom(su.getGeom());
			xmlsu.setId(su.getId());
			xmlsu.setState(su.getState().toString());
			xmlsu.setType(su.getType().toString());
			
			//All sampling unit attribute values for this sampling unit 
			for(SamplingUnitAttributeValue suav : su.getAttributes()){
				org.wcs.smart.er.xml.model.SamplingUnitAttributeValue xmlsuav = new org.wcs.smart.er.xml.model.SamplingUnitAttributeValue();
				
				xmlsuav.setDoubleValue(suav.getNumberValue());
				xmlsuav.setSamplingUnitId(suav.getSamplingUnit().getId());
				xmlsuav.setStringValue(suav.getStringValue());
				xmlsu.getSamplingUnitAttributeValue().add(xmlsuav);
			}
			xml.getSamplingUnit().add(xmlsu);
		}
		monitor.done();
		
		return xml;
	}
}
