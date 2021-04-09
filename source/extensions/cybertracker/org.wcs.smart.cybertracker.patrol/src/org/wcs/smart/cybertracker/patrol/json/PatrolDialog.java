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
package org.wcs.smart.cybertracker.patrol.json;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.cybertracker.patrol.model.CtPatrolLink;
import org.wcs.smart.cybertracker.patrol.model.CtPatrolWpLink;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.PatrolIdGenerator;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.ui.PatrolFilteredComboViewer;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.SmartStyledTitleDialog;

/**
 * Dialog for linking cybertracker patrols to SMART patrols.
 * 
 * @author Emily
 *
 */
public class PatrolDialog extends SmartStyledTitleDialog{

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
			//process new patrols
			for (Entry<UUID, UiData> e : uiItems.entrySet()){
				if (!e.getValue().btnExisting.getSelection()){
					Patrol p = createNewPatrol(e.getKey(), patrols.get(e.getKey()));
					newPatrols.add(p);
				}
			}
			//process merged patrols
			for (Entry<UUID, UiData> e : uiItems.entrySet()){
				if (e.getValue().btnExisting.getSelection()){
					Patrol addTo = (Patrol)session.get(Patrol.class, e.getValue().cmbPatrol.getSelection().getUuid());
					mergePatrol(e.getKey(), patrols.get(e.getKey()), addTo);
					mergedPatrols.add(addTo);
				}
			}
		}catch (Exception ex){
			CyberTrackerPlugIn.log(ex.getMessage(), ex);
			MessageDialog.openWarning(getShell(), Messages.PatrolDialog_ErrorTitle, Messages.PatrolDialog_SaveErrors + ":" + ex.getMessage()); //$NON-NLS-1$
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
		
		//add all legs
		for (PatrolLeg toAdd : newPatrol.getLegs()) {
			addToPatrol.getLegs().add(toAdd);
			toAdd.setPatrol(addToPatrol);
			
			if (addToPatrol.getStartDate().isAfter(toAdd.getStartDate())) {
				addToPatrol.setStartDate(toAdd.getStartDate());
			}
			if (addToPatrol.getEndDate().isBefore(toAdd.getEndDate())) {
				addToPatrol.setEndDate(toAdd.getEndDate());
			}
		}
		PatrolHibernateManager.savePatrol(addToPatrol, session, true);
		
		//create links
		//create links for all new legs
		for (PatrolLeg pl : newPatrol.getLegs()) {
			if (pl == newPatrolLink.getPatrolLeg()) continue;
			CtPatrolLink link = new CtPatrolLink();
			link.setCtUuid(UUID.randomUUID());// TOOD
			link.setPatrolLeg(pl);
			link.setDeviceId(newPatrolLink.getDeviceId());
			link.setLastObservationCnt(-1);
			link.setGroupStartTime(null);
			link.setWaypointLinks(new ArrayList<>());
			session.save(link);
		}
				
		CtPatrolLink link = new CtPatrolLink();
		link.setCtUuid(ctUuid);
		link.setPatrolLeg(newPatrolLink.getPatrolLeg());
		link.setDeviceId(newPatrolLink.getDeviceId());
		link.setLastObservationCnt(newPatrolLink.getLastObservationCnt());
		link.setGroupStartTime(newPatrolLink.getGroupStartTime());
		link.setWaypointLinks(new ArrayList<>());
		for (CtPatrolWpLink l : newPatrolLink.getWaypointLinks()) {
			l.setLink(link);
			link.getWaypointLinks().add(l);
		}
		newPatrolLink.getWaypointLinks().clear();
		session.save(link);
	}
	
	private Patrol createNewPatrol(UUID ctUuid, CtPatrolLink patrol) throws Exception{
		Patrol newPatrol = patrol.getPatrolLeg().getPatrol();
		newPatrol.setConservationArea(SmartDB.getCurrentConservationArea());
		
		//this shouldn't be necessary with the CT Mobile, but 
		//may be required for old CT support
		LocalDate start = newPatrol.getFirstLeg().getStartDate();
		LocalDate end = newPatrol.getFirstLeg().getEndDate();
		for (PatrolLeg pl : newPatrol.getLegs()) {
			if (pl.getStartDate().isBefore(start)) start = pl.getStartDate();
			if (pl.getEndDate().isAfter(end)) end = pl.getEndDate();
		}
		newPatrol.setStartDate(start);
		newPatrol.setEndDate(end);
		
		newPatrol.setId(PatrolIdGenerator.INSTANCE.generatePatrolId(newPatrol, session));
		if (newPatrol.getPatrolType() == null){
			if (newPatrol.getFirstLeg().getType() != null){
				newPatrol.setPatrolType(newPatrol.getFirstLeg().getType().getPatrolType());
			}else{
				throw new Exception(Messages.PatrolDialog_NoTransportType);
			}
		}
		PatrolHibernateManager.savePatrol(newPatrol, session, true);
		
		//create links for all new legs
		for (PatrolLeg pl : newPatrol.getLegs()) {
			if (pl == patrol.getPatrolLeg()) continue;
			CtPatrolLink link = new CtPatrolLink();
			link.setCtUuid(UUID.randomUUID());//TOOD
			link.setPatrolLeg(pl);
			link.setDeviceId(patrol.getDeviceId());
			link.setLastObservationCnt(-1);
			link.setGroupStartTime(null);
			link.setWaypointLinks(new ArrayList<>());
			session.save(link);
		}
		
		CtPatrolLink link = new CtPatrolLink();
		link.setCtUuid(ctUuid);
		link.setPatrolLeg(patrol.getPatrolLeg());
		link.setDeviceId(patrol.getDeviceId());
		link.setLastObservationCnt(patrol.getLastObservationCnt());
		link.setGroupStartTime(patrol.getGroupStartTime());
		link.setWaypointLinks(new ArrayList<>());
		for (CtPatrolWpLink l : patrol.getWaypointLinks()) {
			l.setLink(link);
			link.getWaypointLinks().add(l);
		}
		patrol.getWaypointLinks().clear();
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
		
		List<PatrolLeg> newPatrols = new ArrayList<>();
		for(CtPatrolLink l : patrols.values()){
			newPatrols.add(l.getPatrolLeg());
		}
		//sort
		newPatrols.sort(new Comparator<PatrolLeg>() {
			@Override
			public int compare(PatrolLeg l1, PatrolLeg l2) {
				if (!l1.getStartDate().isEqual(l2.getStartDate())) return l1.getStartDate().compareTo(l2.getStartDate());
				for (PatrolLegDay day1 : l1.getPatrolLegDays()){
					if (day1.getDate().isEqual(l1.getStartDate())){
						for (PatrolLegDay day2 : l2.getPatrolLegDays()){
							if (day2.getDate().isEqual(l2.getStartDate())){
								return day1.getStartTime().compareTo(day2.getStartTime());
							}
						}
					}
				}
				return 0;
			}
		});
		//assign temporary patrol ids
		int cnt = 1;
		for (PatrolLeg p : newPatrols){
			p.getPatrol().setId(MessageFormat.format(Messages.PatrolDialog_ImportedMessage, cnt++));
		}
		
		for (PatrolLeg pl : newPatrols){
			Entry<UUID, CtPatrolLink> e = null;
			for (Entry<UUID, CtPatrolLink> temp : patrols.entrySet()){
				if (temp.getValue().getPatrolLeg() == pl){
					e = temp;
					break;
				}
			}
			Label l = new Label(main, SWT.WRAP);
			StringBuilder lbl = new StringBuilder();
			Patrol p = e.getValue().getPatrolLeg().getPatrol();
			LocalDateTime startDt = p.getFirstLeg().getStartDate().atTime(p.getFirstLeg().getPatrolLegDays().get(0).getStartTime());
			for (PatrolLeg ppl : p.getLegs()) {
				for (PatrolLegDay pld : ppl.getPatrolLegDays()) {
					LocalDateTime d = pld.getDate().atTime(pld.getStartTime());
					if (d.isBefore(startDt)) startDt = d;
				}
			}
			
			lbl.append(p.getId());
			lbl.append("\n"); //$NON-NLS-1$
			lbl.append(Messages.PatrolDialog_StartDateLabel);
			lbl.append(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(startDt)); 
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
			op.setLayout(new GridLayout(2, false));
			op.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)op.getLayoutData()).horizontalIndent = 2;
			
			Button btnNew = new Button(op, SWT.RADIO);
			btnNew.setText(Messages.PatrolDialog_NewPatrolLabel);
			btnNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
			btnNew.setSelection(true);
			
			Button btnExisting = new Button(op, SWT.RADIO);
			btnExisting.setText(Messages.PatrolDialog_AddExistingLabel);
			btnExisting.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
			btnExisting.setSelection(false);
			
			List<Patrol> newPs = new ArrayList<Patrol>();
			newPatrols.stream().forEach(plc -> newPs.add(plc.getPatrol()));
			newPs.remove(p);
			PatrolFilteredComboViewer viewer = new PatrolFilteredComboViewer(op, newPs);
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
					
					if (p.getUuid() == null){
						//we are selecting an new patrol to add to; we want to make sure
						//this new patrol starts before the current patrol
						LocalDateTime addtoDate = null;
						for (PatrolLegDay pld : p.getFirstLeg().getPatrolLegDays()){
							if ( pld.getDate().isEqual(p.getFirstLeg().getStartDate())){
								addtoDate = pld.getDate().atTime(pld.getStartTime());
								break;
							}
						}
						LocalDateTime addfromDate = null;
						for (PatrolLegDay pld : ctP.getFirstLeg().getPatrolLegDays()){
							if ( pld.getDate().isEqual(ctP.getFirstLeg().getStartDate())){
								addfromDate = pld.getDate().atTime(pld.getStartTime());
								break;
							}
						}
						if (addtoDate.isAfter(addfromDate)){
							entry.getValue().errItem.setDescriptionText(Messages.PatrolDialog_InvalidMergeDates);
							entry.getValue().errItem.show();
							error = true;
						}
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
