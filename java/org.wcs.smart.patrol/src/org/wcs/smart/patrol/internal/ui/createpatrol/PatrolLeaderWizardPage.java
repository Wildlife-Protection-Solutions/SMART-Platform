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

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLegMember;

/**
 * Wizard page to identify patrol leader and pilot is applicable.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 *
 */
public class PatrolLeaderWizardPage extends NewPatrolWizardPage {

	private ComboViewer patrolLeaderViewer = null;
	private ComboViewer patrolPilotViewer = null;

	private Label lblPilot;
	
	/**
	 * @param pageName
	 */
	protected PatrolLeaderWizardPage() {
		super("Patrol Leader");
		
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Patrol p = ((CreatePatrolWizard)super.getWizard()).getPatrol();
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(1, false));
		
		Composite center = new Composite(main, SWT.NONE);
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText("Patrol Leader:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		patrolLeaderViewer = new ComboViewer(center, SWT.DROP_DOWN | SWT.READ_ONLY);
		patrolLeaderViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		patrolLeaderViewer.setContentProvider(ArrayContentProvider.getInstance());
		patrolLeaderViewer.setLabelProvider(new EmployeeLabelProvider());
		
		setMessage("Select the patrol leader.");
		
		lblPilot = new Label(center, SWT.NONE);
		lblPilot.setText("Pilot:");
		lblPilot.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
			
		patrolPilotViewer = new ComboViewer(center, SWT.DROP_DOWN | SWT.READ_ONLY);
		patrolPilotViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		patrolPilotViewer.setContentProvider(ArrayContentProvider.getInstance());
		patrolPilotViewer.setLabelProvider(new EmployeeLabelProvider());
			
		patrolPilotViewer.getControl().setVisible(false);
		lblPilot.setVisible(false);
		
		
		
		super.setControl(main);
	}
	
	/**
	 * 
	 */
	@Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
        	Patrol p = ((CreatePatrolWizard)getWizard()).getPatrol();
        
        	List<PatrolLegMember> members = p.getFirstLeg().getMembers();
        	patrolLeaderViewer.setInput(members.toArray());
        	PatrolLegMember plm = p.getFirstLeg().getLeader();
        	if (plm != null){
        		patrolLeaderViewer.setSelection(new StructuredSelection(plm));
        	}else{
        		patrolLeaderViewer.setSelection(new StructuredSelection(members.get(0)));
        	}
        	
        	
        	if (patrolPilotViewer != null){
        		patrolPilotViewer.setInput(members.toArray());
        		plm = p.getFirstLeg().getPilot();
            	if (plm != null){
            		patrolPilotViewer.setSelection(new StructuredSelection(plm));
            	}else{
            		patrolPilotViewer.setSelection(new StructuredSelection(members.get(0)));
            	}
        	}
        	
        	if (p.hasPilot()){
        		setMessage("Select the patrol leader and patrol pilot.");
        		patrolPilotViewer.getControl().setVisible(true);
        		lblPilot.setVisible(true);
        	}else{
        		setMessage("Select the patrol leader.");
        		patrolPilotViewer.getControl().setVisible(false);
        		lblPilot.setVisible(false);
        	}
        	
        }
    }
	
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.createpatrol.NewPatrolWizardPage#updateModel()
	 */
	@Override
	void updateModel(Patrol p) {
		
    	PatrolLegMember plm = (PatrolLegMember) ((IStructuredSelection)patrolLeaderViewer.getSelection()).getFirstElement();
    	if (plm != null){
    		p.getFirstLeg().setLeader(plm);
    	}
    	if (patrolPilotViewer != null){
    		plm = (PatrolLegMember)((IStructuredSelection)patrolPilotViewer.getSelection()).getFirstElement();
    		if (plm != null){
    			p.getFirstLeg().setPilot(plm);
    		}
    	}
    	
		setPageComplete(true);

	}
}
