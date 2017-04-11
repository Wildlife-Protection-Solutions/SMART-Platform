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

package org.wcs.smart.patrol.internal.ui.views;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.PatrolManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.PatrolWaypoint;


/*
 * Dialog allowing users to select details of how to merge Patrols
 * @author Jeff
 */

public class MergePatrolsDialog extends TitleAreaDialog {

	private Session session;
	private ArrayList<Patrol> patrolsToMerge;
	private ComboViewer patrolId;
	private Text txtPatrolId;
	private ComboViewer getStationFromID;
	private ComboViewer getObjectiveFromID; 
	
	public MergePatrolsDialog(Shell parentShell, ArrayList<Patrol> patrols, Session session) {
		super(parentShell);
		this.session = session;
		this.patrolsToMerge = patrols;
	}
	/*
	 * Create the dialog
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Label lbl;
		parent = (Composite) super.createDialogArea(parent);
		
		Composite patrolIdComp = new Composite(parent, SWT.NONE);
		patrolIdComp.setLayout(new GridLayout(2, false));
		patrolIdComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		ArrayList<Object> idOptionsPlusCustom = new ArrayList<Object>();
		ArrayList<Object> idOptions = new ArrayList<Object>();
		
		for(int x=0; x <patrolsToMerge.size(); x++){
			idOptionsPlusCustom.add(patrolsToMerge.get(x));
			idOptions.add(patrolsToMerge.get(x));
		}
		idOptionsPlusCustom.add(Messages.MergePatrolsDialog_EnterCustomId.toString());
		
		patrolId = createCombo(patrolIdComp, Messages.MergePatrolsDialog_PatrolId, idOptionsPlusCustom, patrolsToMerge.get(0));
		patrolId.addPostSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				StructuredSelection selected = (StructuredSelection) event.getSelection();
				if(selected.getFirstElement().equals(Messages.MergePatrolsDialog_EnterCustomId)){
					txtPatrolId.setEnabled(true);
				}else{
					txtPatrolId.setEnabled(false);
				}

			}
		});
		
		lbl = new Label(patrolIdComp, SWT.NONE);
		lbl.setText(Messages.MergePatrolsDialog_CustomPatrolId);
		txtPatrolId = new Text(patrolIdComp, SWT.BORDER);
		txtPatrolId.setTextLimit(Patrol.MAX_ID_LENGTH);
		txtPatrolId.setText(patrolsToMerge.get(0).getId());
		txtPatrolId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		txtPatrolId.setEnabled(false);
	
		getStationFromID = createCombo(patrolIdComp, Messages.MergePatrolsDialog_StationAndTeam, idOptions, patrolsToMerge.get(0));
		getObjectiveFromID = createCombo(patrolIdComp, Messages.MergePatrolsDialog_ObjectiveAndMandate, idOptions, patrolsToMerge.get(0));

		
		Composite notesComp = new Composite(parent, SWT.NONE);
		notesComp.setLayout(new GridLayout(1, false));
		notesComp.setLayoutData(new GridData(SWT.WRAP, SWT.CENTER, true, false));
		Label notes = new Label(notesComp, SWT.NONE);
		notes.setText(Messages.MergePatrolsDialog_Note1);
		Label notes2 = new Label(notesComp, SWT.NONE);
		notes2.setText(Messages.MergePatrolsDialog_Note2);
		
		setMessage(Messages.MergePatrolsDialog_MergeInstructions);
		getShell().setText(Messages.MergePatrolsDialog_MergePatrols);
		setTitle(Messages.MergePatrolsDialog_MergePatrols); 
		return parent;
	}
	
	/*
	 * Create a combo viewer for patrol or text selections
	 */
	private ComboViewer createCombo(Composite parent, String name, ArrayList<Object> options, Object defaultValue){
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(name);
		ComboViewer cmb = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmb.setLabelProvider(new PatrolIDLabelProvider());
		cmb.setContentProvider(ArrayContentProvider.getInstance());
		cmb.setInput(options);
		cmb.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		if(defaultValue != null){
			cmb.setSelection( new StructuredSelection(defaultValue));
		}
		
		return cmb;
	}
	
