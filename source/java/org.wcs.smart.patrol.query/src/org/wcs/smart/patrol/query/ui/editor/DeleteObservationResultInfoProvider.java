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
import org.wcs.smart.patrol.query.engine.DerbyPagedObservationResult;
import org.wcs.smart.patrol.query.engine.IWaypointUpdateableResultSet;
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
public class DeleteObservationResultInfoProvider extends IQueryEditCommand {

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
		
		boolean deleteWp = false;
		if (item.getCategories() == null || item.getCategories().length == 0){
			deleteWp = true;
			
			if (!MessageDialog.openQuestion(Display.getDefault().getActiveShell(), Messages.DeleteObservationResultInfoProvider_DeleteWaypointTitle,
					MessageFormat.format(Messages.DeleteObservationResultInfoProvider_DeleteWaypointMsg, item.getWaypointId(), DateFormat.getDateInstance().format(item.getWpDateTime())))){
				return false;
			}
		}else{
			MessageDialog md = new MessageDialog(Display.getDefault().getActiveShell(), 
				Messages.DeleteObservationResultInfoProvider_DeleteTitle, null,
				MessageFormat.format(Messages.DeleteObservationResultInfoProvider_DeleteMsg, item.getCategories()[item.getCategories().length - 1]),
				MessageDialog.QUESTION, 
				new String[]{Messages.DeleteObservationResultInfoProvider_DeleteObsBtn, Messages.DeleteObservationResultInfoProvider_DeleteWpBtn, Messages.DeleteObservationResultInfoProvider_CancelBtn}, 0);
			int index = md.open();
			if (index == 2) return false;
			if (index == 1) deleteWp = true;
		}
		
		if (deleteWp){
			try{
				if (results instanceof IWaypointUpdateableResultSet)
					return ((IWaypointUpdateableResultSet)results).deleteWaypoint(item.getWaypointUuid());
				return false;
			}catch (Exception ex){
				QueryPlugIn.displayLog(Messages.DeleteObservationResultInfoProvider_DeleteWpError + ex.getMessage(), ex);
				return false;
			}
		}else if (results instanceof DerbyPagedObservationResult){
			try{
				return ((DerbyPagedObservationResult)results).deleteObservation(item.getObservationUuid());
			}catch (Exception ex){
				QueryPlugIn.displayLog(Messages.DeleteObservationResultInfoProvider_DeleteObsError + ex.getMessage(), ex);
				return false;
			}
		}
		return false;
		
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
