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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.IAttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelDatamodelEvent;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelRelationshipGroup;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.model.IntelRelationshipTypeAttribute;

/**
 * Clones required components of the intelligence plugin.
 * 
 * @author Emily
 *
 */
public class ConservationAreaCloner implements IConservationAreaTemplateCloner {

	@SuppressWarnings("unchecked")
	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine,
			IProgressMonitor monitor) throws Exception {

		//clone attributes and attribute list items
		List<IntelAttribute> attributes = engine.getSession().createCriteria(IntelAttribute.class)
				.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
				.list();
		
		for (IntelAttribute attribute: attributes){
			IntelAttribute clone = new IntelAttribute();
			
			clone.setConservationArea(engine.getNewCa());
			engine.copyLabels(attribute, clone);
			clone.setKeyId(attribute.getKeyId());
			clone.setType(attribute.getType());
			
			if (attribute.getType() == IAttributeType.LIST){
				clone.setAttributeList(new ArrayList<IntelAttributeListItem>());
				for (IntelAttributeListItem item : attribute.getAttributeList()){
					IntelAttributeListItem clonei = new IntelAttributeListItem();
					clonei.setAttribute(clone);
					clonei.setKeyId(item.getKeyId());
					engine.copyLabels(item, clonei);
					
					clone.getAttributeList().add(clonei);
				}
			}
			
			engine.getSession().save(clone);
			engine.getSession().flush();
			
			engine.addConservationItemMapping(attribute, clone);
		}
		
		//clone relationship groups
		List<IntelRelationshipGroup> groups = engine.getSession().createCriteria(IntelRelationshipGroup.class)
				.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
				.list();
		
		for (IntelRelationshipGroup group : groups){
			IntelRelationshipGroup clone = new IntelRelationshipGroup();
			clone.setConservationArea(engine.getNewCa());
			clone.setKeyId(group.getKeyId());
			engine.copyLabels(group, clone);
			clone.setRelationshipTypes(new ArrayList<IntelRelationshipType>());
			
			engine.getSession().save(clone);
			engine.getSession().flush();
			
			engine.addConservationItemMapping(group, clone);
		}
		
		//clone entity types
		List<IntelEntityType> entityTypes = engine.getSession().createCriteria(IntelEntityType.class)
				.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
				.list();
		
		for (IntelEntityType etype : entityTypes){
			IntelEntityType clone = new IntelEntityType();
			clone.setConservationArea(engine.getNewCa());
			clone.setKeyId(etype.getKeyId());
			engine.copyLabels(etype, clone);
			
			File src = new File(engine.getTemplateCa().getFileDataStoreLocation(), etype.getBirtTemplate());
			File dest = new File(engine.getNewCa().getFileDataStoreLocation(), etype.getBirtTemplate());
			FileUtils.copyFile(src, dest);
			
			clone.setBirtTemplate(etype.getBirtTemplate());
			clone.setIcon(etype.getIcon());
			clone.setIdAttribute((IntelAttribute)engine.getNewConservationItem(etype.getIdAttribute()));
			clone.setAttributes(new ArrayList<IntelEntityTypeAttribute>());
			for (IntelEntityTypeAttribute atype : etype.getAttributes()){
				IntelEntityTypeAttribute aclone = new IntelEntityTypeAttribute();
				aclone.setAttribute( (IntelAttribute)engine.getNewConservationItem(atype.getAttribute()) );
				aclone.setEntityType(clone);
				clone.getAttributes().add(aclone);
			}
			
			engine.getSession().save(clone);
			engine.getSession().flush();
			
			engine.addConservationItemMapping(etype, clone);
			
		}
		
		
		//clone relationship types
		List<IntelRelationshipType> types = engine.getSession().createCriteria(IntelRelationshipType.class)
				.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
				.list();
		
		for (IntelRelationshipType type : types){
			IntelRelationshipType clone = new IntelRelationshipType();
			clone.setConservationArea(engine.getNewCa());
			clone.setKeyId(type.getKeyId());
			engine.copyLabels(type, clone);
			
			clone.setAttributes(new ArrayList<IntelRelationshipTypeAttribute>());
			for (IntelRelationshipTypeAttribute attribute : type.getAttributes()){
				IntelRelationshipTypeAttribute aclone = new IntelRelationshipTypeAttribute();
				
				
				IntelAttribute newAttribute = (IntelAttribute) engine.getNewConservationItem(attribute.getAttribute());
				aclone.setAttribute(newAttribute);
				aclone.setRelationshipType(clone);
				
				clone.getAttributes().add(aclone);
			}
			
			clone.setIcon(type.getIcon());
			IntelRelationshipGroup group = (IntelRelationshipGroup) engine.getNewConservationItem(type.getRelationshipGroup());
			group.getRelationshipTypes().add(clone);
			clone.setRelationshipGroup(group);
			
			clone.setSourceEntityType((IntelEntityType)engine.getNewConservationItem(type.getSourceEntityType()));
			clone.setTargetEntityType((IntelEntityType)engine.getNewConservationItem(type.getTargetEntityType()));
			
			engine.getSession().save(clone);
			engine.getSession().flush();
			
			engine.addConservationItemMapping(type, clone);
		}

		//clone data model events
		List<IntelDatamodelEvent> dmEvents = engine.getSession().createCriteria(IntelDatamodelEvent.class)
				.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
				.list();
		for (IntelDatamodelEvent dmEvent : dmEvents){
			IntelDatamodelEvent clone = new IntelDatamodelEvent();
			if (dmEvent.getAttributeListItem() != null){
				clone.setAttributeListItem( (AttributeListItem)engine.getNewConservationItem(dmEvent.getAttributeListItem()) );
			}
			if (dmEvent.getAttributeTreeNode() != null){
				clone.setAttributeTreeNode( (AttributeTreeNode)engine.getNewConservationItem(dmEvent.getAttributeTreeNode()) );
			}
			
			clone.setConservationArea(engine.getNewCa());
			if (dmEvent.getCategory() != null){
				clone.setCategory((Category)engine.getNewConservationItem(dmEvent.getCategory()));
			}
			
			engine.getSession().save(clone);
		}
	}

}
