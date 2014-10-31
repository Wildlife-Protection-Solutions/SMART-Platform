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
package org.wcs.smart.er.xml;

import java.text.MessageFormat;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignProperty;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SmartUtils;


/**
 * Converts an SMART SurveyDesign object to its xml representation.
 * 
 * @author Jeff
 *
 */
@SuppressWarnings("unchecked")
public class SurveyDesignToXmlConverter {

	/**
	 * Converts a SurveyDesign to the XML representation
	 * of the xml type.
	 * 
	 * @param entityType
	 * @return
	 * @throws DatatypeConfigurationException 
	 */
	public static org.wcs.smart.er.xml.model.surveydesign.SurveyDesign toXml(SurveyDesign surveyDesign, Session s, IProgressMonitor monitor) throws DatatypeConfigurationException{
		monitor.beginTask(MessageFormat.format(Messages.SurveyDesignToXmlConverter_TaskName, new Object[]{surveyDesign.getName()}), surveyDesign.getProperties().size() + 1);
		org.wcs.smart.er.xml.model.surveydesign.SurveyDesign xml = new org.wcs.smart.er.xml.model.surveydesign.SurveyDesign();

		
		//Names
		for (org.wcs.smart.ca.Label label : surveyDesign.getNames() ){ 
			org.wcs.smart.er.xml.model.surveydesign.NamesType xmlpair = new org.wcs.smart.er.xml.model.surveydesign.NamesType();
			
			xmlpair.setLanguage( label.getId().getLanguage().getCode() );
			xmlpair.setValue(label.getValue());
			xmlpair.setIsDefault(label.getLanguage().equals(SmartDB.getCurrentConservationArea().getDefaultLanguage()));
			xml.getNames().add(xmlpair);
		}

		//start & end dates
		xml.setStartDate(SmartUtils.toXmlDate(surveyDesign.getStartDate()));
		xml.setEndDate(SmartUtils.toXmlDate(surveyDesign.getEndDate()));
		
		//use distance and direction
		xml.setTrackDistanceDirection(surveyDesign.getTrackDistanceDirection());
		xml.setTrackObserver(surveyDesign.getTrackObserver());
		
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
			xml.setConfigurableModelName(""); //$NON-NLS-1$
		}
		

