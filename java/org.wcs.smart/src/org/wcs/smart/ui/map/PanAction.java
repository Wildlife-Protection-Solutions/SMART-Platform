package org.wcs.smart.ui.map;

import net.refractions.udig.tools.internal.PanTool;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public class PanAction implements IViewActionDelegate {

	private MapView view = null;
	
	private String PanToolId = "net.refractions.udig.tools.Pan";
	private PanTool tool = new PanTool();
	
	@Override
	public void run(IAction action) {
		view.setModalTool(PanToolId);
//		IToolManager manager = ApplicationGIS.getToolManager();
//		tool = (PanTool)manager.findTool("net.refractions.udig.tools.Pan");
//		view.setModalTool(tool);
//		IAction aaction = manager.getToolAction("net.refractions.udig.tools.Pan", "net.refractions.udig.tool.category.pan");
//		aaction.run();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	@Override
	public void init(IViewPart view) {
		this.view = (MapView)view;
	}



}
