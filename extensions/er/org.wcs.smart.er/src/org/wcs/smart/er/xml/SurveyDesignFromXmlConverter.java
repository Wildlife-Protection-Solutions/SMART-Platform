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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
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
import org.wcs.smart.er.xml.model.NamesType;
import org.wcs.smart.hibernate.SmartDB;



/**
 * Converts an xml Survey Design schema to an SMART SurveyDesign object.
 * 
 * @author Jeff
 *
 */

public class SurveyDesignFromXmlConverter {
	private static Object r;


	public static SurveyDesign fromXml(org.wcs.smart.er.xml.model.SurveyDesign xml, Session session) throws ParseException{
		
		//create the Survey Java object 
		SurveyDesign surveyDesign = new SurveyDesign();
		 
		importNames(xml.getNames(), surveyDesign, session, true);
		//add the survey design properties 
		ArrayList<SurveyDesignProperty> sdProperties = new ArrayList<SurveyDesignProperty>();
		for (org.wcs.smart.er.xml.model.SurveyDesignProperty xmlSDproperty : xml.getSurveyDesignProperty()){
			SurveyDesignProperty sdp = new SurveyDesignProperty();
			sdp.setName(xmlSDproperty.getName());
			sdp.setValue(xmlSDproperty.getValue());
			sdp.setSurveyDesign(surveyDesign);
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
			
			//check if the attribute exists already
			MissionAttribute existingAttr = getAttr(xmlattr, session);
			if(existingAttr == null){
			
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
			}else{
				//attribute already exists, link it
				mp.setAttribute(existingAttr);
			}

			
			
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
			//no config model found
			throw new ParseException(Messages.SurveyDesignFromXmlConverter_0, 0);
		}
		
		
		if(xml.getState().equals("Active")){ //$NON-NLS-1$
			surveyDesign.setState(State.ACTIVE);
		}else if(xml.getState().equals("Inactive") ){ //$NON-NLS-1$
			surveyDesign.setState(State.INACTIVE);
		}

		return surveyDesign;
	}
	
	private static MissionAttribute getAttr(org.wcs.smart.er.xml.model.MissionAttribute xmlattr, Session s) {
		List<MissionAttribute> values = s.createCriteria(MissionAttribute.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()) ).add(Restrictions.eq("keyId", xmlattr.getKeyId())).list(); //$NON-NLS-1$ //$NON-NLS-2$
		if (values.size() > 0){
			MissionAttribute attr = values.get(0);
			
			for(org.wcs.smart.er.xml.model.MissionAttributeListItem xmlMali :xmlattr.getMissionAttributeListItem()){
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
					newmali.setName(xmlMali.getName());
				
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
