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
package org.wcs.smart.plan;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.plan.filter.PlanFilter;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.NumericPlanTarget.TargetType;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.ui.editor.PlanEditorInput;
import org.wcs.smart.util.SmartUtils;

/**
 * Extension of the smart hibernate manager for plan related data.
 * 
 * @author jeffloun
 * @since 1.0.0
 */
public class PlanHibernateManager{
	
	private static NumberFormat PLAN_ID_FORMATTER = new DecimalFormat("000000"); //$NON-NLS-1$

	
	/**
	 * 
	 * @param s
	 * @return an array of plans without any parents.
	 */
	public static List<Plan> getAllRootPlans(Session s){
		s.beginTransaction();
		try{
			@SuppressWarnings("unchecked")
			List<Plan> plans = s.createCriteria(Plan.class).add(Restrictions.isNull("parent")).list(); //$NON-NLS-1$
			return plans;
		}finally{
			s.getTransaction().rollback();
		}
	}
	
	/**
	 * 
	 * @param s
	 * @return an array of plans without any parents.
	 */
	public static List<PlanEditorInput> getRootPlans(Session s, PlanFilter filter){
		s.beginTransaction();
		try{
			Query filterQuery = filter.buildQuery(s);
			
			@SuppressWarnings("unchecked")
			List<Object[]> results = filterQuery.list();
			
			Map<String, PlanEditorInput> inputs = new HashMap<String, PlanEditorInput>();
			Map<String, String>parents = new HashMap<String,String>();
			
			for (Object[] data : results){
				String uuid = SmartUtils.encodeHex((byte[]) data[0]);
				String name = Plan.generateLabel((String)data[1], (String)data[2]);
				
				inputs.put(uuid, new PlanEditorInput((byte[])data[0], name, (Plan.PlanType)data[3]));
				
				if (data[4] != null){
					parents.put(uuid, SmartUtils.encodeHex((byte[])data[4]));
				}
			}
			List<PlanEditorInput> all = new ArrayList<PlanEditorInput>();
			for(PlanEditorInput in : inputs.values()){
				String parent = parents.get(SmartUtils.encodeHex(in.getUuid()));
				if (parent != null){
					PlanEditorInput pparent = inputs.get(parent);
					if (pparent != null){
						in.setParent(pparent);
						pparent.addKid(in);
					}else{
						//parent not present
						all.add(in);
					}
				}else{
					all.add(in);
				}
			}
			
			return all;
		}finally{
			s.getTransaction().rollback();
		}
	}
		
