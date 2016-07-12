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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.wcs.smart.ca.ConservationArea;
//import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.importexport.AbstractXmlQueryImporter;
import org.wcs.smart.query.importexport.QueryImportEngine;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.StyledQuery;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.ui.importexport.ImportQueryUtil;
import org.wcs.smart.query.xml.model.QueryPart;
import org.wcs.smart.query.xml.model.QueryType;
import org.wcs.smart.query.xml.model.UuidItemType;

/**
 * Importer for importing query definition files.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class SimpleQueryDefinitionImporter extends AbstractXmlQueryImporter {

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
	 * @param importCa the conservation area we are importing into
	 * @return the imported query
	 * @throws Exception if the file cannot be converted to a query.
	 * 
	 */
	@Override
	public Query importQuery(QueryType qt, ConservationArea importCa) throws Exception{
		warnings.clear();
		SimpleQuery wq;

		String langCode = qt.getLanguage();
		IQueryType qType = QueryTypeManager.INSTANCE.findQueryType(qt.getQueryType());
		if (qType == null){
			qType = QueryTypeManager.INSTANCE.findDeprecatedQueryType(qt.getQueryType());
		}
		if (qType == null){
			throw new Exception(MessageFormat.format(Messages.SimpleQueryDefinitionImporter_InvalidPatrolType, new Object[]{qt.getQueryType()}));
		}
		wq = (SimpleQuery) createQuery(qType);
		if (wq == null){
			throw new Exception(MessageFormat.format(Messages.SimpleQueryDefinitionImporter_InvalidPatrolType, new Object[]{qt.getQueryType()}));
		}
		
		QueryImportEngine.importNames(wq, qt, importCa);
		
		HashMap<String, UuidItemType> uuidLookup = new HashMap<String, UuidItemType>();
		for (UuidItemType type : qt.getUuiditem()){
			uuidLookup.put(type.getUuid(), type);
		}
		
		String strQueryFilter = ""; //$NON-NLS-1$
		String strColumnFilter = ""; //$NON-NLS-1$
		String stylePart = null;
		for (QueryPart part : qt.getQueryPart()) {
			if (part.getKey().equals("definition")) { //$NON-NLS-1$
				if (part.getValue() != null && part.getValue().length() > 0) {
					strQueryFilter = processDefinition(importCa, part.getValue(), langCode, uuidLookup);
				}
			}else if (part.getKey().equals("columns")){ //$NON-NLS-1$
				strColumnFilter = part.getValue();
			}else if (part.getKey().equals(StyledQuery.QUERY_STYLE_KEY)){
				stylePart = part.getValue();
			}
		}
		
		wq.setQueryFilter(strQueryFilter);
		wq.setVisibleColumns(strColumnFilter);
		wq.setConservationArea(importCa);
		wq.setOwner(ImportQueryUtil.findEmployee(importCa));
		
		wq.setConservationAreaFilter( (new ConservationAreaFilter(true, importCa)).asString());
		
		if (wq instanceof StyledQuery && stylePart != null){
			wq.setStyle(stylePart);
		}
		
		return wq;
	}
	
	/**
	 * @return warnings generated during import process
	 */
	@Override
	public ArrayList<String> getWarnings(){
		return this.warnings;
	}
	
	/**
	 * Converts the query definition in the xml file
	 * to the query definition.  Performs necessary validation
	 * and updates warnings as required.
	 * @param importCa the conservation area we are importing into
	 * @param queryDef the query definition part string
	 * @param langcode the language code from the query
	 * @param uuidLookup the list of uuid lookup items provided in the query definition file
	 */
	protected abstract String processDefinition(ConservationArea importCa, String queryDef, String langCode, HashMap<String, UuidItemType> uuidLookup) throws Exception;

	@Override
	public abstract boolean canImport(IQueryType qt);
	
	/**
	 * Creates a query for the given query type;
	 * @param queryType
	 * @return
	 */
	protected abstract SimpleQuery createQuery(IQueryType qt);
	
	
}
