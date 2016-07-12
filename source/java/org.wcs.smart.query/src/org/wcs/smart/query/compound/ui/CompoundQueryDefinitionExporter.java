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
package org.wcs.smart.query.compound.ui;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.importexport.DefinitionQueryExporter;
import org.wcs.smart.query.common.model.CompoundMapQuery;
import org.wcs.smart.query.common.model.CompoundMapQueryLayer;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.importexport.QueryExportEngine;
import org.wcs.smart.query.xml.QueryXmlManager;
import org.wcs.smart.query.xml.model.Query;
import org.wcs.smart.query.xml.model.QueryName;
import org.wcs.smart.query.xml.model.QueryPart;
import org.wcs.smart.query.xml.model.QueryType;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Query definition exporter that exports compound queries and associated
 * sub queries.
 * 
 * @author Emily
 *
 */
public class CompoundQueryDefinitionExporter extends DefinitionQueryExporter  {

	@Override
	public boolean canExport(org.wcs.smart.query.model.Query query) {
		return (query.getTypeKey().equals(CompoundMapQuery.TYPE_KEY));
	}

	@Override
	public void writeQuerySpecifics(org.wcs.smart.query.model.Query query, QueryType xmlQuery)
			throws Exception {

	}

	/**
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#getDefaultExtension()
	 */
	@Override
	public String getDefaultExtension() {
		return "zip"; //$NON-NLS-1$
	}
	
	@Override
	public void export(org.wcs.smart.query.model.Query query, IQueryResult result, File file,
			HashMap<String, Object> parameters, IProgressMonitor monitor)
			throws Exception {
		
		Path tempDir = Files.createTempDirectory("smart" + UuidUtils.uuidToString(query.getUuid())); //$NON-NLS-1$
		try{
			Query wpquery = new Query();
			QueryType xmlQuery = new QueryType();
			wpquery.setQuery(xmlQuery);
			xmlQuery.setQueryType(query.getTypeKey());
			
			CompoundMapQuery cquery = (CompoundMapQuery)query;
			
			List<org.wcs.smart.query.model.Query> toExport = new ArrayList<org.wcs.smart.query.model.Query>();
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
				
				for (CompoundMapQueryLayer l : cquery.getLayers()){
					org.wcs.smart.query.model.Query q = QueryHibernateManager.getInstance().findQuery(s, l.getQueryUuid(), QueryTypeManager.INSTANCE.findQueryType(l.getQueryType()));
					toExport.add(q);
				}
			} finally {
				s.getTransaction().rollback();
				s.close();
			}
			
			for (int order = 0; order < cquery.getLayers().size(); order++ ){
				CompoundMapQueryLayer l = cquery.getLayers().get(order);
				QueryPart qp = new QueryPart();
				qp.setKey("QP_UUID_" + order); //$NON-NLS-1$ 
				qp.setValue(UuidUtils.uuidToString(l.getQueryUuid()));
				xmlQuery.getQueryPart().add(qp);
				
				qp = new QueryPart();
				qp.setKey("QP_TYPE_" + order); //$NON-NLS-1$
				qp.setValue(l.getQueryType());
				xmlQuery.getQueryPart().add(qp);
				
				qp = new QueryPart();
				qp.setKey("QP_DATEFILTER_" + order); //$NON-NLS-1$ 
				qp.setValue(l.getDateFilter());
				xmlQuery.getQueryPart().add(qp);
				
				if (l.getQueryStyle() == null){
					qp = new QueryPart();
					qp.setKey("QP_STYLE_" + order); //$NON-NLS-1$
					qp.setValue(l.getQueryStyle());
					xmlQuery.getQueryPart().add(qp);
				}
			}
			
			Path queryFile = tempDir.resolve("query.xml"); //$NON-NLS-1$
			try(OutputStream fout = new BufferedOutputStream(Files.newOutputStream(queryFile))){
				QueryXmlManager.writeQuery(wpquery, fout);
			}
			
			Path subDir = tempDir.resolve("queries"); //$NON-NLS-1$
			Files.createDirectories(subDir);
			
			for (org.wcs.smart.query.model.Query q : toExport){
				IQueryExporter lexporter = null;
				for (IQueryExporter exporter : QueryExportEngine.getQueryExports(q)){
					if (exporter.getId().startsWith(IQueryExporter.QUERY_DEFINTION_EXPORTER_ID)){
						lexporter = exporter;
						break;
					}
				}
				Path output = subDir.resolve(UuidUtils.uuidToString(q.getUuid()) + ".xml"); //$NON-NLS-1$
				//to definition exporter found for query type
				if (lexporter != null){
					lexporter.export(q, null, output.toFile(), parameters, monitor);
				}
			}
			
			ZipUtil.createZip(new File[]{queryFile.toFile(), subDir.toFile()}, file, monitor);
		}finally{
			FileUtils.forceDelete(tempDir.toFile());
		}
	}
}
