/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Runs an entity export.
 * 
 * @author Emily
 *
 */
public class EntityExportReportJob extends Job {

	private IntelEntity entity;
	private Date[] dFilter;
	private EmitterInfo format;
	
	public EntityExportReportJob(IntelEntity entity, Date[] dFilter, EmitterInfo format) {
		super("Running BIRT Report");
		this.entity = entity;
		this.dFilter = dFilter;
		this.format = format;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try{
			final Path out = runEntityType(entity, dFilter);
			Display.getDefault().syncExec(() ->  AttachmentUtil.launch(out.toFile()));
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
		}		
							
		return Status.OK_STATUS;
	}

	
	protected Path runEntityType(IntelEntity entity, Date[] dateFilter) throws Exception{
		
		//get the template
		Path reportFile = IntelReportManager.INSTANCE.getEntityTemplate(entity.getEntityType());
		if (reportFile == null || !Files.exists(reportFile)){
			throw new Exception(MessageFormat.format("BIRT template is not configured for the entity type {0}.  You must configure the template before you can export the entity.", entity.getEntityType().getName()));
			
		}
		//create the pdf file output location
		Path outputFile = IntelReportManager.INSTANCE.getTemporaryDirectory();
		if (!Files.exists(outputFile)){
			try {
				Files.createDirectory(outputFile);
			} catch (IOException e) {
				throw new Exception(MessageFormat.format("Unable to create directory {0} to generate pdf file to.", outputFile.toString()), e);
			}
		}

		String fileName = entity.getIdAttributeAsText();
		fileName = URLUtils.cleanFilename(fileName);
		Path current = outputFile;
		outputFile = current.resolve(fileName + "." + format.getFormat());
		if (Files.exists(outputFile)){
			int cnt = 1;
			while(cnt < 1000){
				outputFile = current.resolve(fileName + "." + cnt + "." + format.getFormat());
				if (!Files.exists(outputFile)){
					break;
				}
			}
		}
			
		
		IRenderOption options = new RenderOption();
		try(FileOutputStream fout = new FileOutputStream(outputFile.toFile())){
			
			options.setOutputStream(fout);
			options.setEmitterID(format.getID());
			options.setSupportedImageFormats("PNG"); //$NON-NLS-1$
			
			HashMap<String, Object> reportParameters = new HashMap<String, Object>();
			reportParameters.put(DataSourceParameter.ENTITY_UUID.getName(), UuidUtils.uuidToString(entity.getUuid()));
			if (dateFilter != null && dateFilter.length == 2){
				reportParameters.put(DataSourceParameter.START_DATE.getName(), dateFilter[0]);
				reportParameters.put(DataSourceParameter.END_DATE.getName(), dateFilter[1]);
			}
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
			throw new Exception(MessageFormat.format("Unable to export entity. {0}", ex.getMessage(), ex));
		}
		
		//delete on exit
		outputFile.toFile().deleteOnExit();
		return outputFile;
		
	}
}
