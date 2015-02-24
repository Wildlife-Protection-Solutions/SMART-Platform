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
package org.wcs.smart.report.internal.ui;

import org.eclipse.birt.report.designer.ui.editors.IReportEditorContants;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.library.SmartBirtLibrary;
import org.wcs.smart.report.ui.SmartLibraryEditorInput;

/**
 * Edit birt report library handler.
 * 
 * @author egouge
 *
 */
public class EditLibraryHandler {

	@Execute
	public void execute(){
		SmartLibraryEditorInput ri = new SmartLibraryEditorInput(SmartBirtLibrary.getInstance().getLibraryFile());
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(
					ri, IReportEditorContants.LIBRARY_EDITOR_ID, true);
		} catch (Exception ex) {
			ReportPlugIn.displayLog(Messages.EditLibraryHandler_Loading_Error + ex.getLocalizedMessage(), ex);
		}
	}
	
	public static class EditLibraryHandlerWrapper extends DIHandler<EditLibraryHandler>{
		public EditLibraryHandlerWrapper(){
			super(EditLibraryHandler.class);
		}
	}

}
