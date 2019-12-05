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
package org.wcs.smart.i2.ui.handler;

import java.text.MessageFormat;
import java.util.UUID;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntityRecordQuery;
import org.wcs.smart.i2.model.IntelEntitySummaryQuery;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.model.IntelRecordSummaryQuery;
import org.wcs.smart.i2.ui.editors.query.IntelEntityRecordQueryEditor;
import org.wcs.smart.i2.ui.editors.query.IntelRecordObservationQueryEditor;
import org.wcs.smart.i2.ui.editors.query.IntelRecordQueryEditor;
import org.wcs.smart.i2.ui.editors.query.IntelSummaryQueryEditor;
import org.wcs.smart.i2.ui.editors.query.QueryEditorInput;

/**
 * Opens an entity record.
 * 
 * @author Emily
 *
 */
public class OpenQueryHandler {

	public void openQuery(QueryEditorInput editorInput, boolean editMode){
		try {
			if (editorInput.getTypeKey().equals(IntelRecordObservationQuery.KEY)) {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(editorInput, IntelRecordObservationQueryEditor.ID);
			}else if (editorInput.getTypeKey().equals(IntelEntitySummaryQuery.KEY)) {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(editorInput, IntelSummaryQueryEditor.ID);
			}else if (editorInput.getTypeKey().equals(IntelRecordSummaryQuery.KEY)) {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(editorInput, IntelSummaryQueryEditor.ID);
			}else if (editorInput.getTypeKey().equals(IntelEntityRecordQuery.KEY)) {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(editorInput, IntelEntityRecordQueryEditor.ID);
			}else if (editorInput.getTypeKey().equals(IntelRecordQuery.KEY)) {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(editorInput, IntelRecordQueryEditor.ID);
			}
		} catch (PartInitException e) {
			Intelligence2PlugIn.displayLog(MessageFormat.format(Messages.OpenQueryHandler_OpenError, e.getMessage()), e);
		}
	}
	
	public void openQuery(UUID queryUuid, String queryType, boolean editMode){
		QueryEditorInput input = new QueryEditorInput(null, queryUuid, queryType);
		openQuery(input, editMode);
	}
}
