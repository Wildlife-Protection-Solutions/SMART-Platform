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
package org.wcs.smart.patrol.internal.advisors;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.patrol.model.Patrol;


/**
 * STations can be deleted if they are not used
 * in any patrols.
 * 
 * @author Emily
 *
 */
public class StationDeleteAdvisor implements IDeleteAdvisor {

	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof Station)){
			return "Object not of type Station. Can not delete.";
		}
		
		Long cnt = (Long) session.createCriteria(Patrol.class).add(Restrictions.eq("station", object)).setProjection(Projections.rowCount()).uniqueResult();
		if (cnt == 0){
			return null;
		}else{
			return cnt + " patrols are associated with the station " + ((Station)object).getName() + ".  These references must be removed before this station can be deleted.";
		}
	}

}
