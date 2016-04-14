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
package org.wcs.smart.intelligence.report;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.UUID;

import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.birt.ui.ReportEngineManager;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.report.execute.SmartReportRunner;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Job for exporting an intelligence to a pdf file for printing
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class ExportIntelligenceJob extends Job {
	
	private UUID uuid;
	private File outputFile;

	public ExportIntelligenceJob(UUID uuid) {
		super(Messages.ExportIntelligenceJob_Title);
		this.uuid = uuid;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		HashMap<String, Object> reportParameters = new HashMap<String, Object>();
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		Intelligence intelligence = null;
		try{
			intelligence = (Intelligence) session.load(Intelligence.class, uuid);
			String tmp = URLUtils.cleanFilename(intelligence.getName());
			tmp = String.format("%-3s",tmp).replace(' ', '_'); //$NON-NLS-1$
			outputFile = File.createTempFile(tmp + "_", ".pdf"); //$NON-NLS-1$ //$NON-NLS-2$
			outputFile.deleteOnExit();
			
			reportParameters.put(ReportIntelligence.UUID, UuidUtils.uuidToString(intelligence.getUuid()));
			reportParameters.put(ReportIntelligence.NAME, intelligence.getName());
			reportParameters.put(ReportIntelligence.DESCRIPTION, (intelligence.getDescription()==null?"" : intelligence.getDescription())); //$NON-NLS-1$
			reportParameters.put(ReportIntelligence.SOURCE, (intelligence.getSource() != null ? intelligence.getSource().getName() : "")); //$NON-NLS-1$
			reportParameters.put(ReportIntelligence.PATROL_ID,  (intelligence.getPatrol() != null ? intelligence.getPatrol().getId() : Messages.IntelligenceEditor_NoValue));
			reportParameters.put(ReportIntelligence.RECEIVED_DATE, new java.sql.Date(intelligence.getReceivedDate().getTime()));
			reportParameters.put(ReportIntelligence.FROM_DATE, new java.sql.Date(intelligence.getFromDate().getTime()));
			reportParameters.put(ReportIntelligence.TO_DATE, new java.sql.Date(intelligence.getToDate() != null ? intelligence.getToDate().getTime() : intelligence.getFromDate().getTime()));
			reportParameters.put(ReportIntelligence.LOCATION, intelligence.getPoints());
			reportParameters.put(ReportIntelligence.CREATOR, intelligence.getCreator() == null ? "" : SmartLabelProvider.getFullLabel(intelligence.getCreator())); //$NON-NLS-1$
			
			try(FileOutputStream fout = new FileOutputStream(outputFile)){
				IRenderOption options = new RenderOption();
				options.setOutputStream(fout);
				options.setEmitterID("org.eclipse.birt.report.engine.emitter.pdf"); //$NON-NLS-1$
				options.setOption(HTMLRenderOption.IMAGE_DIRECTROY, outputFile.getParent());
				options.setSupportedImageFormats("PNG"); //$NON-NLS-1$
				
				SmartReportRunner.INSTANCE.runFile(ReportIntelligence.getIntelligenceTemplate(), 
						SmartDB.getCurrentConservationArea(), 
						SmartLabelProvider.getShortLabel(SmartDB.getCurrentEmployee()),
						ReportEngineManager.getBirtReportEngine(),
						options, session, reportParameters);
				
			}
		} catch (Exception ex) {
			IntelligencePlugIn.displayLog(Messages.ExportIntelligenceJob_ExportError + "\n\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
			return Status.CANCEL_STATUS;
		} finally {
			session.getTransaction().rollback();
			session.close();
		}

		
		//launch the file
		//this has to be done in the display thread to work in linux.
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				AttachmentUtil.launch(outputFile);		
			}});
		
		
		//open in system editor
		return Status.OK_STATUS;
	}

}
