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

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.ui.internal.MapPart;
import net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.ISurveyEventListener;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;

public class MissionEditor extends MultiPageEditorPart implements MapPart, IAdaptable{

	public static final String ID = "org.wcs.smart.er.ui.mission.MissionEditor"; //$NON-NLS-1$

	public static final DecimalFormat DISTANCE_FORMATTER = new DecimalFormat("#0.##"); //$NON-NLS-1$
	
	private Mission mission = null;
	
	private MissionSummaryPage summaryEditor;
	private MissionMapPage mapPage;
	
	private Projection[] projections;
	
//	private CombinedSelectionProvider selectionProvider = new CombinedSelectionProvider();
	
//	private ISurveyEventListener saveListener = new ISurveyEventListener() {
//		@Override
//		public void event(Object o) {
//			Patrol p = null;
//			if (source instanceof PatrolLegDay){
//				p = ((PatrolLegDay)source).getPatrolLeg().getPatrol();
//			}else if (source instanceof Patrol){
//				p = (Patrol)source;
//			}
//			if (p != null && p.equals(patrol)){
//				if (attributeChanged == PatrolEventManager.PATROL_DATES_LEG){
//					//reload patrol & update summary and day pages
//					Job j = new Job("load patrol"){ //$NON-NLS-1$
//						@Override
//						protected IStatus run(IProgressMonitor monitor) {
//							patrol = null;
//							getPatrol();
//							Session s = HibernateManager.openSession();
//							s.beginTransaction();
//							try{
//								s.update(patrol);
//								updateSummaryPage();
//							}finally{
//								s.close();
//							}
//							Display.getDefault().syncExec(new Runnable(){
//								@Override
//								public void run() {
//									createDayPages();
//									mapPage.refresh();
//								}});
//							return Status.OK_STATUS;
//						}					
//					};
//					j.setSystem(true);
//					j.schedule();
//				
//				}else{
//					updateSummaryPage();
//				}
//
//			}
//		}
//	};
	
	
	private ISurveyEventListener missionDeleteListener = new ISurveyEventListener() {
		@Override
		public void event(Object o) {
			Mission mission = (Mission)o;
			if (mission.equals(MissionEditor.this.mission)){
				MissionEditor.this.getEditorSite().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable(){

					@Override
					public void run() {
						MissionEditor.this.getEditorSite().getWorkbenchWindow()
							.getActivePage().closeEditor(MissionEditor.this, false);
					}});
			}
		}
	};
	
	private ISurveyEventListener missionModifiedListener = new ISurveyEventListener() {
		
		@Override
		public void event(Object o) {
			if (o instanceof Mission) {
				if ( Arrays.equals(((Mission)o).getUuid(), mission.getUuid())) {
					try {
						Job j = new Job("Reloading mission") {
							@Override
							protected IStatus run(IProgressMonitor monitor) {
								mission = null;
								getMission(); //to avoid nested transactions exception
								Display.getDefault().syncExec(new Runnable(){
									@Override
									public void run() {
										summaryEditor.initControls();
									}});
								return Status.OK_STATUS;
							}					
						};
						j.setSystem(true);
						j.schedule();
						j.join();
					} catch (InterruptedException e) {
						EcologicalRecordsPlugIn.log("Reload mission job interrupted", e);
					}

				}
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
			return "-" + lhrs + " hour " + " " + lmin + " min"; //$NON-NLS-1$ //$NON-NLS-2$
		}else{
			return lhrs + " hour "  + " " + lmin + " min"; //$NON-NLS-1$
		}
	}
	
	public MissionEditor() {
		super();

		SurveyEventHandler.getInstance().addListener(EventType.MISSION_DELETED, missionDeleteListener);
		SurveyEventHandler.getInstance().addListener(EventType.MISSION_MODIFIED, missionModifiedListener);
	}

	public Projection[] getAvailableProjections(){
		return this.projections;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		
		SurveyEventHandler.getInstance().removeListener(EventType.MISSION_DELETED, missionDeleteListener);
		SurveyEventHandler.getInstance().removeListener(EventType.MISSION_MODIFIED, missionModifiedListener);
	}
	
//	/**
//	 * 
//	 * @return null if the patrol can be editted, otherwise a string
//	 * that described reason why can't be edited.
//	 */
//	public String canEdit(){
//		return PatrolManager.getInstance().canEdit(patrol, ops);
//	}
	
	public Mission getMission(){
		if (this.mission == null){
			
			byte[] muuid = ((MissionEditorInput) getEditorInput()).getUuid();
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			
			this.mission = (Mission) session.load(Mission.class, muuid);
			//load mission items so don't have lazy loading issues later.
			this.mission.getWaypoints().size();
			this.mission.getTracks().size();

			List<Projection> tmp = HibernateManager.getCaProjectionList(session);
			this.projections = tmp.toArray(new Projection[tmp.size()]);
			
			session.getTransaction().commit();
			session.close();
		}
		return this.mission;
	}

	public void updatePartName(){
		MissionEditorInput input = ((MissionEditorInput) getEditorInput());
		super.setPartName("Mission: " + input.getName());
	}
	
	
	@Override
	protected void createPages() {
		MissionEditorInput input = ((MissionEditorInput) getEditorInput());
		super.setPartName(input.getName());
		showBusy(true);
		try {
			
			getMission(); //to avoid nested transactions exception

			summaryEditor = new MissionSummaryPage(this);
			int i = addPage(summaryEditor, getEditorInput());
			setPageText(i, "Summary");
			createDayPages();
			
			mapPage = new MissionMapPage(this);
			int mapIndex = addPage(mapPage, getEditorInput());
			setPageText(mapIndex, "Map");
			
//			getSite().setSelectionProvider(selectionProvider);
		} catch (final Throwable t) {
			getSite().getPage().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable(){
				@Override
						public void run() {
							try {
								MissionEditor.this.dispose();
								MissionEditor.this.getSite().getPage().closeEditor(MissionEditor.this, false);
								if (t instanceof SWTError&& t.getMessage().contains("No more handles")) { //$NON-NLS-1$
									EcologicalRecordsPlugIn.displayLog("Mission editor could not be created.  Please try closing existing open editors and try again." + t.getLocalizedMessage(), t);
								} else {
									EcologicalRecordsPlugIn.displayLog("Error occurred while loading editor." + t.getLocalizedMessage(), t);
								}
							} catch (Exception ex) {
								EcologicalRecordsPlugIn.log("Failure", ex); //$NON-NLS-1$
							}

						}
			});

		}finally{
			showBusy(false);
		}
	}
	
