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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Language;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.xml.QueryXmlManager;
import org.wcs.smart.query.xml.model.Query;
import org.wcs.smart.query.xml.model.QueryName;
import org.wcs.smart.query.xml.model.QueryType;

/**
 * Query definition importer
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
		Query q = QueryXmlManager.readQueryFile(fin);
		fin.close();
		
		QueryType qt = q.getQuery();
		IQueryImporter importer = getQueryImporter(qt);
		if (importer == null){
			throw new Exception(MessageFormat.format(Messages.QueryImporter_InvalidQueryType, new Object[]{ qt.getQueryType()}));
		}
		
		org.wcs.smart.query.model.Query importedQuery = importer.importQuery(qt);
		warnings.addAll(importer.getWarnings());
		return importedQuery;
		
	}
	
	/**
	 * @return a list of warnings generated during the import process
	 */
	public List<String> getWarnings(){
		return this.warnings;
	}
	
	private IQueryImporter getQueryImporter(QueryType qt) {
		if (qt.getQueryType().equalsIgnoreCase(org.wcs.smart.query.model.Query.QueryType.SUMMARY.name())){
			return new SummaryQueryDefinitionImporter();
		}else if (qt.getQueryType().equalsIgnoreCase(org.wcs.smart.query.model.Query.QueryType.OBSERVATION.name())){
			return new SimpleQueryDefinitionImporter();
		}else if (qt.getQueryType().equalsIgnoreCase(org.wcs.smart.query.model.Query.QueryType.PATROL.name())){
			return new SimpleQueryDefinitionImporter();
		}else if (qt.getQueryType().equalsIgnoreCase(org.wcs.smart.query.model.Query.QueryType.GRIDDED.name())){
			return new GriddedQueryDefinitionImporter();
		}
		return null;
	}
	
	
	/**
	 * Imports the query names from the xml query type to the SMART query object.
	 * @param query
	 * @param qt
	 * @throws Exception
	 */
	public static void importNames(org.wcs.smart.query.model.Query query, QueryType qt) throws Exception{
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			query.getNames().clear();
			String xmlDefaultName = null;
			for (QueryName qn : qt.getName()){
				if (qn.getIsDefault()){
					xmlDefaultName = qn.getName();
				}
				List<?> values = session.createCriteria(Language.class).add(Restrictions.eq("ca", SmartDB.getCurrentConservationArea())).add(Restrictions.eq("code",qn.getLanguage())).list(); //$NON-NLS-1$ //$NON-NLS-2$
				if (values.size() > 0){
					for (Object l : values){
						query.updateName((Language)l, qn.getName());
					}
				}
			}
			String defaultName = query.findNameNull(SmartDB.getCurrentConservationArea().getDefaultLanguage());
			if (defaultName == null){
				if (xmlDefaultName != null){
					query.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), xmlDefaultName);
				}else{
					query.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), qt.getName().get(0).getName());
				}
			}
			String name = query.findNameNull(SmartDB.getCurrentLanguage());
			if (name == null){
				name = query.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
			}
			query.setName(name);
		
		} finally {
			session.getTransaction().rollback();
			session.close();
		}
	}
}

