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
package org.wcs.smart.patrol.internal.ui.createpatrol;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.PatrolTransportComposite;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.LabelConstants;
import org.wcs.smart.patrol.ui.NewPatrolWizardPage;

/**
 * Wizard page to gather patrol transport type.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class TransportTypeWizardPage extends NewPatrolWizardPage {

	private PatrolTransportComposite transportType;
	
	/**
	 * 
	 */
	public TransportTypeWizardPage() {
		super("TransportType"); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite center = new Composite(main, SWT.NONE);
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		transportType = new PatrolTransportComposite();
		transportType.createComponent(center, SWT.NONE);
		
		Label lbl = new Label(center, SWT.WRAP);
		lbl.setText(Messages.TransportTypeWizardPage_MultiTransportType_InfoLabel2);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 2,1);
		gd.widthHint = getShell().getBounds().width / 2;
		lbl.setLayoutData(gd);
		
		
		setControl(main);
		
		setTitle(LabelConstants.TRANSPORT_MODE);
		setMessage(Messages.TransportTypeWizardPage_PageMessage2);
	}

	/**
     * @see org.wcs.smart.patrol.ui.NewPatrolWizardPage#initModel(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
     */
    @Override
    public void initModel(Patrol p, Session session) {
       	transportType.setValues(p, session);
    }
    
	/**
	 * @see org.wcs.smart.patrol.ui.NewPatrolWizardPage#updateModel(org.wcs.smart.patrol.model.Patrol)
	 */
	@Override
	public boolean updateModel(Patrol p, Session session) {
		if (transportType.updatePatrol(p, session)){
			setPageComplete(true);
			return true;
		}else{
			return false;
		}
	}

	
}