	/**
	 * Computes the next plan id;
	 * @param p plan to compute id for
	 * @param s active session (should be inside the transaction that is saving plan)
	 * 
	 * @return plan id for given plan
	 */
	public static String generatePlanId(Plan p, Session s) {
		s.beginTransaction();
		StringBuilder sb = new StringBuilder();
		sb.append(p.getConservationArea().getId());

		Query q = s
				.createQuery("SELECT id FROM Plan WHERE id like :id ORDER BY id desc"); //$NON-NLS-1$
		q.setParameter("id", sb.toString() + "%"); //$NON-NLS-1$ //$NON-NLS-2$

		long idNumber = 0;
		List<?> results = q.list();
		for (Iterator<?> iterator = results.iterator(); iterator.hasNext();) {
			String localId = (String) iterator.next();
			try {
				idNumber = Integer.parseInt(localId.substring(localId
						.lastIndexOf('_') + 1));
				break;
			} catch (Exception ex) {
				// not of the form CAID_# skip this one
			}finally{
				s.getTransaction().rollback();
			}
		}
		sb.append("_"); //$NON-NLS-1$
		idNumber = (idNumber + 1) % 1000000;
		if (idNumber <= 0) {
			idNumber = 1;
		}
		sb.append(PLAN_ID_FORMATTER.format(idNumber));

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
			SmartPlanPlugIn.displayLog(Messages.PlanHibernateManager_SavePlan_Error + ex.getMessage(), ex);
			return false;
		}finally{
			session.close();
		}
		return true;
	}
	
	//the 3rd paremeter indicates that the plan is saved and in the database already, so we will definitely get 1-duplicate
	/**
	 * Determines if the given id is already used in the database.
	 * @param s current session
	 * @param id the id to check
	 * @param excludePlanUuid the plan uuid to exclude when checking for duplicates, <code>null</code>
	 * if no plans to be excluded.
	 * 
	 * @return
	 */
	public static boolean isDuplicatePlanId(Session s, String id, byte[] excludePlanUuid) {
		Criteria c = s.createCriteria(Plan.class).add(Restrictions.eq("id", id)); //$NON-NLS-1$
		if (excludePlanUuid != null){
			c.add(Restrictions.ne("uuid", excludePlanUuid)); //$NON-NLS-1$
		}
		c.setProjection(Projections.rowCount());
		Long cnt = (Long)c.list().get(0);
		if (cnt == 0){
			return false;
		}else{
			return true;
		}
	}
	

	/**
	 * Deletes the plan with the given uuid
	 * @param uuid
	 * @return the deleted plan
	 */
	public static Plan deletePlan(byte[] uuid) {
		Session session = HibernateManager.openSession();
		Plan plan = null;
		try {
			session.beginTransaction();
			try {
				plan = (Plan) session.load(Plan.class, uuid);
				
				deleteChildrenPlans(plan, session);
				
				session.getTransaction().commit();
			} catch (Exception ex) {
				session.getTransaction().rollback();
				SmartPlanPlugIn.displayLog(Messages.PlanHibernateManager_DeletePlan_Error + SmartUtils.LINE_SEPARATOR + ex.getLocalizedMessage(), ex);
				return null;
			}
		} finally {
			session.close();
		}
		return plan;
	}
	
	private static void deleteChildrenPlans(Plan parent, Session session){
		//delete all children
		for (Iterator<Plan> iterator = parent.getChildren().iterator(); iterator.hasNext();) {
			Plan child = iterator.next();
			child.setParent(null);
			iterator.remove();
			deleteChildrenPlans(child, session);	
		}
		parent.getChildren().clear();
		//then delete me
		String queryString = "DELETE FROM PatrolPlan WHERE id.plan = :plan"; //$NON-NLS-1$
		Query q = session.createQuery(queryString).setParameter("plan", parent); //$NON-NLS-1$
		q.executeUpdate();
		
		session.delete(parent);
		session.flush();
	}

	/**
	 * Returns the value of the target type for all patrols associated with this
	 * one plan. Does not recurse through the plan tree, that is done in the
	 * plantarget classes such as NumericPlanTarget.
	 * 
	 * @param type
	 *            the variable we are interested in, distance, patrol days,
	 *            etc...
	 * @param plan
	 *            the plan we are querying
	 * 
	 * @return the total calculated value from all associated patrols.
	 */
	public static Double getTargetTotalValue(TargetType type, Plan plan) {
		Double targetTotal;
		StringBuilder sql = new StringBuilder();
		targetTotal = 0.0;
		
		if (type == TargetType.DISTANCE) {
			sql.append(" SELECT "); //$NON-NLS-1$
			sql.append(" sum(t.distance) "); //$NON-NLS-1$
			sql.append(" FROM PatrolPlan pp "); //$NON-NLS-1$
			sql.append(" JOIN pp.id.patrol.legs pl"); //$NON-NLS-1$
			sql.append(" Join pl.patrolLegDays as pld "); //$NON-NLS-1$			
			sql.append(" JOIN pld.tracks as t"); //$NON-NLS-1$
			sql.append(" WHERE pp.id.plan  =:uuid "); //$NON-NLS-1$

			Session session = HibernateManager.openSession();
			Query q = session.createQuery(sql.toString());
			q.setParameter("uuid", plan); //$NON-NLS-1$

			List<?> rs = q.list();
			targetTotal = (Double)rs.get(0);

		}else if (type == TargetType.PATROL_DAYS) {
			sql.append(" SELECT "); //$NON-NLS-1$
			sql.append(" p.endDate, p.startDate "); //$NON-NLS-1$
			sql.append(" FROM PatrolPlan pp "); //$NON-NLS-1$
			sql.append(" JOIN pp.id.patrol p"); //$NON-NLS-1$
			sql.append(" WHERE pp.id.plan  =:uuid "); //$NON-NLS-1$

			Session session = HibernateManager.openSession();
			Query q = session.createQuery(sql.toString());
			q.setParameter("uuid", plan); //$NON-NLS-1$

			List<?> list = q.list();

			Iterator<?> it = list.iterator();
			if(it.hasNext()){
		        while(it.hasNext()){
		          Object[] row = (Object[])it.next();
		          Timestamp t1 = (Timestamp)row[0];
		          Timestamp t2 = (Timestamp)row[1];
		          Long milDiff = t1.getTime() - t2.getTime();
		          targetTotal += Math.round(milDiff / 1000 /60 /60 / 24) + 1; 
		        }
		     }
		}else if (type == TargetType.PATROL_HOURS) {
			sql.append(" SELECT "); //$NON-NLS-1$
			sql.append(" pld.endTime, pld.startTime "); //$NON-NLS-1$
			sql.append(" FROM PatrolPlan pp "); //$NON-NLS-1$
			sql.append(" JOIN pp.id.patrol.legs pl"); //$NON-NLS-1$
			sql.append(" Join pl.patrolLegDays as pld "); //$NON-NLS-1$			
			sql.append(" WHERE pp.id.plan  =:uuid "); //$NON-NLS-1$
			
			Session session = HibernateManager.openSession();
			Query q = session.createQuery(sql.toString());
			q.setParameter("uuid", plan); //$NON-NLS-1$

			List<?> list = q.list();

			Iterator<?> it = list.iterator();
			if(it.hasNext()){
		        while(it.hasNext()){
		          Object[] row = (Object[])it.next();
		          Time t1 = (Time)row[0];
		          Time t2 = (Time)row[1];
		          Long milDiff = (t1.getTime() + 1000)- t2.getTime(); //all our default end times for a whole day are 11:59:59, adding a second here to get 24hours for full days.
		          targetTotal += Math.round(milDiff / 1000 /60 / 60); 
		        }
		    }

		}else if (type == TargetType.PATROL_MANHOURS) {
			sql.append(" SELECT "); //$NON-NLS-1$
			sql.append(" pld.endTime, pld.startTime, m.isLeader "); //$NON-NLS-1$
			sql.append(" FROM PatrolPlan pp "); //$NON-NLS-1$
			sql.append(" JOIN pp.id.patrol.legs pl"); //$NON-NLS-1$
			sql.append(" JOIN pl.members m"); //$NON-NLS-1$
			sql.append(" Join pl.patrolLegDays as pld "); //$NON-NLS-1$			
			sql.append(" WHERE pp.id.plan  =:uuid "); //$NON-NLS-1$
			
			Session session = HibernateManager.openSession();
			Query q = session.createQuery(sql.toString());
			q.setParameter("uuid", plan); //$NON-NLS-1$

			List<?> list = q.list();

			Iterator<?> it = list.iterator();
			if(it.hasNext()){
		        while(it.hasNext()){
		          Object[] row = (Object[])it.next();
		          Time t1 = (Time)row[0];
		          Time t2 = (Time)row[1];
		          Long milDiff = (t1.getTime() + 1000)- t2.getTime(); //all our default end times for a whole day are 11:59:59, adding a second here to get 24hours for full days.
		          targetTotal += Math.round(milDiff / 1000 /60 / 60); 
		        }
		    }

		} else {
			return 1.0;
		}
		
		if(targetTotal== null)targetTotal = 0.0;
		return targetTotal;

	}

	/**
	 * Returns all patrols directory associated with a given plan.
	 *  
	 * @param plan
	 * @return a list of {@link PatrolEditorInput} directly associated with the plan.
	 */
	public static List<PatrolEditorInput> getPatrols(Plan plan){
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT pp.id.patrol.uuid, pp.id.patrol.id, pp.id.patrol.patrolType, pp.id.patrol.startDate, pp.id.patrol.endDate"); //$NON-NLS-1$
		sql.append(" FROM PatrolPlan pp "); //$NON-NLS-1$
		sql.append(" WHERE pp.id.plan  =:uuid ");  //$NON-NLS-1$
		
		Session session = HibernateManager.openSession();
		Query q = session.createQuery(sql.toString());
		q.setParameter("uuid", plan); //$NON-NLS-1$

		List<?> list = q.list();

		List<PatrolEditorInput> patrols = new ArrayList<PatrolEditorInput>();
		for (Iterator<?> iterator = list.iterator(); iterator.hasNext();) {
			Object[] data = (Object[]) iterator.next();
			
			PatrolEditorInput pi = new PatrolEditorInput((byte[])data[0], (String)data[1], (PatrolType.Type)data[2], (Date)data[3], (Date)data[4]);
			patrols.add(pi);
		}
		return patrols;		
	}
	
	/**
	 * Returns all tracks directory associated with a given plan.
	 *  
	 * @param plan
	 * @return a list of {@link PatrolEditorInput} directly associated with the plan.
	 */
	public static List<Track> getAllTracks(Plan plan){
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT pld.tracks"); //$NON-NLS-1$
		sql.append(" FROM PatrolPlan pp "); //$NON-NLS-1$
		sql.append(" JOIN pp.id.patrol.legs pl"); //$NON-NLS-1$
		sql.append(" Join pl.patrolLegDays as pld "); //$NON-NLS-1$			
		sql.append(" WHERE pp.id.plan  =:uuid ");//$NON-NLS-1$

		
		Session session = HibernateManager.openSession();
		Query q = session.createQuery(sql.toString());
		q.setParameter("uuid", plan); //$NON-NLS-1$

		List<?> list = q.list();

		List<Track> tracks = new ArrayList<Track>();
		for (Iterator<?> iterator = list.iterator(); iterator.hasNext();) {
			tracks.add( (Track)iterator.next() );
		}
		return tracks;		

	}
}
