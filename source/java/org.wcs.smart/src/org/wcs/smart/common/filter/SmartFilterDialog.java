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
package org.wcs.smart.common.filter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.SmartStyledTitleDialog;

/**
 * Filter dialog for filtering smart object displayed in the list views.
 * Provides consistent layout for all filtering dialogs
 * 
 * @author elitvin
 * @since 1.0.0
 */
public abstract class SmartFilterDialog extends SmartStyledTitleDialog {

	protected static final int APPLY_ID = 4;
	protected static final int DEFAULTS_ID = 8;

	private IUpdatableView view;
	
	public SmartFilterDialog(Shell parentShell, IUpdatableView view) {
		super(parentShell);
		this.view = view;
	}

	protected Composite createGroupComposite(String title, Composite parent) {
		Group comp = new Group(parent,  SWT.NONE);
		comp.setText(title);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		comp.setLayout(new GridLayout(1, false));
		return comp;
	}
	
	@Override
	protected void okPressed() {
		updateFilterModel();
		applyFilterToView();
		super.okPressed();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, APPLY_ID, Messages.SmartFilterDialog_Apply_Button, false);
		createButton(parent, DEFAULTS_ID, Messages.SmartFilterDialog_Reset_Button, false);
		super.createButtonsForButtonBar(parent);		
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == DEFAULTS_ID){
			resetFilterModel();
			updateControlsValues();
		}else if (buttonId == APPLY_ID){
			updateFilterModel();
			applyFilterToView();
		}
		super.buttonPressed(buttonId);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
	
	protected void applyFilterToView() {
		view.updateContent();
	}
	

	/**
	 * Updates the current filter values
	 */
	protected abstract void updateFilterModel();

	/**
	 * Resets the filter values to the default
	 */
	protected abstract void resetFilterModel();
	
	/**
	 * Updates the widgets with the values from the current filter
	 */
	protected abstract void updateControlsValues();

}
