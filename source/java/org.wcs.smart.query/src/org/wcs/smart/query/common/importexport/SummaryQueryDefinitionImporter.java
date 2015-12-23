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
package org.wcs.smart.query.common.importexport;

import java.util.ArrayList;
import java.util.HashMap;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.importexport.IQueryImporter;
import org.wcs.smart.query.importexport.QueryImportEngine;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.summary.SumQueryDefinition;
import org.wcs.smart.query.ui.importexport.ImportQueryUtil;
import org.wcs.smart.query.xml.model.QueryPart;
import org.wcs.smart.query.xml.model.QueryType;
import org.wcs.smart.query.xml.model.UuidItemType;

/**
 * Query importer for importing summary query definitions
 * @author egouge
 * @since 1.0.0
 */
public abstract class SummaryQueryDefinitionImporter implements IQueryImporter{

	/*
	 * list of warnings generated during import process
	 */
	protected ArrayList<String> warnings = new ArrayList<String>();
	
	
	/**
	 * Imports the given definition file.
	 * 
	 * <p>
	 * The returned query does not have the folder or shared value
	 * set.  This must be set by the calling function based
	 * on where the query is to be saved.
	 * </p> 
	 * 
	 * @param file the query definition xml file to import
	 * @return the imported query
	 * @throws Exception if the file cannot be converted to a query.
	 * 
	 */
	@Override
	public Query importQuery(QueryType xmlQuery, ConservationArea ca) throws Exception{
		warnings.clear();
		
		String langCode = xmlQuery.getLanguage();
		SummaryQuery summaryQuery = createQuery();
		QueryImportEngine.importNames(summaryQuery, xmlQuery);
		
		HashMap<String, UuidItemType> uuidLookup = new HashMap<String, UuidItemType>();
		for (UuidItemType type : xmlQuery.getUuiditem()){
			uuidLookup.put(type.getUuid(), type);
		}
		
		for (QueryPart part : xmlQuery.getQueryPart()) {
			
			if (part.getKey().equals("definition")) { //$NON-NLS-1$
				if (part.getValue() != null && part.getValue().length() > 0) {
					
					summaryQuery.setQuery(part.getValue());
					Session session = HibernateManager.openSession();
					session.beginTransaction();
					try {
						SumQueryDefinition sumDef = summaryQuery.getQueryDefinition();
						validateQuery(ca, sumDef, langCode, uuidLookup, session);					
						summaryQuery.setQuery(sumDef.asQuery(), sumDef);
					} finally {
						session.getTransaction().rollback();
						session.close();
					}
				}
			}
		}
		
		
		summaryQuery.setConservationArea(ca);
		summaryQuery.setOwner(ImportQueryUtil.findEmployee(ca));
		
		summaryQuery.setConservationAreaFilter((new ConservationAreaFilter(true, ca)).asString());
		
		return summaryQuery;
	}
	
	/**
	 * @return warnings generated during import process
	 */
	@Override
	public ArrayList<String> getWarnings(){
		return this.warnings;
	}
	
	/**
	 * Validates the imported query definition.  Updates
	 * warning array as required
	 */
	protected abstract void validateQuery(ConservationArea caImport, SumQueryDefinition sumDef, String langCode, HashMap<String, UuidItemType> uuidLookup, Session session) throws Exception;
	
	@Override
	public abstract boolean canImport(IQueryType qt);
	
	/**
	 * Creates a new summary query;
	 * @param at
	 * @return
	 */
	public abstract SummaryQuery createQuery();
	
}
