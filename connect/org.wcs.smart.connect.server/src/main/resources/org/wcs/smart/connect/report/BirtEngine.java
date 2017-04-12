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
import org.eclipse.core.internal.registry.RegistryProviderFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.locationtech.udig.catalog.internal.wms.WmsPlugin;
import org.locationtech.udig.render.wms.basic.WMSPlugin;
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
				String logLevel = configProps.getProperty("logLevel"); //$NON-NLS-1$
				Level level = Level.OFF;
				if ("SEVERE".equalsIgnoreCase(logLevel)) {  //$NON-NLS-1$
					level = Level.SEVERE;
				} else if ("WARNING".equalsIgnoreCase(logLevel)) {  //$NON-NLS-1$
					level = Level.WARNING;
				} else if ("INFO".equalsIgnoreCase(logLevel)) { //$NON-NLS-1$
					level = Level.INFO;
				} else if ("CONFIG".equalsIgnoreCase(logLevel)) { //$NON-NLS-1$
					level = Level.CONFIG;
				} else if ("FINE".equalsIgnoreCase(logLevel)) { //$NON-NLS-1$
					level = Level.FINE;
				} else if ("FINER".equalsIgnoreCase(logLevel)) { //$NON-NLS-1$
					level = Level.FINER;
				} else if ("FINEST".equalsIgnoreCase(logLevel)) { //$NON-NLS-1$
					level = Level.FINEST;
				} else if ("OFF".equalsIgnoreCase(logLevel)) { //$NON-NLS-1$
					level = Level.OFF;
				}
				config.setLogConfig(configProps.getProperty("logDirectory"),level); //$NON-NLS-1$
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

		SmartContext.INSTANCE.setClass(IDatabaseConnectionProvider.class, new ConnectConnectionProvider(context, Locale.getDefault()));
		
		//startup required udig plugins
		new ProjectPluginWrapper();
		new CatalogPluginWrapper();
		new ShpPluginWrapper();
		new UiPluginWrapper();

		new WmsPlugin(){
			@Override
			public ImageDescriptor getGridObjectImage(){
		    	return null;
		    }
			@Override
		    public ImageDescriptor getGridMissingImage(){
		    	return null;
		    }
		};
		
		new WMSPlugin(){
			private UdigPreferenceStore localPreference;
			
			@Override
		    public ScopedPreferenceStore getPreferenceStore() {
				if (localPreference == null){
					localPreference = new UdigPreferenceStore();	
					localPreference.setValue(
							org.locationtech.udig.render.wms.basic.preferences.PreferenceConstants.P_USE_DEFAULT_ORDER, Boolean.FALSE);
					localPreference.setValue(
							org.locationtech.udig.render.wms.basic.preferences.PreferenceConstants.P_IMAGE_TYPE_ORDER, "image/png,image/png8,image/gif,image/tiff,image/bmp,image/jpeg"); //$NON-NLS-1$
								
				}
				return localPreference;
			}
		};
	}	
	
	
	public static synchronized void destroyBirtEngine() {
		if (birtEngine == null) {
			return;
		}
		birtEngine.destroy();
		Platform.shutdown();
		birtEngine = null;
		
		RegistryProviderFactory.releaseDefault();
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}


}