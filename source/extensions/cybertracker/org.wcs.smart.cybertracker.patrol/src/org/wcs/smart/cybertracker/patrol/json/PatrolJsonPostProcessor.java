/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.patrol.json;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.cybertracker.importer.json.IJsonPostProcessor;
import org.wcs.smart.cybertracker.importer.json.JsonCtParser;
import org.wcs.smart.cybertracker.patrol.model.CtPatrolLink;

/**
 * Removes all ct to SMART links where the
 * patrols are older than 6 months
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class PatrolJsonPostProcessor implements IJsonPostProcessor {

	@Override
	public void postProcess(Session session) {
		//clean up all links that are associated with a patrol that
		//is older than two months old
		StringBuilder hql = new StringBuilder();
		hql.append( "FROM CtPatrolLink l " ); //$NON-NLS-1$
		hql.append( "WHERE " ); //$NON-NLS-1$
		hql.append( "l.patrolLeg.patrol.endDate < :now " ); //$NON-NLS-1$
					
		List<CtPatrolLink> links = session.createQuery(hql.toString(), CtPatrolLink.class)
				.setParameter("now", LocalDate.now().minusMonths(JsonCtParser.CLEANUP_MONTHS) ) //$NON-NLS-1$
				.list();
					
		for (CtPatrolLink l : links) {
			session.delete(l);
		}
		
	}

}
