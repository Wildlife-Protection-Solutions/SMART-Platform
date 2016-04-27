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
package org.wcs.smart.connect.report.udig;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.ICatalog;
import org.locationtech.udig.catalog.ID;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IResolve;
import org.locationtech.udig.catalog.IResolveChangeListener;
import org.locationtech.udig.catalog.IResolveManager;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.ServiceExtension;
import org.locationtech.udig.catalog.internal.ResolveManager2;
import org.locationtech.udig.core.internal.ExtensionPointProcessor;
import org.locationtech.udig.core.internal.ExtensionPointUtil;

import com.vividsolutions.jts.geom.Envelope;

public class CatalogPluginWrapper extends CatalogPlugin {
    /** Lock used to protect map of available services; for the last call? */
    private Lock registeredLock = new ReentrantLock();
    /**
     * Map of ServiceExtension by "id", access control policed by above "lock".
     */
    protected Map<String, ServiceExtension> registered = null; // lazy creation
    
    private IResolveManager resolveManager;
    
	ICatalog defaultCat;
	
	@Override
	public ICatalog getLocalCatalog() {
		if (defaultCat == null){
			//empty catalog that does nothing
			defaultCat = new ICatalog() {
				
				@Override
				public String getTitle() {
					return null;
				}
				
				@Override
				public org.locationtech.udig.catalog.IResolve.Status getStatus() {
					return null;
				}
				
				@Override
				public Throwable getMessage() {
					return null;
				}
				
				@Override
				public URL getIdentifier() {
					return null;
				}
				
				@Override
				public ID getID() {
					return null;
				}
				
				@Override
				public <T> boolean canResolve(Class<T> adaptee) {
					return false;
				}
				
				@Override
				public List<IResolve> search(String pattern, Envelope bbox,
						IProgressMonitor monitor) throws IOException {
					return null;
				}
				
				@Override
				public <T extends IResolve> T getById(Class<T> type, ID id,
						IProgressMonitor monitor) {
					return null;
				}
				
				@Override
				public List<IResolve> find(ID resourceId, IProgressMonitor monitor) {
					return null;
				}
				
				@Override
				public <T> T resolve(Class<T> adaptee, IProgressMonitor monitor)
						throws IOException {
					return null;
				}
				
				@Override
				public void replace(ID id, IService replacement)
						throws UnsupportedOperationException {
				}
				
				@Override
				public void removeListener(IResolveChangeListener listener) {
				}
				
				@Override
				public void remove(IService service) throws UnsupportedOperationException {
				}
				
				@Override
				public void addListener(IResolveChangeListener listener) {	
				}
				
				@Override
				public IService add(IService service) throws UnsupportedOperationException {
					return null;
				}
				
				@Override
				public IService acquire(URL url, IProgressMonitor monitor)
						throws IOException {
					return null;
				}
				
				@Override
				public IService acquire(Map<String, Serializable> connectionParameters,
						IProgressMonitor monitor) throws IOException {
					return null;
				}
				
				@Override
				public void removeCatalogListener(IResolveChangeListener listener) {
				}
				
				@Override
				public String[] getTemporaryDescriptorClasses() {
					return null;
				}
				
				@Override
				public List<IResolve> find(URL resourceId, IProgressMonitor monitor) {
					return null;
				}
				
				@Override
				public IGeoResource createTemporaryResource(Object descriptor)
						throws IllegalArgumentException {
					return null;
				}
				
				@Override
				public List<IService> constructServices(Map<String, Serializable> params,
						IProgressMonitor monitor) throws IOException {
					return null;
				}
				
				@Override
				public List<IService> constructServices(URL url, IProgressMonitor monitor)
						throws IOException {
					return null;
				}
				
				@Override
				public List<IService> checkNonMembers(List<IService> constructServiceList) {
					return null;
				}
				
				@Override
				public List<IService> checkMembers(List<IService> constructServiceList) {
					return null;
				}
				
				@Override
				public void addCatalogListener(IResolveChangeListener listener) {
				}
			};
		}
        return defaultCat;
	}
	
	
	 /**
     * List of registered {@link ServiceExtension}s.
     * @return Registered {@link ServiceExtension}
     */
    public List<ServiceExtension> getServiceExtensions(){
        List<ServiceExtension> list = new ArrayList<ServiceExtension>( getRegisteredExtensionsLocal().values() );
        return Collections.unmodifiableList( list );
    }
    /**
     * Register a service extension by hand.
     * 
     * @param id
     * @param extension
     */
    public void register( String id, ServiceExtension extension ){
        try {
            registeredLock.lock();
            getRegisteredExtensionsLocal();
            if( registered.containsKey(id)){
                ServiceExtension existing = registered.get(id);
                String exsistingName = existing != null ? existing.getClass().getSimpleName() : null;
                throw new IllegalStateException("ServiceExtension "+id+" already registered with "+exsistingName ); //$NON-NLS-1$ //$NON-NLS-2$
            }
            
            registered.put( id, extension );
        } finally {
            registeredLock.unlock();
        }
    }
    
    /**
     * Internal method to lazily process the {@link #CATALOG_SERVICE_EXTENSION}
     * @return Map of {@link ServiceExtension} by id
     */
	protected Map<String, ServiceExtension> getRegisteredExtensionsLocal() {
        try {
            registeredLock.lock();
            if (registered == null) { // load available
                // we are going to sort the map so that "generic" fallback datastores are selected last
                //
                registered = new HashMap<String, ServiceExtension>();
				ExtensionPointUtil.process(CatalogPlugin.getDefault(),
						ServiceExtension.EXTENSION_ID,
						new ExtensionPointProcessor() {
							@Override
							public void process(IExtension extension, IConfigurationElement element) throws Exception {

								String id = element.getAttribute("id"); //$NON-NLS-1$
								try {
									ServiceExtension se = (ServiceExtension) element.createExecutableExtension("class"); //$NON-NLS-1$
									if (id == null || id.length() == 0) {
										id = se.getClass().getSimpleName();
									}
									registered.put(id, se);
								} catch (Throwable t) {
									//we don't include all smart extensions so this is likely to happen
									//for smart service extensions.  this is okay, as we don't use these
									Logger.getLogger(CatalogPluginWrapper.class.getName()).log(Level.WARNING, t.getMessage(), t);
								}

							}
						});
            }
            return registered;
        } finally {
            registeredLock.unlock();
        }
    }
    /** Look up a specific implementation; used mostly for test cases */
    public <E extends ServiceExtension> E serviceImplementation( Class<E> implementation ) {
        for( Map.Entry<String, ServiceExtension> entry : getRegisteredExtensionsLocal().entrySet() ) {
            String id = entry.getKey();
            ServiceExtension serviceExtension = entry.getValue();

            if (id == null || serviceExtension == null)
                continue;
            if (implementation.isInstance(serviceExtension)) {
                return implementation.cast(serviceExtension);
            }
        }
        return null;
    }
    /** Look up a specific implementation; used mostly for test cases */
    public ServiceExtension serviceImplementation( String serviceExtensionId ) {
        for( Map.Entry<String, ServiceExtension> entry : getRegisteredExtensionsLocal().entrySet() ) {
            String id = entry.getKey();
            ServiceExtension serviceExtension = entry.getValue();

            if (id == null || serviceExtension == null)
                continue;
            if (serviceExtensionId.equalsIgnoreCase(id)) {
                return serviceExtension;
            }
        }
        return null;
    }
    
    public IResolveManager getResolveManager() {
    	if (resolveManager == null){
    		resolveManager = new ResolveManager2();
    	}
        return resolveManager;
    }
}
