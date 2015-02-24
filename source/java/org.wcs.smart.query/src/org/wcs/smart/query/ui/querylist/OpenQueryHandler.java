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
package org.wcs.smart.query.ui.querylist;

import java.util.Iterator;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
/**
 * Command handler for adding a new folder to 
 * the query list view.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class OpenQueryHandler {

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection) {
		if (thisSelection == null || !(thisSelection instanceof IStructuredSelection) 
				|| ((IStructuredSelection)thisSelection).isEmpty() ){
			return;
		}
		for (Iterator<?> iterator = ((IStructuredSelection)thisSelection).iterator(); iterator.hasNext();) {
			Object o = (Object) iterator.next();
			if (o instanceof QueryEditorInput){
				openQuery((QueryEditorInput) o);
			}	
		}
		
	}
	
	/**
	 * Opens the appropriate editor for the given 
	 * query input.
	 * 
	 * @param input
	 */
	public void openQuery(QueryEditorInput input){
		try {
			if (SmartDB.isMultipleAnalysis()){
				//ensure data model is loaded; otherwise end up with deadlocking issues
				QueryDataModelManager.getInstance().getDataModel();
			}
			 
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, input.getType().getEditorId());	
		} catch (Throwable t) {
			QueryPlugIn.displayLog(t.getLocalizedMessage(), t);
		}
	}

	public static class OpenQueryHandlerWrapper extends DIHandler<OpenQueryHandler>{
		public OpenQueryHandlerWrapper(){
			super(OpenQueryHandler.class);
		}
	}
}
