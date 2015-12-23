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

import java.util.HashMap;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.er.query.model.ISurveyQuery;
import org.wcs.smart.er.query.model.SurveyGriddedQuery;
import org.wcs.smart.er.query.model.SurveyQueryFactory;
import org.wcs.smart.query.common.importexport.GriddedQueryDefinitionImporter;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.summary.GridQueryDefinition;
import org.wcs.smart.query.xml.model.QueryPart;
import org.wcs.smart.query.xml.model.QueryType;
import org.wcs.smart.query.xml.model.UuidItemType;

/**
 * Query importer for importing gridded queries
 * @author Emily
 *
 */
public class SurveyGridQueryDefinitionImporter extends GriddedQueryDefinitionImporter{

	@Override
	public boolean canImport(IQueryType qt) {
		return qt.getKey().equals(SurveyGriddedQuery.KEY);
	}

	@Override
	protected GriddedQuery createQuery(String qtype) {
		return SurveyQueryFactory.createGriddedQuery();
	}

	@Override
	protected void validateQuery(ConservationArea caImport, GridQueryDefinition def, String langCode,
			HashMap<String, UuidItemType> uuidLookup, Session session) throws Exception {
		
		SurveyQueryValidator validator = new SurveyQueryValidator(caImport, uuidLookup, session);
		if (def.getValueFilter() != null){
			warnings.addAll(validator.validate(def.getValueFilter().getFilter()));
		}
		
		if (def.getRateFilter() != null){
			warnings.addAll(validator.validate(def.getRateFilter().getFilter()));
		}
		
		//process value items
		warnings.addAll(validator.validate(def.getValuePart()));
		
	}
	
	@Override
	public Query importQuery(QueryType qt, ConservationArea caImport) throws Exception{
		Query query = super.importQuery(qt, caImport);
		for (QueryPart part : qt.getQueryPart()) {
			if (part.getKey().equals("surveyDesignFilter")){ //$NON-NLS-1$
				((ISurveyQuery)query).setSurveyDesign(part.getValue());
			}
		}
		return query;
	}
	
}