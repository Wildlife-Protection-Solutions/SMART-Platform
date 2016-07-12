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

import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.importexport.GridTiffImageExporter;
import org.wcs.smart.query.common.importexport.ShapeQueryExporter;
import org.wcs.smart.query.common.model.CompoundMapQuery;
import org.wcs.smart.query.common.model.CompoundMapQueryLayer;
import org.wcs.smart.query.common.model.CompoundMapQueryResults;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.importexport.QueryExportEngine;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;

/**
 * Exporter for compound queries map results.  Exports to map files.
 * 
 * @author Emily
 *
 */
public class CompoundQueryExporter implements IQueryExporter {

	public static final String ID = "org.wcs.smart.query.compound.export.map"; //$NON-NLS-1$
	
	@Override
	public String getId() {
		return ID;
	}

	@Override
	public boolean supportsProjection() {
		return true;
	}

	@Override
	public String getName() {
		return Messages.CompoundQueryExporter_ExporterName;
	}

	@Override
	public String getDefaultExtension() {
		return null;
	}

	@Override
	public boolean canExport(Query query) {
		return query.getTypeKey().equals(CompoundMapQuery.TYPE_KEY);
	}

	@Override
	public void export(Query query, IQueryResult results, File file,
			HashMap<String, Object> parameters, IProgressMonitor monitor)
			throws Exception {
		if (results== null) {
			throw new Exception(Messages.SimpleQueryExporter_Error_QueryNotRun);
		}
		if (!file.isDirectory()){
			throw new Exception(Messages.CompoundQueryExporter_DirectoryRequired);
		}
		CompoundMapQuery mapQuery = (CompoundMapQuery)query;
		CompoundMapQueryResults cqresults = (CompoundMapQueryResults) mapQuery.getCachedResults();
		monitor.beginTask(Messages.CompoundQueryExporter_TaskName, mapQuery.getLayers().size());
		HashMap<Query, IQueryExporter> exports = new HashMap<Query, IQueryExporter>();
		StringBuilder messages = new StringBuilder();
		Session s = HibernateManager.openSession();
		try{
			
			for (CompoundMapQueryLayer q : mapQuery.getLayers()){
				Query queryObj = QueryHibernateManager.getInstance().findQuery(s, q.getQueryUuid(), QueryTypeManager.INSTANCE.findQueryType(q.getQueryType()));
				
				if (cqresults.getResults(queryObj.getUuid()) == null){
					messages.append(MessageFormat.format(Messages.CompoundQueryExporter_NoResults, queryObj.getName()));
				}else{
					for (IQueryExporter exp : QueryExportEngine.getQueryExports(queryObj)){
						if (exp instanceof ShapeQueryExporter){
							exports.put(queryObj, exp);
							break;
						}else if (exp instanceof GridTiffImageExporter){
							exports.put(queryObj, exp);
							break;
						}
					}
					if (exports.get(queryObj) == null){
						messages.append(MessageFormat.format(Messages.CompoundQueryExporter_NoExporter, queryObj.getName()));
					}
				}
			}
		}finally{
			s.close();
		}
		
		if (messages.length() > 0){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					MessageDialog.openWarning(Display.getDefault().getActiveShell(), Messages.CompoundQueryExporter_WarningsLabel, messages.toString());		
				}
			});
		}
		
		for(Entry<Query, IQueryExporter> exporter: exports.entrySet()){
			String queryName = URLUtils.cleanFilename(exporter.getKey().getName() + "_" + exporter.getKey().getId()) + "." + exporter.getValue().getDefaultExtension(); //$NON-NLS-1$ //$NON-NLS-2$
			File f = new File(file, queryName); 
			
			exporter.getValue().export(exporter.getKey(), cqresults.getResults(exporter.getKey().getUuid()), f, 
					parameters, new SubProgressMonitor(monitor, 1));
		}
	}
	

}
