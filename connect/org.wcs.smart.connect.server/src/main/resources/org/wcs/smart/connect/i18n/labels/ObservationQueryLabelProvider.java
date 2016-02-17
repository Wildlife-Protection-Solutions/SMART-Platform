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
package org.wcs.smart.connect.i18n.labels;

import java.util.Locale;

import org.wcs.smart.connect.i18n.Messages;
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
				case CA_ID:return Messages.getString("ObservationQueryLabelProvider.IDLabel", l); //$NON-NLS-1$
				case CA_NAME: return Messages.getString("ObservationQueryLabelProvider.CANameLabel", l); //$NON-NLS-1$
				case WAYPOINT_SOURCE: return Messages.getString("ObservationQueryLabelProvider.SourceLabel", l); //$NON-NLS-1$
				case WAYPOINT_ID: return Messages.getString("ObservationQueryLabelProvider.WPidLabel", l); //$NON-NLS-1$
				case WAYPOINT_DATE: return Messages.getString("ObservationQueryLabelProvider.DateLabel", l); //$NON-NLS-1$
				case WAYPOINT_TIME: return Messages.getString("ObservationQueryLabelProvider.TimeLabel", l); //$NON-NLS-1$
				case WAYPOINT_X: return Messages.getString("ObservationQueryLabelProvider.xLabel", l); //$NON-NLS-1$
				case WAYPOINT_Y: return Messages.getString("ObservationQueryLabelProvider.yLabel", l); //$NON-NLS-1$
				case WAYPOINT_DIRECTION: return Messages.getString("ObservationQueryLabelProvider.DirectionLabel", l); //$NON-NLS-1$
				case WAYPOINT_DISTANCE: return Messages.getString("ObservationQueryLabelProvider.DistanceLabel", l); //$NON-NLS-1$
				case WAYPOINT_COMMENT: return Messages.getString("ObservationQueryLabelProvider.CommentLabel", l); //$NON-NLS-1$
				case WAYPOINT_OBSERVER: return Messages.getString("ObservationQueryLabelProvider.ObserverLabel", l); //$NON-NLS-1$
			}
		}
		return null;
	}

	
}
