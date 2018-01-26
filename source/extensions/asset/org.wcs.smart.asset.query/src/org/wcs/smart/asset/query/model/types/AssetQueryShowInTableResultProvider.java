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

import java.util.UUID;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.asset.query.model.AssetQueryResultItem;
import org.wcs.smart.asset.query.model.AssetWaypointQuery;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.query.common.engine.IQueryImageData;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.ui.QueryResultsEditor;
import org.wcs.smart.query.common.ui.ShowInTableInfoProvider;
import org.wcs.smart.util.E3Utils;

public class AssetQueryShowInTableResultProvider extends ShowInTableInfoProvider {
	/**
	 * Find the query result editor, show the table page and reveal
	 * the given resultItem
	 */
	@Override
	public void doWork(IResultItem resultItem) {
		if (resultItem instanceof IQueryImageData) {
			UUID wpUuid = null;
			UUID obsUuid = null;
			try(Session s = HibernateManager.openSession()){
				ObservationAttachment a = s.get(ObservationAttachment.class, ((IQueryImageData)resultItem).getAttachment().getUuid());
				if (a == null) {
					//waypoint attachment
					WaypointAttachment wa = s.get(WaypointAttachment.class, ((IQueryImageData)resultItem).getAttachment().getUuid());
					wpUuid = wa.getWaypoint().getUuid();
					//pick a random observation to zoom to
					if (!wa.getWaypoint().getObservations().isEmpty()) {
						obsUuid = wa.getWaypoint().getObservations().get(0).getUuid();
					}
				}else {
					obsUuid = a.getObservation().getUuid();
					wpUuid = a.getObservation().getWaypoint().getUuid();
				}

			}
			AssetQueryResultItem tmp = new AssetQueryResultItem();
			tmp.setObservationUuid(obsUuid);
			tmp.setWaypointUuid(wpUuid);
			resultItem = tmp;
		}
		
		IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		EPartService service = ctx.get(EPartService.class);
		for (MPart p : service.getParts()){
			if (p.isVisible() && p.getTags().contains("active")){ //$NON-NLS-1$
				Object src = null;
				if (E3Utils.isCompatibilityEditor(p)){
					src = E3Utils.getSourceObject(p);
				}else{
					src = p.getObject();
				}
				if (src instanceof QueryResultsEditor){
					QueryResultsEditor e = (QueryResultsEditor) src;
					if (e.getQuery().getTypeKey().equals(AssetWaypointQuery.KEY)) ((AssetQueryResultItem)resultItem).setObservationUuid(null);
					e.showTablePage();
					e.getQueryResultsTable().revealSelection(resultItem);	
				}
			}
		}
	}
}
