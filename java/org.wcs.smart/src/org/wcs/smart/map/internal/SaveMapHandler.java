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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.map.internal.settings.MapSettings;
import org.wcs.smart.ui.internal.SaveBasemapDialog;
import org.wcs.smart.ui.map.MapView;


/**
 * Save basemap handler
 * @author egouge
 * @since 1.0.0
 */
public class SaveMapHandler extends AbstractHandler {
	public final static String ID = "org.wcs.smart.action.SaveMapAction";

	
	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		if (part instanceof MapView){
			MapView view = (MapView)part;
			
			view.setTool(ID);
			Map map = view.getMap();
			if(map == null) return null;
			
			SaveBasemapDialog dialog = new SaveBasemapDialog(Display.getDefault().getActiveShell());
			if (dialog.open() != IDialogConstants.OK_ID){
				return null;
			}
			BasemapDefinition mapDef = dialog.getBasemap();
			MapSettings settings = MapSettings.getInstance(mapDef);
			settings.save(map);
			
			
			//update map blackboard
			map.getBlackboard().put(MapSettings.BASEMAP_BLACKBOARD_KEY, map.getLayersInternal());
		}
		return null;
	}



}
