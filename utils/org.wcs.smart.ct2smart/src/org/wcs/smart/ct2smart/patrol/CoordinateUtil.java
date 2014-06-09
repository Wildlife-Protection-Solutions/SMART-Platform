package org.wcs.smart.ct2smart.patrol;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class CoordinateUtil {

	private static final TimeZone ZTIMEZONE = TimeZone.getTimeZone("GMT"); //$NON-NLS-1$
	
	public static Coordinate interpolate(LineString line, Date time) {
		if (line == null || time == null)
			return null;
		Coordinate[] coordinates = line.getCoordinates();
		if (coordinates.length == 0)
			return null;

		double z = updateTimezone(time.getTime());
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

	//copy from PatrolUtils createTrack(...)
	public static LineString buildLineString(List<Coordinate> coordinates) {
		if (coordinates.size() < 2) {
			return null;
		}
		GeometryFactory gf = new GeometryFactory();
		Collections.sort(coordinates, new Comparator<Coordinate>() {
			@Override
			public int compare(Coordinate o1, Coordinate o2) {
				return ((Double) o1.z).compareTo((Double) o2.z);
			}
		});
		
		for (Coordinate c : coordinates){
			c.z = updateTimezone((long)c.z);
		}
		return gf.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
	}
	
	//copy from PatrolUtils createTrack(...)
	private static long updateTimezone(long z) {
		//c.z is the date taking into account the current timezone.  We want to compute
		//the date of GMT timezone and assign that to the point.
		//we need to take the year,month,date, hour, min, sec and assign it to a date with
		//a time zone of gmt
		Calendar c1 = Calendar.getInstance();
		c1.setTimeInMillis(z);
		Calendar c2 = Calendar.getInstance();
		c2.setTimeZone(ZTIMEZONE);
		c2.setTimeInMillis(0);
		c2.set(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH), c1.get(Calendar.DATE), c1.get(Calendar.HOUR_OF_DAY), c1.get(Calendar.MINUTE), c1.get(Calendar.SECOND));
		
		return c2.getTime().getTime();
	}
	
}
