package org.wcs.smart.asset.query.engine;

import java.util.HashMap;
import java.util.UUID;

import org.wcs.smart.query.common.model.SummaryQueryResult;

import com.vividsolutions.jts.geom.Coordinate;

public class AssetSummaryQueryResult extends SummaryQueryResult {
	
	private HashMap<UUID, Coordinate> positions;
	
	public AssetSummaryQueryResult() {
		super();
	}
	
	public void addCoordinateDetails(UUID uuid, Coordinate c) {
		if (positions == null) positions = new HashMap<>();
		positions.put(uuid, c);
	}

	public Coordinate getCoordinate(UUID uuid) {
		if (positions == null) return null;
		return positions.get(uuid);
	}
	
}
