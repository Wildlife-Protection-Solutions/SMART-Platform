package org.wcs.smart.ct2smart.patrol;

import java.util.Date;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

public class InterpolationUtil {

	public static final Coordinate interpolate(LineString line, Date time) {
		if (line == null || time == null)
			return null;
		Coordinate[] coordinates = line.getCoordinates();
		if (coordinates.length == 0)
			return null;

		double z = time.getTime();
		int high = coordinates.length-1;
		int low = 0;
		int mid;

		while (low <= high) {
			mid = (low + high) >> 1;
			if (coordinates[mid].z < z)
				low = mid + 1;
			else if (coordinates[mid].z > z)
				high = mid - 1;
			else
				return coordinates[mid]; // z found (exact match)
		}
		//low now points to next index; high to previous
		if (high < 0)
			return coordinates[low];
		if (low == coordinates.length)
			return coordinates[high];
		
		Coordinate from = coordinates[high];
		Coordinate to = coordinates[low];
		double scale = (z - from.z) / (to.z - from.z);
		double x = from.x + scale * (to.x - from.x);
		double y = from.y + scale * (to.y - from.y);
		return new Coordinate(x, y, z);
	}
}
