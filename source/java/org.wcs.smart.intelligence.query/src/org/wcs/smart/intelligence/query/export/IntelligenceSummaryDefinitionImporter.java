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

import java.util.ArrayList;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.intelligence.query.IntelligenceQueryFactory;
import org.wcs.smart.intelligence.query.model.IntelligenceSummaryQuery;
import org.wcs.smart.query.importexport.IQueryImporter;
import org.wcs.smart.query.importexport.QueryImportEngine;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.ui.importexport.ImportQueryUtil;
import org.wcs.smart.query.xml.model.QueryType;

/**
 * Intelligence Summary query importer.  Imports intelligence summary queries
 * from xml file.
 * 
 * @author Emily
 *
 */
public class IntelligenceSummaryDefinitionImporter implements IQueryImporter{

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
	public Query importQuery(QueryType qt, ConservationArea caImport) throws Exception{
		warnings.clear();
		
		IntelligenceSummaryQuery summaryQuery = createQuery();
		QueryImportEngine.importNames(summaryQuery, qt, caImport);
		
		summaryQuery.setConservationArea(caImport);
		summaryQuery.setOwner(ImportQueryUtil.findEmployee(caImport));
		summaryQuery.setConservationAreaFilter((new ConservationAreaFilter(true, caImport)).asString());
		
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
		if (IntelligenceSummaryQuery.KEY.equals(qt.getKey())){
			return true;
		}
		return false;
	}

	public IntelligenceSummaryQuery createQuery() {
		return IntelligenceQueryFactory.createIntelligenceSummaryQuery();
	}

}
