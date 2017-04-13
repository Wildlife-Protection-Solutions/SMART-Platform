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
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.ui.EmployeeSelectorDialog;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for changing the transport of a given leg.  Takes a leg
 * and splits it into two, setting the transport type on the second part 
 * of the leg to a newly transport Type.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolTransportChangeDialog extends TitleAreaDialog implements SelectionListener{

	private DateFormat dateTimeFormatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
	
	private PatrolLeg existingLeg;
	private PatrolLeg newLeg;
	
	private DateTime startDate;
	private DateTime startTime;
	private Button opStart;
	private Button opCustom;
	private Collection<PatrolLeg> legsToUpdate;
	private PatrolTransportComposite compTransportType;
	private Session session;
	
	/**
	 * Creates a new dialog 
	 * @param parentShell the parent shell
	 * @param patrolLeg the patrol leg to split up
	 * @param legsToUpdate the set of legs to add the new leg to 
	 */
	public PatrolTransportChangeDialog(Shell parentShell, PatrolLeg patrolLeg, 
			Collection<PatrolLeg> legsToUpdate, Session session) {
		super(parentShell);
		this.existingLeg = patrolLeg;
		this.legsToUpdate = legsToUpdate;
		this.session = session;
		
		// create a new leg for updating 
		newLeg = new PatrolLeg();
		// clone the members
		List<PatrolLegMember> members = new ArrayList<PatrolLegMember>();
		for (PatrolLegMember existing : existingLeg.getMembers()){
			PatrolLegMember newmem = existing.clone();
			newmem.setPatrolLeg(newLeg);
			members.add(newmem);
		}
		newLeg.setMembers(members);
		newLeg.setPatrol(existingLeg.getPatrol());
		newLeg.setType(existingLeg.getType());
		
		String legId = existingLeg.getId() + Messages.PatrolTransportChangeDialog_TransportChangeLegIdentifier;
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
		timecomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Label lbl = new Label(timecomp, SWT.NONE);
		lbl.setText(Messages.PatrolTransportChangeDialog_DateLabel);
		startDate = new DateTime(timecomp, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER | SWT.LONG);
		startDate.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		startDate.addSelectionListener(this);
		SmartUtils.initDateDateTimeWidget(startDate, existingLeg.getStartDate());
		
		//time of change
		lbl = new Label(timecomp, SWT.NONE);
		lbl.setText(Messages.PatrolTransportChangeDialog_TimeLabel);
		
		Composite opComp = new Composite(timecomp, SWT.NONE);
		opComp.setLayout(new GridLayout(3, false));

		opStart = new Button(opComp, SWT.RADIO);
		opStart.setText(Messages.PatrolTransportChangeDialog_StartOfDay_Op);
		opStart.setSelection(true);
		opStart.addSelectionListener(this);
		
		opCustom = new Button(opComp, SWT.RADIO);
		opCustom.setText(Messages.PatrolTransportChangeDialog_CustomLabel);
		opCustom.addSelectionListener(this);
		
		startTime = new DateTime(opComp, SWT.TIME | SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER);
		startTime.setEnabled(false);
		startTime.setTime(0, 0, 0);
		startTime.addSelectionListener(this);
		
		
		/* new leader/pilot */
		compTransportType = new PatrolTransportComposite();
		
		Composite c = compTransportType.createComponent(parent, SWT.NONE);
		c.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		boolean close = false;
		if (!session.isOpen()){
			session = HibernateManager.openSession();
			close = true;
		}
		try{
			compTransportType.setValues(newLeg, session);
		}finally{
			if (close){
				session.close();
			}
		}
		
		compTransportType.addChangeListener(new IPatrolItemChangeListener() {
			@Override
			public void itemChanged() {
				validate();	
			}
		});
		
		setTitle(MessageFormat.format(Messages.PatrolTransportChangeDialog_DialogTitle2, existingLeg.getId()));
		super.getShell().setText(Messages.PatrolTransportChangeDialog_DialogTitle);
		setMessage(Messages.PatrolTransportChangeDialog_DialogMessage);
		return parent;
	}
	
	public Date getNewDate(){
		long time = SmartUtils.getDate(startDate).getTime();
		if (opCustom.getSelection()){
			time += startTime.getHours() * 60 * 60 * 1000 + startTime.getMinutes() * 60 * 1000 + startTime.getSeconds();
		}
		Date newStart = new Date( time );
		return newStart;
	}
	
	private void validate(){
		String error = compTransportType.getErrorMessage();
		
		if (error == null && compTransportType.getSelectedTransportType().equals(existingLeg.getType())){
			error = Messages.PatrolTransportChangeDialog_NewTransportTypeError;
		}
		
		Date newStart = getNewDate();
		
		if (error == null && newStart.before(existingLeg.getStartDate())){
			error = MessageFormat.format(
							Messages.PatrolTransportChangeDialog_Error_StartDateAfter,
							new Object[]{dateTimeFormatter.format(existingLeg.getStartDate()) });
		}
		if (error == null && newStart.after(existingLeg.getEndDate())){
			error = MessageFormat.format(
							Messages.PatrolTransportChangeDialog_Error_StartDateBefore,
							new Object[]{null,dateTimeFormatter.format(existingLeg.getEndDate())});
		}
		
		setErrorMessage(error);
		getButton(IDialogConstants.OK_ID).setEnabled(error == null);
	}
	
	/** 
	 * Performs basic validation and updates the collection of legs provided to update.
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		validate();
		if (getErrorMessage() != null){
			MessageDialog.openError(getShell(), Messages.PatrolTransportChangeDialog_DialogTitle, getErrorMessage());
			return;
		}
		
		Date newStart = getNewDate();
		//update dates, leader, & add leg
		newLeg.setEndDate(existingLeg.getEndDate());
		newLeg.setStartDate(newStart);
		compTransportType.updatePatrol(newLeg);
		
		if (!newLeg.getType().getPatrolType().requiresPilot()) {
			//this is needed to clear pilot if changed from transport type with pilot to transport type without pilot
			for (PatrolLegMember member : newLeg.getMembers()) {
				member.setIsPilot(false);
			}
		}
		if (newLeg.getType().getPatrolType().requiresPilot() && newLeg.getPilot() == null) {
			//transport type was changed from a type without pilot to a type that requires pilot, need to for pilot selection
			EmployeeSelectorDialog dialog = new EmployeeSelectorDialog(
					Display.getDefault().getActiveShell(),
					Messages.PatrolTransportChangeDialog_PilotSelectDialog_Title,
					Messages.PatrolTransportChangeDialog_PilotSelectDialog_Message,
					EmployeeSelectorDialog.Type.PILOT, newLeg);
			if (dialog.open() != Window.OK) {
				return;
			}
		}
		
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