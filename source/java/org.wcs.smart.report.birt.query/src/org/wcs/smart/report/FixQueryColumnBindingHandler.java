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
package org.wcs.smart.report;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.birt.report.model.api.SlotHandle;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.data.oda.smart.ui.internal.Messages;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.ui.SmartReportEditorInput;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.E3Utils;

/**
 * Handler to run report query column binding fixer
 * 
 * @author Emily
 *
 */
public class FixQueryColumnBindingHandler {

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
		final List<Report> reports = new ArrayList<Report>();
		for (Object sel : selection){
			if (sel instanceof SlotHandle){
				ModuleHandle mhandle = ((SlotHandle)sel).getModule().getRoot().getModuleHandle();
				fixReport(mhandle, activeShell);
				return;
			}
			if (sel instanceof Report){
				reports.add((Report)sel);
			}
		}
		
		List<Report> toSkip = new ArrayList<Report>();
		for (Report r : reports){
			
			//save an close any existing editor
			for (MPart part : partService.getParts()){
				if (E3Utils.isCompatibilityEditor(part)){
					IEditorPart epart = (IEditorPart)E3Utils.getSourceObject(part);
					if (epart.isDirty()){
						if (epart.getEditorInput() instanceof SmartReportEditorInput){
							SmartReportEditorInput ri = (SmartReportEditorInput)epart.getEditorInput();
							if (ri.getReport().equals(r)){
								//do you want to save your changes or skip update
								MessageDialog md = new MessageDialog(activeShell, Messages.FixQueryColumnBindingHandler_SaveDialogTitle, null, MessageFormat.format(Messages.FixQueryColumnBindingHandler_SaveDialogMessage, r.getName()),
										MessageDialog.QUESTION, new String[]{DialogConstants.SAVE_TEXT, Messages.FixQueryColumnBindingHandler_SaveDialogButton1, Messages.FixQueryColumnBindingHandler_SaveDialogButton2},
										0);
								int index = md.open();
								if (index == 0){
									//save
									PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().saveEditor(epart, false);
									//partService.savePart(part, false); -> for compatibility parts this will still prompt
									partService.hidePart(part, true);
								}
								if (index == 1){
									//close and discard
									partService.hidePart(part, true);
									
								}
								if (index == 2){
									toSkip.add(r);
								}
							}
						}
					}
				}
			}
		}
		reports.removeAll(toSkip);
		
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try{
		pmd.run(true, true, new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				monitor.beginTask(Messages.FixQueryColumnBindingHandler_TaskName, reports.size());
				for (Report r : reports){
					monitor.subTask(MessageFormat.format(Messages.FixQueryColumnBindingHandler_SubTask, r.getName()));
					try{
						ReportQueryColumnBindingFixer fixer = new ReportQueryColumnBindingFixer();
						fixer.fixReport(r, monitor);
					} catch (Exception ex) {
						ReportPlugIn.displayLog(
								MessageFormat.format(Messages.FixQueryColumnBindingHandler_ReportError, r.getName(), ex.getLocalizedMessage()), ex);
					}
					monitor.worked(1);
					if (monitor.isCanceled()){
						activeShell.getDisplay().syncExec(new Runnable(){
							@Override
							public void run() {
								MessageDialog.openWarning(activeShell, Messages.FixQueryColumnBindingHandler_CancelledDialog, Messages.FixQueryColumnBindingHandler_CancelledMsg);		
							}
							
						});
						return;
					}
				}
				monitor.done();
			}
		});
		}catch (Exception ex){
			ReportPlugIn.displayLog(
					Messages.FixQueryColumnBindingHandler_ErrorMsg + ex.getLocalizedMessage(), ex);
		}
	}
	
	private void fixReport(final ModuleHandle rm, final Shell activeShell){
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try{
		pmd.run(true, true, new IRunnableWithProgress() {
			
			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				monitor.beginTask(Messages.FixQueryColumnBindingHandler_TaskName, 2);
				monitor.worked(1);
				monitor.subTask(MessageFormat.format(Messages.FixQueryColumnBindingHandler_SubTask, "")); //$NON-NLS-1$
				
				final ReportQueryColumnBindingFixer fixer = new ReportQueryColumnBindingFixer();
				try{
					fixer.fixReport(rm, monitor);
				} catch (Exception ex) {
					ReportPlugIn.displayLog(
							MessageFormat.format(Messages.FixQueryColumnBindingHandler_ModuleErrorMsg, ex.getLocalizedMessage()), ex);
				}
				monitor.worked(1);
				monitor.done();
			}
		});
		}catch (Exception ex){
			ReportPlugIn.displayLog(
					Messages.FixQueryColumnBindingHandler_ErrorMsg + ex.getLocalizedMessage(), ex);
		}
		
	}
	
	public static class FixQueryHandlerWrapper extends DIHandler<FixQueryColumnBindingHandler>{
		public FixQueryHandlerWrapper(){
			super(FixQueryColumnBindingHandler.class);
		}
	}

}
