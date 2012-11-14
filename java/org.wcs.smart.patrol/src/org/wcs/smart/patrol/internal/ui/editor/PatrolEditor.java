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
package org.wcs.smart.patrol.internal.ui.editor;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.ui.internal.MapPart;
import net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider;

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
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolEventManager.EventType;
import org.wcs.smart.patrol.PatrolEventManager.IPatrolEventListener;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolOptions;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointAttachmentInterceptor;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;
import org.wcs.smart.util.SmartUtils;

/**
 * The patrol editor.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolEditor extends MultiPageEditorPart implements MapPart, IAdaptable{

	public static final String ID = "org.wcs.smart.patrol.ui.PatrolEditor"; //$NON-NLS-1$

	public static final DecimalFormat REST_TIME_FORMATTER = new DecimalFormat("00.00");
	public static final DecimalFormat DISTANCE_FORMATTER = new DecimalFormat("#0.##");
	
	private Patrol patrol = null;
	private PatrolOptions ops = null;
	
	private PatrolSummaryEditor summaryEditor;
	private PatrolMapPageEditor mapPage;
	
	private Projection[] projections;
	
	private IPatrolEventListener saveListener = new IPatrolEventListener() {
		@Override
		public void eventFired(int attributeChanged, Object source) {
			Patrol p = null;
			if (source instanceof PatrolLegDay){
				p = ((PatrolLegDay)source).getPatrolLeg().getPatrol();
			}else if (source instanceof Patrol){
				p = (Patrol)source;
			}
			if (p != null && p.equals(patrol)){
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						updateSummaryPage();
					}
				});
			}
		}
	};
	
	
	private IPatrolEventListener patrolDeleteListener = new IPatrolEventListener() {
		@Override
		public void eventFired(int attributeChanged, Object source) {
			if ( ((Patrol)source ).equals(PatrolEditor.this.patrol)  ){
				//close this editor
				PatrolEditor.this.getEditorSite().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable(){
					@Override
					public void run() {
						PatrolEditor.this.getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(PatrolEditor.this, false);					
					}});
			}
		}
	};
	
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
		super.dispose();
		PatrolEventManager.getInstance().removeListener(EventType.PATROL_SAVED, saveListener);
		PatrolEventManager.getInstance().removeListener(EventType.PATROL_DELETED, patrolDeleteListener);

	}

	public PatrolOptions getOptions(){
		return this.ops;
	}
	
	/**
	 * 
	 * @return null if the patrol can be editted, otherwise a string
	 * that described reason why can't be edited.
	 */
	public String canEdit(){
		
		//analyst users can never edit
		if (SmartDB.getCurrentEmployee().getSmartUserLevel() == Employee.SmartUserLevel.ANALYST){
			return "Insufficient User Privledges";
		}
		
		if (ops.getEditTime() == null || ops.getEditTime() < 0){
			return null;
		}else if (patrol.getStartDate() == null){
			return null;
		}else if (SmartDB.getCurrentEmployee().getSmartUserLevel() == Employee.SmartUserLevel.DATA_ENTRY){
			Date d = new Date();
			d.setTime( d.getTime() - (long)ops.getEditTime() * 24 * 60 * 60 * 1000 );
			if (patrol.getStartDate().after(d)){
				return null;
			}else{
				return "Patrol is older than " + ops.getEditTime() + " days" ;
			}
		}else{
			return null;
		}
	}
	
	public Patrol getPatrol(){
		if (this.patrol == null){
			
			byte[] puuid = ((PatrolEditorInput) getEditorInput()).getUuid();
			Session session = HibernateManager.openSession();
			//load patrol items so don't have lazy loading issues later.
			session.beginTransaction();
			this.patrol = (Patrol) session.load(Patrol.class, puuid);
			this.patrol.getLegs().size();
			this.patrol.getPatrolDatastorePath();
			List<Projection> tmp = HibernateManager.getCaProjectinList(session);
			this.projections = tmp.toArray(new Projection[tmp.size()]);
			session.getTransaction().commit();
			ops = PatrolHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(),session);
			session.close();
		}
		return this.patrol;
	}

	public void updatePartName(){
		PatrolEditorInput input = ((PatrolEditorInput) getEditorInput());
		super.setPartName("Patrol " + input.getPatrolId());
		PatrolEventManager manager = PatrolEventManager.getInstance();
		manager.patrolChanged(PatrolEventManager.PATROL_ID, patrol);
	}
	
	@Override
	protected void createPages() {
		PatrolEditorInput input = ((PatrolEditorInput) getEditorInput());
		super.setPartName("Patrol " + input.getPatrolId());
		showBusy(true);
		try {
			
			getPatrol();
			
			summaryEditor = new PatrolSummaryEditor(this);
			int i = addPage(summaryEditor, getEditorInput());
			setPageText(i, "Summary");
			createDayPages();
			
			mapPage = new PatrolMapPageEditor(PatrolEditor.this);
			int mapIndex = addPage(mapPage, getEditorInput());
			setPageText(mapIndex, "Map");
			showBusy(false);
		} catch (final Throwable t) {
			PatrolEditor.this.getSite().getPage().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable(){
				@Override
						public void run() {
							try {
								PatrolEditor.this.dispose();
								PatrolEditor.this.getSite().getPage().closeEditor(PatrolEditor.this, false);
								if (t instanceof SWTError&& t.getMessage().contains("No more handles")) {
									SmartPatrolPlugIn.displayLog("Patrol editor could not be created.  Please try closing existing open editors and try again.\n" + t.getMessage(), t);
								} else {
									SmartPatrolPlugIn.displayLog("Error occurred while loading editor. "+ t.getMessage(), t);
								}
							} catch (Exception ex) {
								//TODO: Should we fail the program here??
								SmartPatrolPlugIn.log("Failure",ex);
							}

						}
			});

		}
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
			GregorianCalendar calStart = SmartUtils.convertDate(getPatrol().getStartDate());
			calStart = new GregorianCalendar(calStart.get(Calendar.YEAR),calStart.get(Calendar.MONTH),calStart.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
			GregorianCalendar calEnd = SmartUtils.convertDate(getPatrol().getEndDate());
			
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
			SmartPatrolPlugIn.displayLog("Error loading editor", ex);
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
	
	private SaveWaypointJob saveWaypointJob = new SaveWaypointJob();
	
	/**
	 * Saves the collection of waypoints.
	 * 
	 * @param waypoints
	 */
	public void save(Collection<Waypoint> waypoints) {
		saveWaypointJob.setWaypoints(waypoints);
		saveWaypointJob.schedule();
	}
	
	public void delete(final Collection<Waypoint> waypoints) {
		Job saveJob = new Job("Save Patrol Job") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session saveSession = HibernateManager
						.openSession(new WaypointAttachmentInterceptor());
				try{
					saveSession.beginTransaction();
					for (Waypoint wp : waypoints) {
						saveSession.delete(wp);
					}
					saveSession.getTransaction().commit();
				}catch (Exception ex){
					if (saveSession.getTransaction().isActive()){
						saveSession.getTransaction().rollback();
					}
					SmartPatrolPlugIn.displayLog("Error deleting patrol waypoints.  You should close the patrol, re-open it and make your changes again.\n\n" + ex.getMessage(), ex);
				}finally{
					
					saveSession.close();
				}
				
				return Status.OK_STATUS;
			}
		};
		saveJob.schedule();
	}
	
	private void savePatrolPart(final Object object){
		//update all the patrol values
		for (int i = 0; i < getPageCount(); i ++){
			getEditor(i).doSave(new NullProgressMonitor());
		}
		Job saveJob = new Job("Save Patrol Job") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session saveSession = HibernateManager
						.openSession(new WaypointAttachmentInterceptor());
				try{
					saveSession.beginTransaction();
					if (object instanceof Patrol) {
						if (((Patrol) object).getId() == null) {
							String id = PatrolHibernateManager.generatePatrolId(
								((Patrol) object), saveSession);
							((Patrol) object).setId(id);
						}
					}
				
					saveSession.saveOrUpdate(object);
					saveSession.getTransaction().commit();
				}catch (Exception ex){
					if (saveSession.getTransaction().isActive()){
						saveSession.getTransaction().rollback();
					}
					SmartPatrolPlugIn.displayLog("Error saving patrol.  You should close the patrol, re-open it and make your changes again.\n\n" + ex.getMessage(), ex);
				}finally{
					saveSession.close();
				}
				
				PatrolEventManager.getInstance().patrolSaved(patrol);
				return Status.OK_STATUS;
			}};
			saveJob.schedule();
	}
	
	
	@Override
	public void doSave(IProgressMonitor monitor) {

		//update all the patrol values
		for (int i = 0; i < getPageCount(); i ++){
			getEditor(i).doSave(monitor);
		}
		Job saveJob = new Job("Save Patrol Job") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session saveSession = HibernateManager.openSession(new WaypointAttachmentInterceptor());
				try{
					if (PatrolHibernateManager.savePatrol(patrol, saveSession, false)){
					//saved okay
						PatrolEventManager.getInstance().patrolSaved(patrol);
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
	
	
	
	class SaveWaypointJob extends Job {

		private Collection<Waypoint> waypoints;

		public SaveWaypointJob() {
			super("Save Waypoints");
		}

		public void setWaypoints(Collection<Waypoint> points) {
			synchronized (this) {
				this.waypoints = points;
			}

		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			ArrayList<Waypoint> pnts = new ArrayList<Waypoint>();
			synchronized (this) {
				pnts.addAll(waypoints);
			}
			Session saveSession = HibernateManager
					.openSession(new WaypointAttachmentInterceptor());
			try {
				saveSession.beginTransaction();
				for (Waypoint wp : pnts) {
					saveSession.saveOrUpdate(wp);
					saveSession.flush();
					// remove observations with no data
					if (wp.getObservations() != null) {
						for (WaypointObservation wo : wp.getObservations()) {
							List<WaypointObservationAttribute> toDelete = new ArrayList<WaypointObservationAttribute>();
							for (WaypointObservationAttribute att : wo
									.getAttributes()) {
								if (!att.hasValue()) {
									toDelete.add(att);
								}
							}
							wo.getAttributes().removeAll(toDelete);
						}
					}
				}
				saveSession.getTransaction().commit();
			} catch (Exception ex) {
				if (saveSession.getTransaction().isActive()) {
					saveSession.getTransaction().rollback();
				}
				SmartPatrolPlugIn
						.displayLog(
								"Could not save changes.  Please close patrol and re-open it before proceeding. \n\n"
										+ ex.getMessage(), ex);
			} finally {
				saveSession.close();
			}
			return Status.OK_STATUS;
		}
	}
}