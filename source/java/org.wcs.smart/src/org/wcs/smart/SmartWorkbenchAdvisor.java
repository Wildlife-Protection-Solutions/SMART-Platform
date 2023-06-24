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
package org.wcs.smart;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IResolve;
import org.locationtech.udig.catalog.IService;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

/**
 * This workbench advisor creates the window advisor, and specifies the
 * perspective id for the initial window.
 */
public class SmartWorkbenchAdvisor extends WorkbenchAdvisor {

	@Override
	public void initialize(IWorkbenchConfigurer configurer) {
		super.initialize(configurer);
	}

	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		return new SmartWorkbenchWindowAdvisor(configurer);
	}

	public String getInitialWindowPerspectiveId() {
		if (SmartDB.isMultipleAnalysis()){
			return DefaultCrossCaPerspective.ID;
		}else{
			return DefaultPerspective.ID;
		}
	}
	
	/**
	 * Clears the catalog of all services before shutting down.  This ensures
	 * that services are not stored from one run to the next.
	 * 
	 */
	@Override
	public boolean preShutdown() {
		try {
			List<IResolve> resolves = CatalogPlugin.getDefault().getLocalCatalog().members(new NullProgressMonitor());
			for (IResolve r : resolves){
				if (r instanceof IService){
					try{
						CatalogPlugin.getDefault().getLocalCatalog().remove((IService) r);
					} catch (Exception e) {
						SmartPlugIn.log("Error clearning catalog plugin", e); //$NON-NLS-1$
					}
				}
			}
		} catch (IOException e) {
			SmartPlugIn.log("Error clearning catalog plugin", e); //$NON-NLS-1$
		}
		return true;
	}

	@Override
	public void postStartup() {
		//TODO: clean this up
		//run login handlers
		List<ILoginHandler> handlers = new ArrayList<ILoginHandler>();
		try{
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(ILoginHandler.LOGIN_EXT_ID);
			for (IConfigurationElement e : config) {	
				if (e.getName().equals("postWindowOpen")){ //$NON-NLS-1$
					ILoginHandler handler = (ILoginHandler) e.createExecutableExtension("class"); //$NON-NLS-1$
						handlers.add(handler);
				}
			}
		}catch (Exception ex){
			//String error = MessageFormat.format(Messages.SmartStartUp_CannotLogin + "\n\n" + "{1}.", ca.getName(), ex.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
			//SmartPlugIn.displayLog(error, ex);
			SmartPlugIn.displayLog("start up error", ex);
		}
			
		for (ILoginHandler h : handlers){
			try{
				h.onLogin();
			}catch (Exception ex){
				SmartPlugIn.displayLog("start up error", ex);
				//String error = MessageFormat.format(Messages.SmartStartUp_CannotLogin + "\n\n" + Messages.SmartStartUp_LoginHandlerError, ca.getName(), h.getClass().getName(), ex.getMessage()); //$NON-NLS-1$
				//SmartPlugIn.displayLog(error, ex);
				//return false;
			}
		}
	}
	
}

