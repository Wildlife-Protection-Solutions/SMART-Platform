package org.wcs.smart.plan;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;

/**
 * Extension of the smart hibernate manager for plan related data.
 * 
 * @author jeffloun
 * @since 1.0.0
 */
public class PlanHibernateManager extends HibernateManager{
	
	private static NumberFormat PLAN_ID_FORMATTER = new DecimalFormat("000000");

	
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
		List<Team> list = null;
		Criteria query = s.createCriteria(Team.class).add(Restrictions.eq("conservationArea", ca));
		if (onlyActive){
			query.add(Restrictions.eq("isActive", true));
		}
		list = query.list();
		return list;
	}
	
	
		
	/**
	 * Computes the next plan id;
	 * @param p plan to compute id for
	 * @param s active session (should be inside the transaction that is saving plan)
	 * 
	 * @return plan id for given plan
	 */
	public static String generatePlanId(Plan p, Session s){
		StringBuilder sb = new StringBuilder();
		sb.append(p.getConservationArea().getId());

		Query q = s.createQuery("SELECT id FROM Plan WHERE id like :id ORDER BY id desc");
		q.setParameter("id", sb.toString() + "%");

		long idNumber = 0;
		List<?> results = q.list();
		for (Iterator<?> iterator = results.iterator(); iterator.hasNext();) {
			String localId = (String) iterator.next();
			try{
				idNumber = Integer.parseInt(localId.substring(localId.lastIndexOf('_')+1));
				break;
			}catch (Exception ex){
				//not of the form CAID_# skip this one
			}
		}
		sb.append("_");
		idNumber = (idNumber+1) % 1000000;
		if (idNumber <= 0){
			idNumber = 1;
		}
		sb.append(PLAN_ID_FORMATTER.format(idNumber));
		s.evict(p.getConservationArea());
		return sb.toString();
		
	}
	
	/**
	 * Saves a given plan to the database.
	 * 
	 * @param plan the plan to save
	 * @param session the database session to use
	 * @param saveWaypoints if waypoints should also be saved; waypoints saving is not cascade automatically for performance reasons
	 * @return <code>true</code> if saved successfully, <code>false</code> if error
	 */
	public static boolean savePlan(Plan plan, Session session){
		session.beginTransaction();
		try{
			if (plan.getId() == null ){
				String id = PlanHibernateManager.generatePlanId(plan, session);
				plan.setId(id);
			}
			session.saveOrUpdate(plan);
			session.getTransaction().commit();
		}catch (Exception ex){
			session.getTransaction().rollback();
			
			SmartPlanPlugIn.displayLog("Could not save plan. " + ex.getMessage(), ex);
			return false;
		}finally{
			session.close();
		}
		return true;
	}
	
}
