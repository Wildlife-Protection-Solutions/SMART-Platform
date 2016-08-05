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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.connect.dataqueue.cybertracker.survey.internal.Messages;
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
	
	private List<Survey> addedSurveys = new ArrayList<>();
	
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
		

		validate();
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
			MessageDialog.openError(getShell(), Messages.MissionDialog_ErrorTitle, Messages.MissionDialog_PageErrorMsg);
			return ;
		}
		//validate();
		try{
			//process new missions first
			for (Entry<UUID, UiData> e : uiItems.entrySet()){
				if (!e.getValue().btnExisting.getSelection()){
					Survey addTo = e.getValue().cmbSurvey.getSelection();
					if (addTo.getUuid() != null){
						addTo = (Survey)session.get(Survey.class, addTo.getUuid());
					}
					Mission p = createNewMission(e.getKey(), missions.get(e.getKey()), addTo);
					newMission.add(p);
				}
			}
			
			//process new merged options second
			for (Entry<UUID, UiData> e : uiItems.entrySet()){
				if (e.getValue().btnExisting.getSelection()){
					Mission addTo = (Mission)session.get(Mission.class, e.getValue().cmbMission.getSelection().getUuid());
					mergeMission(e.getKey(), missions.get(e.getKey()), addTo);
					mergedMissions.add(addTo);
				}
					
			}
		}catch (Exception ex){
			ex.printStackTrace();
			MessageDialog.openWarning(getShell(), Messages.MissionDialog_ErrorTitle, Messages.MissionDialog_SaveError);
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
						md.setEndTime(new Time(SmartUtils.getMidnight().getTime() - 1));
						md.setStartTime(new Time(SmartUtils.getMidnight().getTime()));
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
						md.setEndTime(new Time(SmartUtils.getMidnight().getTime() - 1));
						md.setStartTime(new Time(SmartUtils.getMidnight().getTime()));
						md.setRestMinutes(0);
						md.setMission(addToMission);
						addToMission.getMissionDays().add(md);
						cal.add(Calendar.DAY_OF_MONTH, 1);
					}
				}
				addToMission.getMissionDays().sort((MissionDay md1, MissionDay md2) -> md1.getDate().compareTo(md2.getDate()));
			}else{
				//update time
				if (newMissionDay.getStartTime().before(addToDay.getStartTime())){
					addToDay.setStartTime(newMissionDay.getStartTime());
				}
				if (newMissionDay.getEndTime().after(addToDay.getEndTime())){
					addToDay.setEndTime(newMissionDay.getEndTime());
				}
				//merge observations; add all the observations to this date
				for (SurveyWaypoint sw : newMissionDay.getWaypoints()){
					addToDay.getWaypoints().add(sw);
					sw.setMissionDay(addToDay);
				}
				
				//add tracks; we don't do any merging; we assume each track is unique as
				//the user would have to start a new mission to get this case.
				for (MissionTrack mr : newMissionDay.getTracks()){
					addToDay.getTracks().add(mr);
					mr.setMissionDay(addToDay);
				}
			}
			
		}
		
		//update mission start end end days
		for (MissionDay md : addToMission.getMissionDays()){
			if (md.getDate().before(addToMission.getStartDate())){
				addToMission.setStartDate(md.getDate());
			}
			if (md.getDate().after(addToMission.getEndDate())){
				addToMission.setEndDate(md.getDate());
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
		
		session.saveOrUpdate(newMission.getSurvey());
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
		header1.setText(Messages.MissionDialog_SummaryLabel);
		Label header2 = new Label(main, SWT.NONE);
		header2.setText(Messages.MissionDialog_ActionLabel);
		Label spacer = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		ISelectionChangedListener listener = new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		};
		
		Listener addSurveyListener = new Listener(){
			@Override
			public void handleEvent(Event event) {
				addedSurveys.addAll(((SurveyFilteredComboViewer)event.widget).getCreatedSurveys());
				
				for(UiData ui: uiItems.values()){
					ui.cmbSurvey.setAdditionalSurveys(addedSurveys);
					ui.cmbSurvey.updateContent();
				}
			}
		};
		
		List<Mission> moreMissions = new ArrayList<>();
		int cnt = 1;
		for (CtMissionLink ll : missions.values()) moreMissions.add(ll.getMission());
		
		//sort new missions based on start time
		moreMissions.sort(new Comparator<Mission>(){
			@Override
			public int compare(Mission m1, Mission m2) {
				if (!SharedUtils.isSameDate(m1.getStartDate(), m2.getStartDate())) return m1.getStartDate().compareTo(m2.getStartDate());
				
				for(MissionDay md: m1.getMissionDays()){
					if (SharedUtils.isSameDate(md.getDate(), m1.getStartDate())){
						for(MissionDay md2: m2.getMissionDays()){
							if (SharedUtils.isSameDate(md2.getDate(), m2.getStartDate())){
								return md.getStartTime().compareTo(md2.getStartTime());
							}
						}		
					}
				}	
				return 0;
			}
		});
		//give them a temporary id for user interface purposes
		for (Mission m : moreMissions){
			m.setId(MessageFormat.format(Messages.MissionDialog_ImportedLabel, cnt++));
		}
		
		//we want to display these in order; 
		for (Mission m : moreMissions){
			Entry<UUID, CtMissionLink> e = null;
			for (Entry<UUID, CtMissionLink> tmp : missions.entrySet()){
				if (tmp.getValue().getMission() == m){
					e = tmp;
					break;
				}
			}
			
			Label l = new Label(main, SWT.WRAP);
			StringBuilder lbl = new StringBuilder();
			
			Date startDt= null;
			for(MissionDay md: e.getValue().getMission().getMissionDays()){
				if (SharedUtils.isSameDate(md.getDate(), e.getValue().getMission().getStartDate())){
					startDt = SmartUtils.combineDateTime(md.getDate(), md.getStartTime());
				}
			}
				
			Mission p = e.getValue().getMission();
			lbl.append(p.getId()); 
			lbl.append("\n"); //$NON-NLS-1$
			lbl.append(Messages.MissionDialog_startdateLabel);
			lbl.append(DateFormat.getDateTimeInstance().format(startDt));
			lbl.append("\n"); //$NON-NLS-1$
			lbl.append(Messages.MissionDialog_surveyDesignLabel);
			lbl.append(e.getValue().getNewSurveyDesign().getName()); 
			lbl.append("\n"); //$NON-NLS-1$
			lbl.append(Messages.MissionDialog_leaderLabel);
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
			btnNew.setText(Messages.MissionDialog_missionLabel);
			btnNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
			btnNew.setSelection(true);
			
			Label sl = new Label(op, SWT.NONE);
			sl.setText(Messages.MissionDialog_SurveyLabel);
			sl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
			
			SurveyFilteredComboViewer surveyViewer = new SurveyFilteredComboViewer(op, e.getValue().getNewSurveyDesign(), true);
			surveyViewer.setEnabled(true);
			surveyViewer.addSelectionChangedListener(listener);
			surveyViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			surveyViewer.setNewSurveyListener(addSurveyListener);
			
			
			Button btnExisting = new Button(op, SWT.RADIO);
			btnExisting.setText(Messages.MissionDialog_addExistingLabel);
			btnExisting.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
			btnExisting.setSelection(false);
			
			ArrayList<Mission> mm = new ArrayList<>(moreMissions);
			mm.remove(e.getValue().getMission());
			MissionFilteredComboViewer viewer = new MissionFilteredComboViewer(op, mm);
			viewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			viewer.setEnabled(false);
			viewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					validate();
					
				}
			});
			
			SelectionListener btnListener = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					viewer.setEnabled(btnExisting.getSelection());
					surveyViewer.setEnabled(btnNew.getSelection());
					validate();
				}
			};
			
			btnNew.addSelectionListener(btnListener);
			btnExisting.addSelectionListener(btnListener);
			
			
			uiItems.put(e.getKey(), new UiData(btnNew, btnExisting, viewer, surveyViewer, cd));
			spacer = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
			spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		}
		scroll.setContent(main);
		scroll.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		setTitle(Messages.MissionDialog_Title);
		setMessage(Messages.MissionDialog_Message);
		getShell().setText(Messages.MissionDialog_ShellTitle);
		
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
				entry.getValue().errItem.setDescriptionText(Messages.MissionDialog_MissionOrSurveyRequired);
				entry.getValue().errItem.show();
				error = true;
			}
			
			if (entry.getValue().btnExisting.getSelection()){
				if (entry.getValue().cmbMission.getSelection() == null){
					entry.getValue().errItem.setDescriptionText(Messages.MissionDialog_MissionRequired);
					entry.getValue().errItem.show();
					error = true;
				}else{
					
					Mission p = entry.getValue().cmbMission.getSelection();
					Mission ctP = missions.get(ctMission).getMission();
					
					double diff = (ctP.getStartDate().getTime() - p.getEndDate().getTime());
					if (diff > 48*60*60*100.0){
						//TODO: should be a warning not an error
						entry.getValue().errItem.setDescriptionText(MessageFormat.format(Messages.MissionDialog_DateWarning, DateFormat.getDateInstance().format(ctP.getStartDate()), DateFormat.getDateInstance().format(p.getEndDate())));
						entry.getValue().errItem.show();
//						error = true;
					}
					
					if (p.getUuid() == null){
						//we are merging to missions in the same file.  Make sure the start date and time of the new mission is
						//after the start date time of the merged mission
						//p before ctp
						MissionDay startAddTo = null;
						for (MissionDay md : p.getMissionDays()){
							if (startAddTo == null || md.getDate().before(startAddTo.getDate())){
								startAddTo = md;
							}
						}
						MissionDay startAddFrom = null;
						for (MissionDay md : ctP.getMissionDays()){
							if (startAddFrom == null || md.getDate().before(startAddFrom.getDate())){
								startAddFrom = md;
							}
						}
						Date startAddToDate = SmartUtils.combineDateTime(startAddTo.getDate(), startAddTo.getStartTime());
						Date startAddToFrom = SmartUtils.combineDateTime(startAddFrom.getDate(), startAddFrom.getStartTime());
						if (startAddToFrom.before(startAddToDate)){
							entry.getValue().errItem.setDescriptionText(Messages.MissionDialog_MergeDateError);
							entry.getValue().errItem.show();
							error = true;
						}
					}
				}
			}
			if (entry.getValue().btnNew.getSelection()){
				if (entry.getValue().cmbSurvey.getSelection() == null){
					entry.getValue().errItem.setDescriptionText(Messages.MissionDialog_SurveyRequired);
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
								entry.getValue().errItem.setDescriptionText(Messages.MissionDialog_MissionDatesInvalid);
								entry.getValue().errItem.show();
								error = true;			
							}
						}
						if (!missions.get(ctMission).getNewSurveyDesign().getUuid().equals(selectedSurvey.getSurveyDesign().getUuid())){
							entry.getValue().errItem.setDescriptionText(MessageFormat.format(Messages.MissionDialog_DifferentSurveyDesign, missions.get(ctMission).getNewSurveyDesign().getName(), selectedSurvey.getSurveyDesign().getName()));
							entry.getValue().errItem.show();
							error = true;
						}
					}else{
						entry.getValue().errItem.setDescriptionText(Messages.MissionDialog_SurveyRequired2);
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
