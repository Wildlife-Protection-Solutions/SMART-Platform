package org.wcs.smart.i2;

import java.text.MessageFormat;
import java.util.UUID;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.i2.model.IntelWorkingSetEntity;
import org.wcs.smart.i2.model.IntelWorkingSetQuery;
import org.wcs.smart.i2.model.IntelWorkingSetRecord;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;

public enum WorkingSetManager {

	INSTANCE;
	
	private UUID activeWorkingSet = null;
	
	public boolean isSet(){
		return activeWorkingSet != null;
	}
	
	public UUID getActiveWorkingSet(){
		return this.activeWorkingSet;
	}
	public void setActiveWorkingSet(IntelWorkingSet active, IEclipseContext context){
		this.activeWorkingSet = active == null ? null : active.getUuid();
		fireEvent(IntelEvents.ACTIVE_WS_SET, active, context);
	}
	
	public void deleteWorkingSet(Session s, IntelWorkingSet set){
		s.delete(set);
	}
	
	public void addToActiveWorkingSet(RecordEditorInput input, IEclipseContext context){
		if (activeWorkingSet == null) return;
		IntelWorkingSet wset = null;
		boolean found = false;
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			wset = (IntelWorkingSet) s.get(IntelWorkingSet.class, activeWorkingSet);
			IntelRecord record = (IntelRecord) s.get(IntelRecord.class, input.getUuid());
			if (wset != null){
				for (IntelWorkingSetRecord r : wset.getRecords()){
					if (r.getRecord().equals(record)){
						found = true;
						break;
					}
				}
				
				if (!found){
					IntelWorkingSetRecord wsrecord = new IntelWorkingSetRecord();
					wsrecord.setRecord(record);
					wsrecord.setWorkingSet(wset);
					s.save(wsrecord);
					wset.getRecords().add(wsrecord);
				}
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog(MessageFormat.format("Could not add intelligence record {0} to active working set. {1}", input.getName(), ex.getMessage()), ex);
			return;
		}
		if (wset != null && !found) fireEvent(IntelEvents.WS_MODIFIED, wset, context);
	}
	
	public void addToActiveWorkingSet(IntelRecord record, IEclipseContext context){
		if (activeWorkingSet == null) return;
		IntelWorkingSet wset = null;
		boolean found = false;
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			wset = (IntelWorkingSet) s.get(IntelWorkingSet.class, activeWorkingSet);
			if (wset != null){
				for (IntelWorkingSetRecord r : wset.getRecords()){
					if (r.getRecord().equals(record)){
						found = true;
						break;
					}
				}
				
				if (!found){
					IntelWorkingSetRecord wsrecord = new IntelWorkingSetRecord();
					wsrecord.setRecord(record);
					wsrecord.setWorkingSet(wset);
					s.save(wsrecord);
					wset.getRecords().add(wsrecord);
				}
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog(MessageFormat.format("Could not add intelligence record {0} to active working set. {1}", record.getTitle(), ex.getMessage()), ex);
			return;
		}
		if (wset != null && !found) fireEvent(IntelEvents.WS_MODIFIED, wset, context);
	}
	
	public void addToActiveWorkingSet(IntelEntity entity, IEclipseContext context){
		if (activeWorkingSet == null) return;
		IntelWorkingSet wset = null;
		boolean found = false;
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			wset = (IntelWorkingSet) s.get(IntelWorkingSet.class, activeWorkingSet);
			if (wset != null){
				for (IntelWorkingSetEntity r : wset.getEntities()){
					if (r.getEntity().equals(entity)){
						found = true;
						break;
					}
				}
				
				if (!found){
					IntelWorkingSetEntity wsrecord = new IntelWorkingSetEntity();
					wsrecord.setEntity(entity);
					wsrecord.setWorkingSet(wset);
					s.save(wsrecord);
					wset.getEntities().add(wsrecord);
				}
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog(MessageFormat.format("Could not add intelligence entity {0} to active working set. {1}", entity.getIdAttributeAsText(), ex.getMessage()), ex);
			return;
		}
		if (wset != null && !found) fireEvent(IntelEvents.WS_MODIFIED, wset, context);
	}
	
