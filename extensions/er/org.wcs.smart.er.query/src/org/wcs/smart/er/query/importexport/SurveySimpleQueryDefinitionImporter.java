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
package org.wcs.smart.er.query.importexport;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.er.query.internal.parser.Parser;
import org.wcs.smart.er.query.model.ISurveyQuery;
import org.wcs.smart.er.query.model.MissionQuery;
import org.wcs.smart.er.query.model.MissionTrackQuery;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.er.query.model.SurveyQueryFactory;
import org.wcs.smart.er.query.model.SurveyWaypointQuery;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.common.importexport.SimpleQueryDefinitionImporter;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.QueryFilter;
import org.wcs.smart.query.xml.model.QueryPart;
import org.wcs.smart.query.xml.model.QueryType;
import org.wcs.smart.query.xml.model.UuidItemType;

/**
 * Importer for importing query definition files.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveySimpleQueryDefinitionImporter extends SimpleQueryDefinitionImporter {

	private String qTypeInternal;
	
	@Override
	public boolean canImport(IQueryType qt) {
		return qt.getKey().equals(SurveyObservationQuery.KEY) ||
				qt.getKey().equals(SurveyWaypointQuery.KEY) ||
				qt.getKey().equals(MissionQuery.KEY) ||
				qt.getKey().equals(MissionTrackQuery.KEY);
	}

	@Override
	protected String processDefinition(ConservationArea importCa, String queryDef, String langCode, HashMap<String, UuidItemType> uuidLookup) throws Exception {
		
		if (qTypeInternal.equals(MissionTrackQuery.KEY)){
			IFilter filter = null;
			try(InputStream is = new ByteArrayInputStream(queryDef.getBytes())){
				Parser parser = new Parser(is);
				filter = parser.ExpressionPart();
			}
			
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try {
				SurveyQueryValidator validator = new SurveyQueryValidator(importCa, uuidLookup, session);
				warnings.addAll(validator.validate(filter));
			} finally {
				session.getTransaction().rollback();
				session.close();
			}
			return filter.asString();
			
		}else{
			QueryFilter queryFilter = null;
			try(InputStream is = new ByteArrayInputStream(queryDef.getBytes())){
				Parser parser = new Parser(is);
				queryFilter = parser.QueryFilter();
			}
			
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try {
				SurveyQueryValidator validator = new SurveyQueryValidator(importCa, uuidLookup, session);
				warnings.addAll(validator.validate(queryFilter.getFilter()));
			} finally {
				session.getTransaction().rollback();
				session.close();
			}
			return queryFilter.asString();

		}
	}

	@Override
	public Query importQuery(QueryType qt, ConservationArea importCa) throws Exception{
		qTypeInternal = qt.getQueryType();
		Query query = super.importQuery(qt, importCa);
		for (QueryPart part : qt.getQueryPart()) {
			if (part.getKey().equals("surveyDesignFilter")){ //$NON-NLS-1$
				((ISurveyQuery)query).setSurveyDesign(part.getValue());
			}
		}
		return query;
	}
	
	@Override
	protected SimpleQuery createQuery(IQueryType qt) {
		return (SimpleQuery) SurveyQueryFactory.createQuery(qt);
	}
	
}
