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
package org.wcs.smart.asset.query.model.types;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.asset.query.model.AssetQueryResultItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.query.model.types.AbstractZoomToInfoProvider;
import org.wcs.smart.query.common.engine.IQueryImageData;
import org.wcs.smart.query.common.engine.IResultItem;

/**
 * Zoom to provider for asset data queries.
 * 
 * @author Emily
 *
 */
public class AssetZoomToResultProvider extends AbstractZoomToInfoProvider {

	@Override
	public void doWork(IResultItem resultItem) {
		if (resultItem instanceof AssetQueryResultItem) {
			AssetQueryResultItem item = (AssetQueryResultItem) resultItem;
			if (item.getWaypointUuid() != null){
				zoomTo(item.getWaypointX(null), item.getWaypointY(null));
				return;
			}
			
		}
		if (resultItem instanceof IQueryImageData) {
			Waypoint wp = null;
			try(Session s = HibernateManager.openSession()){
				ObservationAttachment a = s.get(ObservationAttachment.class, ((IQueryImageData)resultItem).getAttachment().getUuid());
				if (a != null) {
					wp = a.getObservation().getWaypoint();
				}else {
					WaypointAttachment ww = s.get(WaypointAttachment.class, ((IQueryImageData)resultItem).getAttachment().getUuid());
					if (ww != null) wp = ww.getWaypoint();
				}
				if (wp != null) {
					wp.getX();
					wp.getY();
				}
			}
			if (wp != null) {
				zoomTo(wp.getX(), wp.getY());
				return;
			}
		}
		
		MessageDialog.openError(
				Display.getDefault().getActiveShell(),
				ERROR_STR,
				MessageFormat.format(OP_NOT_SUPPORTED_STR,resultItem.getClass().getName()));
		
	}
}
