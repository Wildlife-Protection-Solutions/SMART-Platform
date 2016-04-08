/*
 *    uDig - User Friendly Desktop Internet GIS client
 *    http://udig.refractions.net
 *    (C) 2004, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 *
 */
package org.wcs.smart.udig.catalog.image.auxxml.internal.worldimage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.factory.Hints;
import org.geotools.gce.image.WorldImageFormatFactory;
import org.geotools.referencing.CRS;
import org.locationtech.udig.catalog.ID;
import org.locationtech.udig.catalog.URLUtils;
import org.locationtech.udig.catalog.rasterings.AbstractRasterGeoResource;
import org.locationtech.udig.catalog.rasterings.AbstractRasterService;
import org.locationtech.udig.catalog.rasterings.AbstractRasterServiceInfo;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Copied from uDig, then modified to include the ability to
 * read aux.xml projection files.
 * @author Emily
 * 
 * Provides a handle to a world image service allowing the service to be lazily loaded.
 * 
 * @author mleslie
 * @since 0.6.0
 * 
 * 
 */
public class AuxMdImageRasterService extends AbstractRasterService {
	
    private AbstractRasterGeoResource resource;

    /**
     * Construct <code>WorldImageServiceImpl</code>.
     * 
     * @param id
     * @param factory
     */
    public AuxMdImageRasterService( URL id2, WorldImageFormatFactory factory ) {
        super(id2, AuxMdImageServiceExtension.TYPE, factory);
    }

    /** Added to prevent creation of new GeoResource on each call to members */
    public synchronized AbstractRasterGeoResource getGeoResource( IProgressMonitor monitor ) {
        if (resource == null) {
            URL prjURL = null;
            java.io.File baseFile = URLUtils.urlToFile(getIdentifier());

            java.io.File[] found = URLUtils.findRelatedFiles(baseFile, ".prj"); //$NON-NLS-1$
            if (found.length > 0) {
                try {
                    prjURL = found[0].toURI().toURL();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }else{
            	//look for .aux.xml
            	File f = new File(baseFile.getParent(), baseFile.getName() + ".aux.xml"); //$NON-NLS-1$
            	if (f.exists()){
            	    try {
						prjURL = f.toURI().toURL();
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                 }
            }

            resource = new AuxMdImageGeoResourceImpl(this, getHandle(), prjURL);
        }
        return resource;
    }
    @Override
    public List<AbstractRasterGeoResource> resources( IProgressMonitor monitor ) throws IOException {
        List<AbstractRasterGeoResource> list = new ArrayList<AbstractRasterGeoResource>();
        list.add(getGeoResource(monitor));
        return list;
    }

    protected AbstractRasterServiceInfo createInfo( IProgressMonitor monitor ) throws IOException {
        if (monitor == null)
            monitor = new NullProgressMonitor();
        monitor.beginTask("world image", 2); //$NON-NLS-1$
        try {
            monitor.worked(1);
            return new AbstractRasterServiceInfo(this,
                    "WorldImage", "world image", ".gif", ".jpg", ".jpeg", //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$ //$NON-NLS-5$
                    ".tif", ".tiff", ".png"){ //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$);
            	 
            	
            	@Override
            	public double getMetric() {
            		//the maximum from the worldimage plugin is 2, so we make this bigger than that
            		//to ensure it is selected; this will only be called
            		//if the wms.xml file exists and is readable
            		return 3;
            	}
            }; 
        } finally {
            monitor.done();
        }
    }

    @Override
    public Map<String, Serializable> getConnectionParams() {
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(AuxMdImageServiceExtension.URL_PARAM, getIdentifier());
        return params;
    }

    
    
    
    /**
     * Finds or creates the Reader used to access this service. Apon any exception, the message
     * field is populated and null is returned.
     * 
     * @return Reader linked to this service.
     */
    @Override
    public synchronized AbstractGridCoverage2DReader getReader(IProgressMonitor monitor) {
        if (this.reader == null) {
            try {
                AbstractGridFormat frmt = (AbstractGridFormat) getFormat();
                ID id = getID();
                if( id.isFile() ){
                    File file = id.toFile();
                    if (file != null) {
                        // to force crs
                    	CoordinateReferenceSystem crs = getGeoResource(monitor).getInfo(monitor).getCRS();
                    	
                    	AbstractGridCoverage2DReader tmpreader = (AbstractGridCoverage2DReader) frmt.getReader(file);
                    	if (CRS.equalsIgnoreMetadata(crs, frmt.getReader(file).getCoordinateReferenceSystem())){
                    		this.reader = tmpreader;
                    	}else{
                    		//force crs to metadata crs
                    		Hints hints = new Hints();
                    		hints.put(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, crs);
                        
                    		this.reader = (AbstractGridCoverage2DReader) frmt.getReader(file, hints);
                    	}
                        return this.reader;
	                }
	                else {
	                	throw new FileNotFoundException( id.toFile().toString() );
	                }
                }
                this.reader = (AbstractGridCoverage2DReader) frmt.getReader( id.toURL() );
            } catch (Exception ex) {
                this.message = ex;
            }
        }
        return this.reader;
    }

}
