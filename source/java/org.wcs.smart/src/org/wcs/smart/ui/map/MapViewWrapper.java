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

import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.widgets.Control;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;

/**
 * Mapview wrapper for e4
 * @author Emily
 *
 */
public class MapViewWrapper extends DIViewPart<MapView> implements MapPart{

	public MapViewWrapper() {
		super(MapView.class);
	}

	@SuppressWarnings({ "rawtypes" })
	public Object getAdapter(Class adaptee) {
		Object x = getComponent().getAdapter(adaptee);
		if (x != null){
			return x;
		}
		return super.getAdapter(adaptee);
	}

	@Override
	public Map getMap() {
		return getComponent().getMap();
	}

	@Override
	public void openContextMenu() {
		getComponent().openContextMenu();
	}

	@Override
	public void setFont(Control textArea) {
		getComponent().setFont(textArea);
	}

	@Override
	public void setSelectionProvider(
			IMapEditorSelectionProvider selectionProvider) {
		getComponent().setSelectionProvider(selectionProvider);
		
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return getComponent().getStatusLineManager();
	}
}
