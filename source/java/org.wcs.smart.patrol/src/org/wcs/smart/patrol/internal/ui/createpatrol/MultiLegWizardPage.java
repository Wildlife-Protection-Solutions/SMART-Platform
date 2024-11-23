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

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.ui.NewPatrolWizardPage;

/**
 * Wizard page to select if the
 * patrol is a multi-leg patrol 
 * of not.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class MultiLegWizardPage  extends NewPatrolWizardPage {

	private static final String NAME = "IsMultiLegPatrol"; //$NON-NLS-1$
	
	private Button btnYes;
	private Button btnNo;
	
	/**
	 * Creates a new wizard page
	 */
	public MultiLegWizardPage() {
		super(NAME);
		
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(1, false));
		
		Composite center = new Composite(main, SWT.NONE);
		center.setLayout(new GridLayout(1, false));
		center.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText(Messages.MultiLegWizardPage_IsMulti_Label2);
		
		Composite buttonPanel = new Composite(center, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(1, false));
		buttonPanel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		

		btnNo = new Button(buttonPanel, SWT.RADIO);
		btnNo.setText(Messages.MultiLegWizardPage_OpNo);
		btnNo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		btnNo.setSelection(true);
		btnNo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getWizardInternal().setCanFinish(true);
				getWizardInternal().getContainer().updateButtons();
				
			}
		});
		btnYes = new Button(buttonPanel, SWT.RADIO);
		btnYes.setText(Messages.MultiLegWizardPage_OpYes);
		btnYes.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		btnYes.setSelection(false);
		btnYes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getWizardInternal().setCanFinish(false);
				getWizardInternal().getContainer().updateButtons();
			}
		});
		
		lbl = new Label(center, SWT.WRAP);
		lbl.setText(Messages.MultiLegWizardPage_LegInfo_Label2);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		lbl.setLayoutData(gd);
		
		setTitle(Messages.MultiLegWizardPage_Title2);
		setMessage(Messages.MultiLegWizardPage_PageMessage2);
		super.setControl(main);
	}
    
	@Override
	public void setVisible(boolean isVisible){
		super.setVisible(isVisible);
		if (isVisible){
			((CreatePatrolWizard)getWizard()).getContainer().updateButtons();
		}
	}
	/**
	 * @see org.wcs.smart.patrol.ui.NewPatrolWizardPage#initModel(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
	 */
	@Override
    public void initModel(Patrol p, Session session) {
       	if (p.getLegs() != null && p.getLegs().size() == 1){
       		btnNo.setSelection(true);
       		btnYes.setSelection(false);
       		getWizardInternal().setCanFinish(true);
       	}else{
       		btnYes.setSelection(true);
       		btnNo.setSelection(false);
       		getWizardInternal().setCanFinish(false);
       	}
       	setPageComplete(true);
    }
	
	/**
	 * @see org.wcs.smart.patrol.ui.NewPatrolWizardPage#updateModel()
	 */
	@Override
	public boolean updateModel(Patrol p, Session session) {
		if (btnNo.getSelection()){
			if (p.getLegs().size() > 1){
				PatrolLeg leg1 = p.getLegs().get(0);
				ArrayList<PatrolLeg> newLegs = new ArrayList<PatrolLeg>();
				newLegs.add(leg1);
				leg1.setStartDate(p.getStartDate());
				leg1.setEndDate(p.getEndDate());
				leg1.setId("1"); //$NON-NLS-1$
				leg1.setPatrol(p);
				p.setLegs(newLegs);
			}
		}
		return true;
	}
	
	@Override
	public IWizardPage getNextPage(){
		if (btnNo.getSelection()){
			//skip the patrol legs wizard page
			IWizardPage page = getWizard().getNextPage(this);
			((WizardPage)page).setPageComplete(true);
			return getWizard().getNextPage(page);
		}
		return getWizard().getPage(PatrolLegsWizardPage.PAGE_NAME);
	}

}