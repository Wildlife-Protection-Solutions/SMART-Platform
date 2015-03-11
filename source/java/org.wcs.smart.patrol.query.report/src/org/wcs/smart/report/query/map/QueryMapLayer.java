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
package org.wcs.smart.report.query.map;

import org.locationtech.udig.catalog.IService;
import org.wcs.smart.patrol.query.map.udig.QueryServiceFactory;
import org.wcs.smart.patrol.query.model.types.PatrolGridQueryType;
import org.wcs.smart.patrol.query.model.types.PatrolObservationQueryType;
import org.wcs.smart.patrol.query.model.types.PatrolQueryType;
import org.wcs.smart.patrol.query.model.types.PatrolWaypointQueryType;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.report.birt.query.map.AbstractQueryMapLayer;

/**
 * SMART Query Map Layer
 * 
 * @author Emily
 *
 */
public class QueryMapLayer extends AbstractQueryMapLayer {

	@Override
	public boolean canAddToMap(String queryTypeKey) {
		if (queryTypeKey.equals(PatrolGridQueryType.KEY) ||
				queryTypeKey.equals(PatrolQueryType.KEY) ||
				queryTypeKey.equals(PatrolObservationQueryType.KEY) ||
				queryTypeKey.equals(PatrolWaypointQueryType.KEY)){
			return true;
		}
		//also test the deprecated query types
		if (QueryTypeManager.getInstance().findDeprecatedQueryType(queryTypeKey) != null){
			return true;
		}
		return false;
	}

	@Override
	public IService createQueryService(Query query) {
		return  QueryServiceFactory.generateQueryService(query);
	}

}
