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
package org.wcs.smart.patrol.query.exportimport;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;

import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolQueryFactory;
import org.wcs.smart.patrol.query.model.PatrolQueryValidator;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.patrol.query.parser.internal.parser.Parser;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.common.importexport.SimpleQueryDefinitionImporter;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.filter.QueryFilter;
import org.wcs.smart.query.xml.model.UuidItemType;

/**
 * Importer for importing query definition files.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolSimpleQueryDefinitionImporter extends SimpleQueryDefinitionImporter {

	@Override
	public boolean canImport(IQueryType qt) {
		return qt.getKey().equals(PatrolObservationQuery.KEY) ||
				qt.getKey().equals(PatrolWaypointQuery.KEY) || 
				qt.getKey().equals(PatrolQuery.KEY);
	}

	@Override
	protected String processDefinition(String queryDef, String langCode, HashMap<String, UuidItemType> uuidLookup) throws Exception {
		QueryFilter queryFilter = null;
		
		try(InputStream is = new ByteArrayInputStream(queryDef.getBytes())){
			Parser parser = new Parser(is);
			queryFilter = parser.QueryFilter();
		}
		

		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			PatrolQueryValidator validator = new PatrolQueryValidator(langCode, uuidLookup, session, QueryDataModelManager.getInstance(), SmartDB.getCurrentConservationArea());
			warnings.addAll(validator.validate(queryFilter.getFilter()));
		} finally {
			session.getTransaction().rollback();
			session.close();
		}
		return queryFilter.asString();
	}

	@Override
	protected SimpleQuery createQuery(IQueryType qt) {
		return (SimpleQuery) PatrolQueryFactory.createQuery(qt);
	}
	
}
