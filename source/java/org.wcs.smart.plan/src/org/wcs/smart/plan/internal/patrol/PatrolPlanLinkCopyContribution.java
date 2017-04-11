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
package org.wcs.smart.plan.internal.patrol;

import java.util.ArrayList;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.patrol.IPatrolEditContribution;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.plan.model.PatrolPlan;

/*
 * contribution to Patrol extension points
 * This get run when you split or merge patrols to update the relationships to plans.
 */

public class PatrolPlanLinkCopyContribution implements IPatrolEditContribution {

	@SuppressWarnings("unchecked")
	@Override
	public void splitPatrol(Session s, Patrol originalPatrol, Patrol newPatrol) {
		Criteria c = s.createCriteria(PatrolPlan.class).add(Restrictions.eq("id.patrol", originalPatrol)); //$NON-NLS-1$
		for(PatrolPlan pp : ((ArrayList<PatrolPlan>)c.list())){
			PatrolPlan newPp = new PatrolPlan();
			newPp.setPatrol(newPatrol);
			newPp.setPlan(pp.getPlan());
			s.saveOrUpdate(newPp);
		}
	}

}
