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
package org.wcs.smart.patrol.ui;

import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.common.control.CombinedSelectionProvider;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolEventManager.EventType;
import org.wcs.smart.patrol.PatrolEventManager.IPatrolEventListener;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.PatrolManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.editor.PatrolContributionPageEditor;
import org.wcs.smart.patrol.internal.ui.editor.PatrolDayEditor;
import org.wcs.smart.patrol.internal.ui.editor.PatrolDayEditorInput;
import org.wcs.smart.patrol.internal.ui.editor.PatrolMapPageEditor;
import org.wcs.smart.patrol.internal.ui.editor.PatrolSummaryEditor;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.patrol.model.WaypointAttachmentInterceptor;
import org.wcs.smart.util.SharedUtils;

/**
 * The patrol editor.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolEditor extends MultiPageEditorPart implements MapPart, IAdaptable{

	private static final String HOUR_LABEL = Messages.PatrolEditor_HourLabel;

	private static final String MINUTE_LABEL = Messages.PatrolEditor_MinuteLabel;

	public static final String ID = "org.wcs.smart.patrol.ui.PatrolEditor"; //$NON-NLS-1$

	private static final String SAVE_PATROL_JOB_NAME = Messages.PatrolEditor_SavePatrol_JobName;
	
	public static final DecimalFormat DISTANCE_FORMATTER = new DecimalFormat("#0.##"); //$NON-NLS-1$
	
	private Patrol patrol = null;
	private ObservationOptions ops = null;
	private PatrolSummaryEditor summaryEditor;
	private PatrolMapPageEditor mapPage;
	private Projection[] projections;
	private CombinedSelectionProvider selectionProvider = new CombinedSelectionProvider();
	
	private IPatrolEventListener saveListener = new IPatrolEventListener() {
		@Override
		public void eventFired(final int attributeChanged, Object source) {
			Patrol p = null;
			if (source instanceof PatrolLegDay){
				p = ((PatrolLegDay)source).getPatrolLeg().getPatrol();
			}else if (source instanceof Patrol){
				p = (Patrol)source;
			}
			if (p != null && p.equals(patrol)){
				if (attributeChanged == PatrolEventManager.PATROL_DATES_LEG){
					//reload patrol & update summary and day pages
					Job j = new Job("load patrol"){ //$NON-NLS-1$
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							patrol = null;
							getPatrol();
							Session s = HibernateManager.openSession();
							s.beginTransaction();
							try{
								s.update(patrol);
								updateSummaryPage();
							}finally{
								s.close();
							}
							getSite().getShell().getDisplay().syncExec(new Runnable(){
								@Override
								public void run() {
									createDayPages();
									mapPage.refresh();
								}});
							return Status.OK_STATUS;
						}					
					};
					j.setSystem(true);
					j.schedule();
				
				}else{
					updateSummaryPage();
				}

			}
		}
	};
	
	
	private IPatrolEventListener patrolDeleteListener = new IPatrolEventListener() {
		@Override
		public void eventFired(int attributeChanged, Object source) {
			if ( ((Patrol)source ).equals(PatrolEditor.this.patrol)  ){
				//close this editor
				getSite().getShell().getDisplay().asyncExec(new Runnable(){
					@Override
					public void run() {
						PatrolEditor.this.getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(PatrolEditor.this, false);					
					}});
			}
		}
	};
	
	/**
	 * Converts a double that represents a time range into
	 * and hours and minutes string.  For example: 20.5 is 
	 * converted into "20h 30m"
	 * 
	 * @param hrs time range in hours
	 * @return formatted string
	 */
	public static String formatTimeRange(Double hrs){
		boolean minus = false;
		if (hrs < 0){
			minus = true;
			hrs = -hrs;
		}
		int lhrs = (int)Math.floor(hrs);
		int lmin = (int)Math.round((hrs - lhrs) * 60.0);
		
		if (lmin == 60){
			lmin = 0;
			lhrs++;
		}
		if (minus){
			return "-" + lhrs + HOUR_LABEL + " " + lmin + MINUTE_LABEL; //$NON-NLS-1$ //$NON-NLS-2$
		}else{
			return lhrs + HOUR_LABEL + " " + lmin + MINUTE_LABEL; //$NON-NLS-1$
		}
	}
	
	public PatrolEditor() {
		super();
		PatrolEventManager.getInstance().addListener(EventType.PATROL_SAVED, saveListener);
		PatrolEventManager.getInstance().addListener(EventType.PATROL_DELETED, patrolDeleteListener);
	}

	public Projection[] getAvailableProjections(){
		return this.projections;
	}
	
	@Override
	public void dispose() {
		PatrolEventManager.getInstance().removeListener(EventType.PATROL_SAVED, saveListener);
		PatrolEventManager.getInstance().removeListener(EventType.PATROL_DELETED, patrolDeleteListener);
		super.dispose();
	}

	public ObservationOptions getOptions(){
		return this.ops;
	}
	
	/**
	 * 
	 * @return null if the patrol can be editted, otherwise a string
	 * that described reason why can't be edited.
	 */
	public String canEdit(){
		return PatrolManager.getInstance().canEdit(patrol, ops);
	}
	
	public Patrol getPatrol(){
		if (this.patrol == null){
			
			UUID puuid = ((PatrolEditorInput) getEditorInput()).getUuid();
			Session session = HibernateManager.openSession();
			try{
				//load patrol items so don't have lazy loading issues later.
				session.beginTransaction();
				this.patrol = (Patrol) session.load(Patrol.class, puuid);
				this.patrol.getLegs().size();
				this.patrol.getPatrolDatastorePath();
				List<Projection> tmp = HibernateManager.getCaProjectionList(session);
				this.projections = tmp.toArray(new Projection[tmp.size()]);
				
				try{
					for (PatrolLeg pl : patrol.getLegs()){
						for (PatrolLegDay pld : pl.getPatrolLegDays()){
							for (PatrolWaypoint pw : pld.getWaypoints()){
								ObservationHibernateManager.computeAttachmentLocations(pw.getWaypoint(), session);
							}
						}
					}
				} catch (Exception e) {
					SmartPatrolPlugIn.displayLog(Messages.PatrolEditor_AttachmentError + "\n\n" + e.getMessage(), e); //$NON-NLS-1$
				}
				session.getTransaction().commit();
				if (ops == null){
					ops = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(),session);
				}
			}catch (Exception ex){
				if (session.getTransaction().isActive()){
					session.getTransaction().rollback();
				}
				throw ex;
			}finally{
				session.close();	
			}
			
		}
		return this.patrol;
	}

	public void updatePartName(){
		super.setPartName(Messages.PatrolEditor_EditorName_Prefix + getPatrol().getId());
	}
	
	/**
	 * Finds and displays the given waypoint uuid.
	 * 
	 * @param waypointUuid
	 */
	public void findAndShow(UUID waypointUuid){
		PatrolWaypoint wp = null;
		Session s = HibernateManager.openSession();
		
		try{
			wp = (PatrolWaypoint) s.createCriteria(PatrolWaypoint.class)
					.add(Restrictions.eq("id.waypoint.uuid", waypointUuid)) //$NON-NLS-1$
					.uniqueResult();
			if (wp == null) return;
			wp.getPatrolLegDay().getDate();
		}finally{
			s.close();
		}
		
		final PatrolWaypoint wp2 = wp;
		for (int i = 0; i < getPageCount(); i++){
			IEditorPart part = getEditor(i);
			if (part instanceof PatrolDayEditor){
				final PatrolDayEditor pde = (PatrolDayEditor)part;			
				if ( ((PatrolDayEditorInput)pde.getEditorInput()).getPatrolDay().getTime() == wp.getPatrolLegDay().getDate().getTime()  ){
					setActivePage(i);
						
					Display.getDefault().asyncExec(new Runnable(){
						//do this as a job in the display thread so the ui
						//has a chance to change pages, before it attempts
						//to scroll to the correct controls
						@Override
						public void run() {
							pde.findAndGoTo(wp2);	
						}
					});
					return;
				}
			}
		}
	}
	
	@Override
	protected void createPages() {
		showBusy(true);
		try {
			getPatrol();
			
			summaryEditor = new PatrolSummaryEditor(this);
			int i = addPage(summaryEditor, getEditorInput());
			setPageText(i, Messages.PatrolEditor_PatrolSummaryPageName);
			createDayPages();
			
			mapPage = new PatrolMapPageEditor(PatrolEditor.this);
			int mapIndex = addPage(mapPage, getEditorInput());
			setPageText(mapIndex, Messages.PatrolEditor_PatrolMapPageName);
			
			
			if (PatrolContributionPageEditor.hasContributions()){
				PatrolContributionPageEditor contributionPage = new PatrolContributionPageEditor(PatrolEditor.this);
				int index = addPage(contributionPage, getEditorInput());
				setPageText(index, Messages.PatrolEditor_OtherPatrolTabName);
			}
			
			getSite().setSelectionProvider(selectionProvider);
		} catch (final Throwable t) {
			PatrolEditor.this.getSite().getPage().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable(){
				@Override
						public void run() {
							try {
								PatrolEditor.this.dispose();
								PatrolEditor.this.getSite().getPage().closeEditor(PatrolEditor.this, false);
								if (t instanceof SWTError&& t.getMessage().contains("No more handles")) { //$NON-NLS-1$
									SmartPatrolPlugIn.displayLog(Messages.PatrolEditor_LoadEditorError_NoMoreHandlers + t.getLocalizedMessage(), t);
								} else {
									SmartPatrolPlugIn.displayLog(Messages.PatrolEditor_LoadEditorError_Other+ t.getLocalizedMessage(), t);
								}
							} catch (Exception ex) {
								SmartPatrolPlugIn.log("Failure",ex); //$NON-NLS-1$
							}

						}
			});
			throw new RuntimeException(Messages.PatrolEditor_LoadEditorError_Other + t.getMessage(), t);
		}finally{
			showBusy(false);
		}
		updatePartName();
	}
	
	public CombinedSelectionProvider getSelectionProvider(){
		return this.selectionProvider;
	}
	
	public void updateSummaryPage(){
		summaryEditor.refreshPatrolSummaryTable();
	}
	
	
	public void createDayPages( ) {
		try {
			int i = 0;
			while( i < getPageCount()){
				if (getEditor(i) instanceof PatrolDayEditor){
					removePage(i);
				}else{
					i++;
				}
			}
			int insertindex = 1;
			Calendar calStart = SharedUtils.convertDate(getPatrol().getStartDate());
			calStart.set(Calendar.HOUR, 0);
			calStart.set(Calendar.MINUTE, 0);
			calStart.set(Calendar.SECOND, 0);
			calStart.set(Calendar.MILLISECOND, 0);
			
			Calendar calEnd = SharedUtils.convertDate(getPatrol().getEndDate());
			
			while (calStart.before(calEnd) || calStart.equals(calEnd)) {
				PatrolDayEditorInput input = new PatrolDayEditorInput(calStart.getTime());
				PatrolDayEditor editor = new PatrolDayEditor(this);
				super.addPage(insertindex, editor, input);
				super.setPageText(
						insertindex,
						DateFormat.getDateInstance(DateFormat.MEDIUM).format(
								input.getPatrolDay()));
				insertindex++;
				calStart.add(Calendar.DAY_OF_MONTH, 1);
			}

		} catch (Exception ex) {
			SmartPatrolPlugIn.displayLog(Messages.PatrolEditor_LoadEditorError_ErrorCreatingDayPages, ex);
		}
	}
	

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	public void save(Patrol patrol){
		savePatrolPart(patrol);
	}
	
	public void save(PatrolLegDay patrolLegDay){
		patrolLegDay.getTrack();
		savePatrolPart(patrolLegDay);
	}
	
	/**
	 * Returns a job to save one set of waypoints and remove another. Does not schedule job.
	 * 
	 * @param toSave waypoints to save
	 * @param toDelete waypoints to delete
	 * @return
	 */
	public Job moveWaypoints(final Collection<PatrolWaypoint> toSave, final Collection<PatrolWaypoint> toDelete){
		Job moveJob = new Job(SAVE_PATROL_JOB_NAME) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session saveSession = HibernateManager.openSession(new WaypointAttachmentInterceptor());
				try{
					saveSession.beginTransaction();
					
					/* delete waypoints */
					for (PatrolWaypoint wp : toDelete) {
						saveSession.delete(wp);
						saveSession.delete(wp.getWaypoint());					
					}
					Patrol p = null;
					File rootFolder = null;
					if (toSave.size() > 0){
						PatrolWaypointSource pws = (PatrolWaypointSource) WaypointSourceEngine.INSTANCE.getSource(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
						p = toSave.iterator().next().getPatrolLegDay().getPatrolLeg().getPatrol();
						rootFolder = new File(p.getConservationArea().getFileDataStoreLocation(), pws.getDatastoreFileLocation(p));
					}
					/* save waypoints */
					for (PatrolWaypoint wp : toSave) {
						if (!wp.getPatrolLegDay().getPatrolLeg().getPatrol().equals(p)) throw new Exception("Cannot save waypoints that are not associated with the same patrol in a single statement."); //$NON-NLS-1$
						
						wp.getWaypoint().setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
						wp.getWaypoint().setConservationArea(SmartDB.getCurrentConservationArea());
						
						//configure attachment locations; this is necessary because the waypoint is saved before the patrol waypoint
						//and in these case the waypoint won't be able to find the patrol waypoint which is necessary to compute
						//the filestore location
						if (wp.getWaypoint().getAttachments() != null){
							for (WaypointAttachment wa : wp.getWaypoint().getAttachments()){
								wa.computeFileLocation(new File(rootFolder, wa.getFilename()));
							}
						}
						if (wp.getWaypoint().getObservations() != null){
							for (WaypointObservation wo : wp.getWaypoint().getObservations()){
								if (wo.getAttachments() != null){
									for (ObservationAttachment wa : wo.getAttachments()){
										wa.computeFileLocation(new File(rootFolder, wa.getFilename()));
									}
								}
							}
						}
						
						saveSession.saveOrUpdate(wp.getWaypoint());
						saveSession.saveOrUpdate(wp);
						
						
						// remove observations with no data
						if (wp.getWaypoint().getObservations() != null) {
							for (WaypointObservation wo : wp.getWaypoint().getObservations()) {
								List<WaypointObservationAttribute> toDelete = new ArrayList<WaypointObservationAttribute>();
								for (WaypointObservationAttribute att : wo.getAttributes()) {
									if (!att.hasValue()) {
										toDelete.add(att);
									}
								}
								wo.getAttributes().removeAll(toDelete);
							}
						}
					}
					
					saveSession.getTransaction().commit();
				}catch (Exception ex){
					if (saveSession.getTransaction().isActive()){
						saveSession.getTransaction().rollback();
					}
					SmartPatrolPlugIn.displayLog(Messages.PatrolEditor_DeleteWaypointsError + ex.getLocalizedMessage(), ex);
				}finally{
					saveSession.close();
				}
				
				//load the attachment locations after close to ensure
				//items can be viewed in ui without error
				Session s = HibernateManager.openSession();
				try{
					for (PatrolWaypoint wp : toSave) {
						if (wp.getWaypoint().getAttachments() != null){
							for (WaypointAttachment wa : wp.getWaypoint().getAttachments()){
								wa.computeFileLocation(s);
							}
						}
						if (wp.getWaypoint().getObservations() != null){
							for (WaypointObservation wo : wp.getWaypoint().getObservations()){
								if (wo.getAttachments() != null){
									for (ObservationAttachment wa : wo.getAttachments()){
										wa.computeFileLocation(s);
									}
								}
							}
						}
					}
				}catch (Exception ex){
					ex.printStackTrace();
				}finally{
					s.close();
				}
				
				/* fire events */
				for (PatrolWaypoint wp : toDelete){
					try{
						PatrolEventManager.getInstance().waypointDeleted(wp);
					}catch (Exception ex){
						SmartPatrolPlugIn.log("Error firing event after waypoint delete.", ex); //$NON-NLS-1$
					}
				}
				for (PatrolWaypoint wp : toSave){
					try{
						PatrolEventManager.getInstance().waypointModified(wp);
					}catch (Exception ex){
						SmartPatrolPlugIn.log("Error firing event after waypoint save.", ex); //$NON-NLS-1$
					}
				}
				return Status.OK_STATUS;
			}
		};
		return moveJob;
	}
	
	/**
	 * Saves the collection of waypoints.
	 * 
	 * @param waypoints
	 */
	public Job save(Collection<PatrolWaypoint> waypoints) {
		SaveWaypointJob saveWaypointJob = new SaveWaypointJob();
		saveWaypointJob.setWaypoints(waypoints);
		saveWaypointJob.schedule();
		return saveWaypointJob;
	}
	
	/**
	 * Deletes the collection of waypoints in a separate thread.
	 * 
	 * @param waypoints
	 * @return the job responsible for deleting waypoints
	 */
	public Job delete(final Collection<PatrolWaypoint> waypoints) {
		Job saveJob = new Job(SAVE_PATROL_JOB_NAME) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session saveSession = HibernateManager
						.openSession(new WaypointAttachmentInterceptor());
				try{
					saveSession.beginTransaction();
					for (PatrolWaypoint wp : waypoints) {
						saveSession.delete(wp);
						saveSession.delete(wp.getWaypoint());					
					}
					saveSession.getTransaction().commit();
				}catch (Exception ex){
					if (saveSession.getTransaction().isActive()){
						saveSession.getTransaction().rollback();
					}
					SmartPatrolPlugIn.displayLog(Messages.PatrolEditor_DeleteWaypointsError + ex.getLocalizedMessage(), ex);
				}finally{
					saveSession.close();
				}
				for (PatrolWaypoint wp : waypoints){
					try{
						PatrolEventManager.getInstance().waypointDeleted(wp);
					}catch (Exception ex){
						SmartPatrolPlugIn.log("Error firing event after waypoint delete.", ex); //$NON-NLS-1$
					}
				}
				
				return Status.OK_STATUS;
			}
		};
		saveJob.schedule();
		return saveJob;
	}
	
	private void savePatrolPart(final Object object){
		//update all the patrol values
		for (int i = 0; i < getPageCount(); i ++){
			getEditor(i).doSave(new NullProgressMonitor());
		}
		SavePatrolPartJob saveJob = new SavePatrolPartJob(patrol, object);		
		saveJob.schedule();
		try{
			saveJob.join();
		}catch (InterruptedException ex){
			throw new IllegalStateException("Save Job Interrupted", ex); //$NON-NLS-1$
		}
	}
	
	
	@Override
	public void doSave(IProgressMonitor monitor) {

		//update all the patrol values
		for (int i = 0; i < getPageCount(); i ++){
			getEditor(i).doSave(monitor);
		}
		Job saveJob = new Job(SAVE_PATROL_JOB_NAME) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session saveSession = HibernateManager.openSession(new WaypointAttachmentInterceptor());
				try{
					if (PatrolHibernateManager.savePatrolInTransaction(patrol, saveSession, false)){
						//saved okay
						PatrolEventManager.getInstance().patrolSaved(patrol, false);
					}
				}finally{
					if (saveSession.isOpen()){
						saveSession.close();
					}
				}
				return Status.OK_STATUS;
			}
		};
		saveJob.schedule();
				
	}

	@Override
	public void doSaveAs() {
	}

	/* (non-Javadoc)
	 * @see org.locationtech.udig.project.ui.internal.MapPart#getMap()
	 */
	@Override
	public Map getMap() {
		if (mapPage == null){
			return null;
		}
		return 	mapPage.getMap();
	}

	/* (non-Javadoc)
	 * @see org.locationtech.udig.project.ui.internal.MapPart#openContextMenu()
	 */
	@Override
	public void openContextMenu() {
		mapPage.openContextMenu();
		
	}

	/* (non-Javadoc)
	 * @see org.locationtech.udig.project.ui.internal.MapPart#setFont(org.eclipse.swt.widgets.Control)
	 */
	@Override
	public void setFont(Control textArea) {
		mapPage.setFont(textArea);
		
	}

	/* (non-Javadoc)
	 * @see org.locationtech.udig.project.ui.internal.MapPart#setSelectionProvider(org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider)
	 */
	@Override
	public void setSelectionProvider(
			IMapEditorSelectionProvider selectionProvider) {
		mapPage.setSelectionProvider(selectionProvider);
		
	}

	/* (non-Javadoc)
	 * @see org.locationtech.udig.project.ui.internal.MapPart#getStatusLineManager()
	 */
	@Override
	public IStatusLineManager getStatusLineManager() {
		return mapPage.getStatusLineManager();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object getAdapter(Class adaptee) {
		if (adaptee.isAssignableFrom(Map.class)) {
			return getMap();
		}
		return super.getAdapter(adaptee);
	}
	
}