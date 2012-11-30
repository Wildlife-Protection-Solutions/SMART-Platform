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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query.QueryType;
import org.wcs.smart.query.model.QueryInput;

/**
 * Generic handler for creating new queries.
 * @author egouge
 *
 */
public class CreateHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			String activeId = HandlerUtil.getActivePart(event).getSite().getPage().getPerspective().getId();
			if (!activeId.equals(QueryPerspective.ID)){
				//show query persepective
				HandlerUtil
				.getActiveWorkbenchWindow(event)
				.getWorkbench()
				.showPerspective(QueryPerspective.ID,
						HandlerUtil.getActiveWorkbenchWindow(event));	
			}
			
		} catch (WorkbenchException e) {
			QueryPlugIn
					.displayLog(Messages.CreateHandler_QueryPerspectiveError, e);
		}
		return null;
	}
	
	
	
	protected void createQuery(QueryType type){
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
			QueryPlugIn.displayLog(Messages.CreateHandler_QueryEditorError, e);
		}
	}

}
