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

import org.eclipse.birt.report.engine.api.EmitterInfo;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.export.IExportFormat;
import org.wcs.smart.report.export.IReportExporter;
import org.wcs.smart.report.export.internal.ExportReportEngine;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.internal.ui.export.ExportReportDialog;
import org.wcs.smart.report.model.Report;
/**
 * Handler for exporting reports.
 * <p>Attempts to export all reports
 * in the current selection.</p>
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ExportReportHandler extends AbstractHandler implements IHandler {

	private static final String ERROR_MSG = Messages.ExportReportHandler_ExportError;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection thisSelection = HandlerUtil.getCurrentSelection(event);
		if (thisSelection == null || thisSelection.isEmpty() || !(thisSelection instanceof IStructuredSelection) ){
			return null;
		}
		
		// find all selected reports
		List<Report> selectedReports = new ArrayList<Report>();
		for (Iterator<?> iterator = ((IStructuredSelection)thisSelection).iterator(); iterator.hasNext();) {
			Object type = (Object) iterator.next();
			if (type instanceof Report){
				selectedReports.add((Report)type);
			}
		}

		if (selectedReports.size() == 0){
			//nothing to export
			return null;
		}
//		if (true){
//			Report r = selectedReports.get(0);
//			try{
//			ExportReportEngine.printReport(r);
//			}catch (Exception ex){
//				ex.printStackTrace();
//			}
//			return null;
//		}
		
		
		//get export location information
		ExportReportDialog dia = new ExportReportDialog(HandlerUtil.getActiveShell(event), selectedReports.size() == 1 ? selectedReports.get(0) : null);
		if (dia.open() != Window.OK){
			return null;
		}
		
		IExportFormat format = dia.getOutputFormat();
		File outputDir = new File(dia.getOutputDir());
		
		
		if (selectedReports.size() > 1){
			//dir provided			
			ExportReportEngine.validateDirectory(outputDir);
		}
		if (format.getExporter() instanceof EmitterInfo){
			EmitterInfo outputFormat = (EmitterInfo) format.getExporter();
			//export reports
			try {
				ExportReportEngine.exportReports(selectedReports, outputDir, outputFormat);
			} catch (Exception e) {
				ReportPlugIn.displayLog(ERROR_MSG + e.getLocalizedMessage(), e);
			}
		}else if (format.getExporter() instanceof IReportExporter){
			IReportExporter exporter = (IReportExporter) format.getExporter();
			try {
				ExportReportEngine.exportReports(selectedReports, outputDir, exporter);
			} catch (Exception e) {
				ReportPlugIn.displayLog(ERROR_MSG + e.getLocalizedMessage(), e);
			}
		}
		
		
		return null;
	}
}