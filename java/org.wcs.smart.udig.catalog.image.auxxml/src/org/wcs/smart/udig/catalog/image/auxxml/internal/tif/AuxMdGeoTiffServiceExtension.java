/*
 *    uDig - User Friendly Desktop Internet GIS client
 *    http://udig.refractions.net
 *    (C) 2004-2011, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 *
 */
package org.wcs.smart.udig.catalog.image.auxxml.internal.tif;


import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.geotools.gce.geotiff.GeoTiffFormat;
import org.locationtech.udig.catalog.ID;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.URLUtils;
import org.locationtech.udig.catalog.internal.geotiff.GeoTiffServiceExtension;
import org.wcs.smart.udig.catalog.image.auxxml.internal.WmsXmlReader;
import org.wcs.smart.udig.catalog.image.auxxml.worldimage.internal.Messages;


/**
 *
 * Extended from uDig, then modified to include the ability to
 * read aux.xml projection files.
 * @author Emily
 * 
 * Provides the interface to the catalog service extension point.
 * <p>
 * This class is responsible for ensuring that only those services that the GeoTiff plug-in is
 * capable of processing are created.
 * </p>
 * 
 * @author mleslie
 * @since 0.6.0
 */
public class AuxMdGeoTiffServiceExtension extends GeoTiffServiceExtension {

    public static final String TYPE = "aux+geotiff"; //$NON-NLS-1$
    
    private static GeoTiffFormat format;
    
    private File auxFile = null;
    
    /**
     * Construct <code>GeoTiffServiceExtension</code>.
     */
    public AuxMdGeoTiffServiceExtension() {
        super();
    }

    @Override
    public IService createService( URL id, Map<String, Serializable> params ) {
        URL id2 = id;
        if (id2 == null) {
            id2 = extractID(params);
        }
        if (!canProcess(extractID(params))) {
            return null;
        }
        AuxMdGeoTiffService service = new AuxMdGeoTiffService(extractID(params), getFactory());
        return service;
    }

    private URL extractID( Map<String, Serializable> params ) {
        URL id;
        if (params.containsKey(URL_PARAM)) {
            Object param = params.get(URL_PARAM);
            if (param instanceof String) {
                try {
                    id = new URL((String) param);
                } catch (MalformedURLException ex) {
                    return null;
                }
            } else if (param instanceof URL) {
                id = (URL) param;
            } else {
                return null;
            }
        } else {
            return null;
        }
        return id;
    }
    
    private boolean canProcess( URL id ) {
        if ( reasonForFailure(id)==null )
            return true;
        return false;
    }

    public Map<String, Serializable> createParams( URL url ) {
        if (!canProcess(url))
            return null;

        Map<String, Serializable> params = new HashMap<String, Serializable>();
        if (url != null) {
            params.put(URL_PARAM, url);
        }
        return params;
    }

    public String reasonForFailure( Map<String, Serializable> params ) {
        return reasonForFailure(extractID(params));
    }

    public String reasonForFailure( URL url ) {
        if (url == null) {
            return Messages.GeoTiffServiceExtension_nullURL;
        }

        if( !isSupportedExtension(url) )
            return Messages.GeoTiffServiceExtension_badExt;
        
        File file = null;
        try {
            ID id = new ID( url );
            file = id.toFile();
        } catch (IllegalArgumentException ex) {
            return url.toExternalForm()+Messages.GeoTiffServiceExtension_notFile;
        }
        
        if (!file.exists() )
            return file+Messages.GeoTiffServiceExtension_notExist;

        //check for prj file; if exists we should not use this
        String error = WmsXmlReader.findPrjFile(file);
        if (error != null) return error;
        
        //check for aux file; if doesn't exists we shouldn't use this
        auxFile = WmsXmlReader.findFile(file);
        if (auxFile == null){
        	return Messages.AuxMdGeoTiffServiceExtension_AuxFileNotFound;
        }
        //ensure it can be read
        WmsXmlReader reader = new WmsXmlReader(auxFile);
        if (reader.getCoordinateReferenceSystem() == null){
        	return Messages.AuxMdGeoTiffServiceExtension_InvalidProjectionInAuxFile;
    	}
        
        try {
            if (!getFormat().accepts(file))
                return Messages.GeoTiffServiceExtension_unknown;
        } catch (RuntimeException ex) {
            return Messages.GeoTiffServiceExtension_unknown;
        }
        return null;
    }
    
    private synchronized static GeoTiffFormat getFormat() {
        if (format == null) {
            format = (GeoTiffFormat) getFactory().createFormat();
        }
        return format;
    }
    
    private boolean isSupportedExtension( URL url ) {
        File file = URLUtils.urlToFile(url);
        String fileLower = file.getAbsolutePath().toLowerCase();
        boolean isTiff = fileLower.endsWith(".tiff") || fileLower.endsWith(".tif"); //$NON-NLS-1$ //$NON-NLS-2$
        if (!isTiff) {
            return false;
        }
        return true;
    }

    
}
