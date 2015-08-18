package org.wcs.smart.ca.export;

import java.io.File;
import java.util.UUID;

import org.hibernate.Session;

public interface ICaDataImportEngine {

	/**
	 * @return the current hibernate session
	 */
	public Session getSession();
	
	/**
	 * @return the location of the unzipped
	 * import file
	 */
	public File getImportDataDirectory();
		
	
	/**
	 * 
	 * @return the conservation area uuid of the ca being
	 * importer
	 */
	public UUID getConservationAreaUuid();
}

