package org.wcs.smart.query.model;

public interface IGeometryColumn {
	
	public enum Type{
		
		POINT("Point", 8200),
		LINESTRING("LineString", 8201),
		MULTIPOINT("MultiLineString", 8202),
		MULTILINESTRING("MultiLineString", 8203),
		POLYGON("Polygon", 8204),
		MULTIPOLYGON("MultiPolygon", 8205);
		
		public String geoToolsType;
		public int birtDataType;
		
		Type(String geoToolsType, int birtDataType) {
			this.geoToolsType = geoToolsType;
			this.birtDataType = birtDataType;
		}		
	}
	/**
	 * Used for creating geotools feature types
	 * 
	 * @return Point, Polygon, MultiPolygon, LineString etc.
	 */
	public Type getGeometryType();
	
	public default int getSRID() {
		return 4326;
	}
}
