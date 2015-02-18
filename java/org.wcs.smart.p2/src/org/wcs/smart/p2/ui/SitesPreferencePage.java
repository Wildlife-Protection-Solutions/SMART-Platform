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
package org.wcs.smart.p2.ui;

import org.eclipse.equinox.p2.ui.RepositoryManipulationPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.p2.internal.Messages;

/**
 * Plugin Install Locations preferences page.
 * Same as {@link RepositoryManipulationPage} but is available only to manager and admin users.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class SitesPreferencePage extends RepositoryManipulationPage {

	@Override
	protected Control createContents(Composite parent) {
		if (!isEditable()) {
			Composite main = new Composite(parent, SWT.NONE);
			main.setLayout(new GridLayout(1, false));
			main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

			Label lbl = new Label(main, SWT.WRAP);
			lbl.setText(Messages.SitesPreferencePage_InvalidUser);
			lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			((GridData)lbl.getLayoutData()).widthHint = 100;
			return main;
		}
		return super.createContents(parent);
	}	
	
	@Override
	public boolean performOk() {
		if (!isEditable()) {
			return true;
		}
		return super.performOk();
	}
	
	public void copyToClipboard(Control activeControl) {
		if (isEditable()) {
			super.copyToClipboard(activeControl);
		}
	}
	
	private boolean isEditable(){
		return (SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.ADMIN ||
				SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.MANAGER);
	}
}
