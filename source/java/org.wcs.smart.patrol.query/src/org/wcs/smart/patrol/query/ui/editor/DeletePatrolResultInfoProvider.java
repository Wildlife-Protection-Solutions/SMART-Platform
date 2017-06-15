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

import java.text.DateFormat;
import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.patrol.query.engine.PatrolQueryMemoryResult;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.IQueryEditCommand;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Deletes an observation/waypoint from the results table.
 * 
 * @author Emily
 *
 */
public class DeletePatrolResultInfoProvider extends IQueryEditCommand {

	@Override
	public String getName() {
		return DialogConstants.DELETE_BUTTON_TEXT;
	}

	@Override
	public Image getImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON);
	}

	@Override
	public boolean doWork(IResultItem resultItem, IQueryResult results){
		PatrolQueryResultItem item = (PatrolQueryResultItem)resultItem;
		if (!MessageDialog.openQuestion(Display.getDefault().getActiveShell(), Messages.DeletePatrolResultInfoProvider_DeleteTitle,
				MessageFormat.format(Messages.DeletePatrolResultInfoProvider_ConfirmDelete, item.getPatrolId(), DateFormat.getDateInstance().format(item.getPatrolStartDate()), DateFormat.getDateInstance().format(item.getPatrolEndDate())))){
			return false;
		}
		try{
			if (results instanceof PatrolQueryMemoryResult )
				return ((PatrolQueryMemoryResult)results).deletePatrol(item.getPatrolUuid());
			return false;
		}catch (Exception ex){
			QueryPlugIn.displayLog(Messages.DeleteObservationResultInfoProvider_DeleteWpError + ex.getMessage(), ex);
			return false;
		}
	}


	@Override
	public boolean supportsCcaa() {
		return false;
	}
	
	@Override
	public boolean supportsMap(){
		return true;
	}


}
