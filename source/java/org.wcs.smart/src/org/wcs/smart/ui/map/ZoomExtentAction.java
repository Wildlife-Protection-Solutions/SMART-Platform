package org.wcs.smart.ui.map;

import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.command.navigation.SetViewportBBoxCommand;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.geotools.geometry.jts.ReferencedEnvelope;

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
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IViewPart view) {
		this.view = (MapView) view;

	}

}
