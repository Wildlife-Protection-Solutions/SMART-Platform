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
package org.wcs.smart.asset.query.exportimport;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;

import org.hibernate.Session;
import org.wcs.smart.asset.query.model.AssetObservationQuery;
import org.wcs.smart.asset.query.model.AssetQueryFactory;
import org.wcs.smart.asset.query.model.AssetQueryValidator;
import org.wcs.smart.asset.query.model.AssetWaypointQuery;
import org.wcs.smart.asset.query.parser.internal.parser.Parser;
import org.wcs.smart.ca.ConservationArea;
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
public class AssetSimpleQueryDefinitionImporter extends SimpleQueryDefinitionImporter {

	@Override
	public boolean canImport(IQueryType qt) {
		return qt.getKey().equals(AssetObservationQuery.KEY) ||
				qt.getKey().equals(AssetWaypointQuery.KEY);
	}

	@Override
	protected String processDefinition(ConservationArea importCa, String queryDef, String langCode, HashMap<String, UuidItemType> uuidLookup, Session session) throws Exception {
		QueryFilter queryFilter = null;
		
		try(Reader is = new StringReader(queryDef)){
			Parser parser = new Parser(is);
			queryFilter = parser.QueryFilter();
		}

		AssetQueryValidator validator = new AssetQueryValidator(uuidLookup, session, QueryDataModelManager.getManager(importCa), importCa);
		warnings.addAll(validator.validate(queryFilter.getFilter()));

		return queryFilter.asString();
	}

	@Override
	protected SimpleQuery createQuery(IQueryType qt) {
		return (SimpleQuery) AssetQueryFactory.createQuery(qt);
	}
	
}
