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

import java.util.List;
import java.util.UUID;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.query.model.AssetQueryResultItem;
import org.wcs.smart.asset.ui.handler.OpenStationHandler;
import org.wcs.smart.asset.ui.views.station.StationEditorInput;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.query.common.engine.IQueryImageData;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.IQueryResultInfoProvider;

/**
 * Intel info provider than opens up the intelligence record associated with the
 * result item.
 * 
 * @author Emily
 *
 */
public class AssetResultInfoProvider implements IQueryResultInfoProvider {

	@Override
	public String getName() {
		return GOTO_SOURCE_STR;
	}

	@Override
	public boolean supportsCcaa() {
		return false;
	}
	
	private void showItem(StationEditorInput in, UUID waypointUuid) {
		IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		ctx.set(OpenStationHandler.STATION_PARAM, in);
		if (waypointUuid != null){
			ctx.set(OpenStationHandler.INIT_SELECTION_WP_UUID, waypointUuid);
		}
		ContextInjectionFactory.invoke(new OpenStationHandler(),
				Execute.class, ctx.getActiveLeaf());
	}
	
	@Override
	public void doWork(IResultItem resultItem) {
		UUID stationUuid = null;
		String stationId = null;
		UUID wpUuid = null;
		
		try(Session s = HibernateManager.openSession()){
			if (resultItem instanceof AssetQueryResultItem) {
				wpUuid = ((AssetQueryResultItem)resultItem).getWaypointUuid();
			}else if (resultItem instanceof IQueryImageData) {
				ObservationAttachment a = s.get(ObservationAttachment.class, ((IQueryImageData)resultItem).getAttachment().getUuid());
				if (a != null) {
					wpUuid = a.getObservation().getWaypoint().getUuid();
				}else {
					WaypointAttachment wa = s.get(WaypointAttachment.class, ((IQueryImageData)resultItem).getAttachment().getUuid());
					if (wa != null) wpUuid = wa.getWaypoint().getUuid();
				}
			}
			if (wpUuid == null) return;
			
			List<AssetWaypoint> wps = QueryFactory.buildQuery(s, AssetWaypoint.class, new Object[] {"waypoint.uuid", wpUuid}).list();
			if (wps.isEmpty()) return;
			stationUuid = wps.get(0).getAssetDeployment().getStationLocation().getStation().getUuid();
			stationId = wps.get(0).getAssetDeployment().getStationLocation().getStation().getId();
		}
		showItem(new StationEditorInput(stationUuid, stationId), wpUuid);
//		(new OpenStationHandler()).openStation(new StationEditorInput(stationUuid, stationId));	
			
//		MessageDialog.openError(Display.getDefault().getActiveShell(),ERROR_STR,
//						MessageFormat .format(OP_NOT_SUPPORTED_STR, resultItem.getClass().getName()));
	}

	@Override
	public Image getImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.GOTO_ICON);
	}
	
	@Override
	public boolean supportsMap(){
		return true;
	}

}
