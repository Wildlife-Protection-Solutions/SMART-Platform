/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.connect.dataqueue.cybertracker.patrol;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.connect.dataqueue.cybertracker.patrol.internal.Messages;
import org.wcs.smart.connect.dataqueue.cybertracker.patrol.model.CtPatrolLink;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.ui.PatrolFilteredComboViewer;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Dialog for linking cybertracker patrols to SMART patrols.
 * 
 * @author Emily
 *
 */
public class PatrolDialog extends TitleAreaDialog {

	private HashMap<UUID, CtPatrolLink> patrols;
	
	private HashMap<UUID, UiData> uiItems;
	
	private Session session;
	
	private Set<Patrol> newPatrols;
	private Set<Patrol> mergedPatrols;
	
	public PatrolDialog(Shell parentShell, HashMap<UUID, CtPatrolLink> patrols, Session session) {
		super(parentShell);
		this.patrols = patrols;
		this.session = session;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}
	
	public Set<Patrol> getNewPatrols(){
		return this.newPatrols;
	}
	public Set<Patrol> getMergedPatrols(){
		return this.mergedPatrols;
	}
	
	@Override
	public void okPressed(){
		newPatrols = new HashSet<>();
		mergedPatrols = new HashSet<>();
		if (validate()){
			MessageDialog.openError(getShell(), Messages.PatrolDialog_ErrorTitle, Messages.PatrolDialog_PageErrors);
			return ;
		}
		//validate();
		try{
			for (Entry<UUID, UiData> e : uiItems.entrySet()){
				if (e.getValue().btnExisting.getSelection()){
					Patrol addTo = (Patrol)session.get(Patrol.class, e.getValue().cmbPatrol.getSelection().getUuid());
					mergePatrol(e.getKey(), patrols.get(e.getKey()), addTo);
					mergedPatrols.add(addTo);
				}else{
					Patrol p = createNewPatrol(e.getKey(), patrols.get(e.getKey()));
					newPatrols.add(p);
				}
			}
		}catch (Exception ex){
			ex.printStackTrace();
			MessageDialog.openWarning(getShell(), Messages.PatrolDialog_ErrorTitle, Messages.PatrolDialog_SaveErrors);
			super.cancelPressed();
			return;
		}
		super.okPressed();
	}
	
	private void mergePatrol(UUID ctUuid, CtPatrolLink newPatrolLink, Patrol addToPatrol) throws Exception{
		Patrol newPatrol = newPatrolLink.getPatrolLeg().getPatrol();
		if (!newPatrol.getPatrolType().equals(addToPatrol.getPatrolType())){
			throw new Exception(MessageFormat.format(Messages.PatrolDialog_DifferentType, newPatrol.getPatrolType().getGuiName(Locale.getDefault()), addToPatrol.getPatrolType().getGuiName(Locale.getDefault())));
		}
		
		PatrolLeg toAdd= newPatrol.getFirstLeg();
		addToPatrol.getLegs().add(toAdd);
		toAdd.setPatrol(addToPatrol);
		PatrolHibernateManager.savePatrol(addToPatrol, session, true);
		
		CtPatrolLink link = new CtPatrolLink();
		link.setCtUuid(ctUuid);
		link.setPatrolLeg(newPatrol.getFirstLeg());
		link.setDeviceId(newPatrolLink.getDeviceId());
		link.setLastObservationCnt(newPatrolLink.getLastObservationCnt());
		link.setGroupStartTime(newPatrolLink.getGroupStartTime());
		session.save(link);
	}
	
