/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.handlers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.i2.birt.IntelReportManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelConfigurationOption;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelEntityTypeAttributeGroup;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelProfileEntityType;
import org.wcs.smart.i2.model.IntelProfileRecordSource;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.model.IntelRelationshipGroup;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.model.IntelRelationshipTypeAttribute;
import org.wcs.smart.i2.model.RelationshipDiagramEntityTypeStyle;
import org.wcs.smart.i2.model.RelationshipDiagramRelationshipTypeStyle;
import org.wcs.smart.i2.model.RelationshipDiagramStyle;

/**
 * Clones intelligence template details
 * 
 * @author Emily
 *
 */
public class ConservationAreaCloner implements IConservationAreaTemplateCloner{

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine,
			IProgressMonitor monitor) throws Exception {
		
		SubMonitor progress = SubMonitor.convert(monitor, Messages.ConservationAreaCloner_TaskName, 7);
		
		progress.subTask(Messages.ConservationAreaCloner_AttributeSubTask);
		cloneAttributes(engine);
		progress.worked(1);
		
		progress.subTask(Messages.ConservationAreaCloner_Profiles);
		cloneProfiles(engine);
		progress.worked(1);
		
		progress.subTask(Messages.ConservationAreaCloner_EntityTypeSubTask);
		cloneEntityTypes(engine);
		progress.worked(1);
		
		progress.subTask(Messages.ConservationAreaCloner_GroupsSubTask);
		cloneRelationshipGroups(engine);
		progress.worked(1);
		
		progress.subTask(Messages.ConservationAreaCloner_RelationshiptypesSubTask);
		cloneRelationshipTypes(engine);
		progress.worked(1);
		
		progress.subTask(Messages.ConservationAreaCloner_SourceTypesSubTask);
		cloneRecordSource(engine);
		progress.worked(1);
			
		progress.subTask(Messages.ConservationAreaCloner_SettingsSubTask);
		cloneSettings(engine);
		progress.worked(1);

		progress.subTask(Messages.ConservationAreaCloner_DiagramStylesSubTask);
		cloneStyles(engine);
		progress.worked(1);
		
		//clone record template
		Path source = IntelReportManager.INSTANCE.getRecordTemplate(engine.getTemplateCa());
		Path target = IntelReportManager.INSTANCE.getRecordTemplate(engine.getNewCa());
		if (Files.exists(source)) FileUtils.copyFile(source.toFile(), target.toFile());
		progress.worked(1);
		
	}

	private void cloneSettings(ConservationAreaClonerEngine engine){
		List<IntelConfigurationOption> options = QueryFactory.buildQuery(engine.getSession(), 
				IntelConfigurationOption.class, new Object[] {"conservationArea", engine.getTemplateCa()}).list(); //$NON-NLS-1$
		for (IntelConfigurationOption option: options) {
			IntelConfigurationOption clone = new IntelConfigurationOption();
			clone.setConservationArea(engine.getNewCa());
			clone.setKey(option.getKey());
			clone.setValue(option.getValue());
			
			engine.getSession().save(clone);
		}
		engine.getSession().flush();
	}
	
	private void cloneAttributes(ConservationAreaClonerEngine engine){
		List<IntelAttribute> attributes = QueryFactory.buildQuery(engine.getSession(), IntelAttribute.class, "conservationArea", engine.getTemplateCa()).list(); //$NON-NLS-1$
		
		for (IntelAttribute ia : attributes){
			IntelAttribute clone = new IntelAttribute();
			clone.setConservationArea(engine.getNewCa());
			clone.setKeyId(ia.getKeyId());
			engine.copyLabels(ia, clone);
			clone.setType(ia.getType());
			
			if (ia.getAttributeList() != null){
				clone.setAttributeList(new ArrayList<IntelAttributeListItem>());
				for (IntelAttributeListItem i : ia.getAttributeList()){
					IntelAttributeListItem clonei = new IntelAttributeListItem();
					clonei.setAttribute(clone);
					clonei.setOrder(i.getOrder());
					clonei.setKeyId(i.getKeyId());
					engine.copyLabels(i, clonei);
					clone.getAttributeList().add(clonei);
				}
			}
			engine.addConservationItemMapping(ia, clone);
			engine.getSession().save(clone);
			engine.getSession().flush();
		}	
	}
	
	private void cloneEntityTypes(ConservationAreaClonerEngine engine) throws Exception{
		List<IntelEntityType> entityTypes = QueryFactory.buildQuery(engine.getSession(), IntelEntityType.class, "conservationArea", engine.getTemplateCa()).list(); //$NON-NLS-1$
		
		for (IntelEntityType ia : entityTypes){
			IntelEntityType clone = new IntelEntityType();
			clone.setConservationArea(engine.getNewCa());
			clone.setKeyId(ia.getKeyId());
			engine.copyLabels(ia, clone);
			clone.setIcon(ia.getIcon());
			clone.setIdAttribute((IntelAttribute)engine.getNewConservationItem(ia.getIdAttribute()));
			clone.setProfiles(new HashSet<>());
			if (ia.getBirtTemplate() != null){
				clone.setBirtTemplate(ia.getBirtTemplate());
				Path source = IntelReportManager.INSTANCE.getEntityTemplate(ia);
				Path target = IntelReportManager.INSTANCE.getEntityTemplate(clone);
				if (Files.exists(source)){	
					FileUtils.copyFile(source.toFile(), target.toFile());
				}else{
					clone.setBirtTemplate(null);
				}
			}
			
			if (ia.getDmAttribute() != null) {
				Attribute dmAttribute = (Attribute)engine.getSession().createQuery("From Attribute WHERE conservationArea = :ca and keyId = :keyId") //$NON-NLS-1$
						.setParameter("ca", engine.getNewCa()) //$NON-NLS-1$
						.setParameter("keyId", ia.getDmAttribute().getKeyId()) //$NON-NLS-1$
						.uniqueResult();
				clone.setDmAttribute(dmAttribute);
				
				//remove all list items associated with this attribute
				//as we don't want to clone those
				if (dmAttribute != null) {
					for (AttributeListItem li : dmAttribute.getAttributeList()) {
						li.setAttribute(null);
					}
					dmAttribute.getAttributeList().clear();
					engine.getSession().save(dmAttribute);
				}
				clone.setActiveFilter(ia.getActiveFilter());
			}
			
			engine.getSession().save(clone);
			
			if (ia.getAttributes() != null){
				clone.setAttributes(new ArrayList<IntelEntityTypeAttribute>());
				HashMap<UUID, IntelEntityTypeAttributeGroup> groups = new HashMap<>();
				
				for (IntelEntityTypeAttribute i : ia.getAttributes()){
					IntelEntityTypeAttribute iclone = new IntelEntityTypeAttribute();
					iclone.setAttribute((IntelAttribute)engine.getNewConservationItem(i.getAttribute()));
					
					if (i.getAttributeGroup() != null){
						IntelEntityTypeAttributeGroup group = groups.get(i.getAttributeGroup().getUuid());
						if (group == null){
							group = new IntelEntityTypeAttributeGroup();
							group.setEntityType(clone);
							group.setOrder(i.getAttributeGroup().getOrder());
							engine.copyLabels(i.getAttributeGroup(), group);
							groups.put(i.getAttributeGroup().getUuid(), group);
							engine.getSession().save(group);
						}
						iclone.setAttributeGroup(group);
					}
					iclone.setEntityType(clone);
					iclone.setOrder(i.getOrder());
					clone.getAttributes().add(iclone);
				}
			}
			engine.addConservationItemMapping(ia, clone);
			
			for (IntelProfileEntityType p : ia.getProfiles()) {
				IntelProfile newp = engine.getNewConservationItem(p.getProfile());
				
				IntelProfileEntityType newmap = new IntelProfileEntityType();
				newmap.setProfile(newp);
				newmap.setEntityType(clone);
				
				clone.getProfiles().add(newmap);
				newp.getEntityTypes().add(newmap);
			}
			
			engine.getSession().flush();
		}
	}
	

	private void cloneRelationshipGroups(ConservationAreaClonerEngine engine){
		List<IntelRelationshipGroup> relationshipGroups = QueryFactory.buildQuery(engine.getSession(), IntelRelationshipGroup.class, "conservationArea", engine.getTemplateCa()).list(); //$NON-NLS-1$

		for (IntelRelationshipGroup g : relationshipGroups){
			IntelRelationshipGroup clone = new IntelRelationshipGroup();
			engine.copyLabels(g, clone);
			clone.setConservationArea(engine.getNewCa());
			clone.setKeyId(g.getKeyId());
			engine.addConservationItemMapping(g, clone);
			
			engine.getSession().save(clone);
			engine.getSession().flush();
		}
	}
	
	private void cloneRelationshipTypes(ConservationAreaClonerEngine engine){
		List<IntelRelationshipType> relationshipGroups = QueryFactory.buildQuery(engine.getSession(), IntelRelationshipType.class, "conservationArea", engine.getTemplateCa()).list(); //$NON-NLS-1$

		for (IntelRelationshipType g : relationshipGroups){
			IntelRelationshipType clone = new IntelRelationshipType();
			engine.copyLabels(g, clone);
			clone.setConservationArea(engine.getNewCa());
			clone.setKeyId(g.getKeyId());
			clone.setIcon(g.getIcon());
			
			clone.setSourceProfile( engine.getNewConservationItem(g.getSourceProfile()) );
			clone.setTargetProfile( engine.getNewConservationItem(g.getTargetProfile()) );
			
			if (g.getSourceEntityType() != null){
				clone.setSourceEntityType((IntelEntityType)engine.getNewConservationItem(g.getSourceEntityType()));
			}
			if (g.getTargetEntityType() != null){
				clone.setTargetEntityType((IntelEntityType)engine.getNewConservationItem(g.getTargetEntityType()));
			}
			if (g.getRelationshipGroup() != null){
				IntelRelationshipGroup group = (IntelRelationshipGroup)engine.getNewConservationItem(g.getRelationshipGroup());
				clone.setRelationshipGroup(group);
				if (group.getRelationshipTypes() == null){
					group.setRelationshipTypes(new ArrayList<IntelRelationshipType>());
				}
				group.getRelationshipTypes().add(clone);
				engine.getSession().save(group);	
			}
			
			if (g.getAttributes() != null){
				clone.setAttributes(new ArrayList<>(g.getAttributes().size()));
				for (IntelRelationshipTypeAttribute a : g.getAttributes()){
					IntelRelationshipTypeAttribute aclone = new IntelRelationshipTypeAttribute();
					aclone.setRelationshipType(clone);
					aclone.setOrder(a.getOrder());
					aclone.setAttribute((IntelAttribute)engine.getNewConservationItem(a.getAttribute()));
					
					clone.getAttributes().add(aclone);
				}
			}
			
			engine.getSession().save(clone);
			engine.getSession().flush();

			engine.addConservationItemMapping(g, clone);
		}
		
	}
	
	private void cloneRecordSource(ConservationAreaClonerEngine engine){
		List<IntelRecordSource> sources = QueryFactory.buildQuery(engine.getSession(), IntelRecordSource.class, "conservationArea", engine.getTemplateCa()).list(); //$NON-NLS-1$
		
		for (IntelRecordSource source : sources){
			IntelRecordSource clone = new IntelRecordSource();
			engine.copyLabels(source, clone);
			clone.setConservationArea(engine.getNewCa());
			clone.setIcon(source.getIcon());
			clone.setKeyId(source.getKeyId());
			clone.setAttributes(new ArrayList<IntelRecordSourceAttribute>());
			clone.setProfiles(new HashSet<>());
			for (IntelRecordSourceAttribute attribute : source.getAttributes()){
				IntelRecordSourceAttribute aclone = new IntelRecordSourceAttribute();
				if (attribute.getAttribute() != null){
					aclone.setAttribute( (IntelAttribute)engine.getNewConservationItem(attribute.getAttribute()) );
				}
				if (attribute.getEntityType() != null){
					aclone.setEntityType( (IntelEntityType)engine.getNewConservationItem(attribute.getEntityType()) );
				}
				aclone.setKeyId(attribute.getKeyId());
				aclone.setIsMultiple(attribute.getIsMultiple());
				aclone.setOrder(attribute.getOrder());
				aclone.setSource(clone);
				engine.copyLabels(attribute, aclone);
				clone.getAttributes().add(aclone);
					
			}
			
			for (IntelProfileRecordSource p : source.getProfiles()) {
				IntelProfile newp = engine.getNewConservationItem(p.getProfile());
				
				IntelProfileRecordSource newmap = new IntelProfileRecordSource();
				newmap.getId().setProfile(newp);
				newmap.getId().setRecordSource(clone);
				
				clone.getProfiles().add(newmap);
				newp.getRecordSources().add(newmap);
			}
			engine.getSession().save(clone);
			engine.getSession().flush();
		}
		
	}

	
	private void cloneStyles(ConservationAreaClonerEngine engine) {
		List<RelationshipDiagramStyle> styles = QueryFactory.buildQuery(engine.getSession(), RelationshipDiagramStyle.class, "conservationArea", engine.getTemplateCa()).list(); //$NON-NLS-1$

		for (RelationshipDiagramStyle s : styles) {
			RelationshipDiagramStyle clone = new RelationshipDiagramStyle();
			engine.copyLabels(s, clone);
			clone.setConservationArea(engine.getNewCa());
			clone.setDefault(s.isDefault());
			clone.setOptions(s.getOptions());
			for (RelationshipDiagramEntityTypeStyle ets : s.getEntityTypeStyles().values()) {
				RelationshipDiagramEntityTypeStyle etsClone = new RelationshipDiagramEntityTypeStyle();
				etsClone.setEntityType((IntelEntityType)engine.getNewConservationItem(ets.getEntityType()));
				etsClone.setStyle(clone);
				etsClone.setOptions(ets.getOptions());
				clone.getEntityTypeStyles().put(etsClone.getEntityType(), etsClone);
			}
			for (RelationshipDiagramRelationshipTypeStyle rts : s.getRelationshipTypeStyles().values()) {
				RelationshipDiagramRelationshipTypeStyle rtsClone = new RelationshipDiagramRelationshipTypeStyle();
				rtsClone.setRelationshipType((IntelRelationshipType)engine.getNewConservationItem(rts.getRelationshipType()));
				rtsClone.setStyle(clone);
				rtsClone.setOptions(rts.getOptions());
				clone.getRelationshipTypeStyles().put(rtsClone.getRelationshipType(), rtsClone);
			}
			
			engine.getSession().save(clone);
			engine.getSession().flush();
		}

	}

	private void cloneProfiles(ConservationAreaClonerEngine engine) {
		List<IntelProfile> profiles = QueryFactory.buildQuery(engine.getSession(), IntelProfile.class, 
				"conservationArea", engine.getTemplateCa()).list(); //$NON-NLS-1$
		
		for (IntelProfile source : profiles) {
			IntelProfile target = new IntelProfile();
			target.setConservationArea(engine.getNewCa());
			target.setColor(source.getColor());
			target.setEntityTypes(new HashSet<>());
			target.setKeyId(source.getKeyId());
			target.setRecordSources(new HashSet<>());
			
			engine.copyLabels(source, target);
			
			engine.getSession().save(target);
			engine.getSession().flush();
			
			engine.addConservationItemMapping(source, target);
		}

	}
}
