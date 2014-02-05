package org.wcs.smart.ca.export;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Interface that can be implemented to import
 * conservation area data.
 * <
 * 
 * @author egouge
 * @since 1.0.0
 */
public interface ICaDataImporter {
	/**
	 * Imports data for the conservation area.
	 * 
	 * @param exportEngine export engine
	 * @param monitor progress monitor
	 * @throws Exception
	 */
	void importData(ICaDataImportEngine engine, IProgressMonitor monitor) throws Exception;
	
}
