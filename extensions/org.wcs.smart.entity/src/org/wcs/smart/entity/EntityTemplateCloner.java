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
package org.wcs.smart.entity;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;

/**
 * Clones entity types when a conservation area is cloned.  Will not
 * clone any entities.
 * 
 * @author Emily
 *
 */
public class EntityTemplateCloner implements IConservationAreaTemplateCloner {

	private ConservationAreaClonerEngine engine;
	
	public EntityTemplateCloner() {
	}

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine,
			IProgressMonitor monitor) throws Exception {
		this.engine = engine;
		monitor.beginTask(Messages.EntityTemplateCloner_ProgressCloningTypes, 10);
		try{
			cloneEntityTypes(new SubProgressMonitor(monitor, 10));
		}finally{
			monitor.done();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void cloneEntityTypes(IProgressMonitor monitor) throws Exception{
		List<EntityType> toClone = engine.getSession().createCriteria(EntityType.class).add(Restrictions.eq("conservationArea", engine.getTemplateCa())).list(); //$NON-NLS-1$
		monitor.beginTask(Messages.EntityTemplateCloner_ProgressCloningTypes2, toClone.size());
		for (EntityType et : toClone){
			monitor.subTask(MessageFormat.format(Messages.EntityTemplateCloner_ProgressCloning3, new Object[]{et.getName()}));
			EntityType clone = new EntityType();
			
			clone.setConservationArea(engine.getNewCa());
			clone.setCreator(engine.getNewCa().getEmployees().get(0));
			clone.setDateCreated(new Date());
			
			//remove list items from attribute list; we don't want to
			//be cloning the actual entity entries
			Attribute dmAttribute = findNewAttribute(et.getDmAttribute());
			for (AttributeListItem it : dmAttribute.getAttributeList()){
				it.setAttribute(null);
			}
			dmAttribute.getAttributeList().clear();
			engine.getSession().save(dmAttribute);
			
			clone.setDmAttribute(findNewAttribute(et.getDmAttribute()));
			
			clone.setKeyId(et.getKeyId());
			engine.copyLabels(et, clone);
			clone.setStatus(et.getStatus());
			clone.setType(et.getType());
			
			clone.setAttributes(new ArrayList<EntityAttribute>());
			
			for (EntityAttribute ea : et.getAttributes()){
				EntityAttribute aclone = new EntityAttribute();
				aclone.setDmAttribute(findNewAttribute(ea.getDmAttribute()));
				aclone.setEntityType(clone);
				aclone.setIsPrimary(ea.getIsPrimary());
				aclone.setIsRequired(ea.getIsRequired());
				aclone.setKeyId(ea.getKeyId());
				engine.copyLabels(ea, aclone);
				aclone.setOrder(ea.getOrder());
				
				clone.getAttributes().add(aclone);
			}
			
			engine.getSession().save(clone);
			monitor.worked(1);
		}
		monitor.done();
		
	}

	private Attribute findNewAttribute(Attribute oldAttribute) throws Exception{
		List<?> attributes = engine.getSession().createCriteria(Attribute.class).add(Restrictions.eq("conservationArea", engine.getNewCa())).add(Restrictions.eq("keyId", oldAttribute.getKeyId())).list(); //$NON-NLS-1$ //$NON-NLS-2$
		if (attributes.size() == 1){
			return (Attribute) attributes.get(0);
		}
		throw new Exception(MessageFormat.format(Messages.EntityTemplateCloner_AttributeNotFound, new Object[]{oldAttribute.getKeyId()}));
	}
}
