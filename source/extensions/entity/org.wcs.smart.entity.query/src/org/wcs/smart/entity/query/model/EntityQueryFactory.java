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
package org.wcs.smart.entity.query.model;

import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.entity.query.model.type.EntityGridQueryType;
import org.wcs.smart.entity.query.model.type.EntityObservationQueryType;
import org.wcs.smart.entity.query.model.type.EntitySummaryQueryType;
import org.wcs.smart.entity.query.model.type.EntityWaypointQueryType;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;


/**
 * Factory class for creating new queries
 * @author egouge
 *
 */
public class EntityQueryFactory {

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
	
	/**
	 * Creates a new query with none of the query fields initialized.
	 * @param querytype
	 * @return
	 */
	public static Query createBlankQuery(IQueryType querytype){
		if (querytype.getClass().equals(EntityObservationQueryType.class)){
			return new EntityObservationQuery();
		}else if (querytype.getClass().equals(EntityGridQueryType.class)){
			return new EntityGriddedQuery();
		}else if (querytype.getClass().equals(EntityWaypointQueryType.class)){
			return new EntityWaypointQuery();
		}else if (querytype.getClass().equals(EntitySummaryQueryType.class)){
			return new EntitySummaryQuery();
		}
		return null;
	}
	
	/**
	 * Creates a new query with the various known (conservation area etc)
	 *  query fields initialized.
	 * 
	 * @param querytype
	 * @return
	 */
	public static Query createQuery(IQueryType querytype){
		if (querytype.getClass().equals(EntityObservationQueryType.class)){
			return createObservationQuery();
		}else if (querytype.getClass().equals(EntityGridQueryType.class)){
			return createGriddedQuery();
		}else if (querytype.getClass().equals(EntityWaypointQueryType.class)){
			return createWaypointQuery();
		}else if (querytype.getClass().equals(EntitySummaryQueryType.class)){
			createSummaryQuery();
		}
		return null;
	}
	
	public static EntityObservationQuery createObservationQuery(){
		EntityObservationQuery query = new EntityObservationQuery();
		initQuery(query, null);
		query.setConservationAreaFilter((new ConservationAreaFilter(true, SmartDB.getCurrentConservationArea())).asString());
		return query;
	}
	
	public static EntityWaypointQuery createWaypointQuery(){
		EntityWaypointQuery query = new EntityWaypointQuery();
		initQuery(query, null);
		query.setConservationAreaFilter((new ConservationAreaFilter(true, SmartDB.getCurrentConservationArea())).asString());
		return query;
	}
	
		
	public static EntityGriddedQuery createGriddedQuery(){
		EntityGriddedQuery query = new EntityGriddedQuery();
		initQuery(query, Messages.GriddedQuery_DefaultQueryName);
		query.setConservationAreaFilter((new ConservationAreaFilter(true, SmartDB.getCurrentConservationArea())).asString());
		query.setDateFilter(null);
		return query;
	}
	
	public static EntitySummaryQuery createSummaryQuery(){
		EntitySummaryQuery query = new EntitySummaryQuery();
		initQuery(query, Messages.SummaryQuery_DefaultQueryName);
		
		query.setConservationAreaFilter((new ConservationAreaFilter(true, SmartDB.getCurrentConservationArea())).asString());
		query.setDateFilter(null);
		return query;
	}
}
