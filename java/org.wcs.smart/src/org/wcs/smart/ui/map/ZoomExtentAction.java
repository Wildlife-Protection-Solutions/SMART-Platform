/* uDig - User Friendly Desktop Internet GIS client
 * http://udig.refractions.net
 * (C) 2004-2008, Refractions Research Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package org.wcs.smart.ui.map;

import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.command.navigation.SetViewportBBoxCommand;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * Action to zoom to full extent of map
 * @author Emily
 * @since 1.0.0
 */
public class ZoomExtentAction implements IViewActionDelegate {

	private MapView view = null;

	@Override
	public void run(IAction action) {
		Map map = view.getMap();
		ReferencedEnvelope bounds = map.getBounds(new NullProgressMonitor());
		map.sendCommandASync(new SetViewportBBoxCommand(bounds));

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	@Override
	public void init(IViewPart view) {
		this.view = (MapView) view;

	}

}