	private class PatrolIDLabelProvider extends LabelProvider {
    	@Override
    	public String getText(Object element) {
    		if (element instanceof Patrol) {
    			return ((Patrol)element).getId();
    		}
    		return super.getText(element);
    	}
    }

	
	/**
	 * Create the new Patrol and assign all the legs of each patrol to be merged to the new one.
	 */
	@Override
	protected void okPressed() {
		session.beginTransaction();
		//Make a new Patrol to put everything into:
		Patrol newPatrol = new Patrol();
		newPatrol.setLegs(new ArrayList<PatrolLeg>());

		
		//Set the ID of the new patrol
		Object selection =  ((IStructuredSelection)patrolId.getSelection()).getFirstElement();
		Patrol patrol;
		
		if(selection instanceof Patrol){
			patrol = (Patrol)selection;
			newPatrol.setId(patrol.getId());
		}else{
			newPatrol.setId(txtPatrolId.getText());
		}
		
		//set the station of the new patrol
		Patrol stationId =  (Patrol)((IStructuredSelection)getStationFromID.getSelection()).getFirstElement();
		newPatrol.setStation(stationId.getStation());
		
		//set the Objective
		Patrol objectiveId =  (Patrol)((IStructuredSelection)getObjectiveFromID.getSelection()).getFirstElement();
		newPatrol.setObjective(objectiveId.getObjective());

		//set the rest of the attrs
		newPatrol.setConservationArea(stationId.getConservationArea());
		boolean isArmed = false;
		String allComments = ""; //$NON-NLS-1$
		for(Patrol p :patrolsToMerge){
			if(p.isArmed())isArmed = true;
			allComments += p.getComment() + " -- "; //$NON-NLS-1$
		}
		
		//gather start/end dates
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, 2200);
		cal.set(Calendar.MONTH, Calendar.JANUARY);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		Date startDate = cal.getTime(); 

		cal.set(Calendar.YEAR, 1900);
		cal.set(Calendar.MONTH, Calendar.JANUARY);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		Date endDate = cal.getTime();
		 
		for(Patrol p : patrolsToMerge){
			for(PatrolLeg l : p.getLegs()){
				if(l.getStartDate().before(startDate)) startDate = l.getStartDate();
				if(l.getEndDate().after(endDate)) endDate = l.getEndDate();
		  	}
		}
		newPatrol.setArmed(isArmed);
		newPatrol.setComment(allComments);
		newPatrol.setEndDate(endDate);
		newPatrol.setStartDate(startDate);
		newPatrol.setMandate(objectiveId.getMandate());
		newPatrol.setPatrolType(PatrolType.Type.MIXED);
		newPatrol.setTeam(stationId.getTeam());

		
		//Save the new Patrol
		session.save(newPatrol);
		session.flush();
		session.getTransaction().commit();

		final UUID uuid = newPatrol.getUuid();
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.MergePatrolsDialog_MergingPatrols, patrolsToMerge.size());
					
					
					session.beginTransaction();
					Patrol newPatrol = (Patrol)session.createCriteria(Patrol.class).add(Restrictions.eq("uuid", uuid)).uniqueResult();
					
					for(Patrol p : patrolsToMerge){
					  for(PatrolLeg pl : p.getLegs()){
						PatrolLeg legClone = pl.simpleClone();
						legClone.setPatrolLegDays(new ArrayList<PatrolLegDay>());
						if (pl.getPatrolLegDays() != null && pl.getPatrolLegDays().size() > 0){
							//Clone Leg Days as well
							for (PatrolLegDay pld : pl.getPatrolLegDays()){
								PatrolLegDay legdayClone = pld.clone();
								
								ArrayList<PatrolWaypoint> allWaypoints = new ArrayList<PatrolWaypoint>();
								
								for(PatrolWaypoint wp : pld.getWaypoints()){
									Waypoint toClone = wp.getWaypoint();
									if (toClone.getUuid() != null){
										toClone = (Waypoint)session.merge(toClone);
									}
									Waypoint wpclone = toClone.clone(session);
									
									PatrolWaypoint pw = new PatrolWaypoint();
									pw.setWaypoint(wpclone);
									pw.setPatrolLegDay(legdayClone);
									allWaypoints.add(pw);
								}
								legdayClone.setWaypoints(allWaypoints); 
								legdayClone.setPatrolLeg(legClone);
								legClone.getPatrolLegDays().add(legdayClone);
							}
						}
						legClone.setPatrol(newPatrol);
						newPatrol.getLegs().add(legClone);
					  }	
					}
					

					
					try {
//						PatrolHibernateManager.savePatrol(newPatrol, session,  true);
						newPatrol.createLegs(session);
						PatrolHibernateManager.savePatrol(newPatrol, session,  true);
					} catch (Exception e1) {
						SmartPatrolPlugIn.displayLog(
								Messages.MergePatrolsDialog_MergeError,
								e1);
						session.getTransaction().rollback();
						e1.printStackTrace();
					}
					session.getTransaction().commit();

					
					//delete all the original patrols 
					for (Patrol p: patrolsToMerge){
						try {
							PatrolManager.getInstance().deletePatrol(p.getUuid(), new SubProgressMonitor(monitor, 1));
						} catch (Exception e) {
							SmartPatrolPlugIn.displayLog(
									Messages.DeletePatrolHandler_Error_CouldNotDeletePatrol, e);
						}
					}
					monitor.done();
				}
			});
		} catch (Exception ex) {
			SmartPatrolPlugIn.displayLog(
					Messages.MergePatrolsDialog_MergeError,
					ex);
			session.getTransaction().rollback();
		}
		super.okPressed();
	}
}





