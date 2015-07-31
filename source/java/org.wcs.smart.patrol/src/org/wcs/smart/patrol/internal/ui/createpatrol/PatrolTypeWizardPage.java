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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.ui.LabelConstants;
import org.wcs.smart.patrol.ui.NewPatrolWizardPage;

/**
 * Wizard page to determine the type of patrol.
 * @author Emily
 * @since 1.0.0
 */
public class PatrolTypeWizardPage extends NewPatrolWizardPage {

	private List<Button> btnTypes = new ArrayList<Button>();
	
	/**
	 */
	public PatrolTypeWizardPage() {
		super("PatrolType"); //$NON-NLS-1$
		
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Patrol p = ((CreatePatrolWizard)getWizard()).getPatrol();
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(1, false));
		
		Composite center = new Composite(main, SWT.NONE);
		center.setLayout(new GridLayout(1, false));
		center.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText(Messages.PatrolTypeWizardPage_SelectType_Label);
		
		Composite buttonPanel = new Composite(center, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(1, false));
		buttonPanel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		Session session  = ((CreatePatrolWizard)getWizard()).getSession();
		session.beginTransaction();
		List<PatrolType> types;
		try{
			types = PatrolHibernateManager.getActivePatrolTypes(p.getConservationArea(),  session);
		}finally{
			session.getTransaction().rollback();
		}

		for (PatrolType t: types){
			Button btn = new Button(buttonPanel, SWT.RADIO);
			btn.setText(LabelConstants.getLabel(t));
			btn.setData(t);
			btnTypes.add(btn);
			btn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		}
		
		if (btnTypes.size() > 0){
			btnTypes.get(0).setSelection(true);
			setPageComplete(true);
		}else{
			lbl = new Label(buttonPanel, SWT.WRAP);
			lbl.setText(Messages.PatrolTypeWizardPage_Error_NoTypesFound);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			lbl.setLayoutData(gd);
			gd.widthHint = 200;
			setErrorMessage(Messages.PatrolTypeWizardPage_Error_NoTypesFound);
			setPageComplete(false);
		}
		
		setTitle(Messages.PatrolTypeWizardPage_Title);
		setMessage(Messages.PatrolTypeWizardPage_PageMessage);
		super.setControl(main);

	}

	
	/**
	 * @see org.wcs.smart.patrol.ui.NewPatrolWizardPage#updateModel()
	 */
	@Override
	public boolean updateModel(Patrol p, Session session) {
		for (Button btn: btnTypes){
			if (btn.getSelection()){
				p.setPatrolType(  ((PatrolType)btn.getData()).getType() );
				return true;
			}
		}
		return false;
	}


	/**
	 * @see org.wcs.smart.patrol.ui.NewPatrolWizardPage#initModel(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
	 */
	@Override
	public void initModel(Patrol p, Session session) {
	}
	

}
