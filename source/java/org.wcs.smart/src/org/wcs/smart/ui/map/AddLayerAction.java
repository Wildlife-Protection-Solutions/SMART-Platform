package org.wcs.smart.ui.map;

import net.refractions.udig.project.ui.internal.MapImport;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public class AddLayerAction implements IViewActionDelegate {

	@Override
	public void run(IAction action) {
		MapImport mapImport = new MapImport();
		mapImport.getDialog().open();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	@Override
	public void init(IViewPart view) {
	}

}
