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
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.common.control.CombinedSelectionProvider;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.ISurveyEventListener;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.SurveyPermissionManager;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.util.SharedUtils;

/**
 * Mission editor
 * 
 * @author Emily
 *
 */
public class MissionEditor extends MultiPageEditorPart implements MapPart, IAdaptable{
	
	private static final String HOUR_LABEL = Messages.MissionEditor_HourLabel;
	private static final String MINUTE_LABEL = Messages.MissionEditor_MinuteLabel;
	
	public static final String ID = "org.wcs.smart.er.ui.mission.MissionEditor"; //$NON-NLS-1$

	public static final DecimalFormat DISTANCE_FORMATTER = new DecimalFormat("#0.##"); //$NON-NLS-1$
	
	private Mission mission = null;
	private Date[] missionDates = null;
	
	private MissionSummaryPage summaryEditor;
	private MissionMapPage mapPage;
	
	private Projection[] projections;
	private Boolean trackDistanceDirection = null;
	private Boolean trackObserver = null;
	private ConfigurableModel configurableModel = null;
	private ObservationOptions options;
	private List<SamplingUnit> sUnits;
	private CombinedSelectionProvider selectionProvider = new CombinedSelectionProvider();
	
	private ISurveyEventListener missionDeleteListener = new ISurveyEventListener() {
		@Override
		public void event(Object o) {
			if ((o instanceof Mission &&  
			     mission.equals((Mission)o)) || 
			     (o instanceof SurveyDesign &&
			    mission.getSurvey().getSurveyDesign().equals((SurveyDesign)o))){
				MissionEditor.this.getEditorSite().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable(){

					@Override
					public void run() {
						MissionEditor.this.getEditorSite().getWorkbenchWindow()
							.getActivePage().closeEditor(MissionEditor.this, false);
					}});
			}
		}
	};
	
	/*
	 * if the survey is deleted also close the editor
	 */
	private ISurveyEventListener surveyDeleteListener = new ISurveyEventListener() {
		@Override
		public void event(Object o) {
			Survey survey = (Survey)o;
			if (survey.equals(MissionEditor.this.mission.getSurvey())){
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
		public void event(final Object o) {
			if ((o instanceof Mission && 
				( ((Mission)o).getUuid().equals(mission.getUuid()))) 
				|| ((o instanceof SurveyDesign 
					&& ((SurveyDesign)o).equals(mission.getSurvey().getSurveyDesign())))){
					try {
						Job j = new Job(Messages.MissionEditor_reloadJobName) {
							@Override
							protected IStatus run(IProgressMonitor monitor) {
								Date[] lastDates = missionDates;
								mission = null;
								getMission(); //to avoid nested transactions exception
								final boolean datesChanged = !SharedUtils.isSameDate(lastDates[0], missionDates[0])|| !SharedUtils.isSameDate(lastDates[1], missionDates[1]);
								
								getSite().getShell().getDisplay().syncExec(new Runnable(){
									@Override
									public void run() {
										setPartName(getMission().getId());
										summaryEditor.initControls();
										
										if (datesChanged){
											createDayPages();
										}else{
											for (int i = 0; i < getPageCount(); i ++){
												if (getEditor(i) instanceof MissionDayPage) {
													((MissionDayPage)getEditor(i)).refresh();
												}
											}								
										}
									}});
									mapPage.refresh();
								return Status.OK_STATUS;
							}					
						};
						j.setSystem(true);
						j.schedule();
						j.join();
					} catch (InterruptedException e) {
						EcologicalRecordsPlugIn.log("Reload mission job interrupted", e); //$NON-NLS-1$
					}

				}
			
		}
	};
	
	/**
	 * New mission editor; registers events
	 */
	public MissionEditor() {
		super();

		SurveyEventHandler.getInstance().addListener(EventType.MISSION_DELETED, missionDeleteListener);
		SurveyEventHandler.getInstance().addListener(EventType.SURVEY_DESIGN_DELETED, missionDeleteListener);
		SurveyEventHandler.getInstance().addListener(EventType.MISSION_MODIFIED, missionModifiedListener);
		SurveyEventHandler.getInstance().addListener(EventType.SURVEY_DELETED, surveyDeleteListener);
		SurveyEventHandler.getInstance().addListener(EventType.SURVEY_DESIGN_MODIFIED, missionModifiedListener);
	}

	/**
	 * 
	 * @return list of projections for current conservation area
	 */
	public Projection[] getAvailableProjections(){
		return this.projections;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		SurveyEventHandler.getInstance().removeListener(EventType.SURVEY_DESIGN_DELETED, missionDeleteListener);
		SurveyEventHandler.getInstance().removeListener(EventType.MISSION_DELETED, missionDeleteListener);
		SurveyEventHandler.getInstance().removeListener(EventType.MISSION_MODIFIED, missionModifiedListener);
		SurveyEventHandler.getInstance().removeListener(EventType.SURVEY_DELETED, surveyDeleteListener);
		SurveyEventHandler.getInstance().removeListener(EventType.SURVEY_DESIGN_MODIFIED, missionModifiedListener);
	}
	
	public ConfigurableModel getConfigurableModel(){
		return this.configurableModel;
	}
	/**
	 * Gets the mission from the database; reloading if necessary
	 * @return
	 */
	public Mission getMission(){
		if (this.mission == null){
			
			UUID muuid = ((MissionEditorInput) getEditorInput()).getUuid();
			Session session = HibernateManager.openSession();
			
			
			session.beginTransaction();
			try{
				this.mission = (Mission) session.load(Mission.class, muuid);
				missionDates = new Date[]{new Date(mission.getStartDate().getTime()), new Date(mission.getEndDate().getTime())};
				//load mission items so don't have lazy loading issues later.
				
				for (MissionDay md : mission.getMissionDays()){
					md.getTracks().size();
					try{
						for (SurveyWaypoint wp : md.getWaypoints()){
							ObservationHibernateManager.computeAttachmentLocations(wp.getWaypoint(), session);
						}
					}catch (Exception ex){
						EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
					}
				}

				this.trackDistanceDirection = mission.getSurvey().getSurveyDesign().getTrackDistanceDirection();
				this.trackObserver = mission.getSurvey().getSurveyDesign().getTrackObserver();
				this.configurableModel = mission.getSurvey().getSurveyDesign().getConfigurableModel();
				
				List<Projection> tmp = HibernateManager.getCaProjectionList(session);
				this.projections = tmp.toArray(new Projection[tmp.size()]);
			
				this.options = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(), session);
				if (options.getViewProjection() != null) {
					options.getViewProjection().getDefinition(); //load lazy items
				}
				
				this.sUnits = SurveyHibernateManager.getInstance().getSamplingUnits(mission.getSurvey().getSurveyDesign(), session, null);
				for (SamplingUnit s : sUnits){
					s.getId();
				}
				session.getTransaction().commit();
			}finally{
				session.close();
			}
		}
		return this.mission;
	}
	
	/**
	 * Editor selection provider.
	 * @return
	 */
	public CombinedSelectionProvider getSelectionProvider(){
		return this.selectionProvider;
	}
	
	/**
	 * Sampling unit associated with current design;
	 * @return
	 */
	public List<SamplingUnit> getSamplingUnits(){
		return sUnits;
	}
	/**
	 * 
	 * @return if the mission should record distance and direction
	 * properties of a waypoint.
	 */
	public boolean trackDistanceDirection(){
		if (this.trackDistanceDirection == null){
			getMission();
		}
		return this.trackDistanceDirection;
	}
	
	/**
	 * if the mission should record observer
	 * for observations
	 * 
	 * @return
	 */
	public boolean trackObserver(){
		if (this.trackObserver == null){
			getMission();
		}
		return this.trackObserver;
	}
	
	/**
	 * Updates the part name
	 */
	public void updatePartName(){
		MissionEditorInput input = ((MissionEditorInput) getEditorInput());
		super.setPartName(Messages.MissionEditor_MissionLabel + input.getName());
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
			setPageText(i, Messages.MissionEditor_SummaryPage);
			setPageImage(i, EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.MISSION_ICON));
			
			createDayPages();
			
			mapPage = new MissionMapPage(this);
			int mapIndex = addPage(mapPage, getEditorInput());
			setPageText(mapIndex, Messages.MissionEditor_MapPage);
			setPageImage(mapIndex, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.MAP_ICON));
			
			getSite().setSelectionProvider(selectionProvider);
		} catch (final Throwable t) {
			getSite().getPage().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable(){
				@Override
						public void run() {
							try {
								MissionEditor.this.dispose();
								MissionEditor.this.getSite().getPage().closeEditor(MissionEditor.this, false);
								if (t instanceof SWTError&& t.getMessage().contains("No more handles")) { //$NON-NLS-1$
									EcologicalRecordsPlugIn.displayLog(Messages.MissionEditor_EditorError + t.getLocalizedMessage(), t);
								} else {
									EcologicalRecordsPlugIn.displayLog(Messages.MissionEditor_EditorError2 + t.getLocalizedMessage(), t);
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

	private void createDayPages() {
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
			for (MissionDay md : getMission().getMissionDays()){
				MissionDayPageEditorInput input = new MissionDayPageEditorInput(md.getDate());
				MissionDayPage editor = new MissionDayPage(this);
				super.addPage(insertindex, editor, input);
				super.setPageText(insertindex, DateFormat.getDateInstance(DateFormat.MEDIUM).format(input.getDay()));
				insertindex++;
			}

		} catch (Exception ex) {
			EcologicalRecordsPlugIn.displayLog(Messages.MissionEditor_EditorError3, ex);
		}
		
	}
	

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	/**
	 * Current observation options as of time of editor load
	 * @return
	 */
	public ObservationOptions getObservationOptions(){
		return this.options;
	}
	
	/**
	 * 
	 * @return null if mission can be edited, otherwise a string
	 * that described reason why it can't be edited.
	 */
	public String canEdit(){
		return SurveyPermissionManager.INSTANCE.canEditMission(mission, this.options);
	}
	
	/**
	 * Creates a edit warning label.
	 * 
	 * @param editError
	 * @param parent
	 * @param toolkit
	 */
	public void createEditWarning(String editError, Composite parent, FormToolkit toolkit){
		Composite warning = toolkit.createComposite(parent);
		warning.setLayout(new GridLayout(2, false));
		Label lblImage = toolkit.createLabel(warning, null, SWT.NONE);
		Image x = getSite().getWorkbenchWindow().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
		lblImage.setImage(x);
		Label lblWarning = toolkit.createLabel(warning, "", SWT.NONE); //$NON-NLS-1$
		lblWarning.setText(MessageFormat.format(Messages.MissionEditor_EditorError4, new Object[]{editError})) ;
	}
	
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
			EcologicalRecordsPlugIn.log("InterruptedException while saving waypoints", e); //$NON-NLS-1$
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
	
}