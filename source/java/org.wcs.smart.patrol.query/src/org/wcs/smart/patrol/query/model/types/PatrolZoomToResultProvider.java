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
package org.wcs.smart.patrol.query.model.types;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.observation.query.model.types.AbstractZoomToInfoProvider;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Zoom to provider for patrol data queries.
 * 
 * @author Emily
 *
 */
public class PatrolZoomToResultProvider extends AbstractZoomToInfoProvider {

	@Override
	public void doWork(Object resultItem) {
		if (resultItem instanceof PatrolQueryResultItem) {
			PatrolQueryResultItem item = (PatrolQueryResultItem) resultItem;
			if (item.getWaypointUuid() != null){
				zoomTo(item.getWaypointX(null), item.getWaypointY(null));
				return;
			}else{
				Geometry g = item.asGeometry(PatrolQueryResultItem.TRACK_GEOMCOLUMN_KEY);
				if (g != null){
					zoomTo(g);
					return;
				}else{
					MessageDialog.openError(
						Display.getDefault().getActiveShell(),
						ERROR_STR,
						Messages.PatrolZoomToResultProvider_TrackGeomNotFound);
				}
			}
		}
		
		MessageDialog.openError(
				Display.getDefault().getActiveShell(),
				ERROR_STR,
				MessageFormat.format(OP_NOT_SUPPORTED_STR,resultItem.getClass().getName()));
		
	}
}
