package org.wcs.smart.asset.map.engine;

import java.util.HashMap;
import java.util.UUID;

import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn;

public interface IColumnEngine {

	/**
	 * Compute the statistics values for asset station or station location
	 * 
	 * @param toCompute the column value to computer
	 * @param groupBy the group by option (station or location)
	 * @return a map from object to value.  Key should be station or location uuid depending on the group by option.
	 */
	public HashMap<UUID, Object> computeValues(IOverviewTableColumn toCompute, final IOverviewTableColumn.GroupByOption groupBy);
	
	/**
	 * 
	 * @param column
	 * @return true if this engine can process the given column
	 */
	public boolean canProcess(IOverviewTableColumn column);
}