//	public CombinedSelectionProvider getSelectionProvider(){
//		return this.selectionProvider;
//	}
	
	public void updateSummaryPage(){
//		summaryEditor.refreshPatrolSummaryTable();
	}
	
	
	public void createDayPages() {
		try {
			int i = 0;
			while( i < getPageCount()) {
				if (getEditor(i) instanceof MissionDayPage) {
					removePage(i);
				}else{
					i++;
				}
			}
			int insertindex = 1;
			Calendar calStart = SmartUtils.convertDate(getMission().getStartDate());
			calStart.set(Calendar.HOUR, 0);
			calStart.set(Calendar.MINUTE, 0);
			calStart.set(Calendar.SECOND, 0);
			calStart.set(Calendar.MILLISECOND, 0);
			
			Calendar calEnd = SmartUtils.convertDate(getMission().getEndDate());
			
			while (calStart.before(calEnd) || calStart.equals(calEnd)) {
				MissionDayPageEditorInput input = new MissionDayPageEditorInput(SmartUtils.getDatePart(calStart.getTime(), false));
				MissionDayPage editor = new MissionDayPage(this);
				super.addPage(insertindex, editor, input);
				super.setPageText(insertindex, DateFormat.getDateInstance(DateFormat.MEDIUM).format(input.getDay()));
				insertindex++;
				calStart.add(Calendar.DAY_OF_MONTH, 1);
			}

		} catch (Exception ex) {
			EcologicalRecordsPlugIn.displayLog("Error loading editor", ex);
		}
		
	}
	

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	/**
	 * 
	 * @return null if mission can be edited, otherwise a string
	 * that described reason why it can't be edited.
	 */
	public String canEdit(){
		//TODO:
		return null;
	}
	
	
