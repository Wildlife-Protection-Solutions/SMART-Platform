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
package org.wcs.smart.plan.model;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.model.NumericPlanTarget.Operator;
import org.wcs.smart.plan.model.NumericPlanTarget.TargetType;
import org.wcs.smart.plan.model.PlanTargetStatus.Status;
import org.wcs.smart.util.GeometryUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Computation engine for computing the status of
 * plan targets.
 * 
 * @author Emily
 *
 */
public class PlanTargetEngine {

	private static PlanTargetEngine INSTANCE = new PlanTargetEngine();
	
	public static PlanTargetEngine getInstance(){
		return INSTANCE;
	}
	
	
	/**
	 * Computes the current status of the given plan target.
	 * <p>
	 * This should be called outside of the main gui thread and outside
	 * of a session.  The engine opens and closes its own sessions
	 * are required.
	 * </p>
	 * @param target
	 * @return
	 */
	public PlanTargetStatus computeTargetStatus(PlanTarget target){
		if (target instanceof AdministrativePlanTarget){
			return computeAdministrativePlanTarget((AdministrativePlanTarget) target);
		}else if (target instanceof NumericPlanTarget){
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				return computeNumericPlanTarget((NumericPlanTarget) target, session);
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
		}else if (target instanceof SpatialPlanTarget){
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				return computeSpatialPlanTarget((SpatialPlanTarget) target, session);
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
		}else{
			//unknown
			return new PlanTargetStatus(Status.INCOMPLETE);
		}
	}
	
	/**
	 * Computes the current status of the given plan target.
	 * <p>
	 * This should be called outside of the main gui thread and outside
	 * of a session.
	 * </p>
	 * @param target
	 * @return
	 */
	public PlanTargetStatus computeTargetStatus(PlanTarget target, Session session){
		if (target instanceof AdministrativePlanTarget){
			return computeAdministrativePlanTarget((AdministrativePlanTarget) target);
		}else if (target instanceof NumericPlanTarget){
			return computeNumericPlanTarget((NumericPlanTarget) target, session);
		}else if (target instanceof SpatialPlanTarget){
			return computeSpatialPlanTarget((SpatialPlanTarget) target, session);
		}else{
			//unknown
			return new PlanTargetStatus(Status.INCOMPLETE);
		}
	}
	
	private PlanTargetStatus computeSpatialPlanTarget(SpatialPlanTarget target, Session session) {
		SpatialPlanTarget thisTarget = null;
		PlanTargetStatus result = new PlanTargetStatus(Status.COMPLETE);

		thisTarget = (SpatialPlanTarget) session.load(SpatialPlanTarget.class, target.getUuid());
		if (thisTarget == null) {
			return new PlanTargetStatus(Status.INCOMPLETE);
		}
		List<SpatialPlanTargetPoint> points= target.getPoints();
		for(SpatialPlanTargetPoint n : points){
			//as soon as we see one point was not met, just break so we close the transaction properly;
			if(!pointHasBeenVisited(thisTarget.getDistanceForCompletion(), thisTarget.getPlan(), n, session)){
				result.setStatus(Status.INCOMPLETE);
				break;
			}
		}
		return result;
	}




	private PlanTargetStatus computeAdministrativePlanTarget(AdministrativePlanTarget target) {
		if (target.getStatus()){
			return new PlanTargetStatus(Status.COMPLETE);
		}else{
			return new PlanTargetStatus(Status.INCOMPLETE);
		}
	}
	
	private PlanTargetStatus computeNumericPlanTarget(NumericPlanTarget target, Session session) {
		Double total = -9999d;
		NumericPlanTarget thisTarget = null;
		thisTarget = (NumericPlanTarget) session.load(
				NumericPlanTarget.class, target.getUuid());
		if (thisTarget == null) {
			return new PlanTargetStatus(Status.INCOMPLETE);
		}
		total = calculateTargetStatusValue(thisTarget.getPlan(),
				thisTarget.getType());
		
		String completeMsg = Status.COMPLETE.guiName + " (" + total + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		String incompleteMsg = Status.INCOMPLETE.guiName + " (" + total + ")"; //$NON-NLS-1$ //$NON-NLS-2$

		boolean complete = false;

		Operator op = thisTarget.getOp();
		double value = thisTarget.getValue();
		if (op == Operator.EQUAL) {
			if (total == value) {
				complete = true;
			} else {
				complete = false;
			}
		} else if (op == Operator.GREATER) {
			if (total > value) {
				complete = true;
			} else {
				complete = false;
			}
		} else if (op == Operator.LESS) {
			if (total < value) {
				complete = true;
			} else {
				incompleteMsg = Status.INCOMPLETE.guiName + " (" + total + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				complete = false;
			}
		} else if (op == Operator.NOEQUAL) {
			if (total != value) {
				complete = true;
			} else {
				complete = false;
			}
		}

		if (complete) {
			return new PlanTargetStatus(Status.COMPLETE, completeMsg);
		} else {
			return new PlanTargetStatus(Status.INCOMPLETE, incompleteMsg);
		}

	}

	private Double calculateTargetStatusValue(Plan plan, TargetType type) {
		List<Plan> children = plan.getChildren();
		Double total = PlanHibernateManager.getTargetTotalValue(type, plan);
		for (Plan p : children) {
			total += calculateTargetStatusValue(p, type);
		}
		return total;
	}


	private boolean pointHasBeenVisited(int distanceForCompletion, Plan plan, SpatialPlanTargetPoint spt, Session session) {
		GeometryFactory fact = new GeometryFactory();
		Coordinate c = new Coordinate(spt.getX(), spt.getY());
        Point point = fact.createPoint(c);
        
		for(Track t : PlanHibernateManager.getAllTracks(plan, session) ){
			if(GeometryUtils.distanceInMeters(t.getLineString(), point) <= distanceForCompletion ){
				return true;
			}
		}
		
		for(Plan x: plan.getChildren() ){
			if(pointHasBeenVisited(distanceForCompletion, x, spt, session) ){
				return true;
			}
		}
		return false;
	}
}
