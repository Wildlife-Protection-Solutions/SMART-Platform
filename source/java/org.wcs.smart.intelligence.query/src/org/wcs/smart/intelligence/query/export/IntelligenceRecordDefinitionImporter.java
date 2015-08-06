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
package org.wcs.smart.intelligence.query.export;

import java.util.HashMap;

import org.wcs.smart.intelligence.query.IntelligenceQueryFactory;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordQuery;
import org.wcs.smart.query.common.importexport.SimpleQueryDefinitionImporter;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.xml.model.UuidItemType;

/**
 * Intelligence Record query definition importer.  Imports
 * intelligence record queries from xml format.
 * @author Emily
 *
 */
public class IntelligenceRecordDefinitionImporter extends SimpleQueryDefinitionImporter {

	
	@Override
	protected String processDefinition(String queryDef, String langCode,
			HashMap<String, UuidItemType> uuidLookup) throws Exception {
		return queryDef;
	}

	@Override
	public boolean canImport(IQueryType qt) {
		if (IntelligenceRecordQuery.KEY.equals(qt.getKey())){
			return true;
		}
		return false;
	}

	@Override
	protected SimpleQuery createQuery(IQueryType qt) {
		if (IntelligenceRecordQuery.KEY.equals(qt.getKey())){
			return IntelligenceQueryFactory.createIntelligenceRecordQuery();
		}
		return null;
	}

}
