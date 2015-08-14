/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.i81n.labels;

import java.util.Locale;

import org.wcs.smart.observation.query.model.columns.FixedQueryColumn;
import org.wcs.smart.observation.query.view.IObservationQueryLabelProvider;

/**
 * Observation query label provider implementation.
 * 
 * @author Emily
 *
 */
public class ObservationQueryLabelProvider implements
		IObservationQueryLabelProvider {

	@Override
	public String getLabel(Object key, Locale l) {
		if (key instanceof FixedQueryColumn.FixedColumns){
			switch((FixedQueryColumn.FixedColumns)key){
				case CA_ID:return "Conservation Area ID";
				case CA_NAME: return "Conservation Area Name";
				case WAYPOINT_SOURCE: return "Waypoint Source";
				case WAYPOINT_ID: return "Waypoint ID";
				case WAYPOINT_DATE: return "Waypoint Date";
				case WAYPOINT_TIME: return "Waypoint Time";
				case WAYPOINT_X: return "X";
				case WAYPOINT_Y: return "Y";
				case WAYPOINT_DIRECTION: return "Direction";
				case WAYPOINT_DISTANCE: return "Distance";
				case WAYPOINT_COMMENT: return "Comment";
				case WAYPOINT_OBSERVER: return "Observer";
			}
		}
		return null;
	}

	
}
