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
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.ui.waypoint.QueryResultsEditor;

/**
 * Handler for displaying query perspective
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ShowQueryPersepctiveHandler extends AbstractHandler {

	/**
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		try {
			HandlerUtil
					.getActiveWorkbenchWindow(event)
					.getWorkbench()
					.showPerspective(QueryPerspective.ID,
							HandlerUtil.getActiveWorkbenchWindow(event));
			
			IEditorReference[] ref = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getEditorReferences();
			boolean found = false;
			for (int i = 0; i < ref.length; i ++){
				if (ref[i].getId().equals(QueryResultsEditor.ID)){
					found = true;
					break;
				}
			}
			if (!found){
				//open a new editor 
				CreateQueryHandler h = new CreateQueryHandler();
				h.execute(event);
			}
		} catch (WorkbenchException e) {
			QueryPlugIn.displayLog("Error loading Query perspective.", e);
		}
		return null;
	}

}
