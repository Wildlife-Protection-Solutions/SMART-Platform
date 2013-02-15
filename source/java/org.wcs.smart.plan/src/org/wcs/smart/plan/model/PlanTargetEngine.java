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
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.NumericPlanTarget.Operator;
import org.wcs.smart.plan.model.NumericPlanTarget.TargetType;
import org.wcs.smart.plan.model.PlanTargetStatus.Status;

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
			return computeNumericPlanTarget((NumericPlanTarget) target);
		}else if (target instanceof SpatialPlanTarget){
			//TODO:
			return new PlanTargetStatus(Status.COMPLETE);
		}else{
			//unknown
			return new PlanTargetStatus(Status.INCOMPLETE);
		}
	}
	
	
	private PlanTargetStatus computeAdministrativePlanTarget(AdministrativePlanTarget target) {
		if (target.getStatus()){
			return new PlanTargetStatus(Status.COMPLETE);
		}else{
			return new PlanTargetStatus(Status.INCOMPLETE);
		}
	}
	
	private PlanTargetStatus computeNumericPlanTarget(NumericPlanTarget target) {
		Double total = -9999d;
		NumericPlanTarget thisTarget = null;
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			thisTarget = (NumericPlanTarget) session.load(
					NumericPlanTarget.class, target.getUuid());
			if (thisTarget == null) {
				return new PlanTargetStatus(Status.INCOMPLETE);
			}
			total = calculateTargetStatusValue(thisTarget.getPlan(),
					thisTarget.getType());
			session.getTransaction().rollback();
		} finally {
			session.close();

		}
		
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
				incompleteMsg = Messages.PlanTargetEngine_Missed_Message + " (" + total + ")"; //$NON-NLS-1$ //$NON-NLS-2$
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

}
