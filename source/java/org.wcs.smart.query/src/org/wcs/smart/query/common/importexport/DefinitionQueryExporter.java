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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.common.filter.ISmartProgressMonitor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.StyledQuery;
import org.wcs.smart.query.xml.QueryXmlManager;
import org.wcs.smart.query.xml.model.Query;
import org.wcs.smart.query.xml.model.QueryName;
import org.wcs.smart.query.xml.model.QueryPart;
import org.wcs.smart.query.xml.model.QueryType;

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
	public void export(org.wcs.smart.query.model.Query query, IQueryResult result, File file,
			HashMap<String, Object> parameters, ISmartProgressMonitor monitor)
			throws Exception {
		Query wpquery = new Query();
		QueryType xmlQuery = new QueryType();
		wpquery.setQuery(xmlQuery);
		xmlQuery.setQueryType(query.getTypeKey());
		
		
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try {
			s.saveOrUpdate(query);

			if (query.getConservationArea().getDefaultLanguage() != null){
				xmlQuery.setLanguage(query.getConservationArea().getDefaultLanguage().getCode());
			}else{
				xmlQuery.setLanguage(SmartDB.getCurrentLanguage().getCode());
			}
			
			for (org.wcs.smart.ca.Label l : query.getNames()) {
				QueryName qn = new QueryName();
				qn.setName(l.getValue());
				qn.setLanguage(l.getLanguage().getCode());
				qn.setIsDefault(l.getLanguage().isDefault());

				xmlQuery.getName().add(qn);
			}
		} finally {
			s.getTransaction().rollback();
			s.close();
		}
		
		writeQuerySpecifics(query, xmlQuery);
		
		if (query instanceof StyledQuery){
			String styleString = ((StyledQuery) query).getStyle();
			if (styleString != null){
				QueryPart style = new QueryPart();
				style.setKey(StyledQuery.QUERY_STYLE_KEY);
				style.setValue(styleString);
			
				xmlQuery.getQueryPart().add(style);
			}
		}
		
		try(OutputStream fout = new BufferedOutputStream(new FileOutputStream(file))){
			QueryXmlManager.writeQuery(wpquery, fout);
		}		
	}
}
