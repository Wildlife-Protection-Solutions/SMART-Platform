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
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;

public class EntityTemplateCloner implements IConservationAreaTemplateCloner {

	private ConservationAreaClonerEngine engine;
	
	public EntityTemplateCloner() {
	}

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine,
			IProgressMonitor monitor) throws Exception {
		this.engine = engine;
		monitor.beginTask("Cloning Entity Types", 10);
		try{
			cloneEntityTypes(monitor);
			monitor.worked(10);
		}finally{
			monitor.done();
		}
	}
	
	private void cloneEntityTypes(IProgressMonitor monitor) throws Exception{
		
		List<EntityType> toClone = engine.getSession().createCriteria(EntityType.class).add(Restrictions.eq("conservationArea", engine.getTemplateCa())).list();
		IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
		subMonitor.beginTask("Cloning Entity Types", toClone.size());
		for (EntityType et : toClone){
			subMonitor.subTask(MessageFormat.format("Cloning {0}", new Object[]{et.getName()}));
			EntityType clone = new EntityType();
			
			clone.setConservationArea(engine.getNewCa());
			clone.setCreator(engine.getNewCa().getEmployees().get(0));
			clone.setDateCreated(new Date());
			
			clone.setDmAttribute(findNewAttribute(et.getDmAttribute()));
			
			clone.setId(et.getId());
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
			subMonitor.worked(1);
		}
		subMonitor.done();
		
	}

	private Attribute findNewAttribute(Attribute oldAttribute) throws Exception{
		List<?> attributes = engine.getSession().createCriteria(Attribute.class).add(Restrictions.eq("conservationArea", engine.getNewCa())).add(Restrictions.eq("keyId", oldAttribute.getKeyId())).list(); //$NON-NLS-1$ //$NON-NLS-2$
		if (attributes.size() == 1){
			return (Attribute) attributes.get(0);
		}
		throw new Exception(MessageFormat.format("Cloned attribute could not be found for key {0}.", new Object[]{oldAttribute.getKeyId()}));
	}
}
