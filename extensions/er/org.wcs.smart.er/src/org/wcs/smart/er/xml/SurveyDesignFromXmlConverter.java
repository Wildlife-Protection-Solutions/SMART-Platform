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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnit.GeometryType;
import org.wcs.smart.er.model.SurveyDesign.State;
import org.wcs.smart.er.model.SurveyDesignProperty;
import org.wcs.smart.er.ui.samplingunit.load.ImportAttributes;
import org.wcs.smart.er.xml.model.surveydesign.NamesType;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Converts an xml Survey Design schema to an SMART SurveyDesign object.
 * 
 * @author Jeff
 *
 */
@SuppressWarnings("unchecked")
public class SurveyDesignFromXmlConverter {

	public static SurveyDesign fromXml(org.wcs.smart.er.xml.model.surveydesign.SurveyDesign xml, Session session) throws ParseException{
		
		//create the Survey object 
		SurveyDesign surveyDesign = new SurveyDesign();
		 
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
		importNames(xml.getNames(), surveyDesign, session, true);
		
		//add the survey design properties 
		ArrayList<SurveyDesignProperty> sdProperties = new ArrayList<SurveyDesignProperty>();
		for (org.wcs.smart.er.xml.model.surveydesign.SurveyDesignProperty xmlSDproperty : xml.getSurveyDesignProperty()){
			SurveyDesignProperty sdp = new SurveyDesignProperty();
			sdp.setName(xmlSDproperty.getName());
			sdp.setValue(xmlSDproperty.getValue());
			sdp.setSurveyDesign(surveyDesign);
			sdProperties.add(sdp);
		}
		surveyDesign.setProperties(sdProperties); //survey design properties		
		
		//import mission properties
		//if property is found, we use that property otherwise
		//we import the property as a new property
		surveyDesign.setMissionProperties(new ArrayList<MissionProperty>());
		for (org.wcs.smart.er.xml.model.surveydesign.MissionProperty xmlproperty : xml.getMissionProperty()){
			MissionProperty mp = new MissionProperty();
			mp.setSurveyDesign(surveyDesign);
			mp.setOrder(xmlproperty.getOrder());
			surveyDesign.getMissionProperties().add(mp);
			
			//check if the attribute exists already		
			org.wcs.smart.er.xml.model.surveydesign.MissionAttribute xmlattr = xmlproperty.getMissionAttribute();
			MissionAttribute existingAttr = getMissionAttribute(xmlattr, session);
			if(existingAttr == null){
				//create a new mission attribute
				MissionAttribute attr = new MissionAttribute();
				attr.setConservationArea(SmartDB.getCurrentConservationArea());
				attr.setKeyId(xmlattr.getKeyId());
				importNames(xmlattr.getNames(), attr, session, false);
				attr.setType(AttributeType.valueOf(xmlattr.getAttributeType()));
				
				attr.setAttributeList(new ArrayList<MissionAttributeListItem>());
				for(org.wcs.smart.er.xml.model.surveydesign.MissionAttributeListItem xmlmali : xmlattr.getMissionAttributeListItem()){
					MissionAttributeListItem mali = new MissionAttributeListItem();
					mali.setAttribute(attr);
					mali.setKeyId(xmlmali.getKeyid());
					mali.setListOrder(xmlmali.getListOrder());
					mali.setUuid(null);
					importNames(xmlmali.getNames(), mali, session, false);
					attr.getAttributeList().add(mali);
				}
				mp.setAttribute(attr);
			}else{
				//attribute already exists, link it
				mp.setAttribute(existingAttr);
			}
		}

		
		//configurable model
		String cmName = xml.getConfigurableModelName();
		if (cmName != null && cmName.length() > 0){
			boolean success = false;
			for(ConfigurableModel currentCM : DataentryHibernateManager.getConfigurableModels(SmartDB.getCurrentConservationArea(), session)){
				if(currentCM.getName().equals(cmName) ){
					surveyDesign.setConfigurableModel(currentCM);
					success = true;
				}
			}
		
			if(success == false){
				//no config model found
				throw new ParseException(
						MessageFormat.format(Messages.SurveyDesignFromXmlConverter_0, new Object[]{cmName}), 0);
			}
		}
		
		
		if(xml.getState().equals("Active")){ //$NON-NLS-1$
			surveyDesign.setState(State.ACTIVE);
		}else if(xml.getState().equals("Inactive") ){ //$NON-NLS-1$
			surveyDesign.setState(State.INACTIVE);
		}

		//sampling units attributes
		surveyDesign.setSamplingUnitAttributes(new ArrayList<SurveyDesignSamplingUnitAttribute>());
		for(org.wcs.smart.er.xml.model.surveydesign.SurveyDesignSamplingUnitAttribute xmlsdsua : xml.getSurveyDesignSamplingUnitAttribute()){
			
			SurveyDesignSamplingUnitAttribute sdsua = new SurveyDesignSamplingUnitAttribute();
			sdsua.setSurveyDesign(surveyDesign);
			surveyDesign.getSamplingUnitAttributes().add(sdsua);
			
			org.wcs.smart.er.xml.model.surveydesign.SamplingUnitAttribute xmlsua = xmlsdsua.getSamplingUnitAttributes().get(0);
			SamplingUnitAttribute existingSua = getSamplingUnitAttribute(xmlsua, session);
			
			if(existingSua == null){
				SamplingUnitAttribute sua = new SamplingUnitAttribute();
				sdsua.setSamplingUnitAttribute(sua);
				sua.setType(AttributeType.valueOf(xmlsua.getAttributeType()));
				sua.setConservationArea(SmartDB.getCurrentConservationArea());
				sua.setKeyId(xmlsua.getKeyId());
				importNames(xmlsua.getNames(), sua, session, false);
				sua.setAttributeList(new ArrayList<SamplingUnitAttributeListItem>());
				for (org.wcs.smart.er.xml.model.surveydesign.SamplingUnitAttributeListItem xmlsuli : xmlsua.getListItems()){
					SamplingUnitAttributeListItem suli = new SamplingUnitAttributeListItem();
					suli.setAttribute(sua);
					suli.setKeyId(xmlsuli.getKeyId());
					suli.setListOrder(xmlsuli.getListorder());
					importNames(xmlsuli.getNames(), suli, session, false);
					sua.getAttributeList().add(suli);
				}
				
			
				session.save(sua);
				sdsua.setSamplingUnitAttribute(sua);
			}else{
				sdsua.setSamplingUnitAttribute(existingSua);
			}
		}
		
		return surveyDesign;
	}
	
