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
package org.wcs.smart.entity.query.exportimport;

import java.util.HashMap;

import org.hibernate.Session;
import org.wcs.smart.entity.query.model.EntityGriddedQuery;
import org.wcs.smart.entity.query.model.EntityQueryFactory;
import org.wcs.smart.query.common.importexport.GriddedQueryDefinitionImporter;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.summary.GridQueryDefinition;
import org.wcs.smart.query.xml.model.UuidItemType;

/**
 * Query importer for importing gridded queries
 * @author Emily
 *
 */
public class EntityGriddedQueryDefImporter extends GriddedQueryDefinitionImporter{

	@Override
	public boolean canImport(IQueryType qt) {
		return qt.getKey().equals(EntityGriddedQuery.KEY);
	}

	@Override
	protected GriddedQuery createQuery(String qtype) {
		return EntityQueryFactory.createGriddedQuery();
	}

	/**
	 * There is nothing special to validate in these queries.  
	 */
	@Override
	protected void validateQuery(GridQueryDefinition def, String langCode,
			HashMap<String, UuidItemType> uuidLookup, Session session) throws Exception {

		EntityQueryValidator validator = new EntityQueryValidator(session);
		if (def.getValueFilter() != null){
			warnings.addAll(validator.validate(def.getValueFilter().getFilter()));
		}
		
		if (def.getRateFilter() != null){
			warnings.addAll(validator.validate(def.getRateFilter().getFilter()));
		}
		
		//process value items
		warnings.addAll(validator.validate(def.getValuePart()));
		
	}
	
	
}