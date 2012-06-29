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
package org.wcs.smart.query.internal.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.Query.QueryType;
import org.wcs.smart.query.model.QueryInput;

/**
 * Handler for creating a new query that
 * prompts the user for the type of query they want o created.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CreateQueryHandler extends AbstractHandler {

	private boolean showOptionDialog = true;
	
	/**
	 * 
	 * @param showOptionDialog <code>true</code> if the user
	 * should be prompted to pick the type of query
	 * to create.
	 */
	public CreateQueryHandler(boolean showOptionDialog){
		this.showOptionDialog = showOptionDialog;
	}
	
	/**
	 * Handler that creates a new observation
	 * query.
	 */
	public CreateQueryHandler(){
		this(true);
	}
	
	/**
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		
		//display a dialog to allow users to select the type of
		//query they want to create
		QueryType type = QueryType.OBSERVATION;
		if (showOptionDialog){
			QueryTypeDialog dialog = new QueryTypeDialog(HandlerUtil.getActiveShell(event));
			if (dialog.open() != IDialogConstants.OK_ID){
				return null;
			}
			type = dialog.getSelectedQueryType();
		}
		
		QueryInput input = new QueryInput(type);
		
		try {
			IWorkbenchPage page = null;
			try {
				page =  PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				page.openEditor(input, input.getType().getEditorId());						
			} catch (Throwable t) {
				QueryPlugIn.displayLog(t.getMessage(), t);
			}
		} catch (Exception e) {
			QueryPlugIn.displayLog("Error loading query editor.", e);
		}
		return null;
	}

}
