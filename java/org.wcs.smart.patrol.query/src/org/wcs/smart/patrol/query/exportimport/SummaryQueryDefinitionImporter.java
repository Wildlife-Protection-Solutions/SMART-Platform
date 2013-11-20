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

import java.util.ArrayList;
import java.util.HashMap;

import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.query.model.PatrolQueryFactory;
import org.wcs.smart.patrol.query.model.SummaryQuery;
import org.wcs.smart.patrol.query.model.types.PatrolSummaryQueryType;
import org.wcs.smart.patrol.query.parser.PatrolQueryValidator;
import org.wcs.smart.query.importexport.IQueryImporter;
import org.wcs.smart.query.importexport.QueryImportEngine;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.SumQueryDefinition;
import org.wcs.smart.query.xml.model.QueryPart;
import org.wcs.smart.query.xml.model.QueryType;
import org.wcs.smart.query.xml.model.UuidItemType;

/**
 * Query importer for importing summary query definitions
 * @author egouge
 * @since 1.0.0
 */
public class SummaryQueryDefinitionImporter implements IQueryImporter{

	/*
	 * list of warnings generated during import process
	 */
	private ArrayList<String> warnings = new ArrayList<String>();
	
	
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
	public Query importQuery(QueryType qt) throws Exception{
		warnings.clear();
		
		String langCode = qt.getLanguage();
		SummaryQuery summaryQuery = PatrolQueryFactory.createSummaryQuery();
		QueryImportEngine.importNames(summaryQuery, qt);
		
		HashMap<String, UuidItemType> uuidLookup = new HashMap<String, UuidItemType>();
		for (UuidItemType type : qt.getUuiditem()){
			uuidLookup.put(type.getUuid(), type);
		}
		
		for (QueryPart part : qt.getQueryPart()) {
			
			if (part.getKey().equals("definition")) { //$NON-NLS-1$
				if (part.getValue() != null && part.getValue().length() > 0) {
					
					summaryQuery.setQuery(part.getValue());
					Session session = HibernateManager.openSession();
					session.beginTransaction();
					try {
						SumQueryDefinition sumDef = summaryQuery.getQueryDefinition();
						PatrolQueryValidator validator = new PatrolQueryValidator(langCode, uuidLookup, session);
						if (sumDef.getValueFilter() != null ){
							warnings.addAll(validator.validate(sumDef.getValueFilter().getFilter()));
						}
						if (sumDef.getRateFilter() != null){
							warnings.addAll(validator.validate(sumDef.getRateFilter().getFilter()));
						}
						//process value items
						for (IValueItem item: sumDef.getValuePart().getValueItems()){
							warnings.addAll(validator.validate(item));
						}
						
						//process group by 
						for (IGroupBy gbpart: sumDef.getColumnGroupByPart().getGroupBys()){
							warnings.addAll(validator.validate(gbpart));
						}		
						for (IGroupBy gbpart: sumDef.getRowGroupByPart().getGroupBys()){
							warnings.addAll(validator.validate(gbpart));
						}		
					
						summaryQuery.setQuery(sumDef.asQuery(), sumDef);
					} finally {
						session.getTransaction().rollback();
						session.close();
					}
				}
			}
		}
		
		
		summaryQuery.setConservationArea(SmartDB.getCurrentConservationArea());
		summaryQuery.setOwner(SmartDB.getCurrentEmployee());
		
		summaryQuery.setConservationAreaFilter(new ConservationAreaFilter(true));
		
		return summaryQuery;
	}
	
	/**
	 * @return warnings generated during import process
	 */
	@Override
	public ArrayList<String> getWarnings(){
		return this.warnings;
	}
	

	@Override
	public boolean canImport(IQueryType qt) {
		return qt.getKey().equals(PatrolSummaryQueryType.KEY);
	}
	
}
