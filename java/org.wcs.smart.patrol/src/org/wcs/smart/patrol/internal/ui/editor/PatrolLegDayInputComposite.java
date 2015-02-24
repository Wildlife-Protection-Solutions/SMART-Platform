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

import java.lang.reflect.InvocationTargetException;
import java.sql.Time;
import java.text.Collator;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellHighlighter;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.common.celleditor.DoubleCellEditor;
import org.wcs.smart.common.celleditor.IntegerCellEditor;
import org.wcs.smart.common.celleditor.TimeCellEditor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.common.importwp.GPSDataImport;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.ui.AttachmentCellEditor;
import org.wcs.smart.observation.ui.ObservationCellEditor;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolEventManager.EventType;
import org.wcs.smart.patrol.PatrolEventManager.IPatrolEventListener;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.importwp.PatrolImportGpsDataWizard;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.ui.PatrolEditor;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite for editing patrol leg days data.  This includes modifying
 * the date/time; rest minutes; tracks and waypoints.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolLegDayInputComposite {

	private static final String LOAD_WIZARD_PROGRESS_MSG = Messages.PatrolLegDayInputComposite_Progress_LoadingImportWizard;
	private static final String SHOW_WIZARD_PROGRESS_MSG = Messages.PatrolLegDayInputComposite_Progress_DisplayingImportWizard;
	
	private DateTime dtStartTime;
	private DateTime dtEndTime;
	private Text restMinutes;
	private Label lblTotalHours;

	private TableViewer observationTable;
	private ObservationOptions observationOptions;
	
	private WizardDialog dialog = null;
	private PatrolDayEditor editor;
	private PatrolLegDay patrolLegDate;
	
	private Button btnAddWaypoint;
	private Button btnDeleteWaypoint;
	private Button btnMoveWaypoint;
	
	private DoubleCellEditor doubleCellEditor;
	private DoubleCellEditor nullableDoubleCellEditor;
	private IntegerCellEditor integerCellEditor;
	private TimeCellEditor timeEditor;
	private AttachmentCellEditor attachmentEditor;
	private TextCellEditor commentEditor;
	private ObservationCellEditor observationEditor;
	
	private WaypointSorter waypointSorter;
	
	private HashMap<OtColumn, TableViewerColumn> observationTableColumns;
	private Hyperlink viewTrackPoints;
	private Hyperlink importTrack;
	private Text txtDistance;
	
	private Font okayFont;
	private Font errorFont;
	
	private IPatrolEventListener trackListener = new IPatrolEventListener() {
		@Override
		public void eventFired(int attributeChanged, Object source) {
			if (attributeChanged == PatrolEventManager.PATROL_TRACKS && source.equals(patrolLegDate)){
				updateDistance();
			}
			
		}
	};
	private IPatrolEventListener waypointListener = new IPatrolEventListener() {
		@Override
		public void eventFired(int attributeChanged, Object source) {
			if (attributeChanged == PatrolEventManager.PATROL_WAYPOINTS && source.equals(patrolLegDate)){
				refreshObservationTable();
			}
			
		}
	};
	private Composite mainComposite;
	private Hyperlink lblImportWaypoints;
	
	
	protected enum OtColumn {
		ID(Messages.PatrolLegDayInputComposite_WaypointID_ColumnHeader, 1), EAST(Messages.PatrolLegDayInputComposite_Longitude_ColumnHeader, 2), NORTH(Messages.PatrolLegDayInputComposite_Latitude_ColumnHeader, 2), TIME(
				Messages.PatrolLegDayInputComposite_Time_ColumnHeader, 2), DIRECTION(Messages.PatrolLegDayInputComposite_Direction_ColumnHeader, 1), DISTANCE(Messages.PatrolLegDayInputComposite_Distance_ColumnHeader, 1), OBSERVATION(
				Messages.PatrolLegDayInputComposite_Observation_ColumnHeader, 4), COMMENT(Messages.PatrolLegDayInputComposite_Comment_ColumnHeader, 3), ATTACHMENTS(
				Messages.PatrolLegDayInputComposite_Attachment_ColumnHeader, 3);

		protected String guiName;
		protected int weight;

		private OtColumn(String name, int weight) {
			this.guiName = name;
			this.weight = weight;
		}
	}

	
	public PatrolLegDayInputComposite(PatrolDayEditor editor, ObservationOptions observationOptions){
		this.editor = editor;
		this.observationOptions = observationOptions;
	}

	public void refreshObservationTable(){
		observationTable.refresh();
	}
	
	
	public void setData(PatrolLegDay data) {
		this.patrolLegDate = data;
		
		Calendar cal = Calendar.getInstance();
		if (data.getStartTime() != null) {
			cal.setTime(data.getStartTime());
			dtStartTime.setTime(cal.get(Calendar.HOUR_OF_DAY),
					cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
		}else{
			dtStartTime.setTime(0,0,0);
		}
		if (data.getEndTime() != null) {
			cal.setTime(data.getEndTime());
			dtEndTime.setTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
					cal.get(Calendar.SECOND));
		}else{
			dtEndTime.setTime(23,59,59);
		}
		if (data.getRestMinutes() == null) {
			restMinutes.setText("0"); //$NON-NLS-1$
		} else {
			restMinutes.setText(String.valueOf(data.getRestMinutes()));
		}

		this.lblTotalHours.setText(String.valueOf(data.getHoursWorked()));

		if (data.getWaypoints() == null){
			data.setWaypoints(new ArrayList<PatrolWaypoint>());
		}
		List<PatrolWaypoint> inputList = new ArrayList<PatrolWaypoint>(data.getWaypoints());
		observationTable.setInput(inputList);
		observationTable.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (editor.getPatrolEditor().canEdit() == null){
					boolean enabled = !((IStructuredSelection)observationTable.getSelection()).isEmpty();
					btnDeleteWaypoint.setEnabled(enabled);
					
					if (patrolLegDate.getPatrolLeg().getPatrol().getLegs().size() > 1 || patrolLegDate.getPatrolLeg().getPatrolLegDays().size() > 1){
						btnMoveWaypoint.setEnabled(enabled);	
					}else{
						btnMoveWaypoint.setEnabled(false);
					}
				}
			}
		});
		
		this.viewTrackPoints.setEnabled( this.patrolLegDate.getTrack() != null );
				
		updateTotalHours();
		updateDistance();
		
		btnMoveWaypoint.setEnabled(false);
		btnDeleteWaypoint.setEnabled(false);
		
		if (editor.getPatrolEditor().canEdit() != null){
			dtEndTime.setEnabled(false);
			dtStartTime.setEnabled(false);
			restMinutes.setEditable(false);
			restMinutes.setEnabled(false);
			
			btnAddWaypoint.setVisible(false);
			btnDeleteWaypoint.setVisible(false);
			btnMoveWaypoint.setVisible(false);
			
			importTrack.setVisible(false);
			lblImportWaypoints.setVisible(false);
			
		}
	}

	public Composite createComposite(Composite parent, FormToolkit toolkit) {
		
		mainComposite = toolkit.createComposite(parent);
		mainComposite.setLayout(new GridLayout(1, false));
		mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite timeInfo = toolkit.createComposite(mainComposite);
		timeInfo.setLayout(new GridLayout(4, false));
		((GridLayout) timeInfo.getLayout()).horizontalSpacing = 15;
		 ((GridLayout)timeInfo.getLayout()).marginWidth = 0;
		 ((GridLayout)timeInfo.getLayout()).marginLeft = 5;
		 ((GridLayout)timeInfo.getLayout()).marginHeight = 5;
		timeInfo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Composite c = toolkit.createComposite(timeInfo);
		c.setLayout(new GridLayout(2, false));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		toolkit.createLabel(c, Messages.PatrolLegDayInputComposite_StartTimeLabel);
		dtStartTime = new DateTime(c, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
		toolkit.adapt(dtStartTime);
		dtStartTime.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateTotalHours();
			}
			
		});
		dtStartTime.addFocusListener(new FocusAdapter() {			
			@Override
			public void focusLost(FocusEvent e) {
				if (timeEqual(SmartUtils.getTime(dtStartTime).getTime(), patrolLegDate.getStartTime().getTime())){
					//no changes made
					return;
				}
				editor.getPatrolEditor().save(patrolLegDate);
				PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_DATES_LEG, patrolLegDate);
			}
		});

		c = toolkit.createComposite(timeInfo);
		c.setLayout(new GridLayout(2, false));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		toolkit.createLabel(c, Messages.PatrolLegDayInputComposite_EndTimeLabel);
		dtEndTime = new DateTime(c, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
		toolkit.adapt(dtEndTime);
		dtEndTime.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateTotalHours();
			}
			
		});
		dtEndTime.addFocusListener(new FocusAdapter() {			
			@Override
			public void focusLost(FocusEvent e) {
				if (timeEqual(SmartUtils.getTime(dtEndTime).getTime(), patrolLegDate.getEndTime().getTime())){
					//no changes made
					return;
				}
				editor.getPatrolEditor().save(patrolLegDate);
				PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_DATES_LEG, patrolLegDate);
			}
		});

		
		c = toolkit.createComposite(timeInfo);
		c.setLayout(new GridLayout(2, false));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		toolkit.createLabel(c, Messages.PatrolLegDayInputComposite_RestMinutesLabel);
		restMinutes = toolkit.createText(c, "0", SWT.BORDER); //$NON-NLS-1$
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd.widthHint = 30;
		restMinutes.setLayoutData(gd);
		restMinutes.addFocusListener(new FocusListener() {
			private int oldValue; 
			
			@Override
			public void focusLost(FocusEvent e) {
				try{
					int x = Integer.parseInt(restMinutes.getText());
					if (patrolLegDate.getRestMinutes() != null && x == patrolLegDate.getRestMinutes()){
						return;
					}
					if (x < 0){
						throw new Exception("Rest minutes cannot be negative."); //$NON-NLS-1$
					}
				}catch (Exception ex){
					restMinutes.setText(String.valueOf(oldValue));
					MessageDialog.openWarning(restMinutes.getShell(), Messages.PatrolLegDayInputComposite_Error_DialogTitle, Messages.PatrolLegDayInputComposite_InvalidRestMinutes_DialogMessage1);
					restMinutes.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							restMinutes.setFocus();
						}
					});
					
				}
				updateTotalHours();
				editor.getPatrolEditor().save(patrolLegDate);
				PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_DATES_LEG, patrolLegDate);
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				oldValue = Integer.parseInt(restMinutes.getText());
			}
		});
		
		
		c = toolkit.createComposite(timeInfo);
		c.setLayout(new GridLayout(2, false));
		c.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		toolkit.createLabel(c, Messages.PatrolLegDayInputComposite_TotalHoursPatrolled_Label);
		lblTotalHours = toolkit.createLabel(c, Messages.PatrolLegDayInputComposite_InvalidTotalHoursPatrolled);
		okayFont = lblTotalHours.getFont();
		
		FontData fd = okayFont.getFontData()[0];
		fd.setStyle(SWT.BOLD);
		errorFont = new Font(lblTotalHours.getDisplay(), fd);
		
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.widthHint = 30;
		lblTotalHours.setLayoutData(gd);

		Composite trackComp = toolkit.createComposite(mainComposite);
		trackComp.setLayout(new GridLayout(4, false));
		
		
		toolkit.createLabel(trackComp, Messages.PatrolLegDayInputComposite_DistanceTravelledLabel);
		txtDistance = toolkit.createText(trackComp, "0", SWT.NONE); //$NON-NLS-1$
		txtDistance.setEditable(false);
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd.widthHint = 50;
		txtDistance.setLayoutData(gd);
		
		importTrack = toolkit.createHyperlink(trackComp, Messages.PatrolLegDayInputComposite_SetTrackLabel, SWT.NONE);
		importTrack.addHyperlinkListener(new HyperlinkAdapter(){
			public void linkActivated(HyperlinkEvent e) {
				showImportTrackWizard();
				viewTrackPoints.setEnabled(patrolLegDate.getTrack() != null);
			}
		});
		
		viewTrackPoints = toolkit.createHyperlink(trackComp, Messages.PatrolLegDayInputComposite_ViewTrackLabel, SWT.NONE);
		viewTrackPoints.setEnabled(false);
		viewTrackPoints.addHyperlinkListener(new HyperlinkAdapter(){
			public void linkActivated(HyperlinkEvent e) {
				//showImportWizard();
				PatrolTrackPointDialog tpd = new PatrolTrackPointDialog(viewTrackPoints.getShell(), patrolLegDate.getTrack());
				tpd.open();
				ApplicationGIS.getToolManager().setCurrentEditor(editor.getPatrolEditor());
			}
		});
		
		
		
		Composite observationHcomp = toolkit.createComposite(mainComposite);
		observationHcomp.setLayout(new GridLayout(2, false));
		toolkit.createLabel(observationHcomp, Messages.PatrolLegDayInputComposite_ObservationsWaypointsLabel);
		lblImportWaypoints = toolkit.createHyperlink(observationHcomp, Messages.PatrolLegDayInputComposite_ImportWaypointsLabel, SWT.NONE);
		lblImportWaypoints.addHyperlinkListener(new HyperlinkAdapter(){
			public void linkActivated(HyperlinkEvent e) {
				showImportWaypointWizard();
			}
		});

		observationTable = new TableViewer(mainComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		editor.getPatrolEditor().getSelectionProvider().addSelectionProvider(observationTable);
		toolkit.adapt(observationTable.getTable());
		setupObservationTable();
		observationTable.getTable().addPaintListener(new PaintListener() {
			
			boolean called = false;
			@Override
			public void paintControl(PaintEvent e) {
				if (called) return;
				called = true;
				resize();
				
			}
		});
		
		Composite buttonComp = toolkit.createComposite(mainComposite);
		buttonComp.setLayout(new GridLayout(3, false));
		btnAddWaypoint = toolkit.createButton(buttonComp, Messages.PatrolLegDayInputComposite_AddWaypoint_Button, SWT.PUSH);
		btnAddWaypoint.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addWaypoint();
			}
		});
		
		
		btnDeleteWaypoint = toolkit.createButton(buttonComp, Messages.PatrolLegDayInputComposite_DeleteWaypoint_Button, SWT.PUSH);
		btnDeleteWaypoint.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				deleteSelectedWaypoints();
			}
		});
		
		btnMoveWaypoint = toolkit.createButton(buttonComp, Messages.PatrolLegDayInputComposite_MoveWaypoint_Button, SWT.PUSH);
		btnMoveWaypoint.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				moveSelectedWaypoints();
			}
		});
		
		PatrolEventManager.getInstance().addListener(EventType.PATROL_MODIFIED, trackListener);
		PatrolEventManager.getInstance().addListener(EventType.PATROL_MODIFIED, waypointListener);
		updateTotalHours();
		return mainComposite;
	}
	
	private boolean timeEqual(long t1, long t2){
		Calendar c1 = Calendar.getInstance();
		c1.setTimeInMillis(t1);
		Calendar c2 = Calendar.getInstance();
		c2.setTimeInMillis(t2);
		
		int[] fields = new int[]{Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND};
		
		for (int i = 0; i < fields.length; i ++){
			if (c1.get(fields[i]) != c2.get(fields[i])){
				return false;
			}
		}
		return true;
	}
	
	
	public void dispose(){
		PatrolEventManager.getInstance().removeListener(EventType.PATROL_MODIFIED, trackListener);
		PatrolEventManager.getInstance().removeListener(EventType.PATROL_MODIFIED, waypointListener);
		
		doubleCellEditor.dispose();
		nullableDoubleCellEditor.dispose();
		integerCellEditor.dispose();
		timeEditor.dispose();
		attachmentEditor.dispose();
		commentEditor.dispose();
		observationEditor.dispose();
		doubleCellEditor = null;
		nullableDoubleCellEditor = null;
		integerCellEditor = null;
		timeEditor = null;
		attachmentEditor = null;
		commentEditor = null;
		observationEditor = null;
		
		if (errorFont != null && !errorFont.isDisposed()){
			errorFont.dispose();
		}
		mainComposite.dispose();
		mainComposite = null;
		
		
	}
	
	private void moveSelectedWaypoints(){
		MoveWaypointDialog dialog = new MoveWaypointDialog(mainComposite.getShell(), patrolLegDate.getPatrolLeg().getPatrol());
		if (dialog.open() != Window.OK ){
			return ;
		}
		ArrayList<PatrolWaypoint> deleted = new ArrayList<PatrolWaypoint>();
		ArrayList<PatrolWaypoint> added = new ArrayList<PatrolWaypoint>();
		
		final PatrolLegDay moveTo = dialog.getMoveToPosition();
		Session session = HibernateManager.openSession();
		try {
			IStructuredSelection selection = ((IStructuredSelection) observationTable.getSelection());
			for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
				PatrolWaypoint w = (PatrolWaypoint) iterator.next();
				
				Waypoint toClone = w.getWaypoint();
				if (toClone.getUuid() != null){
					toClone = (Waypoint)session.merge(toClone);
				}
				
				Waypoint cloned = toClone.clone();
				
				if (patrolLegDate.getWaypoints().remove(w)) {
					w.setPatrolLegDay(null);
					
					PatrolWaypoint pw = new PatrolWaypoint();
					pw.setWaypoint(cloned);
					pw.setPatrolLegDay(moveTo);
					moveTo.getWaypoints().add(pw);
					
					deleted.add(w);
					added.add(pw);
				}
				
				//ensure minimum is loaded for the patrol mapping service which assumes
				//to a minimum that this information is already loaded 
				if (cloned.getObservations() != null && cloned.getObservations().size() > 0){
					for (WaypointObservation ob : cloned.getObservations()){
						ob.getCategory().getName();
					}
				}
				
			}
		} finally {
			session.close();
		}
		
		Job j = editor.getPatrolEditor().moveWaypoints(added, deleted);
		j.addJobChangeListener(new JobChangeAdapter() {
			
			@Override
			public void done(IJobChangeEvent event) {
				mainComposite.getDisplay().syncExec(new Runnable(){
					@Override
					public void run() {
						PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_WAYPOINTS, moveTo);
						PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_WAYPOINTS, patrolLegDate);
					}});
			}

		});
		
		
		
	}
	
	
	private void deleteSelectedWaypoints() {
		boolean doDel = MessageDialog.openConfirm(mainComposite.getShell(), Messages.PatrolLegDayInputComposite_DeleteWaypoint_DialogTitle, Messages.PatrolLegDayInputComposite_DeleteWaypoint_DialogMessage);
		if (!doDel){
			return;
		}
		IStructuredSelection selection = ((IStructuredSelection)observationTable.getSelection());
		ArrayList<PatrolWaypoint> deleted = new ArrayList<PatrolWaypoint>();
		
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			PatrolWaypoint w = (PatrolWaypoint) iterator.next();
			if (patrolLegDate.getWaypoints().remove(w)){
				w.setPatrolLegDay(null);
				deleted.add(w);
			}	
		}
		
		//delete waypoints
		Job j = editor.getPatrolEditor().delete(deleted);
		j.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				//once the job is completed we can fire this event
				mainComposite.getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_WAYPOINTS, patrolLegDate);			}
				});
			}
		});
		
		
	}
	
	private void resize(){
		if (observationTableColumns == null){
			return ;
		}
		
		GC gc = new GC(observationTable.getTable().getDisplay());
		gc.setFont(observationTable.getTable().getFont());
		
		for (Iterator<Entry<OtColumn, TableViewerColumn>> iterator = observationTableColumns.entrySet().iterator(); iterator.hasNext();) {
			Entry<OtColumn, TableViewerColumn> type = iterator.next();
			int maxWidth = getMaximumWidth(gc, type.getKey());
			type.getValue().getColumn().setWidth(maxWidth);
		}
		gc.dispose();
	}
	
	/**
	 * This convenience method is used to determine an appropriate width for
	 * the column based on the collection of event objects. The returned
	 * value is the maximum width (in pixels) of the text the receiver
	 * associates with each of the events. The events are provided as
	 * Object[] because converting them to {@link UsageDataEventWrapper}[]
	 * would be an unnecessary expense.
	 * 
	 * @param gc
	 *            a {@link GC} loaded with the font used to display the
	 *            events.
	 * @param events
	 *            an array of {@link UsageDataEventWrapper} instances.
	 * @return the width of the widest event
	 */
	private int getMaximumWidth(GC gc, OtColumn column){ //, Object[] events) {
		int width = 0;
		Point extent = gc.textExtent(column.guiName);
		width = extent.x;
		for (Iterator<PatrolWaypoint> iterator = PatrolLegDayInputComposite.this.patrolLegDate.getWaypoints().iterator(); iterator.hasNext();) {
			PatrolWaypoint e = iterator.next();
			String str = getWaypointValueAsString(e, column);
			
			if (str != null){
				int tmp = gc.textExtent(str).x;
				if ( tmp > width){
					width = tmp;
				}
			}
		}
		width = Math.min(200, width);
		return width + 20;
	}
	
	private void updateTotalHours(){
		double d = Double.parseDouble(this.restMinutes.getText());
		double time = SmartUtils.getTime(dtEndTime).getTime() - SmartUtils.getTime(dtStartTime).getTime() - d * 60 * 1000;
		time = time / (1000 * 60 * 60);
		//lblTotalHours.setText(PatrolEditor.REST_TIME_FORMATTER.format(time));
		lblTotalHours.setText(PatrolEditor.formatTimeRange(time));
		if (time < 0){
			lblTotalHours.setFont(errorFont);
			lblTotalHours.setForeground(lblTotalHours.getDisplay().getSystemColor(SWT.COLOR_RED));
			lblTotalHours.setToolTipText(Messages.PatrolLegDayInputComposite_Error_StartTimeError_Tooltip);
		}else{
			lblTotalHours.setFont(okayFont);
			lblTotalHours.setForeground(lblTotalHours.getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
			lblTotalHours.setToolTipText(null);
		}
		
	}
	
	public void updateDistance(){
		if (this.patrolLegDate.getTrack() == null || this.patrolLegDate.getTrack().getDistance() == null){
			this.viewTrackPoints.setEnabled(false);
			this.txtDistance.setText("0"); //$NON-NLS-1$
		}else{
			this.txtDistance.setText(PatrolEditor.DISTANCE_FORMATTER.format(this.patrolLegDate.getTrack().getDistance()));
			this.viewTrackPoints.setEnabled(true);
		}
	}
	private void setupObservationTable() {
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = observationTable.getTable().computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		observationTable.getTable().setLayoutData(gd);
		observationTable.getTable().setLinesVisible(true);
		observationTable.getTable().setHeaderVisible(true);
		observationTable.setContentProvider(ArrayContentProvider.getInstance());
		
		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(observationTable, new FocusCellHighlighter(observationTable){});
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(observationTable) {
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};
		
		TableViewerEditor.create(observationTable, focusCellManager, actSupport, ColumnViewerEditor.TABBING_HORIZONTAL | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR | ColumnViewerEditor.KEYBOARD_ACTIVATION);

		doubleCellEditor = new DoubleCellEditor(observationTable.getTable(), false);
		nullableDoubleCellEditor = new DoubleCellEditor(observationTable.getTable(), true);
		integerCellEditor = new IntegerCellEditor(observationTable.getTable());
		timeEditor = new TimeCellEditor(observationTable.getTable());
		attachmentEditor = new AttachmentCellEditor(observationTable.getTable());
		commentEditor = new TextCellEditor(observationTable.getTable(), SWT.MULTI | SWT.WRAP);
		observationEditor = new ObservationCellEditor(observationTable.getTable());
		
		observationTableColumns = new HashMap<OtColumn, TableViewerColumn>();
		
		waypointSorter = new WaypointSorter(observationTable);
		observationTable.setComparator(waypointSorter);
		for (int i = 0; i < OtColumn.values().length; i++) {
			final OtColumn columntype = OtColumn.values()[i];
			if (!editor.getPatrolEditor().getOptions().getTrackDistanceDirection() && 
					(columntype == OtColumn.DIRECTION || columntype == OtColumn.DISTANCE)){
				continue;
			}
			
			final TableViewerColumn column = new TableViewerColumn(observationTable,SWT.NONE);
			column.setLabelProvider(new ObsrvationTableLabelProvider(columntype));
			column.getColumn().setText(columntype.guiName);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(false);
			column.getColumn().setWidth(25);

			if(columntype != OtColumn.EAST && columntype != OtColumn.NORTH){
				column.setEditingSupport(new ObservationTableCellModifier(column.getViewer(), columntype));
			}
			
			observationTableColumns.put(columntype, column);
			
			if (columntype == OtColumn.ID || columntype == OtColumn.TIME){
				column.getColumn().addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						waypointSorter.setSortColumn(columntype, column.getColumn());
					}	
				});
				if (columntype == OtColumn.TIME){
					waypointSorter.setSortColumn(columntype, column.getColumn());
				}
			}
		
		}
	}
	

	
	private void showImportTrackWizard(){
		//Show Create Patrol Wizard
		final PatrolImportGpsDataWizard wizard = new PatrolImportGpsDataWizard(this.patrolLegDate, GPSDataImport.ImportType.TRACK);		

		final ProgressMonitorDialog pmd = new ProgressMonitorDialog(editor.getSite().getShell());
		try {
			pmd.run(false, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.setTaskName(LOAD_WIZARD_PROGRESS_MSG);
					dialog = new WizardDialog(editor.getSite().getShell(), wizard);
					dialog.open();
				}
			});
		} catch (Exception ex) {
			dialog = null;
			SmartPatrolPlugIn.displayLog(Messages.PatrolLegDayInputComposite_ErrorImportTracksWizard
					+ ex.getLocalizedMessage(), ex);
				}
		
	}
	

	private void showImportWaypointWizard(){
		//Show Create Patrol Wizard
		final PatrolImportGpsDataWizard wizard = new PatrolImportGpsDataWizard(this.patrolLegDate, GPSDataImport.ImportType.WAYPOINT);		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(editor.getSite().getShell());
		try {
			pmd.run(false, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.setTaskName(LOAD_WIZARD_PROGRESS_MSG);
					dialog = new WizardDialog(editor.getSite().getShell(), wizard);
					
					if (dialog != null) {
						monitor.setTaskName(SHOW_WIZARD_PROGRESS_MSG);
						dialog.open();
					}
				}
			});
		} catch (Exception ex) {
			dialog = null;
			SmartPatrolPlugIn.displayLog(Messages.PatrolLegDayInputComposite_ErrorImportWaypointWizard
					+ ex.getLocalizedMessage(), ex);
				}
		
	}

	private void setWaypointValue(Object element, OtColumn column, Object value){		
		Waypoint waypoint = ((PatrolWaypoint)element).getWaypoint();
		boolean needSave = false;
		if (column == OtColumn.ID) {
			waypoint.setId((Integer)value);
			needSave = true;
		} else if (column == OtColumn.EAST) {
			waypoint.setX((Double)value);
			needSave = true;
		} else if (column == OtColumn.NORTH) {
			waypoint.setY((Double)value);
			needSave = true;
		} else if (column == OtColumn.TIME) {
			if (value instanceof Date){ 
				waypoint.setDateTime(SmartUtils.combineDateTime(patrolLegDate.getDate(), new Time(((Date)value).getTime())));
				needSave = true;
			}
		} else if (column == OtColumn.DIRECTION) {
			needSave = true;
			if (value == null){
				waypoint.setDirection(null);
			}else{
				waypoint.setDirection(( (Double)value).floatValue());
			}
		} else if (column == OtColumn.DISTANCE) {
			if (value == null){
				waypoint.setDistance(null);
			}else{
				waypoint.setDistance( ( (Double)value).floatValue());
			}
			needSave = true;
		} else if (column == OtColumn.OBSERVATION) {
			//updated in cell editor
			needSave = false;
		} else if (column == OtColumn.COMMENT) {
			waypoint.setComment((String)value);
			needSave = true;
		} else if (column == OtColumn.ATTACHMENTS) {
			if (value != null){
				needSave = true;
			}
			//updated in cell editor
		}
		if (needSave){
			editor.getPatrolEditor().save(Collections.singleton((PatrolWaypoint)element));
		}
		observationTable.refresh();
		
	}
	
	private Object getWaypointValue(PatrolWaypoint element, OtColumn column) {

		Waypoint wp = ((PatrolWaypoint) element).getWaypoint();
		if (column == OtColumn.ID) {
			return wp.getId();
		} else if (column == OtColumn.EAST) {
			return wp.getX();
		} else if (column == OtColumn.NORTH) {
			return wp.getY();
		} else if (column == OtColumn.TIME) {
			return wp.getDateTime();
		} else if (column == OtColumn.DIRECTION) {
			return wp.getDirection();
		} else if (column == OtColumn.DISTANCE) {
			return wp.getDistance();
		} else if (column == OtColumn.OBSERVATION) {
			return wp;
		} else if (column == OtColumn.COMMENT) {
			if (wp.getComment() == null){
				return ""; //$NON-NLS-1$
			}
			return wp.getComment();
		} else if (column == OtColumn.ATTACHMENTS) {
			return wp;
		}
	
		return ""; //$NON-NLS-1$
	}
	
	private String getWaypointValueAsString(PatrolWaypoint element, OtColumn column) {

		Waypoint wp = ((PatrolWaypoint) element).getWaypoint();
		if (column == OtColumn.ID) {
			return String.valueOf(wp.getId());
		} else if (column == OtColumn.EAST) {
			return String.valueOf(Projection.transform(wp.getX(), wp.getY(), observationOptions.getViewProjection()).getX());
		} else if (column == OtColumn.NORTH) {
			return String.valueOf(Projection.transform(wp.getX(), wp.getY(), observationOptions.getViewProjection()).getY());
		} else if (column == OtColumn.TIME) {
			if (wp.getDateTime() != null) {
				return DateFormat.getTimeInstance(DateFormat.MEDIUM).format(wp.getDateTime());
			}
			return ""; //$NON-NLS-1$
		} else if (column == OtColumn.DIRECTION) {
			if (wp.getDirection() != null) {
				return String.valueOf(wp.getDirection());
			}
			return ""; //$NON-NLS-1$
		} else if (column == OtColumn.DISTANCE) {
			if (wp.getDistance() != null) {
				return String.valueOf(wp.getDistance());
			}
			return ""; //$NON-NLS-1$
		} else if (column == OtColumn.OBSERVATION) {
			if (wp.getObservations() == null
					|| wp.getObservations().size() == 0) {
				return Messages.PatrolLegDayInputComposite_NoObservationsLabel;
			} else {
				return wp.getObservationsAsString();
			}
		} else if (column == OtColumn.COMMENT) {
			if (wp.getComment() == null) {
				return ""; //$NON-NLS-1$
			}
			return wp.getComment();
		} else if (column == OtColumn.ATTACHMENTS) {
			int wpCnt = 0;
			if (wp.getObservations() != null){
				for (WaypointObservation wo : wp.getObservations()){
					if (wo.getAttachments() != null){
						wpCnt += wo.getAttachments().size();
					}
				}
			}
			if (wp.getAttachments() != null){
				wpCnt += wp.getAttachments().size();
			}
			if (wpCnt == 0 ) {
				return Messages.PatrolLegDayInputComposite_NoAttachmentments_ColumnLabel;
			} else {
				return MessageFormat.format(Messages.PatrolLegDayInputComposite_AttachmentColumnLabel, new Object[]{wpCnt});
			}
		}

		return ""; //$NON-NLS-1$
	}
	
	/**
	 * update patrol leg day values 
	 * @param session
	 */
	public void updateLegDay() {
		
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(0);
		cal.set(Calendar.HOUR_OF_DAY, dtEndTime.getHours());
		cal.set(Calendar.MINUTE, dtEndTime.getMinutes());
		cal.set(Calendar.SECOND, dtEndTime.getSeconds());
		Time t = new Time(cal.getTimeInMillis());
		patrolLegDate.setEndTime(t);
		
		cal.setTimeInMillis(0);
		cal.set(Calendar.HOUR_OF_DAY, dtStartTime.getHours());
		cal.set(Calendar.MINUTE, dtStartTime.getMinutes());
		cal.set(Calendar.SECOND, dtStartTime.getSeconds());
		t = new Time(cal.getTimeInMillis());
		patrolLegDate.setStartTime(t);
		
		int rest = 0;
		try{
			rest = Integer.parseInt(restMinutes.getText());
		}catch (Exception ex){
			SmartPatrolPlugIn.log("Could not parse rest minutes", ex); //$NON-NLS-1$
		}
		patrolLegDate.setRestMinutes(rest);
	}
	
	private void addWaypoint() {
		double y = 0, x = 0;
		int id = -1;
		Date last = null;
		for (Iterator<PatrolWaypoint> iterator = PatrolLegDayInputComposite.this.patrolLegDate.getWaypoints().iterator(); iterator.hasNext();) {
			PatrolWaypoint e = (PatrolWaypoint) iterator.next();
			Date t = (Date)getWaypointValue(e, OtColumn.TIME);
			
			if(last == null || t.after(last) || t.equals(last)  ){
				y = (Double) getWaypointValue(e, OtColumn.NORTH);
				x = (Double) getWaypointValue(e, OtColumn.EAST);
				id = (Integer) getWaypointValue(e, OtColumn.ID);
				last = t;
			}
		}
		AddWaypointDialog add;
	
		if(x == 0 && y == 0){
			add = new AddWaypointDialog(mainComposite.getShell(), editor.getPatrolEditor().getAvailableProjections());
		}else{
			add = new AddWaypointDialog(mainComposite.getShell(), y, x, id+1, editor.getPatrolEditor().getAvailableProjections());
		}
		if (add.open() == Window.OK){
			PatrolWaypoint wp = add.getWaypoint();
			wp.setPatrolLegDay(patrolLegDate);
			
			wp.getWaypoint().setDateTime(SmartUtils.getDatePart(patrolLegDate.getDate(), false));
			
			patrolLegDate.getWaypoints().add(wp);
			
			editor.getPatrolEditor().save(Collections.singleton(wp));
			PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_WAYPOINTS, patrolLegDate);
		}
	}

	class ObsrvationTableLabelProvider extends ColumnLabelProvider {

		private OtColumn column = null;

		public ObsrvationTableLabelProvider(OtColumn column) {
			this.column = column;
		}

		public String getText(Object element) {
			if (element instanceof PatrolWaypoint) {
				PatrolWaypoint wp = (PatrolWaypoint) element;
				return getWaypointValueAsString(wp, column);
			}
			return super.getText(element);
		}
		

	}
	
	
	class ObservationTableCellModifier extends EditingSupport{
		
		private OtColumn column;
		
		public ObservationTableCellModifier(ColumnViewer viewer, OtColumn column){
			super(viewer);
			this.column = column;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.EditingSupport#getCellEditor(java.lang.Object)
		 */
		@Override
		protected CellEditor getCellEditor(Object element) {
			if (column == OtColumn.NORTH || column == OtColumn.EAST ){
				return doubleCellEditor;
			}else if (column == OtColumn.DIRECTION || column == OtColumn.DISTANCE ){
				return nullableDoubleCellEditor;
			}else if (column == OtColumn.ID){
				return integerCellEditor;
			}else if (column == OtColumn.TIME){
				return timeEditor;
			}else if (column == OtColumn.ATTACHMENTS){
				return attachmentEditor;
			}else if (column == OtColumn.COMMENT){
				return commentEditor;
			}else if (column == OtColumn.OBSERVATION){
				//setup employees; if observer is being tracked;
				//we do this each time to ensure the observer list
				//is up-to-date
				if (editor.getPatrolEditor().getOptions().getTrackObserver()){
					List<Employee> emps = new ArrayList<Employee>();
					for (PatrolLegMember m : patrolLegDate.getPatrolLeg().getMembers()){
						emps.add(m.getMember());
					}
					Collections.sort(emps, new Comparator<Employee>() {
						@Override
						public int compare(Employee arg0, Employee arg1) {
							return Collator.getInstance().compare(arg0.getFullLabel().toUpperCase(), arg1.getFullLabel().toUpperCase());
						}
					});
					observationEditor.setObservers(emps);
				}else{
					observationEditor.setObservers(null);
				}
				return observationEditor;
			}
			return null;
			
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.EditingSupport#canEdit(java.lang.Object)
		 */
		@Override
		protected boolean canEdit(Object element) {
			if (PatrolLegDayInputComposite.this.editor.getPatrolEditor().canEdit() != null){
				return false;
			}
			return true;	
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.EditingSupport#getValue(java.lang.Object)
		 */
		@Override
		protected Object getValue(Object element) {
			return getWaypointValue((PatrolWaypoint)element, column);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.EditingSupport#setValue(java.lang.Object, java.lang.Object)
		 */
		@Override
		protected void setValue(Object element, Object value) {
			setWaypointValue((PatrolWaypoint)element, column, value);
		}
	}
}
