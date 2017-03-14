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

package org.wcs.smart.intelligence.patrol;

import java.util.ArrayList;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.intelligence.model.PatrolIntelligence;
import org.wcs.smart.patrol.IPatrolEditContribution;
import org.wcs.smart.patrol.model.Patrol;

/*
 * contribution to Patrol extension points
 * This get run when you split or merge patrols to update the relationships to intel items.
 */

public class PatrolIntelLinkCopyContribution implements IPatrolEditContribution {

	@Override
	public void splitPatrol(Session s, Patrol originalPatrol, Patrol newPatrol) {
		Criteria c = s.createCriteria(PatrolIntelligence.class).add(Restrictions.eq("id.patrol", originalPatrol)); //$NON-NLS-1$ //$NON-NLS-2$
		@SuppressWarnings("unchecked")
		ArrayList<PatrolIntelligence> list = (ArrayList<PatrolIntelligence>)c.list();
		for(PatrolIntelligence pi : list){
			PatrolIntelligence newPp = new PatrolIntelligence();
			newPp.setPatrol(newPatrol);
			newPp.setIntelligence(pi.getIntelligence());
			s.saveOrUpdate(newPp);
		}
	}

}
