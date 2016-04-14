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
package org.wcs.smart.birt.ui;

import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.designer.ui.ReportPlugin;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.EngineConstants;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportEngineFactory;
import org.wcs.smart.SmartContext;

/**
 * Report engine manger for SMART
 * @author Emily
 * @since 2.0.0
 *
 */
public class ReportEngineManager {


	private static IReportEngine reportEngine;
	private static final Object lock = new Object(); 
	
	public static IReportEngine getBirtReportEngine(){
		if (reportEngine != null){
			return reportEngine;
		}
		synchronized (lock) {
			if (reportEngine != null){
				return reportEngine;
			}
			
			IReportEngineFactory factory = (IReportEngineFactory)Platform.createFactoryObject(IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY);
			EngineConfig config = new EngineConfig();
			config.setResourcePath(SmartContext.INSTANCE.getFilestoreLocation());
			config.getAppContext().put(EngineConstants.APPCONTEXT_CLASSLOADER_KEY,
					ReportEngineManager.class.getClassLoader());
			
			reportEngine = factory.createReportEngine(config);
			return reportEngine;
		}
	}
	
	/**
	 * Shuts down the BIRT report engine
	 */
	public static void endReportEngine(){
		synchronized (lock) {
			if (reportEngine != null){
				reportEngine.destroy();
				reportEngine = null;
			}
		}
	}
}
