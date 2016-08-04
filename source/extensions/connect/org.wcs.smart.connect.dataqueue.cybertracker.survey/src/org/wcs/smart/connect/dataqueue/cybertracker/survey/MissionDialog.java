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
package org.wcs.smart.connect.dataqueue.cybertracker.survey;

import java.sql.Time;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import org.wcs.smart.connect.dataqueue.cybertracker.survey.model.CtMissionLink;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.ui.MissionFilteredComboViewer;
import org.wcs.smart.er.ui.SurveyFilteredComboViewer;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for linking cybertracker missions to SMART missions/surveys.
 * 
 * @author Emily
 *
 */
public class MissionDialog extends TitleAreaDialog {

	private HashMap<UUID, CtMissionLink> missions;
	
	private HashMap<UUID, UiData> uiItems;
	
	private Session session;
	
	private Set<Mission> newMission;
	private Set<Mission> mergedMissions;
	
	public MissionDialog(Shell parentShell, HashMap<UUID, CtMissionLink> missions, Session session) {
		super(parentShell);
		this.missions = missions;
		this.session = session;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}
	
	public Set<Mission> getNewMissions(){
		return this.newMission;
	}
	public Set<Mission> getMergedMissions(){
		return this.mergedMissions;
	}
	
	@Override
	public void okPressed(){
		newMission = new HashSet<>();
		mergedMissions = new HashSet<>();
		if (validate()){
			MessageDialog.openError(getShell(), "Error", "Errors exist on page.  Resolve the errors before continuing.");
			return ;
		}
		//validate();
		try{
			for (Entry<UUID, UiData> e : uiItems.entrySet()){
				if (e.getValue().btnExisting.getSelection()){
					Mission addTo = (Mission)session.get(Mission.class, e.getValue().cmbMission.getSelection().getUuid());
					mergeMission(e.getKey(), missions.get(e.getKey()), addTo);
					mergedMissions.add(addTo);
				}else{
					Survey addTo = (Survey)session.get(Survey.class, e.getValue().cmbSurvey.getSelection().getUuid());
					Mission p = createNewMission(e.getKey(), missions.get(e.getKey()), addTo);
					newMission.add(p);
				}
			}
		}catch (Exception ex){
			ex.printStackTrace();
			MessageDialog.openWarning(getShell(), "Error", "Error saving results");
			super.cancelPressed();
			return;
		}
		super.okPressed();
	}
	
	private void mergeMission(UUID ctUuid, CtMissionLink newMissionLink, Mission addToMission) throws Exception{
		Mission newMission = newMissionLink.getMission();
		
		for (MissionDay newMissionDay : newMission.getMissionDays()){
			MissionDay addToDay = null;	
			for (MissionDay md : addToMission.getMissionDays()){
				if (SharedUtils.isSameDate(md.getDate(), newMissionDay.getDate())){
					addToDay = md;
					break;
				}
			}
			
			if (addToDay == null){
				//need to add a new day
				if (newMissionDay.getDate().before(addToMission.getStartDate())){
					addToMission.getMissionDays().add(newMissionDay);
					newMissionDay.setMission(addToMission);
					
					Calendar cal = Calendar.getInstance();
					cal.setTime(newMissionDay.getDate());
					cal.add(Calendar.DAY_OF_MONTH, 1);
					while(cal.getTime().before(addToMission.getStartDate())){
						//create a new empty mission day
						MissionDay md = new MissionDay();
						md.setDate(cal.getTime());
						md.setEndTime(new Time(SmartUtils.getMidnight().getTime()));
						md.setStartTime(new Time(0));
						md.setRestMinutes(0);
						md.setMission(addToMission);
						addToMission.getMissionDays().add(md);
						cal.add(Calendar.DAY_OF_MONTH, 1);
					}
				}else{ //newMissionDay after addToMission; add at end
					addToMission.getMissionDays().add(newMissionDay);
					newMissionDay.setMission(addToMission);
					
					Calendar cal = Calendar.getInstance();
					cal.setTime(addToMission.getEndDate());
					cal.add(Calendar.DAY_OF_MONTH, 1);
					while(cal.getTime().before(newMissionDay.getDate())){
						//create a new empty mission day
						MissionDay md = new MissionDay();
						md.setDate(cal.getTime());
						md.setEndTime(new Time(SmartUtils.getMidnight().getTime()));
						md.setStartTime(new Time(0));
						md.setRestMinutes(0);
						md.setMission(addToMission);
						addToMission.getMissionDays().add(md);
						cal.add(Calendar.DAY_OF_MONTH, 1);
					}
				}
				addToMission.getMissionDays().sort((MissionDay md1, MissionDay md2) -> md1.getDate().compareTo(md2.getDate()));
			}else{
				//merge observations; add all the observations to this date
				for (SurveyWaypoint sw : newMissionDay.getWaypoints()){
					addToDay.getWaypoints().add(sw);
					sw.setMissionDay(addToDay);
				}
				
				//add tracks; we don't merge tracks here
				//TODO: we probalby want to merge them here otherwise we may end up with a whole bunch of really 
				//short tracks
				for (MissionTrack mr : newMissionDay.getTracks()){
					addToDay.getTracks().add(mr);
					mr.setMissionDay(addToDay);
				}
			}
			
		}
		
		SurveyHibernateManager.saveMission(addToMission, session, true);
		
		CtMissionLink link = new CtMissionLink();
		link.setCtUuid(ctUuid);
		link.setMission(addToMission);
		link.setDeviceId(newMissionLink.getDeviceId());
		link.setLastObservationCnt(newMissionLink.getLastObservationCnt());
		link.setGroupStartTime(newMissionLink.getGroupStartTime());
		session.save(link);
	}
	
