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

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.asset.query.AssetQueryPlugIn;
import org.wcs.smart.asset.query.model.AssetQueryResultItem;
import org.wcs.smart.asset.query.model.AssetWaypointQuery;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.WaypointObservation;
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
			//TODO: implement me
//			try(Session s = HibernateManager.openSession()){
//				PatrolWaypoint pw = AssetQueryPlugIn.findWaypoint(s, (IQueryImageData)resultItem);
//				if (pw == null) return;
//				
//				WaypointObservation wo = AssetQueryPlugIn.findObservation(s, (IQueryImageData)resultItem);
//				AssetQueryResultItem tmp = new AssetQueryResultItem();
//				tmp.setPatrolUuid(pw.getPatrolLegDay().getPatrolLeg().getPatrol().getUuid());
//				//tmp.setPatrolLegUuid(pw.getPatrolLegDay().getPatrolLeg().getUuid());
//				tmp.setWaypointUuid(pw.getWaypoint().getUuid());
//				//TODO: this is only applicable for observation queries
//				if (wo != null) {
//					tmp.setObservationUuid(wo.getUuid());
//				}else if (!pw.getWaypoint().getObservations().isEmpty()) {
//					//this attachment is associated with a waypoint so pick any random observation to zoom to
//					tmp.setObservationUuid(pw.getWaypoint().getObservations().get(0).getUuid());
//				}
//				resultItem = tmp;
//			}
		}
		IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		EPartService service = ctx.get(EPartService.class);
		for (MPart p : service.getParts()){
//			if (p.isVisible() && p.getTags().contains("active")){ //$NON-NLS-1$
//				Object src = null;
//				if (E3Utils.isCompatibilityEditor(p)){
//					src = E3Utils.getSourceObject(p);
//				}else{
//					src = p.getObject();
//				}
//				if (src instanceof AssetQueryResultsEditor){
//					AssetQueryResultsEditor e = (AssetQueryResultsEditor) src;
//					e.showTablePage();
//					e.getQueryResultsTable().revealSelection(resultItem);	
//				}
//				if (src instanceof QueryResultsEditor){
//					QueryResultsEditor e = (QueryResultsEditor) src;
//					if (e.getQuery().getTypeKey().equals(AssetWaypointQuery.KEY)) ((AssetQueryResultItem)resultItem).setObservationUuid(null);
//					e.showTablePage();
//					e.getQueryResultsTable().revealSelection(resultItem);	
//				}
//			}
		}
	}
}
