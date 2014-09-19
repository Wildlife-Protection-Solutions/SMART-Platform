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
package org.wcs.smart.er.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignProperty;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;

/**
 * Clone sampling unit attributes, mission attributes, and 
 * survey design objects
 * 
 * @author Emily
 *
 */
public class CaTemplateCloner implements IConservationAreaTemplateCloner {

	public CaTemplateCloner() {
	}

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine,
			IProgressMonitor monitor) throws Exception {

		monitor.beginTask(Messages.CaTemplateCloner_CopySurveyElement, 3);
		
		//clone mission attributes
		cloneMissionAttributes(engine);
		monitor.worked(1);
		engine.getSession().flush();
		
		//clone sampling unit attributes
		cloneSamplingUnitAttributes(engine);
		monitor.worked(1);
		engine.getSession().flush();
		
		//clone survey designs
		cloneDesigns(engine);
		monitor.worked(1);
		engine.getSession().flush();
		
		monitor.done();
		
	}
	
	/*
	 * Clone mission attributes
	 */
	@SuppressWarnings("unchecked")
	private void cloneMissionAttributes(ConservationAreaClonerEngine engine){
		List<MissionAttribute> attributes = engine.getSession()
				.createCriteria(MissionAttribute.class)
				.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
				.list();
			
		for (MissionAttribute attribute : attributes) {
			MissionAttribute clone = new MissionAttribute();
			clone.setConservationArea(engine.getNewCa());
			clone.setKeyId(attribute.getKeyId());
			clone.setType(attribute.getType());
			engine.copyLabels(attribute, clone);

			if (attribute.getAttributeList() != null) {
				clone.setAttributeList(new ArrayList<MissionAttributeListItem>());
				for (MissionAttributeListItem it : attribute.getAttributeList()) {
					MissionAttributeListItem cloneId = new MissionAttributeListItem();
					cloneId.setAttribute(clone);
					cloneId.setKeyId(it.getKeyId());
					cloneId.setListOrder(it.getListOrder());
					engine.copyLabels(it, cloneId);
					clone.getAttributeList().add(cloneId);
				}
			}
			engine.getSession().save(clone);
			engine.addConservationItemMapping(attribute, clone);
		}
	}
	
	/*
	 * Clone sampling unit attributes
	 */
	@SuppressWarnings("unchecked")
	private void cloneSamplingUnitAttributes(ConservationAreaClonerEngine engine){
		List<SamplingUnitAttribute> attributes = engine.getSession()
				.createCriteria(SamplingUnitAttribute.class)
				.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
				.list();
			
		for (SamplingUnitAttribute attribute : attributes) {
			SamplingUnitAttribute clone = new SamplingUnitAttribute();
			clone.setConservationArea(engine.getNewCa());
			clone.setKeyId(attribute.getKeyId());
			clone.setType(attribute.getType());
			engine.copyLabels(attribute, clone);
			engine.getSession().save(clone);
			engine.addConservationItemMapping(attribute, clone);
		}
	}
	
	/*
	 * Clone survey designs
	 */
	@SuppressWarnings("unchecked")
	private void cloneDesigns(ConservationAreaClonerEngine engine){
		List<SurveyDesign> designs = engine.getSession()
				.createCriteria(SurveyDesign.class)
				.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
				.list();
			
		for (SurveyDesign design : designs) {
			SurveyDesign clone = new SurveyDesign();
			clone.setConservationArea(engine.getNewCa());
			engine.copyLabels(design, clone);
			
			clone.setConfigurableModel( (ConfigurableModel) engine.getNewConservationItem(design.getConfigurableModel()));
			
			clone.setDescription(design.getDescription());
			clone.setEndDate(design.getEndDate());
			clone.setKeyId(design.getKeyId());
			clone.setStartDate(design.getStartDate());
			clone.setState(design.getState());
			clone.setTrackDistanceDirection(design.getTrackDistanceDirection());
			
			//clone mission properties
			if (design.getMissionProperties() != null){
				clone.setMissionProperties(new ArrayList<MissionProperty>());
				for (MissionProperty mp : design.getMissionProperties()){
					MissionProperty cln = new MissionProperty();
					cln.setAttribute((MissionAttribute)engine.getNewConservationItem(mp.getAttribute()));
					cln.setOrder(mp.getOrder());
					cln.setSurveyDesign(clone);
					
					clone.getMissionProperties().add(cln);
				}
			}
			
			//clone survey design
			if(design.getProperties() != null){
				clone.setProperties(new ArrayList<SurveyDesignProperty>());
				for (SurveyDesignProperty prop : design.getProperties()){
					SurveyDesignProperty cln = new SurveyDesignProperty();
					cln.setSurveyDesign(clone);
					cln.setName(prop.getName());
					cln.setValue(prop.getValue());
					clone.getProperties().add(cln);
				}
			}
			
			//clone sampling unit attributes
			if (design.getSamplingUnitAttributes() != null){
				clone.setSamplingUnitAttributes(new ArrayList<SurveyDesignSamplingUnitAttribute>());
				for (SurveyDesignSamplingUnitAttribute att : design.getSamplingUnitAttributes()){
					SurveyDesignSamplingUnitAttribute att2 = new SurveyDesignSamplingUnitAttribute();
					att2.setSamplingUnitAttribute((SamplingUnitAttribute) engine.getNewConservationItem(att.getSamplingUnitAttribute()));
					att2.setSurveyDesign(clone);
					clone.getSamplingUnitAttributes().add(att2);
				}
			}
			
			engine.getSession().save(clone);
			
			
			//It doesn't make sense to do this; usually if you are making a new
			//conservation area its going to be a different location with different
			//sampling units
//			//clone sampling unit & attribute values
//			List<SamplingUnit> units = engine.getSession().createCriteria(SamplingUnit.class)
//				.add(Restrictions.eq("surveyDesign", design))
//				.list();
//			for (SamplingUnit unit : units){
//				SamplingUnit cln = new SamplingUnit();
//				
//				cln.setBuffer(unit.getBuffer());
//				cln.setGeom(unit.getGeom());
//				cln.setId(unit.getId());
//				cln.setState(unit.getState());
//				cln.setSurveyDesign(clone);
//				cln.setType(unit.getType());
//				
//				if (unit.getAttributes().size() > 0){
//					cln.setAttributes(new ArrayList<SamplingUnitAttributeValue>());
//					
//					for (SamplingUnitAttributeValue value : unit.getAttributes()){
//						SamplingUnitAttributeValue clnv = new SamplingUnitAttributeValue();
//						clnv.setNumberValue(value.getNumberValue());
//						clnv.setSamplingUnit(cln);
//						clnv.setStringValue(value.getStringValue());
//						clnv.setSamplingUnitAttribute((SamplingUnitAttribute)engine.getNewConservationItem(value.getSamplingUnitAttribute()));
//						
//						cln.getAttributes().add(clnv);
//					}	
//				}
//				
//				engine.getSession().save(cln);
//			}
		}
	}
}