	public static List<SamplingUnit> getSamplingUnits(org.wcs.smart.er.xml.model.surveydesign.SurveyDesign xml, SurveyDesign convertedDesign, Session session) throws ParseException{
		List<SamplingUnit> newUnits = new ArrayList<SamplingUnit>();
		
		for (org.wcs.smart.er.xml.model.surveydesign.SamplingUnit xmlunit : xml.getSamplingUnit()){
			
			SamplingUnit unit = new SamplingUnit();
			unit.setGeom(xmlunit.getGeom());
			unit.setId(xmlunit.getId());
			unit.setState(SamplingUnit.State.valueOf(xmlunit.getState()));
			unit.setType(GeometryType.valueOf(xmlunit.getType()));
			unit.setSurveyDesign(convertedDesign);
			newUnits.add(unit);
			unit.setAttributes(new ArrayList<SamplingUnitAttributeValue>());
			
			//sampling unit attribute values
			for(org.wcs.smart.er.xml.model.surveydesign.SamplingUnitAttributeValue xmlsuav : xmlunit.getSamplingUnitAttributeValue()){
				SamplingUnitAttributeValue suav = new SamplingUnitAttributeValue();
				boolean found = false;
				for(SurveyDesignSamplingUnitAttribute sua : convertedDesign.getSamplingUnitAttributes()){					
					if(xmlsuav.getSamplingUnitAttributeId().equals(sua.getSamplingUnitAttribute().getKeyId())){
						suav.setSamplingUnitAttribute(sua.getSamplingUnitAttribute());
						found = true;
						break;
					}
				}
				if(!found){
					throw new ParseException(Messages.SurveyDesignImportHandler_5 + xmlsuav.getSamplingUnitAttributeId() + Messages.SurveyDesignImportHandler_6, 0);
				}
				if (suav.getSamplingUnitAttribute().getType() == AttributeType.TEXT){
					suav.setStringValue(xmlsuav.getStringValue());	
				}else if (suav.getSamplingUnitAttribute().getType() == AttributeType.NUMERIC){
					suav.setNumberValue(xmlsuav.getDoubleValue());
				}else if (suav.getSamplingUnitAttribute().getType() == AttributeType.LIST){
					SamplingUnitAttributeListItem item = ImportAttributes.findMatch(suav.getSamplingUnitAttribute(), xmlsuav.getStringValue());
					suav.setAttributeListItem(item);
				}
				
				suav.setSamplingUnit(unit);
				unit.getAttributes().add(suav);
			}
		}
		return newUnits;

	}

