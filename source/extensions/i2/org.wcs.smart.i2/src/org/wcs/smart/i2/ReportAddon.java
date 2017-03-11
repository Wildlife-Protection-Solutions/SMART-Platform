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
package org.wcs.smart.i2;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.i2.birt.IntelReportManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntityType;

/**
 * Listens for events that would require entity report templates to be 
 * refreshed.  These changes include changes to the entity type or
 * relationship type.  This does not check all templates but assumes
 * only the template associated with the entity type is modified. 
 * 
 * @author Emily
 *
 */
public class ReportAddon {
	
	@Inject
	@Optional
	public void onEntityTypeModified(@UIEventTopic(IntelEvents.ENTITY_TYPE_TEMPLATE_REFRESH) IntelEntityType value, EModelService model, MApplication app) {
		if (value != null && value.getBirtTemplate() != null){
			Path file = IntelReportManager.INSTANCE.getEntityTemplate(value);
			if (!Files.exists(file)) return; 
			onEntityTypesModified(Collections.singleton(value), model, app);
		}
	}
	
	@Inject
	@Optional
	public void onEntityTypesModified(@UIEventTopic(IntelEvents.ENTITY_TYPE_TEMPLATE_REFRESH) Collection<IntelEntityType> values, EModelService model, MApplication app) {
		if (values != null && !values.isEmpty()){
			//update report files
			//this was the only way I could get the part service I needed
			EPartService partService = model.findElements(app, null, MWindow.class, null).get(0).getContext().get(EPartService.class);
			try{
				ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
				pmd.run(true, false, new IRunnableWithProgress() {
					
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						monitor.beginTask(Messages.ReportAddon_ProgressMsg, values.size() + 1);
						monitor.worked(1);
						for (IntelEntityType value : values){
							monitor.subTask(value.getName());
							try {	
								IntelReportManager.INSTANCE.refreshReportDataset(value, partService);
							} catch (Exception e) {
								Intelligence2PlugIn.log(e.getMessage(), e);
							}
							monitor.worked(1);;
						}
						monitor.done();
					}
				});
				
			}catch (Exception e){
				Intelligence2PlugIn.log(e.getMessage(), e);
			}
		}
	}
	
}