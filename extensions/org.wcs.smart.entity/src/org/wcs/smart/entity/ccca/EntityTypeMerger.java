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
package org.wcs.smart.entity.ccca;

import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.model.EntityType.Status;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryDataModelManager;

/**
 * Tools for merging entity types for cross conservation
 * area analysis and entity types.
 * 
 * @author Emily
 *
 */
public class EntityTypeMerger {

	/**
	 * Get all entity types
	 * @return
	 */
	public static List<EntityType> getEntityTypes() {
		
		final List<EntityType> newTypes = new ArrayList<EntityType>();
		
		Display.getDefault().syncExec(new Runnable(){

			@Override
			public void run() {
				ProgressMonitorDialog dialog = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
				try {
					dialog.run(true, false, new IRunnableWithProgress() {
						
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException,
								InterruptedException {
							Session s = HibernateManager.openSession();
							try{
								List<EntityType> tts = mergeEntityTypes(
										SmartDB.getConservationAreaConfiguration().getConservationAreas().toArray(new ConservationArea[SmartDB.getConservationAreaConfiguration().getConservationAreas().size()]),
										SmartDB.getConservationAreaConfiguration().getMainConservationArea(),
										s, monitor);
								newTypes.addAll(tts);
							}finally{
								s.close();
								
							}
							
						}
					});
				} catch (Exception e) {
					EntityPlugIn.displayLog(Messages.EntityTypeMerger_ErrorMergingEntityTypes + "\n\n" + e.getMessage(), e); //$NON-NLS-1$
				}
			}});
		
		
		return newTypes;
	}
	
	/**
	 * 
	 * @param cas
	 * @param defaultCa
	 * @param session
	 * @param monitor
	 * @return
	 */
	private static List<EntityType> mergeEntityTypes(ConservationArea[] cas, 
			ConservationArea defaultCa, 
			Session session, IProgressMonitor monitor){
		
		monitor.beginTask(Messages.EntityTypeMerger_ProgressLabel, 100);
		
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
			
			monitor.setTaskName(MessageFormat.format(Messages.EntityTypeMerger_ProgressLabelB, new Object[]{entityType}));
			
			EntityType shared = (EntityType) session.createCriteria(EntityType.class).add(Restrictions.eq("keyId", entityType)).add(Restrictions.eq("conservationArea", defaultCa)).list().get(0); //$NON-NLS-1$ //$NON-NLS-2$
			
			EntityType et = new EntityType();
			et.setType(shared.getType());
			et.setStatus(Status.ACTIVE);
			et.setKeyId(entityType);
			et.setName(shared.getName());
			et.setUuid(null);
			et.setDmAttribute( QueryDataModelManager.getInstance().getAttribute(session, shared.getDmAttribute().getKeyId()));

			//merge all attributes
			et.setAttributes(getAttributes(entityType, defaultCa, cas, session));
			for(EntityAttribute ea : et.getAttributes()){
				ea.setEntityType(et);
			}
			
			//set the entities to be empty
			et.setEntities(new ArrayList<Entity>());
			
			clonedTypes.add(et);
			
			
			monitor.worked( (int) ((i++ / ((float)keys.size())) * 100) );
		}
		return clonedTypes;
	}
	
	private static List<EntityAttribute> getAttributes(String entityType, ConservationArea defaultCa, ConservationArea[] cas,  Session session){
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
				ea.setDmAttribute( QueryDataModelManager.getInstance().getAttribute(session, (String)bits[2]) );
				
				attributes.add(ea);
				
				//set the name
				q = session.createQuery("FROM EntityAttribute e WHERE e.entityType.conservationArea = :ca and e.keyId = :key"); //$NON-NLS-1$
				q.setParameter("ca", defaultCa); //$NON-NLS-1$
				q.setParameter("key", ea.getKeyId()); //$NON-NLS-1$
				EntityAttribute defaultAtt = (EntityAttribute) q.list().get(0);
				ea.setName(defaultAtt.getName());
				
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
