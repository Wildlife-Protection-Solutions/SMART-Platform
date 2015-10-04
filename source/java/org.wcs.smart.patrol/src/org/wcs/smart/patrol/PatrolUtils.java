/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.patrol;

import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.TrackUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * A set of utility classes.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolUtils {

	public static final String EDIT_LINK_TEXT = DialogConstants.EDIT_LINK_TEXT;
	/**
	 * Returns the image to use for a given patrol type or
	 * null if no image found
	 * 
	 * @param type patrol type
	 * 
	 * @return image associated with patrol type
	 */
	public static Image getImage(PatrolType.Type type){
		if (type == PatrolType.Type.GROUND){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.GROUND_PATROL_ICON);
		}else if (type == PatrolType.Type.MARINE){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.MARINE_PATROL_ICON);
		}else if (type == PatrolType.Type.AIR){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.AIR_PATROL_ICON);
		}
		return null;
	}
	
	/**
	 * Returns the image descriptor to use for a given patrol type or
	 * null if no image found
	 * 
	 * @param type patrol type
	 * 
	 * @return image descriptor associated with patrol type
	 */
	public static ImageDescriptor getImageDescriptor(PatrolType.Type type){
		if (type == PatrolType.Type.GROUND){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPatrolPlugIn.GROUND_PATROL_ICON);
		}else if (type == PatrolType.Type.MARINE){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPatrolPlugIn.MARINE_PATROL_ICON);
		}else if (type == PatrolType.Type.AIR){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPatrolPlugIn.AIR_PATROL_ICON);
		}
		return null;
	}
	
	/**
	 * Converts a set of coordinate to a track.  Coordinates are first sorted
	 * by date/time which should be stored in the z coordinate
	 * as the the date/time in the current timezone.  This function
	 * convert the z to GMT time.
	 * 
	 * @param coordinates set of coordinates
	 * @return track
	 */
	public static Track convertToTrack(List<Coordinate> coordinates){
		if (coordinates.size() < 2) {
			return null;
		}
		LineString track = TrackUtil.convertToLineString(coordinates, Track.ZTIMEZONE);
		Track t = new Track();
		t.setLineString(track);
		return t;
	}
	
}
