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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
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
import org.wcs.smart.SmartContext;
import org.wcs.smart.observation.model.IWaypointSourceEngine;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.patrol.IPatrolEditContribution;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.PatrolManager;
import org.wcs.smart.patrol.PatrolUtils;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolAttributeValue;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.util.SmartUtils;


/*
 * Dialog allowing users to select details of how to merge Patrols
 * 
 * @author Jeff
 */
public class MergePatrolsDialog extends SmartStyledTitleDialog {

	private Session session;
	private ArrayList<Patrol> patrolsToMerge;
	private ComboViewer patrolId;
	private Text txtPatrolId;
	private ComboViewer getStationFromID;
	private ComboViewer getObjectiveFromID; 
	
	private static final PatrolWaypointSource PATROL_WP_SRC = (PatrolWaypointSource) SmartContext.INSTANCE.getClass(IWaypointSourceEngine.class)
			.getSource(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
	
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
	
		getStationFromID = createCombo(patrolIdComp, Messages.MergePatrolsDialog_StationAndTeam1, idOptions, patrolsToMerge.get(0));
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
		
		//Make a new Patrol to put everything into:
		Patrol newPatrol = new Patrol();
		newPatrol.setLegs(new ArrayList<PatrolLeg>());
		
		//Set the ID of the new patrol
		Object selection = ((IStructuredSelection)patrolId.getSelection()).getFirstElement();
		
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
		newPatrol.setTeam(stationId.getTeam());
		newPatrol.setCustomAttributes(new ArrayList<>());
		//custom attribute
		for (PatrolAttributeValue pa : stationId.getCustomAttributes()) {
			PatrolAttributeValue v = new PatrolAttributeValue();
			v.setPatrolAttribute(pa.getPatrolAttribute());
			v.setAttributeListItem(pa.getAttributeListItem());
			v.setAttributeTreeNode(pa.getAttributeTreeNode());
			v.setStringValue(pa.getStringValue());
			v.setNumberValue(pa.getNumberValue());
			v.setPatrol(newPatrol);
			newPatrol.getCustomAttributes().add(v);
		}
					
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
		if (allComments.length() > Patrol.MAX_COMMENT_LENGTH){
			MessageDialog.openInformation(getShell(), Messages.MergePatrolsDialog_WarningDialogTitle, MessageFormat.format(Messages.MergePatrolsDialog_MergeCommentWarning, Patrol.MAX_COMMENT_LENGTH));
			allComments = allComments.substring(0, Patrol.MAX_COMMENT_LENGTH);
		}
		
		
		
		//gather start/end dates
		LocalDate startDate = LocalDate.MAX;
		LocalDate endDate = LocalDate.MIN;
		for(Patrol p : patrolsToMerge){
			for(PatrolLeg l : p.getLegs()){
				if(l.getStartDate().isBefore(startDate)) startDate = l.getStartDate();
				if(l.getEndDate().isAfter(endDate)) endDate = l.getEndDate();
		  	}
		}
		newPatrol.setArmed(isArmed);
		newPatrol.setComment(allComments);
		newPatrol.setEndDate(endDate);
		newPatrol.setStartDate(startDate);
		newPatrol.recalculateType();
		

		if (ChronoUnit.DAYS.between(newPatrol.getStartDate(), newPatrol.getEndDate()) > Patrol.MAX_PATROL_LENGTH_DAYS){
			MessageDialog.openError(getShell(), Messages.MergePatrolsDialog_ErrorDialogTitle, 
					MessageFormat.format(Messages.MergePatrolsDialog_PatrolToLong, Patrol.MAX_PATROL_LENGTH_DAYS));
			return;
		}
		
		//compute all attachment locations
		try {
			for(Patrol p : patrolsToMerge) PatrolHibernateManager.computeAttachmentLocations(p, session);
		}catch (Exception ex) {
			SmartPatrolPlugIn.displayLog(ex.getMessage(), ex);
			return;
		}
		
		List<IPatrolEditContribution> contributions = IPatrolEditContribution.findContributions();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException,
						InterruptedException {
					SubMonitor progress = SubMonitor.convert(monitor, Messages.MergePatrolsDialog_MergingPatrols, patrolsToMerge.size()+1);
					
					try {
						session.beginTransaction();
						
						session.persist(newPatrol);
						session.flush();
						
						for (Patrol p : patrolsToMerge) {
							for (PatrolLeg pl : p.getLegs()) {
								PatrolLeg legClone = pl.simpleClone();
								legClone.setPatrol(newPatrol);
								newPatrol.getLegs().add(legClone);
								legClone.setPatrolLegDays(new ArrayList<PatrolLegDay>());
								
								session.persist(legClone);
								contributions.forEach(c->c.mergePatrolMovePatrolLeg(pl, legClone, session));
								
								if (pl.getPatrolLegDays() != null
										&& pl.getPatrolLegDays().size() > 0) {
									// Clone Leg Days as well
									for (PatrolLegDay pld : pl.getPatrolLegDays()) {
										PatrolLegDay legdayClone = pld.clone();
										legdayClone.setPatrolLeg(legClone);
										legClone.getPatrolLegDays().add(legdayClone);
										session.persist(legdayClone);
								
										ArrayList<PatrolWaypoint> allWaypoints = new ArrayList<PatrolWaypoint>();
										for (PatrolWaypoint pw : pld.getWaypoints()) {
											
											//reuse waypoints so we don't fire new events
											//#2990
											session.createMutationQuery("DELETE FROM PatrolWaypoint WHERE id.waypoint.uuid = :wpuuid and id.patrolLegDay.uuid = :leguuid") //$NON-NLS-1$
												.setParameter("wpuuid", pw.getWaypoint().getUuid()) //$NON-NLS-1$
												.setParameter("leguuid", pw.getPatrolLegDay().getUuid()) //$NON-NLS-1$
												.executeUpdate();
	
											PatrolWaypoint newPw = new PatrolWaypoint();
											newPw.setWaypoint(pw.getWaypoint());
											newPw.setPatrolLegDay(legdayClone);
											allWaypoints.add(newPw);
											session.persist(newPw);
											
											//create new waypoint attachments
											//then delete the old ones - we do
											//this because the files need to move to a new folder
											List<WaypointAttachment> copiedAttachments = new ArrayList<>();
											for (WaypointAttachment attachment : newPw.getWaypoint().getAttachments()) {
												WaypointAttachment clone = new WaypointAttachment();
												clone.setWaypoint(newPw.getWaypoint());
												
												
												clone.setCopyFromLocation(attachment.getAttachmentFile());
												clone.setFilename(attachment.getFilename());
												clone.setSignatureType(attachment.getSignatureType());
												
												clone.computeFileLocation(Paths.get(p.getConservationArea().getFileDataStoreLocation())
													.resolve(PATROL_WP_SRC.getDatastoreFileLocation(newPatrol, session))
													.resolve(clone.getFilename()));
												
												session.persist(clone);
												copiedAttachments.add(clone);
												
											}
											
											for (WaypointAttachment attachment : newPw.getWaypoint().getAttachments()) {
												session.remove(attachment);
											}
											newPw.getWaypoint().getAttachments().clear();
											newPw.getWaypoint().getAttachments().addAll(copiedAttachments);
											
											//do the same thing for observation attachments
											for (WaypointObservationGroup group : newPw.getWaypoint().getObservationGroups()) {
												for (WaypointObservation wo : group.getObservations()) {
													
													List<ObservationAttachment> copiedAttachments2 = new ArrayList<>();
													for (ObservationAttachment attachment : wo.getAttachments()) {
														ObservationAttachment clone = new ObservationAttachment();
														clone.setObservation(wo);
																											
														clone.setCopyFromLocation(attachment.getAttachmentFile());
														clone.setFilename(attachment.getFilename());
														clone.setSignatureType(attachment.getSignatureType());
														
														clone.computeFileLocation(Paths.get(p.getConservationArea().getFileDataStoreLocation())
															.resolve(PATROL_WP_SRC.getDatastoreFileLocation(newPatrol, session))
															.resolve(clone.getFilename()));
														
														session.persist(clone);
														copiedAttachments2.add(clone);
													}
													
													for (ObservationAttachment attachment : wo.getAttachments()) {
														session.remove(attachment);
													}
													wo.getAttachments().clear();
													wo.getAttachments().addAll(copiedAttachments2);
												}
											}
											
											
										}
										legdayClone.setWaypoints(allWaypoints);
										
									}
								}
								
							}
							progress.worked(1);
						}
						
						session.flush();
						PatrolUtils.createLegDaysForMissingDays(newPatrol);
						PatrolHibernateManager.savePatrol(newPatrol, session,  true);
						
						session.getTransaction().commit();
					}catch (Exception ex) {
						SmartPatrolPlugIn.displayLog(Messages.MergePatrolsDialog_MergeError, ex);
						session.getTransaction().rollback();
						
						//try to remove folder 
						if (newPatrol.getUuid() != null) {
							Path patrolFolder = Paths.get(newPatrol.getConservationArea().getFileDataStoreLocation())
								.resolve(PATROL_WP_SRC.getDatastoreFileLocation(newPatrol));
							try {
								SmartUtils.deleteDirectory(patrolFolder);
							}catch (Exception ex2) {
								SmartPatrolPlugIn.log(ex2.getMessage(), ex2);
							}
						}
						return;
					}

					
					//delete all the original patrols 
					progress.setWorkRemaining(patrolsToMerge.size());
					for (Patrol p: patrolsToMerge){
						try {
							PatrolManager.getInstance().deletePatrol(p.getUuid(), false, progress.split(1));
						} catch (Exception e) {
							SmartPatrolPlugIn.displayLog(
									Messages.DeletePatrolHandler_Error_CouldNotDeletePatrol, e);
						}
					}
				}
			});
		} catch (Exception ex) {
			SmartPatrolPlugIn.displayLog(
					Messages.MergePatrolsDialog_MergeError,
					ex);
			session.getTransaction().rollback();
		}
		
		//fire events
		PatrolEventManager.getInstance().patrolAdded(newPatrol);
		
		super.okPressed();
	}

}





