package org.wcs.smart.entity;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.model.EntityType.Status;
import org.wcs.smart.query.QueryDataModelManager;

public class EntityTypeMerger {

	
	public List<EntityType> mergeEntityTypes(ConservationArea[] cas, 
			ConservationArea defaultCa, 
			Session session, IProgressMonitor monitor){
		

		//entity types must have the same keyid, dm attribute keyid and type
		String hql = "SELECT count(*), e.keyId, e.type, a.keyId FROM EntityType e, Attribute a WHERE e.dm_attribute_uuid = a.uuid and e.conservationArea in (:ca) GROUP BY e.keyId, e.type, a.keyId";//$NON-NLS-1$
		Query q = session.createQuery(hql);
		q.setParameterList("ca", cas);//$NON-NLS-1$
		
		List<?> data = q.list();
		List<String> keys = new ArrayList<String>();
		for (Object d : data){
			Object[] bits = (Object[])d;
			
			Long cnt  = (Long) bits[0];
			if (cnt == cas.length){
				keys.add((String)bits[1]);
			}
		}
		
		List<EntityType> clonedTypes = new ArrayList<EntityType>();
		for (String entityType : keys){
			
			EntityType shared = (EntityType) session.createCriteria(EntityType.class).add(Restrictions.eq("keyId", entityType)).add(Restrictions.eq("conservationArea", defaultCa)).list().get(0);
			
			EntityType et = new EntityType();
			et.setType(shared.getType());
			et.setStatus(Status.ACTIVE);
			et.setKeyId(entityType);
			et.setName(shared.getName());
			et.setUuid(null);
			et.setDmAttribute( QueryDataModelManager.getInstance().getAttribute(session, shared.getDmAttribute().getKeyId()));

			//merge all attributes
			et.setAttributes(attributes);

			//set the entities to be empty
			et.setEntities(new ArrayList<Entity>());
		}
		
		
		
		
		return null;
		
	}
	
	private List<EntityAttribute> getAttributes(String entityType, ConservationArea[] cas, Session session){
		String hql = "SELECT count(*), e.keyId, a.keyId FROM EntityAttribute e, Attribute a WHERE e.dm_attribute_uuid = a.uuid and e.entityType.keyId = :entityType GROUP BY e.keyId, e.type, a.keyId";//$NON-NLS-1$
		
		Query q = session.createQuery(hql);
		q.setParameter("entityType", entityType);//$NON-NLS-1$
		
		List<EntityAttribute> attributes = new ArrayList<EntityAttribute>();
		List<?> data = q.list();
		for (Object d : data ){
			Object[] bits = (Object[])d;
			Long cnt = (Long)bits[0];
			if(cnt == cas.length){
				//we want to keep this attribute
				EntityAttribute ea = new EntityAttribute();
				ea.setKeyId((String)bits[1]);
				ea.setDmAttribute( QueryDataModelManager.getInstance().getAttribute(session, (String)bits[2]) );
				//TODO: need to set the name
				attributes.add(ea);
			}else{
				//not shared between all cas
			}
		}
		
		return attributes;
	}
}
