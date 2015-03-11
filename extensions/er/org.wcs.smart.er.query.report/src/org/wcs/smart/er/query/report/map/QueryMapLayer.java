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
package org.wcs.smart.er.query.report.map;

import org.locationtech.udig.catalog.IService;
import org.wcs.smart.er.query.map.udig.QueryServiceFactory;
import org.wcs.smart.er.query.model.MissionQueryType;
import org.wcs.smart.er.query.model.MissionTrackQueryType;
import org.wcs.smart.er.query.model.SurveyGridQueryType;
import org.wcs.smart.er.query.model.SurveyObservationQueryType;
import org.wcs.smart.er.query.model.SurveyWaypointQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.report.birt.query.map.AbstractQueryMapLayer;

/**
 * SMART query map layer implementation for survey queries
 * 
 * @author Emily
 *
 */
public class QueryMapLayer extends AbstractQueryMapLayer{

	@Override
	public boolean canAddToMap(String queryTypeKey) {
		if (queryTypeKey.equals(SurveyGridQueryType.KEY) ||
				queryTypeKey.equals(MissionQueryType.KEY) ||
				queryTypeKey.equals(SurveyObservationQueryType.KEY) ||
				queryTypeKey.equals(MissionTrackQueryType.KEY) ||
				queryTypeKey.equals(SurveyWaypointQueryType.KEY)){
			return true;
		}
		return false;
	}


	@Override
	public IService createQueryService(Query query) {
		return QueryServiceFactory.generateQueryService(query);
	}

}
