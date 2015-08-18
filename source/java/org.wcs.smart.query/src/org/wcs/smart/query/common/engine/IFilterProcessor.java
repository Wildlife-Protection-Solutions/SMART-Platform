package org.wcs.smart.query.common.engine;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.IFilter;

public interface IFilterProcessor {
	/**
	 * 
	 * @param c database connection
	 * @param queryFilter query filter
	 * @param dateFilter date filter
	 * @param caFilter conservation area filter
	 * @param populateObservation if observation fields (wp_uuid, wp_ob_uuid) are to be populated
	 * @param includeEmptyObservations if waypoints with no observations should be included
	 * 
	 * @param monitor
	 * @throws Exception
	 */
	public void processFilter(Connection c, IFilter queryFilter, 
			DateFilter dateFilter, ConservationAreaFilter caFilter, 
			boolean populateObservation,
			boolean includeEmptyObservations, 
			IProgressMonitor monitor) throws SQLException;
	
	/**
	 * Drop any temporary tables created during
	 * filter processing
	 * @param c
	 */
	public void dropTemporaryTables(Connection c);
}
