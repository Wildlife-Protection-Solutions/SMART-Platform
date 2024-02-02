package org.wcs.smart.patrol.query.engine;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.collections.comparators.NullComparator;
import org.hibernate.Session;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.NativeQuery;
import org.locationtech.jts.geom.Geometry;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.GeometryAttributeValue;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.query.model.IPatrolQueryResultItem;
import org.wcs.smart.patrol.query.model.PatrolObservationResultItem;
import org.wcs.smart.patrol.query.model.PatrolWaypointResultItem;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.query.common.engine.IDesktopWOEngine;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.WaypointQueryResultItem;
import org.wcs.smart.query.common.model.IUpdateableResultSet;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.CategoryQueryColumn;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.ui.SmartLabelProvider;

public class UpdateableResultSet implements IWaypointUpdateableResultSet{
	
	private final static NullComparator NULL_COMPARATOR = new NullComparator(false);

	IDesktopWOEngine<? extends IResultItem> engine;

	protected IUpdateableResultSet wrapper;
	
	public UpdateableResultSet(IDesktopWOEngine<? extends IResultItem> engine, IUpdateableResultSet wrapper) {
		this.engine = engine;
		this.wrapper = wrapper;
		
	}
	@Override
	public boolean update(QueryColumn column, IResultItem item, Object newValue) throws Exception{
		if (!(item instanceof IPatrolQueryResultItem)) return false;
		if (column instanceof FixedQueryColumn){
			return updateWaypointDetails((FixedQueryColumn)column, (IPatrolQueryResultItem)item, newValue);
		}
		if (column instanceof AttributeQueryColumn){
			return updateAttributeColumn((AttributeQueryColumn)column, (PatrolObservationResultItem)item, newValue);
		}else if (column instanceof CategoryQueryColumn){
			return updateObservation((PatrolObservationResultItem)item, newValue);
		}
		return false;
	}

	private boolean updateWaypointDetails(FixedQueryColumn column, IPatrolQueryResultItem item, Object value) throws Exception{
		Waypoint wp = null;
		Patrol p = null;
		boolean change = false;
		try(Session s = HibernateManager.openSession()){
			try {
				s.getTransaction().begin();
				wp = (Waypoint) s.get(Waypoint.class, ((WaypointQueryResultItem)item).getWaypointUuid());
				if (wp != null) {
					PatrolWaypoint pw = PatrolHibernateManager.getPatrolWaypoint(s, wp);
					if (pw != null) {
						p = pw.getPatrolLegDay().getPatrolLeg().getPatrol();
						p.equals(p); //necessary for using outside session
						
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
								if (!newDate.equals(wp.getDateTime().toLocalTime())) {
									change = true;
									updateWaypointTime(wp, newDate, s);
								}
							}
							break;
						case WAYPOINT_DIRECTION:
							if (value == null && wp.getDirection() == null) break;
							if (value != null && wp.getDirection() != null && ((Double)value).floatValue() == wp.getDirection()) break;
							change = true;
							updateWaypointDirection(wp, value == null ? null : ((Double)value).floatValue(), s);
							break;
						case WAYPOINT_DISTANCE:
							if (value == null && wp.getDistance() == null) break;
							if (value != null && wp.getDistance() != null && ((Double)value).floatValue() == wp.getDistance()) break;
							change = true;
							updateWaypointDistance(wp, value == null ? null : ((Double)value).floatValue(), s);
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
								if (!value.equals(wp.getRawX())) {
									change = true;
									updateWaypointPosition(wp, (Double) value, wp.getRawY(), wp.getDistance(), wp.getDirection(), s);
								}
							}
							break;
						case WAYPOINT_Y:
							if (value instanceof Double) {
								if (!value.equals(wp.getRawY())) {
									change = true;
									updateWaypointPosition(wp, wp.getRawX(), (Double) value, wp.getDistance(), wp.getDirection(), s);
								}
							}
							break;
						case WAYPOINT_RAWX:
							if (value instanceof Double) {
								if (!value.equals(wp.getRawX())) {
									change = true;
									updateWaypointPosition(wp, (Double) value, wp.getRawY(), wp.getDistance(), wp.getDirection(), s);
								}
							}
							break;
						case WAYPOINT_RAWY:
							if (value instanceof Double) {
								if (!value.equals(wp.getRawY())) {
									change = true;
									updateWaypointPosition(wp, wp.getRawX(), (Double) value, wp.getDistance(), wp.getDirection(), s);
								}
							}
							break;
						default:
							break;
						}
					}
				}
				
