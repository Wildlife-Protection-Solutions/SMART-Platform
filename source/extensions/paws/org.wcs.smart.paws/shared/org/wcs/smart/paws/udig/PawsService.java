/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.udig;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.gce.geotiff.GeoTiffFormatFactorySpi;
import org.hibernate.Session;
import org.locationtech.udig.catalog.IServiceInfo;
import org.locationtech.udig.catalog.rasterings.AbstractRasterGeoResource;
import org.locationtech.udig.catalog.rasterings.AbstractRasterService;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.paws.model.PawsResultManager;
import org.wcs.smart.paws.model.PawsRun;

/**
 * uDig service for paws raster files
 * 
 * @author Emily
 *
 */
public class PawsService extends AbstractRasterService {

	private Map<String, Serializable> params;
	
	
	private static GeoTiffFormatFactorySpi factory;
	
	/**
     * Finds or creates a GeoTiffFormatFactorySpi.
     * 
     * @return Default GeoTiffFormatFactorySpi
     */
    public synchronized static GeoTiffFormatFactorySpi getFactory() {
        if (factory == null) {
            factory = new GeoTiffFormatFactorySpi();
        }
        return factory;
    }
    
	public PawsService(Map<String, Serializable> params) {
		super(PawsServiceExtension.createURL(params), "geotiff", getFactory()); 
		this.params = params;
	}
	
	@Override
	public Status getStatus() {
		return Status.CONNECTED;
	}

	@Override
	public Throwable getMessage() {
		return null;
	}


	@Override
	public List<AbstractRasterGeoResource> resources(IProgressMonitor monitor) throws IOException {
		
		//for each results; for each layer we have a resource
		//TODO: clean this up; this seems slow and large and unnecessary
		ConservationArea temp = new ConservationArea();
		temp.setUuid((UUID)params.get(PawsServiceExtension.CA_UUID_KEY));
		
		List<AbstractRasterGeoResource> resources = new ArrayList<>();
		
		try(Session session = HibernateManager.openSession()){
			List<PawsRun> runs = QueryFactory.buildQuery(session, PawsRun.class, 
					"conservationArea", temp).list();
		
			for(PawsRun r : runs) {
				PawsResultManager m = new PawsResultManager(r);
				for (Path p : m.getRasterFiles()) {
					resources.add( new PawsTiffGeoResource(this, p) );
				}
			}
		}
		return resources;
	}

	@Override
	public void dispose(IProgressMonitor monitor) {

	}
	@Override
	protected IServiceInfo createInfo(IProgressMonitor monitor) throws IOException {
		return new PawsServiceInfo(this);
	}

	@Override
	public Map<String, Serializable> getConnectionParams() {
		return this.params;
	}
	
	@Override
    public synchronized AbstractGridCoverage2DReader getReader(IProgressMonitor monitor) {
		throw new UnsupportedOperationException();
	}

}
