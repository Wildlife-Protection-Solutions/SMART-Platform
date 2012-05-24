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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.xml.QueryXmlManager;
import org.wcs.smart.query.xml.model.Query;
import org.wcs.smart.query.xml.model.QueryType;

/**
 * Query deifnition importer
 * 
 * @author egouge
 * @since 1.0.0
 */
public class QueryImporter {
	
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
	public org.wcs.smart.query.model.Query importQuery(File file) throws Exception{
		warnings.clear();
		
		InputStream fin = new BufferedInputStream(new FileInputStream(file));
		Query q = QueryXmlManager.readDataModel(fin);
		fin.close();
		
		QueryType qt = q.getQuery();
		if (qt.getQueryType().equalsIgnoreCase(org.wcs.smart.query.model.Query.QueryType.SUMMARY.name())){
			return importSummary(qt);
		}else if (qt.getQueryType().equalsIgnoreCase(org.wcs.smart.query.model.Query.QueryType.OBSERVATION.name())){
			return importObservation(qt);
		}else{
			throw new Exception("Could not import query of type " + qt.getQueryType());
		}
	}
	
	/**
	 * @return a list of warnings generated during the import process
	 */
	public List<String> getWarnings(){
		return this.warnings;
	}
	
	/*
	 * process summary query
	 */
	private org.wcs.smart.query.model.Query importSummary(QueryType qt) throws Exception{
		SummaryQueryDefinitionImporter importer = new SummaryQueryDefinitionImporter();
		org.wcs.smart.query.model.Query query = importer.importQuery(qt);
		warnings.addAll(importer.getWarnings());
		return query;
	}
	
	/*
	 * processes observation query
	 */
	private org.wcs.smart.query.model.Query importObservation(QueryType qt) throws Exception{
		ObservationQueryDefinitionImporter importer = new ObservationQueryDefinitionImporter();
		org.wcs.smart.query.model.Query query = importer.importQuery(qt);
		warnings.addAll(importer.getWarnings());
		return query;
	}
	
	
	
	/**
	 * Attempts to find an employee based on the values.
	 * <p>
	 * First attempts based on the id & name, then the id only, then the name only.
	 * </p>
	 * @param employeeId
	 * @param givenName
	 * @param familyName
	 * @param session
	 * @return null if employee not found, otherwise employee found
	 * 
	 */
	public static Employee findEmployee(String employeeId, String givenName, String familyName, Session session, List<String> warnings){
		Employee e = HibernateManager.findEmployeeByIdAndName(employeeId, givenName, familyName, SmartDB.getCurrentConservationArea(), session);
		if (e != null){
			warnings.add("The unique identifier for Employee " + givenName + " " + familyName + "[" + employeeId + "] did not match the database unique identifier. However an employee with the same name and identifier was found and will be used in the query.");
			return e;
		}
		
		e = HibernateManager.findEmployeeById(employeeId, SmartDB.getCurrentConservationArea(), session);
		if (e != null){
			warnings.add("The unique identifier for Employee " + givenName + " " + familyName + "[" + employeeId + "] did not match the database unique identifier. However an employee with the same identifier but a different name '" + e.getGivenName() + " " + e.getFamilyName() + "' was found and will be used in the query.");
			return e;
		}
		
		e = HibernateManager.findEmployeeByName(givenName, familyName, SmartDB.getCurrentConservationArea(), session);
		if (e != null){
			warnings.add("The unique identifier for Employee " + givenName + " " + familyName + "[" + employeeId + "] did not match the database unique identifier. However employee with the same name but a different identifier '" + e.getId() + "' was found and will be used in the query.");
			return e;
		}
		
		return null;
	}
	
	

	
	
	
}

