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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.filter.PlanFilter;
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
	
	private static NumberFormat PLAN_ID_FORMATTER = new DecimalFormat("000000");

	
	/**
	 * 
	 * @param s
	 * @return an array of plans without any parents.
	 */
	public static List<Plan> getAllRootPlans(Session s){
		s.beginTransaction();
		try{
			List<Plan> plans = s.createCriteria(Plan.class).add(Restrictions.isNull("parent")).list();
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
			
			List<Object[]> results = filterQuery.list();
			
			Map<String, PlanEditorInput> inputs = new HashMap<String, PlanEditorInput>();
			Map<String, String>parents = new HashMap<String,String>();
			
			for (Object[] data : results){
				String uuid = SmartUtils.encodeHex((byte[]) data[0]);
				String name = "[" + (String) data[1] + "]";
				if (data[2] != null){
					name = data[2]  + " " + name;
				}				
				
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
				.createQuery("SELECT id FROM Plan WHERE id like :id ORDER BY id desc");
		q.setParameter("id", sb.toString() + "%");

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
		sb.append("_");
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
			SmartPlanPlugIn.displayLog("Could not save plan. " + ex.getMessage(), ex);
			return false;
		}finally{
			session.close();
		}
		return true;
	}
	
	//the 3rd paremeter indicates that the plan is saved and in the database already, so we will definitely get 1-duplicate
	public static boolean isDuplicatePlanId(Session s, String id, boolean isSaved) {
		int count = numberPlanId(s, id);
		if (count > 1) {
			return true;
		}
		return false;
	}
	public static boolean isDuplicatePlanId(Session s, String id) {
		int count = numberPlanId(s, id);
		if (count > 0) {
			return true;
		}
		return false;
	}
	
	private static int numberPlanId(Session s, String id){
		int count =99;
		s.beginTransaction();
		try {
			Query q = s
					.createQuery("SELECT count(*) FROM Plan WHERE id = '" + id + "'");

			count = Integer.parseInt(q.list().get(0).toString());

		}finally{
			s.getTransaction().rollback();
		}
		return count;
	}


	public static Plan deletePlan(byte[] uuid) {
		Session session = HibernateManager.openSession();
		Plan plan = null;
		try {
			session.beginTransaction();
			try {
				plan = (Plan) session.load(Plan.class, uuid);
				session.delete(plan);
				session.getTransaction().commit();
			} catch (Exception ex) {
				session.getTransaction().rollback();
				SmartPlanPlugIn.displayLog("Error Deleting Plan" + "\n"+ ex.getLocalizedMessage(), ex); //$NON-NLS-1$
				return null;
			}
		} finally {
			session.close();
		}
		return plan;
	}
		
}
