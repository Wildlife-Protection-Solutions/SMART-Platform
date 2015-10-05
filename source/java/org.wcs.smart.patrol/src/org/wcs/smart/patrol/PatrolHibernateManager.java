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
package org.wcs.smart.patrol;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.dataentry.model.ScreenOption;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.meta.PatrolScreenOptionMeta;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.patrol.model.Team;


/**
 * Extension of the smart hibernate manager for patrol related data.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolHibernateManager extends HibernateManager{
	
	private static NumberFormat PATROL_ID_FORMATTER = new DecimalFormat("000000"); //$NON-NLS-1$
	
	/**
	 * Gets all teams (active and in-active) for a given conservation area
	 * 
	 * @param ca conservation area 
	 * @param s active session 
	 * @return list of active and inactive teams
	 */
	public static List<Team> getTeams(ConservationArea ca, Session s){
		return getTeams(ca, s, false);
	}
	
	/**
	 * Gets active teams for a given conservation area
	 * 
	 * @param ca conservation area
	 * @param s active session
	 * @return list of active teams
	 */
	public static List<Team> getActiveTeams(ConservationArea ca, Session s){
		return getTeams(ca, s, true);
	}
	
	/**
	 * Loads teams from database 
	 * 
	 * @param ca conservation area
	 * @param s session 
	 * @param onlyActive <code>true</code> if only active status should be loaded; <code>false</code> returns all stations 
	 * @return list of stations
	 */
	@SuppressWarnings("unchecked")
	private static List<Team> getTeams(ConservationArea ca, Session s, boolean onlyActive){
		List<Team> list = null;
		Criteria query = s.createCriteria(Team.class).add(Restrictions.eq("conservationArea", ca)); //$NON-NLS-1$
		if (onlyActive){
			query.add(Restrictions.eq("isActive", true)); //$NON-NLS-1$
		}
		list = query.list();
		return list;
	}
	
	
	/**
	 * Gets all patrol mandates (active and in-active) for
	 * a given conservation area.
	 * 
	 * @param ca conservation area 
	 * @param s active session 
	 * @return list of mandates
	 */
	public static List<PatrolMandate> getMandates(ConservationArea ca, Session s){
		return getMandates(ca, s, false);
	}
	/**
	 * Gets only active patrol mandates for a given conservation area 
	 * 
	 * @param ca conservation area 
	 * @param s active session
	 * @return list of active mandates
	 */
	public static List<PatrolMandate> getActiveMandates(ConservationArea ca, Session s){
		return getMandates(ca, s, true);
	}
	
	/**
	 * Loads mandates from database 
	 * @param ca conservation area 
	 * @param s active session
	 * @param onlyActive <code>true</code> if only active mandates should be retured, <code>false</code> for all 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static List<PatrolMandate> getMandates(ConservationArea ca, Session s, boolean onlyActive){
		List<PatrolMandate> list = null;
		Criteria query = s.createCriteria(PatrolMandate.class).add(Restrictions.eq("conservationArea", ca)); //$NON-NLS-1$
		if (onlyActive){
			query.add(Restrictions.eq("isActive", true)); //$NON-NLS-1$
		}
		list = query.list();
		return list;
	}
	
	
	/**
	 * Gets all active transportation types for a given patrol type in a given
	 * conservation area.
	 * 
	 * @param ca conservation area
	 * @param s active session
	 * @param type patrol type 
	 * @return list of active transportation types for the given patrol type
	 */
	@SuppressWarnings("unchecked")
	public static List<PatrolTransportType> getActivePatrolTransporationTypes(ConservationArea ca, Session s, PatrolType.Type type){
		List<PatrolTransportType> types = null;
		types = s.createCriteria(PatrolTransportType.class)
				.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
				.add(Restrictions.eq("patrolType", type)) //$NON-NLS-1$
				.add(Restrictions.eq("isActive", true)).list(); //$NON-NLS-1$ 
		return types;
		
	}
	
	/**
	 * Gets all active transportation types for a all active patrol type in a given
	 * conservation area.
	 * 
	 * @param ca conservation area
	 * @param s active session
	 * 
	 * @return list of active transportation types for the given patrol type
	 */
	@SuppressWarnings("unchecked")
	public static List<PatrolTransportType> getActivePatrolTransporationTypes(ConservationArea ca, Session s){
		List<PatrolTransportType> types = null;
		String query = "SELECT p FROM PatrolTransportType p, PatrolType patroltype where patroltype.id.type = p.patrolType and p.isActive = 'true' and patroltype.isActive ='true' and p.conservationArea=:ca and patroltype.id.conservationArea = :ca2"; //'true' = derby fix //$NON-NLS-1$
		Query q = s.createQuery(query);
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.setParameter("ca2", ca); //$NON-NLS-1$
		types = q.list();
//		types = s.createCriteria(PatrolTransportType.class).add(Restrictions.eq("conservationArea", ca)).add(Restrictions.eq("patrolType", type)).add(Restrictions.eq("isActive", true)).list();
		return types;
	}
	
	/**
	 * Gets all  transportation types for a given patrol type in a given
	 * conservation area.
	 * 
	 * @param ca conservation area
	 * @param s active session
	 * @param type patrol type 
	 * @return list of transportation types for the given patrol type
	 */
	@SuppressWarnings("unchecked")
	public static List<PatrolTransportType> getPatrolTransporationTypes(ConservationArea ca, Session s, PatrolType.Type type){
		List<PatrolTransportType> types = null;
		types = s.createCriteria(PatrolTransportType.class)
				.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
				.add(Restrictions.eq("patrolType", type)).list(); //$NON-NLS-1$ 
		return types;
	}
	
	/**
	 * Gets all patrol types (active and in-active)
	 * for a given conservation area.
	 * <p>If types are not initialized
	 * this initializes the types. As a result
	 * this occurs within a transaction and cannot be wrapped within
	 * another transaction</p>
	 * 
	 * @param ca conservation area
	 * @param s active session
	 * @return list of active and in-active patrol types
	 */
	public static List<PatrolType> getPatrolTypes(ConservationArea ca, Session s){
		return getPatrolTypes(ca, s, false);
	}
	
	/**
	 * Gets active patrol types for a given
	 * conservation area.  Will return none, if not yet created.
	 * 
	 * @param ca conservation area
	 * @param s active session
	 * @return list of active patrol types
	 */
	@SuppressWarnings("unchecked")
	public static List<PatrolType> getActivePatrolTypes(ConservationArea ca, Session s){
		Criteria query = s.createCriteria(PatrolType.class).add(Restrictions.eq("id.conservationArea", ca)); //$NON-NLS-1$
		query.add(Restrictions.eq("isActive", true)); //$NON-NLS-1$
		return query.list();
	}

	/**
	 * Gets patrol types for a given conservation area.
	 * @param ca conservation area 
	 * @param s active session 
	 * @param onlyActive <code>true</code> if only active patrol types are to be returned, <code>false</code> otherwise
	 * @return list of patrol types
	 */
	@SuppressWarnings("unchecked")
	private static List<PatrolType> getPatrolTypes(ConservationArea ca, Session s, boolean onlyActive){
		s.beginTransaction();
		List<PatrolType> types = null;
		try{
			Criteria query = s.createCriteria(PatrolType.class)
					.add(Restrictions.eq("id.conservationArea", ca)); //$NON-NLS-1$
			if (onlyActive){
				query.add(Restrictions.eq("isActive", true)); //$NON-NLS-1$
			}
			types = query.list();
			s.getTransaction().rollback();
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(Messages.PatrolHibernateManager_20, ex);
			s.close();
			return null;
		}
		
		if (types.size() == 0){
			types = createPatrolTypes(ca, s);
		}
		return types;
	}
	
	/**
	 * Creates the default patrol types and saves them to the database.
	 * 
	 * @param ca conservation area to create types for
	 * @param s active sesion
	 * @return list of default patrol types
	 */
	public static List<PatrolType> createPatrolTypes(ConservationArea ca, Session s){
		List<PatrolType> types = new ArrayList<PatrolType>();
		s.beginTransaction();
		try {
			for (int i = 0; i < PatrolType.Type.values().length; i++) {
				PatrolType pt = new PatrolType();
				pt.setConservationArea(ca);
				pt.setIsActive(true);
				pt.setType(PatrolType.Type.values()[i]);

				s.save(pt);
				types.add(pt);
			}
			s.getTransaction().commit();
			return types;
		} catch (Exception ex) {
			s.getTransaction().rollback();
			SmartPatrolPlugIn.displayLog(Messages.PatrolHibernateManager_21, ex);
			s.close();
			return null;
		}
		
	}
	/**
	 * Determines if a patrol id already exists in the database
	 * for the given conservation area.
	 * 
	 * @param newId patrol id to validate
	 * @param ca conservation area
	 * @param session session
	 * @return <code>true</code> if id already exists; <code>false</code> otherwise
	 */
	public static boolean isDuplicateId(String newId, ConservationArea ca, Session session){
		Criteria c = session.createCriteria(Patrol.class)
				.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$ 
				.add(Restrictions.eq("id", newId)) //$NON-NLS-1$ 
				.setProjection(Projections.rowCount()); 
		Long cnt = (Long)c.uniqueResult();
		if (cnt > 0){
			return true;
		}
		return false;
	}
	
	/**
	 * Computes the next patrol id;
	 * @param p patrol to compute id for
	 * @param s active session (should be inside the transaction that is saving patrol)
	 * 
	 * @return patrol id for given patrol
	 */
	public static String generatePatrolId(Patrol p, Session s) {
		
		StringBuilder sb = new StringBuilder();
		sb.append(p.getConservationArea().getId());

		Query q = s
				.createQuery("SELECT id FROM Patrol WHERE id like :id and conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("id", sb.toString() + "%_%"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("ca", p.getConservationArea()); //$NON-NLS-1$

		long idNumber = 0;
		List<?> results = q.list();
		for (Iterator<?> iterator = results.iterator(); iterator.hasNext();) {
			String localId = (String) iterator.next();
			try {
				int idx = localId.lastIndexOf('_');
				String keypart = localId.substring(0, idx);
				if (keypart.equalsIgnoreCase(p.getConservationArea().getId())){
					String numpart = localId.substring(idx+1);
					long tmp = Integer.parseInt(numpart);
					if (tmp > idNumber) idNumber = tmp;
				}
				//break;
			} catch (Exception ex) {
				// not of the form CAID_# skip this one
			}
		}
		sb.append("_"); //$NON-NLS-1$
		idNumber = (idNumber + 1) % 1000000;
		if (idNumber <= 0) {
			idNumber = 1;
		}
		sb.append(PATROL_ID_FORMATTER.format(idNumber));

		return sb.toString();

	}
	
	/**
	 * Saves a given patrol to the database first starting a transaction.
	 * <p>This function does not close the session.  The calling code is responsible
	 * for closing the session. 
	 * The session must not have an active transaction; as it creates is own transaction.
	 * </p>
	 * @param patrol the patrol to save
	 * @param session the database session to use
	 * @param saveWaypoints if waypoints should also be saved; waypoints saving is not cascade automatically for performance reasons
	 * @return <code>true</code> if saved successfully, <code>false</code> if error
	 */
	public static boolean savePatrolInTransaction(Patrol patrol, Session session, boolean saveWaypoints){
		session.beginTransaction();
		try{
			savePatrol(patrol, session, saveWaypoints);
			session.getTransaction().commit();
		}catch (Exception ex){
			session.getTransaction().rollback();
			SmartPatrolPlugIn.displayLog(Messages.PatrolHibernateManager_Error_CouldNoSavePatrol + ex.getLocalizedMessage(), ex);
			return false;
		}
		return true;
	}
	
	/**
	 * Similar to savePatrolInTransaction except it doesn't not try to start a 
	 * transaction.  It assumes a transaction is already initialized 
	 * 
	 * @param patrol
	 * @param session
	 * @param saveWaypoints
	 * @return
	 */
	public static void savePatrol(Patrol patrol, Session session, boolean saveWaypoints) throws Exception{
		if (patrol.getId() == null || patrol.getId().equals(Patrol.AUTO_GENERATE_TEXT)){
			String id = PatrolHibernateManager.generatePatrolId(patrol, session);
			patrol.setId(id);
		}
		
		session.saveOrUpdate(patrol);
		
		if (saveWaypoints){
			session.flush();
		
			//save all the waypoints as well
			if (patrol.getLegs() != null) {
				for (PatrolLeg pl : patrol.getLegs()) {
					if (pl.getPatrolLegDays() != null) {
						for (PatrolLegDay pld : pl.getPatrolLegDays()) {
							if (pld.getWaypoints() != null) {
								for (PatrolWaypoint wp: pld.getWaypoints()){
									if (wp.getWaypoint().getAttachments() != null){
										//update all the waypoint attachments directory
										for (WaypointAttachment wa : wp.getWaypoint().getAttachments()){
											wa.setDatastoreFolderExtension(
												((PatrolWaypointSource)wp.getWaypoint().getSource()).getDatastoreFileLocation(patrol));
										}
									}
									if (wp.getWaypoint().getObservations() != null){
										for (WaypointObservation wo : wp.getWaypoint().getObservations()){
											if (wo.getAttachments() != null){
												for (ObservationAttachment wa : wo.getAttachments()){
													wa.setDatastoreFolderExtension(
															((PatrolWaypointSource)wp.getWaypoint().getSource()).getDatastoreFileLocation(patrol));
												}
											}
										}
									}
									
									session.saveOrUpdate(wp.getWaypoint());
									session.saveOrUpdate(wp);
								}
							}
						}
					}
				}
			}
		}
	}

	public static Map<PatrolScreenOptionMeta, ScreenOption> getScreenOptions(ConservationArea ca, Session session) {
		@SuppressWarnings("unchecked")
		List<ScreenOption> results = session.createCriteria(ScreenOption.class)
				.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
				.add(Restrictions.eq("resource", PatrolScreenOptionMeta.PATROL_RESOURCE_ID)) //$NON-NLS-1$
				.list();
		Map<PatrolScreenOptionMeta, ScreenOption> options = new HashMap<PatrolScreenOptionMeta, ScreenOption>();
		for (ScreenOption screenOption : results) {
			try {
				options.put(PatrolScreenOptionMeta.valueOf(screenOption.getType()), screenOption);
			} catch (IllegalArgumentException e) {
				//ignore unexpected screen type
				SmartPlugIn.log("Unexpected type for patrol meta screen.", e); //$NON-NLS-1$
			}
		}
		return options;
	}
	
	/**
	 * 
	 * @param session
	 * @return all patrol ids for the current conservation area
	 */
	public static List<String> getPatrolIds(Session session){
		String hql = "Select id FROM Patrol WHERE conservationArea = :ca"; //$NON-NLS-1$
		Query q = session.createQuery(hql);
		q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<String> data = q.list();
		return data;
	}
}
