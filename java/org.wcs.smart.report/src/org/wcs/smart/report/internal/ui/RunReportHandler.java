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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
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
public class RunReportHandler extends AbstractHandler  {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event)
			throws ExecutionException {
		
		
		ISelection s = HandlerUtil.getCurrentSelection(event);
		if (!(s instanceof StructuredSelection)){
			return null;
		}
		
		IStructuredSelection ss = (IStructuredSelection)s;
		List<Report> reportsToRun = new ArrayList<Report>();
		for (Iterator<?> iterator = ss.iterator(); iterator.hasNext();) {
			Object type = (Object) iterator.next();
			if (type instanceof Report){
				reportsToRun.add((Report)type);
			}
		}
		if (reportsToRun.size() == 0){
			//nothing to run
			return null;
		}
		HashMap<String, Object> reportParamseters = null;
		try{
			ParameterCollecter param = new ParameterCollecter();
			reportParamseters = param.getParameters(reportsToRun.toArray(new Report[reportsToRun.size()]));
			if (reportParamseters == null){
				//cancel pressed
				return null;
			}
		}catch (Exception ex){
			ReportPlugIn.displayLog(Messages.RunReportHandler_ParametersError  + ex.getMessage(), ex);
			return null;
		}
		
		
		for (Report r : reportsToRun){
			ReportManager.viewReport(r, reportParamseters);
		}
		
		return null;
	}

}
