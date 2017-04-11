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
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.i2.birt.IntelReportManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelEntityTypeAttributeGroup;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.model.IntelRelationshipGroup;
import org.wcs.smart.i2.model.IntelRelationshipType;

/**
 * Clones intelligence template details
 * 
 * @author Emily
 *
 */
@SuppressWarnings("unchecked")
public class ConservationAreaCloner implements IConservationAreaTemplateCloner{

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine,
			IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.ConservationAreaCloner_TaskName, 6);
		try{
			monitor.subTask(Messages.ConservationAreaCloner_AttributeSubTask);
			cloneAttributes(engine);
			monitor.worked(1);
			
			monitor.setTaskName(Messages.ConservationAreaCloner_EntityTypeSubTask);
			cloneEntityTypes(engine);
			monitor.worked(1);
			
			monitor.setTaskName(Messages.ConservationAreaCloner_GroupsSubTask);
			cloneRelationshipGroups(engine);
			monitor.worked(1);
			
			monitor.setTaskName(Messages.ConservationAreaCloner_RelationshiptypesSubTask);
			cloneRelationshipTypes(engine);
			monitor.worked(1);
			
			monitor.setTaskName(Messages.ConservationAreaCloner_SourceTypesSubTask);
			cloneRecordSource(engine);
			monitor.worked(1);
			
			//clone record template
			Path source = IntelReportManager.INSTANCE.getRecordTemplate(engine.getTemplateCa());
			Path target = IntelReportManager.INSTANCE.getRecordTemplate(engine.getNewCa());
			if (Files.exists(source)) FileUtils.copyFile(source.toFile(), target.toFile());
			monitor.worked(1);
		}finally{
			monitor.done();
		}
	}

	
	private void cloneAttributes(ConservationAreaClonerEngine engine){
		List<IntelAttribute> attributes = engine.getSession()
				.createCriteria(IntelAttribute.class)
				.add(Restrictions.eq("conservationArea", engine.getTemplateCa())).list(); //$NON-NLS-1$
		
		
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
		List<IntelEntityType> entityTypes = engine.getSession()
				.createCriteria(IntelEntityType.class)
				.add(Restrictions.eq("conservationArea", engine.getTemplateCa())).list(); //$NON-NLS-1$
		
		
		for (IntelEntityType ia : entityTypes){
			IntelEntityType clone = new IntelEntityType();
			clone.setConservationArea(engine.getNewCa());
			clone.setKeyId(ia.getKeyId());
			engine.copyLabels(ia, clone);
			clone.setIcon(ia.getIcon());
			clone.setIdAttribute((IntelAttribute)engine.getNewConservationItem(ia.getIdAttribute()));
			if (ia.getBirtTemplate() != null){
				Path source = IntelReportManager.INSTANCE.getEntityTemplate(ia);
				Path target = IntelReportManager.INSTANCE.getEntityTemplate(clone);
				if (Files.exists(source)){
					clone.setBirtTemplate(ia.getBirtTemplate());
					FileUtils.copyFile(source.toFile(), target.toFile());
				}
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
			
			
			engine.getSession().flush();
		}
	}
	

	private void cloneRelationshipGroups(ConservationAreaClonerEngine engine){
		List<IntelRelationshipGroup> relationshipGroups = engine.getSession()
				.createCriteria(IntelRelationshipGroup.class)
				.add(Restrictions.eq("conservationArea", engine.getTemplateCa())).list(); //$NON-NLS-1$
		
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
		List<IntelRelationshipType> relationshipGroups = engine.getSession()
				.createCriteria(IntelRelationshipType.class)
				.add(Restrictions.eq("conservationArea", engine.getTemplateCa())).list(); //$NON-NLS-1$
		
		for (IntelRelationshipType g : relationshipGroups){
			IntelRelationshipType clone = new IntelRelationshipType();
			engine.copyLabels(g, clone);
			clone.setConservationArea(engine.getNewCa());
			clone.setKeyId(g.getKeyId());
			clone.setIcon(g.getIcon());
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
			
			engine.getSession().save(clone);
			engine.getSession().flush();
		}
		
	}
	
	private void cloneRecordSource(ConservationAreaClonerEngine engine){
		
		List<IntelRecordSource> sources = engine.getSession()
				.createCriteria(IntelRecordSource.class)
				.add(Restrictions.eq("conservationArea", engine.getTemplateCa())).list(); //$NON-NLS-1$
		
		for (IntelRecordSource source : sources){
			IntelRecordSource clone = new IntelRecordSource();
			engine.copyLabels(source, clone);
			clone.setConservationArea(engine.getNewCa());
			clone.setIcon(source.getIcon());
			clone.setKeyId(source.getKeyId());
			clone.setAttributes(new ArrayList<IntelRecordSourceAttribute>());
			
			for (IntelRecordSourceAttribute attribute : source.getAttributes()){
				IntelRecordSourceAttribute aclone = new IntelRecordSourceAttribute();
				if (attribute.getAttribute() != null){
					aclone.setAttribute( (IntelAttribute)engine.getNewConservationItem(attribute.getAttribute()) );
				}
				if (attribute.getEntityType() != null){
					aclone.setEntityType( (IntelEntityType)engine.getNewConservationItem(attribute.getEntityType()) );
				}
				aclone.setIsMultiple(attribute.getIsMultiple());
				aclone.setOrder(attribute.getOrder());
				aclone.setSource(clone);
				engine.copyLabels(attribute, aclone);
				clone.getAttributes().add(aclone);
					
			}
			
			engine.getSession().save(clone);
			engine.getSession().flush();
		}
		
	}

}
