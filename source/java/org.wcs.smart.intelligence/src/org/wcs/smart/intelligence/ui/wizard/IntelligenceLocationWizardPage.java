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
package org.wcs.smart.intelligence.ui.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.ui.wizard.location.LocationSelectComposite;

/**
 * Intelligence Wizard page for collecting the intelligence location(s) information
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceLocationWizardPage extends IntelligenceWizardPage {

	/**
	 * @param pageName
	 */
	public IntelligenceLocationWizardPage() {
		super(Messages.IntelligenceLocationWizardPage_PageTitle);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
        Composite center = new Composite(parent, SWT.NONE);
        center.setLayout(new GridLayout(2, false));
        center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

        Composite locationSelect = new LocationSelectComposite(center, SWT.NONE);
        locationSelect.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        setControl(center);
        setMessage(Messages.IntelligenceLocationWizardPage_Message);
 	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.intelligence.ui.wizard.IntelligenceWizardPage#updateModel(org.wcs.smart.intelligence.model.Intelligence)
	 */
	@Override
	protected boolean updateModel(Intelligence intelligence) {
		// TODO Auto-generated method stub
		return true;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.intelligence.ui.wizard.IntelligenceWizardPage#initModel(org.wcs.smart.intelligence.model.Intelligence, org.hibernate.Session)
	 */
	@Override
	void initModel(Intelligence intelligence, Session session) {
		// TODO Auto-generated method stub

	}

}
