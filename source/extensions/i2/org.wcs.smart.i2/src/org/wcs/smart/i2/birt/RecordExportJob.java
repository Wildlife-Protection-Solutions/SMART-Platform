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
package org.wcs.smart.i2.birt;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.eclipse.birt.report.engine.api.EmitterInfo;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.eclipse.birt.report.engine.api.impl.ReportEngine;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.birt.BirtConstants;
import org.wcs.smart.birt.SmartRunAndRender;
import org.wcs.smart.birt.ui.ReportEngineManager;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.datasource.DataSourceParameter;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Export intelligence record to file using BIRT.
 * 
 * @author Emily
 *
 */
public class RecordExportJob extends Job {

	private IntelRecord record;
	private EmitterInfo format;
	
	public RecordExportJob(IntelRecord record, EmitterInfo format) {
		super(Messages.RecordExportJob_JobName);
		this.record = record;
		this.format = format;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try{
			final Path out = runRecord();
			Display.getDefault().syncExec(() ->  AttachmentUtil.launch(out.toFile()));
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
		}		
							
		return Status.OK_STATUS;
	}

	
	@SuppressWarnings("unchecked")
	protected Path runRecord() throws Exception{
		if (record == null) throw new Exception("Record not loaded."); //$NON-NLS-1$
		//get the template
		Path reportFile = IntelReportManager.INSTANCE.getRecordTemplate(SmartDB.getCurrentConservationArea());
		if (reportFile == null || !Files.exists(reportFile)){
			throw new Exception(Messages.RecordExportJob_NoTemplate);
			
		}
		//create the pdf file output location
		Path outputFile = IntelReportManager.INSTANCE.getTemporaryDirectory();
		if (!Files.exists(outputFile)){
			try {
				Files.createDirectory(outputFile);
			} catch (IOException e) {
				throw new Exception(MessageFormat.format(Messages.RecordExportJob_DirectoryError, outputFile.toString()), e);
			}
		}

		
		String fileName = record.getTitle() + "." + (new SimpleDateFormat("MMMddyyyy")).format(new Date()); //$NON-NLS-1$ //$NON-NLS-2$
		fileName = URLUtils.cleanFilename(fileName);
		Path current = outputFile;
		outputFile = current.resolve(fileName + Messages.RecordExportJob_5 + format.getFormat());
		if (Files.exists(outputFile)){
			int cnt = 1;
			while(cnt < 1000){
				outputFile = current.resolve(fileName + "." + cnt + "." + format.getFormat()); //$NON-NLS-1$ //$NON-NLS-2$
				if (!Files.exists(outputFile)){
					break;
				}
				cnt++;
			}
		}
			
		
		IRenderOption options = new RenderOption();
		try(FileOutputStream fout = new FileOutputStream(outputFile.toFile())){
			
			options.setOutputStream(fout);
			options.setEmitterID(format.getID());
			options.setSupportedImageFormats("PNG"); //$NON-NLS-1$
			
			HashMap<String, Object> reportParameters = new HashMap<String, Object>();
			reportParameters.put(DataSourceParameter.RECORD_UUID.getName(), UuidUtils.uuidToString(record.getUuid()));
			
			Session session = HibernateManager.openSession();
			try{
				IReportRunnable design = ReportEngineManager.getBirtReportEngine().openReportDesign(reportFile.toAbsolutePath().toString());
				IRunAndRenderTask task = new SmartRunAndRender((ReportEngine) ReportEngineManager.getBirtReportEngine(), design, SmartDB.getCurrentConservationArea(), SmartLabelProvider.getShortLabel(SmartDB.getCurrentEmployee()));
				try{
					task.getAppContext().put(BirtConstants.CA_PARAM, SmartDB.getCurrentConservationArea());
					task.getAppContext().put(BirtConstants.SESSION_PARAM, session);
					task.setRenderOption(options);
					task.setParameterValues(reportParameters);
					task.run();
				}finally{
					task.close();
				}
				
			}finally{
				if (session.isOpen()) session.close();
			}
		}catch (Exception ex){
			throw new Exception(MessageFormat.format(Messages.RecordExportJob_ExportError, ex.getMessage(), ex));
		}
		
		//delete on exit
		outputFile.toFile().deleteOnExit();
		return outputFile;
		
	}
}
