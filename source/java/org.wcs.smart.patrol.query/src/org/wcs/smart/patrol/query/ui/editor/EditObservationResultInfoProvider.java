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
package org.wcs.smart.patrol.query.ui.editor;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.patrol.query.engine.DerbyPagedObservationResult;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.ui.edit.EditObservationDialog;
import org.wcs.smart.query.model.IQueryEditCommand;

/**
 * Edit an observation from a query result table.
 * 
 * @author Emily
 *
 */
public class EditObservationResultInfoProvider extends IQueryEditCommand {

	@Override
	public String getName() {
		return Messages.EditObservationResultInfoProvider_EditLabel;
	}

	@Override
	public Image getImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON);
	}

	@Override
	public boolean doWork(IResultItem resultItem, IQueryResult result) {
		PatrolQueryResultItem item = (PatrolQueryResultItem)resultItem;
		EditObservationDialog dialog = new EditObservationDialog(Display.getDefault().getActiveShell(), item.getObservationUuid());
		if (dialog.open() == Window.OK){
			try{
				return (((DerbyPagedObservationResult)result).updateObservation(item, dialog.getUpdatedObservation()));
			}catch (Exception ex){
				QueryPlugIn.displayLog(Messages.EditObservationResultInfoProvider_DeleteError + ex.getMessage(), ex);
				return false;
			}
		}
		return false;
	}
	
	public boolean isEditFeature() { 
		return true; 
	}

	@Override
	public boolean supportsCcaa() {
		return false;
	}

}
