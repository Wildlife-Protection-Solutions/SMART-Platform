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
package org.wcs.smart.report.internal.ui.export;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.birt.report.engine.api.IGetParameterDefinitionTask;
import org.eclipse.birt.report.engine.api.IParameterDefn;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.report.internal.ui.viewer.parameter.DateParameter;
import org.wcs.smart.report.internal.ui.viewer.parameter.ReportParameterDialog;
import org.wcs.smart.report.manger.ReportManager;
import org.wcs.smart.report.model.Report;

/**
 * Parameter collector that prompts the user to enter all parameters
 * required for a collection of reports.
 * <p>If two reports have the same parameter name but different parameter
 * type an exception is thrown. Each parameter name must have a unique type.
 * </p>
 * <p>
 * Only one value can be provided for each parameter name.
 * </p>
 * @author egouge
 * @since 1.0.0
 */
public class ParameterCollecter {

	private HashMap<String, IParameterDefn> allParameters = null;
	private HashMap<String, Object> paramValues = null;
	
	/**
	 * Gets all parameters associated with the given reports.
	 * @param reports reports to collect parameters for
	 * @return a map of parameter name to parameter value or null if cancelled
	 * @throws Exception
	 */
	public  HashMap<String, Object> getParameters(Report[] reports) throws Exception{
		allParameters = new HashMap<String, IParameterDefn>();
		for (int i = 0; i < reports.length; i++){
			getParameters(reports[i]);
		}
		displayParameters();
		return paramValues;
	}
	
	/*
	 * Determines all parameters for a given report
	 * and adds them to the local report collection.
	 */
	private void getParameters(Report r) throws Exception{
		
		IReportEngine engine = ReportManager.getReportEngine();
		
		final IReportRunnable design = engine.openReportDesign(r.getFullReportFilename().getAbsolutePath());
		
		final IGetParameterDefinitionTask paramDefnTask = engine.createGetParameterDefinitionTask( design );
		Collection<?> parameters = paramDefnTask.getParameterDefns(false);
		for (Iterator<?> iterator = parameters.iterator(); iterator.hasNext();) {
			final IParameterDefn param = (IParameterDefn) iterator.next();
			IParameterDefn def = allParameters.get(param.getName());
			if (def == null){
				allParameters.put(param.getName(), param);	
			}else{
				if (def.getDataType() != param.getDataType()){
					throw new Exception("Reports contain parameters with the same name (" + param.getName() + ") but require different datatypes.  These reports cannot be run at the same time.");
				}
			}
		}
		
	}
	
	/*
	 * Displays a parameter dialog to the user where the user can enter
	 * values.
	 */
	private void displayParameters() {
		final ReportParameterDialog dialog = new ReportParameterDialog(Display
				.getDefault().getActiveShell());

		for (Iterator<IParameterDefn> iterator = allParameters.values()
				.iterator(); iterator.hasNext();) {
			IParameterDefn type = (IParameterDefn) iterator.next();
			
			if (type.getDataType() == IParameterDefn.TYPE_DATE) {
				dialog.addComponent(new DateParameter(type.getName(), type.getDisplayName()));
			}
			//TODO: add support for other types
		}

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				paramValues = null;
				if (dialog.open() == Window.OK) {
					paramValues = dialog.getValues();
				}

			}
		});
	}
	
}
