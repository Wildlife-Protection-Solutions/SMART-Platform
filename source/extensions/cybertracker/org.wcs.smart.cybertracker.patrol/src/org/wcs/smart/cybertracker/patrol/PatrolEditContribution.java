/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.patrol;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.cybertracker.patrol.model.CtPatrolLink;
import org.wcs.smart.patrol.IPatrolEditContribution;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
/**
 * Contribution for maintaining SMARTMobile links when patrols are modified
 * 
 * @since 8.1.0
 */
public class PatrolEditContribution implements IPatrolEditContribution {

	public PatrolEditContribution() {
	}

	@Override
	public void splitPatrol(Session s, Patrol originalPatrol, Patrol newPatrol) {
	}

	/**
	 * creating a new patrol leg with the details from the current patrol
	 * leg
	 * 
	 * @param currentLeg
	 * @param toLeg
	 */
	//called when patrols are merged/split
	public void mergePatrolMovePatrolLeg(PatrolLeg currentLeg, PatrolLeg toLeg, Session session) {
		List<CtPatrolLink> links = session.createQuery("FROM CtPatrolLink where patrolLeg = :leg", CtPatrolLink.class) //$NON-NLS-1$
				.setParameter("leg", currentLeg) //$NON-NLS-1$
				.list();
		for (CtPatrolLink link : links) {
			link.setPatrolLeg(toLeg);
		}
		
	}
}
