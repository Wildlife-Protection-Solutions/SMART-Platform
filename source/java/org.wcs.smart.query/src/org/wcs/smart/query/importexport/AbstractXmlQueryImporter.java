package org.wcs.smart.query.importexport;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.xml.QueryXmlManager;
import org.wcs.smart.query.xml.model.QueryType;

public abstract class AbstractXmlQueryImporter implements IQueryImporter {

	@Override
	public boolean canImport(Path file) {
		try{
			org.wcs.smart.query.xml.model.Query q = null;
			try(InputStream fin = new BufferedInputStream(Files.newInputStream(file))){
				q = QueryXmlManager.readQueryFile(fin);
			}
		
			QueryType qt = q.getQuery();
			IQueryType lQueryType = QueryTypeManager.INSTANCE.findQueryType(qt.getQueryType());
			if (lQueryType == null){
				lQueryType = QueryTypeManager.INSTANCE.findDeprecatedQueryType(qt.getQueryType());
			}
			if (lQueryType == null){
				throw new Exception(MessageFormat.format(Messages.QueryImporter_InvalidQueryType2, new Object[]{ qt.getQueryType()}));
			}
			return canImport(lQueryType);
		}catch (Exception ex){
			return false;
		}
	}

	/**
	 * Determines if the importer can import the given query type.
	 * 
	 * @param qt
	 * @return
	 */
	public abstract boolean canImport(IQueryType qt);
	
	@Override
	public List<Query> importQuery(Path file, ConservationArea ca, Session session) throws Exception {
		org.wcs.smart.query.xml.model.Query q = null;
		try(InputStream fin = new BufferedInputStream(Files.newInputStream(file))){
			q = QueryXmlManager.readQueryFile(fin);
		}
		return Collections.singletonList(importQuery(q.getQuery(), ca, session));
	}

	/**
	 * Imports the given xml query.  This function is responsible
	 * for converting to the internal model format.  It is not responsible
	 * for saving the query to the database.  That is done in a separate
	 * process.
	 * 
	 * @param qt the xml query type to import.
	 * @param the conservation area 
	 * @return the imported query definition
	 * @throws Exception if the query cannot be imported
	 */
	public abstract Query importQuery(QueryType xmlQuery, ConservationArea ca, Session session) throws Exception;
	
	@Override
	public abstract ArrayList<String> getWarnings();

	@Override
	public void beforeCommit() throws Exception{}

}
