package org.wcs.smart.qa.routine;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public interface ILocationRoutineData {

	public enum Type {POINT, LINESTRING};
	
	/**
	 * Get the type of geometry supplied (point or polygon)
	 * 
	 * @return
	 */
	public Type getType();
	/**
	 * Only valid for linestring types
	 * @return
	 */
	public Geometry getGeometry();
	
	/**
	 * Only for point types
	 * @return
	 */
	public Coordinate getPoint();
	
	
	
	
}
