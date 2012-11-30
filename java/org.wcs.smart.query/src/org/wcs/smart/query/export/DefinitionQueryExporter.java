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
package org.wcs.smart.query.export;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolQueryOptionType;
import org.wcs.smart.query.xml.QueryXmlManager;
import org.wcs.smart.query.xml.model.Query;
import org.wcs.smart.query.xml.model.QueryType;
import org.wcs.smart.query.xml.model.UuidItemType;
import org.wcs.smart.util.SmartUtils;

/**
 * Query exporter that exports the query definition
 * to a file.  Does not export 
 * query results.
 *  
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class DefinitionQueryExporter implements IQueryExporter {

	public static final String QUERY_DEFINTION_EXPORTER_ID = "org.wcs.smart.query.export.definition"; //$NON-NLS-1$

	@Override
	public String getId(){
		return  QUERY_DEFINTION_EXPORTER_ID;
	}
	
	/**
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#getName()
	 */
	@Override
	public String getName() {
		return Messages.DefinitionQueryExporter_ExporterName;
	}

	/**
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#getDefaultExtension()
	 */
	@Override
	public String getDefaultExtension() {
		return "xml"; //$NON-NLS-1$
	}



	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#canExport(org.wcs.smart.query.model.Query)
	 */
	@Override
	public abstract  boolean canExport(org.wcs.smart.query.model.Query query);

	public abstract void writeQuerySpecifics(org.wcs.smart.query.model.Query query, QueryType xmlQuery) throws Exception;
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#export(org.wcs.smart.query.model.Query, java.io.File, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void export(org.wcs.smart.query.model.Query query, File file,
			IProgressMonitor monitor) throws Exception {
		Query wpquery = new Query();
		QueryType xmlQuery = new QueryType();
		wpquery.setQuery(xmlQuery);
		
		xmlQuery.setLanguage(SmartDB.getCurrentConservationArea().getDefaultLanguage().getCode());
		xmlQuery.setName(query.getName());
		xmlQuery.setQueryType(query.getType().name());
		
		writeQuerySpecifics(query, xmlQuery);
		
		OutputStream fout = new BufferedOutputStream(new FileOutputStream(file));
		try{
			QueryXmlManager.writeQuery(wpquery, fout);
		}finally{
			fout.close();
		}		
	}
	

	/**
	 * Converts a patrol option and associated uuid option to
	 * a xml uuiditemtype
	 * 
	 * @param option
	 * @param uuid
	 * @param session
	 * @return
	 * @throws Exception
	 */
	protected UuidItemType processPatrolOption(PatrolQueryOption option, String uuid, Session session) throws Exception{

		if (option.getType() == PatrolQueryOptionType.UUID){
			//we need to add a uuid type
			UuidItemType item = new UuidItemType();
			item.setUuid(uuid);
			//find item in database
			
			String[] data = option.getNames(session, SmartUtils.decodeHex(uuid));
			if (data != null){
				int index = 0;
				if (data.length > 1){
					item.setId(data[0]);
					index = 1;
				}
				for (;index < data.length; index++){
					item.getValue().add(data[index]);
				}
			}
			
			return item;
		}
		return null;
	}

}
