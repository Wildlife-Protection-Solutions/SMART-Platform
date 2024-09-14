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
import org.hibernate.Session;
import org.wcs.smart.patrol.internal.ui.TrackTypeComposite;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.NewPatrolWizardPage;

/**
 * Wizard page to gather track type.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class TrackTypeWizardPage extends NewPatrolWizardPage {

	public static final String ID = "TrackType"; //$NON-NLS-1$
	
	private TrackTypeComposite trackType;
	
	/**
	 * 
	 */
	public TrackTypeWizardPage() {
		super(ID); 
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
		
		trackType = new TrackTypeComposite();
		trackType.createComponent(center, SWT.NONE);
		
		setControl(main);
		
		setTitle("Track Type");
		setMessage("Select track type");
	}

	/**
     * @see org.wcs.smart.patrol.ui.NewPatrolWizardPage#initModel(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
     */
    @Override
    public void initModel(Patrol p, Session session) {
    	trackType.setValues(p, session);
    }
    
	/**
	 * @see org.wcs.smart.patrol.ui.NewPatrolWizardPage#updateModel(org.wcs.smart.patrol.model.Patrol)
	 */
	@Override
	public boolean updateModel(Patrol p, Session session) {
		if (trackType.updatePatrol(p, session)){
			setPageComplete(true);
			return true;
		}else{
			return false;
		}
	}

	
}
