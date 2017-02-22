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
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.common.celleditor.DoubleCellEditor;
import org.wcs.smart.common.celleditor.IntegerCellEditor;
import org.wcs.smart.common.celleditor.TimeCellEditor;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.ui.mision.importwp.MissionImportGpsDataWizard;
import org.wcs.smart.gpx.GPSDataImport;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.common.importwp.ImportGpsDataWizard;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.ui.AttachmentCellEditor;
import org.wcs.smart.observation.ui.ObservationCellEditor;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite for editing mission days data.  This includes modifying
 * the date/time, rest minutes, tracks and waypoints.
 * A lot of code is copied from PatrolLegDayInputComposite
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class MissionDayComposite {

	private MissionDayPage editor;
	
	private Composite mainComposite;

	private DateTime dtStartTime;
	private DateTime dtEndTime;
	private Text restMinutes;
	private Label lblTotalHours;
	private Font okayFont;
	private Font errorFont;
	private Label txtDistance;
	
	private TableViewer observationTable;
	
	private Hyperlink lnkImportWaypoints;
	private TableViewer trackTable;
	private Hyperlink lnkEditTrack;

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
	private SamplingUnitCellEditor samplingUnitEditor;
	
	private HashMap<OtColumn, TableViewerColumn> observationTableColumns;	
	private CoordinateReferenceSystem lcrs;
	
	protected enum OtColumn {
		ID(Messages.MissionDayComposite_WaypointID, 1),
		EAST(Messages.MissionDayComposite_X, 2),
		NORTH(Messages.MissionDayComposite_Y, 2),
		TIME(Messages.MissionDayComposite_Time, 2),
		DIRECTION(Messages.MissionDayComposite_Direction, 1),
		DISTANCE(Messages.MissionDayComposite_Distance, 1),
		SAMPLING_UNIT(Messages.MissionDayComposite_SamplingUnit, 4),
		OBSERVATION(Messages.MissionDayComposite_Observation, 4),
		COMMENT(Messages.MissionDayComposite_Comment, 3),
		ATTACHMENTS(Messages.MissionDayComposite_Attachment, 3);

		protected String guiName;
		protected int weight;

		private OtColumn(String name, int weight) {
			this.guiName = name;
			this.weight = weight;
		}
	}

	public MissionDayComposite(MissionDayPage editor) {
		this.editor = editor;
		try{
			if (editor.getMissionEditor().getViewProjection() != null){
				lcrs = ReprojectUtils.stringToCrs(editor.getMissionEditor().getViewProjection().getDefinition());
			}else{
				lcrs = GeometryUtils.SMART_CRS;
			}
		}catch (Exception ex){
			EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
		}
	}
	
	public Composite createComposite(Composite parent, FormToolkit toolkit) {
		boolean canEdit = editor.getMissionEditor().canEdit() == null;
		
		mainComposite = toolkit.createComposite(parent);
		mainComposite.setLayout(new GridLayout(1, false));
		mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite timeInfo = toolkit.createComposite(mainComposite);
		timeInfo.setLayout(new GridLayout(4, false));
		((GridLayout) timeInfo.getLayout()).horizontalSpacing = 15;
		 ((GridLayout)timeInfo.getLayout()).marginWidth = 0;
		 ((GridLayout)timeInfo.getLayout()).marginLeft = 5;
		 ((GridLayout)timeInfo.getLayout()).marginHeight = 0;
		timeInfo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Composite c = toolkit.createComposite(timeInfo);
		c.setLayout(new GridLayout(2, false));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		toolkit.createLabel(c, Messages.MissionDayComposite_StartTime);
		dtStartTime = new DateTime(c, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
		toolkit.adapt(dtStartTime);
		dtStartTime.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateTotalHours();
			}
			
		});
		dtStartTime.setEnabled(canEdit);
		dtStartTime.addFocusListener(new FocusAdapter() {			
			@Override
			public void focusLost(FocusEvent e) {
				if (timeEqual(SmartUtils.getTime(dtStartTime).getTime(),
						missionDay.getStartTime().getTime())){
					//no changes made
					return;
				}
				saveChanges();
			}
		});

		c = toolkit.createComposite(timeInfo);
		c.setLayout(new GridLayout(2, false));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		toolkit.createLabel(c, Messages.MissionDayComposite_EndTime);
		dtEndTime = new DateTime(c, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
		toolkit.adapt(dtEndTime);
		dtEndTime.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateTotalHours();
			}
		});
		dtEndTime.setEnabled(canEdit);
		dtEndTime.addFocusListener(new FocusAdapter() {			
			@Override
			public void focusLost(FocusEvent e) {
				if (timeEqual(SmartUtils.getTime(dtEndTime).getTime(), 
						missionDay.getEndTime().getTime())){
					return;
				}
				saveChanges();
			}
		});

		
		c = toolkit.createComposite(timeInfo);
		c.setLayout(new GridLayout(2, false));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		toolkit.createLabel(c, Messages.MissionDayComposite_RestMinutes);
		restMinutes = toolkit.createText(c, "0", SWT.BORDER); //$NON-NLS-1$
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd.widthHint = 30;
		restMinutes.setLayoutData(gd);
		restMinutes.setEnabled(canEdit);
		restMinutes.addFocusListener(new FocusListener() {
			private int oldValue; 
			
			@Override
			public void focusLost(FocusEvent e) {
				try{
					int x = Integer.parseInt(restMinutes.getText());
					if (missionDay.getRestMinutes() != null && 
							x == missionDay.getRestMinutes()){
						return;
					}
					if (x < 0){
						throw new Exception("Rest minutes cannot be negative."); //$NON-NLS-1$
					}
				}catch (Exception ex){
					restMinutes.setText(String.valueOf(oldValue));
					MessageDialog.openWarning(mainComposite.getShell(), Messages.MissionDayComposite_Error, Messages.MissionDayComposite_InvalidRestMinutes);
					mainComposite.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							restMinutes.setFocus();
						}
					});
					
				}
				updateTotalHours();
				saveChanges();
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
		toolkit.createLabel(c, Messages.MissionDayComposite_TotalHours);
		lblTotalHours = toolkit.createLabel(c, Messages.MissionDayComposite_InvalidHours);
		okayFont = lblTotalHours.getFont();
		
		FontData fd = okayFont.getFontData()[0];
		fd.setStyle(SWT.BOLD);
		errorFont = new Font(lblTotalHours.getDisplay(), fd);
		
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.widthHint = 30;
		lblTotalHours.setLayoutData(gd);

		
		Composite half = toolkit.createComposite(mainComposite);
		GridLayout gl = new GridLayout(2, true);
		gl.marginWidth = gl.marginHeight = 0;
		half.setLayout(gl);
		half.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite trackComp = toolkit.createComposite(half);
		gl = new GridLayout(2, false);
		gl.marginWidth = gl.marginHeight = 0;
		trackComp.setLayout(gl);
		trackComp.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		
		toolkit.createLabel(trackComp, Messages.MissionDayComposite_Tracks).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		Table trTable = toolkit.createTable(trackComp, SWT.V_SCROLL | SWT.H_SCROLL);
		trackTable = new TableViewer(trTable);
		trackTable.setContentProvider(ArrayContentProvider.getInstance());
		trackTable.setLabelProvider(new MissionTrackLabelProvider());
		trTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)trTable.getLayoutData()).minimumHeight = 40;
		((GridData)trTable.getLayoutData()).heightHint = 40;
		((GridData)trTable.getLayoutData()).widthHint = 250;
		if (canEdit){
			lnkEditTrack = toolkit.createHyperlink(trackComp, Messages.MissionDayComposite_TrackEditLink, SWT.NONE);
			lnkEditTrack.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
			lnkEditTrack.addHyperlinkListener(new HyperlinkAdapter(){
				public void linkActivated(HyperlinkEvent e) {
					showEditTrackDialog();
				}
			});
		}else{
			toolkit.createLabel(trackComp, ""); //$NON-NLS-1$
		}
				
		Composite distComp = toolkit.createComposite(half);
		gl = new GridLayout(2, false);
		gl.marginHeight = 0;
		gl.marginWidth = 10;
		distComp.setLayout(gl);
		distComp.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		
		toolkit.createLabel(distComp, Messages.MissionDayComposite_DistanceTraveled);
		txtDistance = toolkit.createLabel(distComp, "0", SWT.NONE); //$NON-NLS-1$

		gd = new GridData(SWT.FILL, SWT.BOTTOM, false, false, 1, 1);
		gd.widthHint = 50;
		txtDistance.setLayoutData(gd);
		
		
		Composite observationHcomp = toolkit.createComposite(mainComposite);
		gl = new GridLayout(2, false);
		gl.marginHeight=gl.marginWidth = 0;
		gl.marginTop = 5;
		observationHcomp.setLayout(gl);
		Label l = toolkit.createLabel(observationHcomp, Messages.MissionDayComposite_ObservationsWaypoints);
		l.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		if (canEdit){
			lnkImportWaypoints = toolkit.createHyperlink(observationHcomp, Messages.MissionDayComposite_ImportWaypoints, SWT.NONE);
			lnkImportWaypoints.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
			lnkImportWaypoints.addHyperlinkListener(new HyperlinkAdapter(){
				public void linkActivated(HyperlinkEvent e) {
					showImportWaypointWizard();
				}
			});
		}else{
			toolkit.createLabel(observationHcomp, ""); //$NON-NLS-1$
		}

		
		observationTable = new TableViewer(mainComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		toolkit.adapt(observationTable.getTable());
		setupObservationTable();
		editor.getMissionEditor().getSelectionProvider().addSelectionProvider(observationTable);
		observationTable.getTable().addPaintListener(new PaintListener() {
			boolean called = false;
			@Override
			public void paintControl(PaintEvent e) {
				if (called) return;
				called = true;
				resize();
			}
		});
		
		if (canEdit){
			Composite buttonComp = toolkit.createComposite(mainComposite);
			buttonComp.setLayout(new GridLayout(3, false));
			btnAddWaypoint = toolkit.createButton(buttonComp, Messages.MissionDayComposite_AddWaypoint, SWT.PUSH);
			btnAddWaypoint.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					addWaypoint();
				}
			});
			
			
			btnDeleteWaypoint = toolkit.createButton(buttonComp, Messages.MissionDayComposite_DeleteWaypoint, SWT.PUSH);
			btnDeleteWaypoint.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e){
					deleteSelectedWaypoints();
				}
			});
			
			btnMoveWaypoint = toolkit.createButton(buttonComp, Messages.MissionDayComposite_MoveWaypoint, SWT.PUSH);
			btnMoveWaypoint.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e){
					moveSelectedWaypoints();
				}
			});
		}
		updateTotalHours();
		
		return mainComposite;
	}
	
	private void saveChanges(){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(0);
		cal.set(Calendar.HOUR_OF_DAY, dtEndTime.getHours());
		cal.set(Calendar.MINUTE, dtEndTime.getMinutes());
		cal.set(Calendar.SECOND, dtEndTime.getSeconds());
		Time t = new Time(cal.getTimeInMillis());
		missionDay.setEndTime(t);
		
		cal.setTimeInMillis(0);
		cal.set(Calendar.HOUR_OF_DAY, dtStartTime.getHours());
		cal.set(Calendar.MINUTE, dtStartTime.getMinutes());
		cal.set(Calendar.SECOND, dtStartTime.getSeconds());
		t = new Time(cal.getTimeInMillis());
		missionDay.setStartTime(t);
		
		int rest = 0;
		try{
			rest = Integer.parseInt(restMinutes.getText());
		}catch (Exception ex){
			EcologicalRecordsPlugIn.log("Could not parse rest minutes", ex); //$NON-NLS-1$
		}
		missionDay.setRestMinutes(rest);
		
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			session.saveOrUpdate(missionDay);
			session.getTransaction().commit();
		}catch (Exception ex){
			
		}finally{
			session.close();
		}
		SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, editor.getMissionEditor().getMission());
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
		if (editor.getMissionEditor().getConfigurableModel() != null){
			observationEditor = new ObservationCellEditor(observationTable.getTable(), editor.getMissionEditor().getConfigurableModel());
		}else{
			observationEditor = new ObservationCellEditor(observationTable.getTable());
		}
		samplingUnitEditor = new SamplingUnitCellEditor(observationTable.getTable(), false);
		
		observationTableColumns = new HashMap<OtColumn, TableViewerColumn>();
		
		final WaypointSorter waypointSorter = new WaypointSorter(observationTable);
		observationTable.setComparator(waypointSorter);
		for (int i = 0; i < OtColumn.values().length; i++) {
			final OtColumn columntype = OtColumn.values()[i];
			if (!editor.getMissionEditor().trackDistanceDirection() &&  
					(columntype == OtColumn.DIRECTION || columntype == OtColumn.DISTANCE)){
				continue;
			}
			
			final TableViewerColumn column = new TableViewerColumn(observationTable,SWT.NONE);
			column.setLabelProvider(new ObservationTableLabelProvider(columntype));
			column.getColumn().setText(columntype.guiName);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(false);
			column.getColumn().setWidth(25);

			if(columntype != OtColumn.EAST && columntype != OtColumn.NORTH) {
				column.setEditingSupport(new ObservationTableCellModifier(column.getViewer(), columntype));
			}else{
				if (editor.getMissionEditor().getViewProjection() != null){
					column.getColumn().setToolTipText(editor.getMissionEditor().getViewProjection().getName());
				}else{
					column.getColumn().setToolTipText( lcrs.getName().toString());
				}
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
	
	private MissionDay findMissionDay(Mission m){
		for (MissionDay md : m.getMissionDays()){
			if (SharedUtils.isSameDate(md.getDate(), editor.getDay())){
				return md;		
			}
		}
		return null;
	}
	
	private MissionDay missionDay;
	
	public void initData() {
		missionDay = null;
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			session.update(editor.getMissionEditor().getMission());

			//find mission day
			missionDay = findMissionDay(editor.getMissionEditor().getMission());
			if (missionDay == null){
				throw new IllegalStateException(MessageFormat.format(Messages.MissionDayComposite_DayNotFound, new Object[]{editor.getDay().toString()}));
			}

			dtStartTime.setTime(0,0,0);
			if (missionDay.getStartTime() != null){
				Calendar cal = Calendar.getInstance();
				cal.setTime(missionDay.getStartTime());
				dtStartTime.setTime(cal.get(Calendar.HOUR_OF_DAY),
						cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
			}
			
			dtEndTime.setTime(23,59,59);
			if (missionDay.getEndTime() != null){
				Calendar cal = Calendar.getInstance();
				cal.setTime(missionDay.getEndTime());
				dtEndTime.setTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
						cal.get(Calendar.SECOND));
			}
			if (missionDay.getRestMinutes() == null) {
				restMinutes.setText("0"); //$NON-NLS-1$
			} else {
				restMinutes.setText(String.valueOf(missionDay.getRestMinutes()));
			}
	
			this.lblTotalHours.setText(String.valueOf(missionDay.getHoursWorked()));

			List<MissionTrack> tracks = missionDay.getTracks();
			trackTable.setInput(tracks);
			double distance = 0;
			for (MissionTrack mt : tracks){
				distance += mt.getDistance();
			}
			txtDistance.setText(String.valueOf(distance));
			
			if (missionDay.getWaypoints() == null) {
				missionDay.setWaypoints(new ArrayList<SurveyWaypoint>());
			}
			//load waypoints and attach to session; for performance reasons
			//waypoints are not cascaded (otherwise saves are cascaded too)
			for (SurveyWaypoint wp : missionDay.getWaypoints()) {
				session.update(wp.getWaypoint());
				if (wp.getSamplingUnit() != null){
					session.update(wp.getSamplingUnit());
					wp.getSamplingUnit().getId();
				}
				
				if (wp.getWaypoint().getObservations() != null){
					wp.getWaypoint().getObservations().size();
				}
			}
			observationTable.setInput(missionDay.getWaypoints());
			observationTable.refresh();
			observationTable.addSelectionChangedListener(new ISelectionChangedListener() {
				
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					if (editor.getMissionEditor().canEdit() == null) {
						boolean enabled = !((IStructuredSelection)observationTable.getSelection()).isEmpty();
						btnDeleteWaypoint.setEnabled(enabled);
						
						if (!SharedUtils.isSameDate(editor.getMissionEditor().getMission().getStartDate(), 
								editor.getMissionEditor().getMission().getEndDate())) {
							btnMoveWaypoint.setEnabled(enabled);	
						} else {
							btnMoveWaypoint.setEnabled(false);
						}
					}
				}
			});
			samplingUnitEditor.setInput(missionDay);
			
//			this.viewTrackPoints.setEnabled( this.patrolLegDate.getTrack() != null );
					
			updateTotalHours();
			
			if (btnMoveWaypoint != null){
				btnMoveWaypoint.setEnabled(false);
			}
			if (btnDeleteWaypoint != null){
				btnDeleteWaypoint.setEnabled(false);
			}
			
			if (editor.getMissionEditor().canEdit() != null){
				dtEndTime.setEnabled(false);
				dtStartTime.setEnabled(false);
				restMinutes.setEditable(false);
				restMinutes.setEnabled(false);
				
				if (btnAddWaypoint != null){
					btnAddWaypoint.setVisible(false);
				}
				if (btnDeleteWaypoint != null){
					btnDeleteWaypoint.setVisible(false);
				}
				if (btnMoveWaypoint != null){
					btnMoveWaypoint.setVisible(false);
				}
				if (lnkImportWaypoints != null){
					lnkImportWaypoints.setVisible(false);
				}
				if (lnkEditTrack != null){
					lnkEditTrack.setVisible(false);
				}
			}
		}catch (Exception ex){
			EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
		} finally {
			session.getTransaction().rollback();
			session.close();
		}
		
	}

	public void selectWaypoint(SurveyWaypoint wp){
		observationTable.setSelection(new StructuredSelection(wp));
		observationTable.getTable().showSelection();
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

	private int getMaximumWidth(GC gc, OtColumn column){ //, Object[] events) {
		int width = 0;
		Point extent = gc.textExtent(column.guiName);
		width = extent.x;

		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			MissionDay md = (MissionDay)session.merge(missionDay);

			for (Iterator<SurveyWaypoint> iterator = md.getWaypoints().iterator(); iterator.hasNext();) {
				SurveyWaypoint e = iterator.next();
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
		} finally {
			session.getTransaction().rollback();
			session.close();
		}
	}

	protected void showEditTrackDialog() {
		try {
			final MissionTrackEditDialog editDialog = new MissionTrackEditDialog(editor.getSite().getShell(), missionDay);
			editDialog.open();
		} finally {
			ApplicationGIS.getToolManager().setCurrentEditor(editor.getMissionEditor());
		}
	}
	
	protected void showImportWaypointWizard() {
		final ImportGpsDataWizard wizard = new MissionImportGpsDataWizard(missionDay, GPSDataImport.ImportType.WAYPOINT);
		wizard.setDateOption(editor.getDay());
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(editor.getSite().getShell());
		try {
			pmd.run(false, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.setTaskName(Messages.MissionDayComposite_LoadingWizard);
					WizardDialog dialog = new WizardDialog(editor.getSite().getShell(), wizard);

					if (dialog != null) {
						monitor.setTaskName(Messages.MissionDayComposite_DisplayingWizard);
						dialog.open();
					}
				}
			});
		} catch (Exception ex) {
			EcologicalRecordsPlugIn.displayLog(Messages.MissionDayComposite_ImportWizardError + ex.getLocalizedMessage(), ex);
		}
	}

	protected void updateTotalHours() {
		double d = Double.parseDouble(this.restMinutes.getText());
		double time = SmartUtils.getTime(dtEndTime).getTime() - SmartUtils.getTime(dtStartTime).getTime() - d * 60 * 1000;
		time = time / (1000 * 60 * 60);
		//lblTotalHours.setText(PatrolEditor.REST_TIME_FORMATTER.format(time));
		lblTotalHours.setText(MissionEditor.formatTimeRange(time));
		if (time < 0){
			lblTotalHours.setFont(errorFont);
			lblTotalHours.setForeground(lblTotalHours.getDisplay().getSystemColor(SWT.COLOR_RED));
			lblTotalHours.setToolTipText(Messages.MissionDayComposite_startBeforeEnd);
		}else{
			lblTotalHours.setFont(okayFont);
			lblTotalHours.setForeground(lblTotalHours.getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
			lblTotalHours.setToolTipText(null);
		}
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
	
	public void refreshTable() {
		observationTable.refresh(true);
	}
	
	protected void addWaypoint() {
		SurveyWaypoint lastWp = null;
		
		for (Iterator<SurveyWaypoint> iterator = missionDay.getWaypoints().iterator(); iterator.hasNext();) {
			SurveyWaypoint e = iterator.next();
			Date t = (Date)getWaypointValue(e, OtColumn.TIME);
			
			if(lastWp == null || 
					t.after((Date)getWaypointValue(lastWp, OtColumn.TIME)) || 
					t.equals((Date)getWaypointValue(lastWp, OtColumn.TIME))  ){
				lastWp = e;
			}
		}
		AddWaypointDialog add;
	
		if(lastWp == null){
			add = new AddWaypointDialog(mainComposite.getShell(), 
					editor.getMissionEditor().getAvailableProjections(),
					editor.getMissionEditor().getSamplingUnits());
		}else{
			add = new AddWaypointDialog(mainComposite.getShell(), lastWp,
					editor.getMissionEditor().getAvailableProjections(),
					editor.getMissionEditor().getSamplingUnits());
		}
		if (add.open() == Window.OK){
			SurveyWaypoint wp = add.getWaypoint();
			wp.setMissionDay(missionDay);
			
			wp.getWaypoint().setDateTime(SharedUtils.getDatePart(missionDay.getDate(), false));
			
			missionDay.getWaypoints().add(wp);
			
			editor.getMissionEditor().save(Collections.singleton(wp));
			SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, editor.getMissionEditor().getMission());
		}
	}

	protected void deleteSelectedWaypoints() {
		boolean doDel = MessageDialog.openConfirm(mainComposite.getShell(), Messages.MissionDayComposite_DeleteDialog_Title, Messages.MissionDayComposite_DeleteDialog_Message);
		if (!doDel){
			return;
		}
		IStructuredSelection selection = ((IStructuredSelection)observationTable.getSelection());
		ArrayList<SurveyWaypoint> deleted = new ArrayList<SurveyWaypoint>();
		
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			SurveyWaypoint w = (SurveyWaypoint) iterator.next();
			if (missionDay.getWaypoints().remove(w)){
				w.setMissionDay(null);
				deleted.add(w);
			}	
		}
		
		//delete waypoints
		Job j = editor.getMissionEditor().delete(deleted);
		j.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				//once the job is completed we can fire this event
				mainComposite.getDisplay().syncExec(new Runnable() {
					
					@Override
					public void run() {
						SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, editor.getMissionEditor().getMission());
					}
				});
			}
		});
	}

	protected void moveSelectedWaypoints() {
		MoveWaypointDialog dialog = new MoveWaypointDialog(mainComposite.getShell(), missionDay);
		if (dialog.open() == Window.OK) {
			IStructuredSelection selection = ((IStructuredSelection)observationTable.getSelection());
			
			MissionDay moveTo = dialog.getMoveToDate();
			List<SurveyWaypoint> toUpdate = new ArrayList<SurveyWaypoint>();
			for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
				SurveyWaypoint w = (SurveyWaypoint) iterator.next();
				toUpdate.add(w);
				Waypoint wp = w.getWaypoint();
				wp.setDateTime(SmartUtils.combineDateTime(moveTo.getDate(), wp.getDateTime()));
				w.setMissionDay(moveTo);
			}

			editor.getMissionEditor().save(toUpdate);
			SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, editor.getMissionEditor().getMission());
		}
	}
	
	private String getWaypointValueAsString(SurveyWaypoint element, OtColumn column) {

		Waypoint wp = element.getWaypoint();
		if (column == OtColumn.ID) {
			return String.valueOf(wp.getId());
		} else if (column == OtColumn.EAST) {
			return String.valueOf(ReprojectUtils.transform(wp.getX(), wp.getY(), lcrs).getX());
		} else if (column == OtColumn.NORTH) {
			return String.valueOf(ReprojectUtils.transform(wp.getX(), wp.getY(), lcrs).getY());
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
				return Messages.MissionDayComposite_None;
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
				return Messages.MissionDayComposite_None;
			} else {
				return MessageFormat.format(Messages.MissionDayComposite_Files, wpCnt);
			}
		} else if (column == OtColumn.SAMPLING_UNIT) {
			SamplingUnit su = element.getSamplingUnit();
			if (su != null){
				return su.getId();
			}
			if (element.getMissionTrack() != null){
				return element.getMissionTrack().getId();
			}
			return Messages.MissionDayComposite_None;
		}

		return ""; //$NON-NLS-1$
	}

	private Object getWaypointValue(SurveyWaypoint element, OtColumn column) {
		Waypoint wp = element.getWaypoint();
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
		} else if (column == OtColumn.SAMPLING_UNIT) {
			if (element.getSamplingUnit() != null){
				return samplingUnitEditor.getIndex(element.getSamplingUnit());
			}else if (element.getMissionTrack() != null){
				return samplingUnitEditor.getIndex(element.getMissionTrack());
			}
			return samplingUnitEditor.getIndex(null);
		}
		return ""; //$NON-NLS-1$
	}

	private void setWaypointValue(SurveyWaypoint element, OtColumn column, Object value){		
		Waypoint waypoint = element.getWaypoint();
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
				waypoint.setDateTime(SmartUtils.combineDateTime(missionDay.getDate(), new Time(((Date)value).getTime())));
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
		} else if (column == OtColumn.SAMPLING_UNIT) {
			if (value instanceof Integer) {
				Object x = samplingUnitEditor.getSamplingUnit((Integer)value);
				if (x == null){
					element.setSamplingUnit(null);
					element.setMissionTrack(null);
				}else if (x instanceof SamplingUnit){
					element.setSamplingUnit((SamplingUnit) x);
					element.setMissionTrack(null);
				}else if (x instanceof MissionTrack){
					element.setSamplingUnit(null);
					element.setMissionTrack((MissionTrack) x);
				}
				needSave = true;
			}
		}
		
		if (needSave){
			editor.getMissionEditor().save(Collections.singleton((SurveyWaypoint)element));
		}
		observationTable.refresh();
	}
	
	public void dispose(){
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
	
	public void refreshSamplingUnitEditor(){
		samplingUnitEditor.setInput(missionDay);
	}
	
	/**
	 * ColumnLabelProvider
	 * @author elitvin
	 * @since 3.0.0
	 */
	private class ObservationTableLabelProvider extends ColumnLabelProvider {

		private OtColumn column = null;

		public ObservationTableLabelProvider(OtColumn column) {
			this.column = column;
		}

		public String getText(Object element) {
			if (element instanceof SurveyWaypoint) {
				SurveyWaypoint wp = (SurveyWaypoint) element;
				return getWaypointValueAsString(wp, column);
			}
			return super.getText(element);
		}
	}
	
	
	private class ObservationTableCellModifier extends EditingSupport {
		
		private OtColumn column;
		
		public ObservationTableCellModifier(ColumnViewer viewer, OtColumn column){
			super(viewer);
			this.column = column;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			switch (column) {
			case NORTH:			return doubleCellEditor;
			case EAST:			return doubleCellEditor;
			case DIRECTION:		return nullableDoubleCellEditor;
			case DISTANCE:		return nullableDoubleCellEditor;
			case ID: 			return integerCellEditor;
			case TIME: 			return timeEditor;
			case ATTACHMENTS:	return attachmentEditor;
			case COMMENT:		return commentEditor;
			case OBSERVATION:
				//setup employees here incase they have changed
				if (editor.getMissionEditor().trackObserver()){
					List<Employee> emps = new ArrayList<Employee>();
					for (MissionMember mm : missionDay.getMission().getMembers()){
						emps.add(mm.getMember());
						SmartLabelProvider.getFullLabel(mm.getMember());
					}
					//sort
					Collections.sort(emps, new Comparator<Employee>() {
						@Override
						public int compare(Employee arg0, Employee arg1) {
							return Collator.getInstance().compare(
									SmartLabelProvider.getFullLabel(arg0).toUpperCase(), 
									SmartLabelProvider.getFullLabel(arg1).toUpperCase());
						}
					});
					observationEditor.setObservers(emps);
				}else{
					observationEditor.setObservers(null);
				}
				
				return observationEditor;
			case SAMPLING_UNIT: return samplingUnitEditor;
			default:
				break;
			}
			return null;
		}

		@Override
		protected boolean canEdit(Object element) {
			if (MissionDayComposite.this.editor.getMissionEditor().canEdit() != null){
				return false;
			}
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			return getWaypointValue((SurveyWaypoint)element, column);
		}

		@Override
		protected void setValue(Object element, Object value) {
			setWaypointValue((SurveyWaypoint)element, column, value);
		}
	}
	
}
