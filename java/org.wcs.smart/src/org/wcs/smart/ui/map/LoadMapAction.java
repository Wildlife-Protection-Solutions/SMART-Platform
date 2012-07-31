/**
 * 
 */
package org.wcs.smart.ui.map;

import net.refractions.udig.project.internal.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.internal.settings.MapSettings;
import org.wcs.smart.ui.internal.LoadBasemapDialog;

/**
 * FIXME WARNING only for testing purpose. This class should be removed from release version.
 * 
 * <p>
 * This action was added in order to avoid restart the application (in development environment).
 * It shouldn't be part of the final product.  
 * </p>
 * 
 * @author Mauricio Pazos
 *
 */
public class LoadMapAction implements IViewActionDelegate {

	private MapView view;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(IAction action) {
		final Map map = this.view.getMap();
		if(map == null) return;
		
		final LoadBasemapDialog dialog = new LoadBasemapDialog(Display.getDefault().getActiveShell());
		if (dialog.open() != IDialogConstants.OK_ID){
			return;
		}
		Job loadMap = new Job("restore basemap"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				MapSettings settings = MapSettings.getInstance(dialog.getBasemap()); 
				settings.applyTo(map);
				return Status.OK_STATUS;
			}
		};
		loadMap.schedule();
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IViewActionDelegate#init(org.eclipse.ui.IViewPart)
	 */
	@Override
	public void init(IViewPart view) {
		this.view = (MapView)view;

	}

}
