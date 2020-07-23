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
package org.wcs.smart.plan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.report.ReportPlan;
import org.wcs.smart.util.SmartUtils;

/**
 * Template cloner than copies the plan printing template
 * to the new conservation area.
 * 
 * @author Emily
 *
 */
public class PlanTemplateCloner implements
		IConservationAreaTemplateCloner {

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine, IProgressMonitor monitor) throws Exception {
		SubMonitor.convert(monitor, Messages.PlanTemplateCloner_Progress, 1);
		
		//need to clone: the plan template
		Path f = Paths.get(engine.getTemplateCa().getFileDataStoreLocation()).resolve(SmartPlanPlugIn.PLAN_DIR);
		Path templateFile = f.resolve(ReportPlan.PLAN_TEMPLATE);
		
		if (Files.exists(templateFile)){
			//copy plan template
			f = Paths.get(engine.getNewCa().getFileDataStoreLocation()).resolve(SmartPlanPlugIn.PLAN_DIR);
			Path newFile = f.resolve(ReportPlan.PLAN_TEMPLATE);
			SmartUtils.copyFile(templateFile, newFile);
		}
		
	}

}
