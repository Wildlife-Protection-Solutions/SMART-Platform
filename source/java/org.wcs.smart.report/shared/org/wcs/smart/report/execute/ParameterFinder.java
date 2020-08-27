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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
	public List<IParameterDefnBase> getParameters(Report report, IReportEngine engine) throws Exception{
		List<IParameterDefnBase> allParameters = new ArrayList<>();
		Path reportFile = report.getFullPath();
		final IReportRunnable design = engine.openReportDesign(reportFile.toAbsolutePath().toString());
		final IGetParameterDefinitionTask paramDefnTask = engine.createGetParameterDefinitionTask( design );
		Collection<?> parameters = paramDefnTask.getParameterDefns(true);
		
		for (Iterator<?> iterator = parameters.iterator(); iterator.hasNext();) {
			IParameterDefnBase param = (IParameterDefnBase)iterator.next();
			allParameters.add(param);			
		}
		return allParameters;
		
	}

}
