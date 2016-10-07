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
package org.wcs.smart.report.birt.map.udig;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.IServiceInfo;

/**
 *  Map Query service info for BIRT SMART Map Layer
 * @author Emily
 *
 */
public class MapQueryService extends IService {
	
	public static final String URL = "smart://org.wcs.smart.report.birt/"; //$NON-NLS-1$
	
    public final static URLStreamHandler RELAXED_HANDLER = new URLStreamHandler(){

        @Override
        protected URLConnection openConnection( URL u ) throws IOException {
            try{
                URL url=new URL(u.toString());
                return url.openConnection();
            }catch (MalformedURLException e){
                return null;
            }
        }
    };
	
    private List<IGeoResource> resources = new ArrayList<IGeoResource>();
	
    private IServiceInfo info = new IServiceInfo("Temporary service for report maps", null, null, null, null, null, null, null); //$NON-NLS-1$
	
    private URL url;
	
	public MapQueryService(){
		try {
			this.url = new URL(null,  URL + System.nanoTime(), RELAXED_HANDLER);
		} catch (MalformedURLException e) {
			Logger.getLogger(MapGeoResourceInfo.class.getName()).log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	public void addResource(IGeoResource resource){
		resources.add(resource);
	}
	
	@Override
	public Throwable getMessage() {
		return null;
	}

	@Override
	public URL getIdentifier() {
		return url;
	}

	@Override
	public List<? extends IGeoResource> resources(IProgressMonitor monitor)
			throws IOException {
		return resources;
	}

	@Override
	protected IServiceInfo createInfo(IProgressMonitor monitor)
			throws IOException {
		return info;
	}

	@Override
	public Map<String, Serializable> getConnectionParams() {
		return null;
	}

	@Override
	public <T> boolean canResolve( Class<T> adaptee ) {
		 for (IGeoResource r : resources){
			 if (r.canResolve(adaptee)){
				 return true;
			 }
		 }
		 return super.canResolve(adaptee);
	}
	@Override
	public <T> T resolve( Class<T> adaptee, IProgressMonitor monitor ) throws IOException {
		for (IGeoResource r : resources){
			T resolve = r.resolve(adaptee, monitor);
			if (resolve != null) return resolve;
		}
		return super.resolve(adaptee, monitor);
	}
}
