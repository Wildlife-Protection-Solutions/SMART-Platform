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

import java.text.Collator;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.plan.filter.PlanFilter;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.PatrolPlan;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.ui.editor.PlanEditorInput;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

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
	public static List<PlanEditorInput> getRootPlans(Session s, PlanFilter filter){
		s.beginTransaction();
		try{
			Query filterQuery = filter.buildQuery(s);
			
			@SuppressWarnings("unchecked")
			List<Object[]> results = filterQuery.list();
			// ORDER BY p.name asc, p.id desc
			Collections.sort(results, new Comparator<Object[]>(){
				@Override
				public int compare(Object[] arg0, Object[] arg1) {
					String name0 = (String)arg0[2];
					String name1 = (String)arg1[2];
					if (name0 == null) name0 = ""; //$NON-NLS-1$
					if (name1 == null) name1 = ""; //$NON-NLS-1$
					int result = Collator.getInstance().compare(name0, name1);
					if (result != 0)
						return result;
					String id0 = (String)arg0[1];
					String id1 = (String)arg1[1];
					if (id0 == null) id0 = ""; //$NON-NLS-1$
					if (id1 == null) id1 = ""; //$NON-NLS-1$
					return -Collator.getInstance().compare(id0, id1); //minus for desc sort
				}
			});
			
			Map<String, PlanEditorInput> inputs = new LinkedHashMap<String, PlanEditorInput>();
			Map<String, String>parents = new LinkedHashMap<String,String>();
			
			for (Object[] data : results){
				String uuid = UuidUtils.uuidToString((UUID) data[0]);
				String name = Plan.generateLabel((String)data[1], (String)data[2]);
				
				inputs.put(uuid, new PlanEditorInput((UUID)data[0], name, (Plan.PlanType)data[3]));
				
				if (data[4] != null){
					parents.put(uuid, UuidUtils.uuidToString((UUID)data[4]));
				}
			}
			List<PlanEditorInput> all = new ArrayList<PlanEditorInput>();
			for(PlanEditorInput in : inputs.values()){
				String parent = parents.get(UuidUtils.uuidToString(in.getUuid()));
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
		
		StringBuilder sb = new StringBuilder();
		sb.append(p.getConservationArea().getId());
		sb.append("_"); //$NON-NLS-1$
		
		Transaction tx = s.getTransaction();
		tx.begin();
		try{
			Query q = s.createQuery("SELECT id FROM Plan WHERE id like :id and conservationArea = :ca ORDER BY id desc"); //$NON-NLS-1$
			q.setParameter("id", sb.toString() + "%"); //$NON-NLS-1$ //$NON-NLS-2$
			q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$

			long idNumber = 0;
			List<?> results = q.list();
			for (Iterator<?> iterator = results.iterator(); iterator.hasNext();) {
				String localId = (String) iterator.next();
				try {
					idNumber = Integer.parseInt(localId.substring(localId.lastIndexOf('_') + 1));
					break;
				} catch (Exception ex) {
					// not of the form CAID_# skip this one
				}
			}
			idNumber = (idNumber + 1) % 1000000;
			if (idNumber <= 0) {
				idNumber = 1;
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
			session.saveOrUpdate(plan);
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
		Criteria c = s.createCriteria(Plan.class).add(Restrictions.eq("id", id)).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())); //$NON-NLS-1$ //$NON-NLS-2$
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
	 * @return the deleted plans - all children are automatically deleted
	 */
	public static Set<Plan> deletePlan(UUID uuid) {
		Set<Plan> deletedItems = new HashSet<Plan>();
		Session session = HibernateManager.openSession();
		Plan plan = null;
		try {
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
		} finally {
			session.close();
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
		Query q = session.createQuery(queryString).setParameter("plan", parent); //$NON-NLS-1$
		q.executeUpdate();
		
		session.delete(parent);
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

		Query q = session.createQuery(sql.toString());
		q.setParameter("uuid", plan); //$NON-NLS-1$

		List<?> list = q.list();

		for (Iterator<?> iterator = list.iterator(); iterator.hasNext();) {
			Object[] data = (Object[]) iterator.next();

			PatrolEditorInput pi = new PatrolEditorInput((UUID) data[0],
					(String) data[1], (PatrolType.Type) data[2],
					(Date) data[3], (Date) data[4]);
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
	public static List<String> getPlanChildrenOutOfDateRange(UUID planUuid, Date start, Date end) {
		if (planUuid == null) {
			return Collections.emptyList();
		}
		Session session = HibernateManager.openSession();
		try {
			String sql = "SELECT p.id FROM Plan p WHERE p.parent.uuid = :uuid AND (p.startDate < :start OR coalesce(p.endDate, p.startDate) > :end)"; //$NON-NLS-1$
			List<String> plans = new ArrayList<String>();
			Query q = session.createQuery(sql);
			q.setParameter("uuid", planUuid); //$NON-NLS-1$
			q.setParameter("start", start); //$NON-NLS-1$
			q.setParameter("end", end); //$NON-NLS-1$

			List<?> list = q.list();
			for (Iterator<?> iterator = list.iterator(); iterator.hasNext();) {
				plans.add((String) iterator.next());
			}
			return plans;		
		} finally {
			session.close();
		}
	}

	/**
	 * Returns a list of all plans in given Conservation area
	 * 
	 * @return a list of Plans
	 */
	public static List<Plan> getPlans(ConservationArea ca, Session session) {
		Criteria criteria = session.createCriteria(Plan.class);
		criteria.add(Restrictions.eq("conservationArea", ca)); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<Plan> plans = criteria.list();
		return plans;
	}

	/**
	 * Loads the list item for the given plan uuid
	 * 
	 * @return a list item for the given plan
	 * @throws Exception 
	 * @throws  
	 */
	public static ListItem getPlan(Session session, String id) throws Exception {
		Query q = session.createQuery("SELECT uuid, id, name FROM Plan WHERE uuid =:uuid"); //$NON-NLS-1$
		q.setParameter("uuid", UuidUtils.stringToUuid(id)); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<Object[]> results = q.list();
		if (results.size() == 1) {
			String displayName = Plan.generateLabel((String)((Object[])results.get(0))[1], (String)((Object[])results.get(0))[2]);
			return new ListItem( (UUID)((Object[])results.get(0))[0], displayName);
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
		List<?> plans = session.createCriteria(PatrolPlan.class).add(Restrictions.eq("id.patrol", patrol)).list(); //$NON-NLS-1$

		if (plans.size() == 1) {
			return ((PatrolPlan) plans.get(0)).getPlan();
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
		Criteria criteria = session.createCriteria(Plan.class);
		criteria.add(Restrictions.eq("conservationArea", ca)); //$NON-NLS-1$
		criteria.add(Restrictions.eq("id", id)); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<Plan> plans = criteria.list();
		return plans;
	}

	
	
}
