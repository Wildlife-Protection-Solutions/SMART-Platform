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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.parameter.ParameterGroup;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.rasterings.AbstractRasterGeoResource;
import org.locationtech.udig.catalog.rasterings.GridCoverageLoader;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.udig.catalog.image.auxxml.internal.WmsXmlReader;

/**
 * 
 * Copied from uDig, then modified to include the ability to
 * read aux.xml projection files.
 * @author Emily
 * 
 * Provides a handle to a world image resource allowing the service to be lazily loaded.
 * 
 * @author mleslie
 * @since 0.6.0
 */
public class AuxMdImageGeoResourceImpl extends AbstractRasterGeoResource {
    
	private URL prjURL;

    /**
     * Construct <code>WorldImageGeoResourceImpl</code>.
     * 
     * @param service Service creating this resource.
     * @param name Human readable name of this resource.
     * @param prjURL Name a projection file associated with this resource can be expected to have.
     */
    public AuxMdImageGeoResourceImpl( AuxMdImageRasterService service, String name, URL prjURL ) {
        super(service, name);
        this.prjURL = prjURL;
    }
    
    @Override
    public AuxMdImageInfo getInfo( IProgressMonitor monitor ) throws IOException {
        return (AuxMdImageInfo) super.getInfo(monitor);
    }
    protected AuxMdImageInfo createInfo( IProgressMonitor monitor ) throws IOException {
        this.lock.lock();
        try {
            if (getStatus() == Status.BROKEN) {
                return null; // unavailable
            }
            CoordinateReferenceSystem crs = readCrs();
            return new AuxMdImageInfo(this, crs);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <T> boolean canResolve( Class<T> adaptee ) {
        if (GridCoverageLoader.class.isAssignableFrom(adaptee))
            return true;

        return super.canResolve(adaptee);
    }

    @Override
    public <T> T resolve( Class<T> adaptee, IProgressMonitor monitor ) throws IOException {
        return super.resolve(adaptee, monitor);
    }

    public ParameterGroup getReadParameters() {
        try {
            CoordinateReferenceSystem crsSys = readCrs();

            DefaultParameterDescriptor<CoordinateReferenceSystem> crs = new DefaultParameterDescriptor<CoordinateReferenceSystem>(
                    "crs", //$NON-NLS-1$
                    CoordinateReferenceSystem.class, null, crsSys);

            DefaultParameterDescriptor<GridGeometry> gridGeometryDescriptor = getWorldGridGeomDescriptor();

            // HashMap duplicate of that in GeoTools WorldImageFormat mInfo.
            // due to visibility restrictions
            HashMap<String, Object> info1 = new HashMap<String, Object>();
            info1.put("name", "WorldImage"); //$NON-NLS-1$//$NON-NLS-2$
            info1.put("description", //$NON-NLS-1$
                    "A raster file accompanied by a spatial data file"); //$NON-NLS-1$
            info1.put("vendor", "Geotools"); //$NON-NLS-1$ //$NON-NLS-2$
            info1.put("docURL", "http://www.geotools.org/WorldImageReader+formats"); //$NON-NLS-1$ //$NON-NLS-2$
            info1.put("version", "1.0"); //$NON-NLS-1$ //$NON-NLS-2$
            return new ParameterGroup(new DefaultParameterDescriptorGroup(info1,
                    new GeneralParameterDescriptor[]{crs, gridGeometryDescriptor}));

        } catch (MalformedURLException e) {
            CatalogPlugin.getDefault().getLog().log(
                    new org.eclipse.core.runtime.Status(IStatus.WARNING,
                            "org.locationtech.udig.catalog", 0, //$NON-NLS-1$
                            "", e)); //$NON-NLS-1$
            return super.getReadParameters();
        } catch (IOException e) {
            CatalogPlugin.getDefault().getLog().log(
                    new org.eclipse.core.runtime.Status(IStatus.WARNING,
                            "org.locationtech.udig.catalog", 0, //$NON-NLS-1$
                            "", e)); //$NON-NLS-1$
            return super.getReadParameters();
        }
    }

    private CoordinateReferenceSystem readCrs() throws IOException {
        //try wms.xml
    	try{
    		WmsXmlReader reader = new WmsXmlReader(this.prjURL);
    		CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
    		if (crs != null){
    			return crs;
    		}
    	}catch (Exception ex){}
        return DefaultEngineeringCRS.GENERIC_2D;
    }

}
