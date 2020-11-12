/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.asset.query.engine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.collections.comparators.NullComparator;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StringType;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.model.AssetWaypointAttachment;
import org.wcs.smart.asset.query.AssetQueryPlugIn;
import org.wcs.smart.asset.query.model.AssetObservationResultItem;
import org.wcs.smart.asset.query.model.IAssetResultItem;
import org.wcs.smart.asset.query.model.observation.FixedQueryColumn;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.query.common.engine.IDesktopWOEngine;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.ObservationQueryResultItem;
import org.wcs.smart.query.common.engine.WaypointQueryResultItem;
import org.wcs.smart.query.common.model.IUpdateableResultSet;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.CategoryQueryColumn;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Tools for updating asset query result sets
 * 
 * @author Emily
 *
 */
public class UpdateableResultSet {
	
	private final static NullComparator NULL_COMPARATOR = new NullComparator(false);

	IDesktopWOEngine<? extends IResultItem> engine;
	
	public UpdateableResultSet(IDesktopWOEngine<? extends IResultItem> engine) {
		this.engine = engine;
		
	}
	
	public boolean update(QueryColumn column, IResultItem item, Object newValue) throws Exception{
		if (!(item instanceof IAssetResultItem)) return false;
		
		if (column instanceof FixedQueryColumn){
			return updateWaypointDetails((FixedQueryColumn)column, (WaypointQueryResultItem)item, newValue);
		}	
		if (column instanceof AttributeQueryColumn){
			return updateAttributeColumn((AttributeQueryColumn)column, (AssetObservationResultItem)item, newValue);
		}else if (column instanceof CategoryQueryColumn){
			return updateObservation((AssetObservationResultItem)item, newValue);
		}
		return false;
	}

	
	private boolean updateAttribute(WaypointObservation wo, String attributeKey, Object newValue, Session session){
		WaypointObservationAttribute toUpdate = null;
		for (WaypointObservationAttribute woa : wo.getAttributes()){
			if (woa.getAttribute().getKeyId().equals(attributeKey)){
				toUpdate = woa;
			}
		}
		if (toUpdate == null) return false;
		
		boolean updated = false;
		switch(toUpdate.getAttribute().getType()){
			case BOOLEAN:
				if (newValue instanceof Boolean){
					Boolean newBoolean = (Boolean)newValue;
					if ((toUpdate.getNumberValue() > 0.5 && !newBoolean) || (toUpdate.getNumberValue() < 0.5 && newBoolean)){
						if (newBoolean){
							toUpdate.setNumberValue(1.0);
						}else{
							toUpdate.setNumberValue(0.0);
						}
						updated = true;
					}
				}
				break;
			case DATE:
				if (newValue instanceof LocalDate){
					LocalDate newDate = (LocalDate)newValue;
					if (!newDate.isEqual(toUpdate.getDateValue())){
						toUpdate.setDateValue(newDate);
						updated = true;
					}
				}
				break;
			case LIST:
				if (newValue instanceof AttributeListItem){
					AttributeListItem newItem = (AttributeListItem)newValue;
					if (!newItem.equals(toUpdate.getAttributeListItem())){
						toUpdate.setAttributeListItem(newItem);
						updated = true;
						
						//add label to query results if necessary
						if (engine instanceof AssetObservationEngine){
							((AssetObservationEngine)engine).addListLabel(session, newItem);
						}
					}
				}
				break;
			case MLIST:
				if (newValue instanceof Collection<?>) {
					if (toUpdate.getAttributeListItems() == null) toUpdate.setAttributeListItems(new ArrayList<>());
					
					List<AttributeListItem> newItems = new ArrayList<>();
					
					for (Object l : ((Collection<?>)newValue)) {
						if (l instanceof AttributeListItem) {
							AttributeListItem li = (AttributeListItem) l;
							newItems.add(li);
							if (engine instanceof AssetObservationEngine){
								((AssetObservationEngine)engine).addListLabel(session, li);
							}
						}
					}
					
					for (Iterator<WaypointObservationAttributeList> iterator = toUpdate.getAttributeListItems().iterator(); iterator.hasNext();) {
						WaypointObservationAttributeList attributeListItem = (WaypointObservationAttributeList) iterator.next();
						if (!newItems.contains(attributeListItem.getAttributeListItem())) {
							iterator.remove();
							updated = true;
						}else {
							newItems.remove(attributeListItem.getAttributeListItem());
						}
					}
					for (AttributeListItem ni:newItems) {
						WaypointObservationAttributeList w = new WaypointObservationAttributeList();
						w.setAttributeLisItem(ni);
						w.setObservationAttribute(toUpdate);
						toUpdate.getAttributeListItems().add(w);
						updated = true;
					}
				}
				break;
			case NUMERIC:
				if (newValue == null && toUpdate.getNumberValue() == null) break;
				if (newValue == null) {
					toUpdate.setNumberValue(null);
					updated = true;
				}else if (newValue instanceof Double){
					Double newDouble = (Double)newValue;
					if (toUpdate.getNumberValue().doubleValue() != newDouble.doubleValue()){
						toUpdate.setNumberValue(newDouble);
						updated = true;
					}
				}
				break;
			case TEXT:
				if (newValue instanceof String){
					String newString = (String)newValue;
					if (!toUpdate.getStringValue().equals(newString)){
						toUpdate.setStringValue(newString);
						updated = true;
					}
				}
				break;
			case TREE:
				if (newValue instanceof AttributeTreeNode){
					AttributeTreeNode newItem = (AttributeTreeNode)newValue;
					if (!newItem.equals(toUpdate.getAttributeTreeNode())){
						toUpdate.setAttributeTreeNode(newItem);
						updated = true;
						
						//add label to query results if necessary
						if (engine instanceof AssetObservationEngine){
							((AssetObservationEngine)engine).addTreeLabel(session, newItem);
						}
					}
				}
				break;
		}
		return updated;
	}
	
	
	public boolean deleteObservation(IUpdateableResultSet results, UUID observationUuid) throws Exception{
		Waypoint wp = null;
		try(Session s = HibernateManager.openSession(new AttachmentInterceptor())){
			s.getTransaction().begin();
			try{		
				WaypointObservation wo = (WaypointObservation) s.get(WaypointObservation.class, observationUuid);
				if (wo == null) return false;
				wp = wo.getWaypoint();
				s.delete(wo);

				//delete empty observation groups
				wo.getObservationGroup().getObservations().remove(wo);
				if (wo.getObservationGroup().getObservations().isEmpty()) {
					s.delete(wo.getObservationGroup());
					wo.getObservationGroup().getWaypoint().getObservationGroups().remove(wo.getObservationGroup());
				}
				
				//update category names in query results table
				StringBuilder sql = new StringBuilder();
				sql.append("SELECT count(*) FROM " + engine.getQueryDataTable() + " WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
				NativeQuery<?> queryUpdate = s.createNativeQuery(sql.toString());
				queryUpdate.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
				Integer cnt = (Integer)queryUpdate.uniqueResult();
				if (cnt > 1){
					sql = new StringBuilder();
					sql.append(" DELETE FROM " +  engine.getQueryDataTable() + " WHERE ob_uuid = :uuid "); //$NON-NLS-1$ //$NON-NLS-2$
					queryUpdate = s.createNativeQuery(sql.toString());
					queryUpdate.setParameter("uuid", observationUuid); //$NON-NLS-1$
					queryUpdate.executeUpdate();
					
					((AssetObservationEngine) engine).updateResultCount(s, results);
				}else{
					sql = new StringBuilder();
					sql.append(" UPDATE " +  engine.getQueryDataTable() + " SET ob_uuid = null, wp_group_uuid = null, "); //$NON-NLS-1$ //$NON-NLS-2$
					for (int j = 0; j < ((AssetObservationEngine)engine).getCategoryCount(); j++) {
						sql.append("category_" + j + " = null, "); //$NON-NLS-1$ //$NON-NLS-2$
					}
					sql.deleteCharAt(sql.length() - 1);
					sql.deleteCharAt(sql.length() - 1);
					sql.append(" WHERE ob_uuid = :uuid "); //$NON-NLS-1$
					queryUpdate = s.createNativeQuery(sql.toString());
					queryUpdate.setParameter("uuid", observationUuid); //$NON-NLS-1$
					queryUpdate.executeUpdate();
				}
				s.flush();
				updateLastModified(wp, s);
				s.getTransaction().commit();
			}catch(Exception ex){
				s.getTransaction().rollback();
				throw ex;
			}
		}
		WaypointEventManager.getInstance().waypointModified(wp);
		getEventBroker().post(AssetEvents.ASSETDATA, null);
		return true;
	}
	
	public boolean updateObservation(ObservationQueryResultItem item, Object newValue) throws Exception{
		if (!(newValue instanceof WaypointObservation)) return false;
		WaypointObservation newOb = (WaypointObservation)newValue;
		
		if (newOb.getUuid() == null && item.getObservationUuid() != null) return false;	//cannot add a new feature to a row that already has an observation
		if (newOb.getUuid() == null && newOb.getCategory() == null) return false;//nothing to do
		
		try(Session s = HibernateManager.openSession()){
			try{
				s.getTransaction().begin();
				
				WaypointObservation wo;
				if (newOb.getUuid() == null){
					//new
					Waypoint wp = (Waypoint) s.get(Waypoint.class, item.getWaypointUuid());
					if (wp.getObservationGroups() == null)  wp.setObservationGroups(new ArrayList<>());
					if (wp.getObservationGroups().isEmpty()) {
						WaypointObservationGroup g = new WaypointObservationGroup();
						g.setObservations(new ArrayList<>());
						g.setWaypoint(wp);
						wp.getObservationGroups().add(g);
						s.save(g);
						item.setObservationGroupUuid(g.getUuid());
					}
					//add to first group
					WaypointObservationGroup first = wp.getObservationGroups().get(0);
					wo = newOb;
					wo.setObservationGroup(first);
					first.getObservations().add(wo);
					s.save(wo);
					
				}else{
					wo = (WaypointObservation) s.get(WaypointObservation.class, newOb.getUuid());
					if (wo == null) return false;
					if (wo.getAttributes() != null){
						for (WaypointObservationAttribute a : wo.getAttributes()){
							s.delete(a);
						}
						wo.getAttributes().clear();
						s.flush();
					}
					wo.setCategory(newOb.getCategory());
					if (wo.getAttributes() == null) wo.setAttributes(new ArrayList<>());
					for (WaypointObservationAttribute newA : newOb.getAttributes()){
						wo.getAttributes().add(newA);
						newA.setObservation(wo);
					}
				}
				
				//update category names in query results table
				List<String> names = new ArrayList<>();
				Category c = wo.getCategory();
				while(c != null){
					names.add(0, c.getName());
					c = c.getParent();
				}
				HashMap<String,Object> params = new HashMap<>();
				StringBuilder sql = new StringBuilder();
				sql.append("UPDATE " + engine.getQueryDataTable() + " SET "); //$NON-NLS-1$ //$NON-NLS-2$
				if (item.getObservationUuid()==null){
					sql.append("ob_uuid = :obuuid, "); //$NON-NLS-1$
					params.put("obuuid", wo.getUuid()); //$NON-NLS-1$
					
					sql.append("wp_group_uuid = :grpuuid, "); //$NON-NLS-1$
					params.put("grpuuid", wo.getObservationGroup().getUuid()); //$NON-NLS-1$
				}
				
				for (int j = 0; j < ((AssetObservationEngine)engine).getCategoryCount(); j++) {
					if (j > 0){
						sql.append(", "); //$NON-NLS-1$
					}
					sql.append("category_"); //$NON-NLS-1$
					sql.append(j);
					if (j < names.size()){
						sql.append("= :cat"); //$NON-NLS-1$
						sql.append(j);
						params.put("cat" + j, names.get(j)); //$NON-NLS-1$
					}else{
						sql.append(" = null"); //$NON-NLS-1$
					}
				}
				sql.append(" WHERE "); //$NON-NLS-1$
				if (item.getObservationUuid() == null){
					sql.append("wp_uuid = :uuid"); //$NON-NLS-1$
					params.put("uuid", wo.getWaypoint().getUuid()); //$NON-NLS-1$
				}else{
					sql.append("ob_uuid = :uuid"); //$NON-NLS-1$
					params.put("uuid", wo.getUuid()); //$NON-NLS-1$
				}
				
				NativeQuery<?> queryUpdate = s.createNativeQuery(sql.toString());
				for (Entry<String,Object> e : params.entrySet()){
					queryUpdate.setParameter(e.getKey(), e.getValue());
				}
				queryUpdate.executeUpdate();
				
				//add label to query results if necessary
				if (engine instanceof AssetObservationEngine){
					for (WaypointObservationAttribute a : wo.getAttributes()){
						if (a.getAttribute().getType() == AttributeType.LIST){
							if (a.getAttributeListItem() != null){
								((AssetObservationEngine)engine).addListLabel(s, a.getAttributeListItem());
							}
						}else if (a.getAttribute().getType() == AttributeType.TREE){
							((AssetObservationEngine)engine).addTreeLabel(s, a.getAttributeTreeNode());
						}
					}
				}
				s.flush();
				updateLastModified(wo.getWaypoint(), s);
				s.getTransaction().commit();
			}catch (Exception ex){
				s.getTransaction().rollback();
				throw ex;
			}
		}
		return true;
	}
	
	private boolean updateAttributeColumn(AttributeQueryColumn column, ObservationQueryResultItem item, Object value) throws Exception{
		boolean change = false;
		WaypointObservation wpo = null;
		if (item.getObservationUuid() == null) return false;
		try(Session s = HibernateManager.openSession()){
			s.getTransaction().begin();
			try {
				wpo = (WaypointObservation) s.get(WaypointObservation.class, item.getObservationUuid());
				if (wpo != null) {
					change = updateAttribute(wpo, column.getAttributeId(), value, s);
					s.flush();
					updateLastModified(wpo.getWaypoint(), s);
				}
				s.getTransaction().commit();
			} catch (Exception ex) {
				s.getTransaction().rollback();
				throw ex;
			}
		}

		if (change) {
			WaypointEventManager.getInstance().waypointModified(wpo.getWaypoint());
			getEventBroker().post(AssetEvents.ASSETDATA, null);
			return true;
		}
		return false;
	}
	
	private boolean updateWaypointDetails(FixedQueryColumn column, WaypointQueryResultItem item, Object value) throws Exception{
		Waypoint wp = null;
		boolean change = false;
		try(Session s = HibernateManager.openSession()){
			try {
				s.getTransaction().begin();
				wp = (Waypoint) s.get(Waypoint.class, item.getWaypointUuid());
				if (wp != null) {
					switch (column.getColumn()) {
						case WAYPOINT_COMMENT:
							if (value instanceof String) {
								if (((String) value).length() == 0) value = null;
								if (NULL_COMPARATOR.compare(value, wp.getComment()) != 0) {
									change = true;
									updateWaypointComment(wp,(String) value, s);
								}
							}
							break;
						case WAYPOINT_TIME:
							if (value instanceof LocalTime) {
								LocalTime newDate = (LocalTime) value;
								if (!newDate.equals(wp.getDateTime().toLocalTime())){
									change = true;
									updateWaypointTime(wp, newDate, s);
								}
							}
							break;

						case WAYPOINT_ID:
							if (value instanceof String) {
								if (!value.equals(wp.getId())) {
									change = true;
									updateWaypointId(wp, (String) value, s);
								}
							}
							break;
						case WAYPOINT_X:
							if (value instanceof Double) {
								if (!value.equals(wp.getX())) {
									change = true;
									updateWaypointPosition(wp, (Double) value, wp.getY(), s);
								}
							}
							break;
						case WAYPOINT_Y:
							if (value instanceof Double) {
								if (!value.equals(wp.getX())) {
									change = true;
									updateWaypointPosition(wp, wp.getX(), (Double) value, s);
								}
							}
							break;
						default:
							break;
					}
					
				}
				
				s.flush();
				if (change) {
					updateLastModified(wp, s);
				}
				s.getTransaction().commit();
			} catch (Exception ex) {
				s.getTransaction().rollback();
				throw ex;
			}
		}

		if (change) {
			WaypointEventManager.getInstance().waypointModified(wp);
			getEventBroker().post(AssetEvents.ASSETDATA, null);
			return true;
		}
		return false;
	}
	
	protected void updateLastModified(Waypoint wp, Session s) {
		NativeQuery<?> q = s.createNativeQuery("update " + engine.getQueryDataTable() + " SET wp_lastmodified = :lastmodified, wp_lastmodifiedbyname = :lastmodifiedby WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("lastmodified", wp.getLastModified()); //$NON-NLS-1$
		q.setParameter("lastmodifiedby", SmartLabelProvider.getShortLabel(SmartDB.getCurrentEmployee())); //$NON-NLS-1$
		q.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	private void updateWaypointPosition(Waypoint wp, double newX, double newY, Session session){
		wp.setRawX(newX);
		wp.setRawY(newY);
		
		NativeQuery<?> q = session.createNativeQuery("update " + engine.getQueryDataTable() + " SET wp_x = :x, wp_y = :y WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("x", newX); //$NON-NLS-1$
		q.setParameter("y", newY); //$NON-NLS-1$
		q.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
		q.executeUpdate();
		
	}
	private void updateWaypointId(Waypoint wp, String newId, Session session){
		wp.setId(newId);
		
		NativeQuery<?> q = session.createNativeQuery("update " + engine.getQueryDataTable() + " SET wp_id = :id WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("id", newId); //$NON-NLS-1$
		q.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	private void updateWaypointTime(Waypoint wp, LocalTime newTime, Session session){
		
		LocalDateTime ldt = LocalDateTime.of(wp.getDateTime().toLocalDate(), newTime);
		wp.setDateTime(ldt);
		
		NativeQuery<?> q = session.createNativeQuery("update " + engine.getQueryDataTable() + " SET wp_date = :id WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("id", wp.getDateTime()); //$NON-NLS-1$
		q.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	private void updateWaypointComment(Waypoint wp, String newComment, Session session){
		wp.setComment(newComment);
		
		NativeQuery<?> q = session.createNativeQuery("update " + engine.getQueryDataTable() + " SET wp_comment = :cmt WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("cmt", newComment,  StringType.INSTANCE); //$NON-NLS-1$
		q.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
		
		q.executeUpdate();
	}
	
	protected IEventBroker getEventBroker() {
		return EclipseContextFactory.getServiceContext(AssetQueryPlugIn.getDefault().getBundle().getBundleContext()).get(IEventBroker.class);
	}
	
	
	public boolean deleteWaypoint(IUpdateableResultSet results, UUID waypointUuid) throws Exception{
		Waypoint wp = null;
		try(Session s = HibernateManager.openSession(new AttachmentInterceptor())){
			try{
				s.getTransaction().begin();
				
				Waypoint wo = (Waypoint) s.get(Waypoint.class, waypointUuid);
				if (wo == null) return false;
				wp = wo;
				
				//delete asset waypoints 
				List<AssetWaypoint> assetWaypoints =  QueryFactory.buildQuery(s, AssetWaypoint.class, new Object[] {"waypoint", wo}).list(); //$NON-NLS-1$
				for (AssetWaypoint aw : assetWaypoints) {
					for (AssetWaypointAttachment attlink : aw.getAttachments()) {
						s.delete(attlink);
					}
					s.delete(aw);
				}
				//delete waypoint
				s.delete(wo);
				s.flush();
				
				//update category names in query results table
				StringBuilder sql = new StringBuilder();
				sql.append(" DELETE FROM " + engine.getQueryDataTable() + " WHERE wp_uuid = :uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				NativeQuery<?> queryUpdate = s.createNativeQuery(sql.toString());
				queryUpdate.setParameter("uuid", waypointUuid); //$NON-NLS-1$
				queryUpdate.executeUpdate();
					
				if (engine instanceof IDerbyWaypointEngine) {
					((IDerbyWaypointEngine) engine).updateResultCount(s, results);
				}
				
				s.getTransaction().commit();
				
			}catch(Exception ex){
				s.getTransaction().rollback();
				throw ex;
			}
		}
		
		WaypointEventManager.getInstance().waypointModified(wp);
		getEventBroker().post(AssetEvents.ASSETDATA, null);
		return true;
	}
	
	public boolean updateWaypointPosition(WaypointQueryResultItem item, Double x, Double y) throws Exception{
		Waypoint wp = null;
		boolean change = false;
		try(Session s = HibernateManager.openSession()){
			try {
				s.getTransaction().begin();
				wp = (Waypoint) s.get(Waypoint.class, item.getWaypointUuid());
				if (wp != null) {
					updateWaypointPosition(wp, x, y, s);
					change = true;
				}
				s.getTransaction().commit();
			} catch (Exception ex) {
				s.getTransaction().rollback();
				throw ex;
			}
		}

		if (change) {
			WaypointEventManager.getInstance().waypointModified(wp);
			getEventBroker().post(AssetEvents.ASSETDATA, null);
			return true;
		}
		return false;
	}

	
}
