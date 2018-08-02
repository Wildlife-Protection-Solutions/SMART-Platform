package org.wcs.smart.birt;

public class BirtConstants {
	/**
	 * Conservation Area report parameter 
	 */
	public static final String CA_PARAM = "org.wcs.smart.ca"; //$NON-NLS-1$

	/**
	 * Database session report parameter
	 */
	public static final String SESSION_PARAM = "org.wcs.smart.session"; //$NON-NLS-1$
	
	/**
	 * App context projection provider variable
	 */
	public static final String PROJECTION_PROVIDER_CONTEXT_VAR = "org.wcs.smart.report.crs"; //$NON-NLS-1$
	
	/**
	 * Default DPI for reports.  The default value is 96 if not provided
	 */
	public static final String DEFAULT_DPI_PARAM = "org.wcs.smart.birt.map.defaultdpi"; //$NON-NLS-1$
	
	/**
	 * Optional location for storing decrypted images for reports viewing in smart
	 */
	public static final String WORKING_DIRECTORY = "org.wcs.smart.birt.map.workingdirectory"; //$NON-NLS-1$
}
