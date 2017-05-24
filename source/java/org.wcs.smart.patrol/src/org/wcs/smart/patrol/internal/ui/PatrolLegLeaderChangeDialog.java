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
package org.wcs.smart.patrol.internal.ui;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for changing the leader of a given leg.  Takes a leg
 * and splits it into two, setting the leader on the second part 
 * of the leg to a newly selected leader.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolLegLeaderChangeDialog extends TitleAreaDialog implements SelectionListener{

	private DateFormat dateTimeFormatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
	private PatrolLeg existingLeg;
	private PatrolLeg newLeg;
	
	
	private LeaderPilotComposite leaderPilotcomp;
	private DateTime startDate;
	private DateTime startTime;
	private Button opStart;
	private Button opCustom;
	private Collection<PatrolLeg> legsToUpdate;
	

	
	/**
	 * Creates a new dialog 
	 * @param parentShell the parent shell
	 * @param patrolLeg the patrol leg to split up
	 * @param legsToUpdate the set of legs to add the new leg to 
	 */
	public PatrolLegLeaderChangeDialog(Shell parentShell, PatrolLeg patrolLeg,
			Collection<PatrolLeg> legsToUpdate) {
		super(parentShell);
		this.existingLeg = patrolLeg;
		this.legsToUpdate = legsToUpdate;
		
		// create a new leg for updating 
		newLeg = new PatrolLeg();
		// clone the members
		List<PatrolLegMember> members = new ArrayList<PatrolLegMember>();
		for (PatrolLegMember existing : existingLeg.getMembers()){
			PatrolLegMember newmem = existing.clone();
			newmem.setPatrolLeg(newLeg);
			members.add(newmem);
		}
		newLeg.setMandate(patrolLeg.getMandate());
		newLeg.setMembers(members);
		newLeg.setPatrol(existingLeg.getPatrol());
		newLeg.setType(existingLeg.getType());
		
		String legId = existingLeg.getId() + Messages.PatrolLegLeaderChangeDialog_LegIdPostfix;
		if (legId.length() > PatrolLeg.ID_MAX_SIZE){
			legId = legId.substring(0, PatrolLeg.ID_MAX_SIZE);
		}
		newLeg.setId( legId  );
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite)super.createDialogArea(parent);
		
		// date  
		Composite timecomp = new Composite(parent, SWT.NONE);
		timecomp.setLayout(new GridLayout(2, false));
		Label lbl = new Label(timecomp, SWT.NONE);
		lbl.setText(Messages.PatrolLegLeaderChangeDialog_DateChange_Label);
		startDate = new DateTime(timecomp, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER | SWT.LONG);
		startDate.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		startDate.addSelectionListener(this);
		
		SmartUtils.initDateDateTimeWidget(startDate, existingLeg.getStartDate());
		
		//time of change
		lbl = new Label(timecomp, SWT.NONE);
		lbl.setText(Messages.PatrolLegLeaderChangeDialog_Time_Label);
		
		Composite opComp = new Composite(timecomp, SWT.NONE);
		opComp.setLayout(new GridLayout(3, false));
		
		opStart = new Button(opComp, SWT.RADIO);
		opStart.setText(Messages.PatrolLegLeaderChangeDialog_OpStartOfDay);
		opStart.setSelection(true);
		opStart.addSelectionListener(this);
		
		opCustom = new Button(opComp, SWT.RADIO);
		opCustom.setText(Messages.PatrolLegLeaderChangeDialog_OpCustome);
		opCustom.addSelectionListener(this);
		startTime = new DateTime(opComp, SWT.TIME | SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER);
		startTime.setEnabled(false);
		startTime.setTime(0, 0, 0);
		startTime.addSelectionListener(this);
		
		/* new leader/pilot */
		leaderPilotcomp = new LeaderPilotComposite();
		Composite c = leaderPilotcomp.createComponent(parent, SWT.NONE);
		c.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		leaderPilotcomp.setValues(this.newLeg, null);
		leaderPilotcomp.addChangeListener(new IPatrolItemChangeListener() {
			@Override
			public void itemChanged() {
				validate();
			}
		});
		
		setTitle(MessageFormat.format(Messages.PatrolLegLeaderChangeDialog_DialogTitle2, existingLeg.getId()));
		super.getShell().setText(Messages.PatrolLegLeaderChangeDialog_DialogTitle);
		setMessage(Messages.PatrolLegLeaderChangeDialog_DialogMessage);
		return parent;
	}
	
	private void validate(){
		String error = null;
		Date newStart = getNewStartDate();
	
		error = leaderPilotcomp.getErrorMessage();
		if (error == null && leaderPilotcomp.getSelectedLeader().equals(existingLeg.getLeader().getMember())){
			error = Messages.PatrolLegLeaderChangeDialog_NewLeaderRequired;
		}
		if (newStart.before(existingLeg.getStartDate())){
			error = MessageFormat.format(
					Messages.PatrolLegLeaderChangeDialog_Error_StartDateAfterStart1,
					new Object[]{ dateTimeFormatter.format(existingLeg.getStartDate())}) ;
			
		}else if (newStart.after(existingLeg.getEndDate())){
			error = MessageFormat.format(
					Messages.PatrolLegLeaderChangeDialog_Error_StartDateBeforeEnd1,
					new Object[]{ dateTimeFormatter.format(existingLeg.getEndDate()) });
		}

		setErrorMessage(error);
		getButton(IDialogConstants.OK_ID).setEnabled(error == null);
	}
	
	
	private Date getNewStartDate(){
		long time = SmartUtils.getDate(startDate).getTime();
		if (opCustom.getSelection()){
			time += startTime.getHours() * 60 * 60 * 1000 + startTime.getMinutes() * 60 * 1000 + startTime.getSeconds();
		}
		return new Date(time);
	}
	
	
	/** 
	 * Performs basic validation and updates the collection of legs provided to update.
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		//date validation
		validate();
		if (getErrorMessage() != null){
			MessageDialog.openError(opStart.getShell(), Messages.PatrolLegLeaderChangeDialog_DialogTitle, getErrorMessage());
			return;
		}
		
		Date newStart = getNewStartDate();

		//update dates, leader, & add leg
		newLeg.setEndDate(existingLeg.getEndDate());
		newLeg.setStartDate(newStart);
		leaderPilotcomp.updatePatrol(newLeg);
		legsToUpdate.add(newLeg);
		
		//update the existing leg
		existingLeg.setEndDate(newStart);
		if (existingLeg.getEndDate().getTime() - existingLeg.getStartDate().getTime() < 2){
			legsToUpdate.remove(existingLeg);
			existingLeg.setPatrol(null);
			existingLeg = null;
		}
		super.okPressed();
		
	}


	@Override
	public void widgetSelected(SelectionEvent e) {
		startTime.setEnabled(opCustom.getSelection());
		validate();
	}


	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}
	
}