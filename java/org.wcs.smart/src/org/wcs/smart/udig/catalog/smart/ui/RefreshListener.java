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
package org.wcs.smart.udig.catalog.smart.ui;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.ID;
import org.locationtech.udig.catalog.IResolve;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.ILoginHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.udig.catalog.smart.SmartGeoResource;
import org.wcs.smart.udig.catalog.smart.SmartService;
import org.wcs.smart.udig.catalog.smart.SmartServiceExtension;

/**
 * Smart service refresh listener.
 * 
 * @author Emily
 *
 */
public class RefreshListener implements ILoginHandler{

	//listeners to ensure service is refreshed when data is modified
	private IEventBroker eb ;
	
	private ID serviceUrl;
	private EventHandler dataChangeEventHandler = new EventHandler(){
		@Override
		public void handleEvent(Event event) {
			NullProgressMonitor monitor = new NullProgressMonitor();
			SmartService service = CatalogPlugin.getDefault().getLocalCatalog().getById(SmartService.class,
					serviceUrl, monitor);
			if (service == null) return;
			try{
				for (IResolve r : service.members(monitor)){
					if (r.canResolve(SmartGeoResource.class)){
						r.resolve(SmartGeoResource.class, monitor).reset();
					}
				}
			}catch (Exception ex){
				SmartPlugIn.displayLog(Messages.RefreshListener_MapLayerRefreshError, ex);
			}
		}
	};
	
	public RefreshListener(){

	}
	
	public void dispose(){
		eb.unsubscribe(dataChangeEventHandler);
	}

	@Override
	public void onLogin() throws Exception {
		//on reset info
		IEclipseContext ctx =(IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		eb = (IEventBroker) ctx.get(IEventBroker.class.getName());
		eb.subscribe(SmartPlugIn.E4_DATABASE_CHANGED_EVENT, null, dataChangeEventHandler, true);

		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(SmartServiceExtension.CA_UUID_KEY, SmartDB.getCurrentConservationArea().getUuid());
		serviceUrl = new ID(SmartServiceExtension.createURL(params));
		
		//dispose on shutdown
		PlatformUI.getWorkbench().addWorkbenchListener(new IWorkbenchListener() {
			@Override
			public boolean preShutdown(IWorkbench workbench, boolean forced) {
				return true;
			}
			
			@Override
			public void postShutdown(IWorkbench workbench) {
				dispose();
			}
		});
	}
	
}
