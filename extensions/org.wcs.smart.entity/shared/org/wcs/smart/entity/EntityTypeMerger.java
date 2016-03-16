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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.model.Status;

/**
 * Merges entity types across Conservation Areas.
 * 
 * @author Emily
 *
 */
public class EntityTypeMerger {
	
	private Locale l;
	
	public EntityTypeMerger(Locale l){
		this.l = l;
	}

	/**
	 * Merges all entity types that occur in the list of cas.
	 * 
	 * @param cas Conservation Areas to merge entity types
	 * @param defaultCa  Main CA to use for labels
	 * @param session database connection
	 * @param monitor progress monitor, can be null
	 * @return
	 */
	public List<EntityType> mergeEntityTypes(ConservationArea[] cas, 
			ConservationArea defaultCa, 
			Session session, IProgressMonitor monitor){
		
		if (monitor != null ) monitor.beginTask(SmartContext.INSTANCE.getClass(IEntityLabelProvider.class).getLabel(IEntityLabelProvider.MERGE_PROGRESS1_KEY, l), 100);
		
		//entity types must have the same keyid, dm attribute keyid and type
		String hql = "SELECT count(*), e.keyId, e.type, a.keyId FROM EntityType e, Attribute a WHERE e.dmAttribute.uuid = a.uuid and e.conservationArea in (:ca) GROUP BY e.keyId, e.type, a.keyId";//$NON-NLS-1$
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
		int i = 0;
		for (String entityType : keys){
			if (monitor != null ) monitor.setTaskName(SmartContext.INSTANCE.getClass(IEntityLabelProvider.class).getLabel(IEntityLabelProvider.MERGE_PROGRESS1_KEY, l));
			
			EntityType shared = (EntityType) session
					.createCriteria(EntityType.class)
					.add(Restrictions.eq("keyId", entityType)) //$NON-NLS-1$
					.add(Restrictions.eq("conservationArea", defaultCa)) //$NON-NLS-1$
					.list().get(0);
			
			EntityType et = new EntityType();
			et.setType(shared.getType());
			et.setStatus(Status.ACTIVE);
			et.setKeyId(entityType);
			et.setName(shared.getName());
			et.setUuid(null);
			et.setDmAttribute(  findAttribute(session,  shared.getDmAttribute().getKeyId(), cas, defaultCa) );

			//merge all attributes
			et.setAttributes(getAttributes(entityType, defaultCa, cas, session));
			for(EntityAttribute ea : et.getAttributes()){
				ea.setEntityType(et);
			}
			
			//set the entities to be empty
			et.setEntities(new ArrayList<Entity>());
			
			clonedTypes.add(et);
			
			i++;
			if (monitor != null ) monitor.worked( (int) ((i/ ((float)keys.size())) * 100) );
		}
		return clonedTypes;
	}
	
	private Attribute cloneAttribute(Attribute a, ConservationArea defaultCa){
		Attribute copy = new Attribute();
		copy.setAggregations(a.getAggregations());
		copy.setConservationArea(defaultCa);
		copy.setIsRequired(a.getIsRequired());
		copy.setKeyId(a.getKeyId());
		copy.setMaxValue(a.getMaxValue());
		copy.setMinValue(a.getMinValue());
		copy.setName(a.getName());
		copy.setRegex(a.getRegex());
		copy.setType(a.getType());
		return copy;
	}
	
	@SuppressWarnings("unchecked")
	private Attribute findAttribute(Session session, String keyId, ConservationArea[] cas, ConservationArea defaultCa){
		Attribute a = (Attribute) session.createCriteria(Attribute.class)
				.add(Restrictions.eq("conservationArea", defaultCa)) //$NON-NLS-1$
				.add(Restrictions.eq("keyId", keyId)) //$NON-NLS-1$
				.uniqueResult();
		if (a != null ) return cloneAttribute(a, defaultCa);
		
		List<Attribute> attributes = (List<Attribute>) session.createCriteria(Attribute.class)
				.add(Restrictions.in("conservationArea", cas)) //$NON-NLS-1$
				.add(Restrictions.eq("keyId", keyId)) //$NON-NLS-1$
				.list();
		if (attributes.size() > 0){
			return cloneAttribute(attributes.get(0), defaultCa);
		}
		return null;
	}
	
	private List<EntityAttribute> getAttributes(String entityType, ConservationArea defaultCa, ConservationArea[] cas,  Session session){
		String hql = "SELECT count(*), e.keyId, a.keyId FROM EntityAttribute e, Attribute a " //$NON-NLS-1$
			+ "WHERE e.dmAttribute.uuid = a.uuid and e.entityType.keyId = :entityType and e.entityType.conservationArea in (:cas) "  //$NON-NLS-1$
			+ " GROUP BY e.keyId, a.keyId";//$NON-NLS-1$
		
		Query q = session.createQuery(hql);
		q.setParameter("entityType", entityType);//$NON-NLS-1$
		q.setParameterList("cas", cas); //$NON-NLS-1$
		
		List<EntityAttribute> attributes = new ArrayList<EntityAttribute>();
		List<?> data = q.list();
		for (Object d : data ){
			Object[] bits = (Object[])d;
			Long cnt = (Long)bits[0];
			if(cnt == cas.length){
				//we want to keep this attribute
				EntityAttribute ea = new EntityAttribute();
				ea.setKeyId((String)bits[1]);
				ea.setDmAttribute( findAttribute(session, (String)bits[2], cas, defaultCa) );
				
				attributes.add(ea);
				
				//set the name
				q = session.createQuery("FROM EntityAttribute e WHERE e.entityType.conservationArea = :ca and e.keyId = :key"); //$NON-NLS-1$
				q.setParameter("ca", defaultCa); //$NON-NLS-1$
				q.setParameter("key", ea.getKeyId()); //$NON-NLS-1$
				EntityAttribute defaultAtt = (EntityAttribute) q.list().get(0);
				ea.setName(defaultAtt.getName());

				//is primary
				q = session.createQuery("FROM EntityAttribute e WHERE e.entityType.conservationArea in (:ca) and e.keyId = :key and is_primary = :primary"); //$NON-NLS-1$
				q.setParameterList("ca", cas); //$NON-NLS-1$
				q.setParameter("key", ea.getKeyId()); //$NON-NLS-1$
				q.setParameter("primary", Boolean.TRUE); //$NON-NLS-1$
				if (q.list().size() > 0){
					ea.setIsPrimary(true);
				}
				
			}else{
				//not shared between all cas
			}
		}
		
		//sort attribute alphabetically
		Collections.sort(attributes, new Comparator<EntityAttribute>() {
			@Override
			public int compare(EntityAttribute o1, EntityAttribute o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}
		});
		return attributes;
	}
	
}
