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
package org.wcs.smart.i2;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.i2.model.IntelWorkingSetEntity;
import org.wcs.smart.i2.model.IntelWorkingSetQuery;
import org.wcs.smart.i2.model.IntelWorkingSetRecord;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;

/**
 * Working set manager.
 * 
 * @author Emily
 *
 */
public enum WorkingSetManager {

	INSTANCE;
	
	private UUID activeWorkingSet = null;
	
	public boolean isSet(){
		return activeWorkingSet != null;
	}
	
	public UUID getActiveWorkingSet(){
		return this.activeWorkingSet;
	}
	
	/**
	 * Sets the active working set 
	 * @param active
	 * @param context
	 * @throws Exception
	 */
	public void setActiveWorkingSet(IntelWorkingSet active, IEclipseContext context) throws Exception{
		if(active == null){
			this.activeWorkingSet = null;
		}else{
			if (!active.getConservationArea().equals(SmartDB.getCurrentConservationArea())){
				throw new Exception("Working set not valid for current Conservation Area");
			}
			this.activeWorkingSet = active.getUuid();
		}
		fireEvent(IntelEvents.ACTIVE_WS_SET, active, context);
	}
	
	/**
	 * Deletes the provided working set
	 * @param s
	 * @param set
	 */
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
					wsrecord.setIsVisible(true);
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
		}finally{
			s.close();
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
					wsrecord.setIsVisible(true);
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
		}finally{
			s.close();
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
					wsrecord.setIsVisible(true);
					s.save(wsrecord);
					wset.getEntities().add(wsrecord);
				}
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog(MessageFormat.format("Could not add intelligence entity {0} to active working set. {1}", entity.getIdAttributeAsText(Locale.getDefault()), ex.getMessage()), ex);
			return;
		}finally{
			s.close();
		}
		if (wset != null && !found) fireEvent(IntelEvents.WS_MODIFIED, wset, context);
	}
	
	public void addQueryToActiveWorkingSet(UUID queryUuid, IEclipseContext context){
		if (activeWorkingSet == null) return;
		IntelWorkingSet wset = null;
		Session s = HibernateManager.openSession();
		boolean found = false;
		String queryName = queryUuid.toString();
		try{
			s.beginTransaction();
			
			IntelRecordObservationQuery query = (IntelRecordObservationQuery) s.get(IntelRecordObservationQuery.class, queryUuid);
			if (query == null) throw new Exception("Query not found.");
			
			queryName = query.getName();
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
					wsrecord.setIsVisible(true);
					s.save(wsrecord);
					wset.getQueries().add(wsrecord);
				}
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog(MessageFormat.format("Could not add intelligence query {0} to active working set. {1}", queryName, ex.getMessage()), ex);
			return;
		}finally{
			s.close();
		}
		if (wset != null && !found) fireEvent(IntelEvents.WS_MODIFIED, wset, context);
	}
	
	public void addToActiveWorkingSet(IntelRecordObservationQuery query, IEclipseContext context){
		addQueryToActiveWorkingSet(query.getUuid(), context);
	}
	
	public void removeFromWorkingSet(IntelRecordObservationQuery query, IEclipseContext context){
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
		}finally{
			s.close();
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
		}finally{
			s.close();
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
		}finally{
			s.close();
		}
		if (wset != null) fireEvent(IntelEvents.WS_MODIFIED, wset, context);
	}
	
	private void fireEvent(String eventTopic, IntelWorkingSet set, IEclipseContext context){
		context.get(IEventBroker.class).send(eventTopic, set);
	}
	
	/**
	 * Clones a working set (does not clone the names)
	 * 
	 * @param toCopy
	 * @return
	 */
	public IntelWorkingSet clone(IntelWorkingSet toCopy){
		IntelWorkingSet clone = new IntelWorkingSet();
		
		clone.setConservationArea(SmartDB.getCurrentConservationArea());
		clone.setEntities(new ArrayList<IntelWorkingSetEntity>());
		clone.setQueries(new ArrayList<IntelWorkingSetQuery>());
		clone.setRecords(new ArrayList<IntelWorkingSetRecord>());
		
		if (toCopy.getEntities() != null){
			toCopy.getEntities().forEach((e)->{
				IntelWorkingSetEntity ws = new IntelWorkingSetEntity();
				ws.setEntity(e.getEntity());
				ws.setWorkingSet(clone);
				ws.setMapStyle(e.getMapStyle());
				ws.setIsVisible(e.getIsVisible());
				clone.getEntities().add(ws);
			});
		}
		if (toCopy.getRecords() != null){
			toCopy.getRecords().forEach((e)->{
				IntelWorkingSetRecord ws = new IntelWorkingSetRecord();
				ws.setRecord(e.getRecord());
				ws.setWorkingSet(clone);
				ws.setMapStyle(e.getMapStyle());
				ws.setIsVisible(e.getIsVisible());
				clone.getRecords().add(ws);
			});
		}
		if (toCopy.getQueries() != null){
			toCopy.getQueries().forEach((e)->{
				IntelWorkingSetQuery ws = new IntelWorkingSetQuery();
				ws.setQuery(e.getQuery());
				ws.setWorkingSet(clone);
				ws.setMapStyle(e.getMapStyle());
				ws.setIsVisible(e.getIsVisible());
				clone.getQueries().add(ws);
			});
		}
		return clone;
	}
	
}