	private Patrol createNewPatrol(UUID ctUuid, CtPatrolLink patrol) throws Exception{
		Patrol newPatrol = patrol.getPatrolLeg().getPatrol();
		newPatrol.setConservationArea(SmartDB.getCurrentConservationArea());
		newPatrol.setStartDate(newPatrol.getFirstLeg().getStartDate());
		newPatrol.setEndDate(newPatrol.getFirstLeg().getEndDate());
		
		newPatrol.setId(PatrolHibernateManager.generatePatrolId(newPatrol, session));
		if (newPatrol.getPatrolType() == null){
			if (newPatrol.getFirstLeg().getType() != null){
				newPatrol.setPatrolType(newPatrol.getFirstLeg().getType().getPatrolType());
			}else{
				throw new Exception(Messages.PatrolDialog_NoTransportType);
			}
		}
		PatrolHibernateManager.savePatrol(newPatrol, session, true);
		CtPatrolLink link = new CtPatrolLink();
		link.setCtUuid(ctUuid);
		link.setPatrolLeg(newPatrol.getFirstLeg());
		link.setDeviceId(patrol.getDeviceId());
		link.setLastObservationCnt(patrol.getLastObservationCnt());
		link.setGroupStartTime(patrol.getGroupStartTime());
		session.save(link);
		
		return newPatrol;
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		// add controls to composite as necessary
		
		ScrolledComposite scroll = new ScrolledComposite(composite, SWT.V_SCROLL | SWT.H_SCROLL);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Composite main = new Composite(scroll, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		
		uiItems = new HashMap<UUID, PatrolDialog.UiData>();
		Label header1 = new Label(main, SWT.NONE);
		header1.setText(Messages.PatrolDialog_SummaryLabel);
		Label header2 = new Label(main, SWT.NONE);
		header2.setText(Messages.PatrolDialog_ActionLabel);
		Label spacer = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		for (Entry<UUID, CtPatrolLink> e : patrols.entrySet()){
			Label l = new Label(main, SWT.WRAP);
			StringBuilder lbl = new StringBuilder();
			Patrol p = e.getValue().getPatrolLeg().getPatrol();
			lbl.append(Messages.PatrolDialog_StartDateLabel);
			lbl.append(p.getStartDate() == null ? "" : DateFormat.getDateInstance().format(p.getStartDate())); //$NON-NLS-1$
			lbl.append("\n"); //$NON-NLS-1$
			lbl.append(Messages.PatrolDialog_TypeLabel);
			lbl.append(p.getFirstLeg().getType() == null ? "" : p.getFirstLeg().getType().getName()); //$NON-NLS-1$
			lbl.append(" (" + (p.getPatrolType() == null ? "" : p.getPatrolType().getGuiName(Locale.getDefault())) + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			lbl.append("\n"); //$NON-NLS-1$
			lbl.append(Messages.PatrolDialog_LeaderLabel);
			lbl.append(p.getFirstLeg().getLeader() == null ? "" : SmartLabelProvider.getShortLabel(p.getFirstLeg().getLeader().getMember())); //$NON-NLS-1$
			
			l.setText(lbl.toString());
			
			ControlDecoration cd = new ControlDecoration(l, SWT.RIGHT | SWT.TOP);
			cd.setImage(FieldDecorationRegistry.getDefault()
					.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
			cd.hide();
			
			
			Composite op= new Composite(main, SWT.NONE);
			op.setLayout(new GridLayout(3, false));
			op.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)op.getLayoutData()).horizontalIndent = 2;
			
			Button btnNew = new Button(op, SWT.RADIO);
			btnNew.setText(Messages.PatrolDialog_NewPatrolLabel);
			btnNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
			btnNew.setSelection(true);
			
			Button btnExisting = new Button(op, SWT.RADIO);
			btnExisting.setText(Messages.PatrolDialog_AddExistingLabel);
			btnExisting.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
			btnExisting.setSelection(false);
			
			PatrolFilteredComboViewer viewer = new PatrolFilteredComboViewer(op);
			viewer.setEnabled(false);
			viewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					validate();
					
				}
			});
			viewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			SelectionListener listener = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					viewer.setEnabled(btnExisting.getSelection());
					validate();
				}
			};
			
			btnNew.addSelectionListener(listener);
			btnExisting.addSelectionListener(listener);
			
			uiItems.put(e.getKey(), new UiData(btnNew, btnExisting, viewer, cd));
			spacer = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
			spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		}
		scroll.setContent(main);
		scroll.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		setTitle(Messages.PatrolDialog_DialogTitle);
		setMessage(Messages.PatrolDialog_DialogMsg);
		getShell().setText(Messages.PatrolDialog_ShellTitle);
		return composite;

	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	private boolean validate(){
		boolean error = false;
		for (Entry<UUID, UiData> entry : uiItems.entrySet()){
			UUID ctPatrol = entry.getKey();
			entry.getValue().errItem.hide();
			if (!entry.getValue().btnExisting.getSelection() && !entry.getValue().btnNew.getSelection()){
				entry.getValue().errItem.setDescriptionText(Messages.PatrolDialog_PatrolRequiredError);
				entry.getValue().errItem.show();
				error = true;
			}
			
			if (entry.getValue().btnExisting.getSelection()){
				if (entry.getValue().cmbPatrol.getSelection() == null){
					entry.getValue().errItem.setDescriptionText(Messages.PatrolDialog_PatrolRequiredError2);
					entry.getValue().errItem.show();
					error = true;
				}else{
					
					Patrol p = entry.getValue().cmbPatrol.getSelection();
					Patrol ctP = patrols.get(ctPatrol).getPatrolLeg().getPatrol();
					if (!p.getPatrolType().equals(ctP.getPatrolType())){
						entry.getValue().errItem.setDescriptionText(MessageFormat.format(Messages.PatrolDialog_DifferentTypeError, p.getPatrolType().getGuiName(Locale.getDefault()), ctP.getPatrolType().getGuiName(Locale.getDefault())));
						entry.getValue().errItem.show();
						error = true;
					}
				}
			}
		}
		getButton(IDialogConstants.OK_ID).setEnabled(!error);
		return error;
	}
	
	private class UiData{
		Button btnNew;
		Button btnExisting;
		PatrolFilteredComboViewer cmbPatrol;
		ControlDecoration errItem;
		
		public UiData(Button btnNew, Button btnExisting, PatrolFilteredComboViewer cmbPatrol, ControlDecoration errItem){
			this.btnNew = btnNew;
			this.btnExisting = btnExisting;
			this.cmbPatrol = cmbPatrol;
			this.errItem = errItem;
		}
		
	}
}
