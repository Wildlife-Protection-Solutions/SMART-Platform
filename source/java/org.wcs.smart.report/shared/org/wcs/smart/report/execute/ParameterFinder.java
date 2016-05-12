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
package org.wcs.smart.report.execute;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.birt.report.engine.api.IGetParameterDefinitionTask;
import org.eclipse.birt.report.engine.api.IParameterDefnBase;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.wcs.smart.report.model.Report;

/**
 * Reads a report and finds all parameters.
 * 
 * @author Emily
 *
 */
public enum ParameterFinder {
	
	INSTANCE;
	
	/*
	 * Determines all parameters for a given report
	 * and adds them to the local report collection.
	 */
	public HashMap<String, IParameterDefnBase> getParameters(Report report, IReportEngine engine) throws Exception{
		HashMap<String, IParameterDefnBase> allParameters = new HashMap<String, IParameterDefnBase>();
		
		File reportFile = new File(report.getConservationArea().getFileDataStoreLocation()
				+ File.separator
				+ Report.REPORT_DIR + File.separator + report.getFilename());
		
		final IReportRunnable design = engine.openReportDesign(reportFile.getAbsolutePath());
		final IGetParameterDefinitionTask paramDefnTask = engine.createGetParameterDefinitionTask( design );
		Collection<?> parameters = paramDefnTask.getParameterDefns(true);
		
		for (Iterator<?> iterator = parameters.iterator(); iterator.hasNext();) {
			IParameterDefnBase param = (IParameterDefnBase)iterator.next();
			IParameterDefnBase def = allParameters.get(param.getName());
			if (def == null){
				allParameters.put(param.getName(), param);	
			}else{
				if (def.getParameterType() != param.getParameterType()){
					throw new Exception(MessageFormat.format("Reports contain parameters with the same name ({0}) but require different parameter types.  These reports cannot be run at the same time.", new Object[]{param.getName()}));
				}
				//TODO: implement more here to make sure they are the same
			}
			
		}
		return allParameters;
		
	}

}
