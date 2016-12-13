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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.model.CompoundMapQuery;
import org.wcs.smart.query.common.model.CompoundMapQueryLayer;
import org.wcs.smart.query.importexport.IQueryImporter;
import org.wcs.smart.query.importexport.QueryImportEngine;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.ui.importexport.ImportQueryUtil;
import org.wcs.smart.query.xml.QueryXmlManager;
import org.wcs.smart.query.xml.model.QueryPart;
import org.wcs.smart.query.xml.model.QueryType;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Importer for compound query definitions.
 * 
 * @author Emily
 *
 */
public class CompoundQueryDefinitionImporter implements IQueryImporter {
	
	private ArrayList<String> warnings = new ArrayList<String>();
	
	private CompoundMapQuery compoundQuery;
	private HashMap<Integer, Query> layerQueryMap;
	
	@Override
	public boolean canImport(File file) {
		try{
			Path tempDir = Files.createTempDirectory("smart" + System.nanoTime()); //$NON-NLS-1$
			try{
			
				ZipUtil.unzipFolder(file, tempDir.toFile());
				Path queryfile = tempDir.resolve(CompoundQueryDefinitionExporter.MAIN_QUERY_FILE);
				if (Files.exists(queryfile)){
					org.wcs.smart.query.xml.model.Query q = null;
					try(InputStream fin = new BufferedInputStream(Files.newInputStream(queryfile))){
						q = QueryXmlManager.readQueryFile(fin);
					}
					QueryType qt = q.getQuery();
					if (qt.getQueryType().equalsIgnoreCase(CompoundMapQuery.TYPE_KEY)){
						return true;
					}
				}
			}finally{
				FileUtils.forceDelete(tempDir.toFile());
			}
		}catch (Exception ex){
			
		}
		return false;
	}

	@Override
	public List<Query> importQuery(File file, ConservationArea ca) throws Exception {
		layerQueryMap = new HashMap<Integer, Query>();
		
		List<Query> imported = new ArrayList<Query>();
		Path tempDir = Files.createTempDirectory("smart" + System.nanoTime()); //$NON-NLS-1$
		try{
		
			ZipUtil.unzipFolder(file, tempDir.toFile());
			Path queryfile = tempDir.resolve(CompoundQueryDefinitionExporter.MAIN_QUERY_FILE); 
			if (Files.exists(queryfile)){
				org.wcs.smart.query.xml.model.Query q = null;
				try(InputStream fin = new BufferedInputStream(Files.newInputStream(queryfile))){
					q = QueryXmlManager.readQueryFile(fin);
				}
				QueryType qt = q.getQuery();
				
				compoundQuery = new CompoundMapQuery();
				QueryImportEngine.importNames(compoundQuery, qt, ca);
				compoundQuery.setConservationArea(ca);
				compoundQuery.setLayers(new ArrayList<CompoundMapQueryLayer>());
				compoundQuery.setOwner(ImportQueryUtil.findEmployee(ca));
				
				int maxorder = -1;
				for (QueryPart p : qt.getQueryPart()){
					int order = Integer.parseInt(p.getKey().split("_")[2]); //$NON-NLS-1$
					if (order > maxorder) maxorder = order;
				}
				for (int i = 0; i <= maxorder; i ++){
					CompoundMapQueryLayer layer = new CompoundMapQueryLayer();
					layer.setMapQuery(compoundQuery);
					layer.setOrder(i);
					compoundQuery.getLayers().add(layer);
				}
				for (QueryPart p : qt.getQueryPart()){
					int order = Integer.parseInt(p.getKey().split("_")[2]); //$NON-NLS-1$
					CompoundMapQueryLayer layer = compoundQuery.getLayers().get(order);
					if (p.getKey().startsWith(CompoundQueryDefinitionExporter.QUERY_UUID_KEY_PART)){ 
						layer.setQueryUuid(UuidUtils.stringToUuid(p.getValue()));
					}else if (p.getKey().startsWith(CompoundQueryDefinitionExporter.QUERY_TYPE_KEY_PART)){ 
						layer.setQueryType(p.getValue());
					}else if (p.getKey().startsWith(CompoundQueryDefinitionExporter.DATEFILTER_KEY_PART)){ 
						layer.setDateFilter(p.getValue());
					}else if (p.getKey().startsWith(CompoundQueryDefinitionExporter.STYLE_KEY_PART)){ 
						layer.setQueryStyle(p.getValue());
					}
				}
				imported.add(compoundQuery);
				
				List<CompoundMapQueryLayer> toDelete = new ArrayList<CompoundMapQueryLayer>();
				
				for (CompoundMapQueryLayer l : compoundQuery.getLayers()){
					Query localQuery = null;
				
					Session s = HibernateManager.openSession();
					try{
						localQuery = QueryHibernateManager.getInstance().findQuery(s, l.getQueryUuid(), QueryTypeManager.INSTANCE.findQueryType(l.getQueryType()));
						if (localQuery != null) localQuery.getConservationArea().equals(ca);	//implemented to fix hibernate session loading problem
					}finally{
						s.close();
					}
					
					if (localQuery == null || !localQuery.getConservationArea().equals(ca)){
					 
						//need to import query
						Path queryPath = tempDir.resolve(CompoundQueryDefinitionExporter.SUBFOLDER_NAME).resolve( UuidUtils.uuidToString(l.getQueryUuid()) + ".xml"); //$NON-NLS-1$
						IQueryImporter importer = QueryImportEngine.getQueryImporter(queryPath.toFile());
						if (importer == null){
							warnings.add(Messages.CompoundQueryDefinitionImporter_ImporterNotFound);
						}else{
							try{
								//we only reference a single query
								List<Query> importedQuery = importer.importQuery(queryPath.toFile(), ca);
								Query importedQueryMain = importedQuery.get(0);
								//lets find the same query in the current database 
								
								
								localQuery = null;
								s = HibernateManager.openSession();
								try{
									List<Query> localOptions = QueryHibernateManager.getInstance().findQuery(s, importedQueryMain.getName(), QueryTypeManager.INSTANCE.findQueryType(l.getQueryType()), ca, SmartDB.getCurrentEmployee());
									for (Query localQueryOp : localOptions){
										if (localQueryOp.isDefinitionEqual(importedQueryMain)){
											localQuery = localQueryOp;
										}
									}
								}finally{
									s.close();
								}
								
								if (localQuery != null){
									//reference the local query
									l.setQueryUuid(localQuery.getUuid());
								}else{
									layerQueryMap.put(l.getOrder(), importedQueryMain);
									imported.addAll(importedQuery);
								}
								
							}catch (Exception ex){
								toDelete.add(l);
								warnings.add(MessageFormat.format(Messages.CompoundQueryDefinitionImporter_SubImporterError, ex.getMessage()));
								QueryPlugIn.log(ex.getMessage(), ex);
							}
						}
					}else{
						//could potentially validate the query definitions here ,but seems unnecessary
					}
				}
				compoundQuery.getLayers().removeAll(toDelete);
				
				return imported;
			}else{
				throw new Exception(Messages.CompoundQueryDefinitionImporter_QueryNotFound);
			}
		}finally{
			FileUtils.forceDelete(tempDir.toFile());
		}
	}

	@Override
	public ArrayList<String> getWarnings() {
		return warnings;
	}


	public void beforeCommit() throws Exception{
		for (CompoundMapQueryLayer l : compoundQuery.getLayers()){
			Query q = layerQueryMap.get(l.getOrder());
			if (q != null){
				l.setQueryUuid(q.getUuid());
				l.setQueryType(q.getTypeKey());
			}
		}
	}
}
