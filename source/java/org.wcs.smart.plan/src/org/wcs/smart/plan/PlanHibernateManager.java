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

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.plan.filter.PlanFilter;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.PatrolPlan;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.ui.editor.PlanEditorInput;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Extension of the smart hibernate manager for plan related data.
 * 
 * @author jeffloun
 * @since 1.0.0
 */
public class PlanHibernateManager{
	
	private static NumberFormat PLAN_ID_FORMATTER = new DecimalFormat("000000"); //$NON-NLS-1$

	
	public static Plan loadPlan(UUID planUuid) {
		Plan p = null;
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				p = (Plan) Hibernate.unproxy( session.get(Plan.class, planUuid));
				if (p.getTargets() != null) p.setTargets(new ArrayList<>(p.getTargets()));
				Hibernate.initialize(p.getNames());
				Hibernate.initialize(p.getStation());
				Hibernate.initialize(p.getTeam());
				Hibernate.initialize(p.getParent());
			}finally{
				session.getTransaction().rollback();
			}
		}
		return p;
	}
	
	/**
	 * 
	 * @param s
	 * @return an array of plans without any parents.
	 */
	public static List<PlanEditorInput> getRootPlans(Session s, PlanFilter filter){
		s.beginTransaction();
		try{
			return filter.getResultsAsTree(s);
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
		return generatePlanId(p, s, Collections.emptySet());
	}
	
	/**
	 * 
	 * @param p
	 * @param s
	 * @param others set of other ids that cannot be used
	 * @return
	 */
	public static String generatePlanId(Plan p, Session s, Set<String> others) {
		StringBuilder sb = new StringBuilder();
		sb.append(p.getConservationArea().getId());
		sb.append("_"); //$NON-NLS-1$
		
		Transaction tx = s.getTransaction();
		tx.begin();
		try{
			List<String> results = s.createQuery("SELECT id FROM Plan WHERE id like :id and conservationArea = :ca ORDER BY id desc", String.class) //$NON-NLS-1$
				.setParameter("id", sb.toString() + "%") //$NON-NLS-1$ //$NON-NLS-2$
				.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
				.list();

			long idNumber = 0;
			for (String localId : results) {
				try {
					idNumber = Integer.parseInt(localId.substring(localId.lastIndexOf('_') + 1));
					break;
				} catch (Exception ex) {
					// not of the form CAID_# skip this one
				}
			}
			
			for (int i = 0; i < 100_000; i++) {
				idNumber = (idNumber + 1) % 1000000;
				if (idNumber <= 0) idNumber = 1;
				
				String id = sb.toString() + PLAN_ID_FORMATTER.format(idNumber);
				if (!others.contains(id)) break;
			}
			
			sb.append(PLAN_ID_FORMATTER.format(idNumber));
		}finally{
			tx.commit();
			
		}
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
			//save a name
			plan.updateName(SmartDB.getCurrentLanguage(), plan.getName());
			if (plan.getUuid() == null) {
				session.persist(plan);
			} else {
				for (PlanTarget pt : plan.getTargets()) {
					if (pt.getUuid() == null) session.persist(pt);
				}
				session.merge(plan);
			}
			
			session.getTransaction().commit();
		}catch (Exception ex){
			session.getTransaction().rollback();
			SmartPlanPlugIn.displayLog(Messages.PlanHibernateManager_SavePlan_Error + ex.getMessage(), ex);
			return false;
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
	public static boolean isDuplicatePlanId(Session s, String id, UUID excludePlanUuid) {
		CriteriaBuilder cb = s.getCriteriaBuilder();
		
		CriteriaQuery<Long> c = cb.createQuery(Long.class);
		Root<Plan> from = c.from(Plan.class);
		c.select(cb.count(from));		
		Predicate[] filters = new Predicate[excludePlanUuid != null ? 3 : 2];
		filters[0] = cb.equal(from.get("id"), id); //$NON-NLS-1$
		filters[1] = cb.equal(from.get("conservationArea"), SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		if (excludePlanUuid != null) {
			filters[2] = cb.notEqual(from.get("uuid"), excludePlanUuid); //$NON-NLS-1$
		}
		c.where(cb.and(filters));
		Long cnt = s.createQuery(c).uniqueResult();
		if (cnt == 0){
			return false;
		}else{
			return true;
		}
	}
	

	/**
	 * Deletes the plan with the given uuid
	 * @param uuid
	 * @return the deleted plans - all children are automatically deleted
	 */
	public static Set<Plan> deletePlan(UUID uuid) {
		Set<Plan> deletedItems = new HashSet<Plan>();
		Plan plan = null;
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				plan = (Plan) session.get(Plan.class, uuid);
				if (plan != null){	//if null then already removed from the database
					deleteChildrenPlans(plan, session, deletedItems);
				}
				
				session.getTransaction().commit();
			} catch (Exception ex) {
				session.getTransaction().rollback();
				SmartPlanPlugIn.displayLog(Messages.PlanHibernateManager_DeletePlan_Error + SharedUtils.LINE_SEPARATOR + ex.getLocalizedMessage(), ex);
				return null;
			}
		}
		return deletedItems;
	}
	
	private static void deleteChildrenPlans(Plan parent, Session session, Set<Plan> deletedItems){
		//delete all children
		for (Iterator<Plan> iterator = parent.getChildren().iterator(); iterator.hasNext();) {
			Plan child = iterator.next();
			child.setParent(null);
			iterator.remove();
			deleteChildrenPlans(child, session, deletedItems);	
		}
		parent.getChildren().clear();
		//then delete me
		String queryString = "DELETE FROM PatrolPlan WHERE id.plan = :plan"; //$NON-NLS-1$
		
		session.createMutationQuery(queryString)
			.setParameter("plan", parent) //$NON-NLS-1$
			.executeUpdate();
		
		session.remove(parent);
		deletedItems.add(parent);
	}

	

	/**
	 * Returns all patrols directly associated with a given plan.
	 *  
	 * @param plan
	 * @return a list of {@link PatrolEditorInput} directly associated with the plan.
	 */
	public static List<PatrolEditorInput> getPatrols(Plan plan, Session session){
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT pp.id.patrol.uuid, pp.id.patrol.id, pp.id.patrol.patrolType, pp.id.patrol.startDate, pp.id.patrol.endDate"); //$NON-NLS-1$
		sql.append(" FROM PatrolPlan pp "); //$NON-NLS-1$
		sql.append(" WHERE pp.id.plan  =:uuid ");  //$NON-NLS-1$
		
		List<PatrolEditorInput> patrols = new ArrayList<PatrolEditorInput>();

		Query<Tuple> q = session.createQuery(sql.toString(), Tuple.class);
		q.setParameter("uuid", plan); //$NON-NLS-1$

		List<Tuple> list = q.list();

		for (Iterator<Tuple> iterator = list.iterator(); iterator.hasNext();) {
			Tuple data = (Tuple) iterator.next();

			PatrolEditorInput pi = new PatrolEditorInput((UUID)data.get(0),
					(String) data.get(1), (PatrolType.Type) data.get(2),
					(LocalDate) data.get(3), (LocalDate) data.get(4));
			patrols.add(pi);
		}
		return patrols;
	}
	


	/**
	 * Returns all plan IDs of given parent that do not fit in specify start/end date range.
	 * Used for validation purposes when changing date of a plan that has child plans.
	 * 
	 * @param planUuid - uuid of a parent Plan
	 * @param start - start date
	 * @param end - end date
	 * @return a list of Plan IDs
	 */
	public static List<String> getPlanChildrenOutOfDateRange(UUID planUuid, LocalDate start, LocalDate end) {
		if (planUuid == null) {
			return Collections.emptyList();
		}
		try(Session session = HibernateManager.openSession()) {
			String sql = "SELECT p.id FROM Plan p WHERE p.parent.uuid = :uuid AND (p.startDate < :start OR coalesce(p.endDate, p.startDate) > :end)"; //$NON-NLS-1$
			List<String> plans = new ArrayList<String>();
			Query<String> q = session.createQuery(sql, String.class);
			q.setParameter("uuid", planUuid); //$NON-NLS-1$
			q.setParameter("start", start); //$NON-NLS-1$
			q.setParameter("end", end); //$NON-NLS-1$

			List<?> list = q.list();
			for (Iterator<?> iterator = list.iterator(); iterator.hasNext();) {
				plans.add((String) iterator.next());
			}
			return plans;		
		}
	}

	/**
	 * Returns a list of all plans in given Conservation area
	 * 
	 * @return a list of Plans
	 */
	public static List<Plan> getPlans(ConservationArea ca, Session session) {
		return QueryFactory.buildQuery(session, Plan.class, "conservationArea", ca).getResultList(); //$NON-NLS-1$
	}

	/**
	 * Loads the list item for the given plan uuid
	 * 
	 * @return a list item for the given plan
	 * @throws Exception 
	 * @throws  
	 */
	public static ListItem getPlan(Session session, String id) throws Exception {
		Query<Tuple> q = session.createQuery("SELECT uuid, id, name FROM Plan WHERE uuid =:uuid", Tuple.class); //$NON-NLS-1$
		q.setParameter("uuid", UuidUtils.stringToUuid(id)); //$NON-NLS-1$
		List<Tuple> results = q.list();
		if (results.size() == 1) {
			Tuple data = results.get(0);
			
			String displayName = Plan.generateLabel((String)data.get(1), (String)data.get(2));
			return new ListItem( (UUID)data.get(0), displayName);
		} else {
			SmartPlanPlugIn.displayLog(MessageFormat.format(Messages.PlanHibernateManager_Plan_NotFound_Error, id), null);
			return null;
		}
	}

	
	/**
	 * For given patrol returns a plan to which this patrol belong.
	 * 
	 * @return plan to which the patrol belong
	 */
	public static Plan getPlanForPatrol(Patrol patrol, Session session) {
		List<PatrolPlan> plans = QueryFactory.buildQuery(session, PatrolPlan.class, "id.patrol", patrol).getResultList(); //$NON-NLS-1$
		if (plans.size() == 1) {
			return plans.get(0).getPlan();
		} else if (plans.size() > 1) {
			SmartPlanPlugIn.displayLog(Messages.PlanHibernateManager_ErrorMatchingPatrolToPlan, null);
		}
		return null;
	}

	/**
	 * Returns a list of all plans in given Conservation area with give id
	 * 
	 * @return a list of Plans
	 */
	public static List<Plan> getPlansById(Session session, ConservationArea ca, String id) {
		return QueryFactory.buildQuery(session, Plan.class,
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"id", id}).getResultList(); //$NON-NLS-1$
	}

	
	
}
