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
package org.wcs.smart.query.qimport;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFactory;
import org.wcs.smart.query.model.SimpleQuery;
import org.wcs.smart.query.parser.filter.ConservationAreaFilter;
import org.wcs.smart.query.parser.filter.FilterValidator;
import org.wcs.smart.query.parser.internal.filter.IFilter;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.xml.model.QueryPart;
import org.wcs.smart.query.xml.model.QueryType;
import org.wcs.smart.query.xml.model.UuidItemType;

/**
 * Importer for importing query definition files.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SimpleQueryDefinitionImporter implements IQueryImporter {

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
		SimpleQuery wq;

		String langCode = qt.getLanguage();
		if (qt.getQueryType().equalsIgnoreCase(org.wcs.smart.query.model.Query.QueryType.OBSERVATION.name())){
			wq = QueryFactory.createObservationQuery();	
		}else if (qt.getQueryType().equalsIgnoreCase(org.wcs.smart.query.model.Query.QueryType.PATROL.name())){
			wq = QueryFactory.createPatrolQuery();
		}else{
			throw new Exception(MessageFormat.format(Messages.SimpleQueryDefinitionImporter_InvalidPatrolType, new Object[]{qt.getQueryType()}));
		}
		
		QueryImporter.importNames(wq, qt);
		
		HashMap<String, UuidItemType> uuidLookup = new HashMap<String, UuidItemType>();
		for (UuidItemType type : qt.getUuiditem()){
			uuidLookup.put(type.getUuid(), type);
		}
		
		String strQueryFilter = ""; //$NON-NLS-1$
		String strColumnFilter = ""; //$NON-NLS-1$
		for (QueryPart part : qt.getQueryPart()) {
			if (part.getKey().equals("definition")) { //$NON-NLS-1$
				if (part.getValue() != null && part.getValue().length() > 0) {
					InputStream is = new ByteArrayInputStream(part.getValue()
							.getBytes());
					Parser parser = new Parser(is);
					IFilter queryFilter = parser.QueryFilter();
					is.close();

					Session session = HibernateManager.openSession();
					session.beginTransaction();
					try {
						FilterValidator validator = new FilterValidator();
						validator.validateFilterPart(queryFilter, langCode, uuidLookup, session);
						warnings.addAll(validator.getWarnings());
					} finally {
						session.getTransaction().rollback();
						session.close();
					}
					strQueryFilter = queryFilter.asString();
				}
			}else if (part.getKey().equals("columns")){ //$NON-NLS-1$
				strColumnFilter = part.getValue();
			}
		}
		
		wq.setQueryFilter(strQueryFilter);
		wq.setVisibleColumns(strColumnFilter);
		wq.setConservationArea(SmartDB.getCurrentConservationArea());
		wq.setOwner(SmartDB.getCurrentEmployee());
		
		ConservationAreaFilter caFilter = new ConservationAreaFilter();
		caFilter.addConservationArea(SmartDB.getCurrentConservationArea());
		wq.setConservationAreaFilter(caFilter);
		
		
		return wq;
	}
	
	/**
	 * @return warnings generated during import process
	 */
	@Override
	public ArrayList<String> getWarnings(){
		return this.warnings;
	}
	
}
