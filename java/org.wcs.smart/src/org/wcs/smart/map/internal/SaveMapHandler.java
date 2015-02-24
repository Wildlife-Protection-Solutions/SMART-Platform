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

import java.text.MessageFormat;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.locationtech.udig.project.internal.Map;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.map.internal.settings.MapSettings;
import org.wcs.smart.ui.internal.SaveBasemapDialog;
import org.wcs.smart.util.E3Utils;


/**
 * Save basemap handler
 * @author egouge
 * @since 1.0.0
 */
public class SaveMapHandler {

	@Execute
	public void execute(MPart activePart, final Shell shell){
		Object x = E3Utils.getSourceObject(activePart);
		Map map = null;
		if (x instanceof IAdaptable){
			map = (Map) ((IAdaptable) x).getAdapter(Map.class);
		}
		if (map == null) return;
		
		SaveBasemapDialog dialog = new SaveBasemapDialog(shell);
		if (dialog.open() != IDialogConstants.OK_ID){
			return;
		}
		
		final BasemapDefinition mapDef = dialog.getBasemap();
		if (mapDef.getUuid() != null){
			boolean ok = MessageDialog.openConfirm(shell, Messages.SaveMapHandler_OverwriteDialogTitle, MessageFormat.format(Messages.SaveMapHandler_OverwriteMessage, new Object[]{mapDef.getName()}));
			if (!ok) return;
		}
		
		final Map localMap = map;
		Job j = new Job(Messages.SaveMapHandler_JobName){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				MapSettings settings = MapSettings.getInstance(mapDef);
				try{
					settings.save(localMap);
				}catch (final Exception ex){
					SmartPlugIn.displayLog(Messages.MapSettings_Error_SaveBasemap + "\n\n" +  ex.getLocalizedMessage(), ex);		 //$NON-NLS-1$
					return Status.OK_STATUS;
				}
				
				//update the current map to reference the settings
				//this will update the layers to point to the
				//filestore
				settings.applyTo(localMap);
				shell.getDisplay().asyncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog.openInformation(shell, Messages.SaveMapHandler_SaveOkDialogTitle, Messages.SaveMapHandler_SaveOkMessage);
					}});
				
				return Status.OK_STATUS;
			}	
		};
		j.schedule();
		return;
	}
	
	public static class SaveMapHandlerWrapper extends DIHandler<SaveMapHandler>{
		public SaveMapHandlerWrapper() {
			super(SaveMapHandler.class);
		}
		
	}
}