	private static MissionAttribute getMissionAttribute(org.wcs.smart.er.xml.model.surveydesign.MissionAttribute xmlattr, Session s) {
		List<MissionAttribute> values = s.createCriteria(MissionAttribute.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()) ) //$NON-NLS-1$
				.add(Restrictions.eq("keyId", xmlattr.getKeyId())).list(); //$NON-NLS-1$ 
		if (values.size() > 0){
			MissionAttribute attr = values.get(0);
			
			for(org.wcs.smart.er.xml.model.surveydesign.MissionAttributeListItem xmlMali :xmlattr.getMissionAttributeListItem()){
				boolean match = false;
				for(MissionAttributeListItem mali : attr.getAttributeList()){
					if(xmlMali.getKeyid().equals(mali.getKeyId())){
						match = true;
						break;
					}
				}
				if (match == false){
					//missing list item, add it
					MissionAttributeListItem newmali = new MissionAttributeListItem();
					newmali.setAttribute(attr);
					newmali.setKeyId(xmlMali.getKeyid());
					newmali.setListOrder(xmlMali.getListOrder());
					importNames(xmlMali.getNames(), newmali, s, false);
					attr.getAttributeList().add(newmali);
				}

			}

			return attr;
		}
		return null;
	}

	private static SamplingUnitAttribute getSamplingUnitAttribute(org.wcs.smart.er.xml.model.surveydesign.SamplingUnitAttribute xmlsua, Session s) {
		List<SamplingUnitAttribute> values = s.createCriteria(SamplingUnitAttribute.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()) ) //$NON-NLS-1$
				.add(Restrictions.eq("keyId", xmlsua.getKeyId())).list(); //$NON-NLS-1$ 
		if (values.size() > 0){
			SamplingUnitAttribute attr = values.get(0);
			
			for(org.wcs.smart.er.xml.model.surveydesign.SamplingUnitAttributeListItem xmlMali :xmlsua.getListItems()){
				boolean match = false;
				for(SamplingUnitAttributeListItem mali : attr.getAttributeList()){
					if(xmlMali.getKeyId().equals(mali.getKeyId())){
						match = true;
						break;
					}
				}
				if (match == false){
					//missing list item, add it
					SamplingUnitAttributeListItem newmali = new SamplingUnitAttributeListItem();
					newmali.setAttribute(attr);
					newmali.setKeyId(xmlMali.getKeyId());
					newmali.setListOrder(xmlMali.getListorder());
					importNames(xmlMali.getNames(), newmali, s, false);
					attr.getAttributeList().add(newmali);
				}

			}
			return attr;
		}
		return null;
	}

	/**
	 * Imports the i8n names from the xml query type to the SMART query object.
	 * @param query
	 * @param qt
	 * @throws Exception
	 */
	public static void importNames(List<NamesType> names, NamedItem toUpdate, Session session, boolean required) {
		String xmlDefaultName = null;
		for (NamesType label : names){
			if (label.isIsDefault()){
				xmlDefaultName = label.getValue();
			}
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
				toUpdate.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(),xmlDefaultName);
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
