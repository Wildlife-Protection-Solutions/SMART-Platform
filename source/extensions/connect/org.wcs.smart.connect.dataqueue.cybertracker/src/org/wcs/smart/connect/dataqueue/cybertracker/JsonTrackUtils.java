package org.wcs.smart.connect.dataqueue.cybertracker;

import java.util.Arrays;
import java.util.Date;

import org.wcs.smart.map.GeometryFactoryProvider;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

public class JsonTrackUtils {
	
	public static LineString addPointToTrack(LineString track, Coordinate pnt, Date pntTime){
		Coordinate[] c = null;
		if (track == null){
			c = new Coordinate[] {new Coordinate(pnt.x, pnt.y, pntTime.getTime())};
		}else{
			c = track.getCoordinates();
			if (c[0].x == c[1].x && c[0].y == c[1].y && c[0].z == c[1].z){
				//the first two points are the same so lets remove one
				c = Arrays.copyOfRange(c, 1, c.length);		
			}
			for (Coordinate cd : c){
				if (cd.x == pnt.x && cd.y == pnt.y && cd.z == pntTime.getTime()){
					//point already exists; do not duplicate it
					return track;
				}
			}
		}
		
		c = Arrays.copyOf(c, c.length + 1);
		c[c.length-1] = new Coordinate(pnt.x, pnt.y, pntTime.getTime());
		
		//sort
		Arrays.sort(c, 0, c.length, (Coordinate c1, Coordinate c2) -> ((Double)c1.z).compareTo(c2.z));

		return (LineString)GeometryFactoryProvider.getFactory().createLineString(c);		
	}
}
