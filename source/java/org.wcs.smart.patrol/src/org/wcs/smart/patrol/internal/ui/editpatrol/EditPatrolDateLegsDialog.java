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
package org.wcs.smart.patrol.internal.ui.editpatrol;

import java.util.Date;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.DateComposite;
import org.wcs.smart.patrol.internal.ui.IPatrolItemChangeListener;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * Dialog for editing patrol dates when there are multiple patrol legs.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class EditPatrolDateLegsDialog extends AbstractPropertyJHeaderDialog{

	private DateComposite item;
	private Date startDate;
	private Date endDate;
	
	private IPatrolItemChangeListener listener = new IPatrolItemChangeListener() {			
		@Override
		public void itemChanged() {
			setChangesMade(true);
			setErrorMessage(item.getErrorMessage());
			if (getButton(IDialogConstants.OK_ID) != null){
				getButton(IDialogConstants.OK_ID).setEnabled(item.getErrorMessage() == null);
			}
		}
	};
	
	/**
	 * 
	 */
	public EditPatrolDateLegsDialog(Shell parent, 
			Date startDate, Date endDate){
		
		super(parent, Messages.EditPatrolDateLegsDialog_Title);
		item = new DateComposite();
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	
	/**
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#close()
	 */
	@Override
	public boolean close(){
		if (super.close()){
			item.removeChangeListener(listener);
			return true;
		}
		return false;
	}
	
	/**
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		Composite comp = item.createComponent(parent, SWT.NONE);
		item.addChangeListener(listener);
		
		item.setValues(startDate,  endDate);
		setTitle(item.getTitle());
		setChangesMade(false);
		return comp;
	}
	
	
	/**
	 * Saves the updates to he database.
	 * 
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	@Override
	protected boolean performSave() {
		setChangesMade(false);
		startDate = item.getStartDate();
		endDate = item.getEndDate();
		return true;
	}
	/**
	 * 
	 * @return selected start date
	 */
	public Date getStartDate(){
		return this.startDate;
	}
	/**
	 * 
	 * @return selected end date
	 */
	public Date getEndDate(){
		return this.endDate;
	}

}