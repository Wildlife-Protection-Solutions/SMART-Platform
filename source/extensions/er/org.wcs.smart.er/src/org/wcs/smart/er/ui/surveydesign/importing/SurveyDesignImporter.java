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
package org.wcs.smart.er.ui.surveydesign.importing;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignProperty;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Class is responsible for creating clones for {@link SurveyDesign} and related object
 * Mainly used when creating new design from template and importing from another conservation area
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class SurveyDesignImporter {

	public SurveyDesign importSurveyDesign(Session session, SurveyDesign copy) {
		SurveyDesign design = new SurveyDesign();
		
		//copy of design elements
		design.setStartDate(copy.getStartDate());
		design.setEndDate(copy.getEndDate());
		design.setDescription(copy.getDescription());
		design.setTrackDistanceDirection(copy.getTrackDistanceDirection());
		design.setTrackObserver(copy.getTrackObserver());
		design.setConservationArea(SmartDB.getCurrentConservationArea());

		importNames(session, design, copy.getNames(), true);
		design.setKeyId(copy.getKeyId());
		design.setState(copy.getState());
		
		//configurable model
		if (copy.getConfigurableModel() != null) {
			String cmName = copy.getConfigurableModel().getName();
			if (cmName != null && cmName.length() > 0){
				boolean success = false;
				List<ConfigurableModel> models = DataentryHibernateManager.getConfigurableModels(SmartDB.getCurrentConservationArea(), session);
				for(ConfigurableModel currentCM : models){
					if(currentCM.getName().equals(cmName) ){
						design.setConfigurableModel(currentCM);
						success = true;
						break;
					}
				}
			
				if(!success){
					//no config model found
					final String msg = MessageFormat.format(Messages.SurveyDesignFromXmlConverter_0, cmName, design.getName());
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog.openWarning(Display.getDefault().getActiveShell(), Messages.SurveyDesignFromXmlConverter_1, msg);
						}
					});
				}
			}
		}
		
		importMissionProperties(session, design, copy);

		//survey design properties
		design.setProperties(new ArrayList<SurveyDesignProperty>());
		if (copy.getProperties() != null){
			for (SurveyDesignProperty sdp : copy.getProperties()){
				SurveyDesignProperty clone = new SurveyDesignProperty();
				clone.setSurveyDesign(design);
				clone.setName(sdp.getName());
				clone.setValue(sdp.getValue());

				design.getProperties().add(clone);
			}
		}
		
		//copy sampling unit attributes
		design.setSamplingUnitAttributes(new ArrayList<SurveyDesignSamplingUnitAttribute>());
		for (SurveyDesignSamplingUnitAttribute sua : copy.getSamplingUnitAttributes()){
			SurveyDesignSamplingUnitAttribute a2 = new SurveyDesignSamplingUnitAttribute();
			a2.setSamplingUnitAttribute(sua.getSamplingUnitAttribute());
			a2.setSurveyDesign(design);
			
			design.getSamplingUnitAttributes().add(a2);
		}

		//copy sampling units & associated attributes
