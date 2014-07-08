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
package org.wcs.smart.map.internal;

import net.refractions.udig.project.internal.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.map.internal.settings.MapSettings;
import org.wcs.smart.ui.internal.LoadBasemapDialog;
import org.wcs.smart.ui.map.MapView;

/**
 * Load basemap handler
 * @author egouge
 * @since 1.0.0
 */
public class LoadBasemapHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		if (part instanceof MapView){
			MapView view = (MapView)part;
			final Map map = view.getMap();
			if(map == null) return null;
		
			loadBasemap(map);
		}
		return null;
	}
	
	/**
	 * Opens the basemap selection dialog and updates the basemap
	 * of the current map.
	 * 
	 * @param map
	 */
	public static void loadBasemap(final Map map){
		final BasemapDefinition[] def =  new BasemapDefinition[]{null};
		
		Display.getDefault().syncExec(new Runnable(){

			@Override
			public void run() {
				final LoadBasemapDialog dialog = new LoadBasemapDialog(Display.getDefault().getActiveShell());
				if (dialog.open() != IDialogConstants.OK_ID){
					return ;
				}

				def[0] = dialog.getBasemap();
			}});
				
		if (def[0] == null){
			return;
		}
		
		Job loadMap = new Job(Messages.LoadBasemapHandler_JobName){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				MapSettings settings = MapSettings.getInstance(def[0]); 
				settings.applyTo(map);
				return Status.OK_STATUS;
			}
		};
		loadMap.schedule();
	}
}
