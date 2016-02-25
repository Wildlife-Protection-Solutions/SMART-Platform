/* uDig - User Friendly Desktop Internet GIS client
 * http://udig.refractions.net
 * (C) 2004-2012, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.wcs.smart.udig.catalog.image.auxxml.internal.worldimage;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.geotools.gce.image.WorldImageFormat;
import org.geotools.gce.image.WorldImageFormatFactory;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.ServiceExtension2;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.udig.catalog.image.auxxml.AuxMdImagePlugin;
import org.wcs.smart.udig.catalog.image.auxxml.internal.WmsXmlReader;
import org.wcs.smart.udig.catalog.image.auxxml.worldimage.internal.Messages;

/**
 * Copied from uDig, then modified to include the ability to
 * read aux.xml projection files.
 * @author Emily
 * 
 * Provides the interface to the catalog service extension point.
 * <p>
 * This class is responsible for ensuring that only those services that the 
 * WorldImage plugin is capable of processing are created.
 * </p>
 * @author mleslie
 * @since 0.6.0
 * 
 * 
 */
public class AuxMdImageServiceExtension implements ServiceExtension2 {
    /** <code>URL_PARAM</code> field */
    public final static String URL_PARAM = "URL"; //$NON-NLS-1$

    public static final String TYPE = "aux+image"; //$NON-NLS-1$

    private static WorldImageFormatFactory factory;

    /**
     * Construct <code>WorldImageServiceExtension</code>.
     *
     */
    public AuxMdImageServiceExtension() {
        super();
    }

    public IService createService(URL id, Map<String, Serializable> params ) {
        URL id2 = getID(params);

        if (!canProcess(id2)) {
            return null;
        }
        AuxMdImageRasterService service = 
                new AuxMdImageRasterService(id2, getFactory());
        return service;
    }
    
    private URL getID( Map<String, Serializable> params ) {
        if (params.containsKey(URL_PARAM)) {
            Object param = params.get(URL_PARAM);
            if (param instanceof String) {
                try {
                    return new URL((String) param);
                } catch (MalformedURLException ex) {
                    return null;
                }
            } else if (param instanceof URL) {
                return (URL) param;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Finds or creates a WorldImageFormatFactory.
     * 
     * @return Default instance of WorldImageFormatFactory
     */
    public static WorldImageFormatFactory getFactory() {
        if(factory == null) {
            factory = new WorldImageFormatFactory();
        }
        return factory;
    }

    private boolean canProcess( URL id ) {
        return reasonForFailure(id)==null;
    }


    public Map<String, Serializable> createParams( URL url ) {
    	if( !canProcess(url))
    		return null;

        
        if (url != null) {
        	Map<String, Serializable> params = new HashMap<String, Serializable>();	
            params.put(URL_PARAM, url);
            return params;
        }
        
        return null;
    }

    public String reasonForFailure( Map<String, Serializable> params ) {
        return reasonForFailure(getID(params));
    }

    public String reasonForFailure( URL id ) {
        if(id == null) {
            return ""; //$NON-NLS-1$
        }
        File file = URLUtils.urlToFile(id);
        if(file == null) {
            return Messages.AuxMdImageServiceExtension_NotAFile;
        }
        String path = file.getAbsolutePath();
        String fileExt = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
        if(     fileExt.compareToIgnoreCase("BMP") != 0 && //$NON-NLS-1$
                fileExt.compareToIgnoreCase("PNG") != 0 &&  //$NON-NLS-1$
                fileExt.compareToIgnoreCase("GIF") != 0 && //$NON-NLS-1$
                fileExt.compareToIgnoreCase("JPG") != 0 && //$NON-NLS-1$
                fileExt.compareToIgnoreCase("JPEG") != 0 && //&& //$NON-NLS-1$
                fileExt.compareToIgnoreCase("TIF") != 0 && //$NON-NLS-1$
                fileExt.compareToIgnoreCase("TIFF") != 0) { //$NON-NLS-1$
           return "" + fileExt; //$NON-NLS-1$
        }       

        Collection<String> endings = new HashSet<String>(WorldImageFormat.getWorldExtension(fileExt));
        endings.add(".wld"); //$NON-NLS-1$
        endings.add(fileExt+"w"); //$NON-NLS-1$
        File[] found = URLUtils.findRelatedFiles(file, endings.toArray(new String[0]) );
        
        if (found.length==0) {
                return ""; //$NON-NLS-1$
        }
        
        if(!id.getProtocol().equals(AuxMdImagePlugin.PROTOCOL_FILE)) {
            return ""; //$NON-NLS-1$
        }
        try {
            @SuppressWarnings("unused")
            File fileTest = URLUtils.urlToFile(id);
        } catch(IllegalArgumentException ex) {
            return "" + id.getFile() + ""; //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        String error = WmsXmlReader.findPrjFile(file);
        if (error != null) return error;
        
        File auxFile = WmsXmlReader.findFile(file);
        if (auxFile == null){
        	return Messages.AuxMdImageServiceExtension_AuxXmlFileNotFound;
        }
        //ensure it can be read
        WmsXmlReader reader = new WmsXmlReader(auxFile);
        if (reader.getCoordinateReferenceSystem() == null){
        	return Messages.AuxMdImageServiceExtension_AuxProjectionInvalid;
        }
        
        return null;
    }
}
