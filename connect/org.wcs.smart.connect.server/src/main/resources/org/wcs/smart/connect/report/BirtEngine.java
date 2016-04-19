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
package org.wcs.smart.connect.report;

import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;

import javax.servlet.ServletContext;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.framework.IPlatformContext;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.core.framework.PlatformServletContext;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.EngineConstants;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportEngineFactory;
import org.locationtech.udig.project.internal.ProjectPlugin;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.report.udig.CatalogPluginWrapper;
import org.wcs.smart.connect.report.udig.ProjectPluginWrapper;
import org.wcs.smart.connect.report.udig.ShpPluginWrapper;
import org.wcs.smart.connect.report.udig.UdigPreferenceStore;
import org.wcs.smart.connect.report.udig.UiPluginWrapper;
import org.wcs.smart.data.oda.smart.impl.ISmartConnectionFactory;
import org.wcs.smart.udig.catalog.smart.IDatabaseConnectionProvider;

/**
 * BIRT Report Engine;
 * 
 * @author Emily
 *
 */
public class BirtEngine {

	private static IReportEngine birtEngine = null;

	private static Properties configProps = new Properties();

	public static synchronized IReportEngine getBirtEngine(ServletContext sc) {
		if (birtEngine == null) {
			SmartContext.INSTANCE.setClass(ISmartConnectionFactory.class, new ConnectionFactory());
			EngineConfig config = new EngineConfig();
			if (configProps != null) {
				String logLevel = configProps.getProperty("logLevel");
				Level level = Level.OFF;
				if ("SEVERE".equalsIgnoreCase(logLevel)) {
					level = Level.SEVERE;
				} else if ("WARNING".equalsIgnoreCase(logLevel)) {
					level = Level.WARNING;
				} else if ("INFO".equalsIgnoreCase(logLevel)) {
					level = Level.INFO;
				} else if ("CONFIG".equalsIgnoreCase(logLevel)) {
					level = Level.CONFIG;
				} else if ("FINE".equalsIgnoreCase(logLevel)) {
					level = Level.FINE;
				} else if ("FINER".equalsIgnoreCase(logLevel)) {
					level = Level.FINER;
				} else if ("FINEST".equalsIgnoreCase(logLevel)) {
					level = Level.FINEST;
				} else if ("OFF".equalsIgnoreCase(logLevel)) {
					level = Level.OFF;
				}

				config.setLogConfig(configProps.getProperty("logDirectory"),
						level);
			}
			config.setResourcePath(SmartContext.INSTANCE.getFilestoreLocation());
			config.getAppContext().put(
					EngineConstants.APPCONTEXT_CLASSLOADER_KEY,
					Thread.currentThread().getContextClassLoader());
			// if you are using 3.7 POJO Runtime no need to setEngineHome
			//config.setEngineHome("");
			IPlatformContext context = new PlatformServletContext(sc);
			config.setPlatformContext(context);
			
			try {
				Platform.startup(config);
			} catch (BirtException e) {
				e.printStackTrace();
			}
			
			IReportEngineFactory factory = (IReportEngineFactory) Platform
					.createFactoryObject(IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY);
			birtEngine = factory.createReportEngine(config);
			
			configureUdig(sc);

		}
		return birtEngine;
	}

	public static void configureUdig(ServletContext context){
		//TODO: we only need to call this code once put in birt setup
		SmartContext.INSTANCE.setClass(IDatabaseConnectionProvider.class, new ConnectConnectionProvider(context, Locale.getDefault()));
		
		new ProjectPluginWrapper();
//		new UdigPreferenceStore();
		new CatalogPluginWrapper();
		new ShpPluginWrapper();
		new UiPluginWrapper();
	}
	
	
	public static synchronized void destroyBirtEngine() {
		if (birtEngine == null) {
			return;
		}
		birtEngine.destroy();
		Platform.shutdown();
		birtEngine = null;
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}


}