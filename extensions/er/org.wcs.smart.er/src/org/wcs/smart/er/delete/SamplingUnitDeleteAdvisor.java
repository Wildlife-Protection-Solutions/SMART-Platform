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
package org.wcs.smart.er.delete;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyWaypoint;

/**
 * Delete sampling unit advisor.
 * 
 * @author Emily
 *
 */
public class SamplingUnitDeleteAdvisor implements IDeleteAdvisor {

	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof SamplingUnit)){
			return Messages.MissionAttributeListItemDeleteAdvisor_InvalidObject;
		}
		SamplingUnit su = (SamplingUnit)object;
		//check survey waypoints for references
		Long wpCnt = (Long) session.createCriteria(SurveyWaypoint.class)
				.add(Restrictions.eq("samplingUnit", su)) //$NON-NLS-1$
				.setProjection(Projections.rowCount())
				.list().get(0);

		if (wpCnt > 0){
			return MessageFormat.format(Messages.SamplingUnitDeleteAdvisor_WaypointError, new Object[]{wpCnt});
		}
		
		//check mission tracks for references
		Long suCnt = (Long) session.createCriteria(MissionTrack.class)
				.add(Restrictions.eq("samplingUnit", su)) //$NON-NLS-1$
				.setProjection(Projections.rowCount())
				.list().get(0);

		if (suCnt > 0){
			return MessageFormat.format(Messages.SamplingUnitDeleteAdvisor_TrackError, new Object[]{suCnt});
		}
		
		return null;
	}

}
