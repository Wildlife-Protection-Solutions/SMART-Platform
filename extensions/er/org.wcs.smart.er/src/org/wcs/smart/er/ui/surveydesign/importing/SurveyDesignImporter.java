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
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
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
		importSamplingUnitAttributes(session, design, copy);
		
		//copy sampling units & associated attributes
//		List<SamplingUnit> newSamplingUnits = cloneSamplingUnits(session, copy, design);
		
		return design;
	}

	
	private void importSamplingUnitAttributes(Session session, SurveyDesign to, SurveyDesign from) {
		//import sampling unit attributes; creating new attributes that have not been defined
		
		to.setSamplingUnitAttributes(new ArrayList<SurveyDesignSamplingUnitAttribute>());
		for (SurveyDesignSamplingUnitAttribute property : from.getSamplingUnitAttributes()){
			SurveyDesignSamplingUnitAttribute copy = new SurveyDesignSamplingUnitAttribute();
			copy.setSurveyDesign(to);
			SamplingUnitAttribute attribute = getSamplingUnitAttribute(session, property.getSamplingUnitAttribute());
			if (attribute == null){
				//create new sampling unit attribute
				attribute = createSamplingUnitAttribute(property.getSamplingUnitAttribute(), session);
			}
			copy.setSamplingUnitAttribute(attribute);
			to.getSamplingUnitAttributes().add(copy);	
		}
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
		if (source.getConservationArea().equals(SmartDB.getCurrentConservationArea())){
			return source;
		}
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
	
	private SamplingUnitAttribute getSamplingUnitAttribute(Session s, SamplingUnitAttribute source) {
		if (source.getConservationArea().equals(SmartDB.getCurrentConservationArea())){
			return source;
		}
		
		@SuppressWarnings("unchecked")
		List<SamplingUnitAttribute> values = s.createCriteria(SamplingUnitAttribute.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()) ) //$NON-NLS-1$
				.add(Restrictions.eq("keyId", source.getKeyId())).list(); //$NON-NLS-1$ 
		
		if (values.size() > 0){
			SamplingUnitAttribute attr = values.get(0);
			
			//if this is list we need to match list items as well
			for(SamplingUnitAttributeListItem srcMali : source.getAttributeList()){
				boolean match = false;
				for(SamplingUnitAttributeListItem mali : attr.getAttributeList()){
					if(srcMali.getKeyId().equals(mali.getKeyId())){
						match = true;
						break;
					}
				}
				if (!match){
					//missing list item, add it
					SamplingUnitAttributeListItem newmali = new SamplingUnitAttributeListItem();
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
	
	private SamplingUnitAttribute createSamplingUnitAttribute(SamplingUnitAttribute from, Session session){
		if (from.getConservationArea().equals(SmartDB.getCurrentConservationArea())){
			return from;
		}
		SamplingUnitAttribute attr = new SamplingUnitAttribute();
		attr.setConservationArea(SmartDB.getCurrentConservationArea());
		attr.setKeyId(from.getKeyId());
		importNames(session, attr, from.getNames(), false);
		attr.setType(from.getType());
		
		attr.setAttributeList(new ArrayList<SamplingUnitAttributeListItem>());
		for(SamplingUnitAttributeListItem srcMali : from.getAttributeList()){
			SamplingUnitAttributeListItem mali = new SamplingUnitAttributeListItem();
			mali.setAttribute(attr);
			mali.setKeyId(srcMali.getKeyId());
			mali.setListOrder(srcMali.getListOrder());
			mali.setUuid(null);
			importNames(session, mali, srcMali.getNames(), false);
			attr.getAttributeList().add(mali);
		}
		return attr;
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
	
	public static List<SamplingUnit> importSamplingUnits(Session session, SurveyDesign from, SurveyDesign to) {
		List<SamplingUnit> newSamplingUnits = new ArrayList<SamplingUnit>();
		@SuppressWarnings("unchecked")
		List<SamplingUnit> sus = session.createCriteria(SamplingUnit.class).add(Restrictions.eq("surveyDesign", from)).list(); //$NON-NLS-1$
		boolean isSameCa = from.getConservationArea().equals(to.getConservationArea());
		
		for (SamplingUnit s2: sus){
			SamplingUnit newsu = new SamplingUnit();
			newsu.setGeom(s2.getGeom());
			newsu.setId(s2.getId());
			newsu.setState(s2.getState());
			newsu.setSurveyDesign(to);
			newsu.setType(s2.getType());
			newsu.setAttributes(new ArrayList<SamplingUnitAttributeValue>());
			
			for (SamplingUnitAttributeValue suav : s2.getAttributes()){
				boolean add = true;
		
				SamplingUnitAttribute attribute = suav.getSamplingUnitAttribute();
				if (!isSameCa){
					//need to find attribute with the same key in the new design
					add = false;
					for (SurveyDesignSamplingUnitAttribute newAttribute : to.getSamplingUnitAttributes()){
						if (newAttribute.getSamplingUnitAttribute().getKeyId().equals(attribute.getKeyId())){
							attribute = newAttribute.getSamplingUnitAttribute();
							add = true;
							break;
						}
					}
				}
				if (!add) continue;
				
				SamplingUnitAttributeValue newAv = new SamplingUnitAttributeValue();
				newAv.setSamplingUnitAttribute(attribute);
				newAv.setNumberValue(suav.getNumberValue());
				newAv.setStringValue(suav.getStringValue());
				
				if (suav.getAttributeListItem() != null){
					if (isSameCa){
						newAv.setAttributeListItem(suav.getAttributeListItem());
					}else{
						add = false;
						//need to find the sampling unit list item from the new ca; if new ca
						String keyToFind = suav.getAttributeListItem().getKeyId();
						for (SamplingUnitAttributeListItem li : attribute.getAttributeList()){
							if (li.getKeyId().equals(keyToFind)){
								add = true;
								newAv.setAttributeListItem(li);
								break;
							}
						}
					}	
				}
				if (!add) continue;
				
				newAv.setSamplingUnit(newsu);
				newsu.getAttributes().add(newAv);
			}
			newSamplingUnits.add(newsu);
		}
		return newSamplingUnits;
	}
	
	
	/**
	 * Clones the information to toCopy into toUpdate.
	 * 
	 * @param toCopy the survey design to copy
	 * @param includeSamplingUnits if sampling units should be cloned
	 * @param session
	 * @return list of cloned sampling units if includeSamplingUnits is true; otherwise returns null
	 */
	public static void copyDesign(SurveyDesign toUpdate, SurveyDesign toCopy, Session session){
		//copy of design elements
		SurveyDesign clone = toUpdate;
		clone.setStartDate(toCopy.getStartDate());
		clone.setEndDate(toCopy.getEndDate());
		clone.setDescription(toCopy.getDescription());
		clone.setTrackDistanceDirection(toCopy.getTrackDistanceDirection());
		clone.setTrackObserver(toCopy.getTrackObserver());
		clone.setConservationArea(SmartDB.getCurrentConservationArea());
		
		//mission properties
		SurveyDesignImporter im = new SurveyDesignImporter();
		im.importMissionProperties(session, clone, toCopy);
		for (MissionProperty mp : clone.getMissionProperties()){
			if (mp.getAttribute().getUuid() == null){
				session.save(mp.getAttribute());
			}
		}
		session.flush();
		
		//sampling unit attributes
		im.importSamplingUnitAttributes(session, clone, toCopy);
		for (SurveyDesignSamplingUnitAttribute att: clone.getSamplingUnitAttributes()){
			if (att.getSamplingUnitAttribute().getUuid() == null){
				session.save(att.getSamplingUnitAttribute());
			}
		}
		session.flush();
		
		
		//survey design properties
		clone.setProperties(new ArrayList<SurveyDesignProperty>());
		if (clone.getProperties() != null){
			for (SurveyDesignProperty sdp : clone.getProperties()){
				SurveyDesignProperty propclone = new SurveyDesignProperty();
				propclone.setSurveyDesign(clone);
				propclone.setName(sdp.getName());
				propclone.setValue(sdp.getValue());

				clone.getProperties().add(propclone);
			}
		}	
	}
}
