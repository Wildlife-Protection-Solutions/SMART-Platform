/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation {
 * files (the "Software"), to deal in
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.inject.Named;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.internal.ui.export.ParameterCollecter;
import org.wcs.smart.report.manger.ReportManager;
import org.wcs.smart.report.model.Report;

/**
 * Handler that runs all selected reports.
 * 
 * @author Emily
 *
 */
public class RunReportHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Execute
	public void execute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection, 
			Shell activeShell, IEclipseContext context){
		if (thisSelection == null || !(thisSelection instanceof IStructuredSelection) || ((IStructuredSelection)thisSelection).isEmpty() ){
			return;
		}
		
		IStructuredSelection ss = (IStructuredSelection)thisSelection;
		List<Report> reportsToRun = new ArrayList<Report>();
		for (Iterator<?> iterator = ss.iterator(); iterator.hasNext();) {
			Object type = (Object) iterator.next();
			if (type instanceof Report){
				reportsToRun.add((Report)type);
			}
		}
		if (reportsToRun.size() == 0){
			//nothing to run
			return;
		}
		HashMap<String, Object> reportParamseters = null;
		try{
			ParameterCollecter param = new ParameterCollecter();
			reportParamseters = param.getParameters(reportsToRun.toArray(new Report[reportsToRun.size()]));
			if (reportParamseters == null) return;
		}catch (Exception ex){
			ReportPlugIn.displayLog(Messages.RunReportHandler_ParametersError  + ex.getLocalizedMessage(), ex);
			return;
		}
		
		for (Report r : reportsToRun){
			ReportManager.viewReport(r, reportParamseters, context);
		}
	}
	
	public static class RunReportHandlerWrapper extends DIHandler<RunReportHandler>{
		public RunReportHandlerWrapper(){
			super(RunReportHandler.class);
		}
	}

}
