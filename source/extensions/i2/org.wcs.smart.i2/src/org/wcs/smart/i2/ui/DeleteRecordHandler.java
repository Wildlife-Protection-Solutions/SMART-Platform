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
package org.wcs.smart.i2.ui;

import java.text.MessageFormat;
import java.util.Collection;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.RecordManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;

public class DeleteRecordHandler {

	/**
	 * 
	 * @param toDelete list of IntelRecord or RecordEditorInput
	 * @param context
	 * @return
	 */
	public boolean deleteRecords(Collection<Object> toDelete, IEclipseContext context){
		if (toDelete.isEmpty()) return false;
		
		String confirmMessage = MessageFormat.format(Messages.DeleteRecordHandler_DeleteConfirmMulti,  toDelete.size());
		if (toDelete.size() == 1){
			Object obj = toDelete.iterator().next();
			if (obj instanceof IntelRecord ){
				confirmMessage = MessageFormat.format(Messages.DeleteRecordHandler_DeleteConfirmSingle,  ((IntelRecord)obj).getTitle() );
			}else if (obj instanceof RecordEditorInput){
				confirmMessage = MessageFormat.format(Messages.DeleteRecordHandler_DeleteConfirmSingle,  ((RecordEditorInput)obj).getName() );
			}
			
		}
		
		String message = null;
		InputDialog confirm = new InputDialog(context.get(Shell.class), Messages.RecordsView_DeleteTitle, 
				(message == null ? "" :  message + "\n") + confirmMessage, "",null){ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					
			@Override
			protected void okPressed() {
				if (!HibernateManager.validatePassword(getText().getText(), SmartDB.getCurrentEmployee())){
					setErrorMessage(Messages.DeleteRecordHandler_InvalidPassword);
				}else{
					setReturnCode(OK);
					close();
				}
			}
			
			@Override
			protected int getInputTextStyle() {
				return super.getInputTextStyle() | SWT.PASSWORD;
			}
		};
		if (confirm.open() != Window.OK){
			return false;
		}
		
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(context.get(Shell.class));
		try {
			pmd.run(true, true, (monitor)-> RecordManager.INSTANCE.deleteRecords(toDelete, context,monitor));
		} catch (Exception ex) {
			Intelligence2PlugIn.displayLog(Messages.RecordsView_DeleteErrorMessage + ex.getMessage(), ex);
		}
		return true;
	}
}
