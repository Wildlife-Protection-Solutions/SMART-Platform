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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.Query.QueryType;
import org.wcs.smart.query.model.QueryInput;
import org.wcs.smart.query.ui.gridded.GriddedEditor;
import org.wcs.smart.query.ui.observation.QueryResultsEditor;
import org.wcs.smart.query.ui.patrol.PatrolQueryResultsEditor;
import org.wcs.smart.query.ui.summary.SummaryEditor;

/**
 * Command handler for adding a new folder to 
 * the query list view.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class OpenQueryHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		ISelection thisSelection = HandlerUtil.getCurrentSelection(event);
		if (thisSelection == null || thisSelection.isEmpty() || !(thisSelection instanceof IStructuredSelection) ){
			return null;
		}
		for (Iterator<?> iterator = ((IStructuredSelection)thisSelection).iterator(); iterator.hasNext();) {
			Object o = (Object) iterator.next();
			if (o instanceof QueryInput){
				openQuery((QueryInput) o);
			}	
		}
		return null;
		
	}
	
	/**
	 * Opens the appropriate editor for the given 
	 * query input.
	 * 
	 * @param input
	 */
	public static void openQuery(QueryInput input){
		
		String editorid = null;
		if (input.getType() == QueryType.OBSERVATION){
			editorid = QueryResultsEditor.ID;
		}else if (input.getType() == QueryType.SUMMARY){
			editorid = SummaryEditor.ID;
		}else if (input.getType() == QueryType.PATROL){
			editorid = PatrolQueryResultsEditor.ID;
		}else if (input.getType() == QueryType.GRIDDED){
			editorid = GriddedEditor.ID;
		}
		if (editorid != null){
			try {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, editorid);	
			} catch (Throwable t) {
				QueryPlugIn.displayLog(t.getMessage(), t);
			}
		}
		
	}

}
