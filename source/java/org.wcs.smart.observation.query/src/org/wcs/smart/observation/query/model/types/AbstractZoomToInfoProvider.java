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
package org.wcs.smart.observation.query.model.types;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.query.model.IQueryResultInfoProvider;
import org.wcs.smart.query.ui.editor.IMapQueryEditor;
import org.wcs.smart.util.E3Utils;
import org.wcs.smart.util.GeometryUtils;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Zoom to map query result command that will zoom the map in the current active
 * editor to the coordinates supplied.
 * 
 * @author Emily
 *
 */
public abstract class AbstractZoomToInfoProvider implements IQueryResultInfoProvider {

	@Override
	public String getName() {
		return Messages.AbstractZoomToInfoProvider_Name;
	}

	@Override
	public Image getImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ZOOM_IMAGE);
	}
	
	@Override
	public boolean supportsCcaa() {
		return true;
	}
	
	/**
	 * Users must override.  Call showItem to display
	 * a given waypoint.
	 */
	@Override
	public abstract void doWork(Object resultItem);	
	
	/**
	 * Zooms to the provided coordinates.  Coordinates must be in lat long.
	 * @param x longitude
	 * @param y latitude
	 */
	protected void zoomTo(double x, double y){
		double offset = 0.001;
		ReferencedEnvelope re = new ReferencedEnvelope(x-offset, x+offset, y-offset, y+offset, GeometryUtils.SMART_CRS);
		zoomTo(re);
	}
	/**
	 * Zoom to a geometry.  The geometry must be provided in lat/long
	 * @param g
	 */
	protected void zoomTo(Geometry g){
		if (g.isEmpty()){
			MessageDialog.openError(Display.getDefault().getActiveShell(),
					ERROR_STR,
					Messages.AbstractZoomToInfoProvider_NoGeometryFound);
			return;
		}
		zoomTo(new ReferencedEnvelope(g.getEnvelopeInternal(), GeometryUtils.SMART_CRS));
	}
	
	/**
	 * Zooms to a referenced envelope
	 * @param env
	 */
	protected void zoomTo(ReferencedEnvelope env){
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
				if (src instanceof IMapQueryEditor){
					((IMapQueryEditor) src).showMapPage(env);
				}else if (src instanceof MapPart){
					final Map map = ((MapPart)src).getMap();
					SetViewportBBoxCommand cmd = new SetViewportBBoxCommand(env, true);
					map.sendCommandASync(cmd);					
				}
			}
		}
	}

}