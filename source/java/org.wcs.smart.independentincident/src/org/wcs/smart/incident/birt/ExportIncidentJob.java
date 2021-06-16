/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.incident.birt;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.birt.ui.ReportEngineManager;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.report.execute.SmartReportRunner;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Job for exporting a incidents to a pdf file
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class ExportIncidentJob extends Job {

	private Set<UUID> incidentUuids;
	private Path outputLocation;
	private int dpi;
	
	public ExportIncidentJob(Set<UUID> incidentUuids, Path outputDirectory) {
		super(Messages.ExportIncidentJob_jobname);
		this.incidentUuids = incidentUuids;
		this.outputLocation = outputDirectory;
		this.dpi = Display.getDefault().getDPI().x;
	}
	
	public ExportIncidentJob(UUID incidentUuid) {
		super(Messages.ExportIncidentJob_jobname);
		this.incidentUuids = Collections.singleton(incidentUuid);
		this.dpi = Display.getDefault().getDPI().x;
		this.outputLocation = null;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		if (outputLocation == null && incidentUuids.size() == 1) {
			try {
				Path outputFile = Files.createTempFile("smartincident", ".pdf"); //$NON-NLS-1$ //$NON-NLS-2$
				outputFile.toFile().deleteOnExit();
				
				HashMap<String, Object> params = new HashMap<>();
				UUID uuid = incidentUuids.iterator().next();
				params.put(IncidentDatasetParameterMetadata.UUID_PARAM_NAME, UuidUtils.uuidToString(uuid));
				
				try(OutputStream fout = Files.newOutputStream(outputFile);
						Session session = HibernateManager.openSession()){
					IRenderOption options = new RenderOption();
					options.setOutputStream(fout);
					options.setEmitterID("org.eclipse.birt.report.engine.emitter.pdf"); //$NON-NLS-1$
					options.setOption(HTMLRenderOption.IMAGE_DIRECTROY, outputFile.getParent());
					options.setSupportedImageFormats("PNG"); //$NON-NLS-1$
					
					SmartReportRunner.INSTANCE.runFile(IncidentBirtManager.INSTANCE.getIncidentTemplate(),
							SmartDB.getCurrentConservationArea(),
							SmartLabelProvider.getShortLabel(SmartDB.getCurrentEmployee()),
							ReportEngineManager.getBirtReportEngine(), 
							options, session, params, dpi);
					
					//launch the file
					//this has to be done in the display thread to work in linux.
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							AttachmentUtil.launch(outputFile);		
						}});
				}					
			}catch (Exception ex) {
				IncidentPlugIn.displayLog(ex.getMessage(), ex);
			}
		}else {
			
			
			try(Session session = HibernateManager.openSession()){
				Files.createDirectories(outputLocation);
				
				int cnt = 0;
				for (UUID uuid : incidentUuids) {
					HashMap<String, Object> params = new HashMap<>();
					params.put(IncidentDatasetParameterMetadata.UUID_PARAM_NAME, UuidUtils.uuidToString(uuid));
					
					Waypoint wp = session.get(Waypoint.class, uuid);
					if (wp == null) continue;
					
					String fname = wp.getId() + "-" + wp.getConservationArea().getId(); //$NON-NLS-1$
					Path output = outputLocation.resolve(URLUtils.cleanFilename(fname) +".pdf"); //$NON-NLS-1$
					try(OutputStream fout = Files.newOutputStream(output)){
						IRenderOption options = new RenderOption();
						options.setOutputStream(fout);
						options.setEmitterID("org.eclipse.birt.report.engine.emitter.pdf"); //$NON-NLS-1$
						options.setOption(HTMLRenderOption.IMAGE_DIRECTROY, outputLocation.getParent());
						options.setSupportedImageFormats("PNG"); //$NON-NLS-1$
						
						SmartReportRunner.INSTANCE.runFile(IncidentBirtManager.INSTANCE.getIncidentTemplate(),
								SmartDB.getCurrentConservationArea(),
								SmartLabelProvider.getShortLabel(SmartDB.getCurrentEmployee()),
								ReportEngineManager.getBirtReportEngine(), 
								options, session, params, dpi);
						
						cnt++;
					}
				}
				int fcnt = cnt;
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog.openInformation(Display.getDefault().getActiveShell(),
								Messages.ExportIncidentJob_ExportTitle, 
								MessageFormat.format(Messages.ExportIncidentJob_ExportOkMsg,fcnt,incidentUuids.size(),outputLocation.toString() ));
								
					}});
			}catch (Exception ex) {
				IncidentPlugIn.displayLog(ex.getMessage(), ex);
			}
		}
		
		//open in system editor
		return Status.OK_STATUS;
	}
	
	
}
