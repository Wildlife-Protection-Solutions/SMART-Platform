package org.wcs.smart.ui.map;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public class ZoomAction implements IViewActionDelegate {
	private MapView view = null;
	
	private static String zoomToolId = "net.refractions.udig.tools.Zoom";
	
	@Override
	public void run(IAction action) {
		view.setModalTool(zoomToolId);

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	@Override
	public void init(IViewPart view) {
		this.view = (MapView)view;

	}

}
