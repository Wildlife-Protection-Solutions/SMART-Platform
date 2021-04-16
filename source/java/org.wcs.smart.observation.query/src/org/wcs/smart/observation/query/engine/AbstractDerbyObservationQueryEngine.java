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
package org.wcs.smart.observation.query.engine;

import org.wcs.smart.query.common.engine.AbstractQueryEngine;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.FilterType;

/**
 * Query engine for executing 
 * queries using derby.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class AbstractDerbyObservationQueryEngine extends AbstractQueryEngine {

	@Override
	public IFilterProcessor getFilterProcessor(FilterType filterType, String queryDataTable, Query query) {
		if (filterType == FilterType.OBSERVATION){
			return new FilterProcessor(queryDataTable, this, query);
		}else if (filterType == FilterType.GROUP){
			return new WaypointGroupFilterProcessor(queryDataTable, this, query);
		}else{
			return new WaypointFilterProcessor(queryDataTable, this, query);
		}

	}

}