//	public void save(Mission mission){
//		savePatrolPart(patrol);
//	}
//	
//	public void save(PatrolLegDay patrolLegDay){
//		patrolLegDay.getTrack();
//		savePatrolPart(patrolLegDay);
//	}
	
	
//	public Job moveWaypoints(final Collection<SurveyWaypoint> toSave, final Collection<SurveyWaypoint> toDelete){
//		Job moveJob = new Job("Moving waypoints job") {
//			@Override
//			protected IStatus run(IProgressMonitor monitor) {
//				Session saveSession = HibernateManager
//						.openSession(new WaypointAttachmentInterceptor());
//				try{
//					saveSession.beginTransaction();
//					
//					/* delete waypoints */
//					for (SurveyWaypoint wp : toDelete) {
//						saveSession.delete(wp);
//						saveSession.delete(wp.getWaypoint());					
//					}
//					/* save waypoints */
//					for (SurveyWaypoint wp : toSave) {
//						wp.getWaypoint().setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
//						wp.getWaypoint().setConservationArea(SmartDB.getCurrentConservationArea());
//						saveSession.saveOrUpdate(wp.getWaypoint());
//						saveSession.saveOrUpdate(wp);
//						
//						// remove observations with no data
//						if (wp.getWaypoint().getObservations() != null) {
//							for (WaypointObservation wo : wp.getWaypoint().getObservations()) {
//								List<WaypointObservationAttribute> toDelete = new ArrayList<WaypointObservationAttribute>();
//								for (WaypointObservationAttribute att : wo.getAttributes()) {
//									if (!att.hasValue()) {
//										toDelete.add(att);
//									}
//								}
//								wo.getAttributes().removeAll(toDelete);
//							}
//						}
//					}
//					
//					saveSession.getTransaction().commit();
//				}catch (Exception ex){
//					if (saveSession.getTransaction().isActive()){
//						saveSession.getTransaction().rollback();
//					}
//					SmartPatrolPlugIn.displayLog(Messages.PatrolEditor_DeleteWaypointsError + ex.getLocalizedMessage(), ex);
//				}finally{
//					saveSession.close();
//				}
//				
//				/* fire events */
//				for (PatrolWaypoint wp : toDelete){
//					try{
//						PatrolEventManager.getInstance().waypointDeleted(wp);
//					}catch (Exception ex){
//						SmartPatrolPlugIn.log("Error firing event after waypoint delete.", ex); //$NON-NLS-1$
//					}
//				}
//				for (PatrolWaypoint wp : toSave){
//					try{
//						PatrolEventManager.getInstance().waypointModified(wp);
//					}catch (Exception ex){
//						SmartPatrolPlugIn.log("Error firing event after waypoint save.", ex); //$NON-NLS-1$
//					}
//				}
//				return Status.OK_STATUS;
//			}
//		};
//		moveJob.schedule();
//		return moveJob;
//		
//	}
	/**
	 * Saves the collection of waypoints.
	 * 
	 * @param waypoints
	 */
	public Job save(Collection<SurveyWaypoint> waypoints) {
		SaveWaypointJob saveWaypointJob = new SaveWaypointJob();
		saveWaypointJob.setWaypoints(waypoints);
		saveWaypointJob.schedule();
		try {
			saveWaypointJob.join();
		} catch (InterruptedException e) {
			EcologicalRecordsPlugIn.log("InterruptedException while saving waypoints", e);
		}
		return saveWaypointJob;
	}
	
	/**
	 * Deletes the collection of waypoints in a separate thread.
	 * 
	 * @param waypoints
	 * @return the job responsible for deleting waypoints
	 */
	public Job delete(final Collection<SurveyWaypoint> waypoints) {
		DeleteWaypointJob job = new DeleteWaypointJob();
		job.setWaypoints(waypoints);
		job.schedule();
		return job;
	}
	
	
	@Override
	public void doSave(IProgressMonitor monitor) {
				
	}

	@Override
	public void doSaveAs() {
	}

	/* (non-Javadoc)
	 * @see net.refractions.udig.project.ui.internal.MapPart#getMap()
	 */
	@Override
	public Map getMap() {
		if (mapPage == null){
			return null;
		}
		return 	mapPage.getMap();
	}

	/* (non-Javadoc)
	 * @see net.refractions.udig.project.ui.internal.MapPart#openContextMenu()
	 */
	@Override
	public void openContextMenu() {
		mapPage.openContextMenu();
		
	}

	/* (non-Javadoc)
	 * @see net.refractions.udig.project.ui.internal.MapPart#setFont(org.eclipse.swt.widgets.Control)
	 */
	@Override
	public void setFont(Control textArea) {
		mapPage.setFont(textArea);
		
	}

	/* (non-Javadoc)
	 * @see net.refractions.udig.project.ui.internal.MapPart#setSelectionProvider(net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider)
	 */
	@Override
	public void setSelectionProvider(
			IMapEditorSelectionProvider selectionProvider) {
		mapPage.setSelectionProvider(selectionProvider);
		
	}

	/* (non-Javadoc)
	 * @see net.refractions.udig.project.ui.internal.MapPart#getStatusLineManager()
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