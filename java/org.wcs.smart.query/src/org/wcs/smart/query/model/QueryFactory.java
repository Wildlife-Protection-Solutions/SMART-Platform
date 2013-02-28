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
package org.wcs.smart.query.model;

import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.parser.filter.ConservationAreaFilter;

/**
 * Factory class for creating new queries
 * @author egouge
 *
 */
public class QueryFactory {

	private static void initQuery(Query q, String defaultName){
		if (defaultName == null){
			defaultName = Messages.Query_DefaultQueryName;
		}
		q.setConservationArea(SmartDB.getCurrentConservationArea());
		q.setOwner(SmartDB.getCurrentEmployee());
		q.setFolder(null);
		q.setIsShared(false);
		
		q.updateName(SmartDB.getCurrentLanguage(), defaultName);
		q.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), defaultName);
		q.setName(defaultName);
		
	}
	
	public static ObservationQuery createObservationQuery(){
		ObservationQuery query = new ObservationQuery();
		initQuery(query, null);
		query.setConservationAreaFilter(new ConservationAreaFilter(true));
		return query;
	}
	
	public static PatrolQuery createPatrolQuery(){
		PatrolQuery query = new PatrolQuery();
		initQuery(query, null);
		query.setConservationAreaFilter(new ConservationAreaFilter(true));
		return query;
	}
	
	
	public static GriddedQuery createGriddedQuery(){
		GriddedQuery query = new GriddedQuery();
		initQuery(query, Messages.GriddedQuery_DefaultQueryName);
		query.setConservationAreaFilter(new ConservationAreaFilter(true));
		query.setDateFilter(null);
		return query;
	}
	
	public static SummaryQuery createSummaryQuery(){
		SummaryQuery query = new SummaryQuery();
		initQuery(query, Messages.SummaryQuery_DefaultQueryName);
		
		query.setConservationAreaFilter(new ConservationAreaFilter(true));
		query.setDateFilter(null);
		return query;
	}
}