		//Survey design properties 
		for (SurveyDesignProperty sdp : surveyDesign.getProperties() ){
			org.wcs.smart.er.xml.model.surveydesign.SurveyDesignProperty xmlsdp = new org.wcs.smart.er.xml.model.surveydesign.SurveyDesignProperty();
			xmlsdp.setName(sdp.getName());
			xmlsdp.setValue(sdp.getValue());
			
			xml.getSurveyDesignProperty().add(xmlsdp);
			monitor.worked(1);
		}
		
		
		//mission attributes and their attribteListItems
		for(MissionProperty mp : surveyDesign.getMissionProperties()){
			org.wcs.smart.er.xml.model.surveydesign.MissionProperty xmlmp = new org.wcs.smart.er.xml.model.surveydesign.MissionProperty();
		
			xmlmp.setOrder(mp.getOrder());
		
			org.wcs.smart.er.model.MissionAttribute attr = mp.getAttribute(); 
			org.wcs.smart.er.xml.model.surveydesign.MissionAttribute xmlma = new org.wcs.smart.er.xml.model.surveydesign.MissionAttribute();

			xmlma.setAttributeType(attr.getType().toString());
			xmlma.setKeyId(attr.getKeyId());
			 
			//all the names for a mission_Attribute
			for (org.wcs.smart.ca.Label label : attr.getNames() ){ 
				org.wcs.smart.er.xml.model.surveydesign.NamesType xmlpair = new org.wcs.smart.er.xml.model.surveydesign.NamesType();
				
				xmlpair.setLanguage( label.getId().getLanguage().getCode() );
				xmlpair.setValue(label.getValue());
				xmlpair.setIsDefault(label.getLanguage().equals(SmartDB.getCurrentConservationArea().getDefaultLanguage()));
				xmlma.getNames().add(xmlpair);
			}
			
			//All the missionAttributeListItems
			for(MissionAttributeListItem mali : attr.getAttributeList()){
				org.wcs.smart.er.xml.model.surveydesign.MissionAttributeListItem xmlmali = new org.wcs.smart.er.xml.model.surveydesign.MissionAttributeListItem();
				
				xmlmali.setKeyid(mali.getKeyId());
				xmlmali.setListOrder(mali.getListOrder());

				//all the names for a single mission_Attribute list item
				for (org.wcs.smart.ca.Label label : mali.getNames() ){ 
					org.wcs.smart.er.xml.model.surveydesign.NamesType xmlpair = new org.wcs.smart.er.xml.model.surveydesign.NamesType();
					
					xmlpair.setLanguage(label.getId().getLanguage().getCode());
					xmlpair.setValue(label.getValue());
					xmlpair.setIsDefault(label.getLanguage().equals(SmartDB.getCurrentConservationArea().getDefaultLanguage()));
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
			
			org.wcs.smart.er.xml.model.surveydesign.SurveyDesignSamplingUnitAttribute xmlSDsua = new org.wcs.smart.er.xml.model.surveydesign.SurveyDesignSamplingUnitAttribute();
			org.wcs.smart.er.xml.model.surveydesign.SamplingUnitAttribute xmlsua = new org.wcs.smart.er.xml.model.surveydesign.SamplingUnitAttribute();
						
			xmlsua.setAttributeType(sua.getType().toString());
			xmlsua.setDefaultName(sua.getDefaultName());
			xmlsua.setKeyId(sua.getKeyId());

			//all the names for the Attribute
			for (org.wcs.smart.ca.Label label : sua.getNames() ){ 
				org.wcs.smart.er.xml.model.surveydesign.NamesType xmlpair = new org.wcs.smart.er.xml.model.surveydesign.NamesType();
				
				xmlpair.setLanguage( label.getId().getLanguage().getCode() );
				xmlpair.setValue(label.getValue());
				xmlpair.setIsDefault(label.getLanguage().equals(SmartDB.getCurrentConservationArea().getDefaultLanguage()));
				xmlsua.getNames().add(xmlpair);
			}
			
			//All the list items
			for(SamplingUnitAttributeListItem mali : sua.getAttributeList()){
				org.wcs.smart.er.xml.model.surveydesign.SamplingUnitAttributeListItem xmlmali = new org.wcs.smart.er.xml.model.surveydesign.SamplingUnitAttributeListItem();
				
				xmlmali.setKeyId(mali.getKeyId());
				xmlmali.setListorder(mali.getListOrder());

				//all the names for a single mission_Attribute list item
				for (org.wcs.smart.ca.Label label : mali.getNames() ){ 
					org.wcs.smart.er.xml.model.surveydesign.NamesType xmlpair = new org.wcs.smart.er.xml.model.surveydesign.NamesType();
					
					xmlpair.setLanguage(label.getId().getLanguage().getCode());
					xmlpair.setValue(label.getValue());
					xmlpair.setIsDefault(label.getLanguage().equals(SmartDB.getCurrentConservationArea().getDefaultLanguage()));
					xmlmali.getNames().add(xmlpair);
				}
				
				xmlsua.getListItems().add(xmlmali);
			}
			
			xmlSDsua.getSamplingUnitAttributes().add(xmlsua);
			xml.getSurveyDesignSamplingUnitAttribute().add(xmlSDsua);
					
		}

		//All Sampling Units
		List<SamplingUnit> units = s.createCriteria(SamplingUnit.class).add(Restrictions.eq("surveyDesign", surveyDesign )).list(); //$NON-NLS-1$
		for(SamplingUnit su :  units) {
			org.wcs.smart.er.xml.model.surveydesign.SamplingUnit xmlsu = new org.wcs.smart.er.xml.model.surveydesign.SamplingUnit();
			xmlsu.setGeom(su.getGeom());
			xmlsu.setId(su.getId());
			xmlsu.setState(su.getState().toString());
			xmlsu.setType(su.getType().toString());
			
			//All sampling unit attribute values for this sampling unit 
			for(SamplingUnitAttributeValue suav : su.getAttributes()){
				org.wcs.smart.er.xml.model.surveydesign.SamplingUnitAttributeValue xmlsuav = new org.wcs.smart.er.xml.model.surveydesign.SamplingUnitAttributeValue();
				
				
				xmlsuav.setSamplingUnitAttributeId(suav.getSamplingUnit().getId());
				if (suav.getSamplingUnitAttribute().getType() == AttributeType.TEXT){
					xmlsuav.setStringValue(suav.getStringValue());	
				}else if (suav.getSamplingUnitAttribute().getType() == AttributeType.NUMERIC){
					xmlsuav.setDoubleValue(suav.getNumberValue());
				}else if (suav.getSamplingUnitAttribute().getType() == AttributeType.LIST){
					if (suav.getAttributeListItem() != null){
						xmlsuav.setStringValue(suav.getAttributeListItem().getKeyId());
					}
				}
				xmlsuav.setSamplingUnitAttributeId(suav.getSamplingUnitAttribute().getKeyId());
				xmlsu.getSamplingUnitAttributeValue().add(xmlsuav);
			}
			xml.getSamplingUnit().add(xmlsu);
		}
		monitor.done();
		
		return xml;
	}
}
