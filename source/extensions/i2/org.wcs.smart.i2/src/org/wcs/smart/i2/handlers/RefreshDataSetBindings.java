/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.handlers;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.inject.Named;

import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.SlotHandle;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.IntelReportManager;
import org.wcs.smart.i2.internal.Messages;

@SuppressWarnings("restriction")
public class RefreshDataSetBindings {

	@SuppressWarnings("unchecked")
	@Execute
	public void execute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection, 
		EPartService partService, final Shell activeShell){
		
		if (thisSelection == null || 
			!(thisSelection instanceof IStructuredSelection) || 
			((IStructuredSelection)thisSelection).isEmpty() ){		
			return;
		}
		
		final List<Object> selection= ((IStructuredSelection)thisSelection).toList();
		for (Object sel : selection){
			if (sel instanceof SlotHandle){
				ModuleHandle mhandle = ((SlotHandle)sel).getModule().getRoot().getModuleHandle();
				fixReport(mhandle, activeShell);
				return;
			}
		}
	}
	
	private void fixReport(final ModuleHandle rm, final Shell activeShell){
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try{
		pmd.run(true, true, new IRunnableWithProgress() {
			
			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				monitor.beginTask(Messages.RefreshDataSetBindings_TaskName, 2);
				monitor.worked(1);
				
				try{
					IntelReportManager.INSTANCE.refreshReportDataset((ReportDesignHandle)rm.getModuleHandle());
				} catch (Exception ex) {
					Intelligence2PlugIn.displayLog(ex.getMessage(), ex);

				}
				monitor.worked(1);
				monitor.done();
			}
		});
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
		}
		
	}
	
	public static class RefreshDataSetBindingsWrapper extends DIHandler<RefreshDataSetBindings>{
		public RefreshDataSetBindingsWrapper(){
			super(RefreshDataSetBindings.class);
		}
	}

}