	public void addToActiveWorkingSet(IntelRecordQuery query, IEclipseContext context){
		if (activeWorkingSet == null) return;
		IntelWorkingSet wset = null;
		Session s = HibernateManager.openSession();
		boolean found = false;
		try{
			s.beginTransaction();
			wset = (IntelWorkingSet) s.get(IntelWorkingSet.class, activeWorkingSet);
			if (wset != null){
				
				for (IntelWorkingSetQuery r : wset.getQueries()){
					if (r.getQuery().equals(query)){
						found = true;
						break;
					}
				}
				
				if (!found){
					IntelWorkingSetQuery wsrecord = new IntelWorkingSetQuery();
					wsrecord.setQuery(query);
					wsrecord.setWorkingSet(wset);
					s.save(wsrecord);
					wset.getQueries().add(wsrecord);
				}
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog(MessageFormat.format("Could not add intelligence query {0} to active working set. {1}", query.getName(), ex.getMessage()), ex);
			return;
		}
		if (wset != null && !found) fireEvent(IntelEvents.WS_MODIFIED, wset, context);
	}
	
	public void removeFromWorkingSet(IntelRecordQuery query, IEclipseContext context){
		if (activeWorkingSet == null) return;
		IntelWorkingSet wset = null;
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			wset = (IntelWorkingSet) s.get(IntelWorkingSet.class, activeWorkingSet);
			if (wset != null){
				IntelWorkingSetQuery wsrecord = null;
				for (IntelWorkingSetQuery r : wset.getQueries()){
					if (r.getQuery().equals(query)){
						wsrecord = r;
						break;
					}
				}
				if (wsrecord != null){
					wset.getQueries().remove(wsrecord);
				}
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog(MessageFormat.format("Could not remove intelligence query {0} from active working set. {1}", query.getName(), ex.getMessage()), ex);
			return;
		}
		if (wset != null) fireEvent(IntelEvents.WS_MODIFIED, wset, context);
	}
	
	public void removeFromWorkingSet(IntelEntity entity, IEclipseContext context){
		if (activeWorkingSet == null) return;
		IntelWorkingSet wset = null;
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			wset = (IntelWorkingSet) s.get(IntelWorkingSet.class, activeWorkingSet);
			if (wset != null){
				IntelWorkingSetEntity wsrecord = null;
				for (IntelWorkingSetEntity r : wset.getEntities()){
					if (r.getEntity().equals(entity)){
						wsrecord = r;
						break;
					}
				}
				if (wsrecord != null){					
					wset.getEntities().remove(wsrecord);
				}
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog(MessageFormat.format("Could not remove intelligence entity {0} from active working set. {1}", entity.getIdAttributeAsText(), ex.getMessage()), ex);
			return;
		}
		if (wset != null) fireEvent(IntelEvents.WS_MODIFIED, wset, context);
	}
	
	public void removeFromWorkingSet(IntelRecord record, IEclipseContext context){
		if (activeWorkingSet == null) return;
		IntelWorkingSet wset = null;
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			wset = (IntelWorkingSet) s.get(IntelWorkingSet.class, activeWorkingSet);
			if (wset != null){
				IntelWorkingSetRecord wsrecord = null;
				for (IntelWorkingSetRecord r : wset.getRecords()){
					if (r.getRecord().equals(record)){
						wsrecord = r;
						break;
					}
				}
				if (wsrecord != null){
					wset.getRecords().remove(wsrecord);
				}
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog(MessageFormat.format("Could not remove intelligence record {0} from active working set. {1}", record.getTitle(), ex.getMessage()), ex);
			return;
		}
		if (wset != null) fireEvent(IntelEvents.WS_MODIFIED, wset, context);
	}
	private void fireEvent(String eventTopic, IntelWorkingSet set, IEclipseContext context){
		context.get(IEventBroker.class).send(eventTopic, set);
	}
	
}
