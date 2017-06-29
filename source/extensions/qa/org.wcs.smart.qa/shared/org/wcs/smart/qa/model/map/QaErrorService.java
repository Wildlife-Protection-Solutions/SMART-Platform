/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.model.map;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.IServiceInfo;
import org.locationtech.udig.core.internal.CorePlugin;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.model.QaError;

/**
 * UDig map service for displaying the results
 * of data validation.
 * 
 * @author Emily
 *
 */
public class QaErrorService extends IService {

	private Map<String, Serializable> params;
	private URL url;
	private volatile List<QaErrorGeoResource> members;
	private QaErrorMemoryDatastore ds = null;
	
	public QaErrorService(Collection<QaError> errors) {
		ds = new QaErrorMemoryDatastore(errors);
		try {
			this.url = new URL(null, "smart://smartdb/qa/" + System.nanoTime(), CorePlugin.RELAXED_HANDLER); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			QaPlugIn.log(e.getMessage(), e);
		}
	}

	public QaErrorMemoryDatastore getDataStore(){
		return this.ds;
	}
	/**
	 * @see org.locationtech.udig.catalog.IResolve#getStatus()
	 */
	@Override
	public Status getStatus() {
		return Status.CONNECTED;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.locationtech.udig.catalog.IResolve#getMessage()
	 */
	@Override
	public Throwable getMessage() {
		return null;
	}

	/**
	 * @see org.locationtech.udig.catalog.IResolve#getIdentifier()
	 */
	@Override
	public URL getIdentifier() {
		return this.url;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.locationtech.udig.catalog.IService#resources(org.eclipse.core.runtime
	 * .IProgressMonitor)
	 */
	@Override
	public List<? extends IGeoResource> resources(IProgressMonitor monitor)
			throws IOException {
		if (members == null){
			synchronized (this) {
				if (members == null){					
					ArrayList<QaErrorGeoResource> list = new ArrayList<>();
					//two resources per entity one for points and one for polygons
					list.add(new QaErrorGeoResource(this, FeatureFactory.QA_ERROR_PNT_TYPE_NAME));
					list.add(new QaErrorGeoResource(this, FeatureFactory.QA_ERROR_LINE_TYPE_NAME));
					members = list;
				}
			}
		}
		return members;
	}

	/**
	 * @see org.locationtech.udig.catalog.IService#createInfo(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IServiceInfo createInfo(IProgressMonitor monitor)
			throws IOException {
		return new IServiceInfo("Temporary QA Service", "Temporary service for displaying QA result layers",null, null,null, null, new String[]{},null);
	}

	/**
	 * @see org.locationtech.udig.catalog.IService#getConnectionParams()
	 */
	@Override
	public Map<String, Serializable> getConnectionParams() {
		return params;
	}

	@Override
	public void dispose( IProgressMonitor monitor ) {
		super.dispose(monitor);
        if (this.ds != null){
        	this.ds.dispose();
        }
    }
}
