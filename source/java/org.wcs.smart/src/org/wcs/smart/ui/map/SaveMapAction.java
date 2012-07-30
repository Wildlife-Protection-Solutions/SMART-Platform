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
package org.wcs.smart.ui.map;

import net.refractions.udig.project.internal.Map;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.internal.settings.MapSettings;

/**
 * Action associated to the Save Map button.
 * 
 * <p>
 * This action will save the map custom settings
 * </p>
 * 
 * @author Mauricio Pazos
 *
 */
public final class SaveMapAction implements IViewActionDelegate {
	
	public final static String ID = "org.wcs.smart.action.SaveMapAction";

	private MapView view;
	

	/**
	 * Saves the map setting done for the current Employee.
	 */
	@Override
	public void run(IAction action) {
		view.setModalTool(ID);
		
		if(this.view == null) return;

		Map map = this.view.getMap();
		if(map == null) return;
		
		MapSettings settings = MapSettings.getInstance(SmartDB.getCurrentEmployee());
		settings.save(map);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// Null implementation
	}

	/**
	 * Sets the MapView
	 * @param view  a {@link MapView} instance
	 */
	@Override
	public void init(IViewPart view) {
		this.view = (MapView)view;

	}

}
