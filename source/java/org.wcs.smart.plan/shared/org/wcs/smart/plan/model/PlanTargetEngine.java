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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.plan.model.NumericPlanTarget.Operator;
import org.wcs.smart.plan.model.NumericPlanTarget.TargetType;
import org.wcs.smart.plan.model.PlanTargetStatus.Status;
import org.wcs.smart.util.GeometryUtils;

import jakarta.persistence.Tuple;

/**
 * Computation engine for computing the status of
 * plan targets.
 * 
 * @author Emily
 *
 */
public class PlanTargetEngine {

	private Locale l;
	
	public PlanTargetEngine(){
		this(Locale.getDefault());
	}
	
	public PlanTargetEngine(Locale l){
		this.l = l;
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

		thisTarget = session.get(SpatialPlanTarget.class, target.getUuid());
		if (thisTarget == null) {
			return new PlanTargetStatus(Status.INCOMPLETE);
		}
		List<SpatialPlanTargetPoint> points= target.getPoints();
		for(SpatialPlanTargetPoint n : points){
			//as soon as we see one point was not met, just break so we close the transaction properly;
			try{
				if(!pointHasBeenVisited(thisTarget.getDistanceForCompletion(), thisTarget.getPlan(), n, session)){
					result.setStatus(Status.INCOMPLETE);
					break;
				}
			}catch(Exception e){
				result.setStatus(Status.INCOMPLETE);
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
		thisTarget = session.get(NumericPlanTarget.class, target.getUuid());
		if (thisTarget == null) {
			return new PlanTargetStatus(Status.INCOMPLETE);
		}
		total = calculateTargetStatusValue(thisTarget.getPlan(),
				thisTarget.getType(), session);
		
		String completeMsg = Status.COMPLETE.getGuiName(l) + " (" + total + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		String incompleteMsg = Status.INCOMPLETE.getGuiName(l) + " (" + total + ")"; //$NON-NLS-1$ //$NON-NLS-2$

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
				incompleteMsg = Status.INCOMPLETE.getGuiName(l) + " (" + total + ")"; //$NON-NLS-1$ //$NON-NLS-2$
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

	private Double calculateTargetStatusValue(Plan plan, TargetType type, Session session) {
		List<Plan> children = plan.getChildren();
		Double total = getTargetTotalValue(type, plan, session);
		for (Plan p : children) {
			total += calculateTargetStatusValue(p, type, session);
		}
		return total;
	}


	private boolean pointHasBeenVisited(int distanceForCompletion, Plan plan, SpatialPlanTargetPoint spt, Session session) throws Exception {
		GeometryFactory fact = GeometryFactoryProvider.getFactory();
		Coordinate c = new Coordinate(spt.getX(), spt.getY());
        Point point = fact.createPoint(c);
        
		for(Track t : getAllTracks(plan, session) ){
			for (LineString ls : t.getLineStrings()) {
				if(GeometryUtils.distanceInMeters(ls, point) <= distanceForCompletion ){
					return true;
				}
			}
		}
		
		for(Plan x: plan.getChildren() ){
			if(pointHasBeenVisited(distanceForCompletion, x, spt, session) ){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns all tracks directory associated with a given plan.
	 *  
	 * @param plan the plan you want all the tracks from
	 * @param session the session/transaction that is already open and being used with this plan
	 * @return a list of tracks associated with the plan.
	 */
	private List<Track> getAllTracks(Plan plan, Session session){
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT pld.tracks"); //$NON-NLS-1$
		sql.append(" FROM PatrolPlan pp "); //$NON-NLS-1$
		sql.append(" JOIN pp.id.patrol.legs pl"); //$NON-NLS-1$
		sql.append(" Join pl.patrolLegDays as pld "); //$NON-NLS-1$			
		sql.append(" WHERE pp.id.plan  =:uuid ");//$NON-NLS-1$
		
		return session.createQuery(sql.toString(), Track.class)
			.setParameter("uuid", plan) //$NON-NLS-1$
			.list();
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
	public static Double getTargetTotalValue(TargetType type, Plan plan, Session session) {
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

			List<Tuple> list = session.createQuery(sql.toString(), Tuple.class)
					.setParameter("uuid", plan) //$NON-NLS-1$
					.list();
			targetTotal = (Double) list.get(0).get(0);

		}else if (type == TargetType.PATROL_DAYS) {
			sql.append(" SELECT "); //$NON-NLS-1$
			sql.append(" p.endDate, p.startDate "); //$NON-NLS-1$
			sql.append(" FROM PatrolPlan pp "); //$NON-NLS-1$
			sql.append(" JOIN pp.id.patrol p"); //$NON-NLS-1$
			sql.append(" WHERE pp.id.plan  =:uuid "); //$NON-NLS-1$

			List<Tuple> list = session.createQuery(sql.toString(), Tuple.class)
					.setParameter("uuid", plan) //$NON-NLS-1$
					.list();

			for (Tuple row : list) {
				LocalDate t1 = (LocalDate)row.get(0);
		        LocalDate t2 = (LocalDate)row.get(1);
		        targetTotal += (ChronoUnit.DAYS.between(t2, t1) + 1); 		        
		     }
		}else if (type == TargetType.PATROL_HOURS) {
			sql.append(" SELECT "); //$NON-NLS-1$
			sql.append(" pld.endTime, pld.startTime "); //$NON-NLS-1$
			sql.append(" FROM PatrolPlan pp "); //$NON-NLS-1$
			sql.append(" JOIN pp.id.patrol.legs pl"); //$NON-NLS-1$
			sql.append(" Join pl.patrolLegDays as pld "); //$NON-NLS-1$			
			sql.append(" WHERE pp.id.plan  =:uuid "); //$NON-NLS-1$
			
			List<Tuple> list = session.createQuery(sql.toString(), Tuple.class)
				.setParameter("uuid", plan) //$NON-NLS-1$
				.list();
					
			for (Tuple row : list) {
				LocalTime t1 = (LocalTime)row.get(0);
				LocalTime t2 = (LocalTime)row.get(1);
				Long milDiff = ChronoUnit.MILLIS.between(t2, t1); 
				targetTotal += (milDiff / 3_600_000.0); 
		    }
		    

		}else if (type == TargetType.PATROL_MANHOURS) {
			sql.append(" SELECT "); //$NON-NLS-1$
			sql.append(" pld.endTime, pld.startTime, m.isLeader "); //$NON-NLS-1$
			sql.append(" FROM PatrolPlan pp "); //$NON-NLS-1$
			sql.append(" JOIN pp.id.patrol.legs pl"); //$NON-NLS-1$
			sql.append(" JOIN pl.members m"); //$NON-NLS-1$
			sql.append(" Join pl.patrolLegDays as pld "); //$NON-NLS-1$			
			sql.append(" WHERE pp.id.plan  =:uuid "); //$NON-NLS-1$
			

			List<Tuple> list = session.createQuery(sql.toString(), Tuple.class)
					.setParameter("uuid", plan) //$NON-NLS-1$
					.list();
			
			for (Tuple row : list) {
				LocalTime t1 = (LocalTime)row.get(0);
		        LocalTime t2 = (LocalTime)row.get(1);
		          
		        Long milDiff = ChronoUnit.MILLIS.between(t2, t1);
		        targetTotal += (milDiff / 3_600_000.0); 
		    }

		} else {
			return 1.0;
		}
		
		if(targetTotal== null)targetTotal = 0.0;
		return targetTotal;

	}
}
