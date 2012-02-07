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
import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolOptions;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointAttachment;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;

/**
 * Extension of the smart hibernate manager for patrol related data.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolHibernateManager extends HibernateManager{
	
	private static NumberFormat PATROL_ID_FORMATTER = new DecimalFormat("000");
	private static NumberFormat PATROL_ID_DATE_FIELD_FORMATTER = new DecimalFormat("00");
	
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
	private static List<Team> getTeams(ConservationArea ca, Session s, boolean onlyActive){
		s.beginTransaction();
		List<Team> list = null;
		try{
			Criteria query = s.createCriteria(Team.class).add(Restrictions.eq("conservationArea", ca));
			if (onlyActive){
				query.add(Restrictions.eq("isActive", true));
			}
			list = query.list();
			s.getTransaction().commit();
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog("Error loading patrol mandates.", ex);
			s.close();
		}
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
	private static List<PatrolMandate> getMandates(ConservationArea ca, Session s, boolean onlyActive){
		s.beginTransaction();
		List<PatrolMandate> list = null;
		try{
			Criteria query = s.createCriteria(PatrolMandate.class).add(Restrictions.eq("conservationArea", ca));
			if (onlyActive){
				query.add(Restrictions.eq("isActive", true));
			}
			list = query.list();
			s.getTransaction().commit();
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog("Error loading patrol mandates.", ex);
			s.close();
		}
		return list;
		
	}
	
	/**
	 * Loads the patrol options for a given conservation area.
	 * <p>
	 * If patrol options does not exist, it will create a new one with default values.
	 * </p>
	 * 
	 * @param ca conservation area 
	 * @param s active session
	 * @return the patrol options
	 */
	public static PatrolOptions getPatrolOptions(ConservationArea ca, Session s){
		s.beginTransaction();
		try{
			List<PatrolOptions> ops = s.createCriteria(PatrolOptions.class).
					add(Restrictions.eq("uuid", ca.getUuid())).list();
			if (ops.size() > 1){
				throw new IllegalStateException("A conservation area cannot have more than one set of patrol options.");
			}
			PatrolOptions op = null;
			if (ops.size() == 0){
				op = createPatrolOption(ca, s);
			}else{
				op = ops.get(0);
			}
			s.getTransaction().commit();
			return op;
		}catch (Exception ex){
			s.getTransaction().rollback();
			s.close();
			SmartPatrolPlugIn.displayLog("Could not load patrol options. " + ex.getMessage(), ex);
			
		}
		return null;
	}
	
	/**
	 * Creates new patrol options for a given conservation area and saves it to the database.
	 * 
	 * @param ca conservation area 
	 * @param s active session
	 * @return
	 */
	private static PatrolOptions createPatrolOption(ConservationArea ca, Session s){
		PatrolOptions po = new PatrolOptions();
		po.setTrackDistanceDirection(false);
		po.setEditTime(null);
		po.setUuid(ca.getUuid());
		s.saveOrUpdate(po);
		return po;
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
	public static List<PatrolTransportType> getActivePatrolTransporationTypes(ConservationArea ca, Session s, PatrolType.Type type){
		s.beginTransaction();
		List<PatrolTransportType> types = null;
		try{
			types = s.createCriteria(PatrolTransportType.class).add(Restrictions.eq("conservationArea", ca)).add(Restrictions.eq("patrolType", type)).add(Restrictions.eq("isActive", true)).list();
			s.getTransaction().commit();
			return types;
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog("Error loading patrol types", ex);
			s.close();
		}
		return null;
	}
	
	/**
	 * Gets all patrol types (active and in-active)
	 * for a given conservation area.
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
	 * conservation area.
	 * 
	 * @param ca conservation area
	 * @param s active session
	 * @return list of active patrol types
	 */
	public static List<PatrolType> getActivePatrolTypes(ConservationArea ca, Session s){
		return getPatrolTypes(ca, s, true);
	}

	/**
	 * Gets patrol types for a given conservation area.
	 * @param ca conservation area 
	 * @param s active session 
	 * @param onlyActive <code>true</code> if only active patrol types are to be returned, <code>false</code> otherwise
	 * @return list of patrol types
	 */
	private static List<PatrolType> getPatrolTypes(ConservationArea ca, Session s, boolean onlyActive){
		s.beginTransaction();
		List<PatrolType> types = null;
		try{
			Criteria query = s.createCriteria(PatrolType.class).add(Restrictions.eq("id.conservationArea", ca));
			if (onlyActive){
				query.add(Restrictions.eq("isActive", true));
			}
			types = query.list();
			s.getTransaction().commit();
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog("Error loading patrol types", ex);
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

				s.saveOrUpdate(pt);
				types.add(pt);
			}
			s.getTransaction().commit();
			return types;
		} catch (Exception ex) {
			s.getTransaction().rollback();
			SmartPatrolPlugIn.displayLog("Error loading patrol types.  Please contact the conservation area administrator and ensure the patrol types have been initialized.", ex);
			s.close();
			return null;
		}
		
	}
	
	/**
	 * Saves a patrol.
	 * 
	 * @param p patrol to save
	 * @param s active session
	 * @param objectsToDelete items removed from the patrol that need to be deleted from the database
	 * 
	 * @return <code>true</code> if saved successfully, <code>false</code> if failed to save
	 */
	public static boolean  savePatrol(Patrol p, Session s, List<Object> objectsToDelete){
		s.beginTransaction();
		
		try{
			if (p.getId() == null){
				String id = PatrolHibernateManager.generatePatrolId(p, s);
				p.setId(id);
			}
			
			if (objectsToDelete != null){
				for (Object toDelete :objectsToDelete){
					s.delete(toDelete);
				}
			}
			s.saveOrUpdate(p);
			for (PatrolLeg leg: p.getLegs()){
				s.saveOrUpdate(leg);
				for (PatrolLegDay day : leg.getPatrolLegDays()){
					s.saveOrUpdate(day);
					if (day.getWaypoints() != null){
						for (Waypoint wpt : day.getWaypoints()){
							s.saveOrUpdate(wpt);
						
							if (wpt.getAttachments() != null){
								for (WaypointAttachment attachment: wpt.getAttachments()){
									s.saveOrUpdate(attachment);
								}
							}
							if (wpt.getObservations() != null){
								for (WaypointObservation observation : wpt.getObservations()){
									s.saveOrUpdate(observation);
									if (observation.getAttributes() != null){
										for(WaypointObservationAttribute att : observation.getAttributes()){
											s.saveOrUpdate(att);
										}
									}
								}
							}
						}
					}
				}
				
				for (PatrolLegMember member : leg.getMembers()){
					s.saveOrUpdate(member);
				}
			}
			s.getTransaction().commit();
			return true;
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog("Could not save patrol. " + ex.getMessage(), ex);
			s.getTransaction().rollback();
			s.close();
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
	public static String generatePatrolId(Patrol p, Session s){
		StringBuilder sb = new StringBuilder();
		sb.append(p.getConservationArea().getId());
		sb.append("_");
		Calendar cal = SmartPlugIn.convertDate(p.getStartDate());
		sb.append(PATROL_ID_DATE_FIELD_FORMATTER.format(cal.get(Calendar.YEAR)));
		sb.append("_");
		sb.append(PATROL_ID_DATE_FIELD_FORMATTER.format(cal.get(Calendar.MONTH)+1));
		sb.append("_");
		sb.append(PATROL_ID_DATE_FIELD_FORMATTER.format(cal.get(Calendar.DAY_OF_MONTH)));

		Criteria c = s.createCriteria(Patrol.class).add(Restrictions.like("id", sb.toString() + "%")).setProjection(Projections.rowCount());
			
		long cnt = (Long) c.list().get(0);
		cnt++;
		cnt = cnt % 999;

		sb.append("_");
		sb.append(PATROL_ID_FORMATTER.format(cnt));
		return sb.toString();
		
	}
}
