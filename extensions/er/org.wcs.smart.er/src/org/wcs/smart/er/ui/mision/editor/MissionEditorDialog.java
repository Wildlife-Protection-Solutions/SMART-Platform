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
package org.wcs.smart.er.ui.mision.editor;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.ui.ISurveyListener;
import org.wcs.smart.er.ui.mision.MissionComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for editing mission properties. 
 * 
 * @author Emily
 *
 */
public class MissionEditorDialog extends TitleAreaDialog {

	private MissionComposite composite;
	private Mission toUpdate;

	private boolean isChanged = false;
	
	/**
	 * 
	 * @param parentShell
	 * @param composite the edit composite
	 * @param toUpdate the mission to edit
	 */
	public MissionEditorDialog(Shell parentShell, 
			MissionComposite composite, 
			Mission toUpdate) {
		super(parentShell);
	
		this.composite = composite;
		this.toUpdate = toUpdate;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		Button ok = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		ok.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
	}
	
	
	public boolean close() {
		if (isChanged){
			MessageDialog md = new MessageDialog(getShell(), 
					Messages.MissionEditorDialog_SaveDialogTitle, 
					null, 
					Messages.MissionEditorDialog_SaveWarning, MessageDialog.QUESTION_WITH_CANCEL, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL},0);
			int ret = md.open();
			if (ret == 2){
				//cancel
				return false;
			}else if (ret == 0){
				//yes
				if (!saveChanges()){
					return false;
				}else{
					setReturnCode(IDialogConstants.OK_ID);
				}
			}
		}	
		return super.close();
	}
	
	private boolean saveChanges(){
		if (!composite.isValid()){
			return false;
		}
		
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			session.saveOrUpdate(toUpdate);
		
			Date currentStart = toUpdate.getStartDate();
			Date currentEnd = toUpdate.getEndDate();
			
			composite.updateDesign(toUpdate);
			
			if (!currentStart.equals(toUpdate.getStartDate()) ||
				!currentEnd.equals(toUpdate.getEndDate())){
				//dates have changed;
				//we need to:
				//1. remove any waypoints & attachments that are not associated with a day
				//2. remove any tracks that are not associated with a day
				
				//waypoints
				List<MissionDay> wpDelete = new ArrayList<MissionDay>();
				for (MissionDay md : toUpdate.getMissionDays()){
					if (!isBetweenMissionDates(
							SmartUtils.getDatePart(md.getDate(), false))){
						wpDelete.add(md);
					}
					
				}
				for (MissionDay md : wpDelete){
					for (SurveyWaypoint sw : md.getWaypoints()){
						Waypoint delete = sw.getWaypoint();
						session.delete(sw);
						session.delete(delete);
					}
					
					for (MissionTrack mt : md.getTracks()){
						session.delete(mt);
					}
					md.getWaypoints().clear();
					md.getTracks().clear();
					
					session.delete(md);
				}
				toUpdate.getMissionDays().removeAll(wpDelete);
				
				//need to create new mission days as required
				//create days
				Calendar calStart = SmartUtils.convertDate(toUpdate.getStartDate());
				calStart.set(Calendar.HOUR, 0);
				calStart.set(Calendar.MINUTE, 0);
				calStart.set(Calendar.SECOND, 0);
				calStart.set(Calendar.MILLISECOND, 0);
				
				Calendar calEnd = SmartUtils.convertDate(toUpdate.getEndDate());
				while (calStart.before(calEnd) || calStart.equals(calEnd)) {
					boolean found = false;
					for(MissionDay md : toUpdate.getMissionDays()){
						if (SmartUtils.isSameDate(md.getDate(), calStart.getTime())){
							found = true;
							break;
						}
					}
					if (!found){
						MissionDay md = new MissionDay();
						md.setDate(SmartUtils.getDatePart(calStart.getTime(), false));
						md.setStartTime(createTime(0, 0, 0));
						md.setEndTime(createTime(23, 59, 59));
						md.setRestMinutes(0);
						md.setTracks(new ArrayList<MissionTrack>());
						md.setWaypoints(new ArrayList<SurveyWaypoint>());
						md.setMission(toUpdate);
						toUpdate.getMissionDays().add(md);
					}
					calStart.add(Calendar.DAY_OF_MONTH, 1);
				}
				
				//ensure these are sorted correctly
				Collections.sort(toUpdate.getMissionDays(), new Comparator<MissionDay>() {

					@Override
					public int compare(MissionDay o1, MissionDay o2) {
						return o1.getDate().compareTo(o2.getDate());
					}
				});
			}
			
			
			session.getTransaction().commit();
			isChanged = false;
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}catch (Exception ex){
			EcologicalRecordsPlugIn.displayLog(Messages.MissionEditorDialog_SaveError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			return false;
		}finally{
			session.close();
		}
		
		SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, toUpdate);
		return true;
	}
	
	private Time createTime(int hours, int minute, int second){
		Calendar cForProcessing = Calendar.getInstance();
		cForProcessing.setTimeInMillis(0);
		cForProcessing.set(Calendar.HOUR_OF_DAY, hours);
		cForProcessing.set(Calendar.MINUTE, minute);
		cForProcessing.set(Calendar.SECOND, second);
		cForProcessing.set(Calendar.MILLISECOND, 0);
		return new Time(cForProcessing.getTime().getTime());
	}
	
	private boolean isBetweenMissionDates(Date date){
		return date.equals(toUpdate.getStartDate()) ||
			   date.equals(toUpdate.getEndDate()) ||
			   (date.before(toUpdate.getEndDate()) &&
				date.after(toUpdate.getStartDate()));		
	}
	protected void okPressed() {
		if (!saveChanges()){
			return ;
		}
		//super.okPressed();
	}

	
	protected Control createDialogArea(Composite parent) {
		Composite c = (Composite)super.createDialogArea(parent);
		
		
		composite.createControl(c);
		Session session = HibernateManager.openSession();
		try{
			session.update(toUpdate);
			session.update(toUpdate.getSurvey().getSurveyDesign());
			composite.init(toUpdate, session);
		}finally{
			session.close();
		}
		
		composite.addChangeListener(new ISurveyListener() {
			@Override
			public void compositeModified() {
				if (getButton(IDialogConstants.OK_ID) == null) return;
				getButton(IDialogConstants.OK_ID).setEnabled(composite.isValid());
				isChanged = true;
			}
		});
		
		setTitle(composite.getTitle());
		setMessage(composite.getDescription());
		getShell().setText(Messages.MissionEditorDialog_Title);
		return c;
	}
	
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
