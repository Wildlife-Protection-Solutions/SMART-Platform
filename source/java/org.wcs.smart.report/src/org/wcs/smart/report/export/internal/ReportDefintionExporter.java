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

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.SessionHandle;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.Label;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.importexport.QueryExportEngine;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.export.IReportExporter;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.manger.ReportManager;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.Zipper;

/**
 * Report exporter that exports the report
 * definition and associated query files.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ReportDefintionExporter implements IReportExporter {

	private static final String QUERYFILE_EXTENSION = ".query"; //$NON-NLS-1$
	
	public static final String VERSION_2 = "2"; //$NON-NLS-1$
	
	public static final String VERSION_KEY = "version"; //$NON-NLS-1$
	
	public static final String FILENAME_KEY = "filename"; //$NON-NLS-1$

	public ReportDefintionExporter() {
	}

	/**
	 * Exports each report to a different zip file
	 */
	@Override
	public void exportReport(Path file, Report report, Map<String, Object> reportParams, IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.ReportDefintionExporter_Progress_Exporting, 3);
		
		if (!SmartUtils.createDirectory(file.getParent())) return;
		
		Zipper zipper = Zipper.create(file);
		
		try{
			
			//export queries
			monitor.subTask(Messages.ReportDefintionExporter_Progress_Queries);
			exportQueries(zipper, report);
			monitor.worked(1);

			//add report file
			monitor.subTask(Messages.ReportDefintionExporter_Progress_Definition);
			zipper.addFile(ReportPlugIn.getDefault().getReportFile(report), report.getFilename());
			monitor.worked(1);
			
			//add report info file
			monitor.subTask(Messages.ReportDefintionExporter_Progress_ReportInfo);
			
			String filename = ExportReportEngine.getOutputFileName(report, null, "rpt").getFileName().toString(); //$NON-NLS-1$
			Path reportInfo = Files.createTempFile(filename,".tmp"); //$NON-NLS-1$ 
			try{
				writeReportInfo(reportInfo, report);
				zipper.addFile(reportInfo, filename);
			}finally{
				Files.delete(reportInfo);
			}
			monitor.worked(1);
		}finally {
			zipper.close();
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
	private void writeReportInfo(Path f, Report report) throws Exception{
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try {
				report = HibernateManager.saveOrMerge(s, report);
				Properties prop = new Properties();
				for (Label l : report.getNames()) {
					prop.setProperty(
							"name_" + l.getLanguage().getCode(), l.getValue()); //$NON-NLS-1$
				}
				prop.setProperty(FILENAME_KEY, report.getFilename());
				prop.setProperty(VERSION_KEY, VERSION_2);
				try(OutputStream fout = Files.newOutputStream(f)){
					prop.store(fout, null);
				}
			} finally {
				s.getTransaction().rollback();
			}
		}
	}
	
	/**
	 * Exports the queries into the report and add the results
	 * to the zip file.
	 * 
	 * @param zout 
	 * @param report
	 * @throws Exception
	 */
	private void exportQueries(Zipper zipper, Report report) throws Exception{
		
		Path reportFile = ReportPlugIn.getReportDirectory(report.getConservationArea()).resolve(report.getFilename());

		SessionHandle session = SessionHandleAdapter.getInstance().getSessionHandle();

		ReportDesignHandle rdh = session.openDesign(reportFile.toAbsolutePath().toString());
		HashSet<String> processedQueries = new HashSet<String>();
		List<?> datasets = rdh.getDataSets().getContents();
		for (Iterator<?> iterator = datasets.iterator(); iterator.hasNext();) {
			DataSetHandle dataset = (DataSetHandle) iterator.next();
			if (dataset instanceof OdaDataSetHandle){
				if (ReportManager.isSmartQueryHandle((OdaDataSetHandle) dataset)){
					String queryUuid = ((OdaDataSetHandle) dataset).getQueryText().split(":")[1]; //$NON-NLS-1$
					if (!processedQueries.contains(queryUuid)){
						processedQueries.add(queryUuid);
						IQueryType qType = QueryTypeManager.INSTANCE.findQueryType((((OdaDataSetHandle) dataset).getQueryText().split(":")[0])); //$NON-NLS-1$
						UUID uuid = UuidUtils.stringToUuid(queryUuid);
						Query smartQuery = null;
						try(Session hsession = HibernateManager.openSession()){
							hsession.beginTransaction();
							try{
								smartQuery = QueryHibernateManager.getInstance().findQuery(hsession, uuid, qType);
							}finally{
								hsession.getTransaction().commit();
							}
						}	
					
						if (smartQuery == null) throw new Exception(Messages.ReportDefintionExporter_Error_LoadingQueryDef);
						exportQuery(smartQuery, zipper);
					}
				}
			}
		}
		rdh.close();
		
	}
	
	/**
	 * Exports a query 
	 * @param query
	 * @param zipOut
	 * @throws Exception
	 */
	private void exportQuery(Query query, Zipper zipper) throws Exception{
		List<IQueryExporter> queryExports = QueryExportEngine.getQueryExports(query);
		IQueryExporter definitionExporter = null;
		for (IQueryExporter exporter : queryExports){
			if (exporter.getId().startsWith(IQueryExporter.QUERY_DEFINTION_EXPORTER_ID)){
				definitionExporter = exporter;
			}
		}
		if (definitionExporter == null){
			throw new Exception(MessageFormat.format(Messages.ReportDefintionExporter_Error_NoExporter, new Object[]{ query.getName()}));
		}
		
		Path tmpFile = Files.createTempFile(UuidUtils.uuidToString(query.getUuid()), QUERYFILE_EXTENSION);
		try{
			definitionExporter.export(query, null, tmpFile, null, new NullProgressMonitor());
			zipper.addFile(tmpFile, UuidUtils.uuidToString(query.getUuid()) + QUERYFILE_EXTENSION);
		}finally{
			Files.delete(tmpFile);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.report.export.IReportExporter#getName()
	 */
	@Override
	public String getName() {
		return Messages.ReportDefintionExporter_ExporterName;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.report.export.IReportExporter#getFormat()
	 */
	@Override
	public String getExportFormat() {
		return "zip"; //$NON-NLS-1$
	}

	@Override
	public boolean requiresParameters() {
		return false;
	}


}
