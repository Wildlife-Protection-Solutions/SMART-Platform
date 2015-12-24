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
package org.wcs.smart.report.internal.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Named;

import org.eclipse.birt.report.engine.api.EmitterInfo;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.export.IExportFormat;
import org.wcs.smart.report.export.IReportExporter;
import org.wcs.smart.report.export.internal.ExportReportEngine;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.internal.ui.export.ExportReportWizard;
import org.wcs.smart.report.internal.ui.export.ExportReportWizardPage;
import org.wcs.smart.report.model.Report;
/**
 * Handler for exporting reports.
 * <p>Attempts to export all reports
 * in the current selection.</p>
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ExportReportHandler {

	private static final String MULTIPLE_PARAM = "org.wcs.smart.report.export.parameter.isMultiple"; //$NON-NLS-1$
	

	@Execute
	public void execute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection, 
			@Optional @Named(MULTIPLE_PARAM)String multipleParam, Shell activeShell){
		
		List<Report> selectedReports = new ArrayList<Report>();
		
		// find all selected reports
		if (thisSelection != null){
			for (Iterator<?> iterator = ((IStructuredSelection)thisSelection).iterator(); iterator.hasNext();) {
				Object type = (Object) iterator.next();
				if (type instanceof Report){
					selectedReports.add((Report)type);
				}
			}
		}
		boolean isMultiple = !(selectedReports.size() == 1);
		
		if (multipleParam != null && multipleParam.equalsIgnoreCase("true")){ //$NON-NLS-1$
			//this is from the main report->export reports menu item; we want users to pick reports
			isMultiple = true;
		}
		
		//get export location information
		ExportReportWizard exportWizard = new ExportReportWizard(isMultiple, selectedReports);
		WizardDialog wd = new WizardDialog(activeShell, exportWizard);
		wd.open();
//		if (dia.open() != Window.OK){
//			return ;
//		}
//		
//		selectedReports = dia.getSelectedReports();
//		IExportFormat format = dia.getOutputFormat();
//		File outputLocation = new File(dia.getOutputDir());
//		
//		
//		if (isMultiple){
//			//dir provided			
//			ExportReportEngine.validateDirectory(outputLocation);
//		}
//		if (format.getExporter() instanceof EmitterInfo){
//			EmitterInfo outputFormat = (EmitterInfo) format.getExporter();
//			//export reports
//			try {
//				if (isMultiple){
//					ExportReportEngine.exportReports(selectedReports, outputLocation, null, outputFormat);
//				}else{
//					ExportReportEngine.exportReports(selectedReports, null, outputLocation, outputFormat);
//				}
//			} catch (Exception e) {
//				ReportPlugIn.displayLog(ERROR_MSG + e.getLocalizedMessage(), e);
//			}
//		}else if (format.getExporter() instanceof IReportExporter){
//			IReportExporter exporter = (IReportExporter) format.getExporter();
//			try {
//				if (isMultiple){
//					ExportReportEngine.exportReports(selectedReports, outputLocation, null, exporter);
//				}else{
//					ExportReportEngine.exportReports(selectedReports, null, outputLocation, exporter);
//				}
//			} catch (Exception e) {
//				ReportPlugIn.displayLog(ERROR_MSG + e.getLocalizedMessage(), e);
//			}
//		}
	}
	
	public static class ExportReportHandlerWrapper extends AbstractHandler {

		private ExportReportHandler component;

		public ExportReportHandlerWrapper() {
			IEclipseContext context = getActiveContext();
			component = ContextInjectionFactory.make(ExportReportHandler.class, context);
		}

		private static IEclipseContext getActiveContext() {
			IEclipseContext parentContext = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
			return parentContext.getActiveLeaf();
		}
		
		@Override
		public Object execute(ExecutionEvent event) throws ExecutionException {
			//Default di handler does not add parameters into context
			IEclipseContext ctx = getActiveContext();
			ctx.set(MULTIPLE_PARAM, event.getParameter(MULTIPLE_PARAM));
			return ContextInjectionFactory.invoke(component, Execute.class, ctx);
		}

	}
}