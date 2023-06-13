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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
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
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.xml.QueryXmlManager;
import org.wcs.smart.query.xml.model.Query;
import org.wcs.smart.query.xml.model.QueryName;
import org.wcs.smart.query.xml.model.QueryPart;
import org.wcs.smart.query.xml.model.QueryType;
import org.wcs.smart.util.SmartUtils;
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

	public static final String SUBFOLDER_NAME = "queries"; //$NON-NLS-1$
	public static final String MAIN_QUERY_FILE = "query.xml"; //$NON-NLS-1$
	public static final String STYLE_KEY_PART = "QP_STYLE_"; //$NON-NLS-1$
	public static final String DATEFILTER_KEY_PART = "QP_DATEFILTER_"; //$NON-NLS-1$
	public static final String QUERY_TYPE_KEY_PART = "QP_TYPE_"; //$NON-NLS-1$
	public static final String QUERY_UUID_KEY_PART = "QP_UUID_"; //$NON-NLS-1$

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
	public void export(org.wcs.smart.query.model.Query query, IQueryResult result, Path file,
			HashMap<String, Object> parameters, IProgressMonitor monitor)
			throws Exception {
		SubMonitor progress = SubMonitor.convert(monitor, "", 3); //$NON-NLS-1$
		Path tempDir = Files.createTempDirectory("smart" + UuidUtils.uuidToString(query.getUuid())); //$NON-NLS-1$
		try{
			Query wpquery = new Query();
			QueryType xmlQuery = new QueryType();
			wpquery.setQuery(xmlQuery);
			
			List<org.wcs.smart.query.model.Query> toExport = new ArrayList<org.wcs.smart.query.model.Query>();
			try(Session s = HibernateManager.openSession()){
				s.beginTransaction();
				try {
					query = s.getReference(query);
					
					xmlQuery.setQueryType(query.getTypeKey());
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
					
					for (CompoundMapQueryLayer l : ((CompoundMapQuery)query).getLayers()){
						UUID qUuid = l.getQueryUuid();
						IQueryType type = QueryTypeManager.INSTANCE.findQueryType(l.getQueryType());
						if (type == null) {
							throw new Exception(MessageFormat.format(Messages.CompoundQueryDefinitionExporter_QueryTypeNotSupported, l.getQueryType()));
						}
						org.wcs.smart.query.model.Query q = QueryHibernateManager.getInstance().findQuery(s, qUuid, type);
						toExport.add(q);
					}
				} finally {
					s.getTransaction().rollback();
				}
			}
			CompoundMapQuery cquery = (CompoundMapQuery)query;
			for (int order = 0; order < cquery.getLayers().size(); order++ ){
				CompoundMapQueryLayer l = cquery.getLayers().get(order);
				QueryPart qp = new QueryPart();
				qp.setKey(QUERY_UUID_KEY_PART + order); 
				qp.setValue(UuidUtils.uuidToString(l.getQueryUuid()));
				xmlQuery.getQueryPart().add(qp);
				
				qp = new QueryPart();
				qp.setKey(QUERY_TYPE_KEY_PART + order); 
				qp.setValue(l.getQueryType());
				xmlQuery.getQueryPart().add(qp);
				
				qp = new QueryPart();
				qp.setKey(DATEFILTER_KEY_PART + order); 
				qp.setValue(l.getDateFilter());
				xmlQuery.getQueryPart().add(qp);
				
				if (l.getQueryStyle() == null){
					qp = new QueryPart();
					qp.setKey(STYLE_KEY_PART + order);
					qp.setValue(l.getQueryStyle());
					xmlQuery.getQueryPart().add(qp);
				}
			}
			
			Path queryFile = tempDir.resolve(MAIN_QUERY_FILE); 
			try(OutputStream fout = new BufferedOutputStream(Files.newOutputStream(queryFile))){
				QueryXmlManager.writeQuery(wpquery, fout);
			}
			
			Path subDir = tempDir.resolve(SUBFOLDER_NAME); 
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
					lexporter.export(q, null, output, parameters, progress.split(2));
				}
			}
			
			ZipUtil.createZip(new Path[]{queryFile, subDir}, file, progress.split(1));
		}finally{
			SmartUtils.deleteDirectory(tempDir);
		}
	}
}
