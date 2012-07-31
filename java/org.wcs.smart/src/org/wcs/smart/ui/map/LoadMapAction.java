/**
 * 
 */
package org.wcs.smart.ui.map;

import net.refractions.udig.project.internal.Map;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.internal.settings.MapSettings;

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
		

		Map map = this.view.getMap();
		if(map == null) return;
		
		MapSettings settings = MapSettings.getInstance(SmartDB.getCurrentEmployee()); 
		settings.applyTo(map);
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