	private Mission createNewMission(UUID ctUuid, CtMissionLink mission, Survey survey) throws Exception{
		Mission newMission = mission.getMission();
		newMission.setSurvey(survey);
		survey.getMissions().add(newMission);
		
		Date startDate = null;
		Date endDate = null;
		for (MissionDay md : newMission.getMissionDays()){
			if (startDate == null || md.getDate().before(startDate)){
				startDate = md.getDate();
			}
			if (endDate == null || md.getDate().after(endDate)){
				endDate = md.getDate();
			}
		}
		newMission.setStartDate(startDate);
		newMission.setEndDate(endDate);
		newMission.setId(SurveyHibernateManager.generateMissionId(session));
		SurveyHibernateManager.saveMission(newMission, session, true);
		
		CtMissionLink link = new CtMissionLink();
		link.setCtUuid(ctUuid);
		link.setMission(newMission);
		link.setDeviceId(mission.getDeviceId());
		link.setLastObservationCnt(mission.getLastObservationCnt());
		link.setGroupStartTime(mission.getGroupStartTime());
		session.save(link);
		
		return newMission;
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
		
		uiItems = new HashMap<UUID, MissionDialog.UiData>();
		Label header1 = new Label(main, SWT.NONE);
		header1.setText("Mission Summary:");
		Label header2 = new Label(main, SWT.NONE);
		header2.setText("Action:");
		Label spacer = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		for (Entry<UUID, CtMissionLink> e : missions.entrySet()){
			Label l = new Label(main, SWT.WRAP);
			StringBuilder lbl = new StringBuilder();
			Mission p = e.getValue().getMission();
			lbl.append("Start Date:");
			lbl.append(p.getStartDate() == null ? "" : DateFormat.getDateInstance().format(p.getStartDate())); //$NON-NLS-1$
			lbl.append("\n"); //$NON-NLS-1$
			lbl.append("Survey Design:");
			lbl.append(e.getValue().getNewSurveyDesign().getName()); 
			lbl.append("\n"); //$NON-NLS-1$
			lbl.append("Leader:");
			lbl.append(p.getLeader() == null ? "" : SmartLabelProvider.getShortLabel(p.getLeader().getMember())); //$NON-NLS-1$
			
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
			btnNew.setText("New Mission");
			btnNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
			btnNew.setSelection(true);
			
			Label sl = new Label(op, SWT.NONE);
			sl.setText("Survey:");
			sl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
			
			SurveyFilteredComboViewer surveyViewer = new SurveyFilteredComboViewer(op, e.getValue().getNewSurveyDesign(), true, session);
			surveyViewer.setEnabled(true);
			surveyViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					validate();
					
				}
			});
			surveyViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			
			
			Button btnExisting = new Button(op, SWT.RADIO);
			btnExisting.setText("Add to Existing Mission");
			btnExisting.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
			btnExisting.setSelection(false);
			
			MissionFilteredComboViewer viewer = new MissionFilteredComboViewer(op);
			viewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			viewer.setEnabled(false);
			viewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					validate();
					
				}
			});
			
			SelectionListener listener = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					viewer.setEnabled(btnExisting.getSelection());
					surveyViewer.setEnabled(btnNew.getSelection());
					validate();
				}
			};
			
			btnNew.addSelectionListener(listener);
			btnExisting.addSelectionListener(listener);
			
			
			
			uiItems.put(e.getKey(), new UiData(btnNew, btnExisting, viewer, surveyViewer, cd));
			spacer = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
			spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		}
		scroll.setContent(main);
		scroll.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		setTitle("Import CyberTracker Data");
		setMessage("The following CyberTracker data needs to be identified as new mission new added to existing mission.");
		getShell().setText("CyberTracker Data Import");
		return composite;

	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	private boolean validate(){
		boolean error = false;
		for (Entry<UUID, UiData> entry : uiItems.entrySet()){
			UUID ctMission = entry.getKey();
			entry.getValue().errItem.hide();
			if (!entry.getValue().btnExisting.getSelection() && !entry.getValue().btnNew.getSelection()){
				entry.getValue().errItem.setDescriptionText("Must add to existing or create a new mission");
				entry.getValue().errItem.show();
				error = true;
			}
			
			if (entry.getValue().btnExisting.getSelection()){
				if (entry.getValue().cmbMission.getSelection() == null){
					entry.getValue().errItem.setDescriptionText("Must select a mission");
					entry.getValue().errItem.show();
					error = true;
				}else{
					
					Mission p = entry.getValue().cmbMission.getSelection();
					Mission ctP = missions.get(ctMission).getMission();
					
					double diff = (ctP.getStartDate().getTime() - p.getEndDate().getTime());
					if (diff > 48*60*60*100.0){
						//TODO: should be a warning not an error
						entry.getValue().errItem.setDescriptionText(MessageFormat.format("The start date of the new mission ({0}) is more than 48 hrs from the end date of the selected mission ({1}).", DateFormat.getDateInstance().format(ctP.getStartDate()), DateFormat.getDateInstance().format(p.getEndDate())));
						entry.getValue().errItem.show();
//						error = true;
					}
				}
			}
			if (entry.getValue().btnNew.getSelection()){
				if (entry.getValue().cmbSurvey.getSelection() == null){
					entry.getValue().errItem.setDescriptionText("Must select a survey to add the new mission to");
					entry.getValue().errItem.show();
					error = true;
				}else{
					Object x = entry.getValue().cmbSurvey.getSelection();
					if (x instanceof Survey){
						Survey selectedSurvey = (Survey) x;
						if (selectedSurvey.getStartDate() != null && selectedSurvey.getEndDate() != null){
							Mission ctP = missions.get(ctMission).getMission();
							if (ctP.getStartDate().before(selectedSurvey.getStartDate()) ||
									ctP.getEndDate().after(selectedSurvey.getEndDate())){
								entry.getValue().errItem.setDescriptionText("Mission dates do not fall within survey dates");
								entry.getValue().errItem.show();
								error = true;			
							}
						}
					}else{
						entry.getValue().errItem.setDescriptionText("Must select a survey");
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
		MissionFilteredComboViewer cmbMission;
		SurveyFilteredComboViewer cmbSurvey;
		ControlDecoration errItem;
		
		public UiData(Button btnNew, Button btnExisting, MissionFilteredComboViewer cmbMission, 
				SurveyFilteredComboViewer cmbSurvey, ControlDecoration errItem){
			this.btnNew = btnNew;
			this.btnExisting = btnExisting;
			this.cmbMission = cmbMission;
			this.cmbSurvey = cmbSurvey;
			this.errItem = errItem;
		}
		
	}
}
