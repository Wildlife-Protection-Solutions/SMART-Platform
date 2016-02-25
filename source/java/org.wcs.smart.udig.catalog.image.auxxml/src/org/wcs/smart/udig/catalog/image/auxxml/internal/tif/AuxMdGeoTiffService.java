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
package org.wcs.smart.udig.catalog.image.auxxml.internal.tif;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFactorySpi;
import org.geotools.factory.Hints;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.locationtech.udig.catalog.ID;
import org.locationtech.udig.catalog.URLUtils;
import org.locationtech.udig.catalog.internal.geotiff.GeoTiffServiceImpl;
import org.locationtech.udig.catalog.rasterings.AbstractRasterServiceInfo;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.udig.catalog.image.auxxml.internal.WmsXmlReader;

/**
 * Extended from uDig, then modified to include the ability to
 * read aux.xml projection files.
 * @author Emily
 * 
 * Extension of geotiff server to be used when projection
 * data provided in wms xml file.
 * 
 */
public class AuxMdGeoTiffService extends GeoTiffServiceImpl {

	private ID id;
	private CoordinateReferenceSystem auxCrs = null;
	
	/**
     * Construct <code>GeoTiffServiceImpl</code>.
     *
     * @param id
     * @param factory
     */
    public AuxMdGeoTiffService(URL id, GridFormatFactorySpi factory) {
        super(id, factory);
        this.id = new ID( id, AuxMdGeoTiffServiceExtension.TYPE );
    }

    @Override
    public URL getIdentifier() {
        return id.toURL();
    }
    
    @Override
    public ID getID() {
        return id;
    }

    
    @Override 
    public synchronized AbstractGridCoverage2DReader getReader() {
        if (this.reader == null) {
            return getReader(new NullProgressMonitor());
        }
        return this.reader;
    }
    
    @Override
    public synchronized AbstractGridCoverage2DReader getReader(IProgressMonitor monitor) {
        if (this.reader == null) {
            try {
                File file = new File(getIdentifier().toURI());
                GeoTiffFormat geoTiffFormat = (GeoTiffFormat) getFormat();
                
                CoordinateReferenceSystem crs = readCrs();
            	
            	AbstractGridCoverage2DReader tmpreader = (AbstractGridCoverage2DReader) geoTiffFormat.getReader(file);
            	if (CRS.equalsIgnoreMetadata(crs, tmpreader.getCoordinateReferenceSystem())){
            		this.reader = tmpreader;
            	}else{
            		//force crs to metadata crs
            		Hints hints = new Hints();
            		hints.put(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, crs);
            		this.reader = (AbstractGridCoverage2DReader) geoTiffFormat.getReader(file, hints);
            	}
                return this.reader;
            } catch (Exception ex) {
                this.message = ex;
            }
        }
        return this.reader;
    }

    protected synchronized AbstractRasterServiceInfo createInfo(IProgressMonitor monitor) {
         if(monitor == null) monitor = new NullProgressMonitor();
         try {
             monitor.beginTask("loading image", 2);  //$NON-NLS-1$
             monitor.worked(1);
             return new AbstractRasterServiceInfo(this, "geotiff", "tiff", "tif"){//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
            	@Override
             	public double getMetric() {
            		//the maximum from the tif plugin is 2, so we make this bigger than that
            		//to ensure it is selected; this will only be called
            		//if the wms.xml file exists and is readable
            		return 3;
             	}
             };   
         }
         finally {
             monitor.done();
         }
    }
    
	private synchronized CoordinateReferenceSystem readCrs() throws IOException {
		if (auxCrs != null)
			return auxCrs;

		// try wms.xml
		File urlFile = URLUtils.urlToFile(getIdentifier());
		File wmsxml = new File(urlFile.toString() + ".aux.xml"); //$NON-NLS-1$
		WmsXmlReader reader = new WmsXmlReader(wmsxml);
		auxCrs = reader.getCoordinateReferenceSystem();
		
		if (auxCrs == null) {
			// prj file not read, default to lat long
			auxCrs = DefaultEngineeringCRS.GENERIC_2D;
		}
		return auxCrs;
	}
}
