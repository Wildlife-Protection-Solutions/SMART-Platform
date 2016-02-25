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
package org.wcs.smart.udig.catalog.image.auxxml.worldimage.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.locationtech.udig.catalog.worldimage.internal.messages"; //$NON-NLS-1$
	public static String InMemoryCoverageLoader_close_button;
    public static String InMemoryCoverageLoader_message;
    public static String InMemoryCoverageLoader_msgTitle;
    public static String InMemoryCoverageLoader_restart_button;
    public static String WorldImageGeoResourceImpl_PrjUnavailable;
    public static String WorldImageServiceExtension_geotoolsDisagrees;
    public static String WorldImageServiceExtension_or;
	public static String AuxMdGeoTiffServiceExtension_AuxFileNotFound;
	public static String AuxMdGeoTiffServiceExtension_InvalidProjectionInAuxFile;
	public static String AuxMdImageServiceExtension_AuxProjectionInvalid;
	public static String AuxMdImageServiceExtension_AuxXmlFileNotFound;
	public static String AuxMdImageServiceExtension_NotAFile;
	public static String GeoTiffGeoResource_connect;
    public static String GeoTiffServiceExtension_badExt;
    public static String GeoTiffServiceExtension_notExist;
    public static String GeoTiffServiceExtension_notFile;
    public static String GeoTiffServiceExtension_NotRead;
    public static String GeoTiffServiceExtension_notReadWError;
    public static String GeoTiffServiceExtension_NotTiff;
    public static String GeoTiffServiceExtension_NotTiff1;
    public static String GeoTiffServiceExtension_nullURL;
    public static String GeoTiffServiceExtension_unknown;
	public static String GeoTiffServiceImpl_connecting_to;
	public static String GeoTiffServiceImpl_loading_task_title;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