				if (change) {
					s.flush();
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
			if (p != null){
				PatrolEventManager.getInstance().patrolSaved(p, true);
			}
			return true;
		}
		return false;
	}
	
	protected void updateLastModified(Waypoint wp, Session s) {
		s.createNativeMutationQuery("update " + engine.getQueryDataTable() + " SET wp_lastmodified = :lastmodified, wp_lastmodifiedbyname = :lastmodifiedby WHERE wp_uuid = :uuid") //$NON-NLS-1$ //$NON-NLS-2$
			.setParameter("lastmodified", wp.getLastModified()) //$NON-NLS-1$
			.setParameter("lastmodifiedby", SmartLabelProvider.getShortLabel(SmartDB.getCurrentEmployee())) //$NON-NLS-1$
			.setParameter("uuid", wp.getUuid()) //$NON-NLS-1$
			.executeUpdate();
	}
	
	private void updateWaypointPosition(Waypoint wp, double newX, double newY, Float distance, Float direction, Session session){
		wp.setRawX(newX);
		wp.setRawY(newY);
		wp.setDirection(direction);
		wp.setDistance(distance);
		
		session.createNativeMutationQuery("update " + engine.getQueryDataTable() + " SET wp_x = :x, wp_y = :y, wp_distance = :distance, wp_direction = :direction WHERE wp_uuid = :uuid") //$NON-NLS-1$ //$NON-NLS-2$
			.setParameter("x", newX) //$NON-NLS-1$
			.setParameter("y", newY) //$NON-NLS-1$
			.setParameter("distance", distance) //$NON-NLS-1$
			.setParameter("direction", direction) //$NON-NLS-1$
			.setParameter("uuid", wp.getUuid()) //$NON-NLS-1$
			.executeUpdate();
		
	}
	private void updateWaypointDistance(Waypoint wp, Float newDistance, Session session){
		wp.setDistance(newDistance);
		
		session.createNativeMutationQuery("update " + engine.getQueryDataTable() + " SET wp_distance = :id WHERE wp_uuid = :uuid") //$NON-NLS-1$ //$NON-NLS-2$
			.setParameter("id", newDistance) //$NON-NLS-1$
			.setParameter("uuid", wp.getUuid()) //$NON-NLS-1$
			.executeUpdate();
	}
	
	private void updateWaypointDirection(Waypoint wp, Float newDirection, Session session){
		wp.setDirection(newDirection);
		
		session.createNativeMutationQuery("update " + engine.getQueryDataTable() + " SET wp_direction = :id WHERE wp_uuid = :uuid") //$NON-NLS-1$ //$NON-NLS-2$
			.setParameter("id", newDirection) //$NON-NLS-1$
			.setParameter("uuid", wp.getUuid()) //$NON-NLS-1$
			.executeUpdate();	
	}
	
	private void updateWaypointId(Waypoint wp, String newId, Session session){
		wp.setId(newId);
		
		session.createNativeMutationQuery("update " + engine.getQueryDataTable() + " SET wp_id = :id WHERE wp_uuid = :uuid") //$NON-NLS-1$ //$NON-NLS-2$
			.setParameter("id", newId) //$NON-NLS-1$
			.setParameter("uuid", wp.getUuid()) //$NON-NLS-1$
			.executeUpdate();
	}
	
	private void updateWaypointTime(Waypoint wp, LocalTime newTime, Session session){
		wp.setDateTime( wp.getDateTime().toLocalDate().atTime(newTime) );
		
		session.createNativeMutationQuery("update " + engine.getQueryDataTable() + " SET wp_time = :id WHERE wp_uuid = :uuid") //$NON-NLS-1$ //$NON-NLS-2$
			.setParameter("id", wp.getDateTime()) //$NON-NLS-1$
			.setParameter("uuid", wp.getUuid()) //$NON-NLS-1$
			.executeUpdate();
	}
	
	private void updateWaypointComment(Waypoint wp, String newComment, Session session){
		wp.setComment(newComment);
		
		session.createNativeMutationQuery("update " + engine.getQueryDataTable() + " SET wp_comment = :cmt WHERE wp_uuid = :uuid") //$NON-NLS-1$ //$NON-NLS-2$
			.setParameter("cmt", newComment) //$NON-NLS-1$
			.setParameter("uuid", wp.getUuid()) //$NON-NLS-1$
			.executeUpdate();
	}
	
	@Override
	public boolean deleteWaypoint(UUID waypointUuid) throws Exception{
		Waypoint wp = null;
		Patrol p = null;
		try(Session s = HibernateManager.openSession(new AttachmentInterceptor())){
			
			try{
				s.getTransaction().begin();
				
				Waypoint wo = (Waypoint) s.get(Waypoint.class, waypointUuid);
				if (wo == null) return false;
				wp = wo;
				
				//find patrol updated for events
				PatrolWaypoint pw = PatrolHibernateManager.getPatrolWaypoint(s, wp);
				if (pw == null) throw new Exception("No patrol link found for waypoint.  Waypoint will not be deleted."); //$NON-NLS-1$
				s.remove(pw);
				s.remove(wp);
				
				p = pw.getPatrolLegDay().getPatrolLeg().getPatrol();
				p.equals(p);	//required to prevent patrol equals from failing in event manager
				
				s.flush();
				
				//update category names in query results table
				StringBuilder sql = new StringBuilder();
				sql.append(" DELETE FROM " + engine.getQueryDataTable() + " WHERE wp_uuid = :uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				s.createNativeMutationQuery(sql.toString())
					.setParameter("uuid", waypointUuid) //$NON-NLS-1$
					.executeUpdate();
					
				if (engine instanceof IDerbyWaypointEngine) {
					((IDerbyWaypointEngine) engine).updateResultCount(s, wrapper);
				}
				
				s.getTransaction().commit();
				
			}catch(Exception ex){
				s.getTransaction().rollback();
				throw ex;
			}
		}
		
		WaypointEventManager.getInstance().waypointModified(wp);
		if (p != null){
			PatrolEventManager.getInstance().patrolSaved(p, true);
		}
		return true;
	}
	
	
	@Override
	public boolean updateWaypointPosition(IPatrolQueryResultItem item, Double x, Double y, Float distance, Float direction) throws Exception{
		Waypoint wp = null;
		Patrol p = null;
		boolean change = false;
		
		try(Session s = HibernateManager.openSession()){
			try {
				s.getTransaction().begin();
				wp = (Waypoint) s.get(Waypoint.class, ((WaypointQueryResultItem)item).getWaypointUuid());
				if (wp != null) {
					PatrolWaypoint pw = PatrolHibernateManager.getPatrolWaypoint(s, wp);
					if (pw != null) {
						p = pw.getPatrolLegDay().getPatrolLeg().getPatrol();
						p.equals(p); //necessary for using outside session
						updateWaypointPosition(wp, x, y, distance, direction, s);
						change = true;
					}
				}
				s.getTransaction().commit();
			} catch (Exception ex) {
				s.getTransaction().rollback();
				throw ex;
			}
		}

		if (change) {
			WaypointEventManager.getInstance().waypointModified(wp);
			if (p != null){
				PatrolEventManager.getInstance().patrolSaved(p, true);
			}
			return true;
		}
		return false;
	}
	
	@Override
	public boolean canUpdate(Class<? extends IResultItem> item) {
		return (item.equals(PatrolObservationResultItem.class)) ||
				(item.equals(PatrolWaypointResultItem.class));
	}
	
	
	public boolean updateObservation(PatrolObservationResultItem item, Object newValue) throws Exception{
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
						s.persist(g);
						item.setObservationGroupUuid(g.getUuid());
					}
					//add to first group
					WaypointObservationGroup first = wp.getObservationGroups().get(0);
					wo = newOb;
					wo.setObservationGroup(first);
					first.getObservations().add(wo);
					
					s.persist(wo);
					
				}else{
					wo = (WaypointObservation) s.get(WaypointObservation.class, newOb.getUuid());
					if (wo == null) return false;
					if (wo.getAttributes() != null){
						for (WaypointObservationAttribute a : wo.getAttributes()){
							s.remove(a);
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
				if (item.getObservationUuid() == null){
					sql.append("ob_uuid = :obuuid, "); //$NON-NLS-1$
					params.put("obuuid", wo.getUuid()); //$NON-NLS-1$
					
					sql.append("wp_group_uuid = :grpuuid, "); //$NON-NLS-1$
					params.put("grpuuid", wo.getObservationGroup().getUuid()); //$NON-NLS-1$
				}
				
				
				for (int j = 0; j < ((DerbyObservationEngine)engine).getCategoryCount(); j++) {
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
				
				MutationQuery queryUpdate = s.createNativeMutationQuery(sql.toString());
				for (Entry<String,Object> e : params.entrySet()){
					queryUpdate.setParameter(e.getKey(), e.getValue());
				}
				queryUpdate.executeUpdate();
				
				//add label to query results if necessary
				if (engine instanceof DerbyObservationEngine){
					for (WaypointObservationAttribute a : wo.getAttributes()){
						if (a.getAttribute().getType() == AttributeType.LIST){
							if (a.getAttributeListItem() != null){
								((DerbyObservationEngine)engine).addListLabel(s, a.getAttributeListItem());
							}
						}else if (a.getAttribute().getType() == AttributeType.TREE){
							((DerbyObservationEngine)engine).addTreeLabel(s, a.getAttributeTreeNode());
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
	
	public boolean deleteObservation(UUID observationUuid) throws Exception {
		Waypoint wp = null;
		Patrol p = null;

		try (Session s = HibernateManager.openSession(new AttachmentInterceptor())) {

			s.getTransaction().begin();
			try {
				WaypointObservation wo = (WaypointObservation) s.get(WaypointObservation.class, observationUuid);
				if (wo == null)
					return false;
				wp = wo.getWaypoint();
				s.remove(wo);

				// delete empty observation groups
				wo.getObservationGroup().getObservations().remove(wo);
				if (wo.getObservationGroup().getObservations().isEmpty()) {
					s.remove(wo.getObservationGroup());
					wo.getObservationGroup().getWaypoint().getObservationGroups().remove(wo.getObservationGroup());
				}

				// find patrol updated for events
				PatrolWaypoint pw = PatrolHibernateManager.getPatrolWaypoint(s, wp);
				if (pw != null) {
					p = pw.getPatrolLegDay().getPatrolLeg().getPatrol();
					p.equals(p); // required to prevent patrol equals from failing in event manager
				}

				// update category names in query results table
				StringBuilder sql = new StringBuilder();
				sql.append("SELECT count(*) FROM " + engine.getQueryDataTable() + " WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
				NativeQuery<Integer> queryUpdate = s.createNativeQuery(sql.toString(), Integer.class);
				queryUpdate.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
				Integer cnt = queryUpdate.uniqueResult();
				if (cnt > 1) {
					sql = new StringBuilder();
					sql.append(" DELETE FROM " + engine.getQueryDataTable() + " WHERE ob_uuid = :uuid "); //$NON-NLS-1$ //$NON-NLS-2$
					s.createNativeMutationQuery(sql.toString())
						.setParameter("uuid", observationUuid) //$NON-NLS-1$
						.executeUpdate();

					((DerbyObservationEngine) engine).updateResultCount(s, wrapper);
				} else {
					sql = new StringBuilder();
					sql.append(" UPDATE " + engine.getQueryDataTable() + " SET ob_uuid = null, wp_group_uuid = null, "); //$NON-NLS-1$ //$NON-NLS-2$
					for (int j = 0; j < ((DerbyObservationEngine) engine).getCategoryCount(); j++) {
						sql.append("category_" + j + " = null, "); //$NON-NLS-1$ //$NON-NLS-2$
					}
					sql.deleteCharAt(sql.length() - 1);
					sql.deleteCharAt(sql.length() - 1);
					sql.append(" WHERE ob_uuid = :uuid "); //$NON-NLS-1$
					s.createNativeMutationQuery(sql.toString())
						.setParameter("uuid", observationUuid) //$NON-NLS-1$
						.executeUpdate();
				}

				s.flush();
				updateLastModified(wp, s);
				s.getTransaction().commit();

			} catch (Exception ex) {
				s.getTransaction().rollback();
				throw ex;
			}
		}

		WaypointEventManager.getInstance().waypointModified(wp);
		if (p != null) {
			PatrolEventManager.getInstance().patrolSaved(p, true);
		}
		return true;
	}
	private boolean updateAttributeColumn(AttributeQueryColumn column, PatrolObservationResultItem item, Object value) throws Exception{
		Patrol p = null;
		WaypointObservation wpo = null;
		boolean change = false;
		if (item.getObservationUuid() == null) return false;
		
		try(Session s = HibernateManager.openSession()){
			s.getTransaction().begin();
			try {
				wpo = (WaypointObservation) s.get(WaypointObservation.class, item.getObservationUuid());
				if (wpo != null) {
					//find patrol updated for events
					PatrolWaypoint pw = PatrolHibernateManager.getPatrolWaypoint(s, wpo.getWaypoint());					
					if (pw != null) {
						p = pw.getPatrolLegDay().getPatrolLeg().getPatrol();
						p.equals(p);	//required to prevent patrol equals from failing in event manager
						//update attribute value
						change = updateAttribute(wpo, column.getAttributeId(), value, s);
					}
					
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
			if (p != null){
				PatrolEventManager.getInstance().patrolSaved(p, true);
			}
			return true;
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
						if (engine instanceof DerbyObservationEngine){
							((DerbyObservationEngine)engine).addListLabel(session, newItem);
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
							if (engine instanceof DerbyObservationEngine){
								((DerbyObservationEngine)engine).addListLabel(session, li);
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
					if (toUpdate.getNumberValue() == null || toUpdate.getNumberValue().doubleValue() != newDouble){
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
						if (engine instanceof DerbyObservationEngine){
							((DerbyObservationEngine)engine).addTreeLabel(session, newItem);
						}
					}
				}
				break;
			case LINE:
			case POLYGON:
				if (newValue instanceof Geometry geom) {
					//setting source to null will ensure the source is not changed
					GeometryAttributeValue geomValue = new GeometryAttributeValue(geom, null);
					toUpdate.setGeometry(geomValue);
					updated = true;
				}
				break;
		}
		return updated;
	}
}