//		List<SamplingUnit> newSamplingUnits = cloneSamplingUnits(session, copy, design);
		
		return design;
	}

	private void importMissionProperties(Session session, SurveyDesign to, SurveyDesign from) {
		//import mission properties
		//if property is found, we use that property otherwise
		//we import the property as a new property
		to.setMissionProperties(new ArrayList<MissionProperty>());
		for (MissionProperty property : from.getMissionProperties()){
			MissionProperty mp = new MissionProperty();
			mp.setSurveyDesign(to);
			mp.setOrder(property.getOrder());
			to.getMissionProperties().add(mp);
			
			//check if the attribute exists already		
			MissionAttribute srcAttr = property.getAttribute();
			MissionAttribute existingAttr = getMissionAttribute(session, srcAttr);
			if(existingAttr == null){
				//create a new mission attribute
				MissionAttribute attr = new MissionAttribute();
				attr.setConservationArea(SmartDB.getCurrentConservationArea());
				attr.setKeyId(srcAttr.getKeyId());
				importNames(session, attr, srcAttr.getNames(), false);
				attr.setType(srcAttr.getType());
				
				attr.setAttributeList(new ArrayList<MissionAttributeListItem>());
				for(MissionAttributeListItem srcMali : srcAttr.getAttributeList()){
					MissionAttributeListItem mali = new MissionAttributeListItem();
					mali.setAttribute(attr);
					mali.setKeyId(srcMali.getKeyId());
					mali.setListOrder(srcMali.getListOrder());
					mali.setUuid(null);
					importNames(session, mali, srcMali.getNames(), false);
					attr.getAttributeList().add(mali);
				}
				mp.setAttribute(attr);
			}else{
				//attribute already exists, link it
				mp.setAttribute(existingAttr);
			}
		}
	}

	private MissionAttribute getMissionAttribute(Session s, MissionAttribute source) {
		@SuppressWarnings("unchecked")
		List<MissionAttribute> values = s.createCriteria(MissionAttribute.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()) ) //$NON-NLS-1$
				.add(Restrictions.eq("keyId", source.getKeyId())).list(); //$NON-NLS-1$ 
		if (values.size() > 0){
			MissionAttribute attr = values.get(0);
			//if this is list we need to match list items as well
			for(MissionAttributeListItem srcMali : source.getAttributeList()){
				boolean match = false;
				for(MissionAttributeListItem mali : attr.getAttributeList()){
					if(srcMali.getKeyId().equals(mali.getKeyId())){
						match = true;
						break;
					}
				}
				if (!match){
					//missing list item, add it
					MissionAttributeListItem newmali = new MissionAttributeListItem();
					newmali.setAttribute(attr);
					newmali.setKeyId(srcMali.getKeyId());
					newmali.setListOrder(srcMali.getListOrder());
					importNames(s, newmali, srcMali.getNames(), false);
					attr.getAttributeList().add(newmali);
				}

			}
			return attr;
		}
		return null;
	}
	
	private void importNames(Session session, NamedItem toUpdate, Set<Label> names, boolean required) {
		String srcDefaultName = null;
		for (Label label : names){
			if (label.getLanguage().isDefault()){
				srcDefaultName = label.getValue();
			}
			List<?> values = session.createCriteria(Language.class).
				add(Restrictions.eq("ca", SmartDB.getCurrentConservationArea())). //$NON-NLS-1$ 
				add(Restrictions.eq("code",label.getLanguage().getCode())).list(); //$NON-NLS-1$
				
			if (values.size() > 0){
				for (Object l : values){
					toUpdate.updateName((Language)l, label.getValue());
				}
			}
		}
			
		//ensure a default exists
		String defaultName = toUpdate.findNameNull(SmartDB.getCurrentConservationArea().getDefaultLanguage());
		if (defaultName == null){
			if (srcDefaultName != null){
				toUpdate.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(),srcDefaultName);
			}else{
				if (required && !names.isEmpty()){
					toUpdate.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), 
						names.iterator().next().getValue());
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
	
	public List<SamplingUnit> importSamplingUnits(Session session, SurveyDesign from, SurveyDesign to) {
		List<SamplingUnit> newSamplingUnits = new ArrayList<SamplingUnit>();
		@SuppressWarnings("unchecked")
		List<SamplingUnit> sus = session.createCriteria(SamplingUnit.class).add(Restrictions.eq("surveyDesign", from)).list(); //$NON-NLS-1$
		for (SamplingUnit s2: sus){
			SamplingUnit newsu = new SamplingUnit();
			newsu.setGeom(s2.getGeom());
			newsu.setId(s2.getId());
			newsu.setState(s2.getState());
			newsu.setSurveyDesign(to);
			newsu.setType(s2.getType());
			newsu.setAttributes(new ArrayList<SamplingUnitAttributeValue>());
			
			for (SamplingUnitAttributeValue suav : s2.getAttributes()){
				SamplingUnitAttributeValue newAv = new SamplingUnitAttributeValue();
				newAv.setNumberValue(suav.getNumberValue());
				newAv.setStringValue(suav.getStringValue());
				newAv.setSamplingUnit(newsu);
				newAv.setSamplingUnitAttribute(suav.getSamplingUnitAttribute());
				newsu.getAttributes().add(newAv);
			}
			newSamplingUnits.add(newsu);
		}
		return newSamplingUnits;
	}
	
}
