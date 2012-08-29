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
package org.wcs.smart.report.export.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.SessionHandle;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.export.DefinitionQueryExporter;
import org.wcs.smart.query.export.IQueryExporter;
import org.wcs.smart.query.export.QueryExportEngine;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.export.IReportExporter;
import org.wcs.smart.report.manger.ReportManager;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.util.SmartUtils;

/**
 * Report exporter that exports the report
 * definition and associated query files.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ReportDefintionExporter implements IReportExporter {

	public ReportDefintionExporter() {
	}

	/**
	 * Exports each report to a different zip file
	 */
	@Override
	public void exportReport(File file, Report report, HashMap<String, Object> reportParams, IProgressMonitor monitor) throws Exception {
		ZipOutputStream zout = null; 
		zout = new ZipOutputStream(new FileOutputStream(file));
		
		monitor.beginTask("Exporting work", 3);
		try{
			zout.setLevel(Deflater.DEFAULT_COMPRESSION);
			
			//export queries
			monitor.subTask("Exporting Queries");
			exportQueries(zout, report);
			monitor.worked(1);

			//add report file
			monitor.subTask("Exporting Report Definition");
			addFile(report.getFullReportFilename(), report.getFilename(), zout);
			monitor.worked(1);
			
			//add report info file
			monitor.subTask("Exporting Report Info");
			File reportInfo = File.createTempFile(report.getName().replaceAll("[^a-zA-Z0-9]", ""), ".rpt");
			try{
				writeReportInfo(reportInfo, report);
				addFile(reportInfo, reportInfo.getName(), zout);
			}finally{
				reportInfo.delete();
			}
			monitor.worked(1);
		}finally{
			zout.close();
		}
		
	}
	
	/**
	 * Writes a java properties file that contains
	 * the report name and filename.
	 * 
	 * @param f the report properties filesname
	 * @param report report 
	 * @throws Exception
	 */
	private void writeReportInfo(File f, Report report) throws Exception{
		Properties prop = new Properties();
		prop.setProperty("name", report.getName());
		prop.setProperty("filename", report.getFilename());
		FileOutputStream fout = new FileOutputStream(f);
		prop.store(fout, null);
		fout.close();
	}
	
	/**
	 * Exports the queries into the report and add the results
	 * to the zip file.
	 * 
	 * @param zout 
	 * @param report
	 * @throws Exception
	 */
	private void exportQueries(ZipOutputStream zout, Report report) throws Exception{
		
		File reportFile = new File(ReportPlugIn.getReportDirectory(), report.getFilename());

		SessionHandle session = SessionHandleAdapter.getInstance().getSessionHandle();

		ReportDesignHandle rdh = session.openDesign(reportFile.getAbsolutePath());
		
		List<?> datasets = rdh.getDataSets().getContents();
		for (Iterator<?> iterator = datasets.iterator(); iterator.hasNext();) {
			DataSetHandle dataset = (DataSetHandle) iterator.next();
			if (dataset instanceof OdaDataSetHandle){
				if (((OdaDataSetHandle)dataset).getExtensionID().equals(ReportManager.SMART_DATASET_TYPE)){
					String queryUuid = ((OdaDataSetHandle) dataset).getQueryText().split(":")[1];
					byte[] uuid = SmartUtils.decodeHex(queryUuid);Session hsession = HibernateManager.openSession();
					Query smartQuery = null;
					try{
						hsession.beginTransaction();
						smartQuery = QueryHibernateManager.findQuery(hsession, uuid, null);
					}finally{
						hsession.getTransaction().commit();
						hsession.close();
					}
					
					if (smartQuery == null) throw new Exception("Failed to find definition for smart query.");
					exportQuery(smartQuery, zout);
				}
			}
		}
		rdh.save();
		rdh.close();
		
	}
	
	/**
	 * Exports a query 
	 * @param query
	 * @param zipOut
	 * @throws Exception
	 */
	private void exportQuery(Query query, ZipOutputStream zipOut) throws Exception{
		List<IQueryExporter> queryExports = QueryExportEngine.getQueryExports(query);
		IQueryExporter definitionExporter = null;
		for (IQueryExporter exporter : queryExports){
			if (exporter.getId().startsWith(DefinitionQueryExporter.QUERY_DEFINTION_EXPORTER_ID)){
				definitionExporter = exporter;
			}
		}
		if (definitionExporter == null){
			throw new Exception("Could not find exporter to export query " + query.getName());
		}
		
		File tmpFile = File.createTempFile(SmartUtils.encodeHex(query.getUuid()), ".query");
		try{
			definitionExporter.export(query, tmpFile, new NullProgressMonitor());
			addFile(tmpFile, SmartUtils.encodeHex(query.getUuid()) + ".query", zipOut);
		}finally{
			tmpFile.delete();
		}
	}
	
	
	/**
	 * Adds the given file to the zip output stream
	 * 
	 * @param f
	 * @param zout
	 * @throws Exception
	 */
	private void addFile(File f, String name, ZipOutputStream zout) throws Exception{
		zout.putNextEntry(new ZipEntry(name));
		FileInputStream inStream = new FileInputStream(f);

		byte[] buffer = new byte[1024];
		int bytesRead;
		try {
			while ((bytesRead = inStream.read(buffer)) > 0) {
				zout.write(buffer, 0, bytesRead);
			}
		} finally {
			inStream.close();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.report.export.IReportExporter#getName()
	 */
	@Override
	public String getName() {
		return "Report Definition";
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.report.export.IReportExporter#getFormat()
	 */
	@Override
	public String getExportFormat() {
		return "zip";
	}

	@Override
	public boolean requiresParameters() {
		return false;
	}


}
