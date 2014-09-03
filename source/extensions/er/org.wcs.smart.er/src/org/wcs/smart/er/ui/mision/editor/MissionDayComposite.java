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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.common.celleditor.DoubleCellEditor;
import org.wcs.smart.common.celleditor.IntegerCellEditor;
import org.wcs.smart.common.celleditor.TimeCellEditor;
import org.wcs.smart.er.ISurveyEventListener;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.ui.AttachmentCellEditor;
import org.wcs.smart.observation.ui.ObservationCellEditor;
import org.wcs.smart.ui.map.location.SmartPointLabelProvider;
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
	private Mission mission;
	
	private Composite mainComposite;

	private DateTime dtStartTime;
	private DateTime dtEndTime;
	private Text restMinutes;
	private Text txtDistance;
	private Label lblTotalHours;

	private TableViewer observationTable;
	private ObservationOptions observationOptions;
	private List<SurveyWaypoint> input;
	
	private Font okayFont;
	private Font errorFont;
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
	
	private HashMap<OtColumn, TableViewerColumn> observationTableColumns;	
	
	private ISurveyEventListener trackListener = new ISurveyEventListener() {
		@Override
		public void event(Object o) {
//			updateDistance();
			// TODO Auto-generated method stub
			
		}
	};
	private ISurveyEventListener waypointListener = new ISurveyEventListener() {
		@Override
		public void event(Object o) {
			refreshTable();
		}
	};
	
	protected enum OtColumn {
		ID("Waypoint ID", 1),
		EAST("X", 2),
		NORTH("Y", 2),
		TIME("Time", 2),
		DIRECTION("Direction", 1),
		DISTANCE("Distance", 1),
		OBSERVATION("Observation", 4),
		COMMENT("Comment", 3),
		ATTACHMENTS("Attachment", 3);

		protected String guiName;
		protected int weight;

		private OtColumn(String name, int weight) {
			this.guiName = name;
			this.weight = weight;
		}
	}

	public MissionDayComposite(MissionDayPage editor, ObservationOptions observationOptions) {
		this.editor = editor;
		this.observationOptions = observationOptions;
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
		toolkit.createLabel(c, "StartTime:");
		dtStartTime = new DateTime(c, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
		toolkit.adapt(dtStartTime);
		dtStartTime.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateTotalHours();
			}
			
		});
//		dtStartTime.addFocusListener(new FocusAdapter() {			
//			@Override
//			public void focusLost(FocusEvent e) {
//				if (timeEqual(SmartUtils.getTime(dtStartTime).getTime(), patrolLegDate.getStartTime().getTime())){
//					//no changes made
//					return;
//				}
//				editor.getPatrolEditor().save(patrolLegDate);
//				PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_DATES_LEG, patrolLegDate);
//			}
//		});

		c = toolkit.createComposite(timeInfo);
		c.setLayout(new GridLayout(2, false));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		toolkit.createLabel(c, "EndTime:");
		dtEndTime = new DateTime(c, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
		toolkit.adapt(dtEndTime);
		dtEndTime.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateTotalHours();
			}
			
		});
//		dtEndTime.addFocusListener(new FocusAdapter() {			
//			@Override
//			public void focusLost(FocusEvent e) {
//				if (timeEqual(SmartUtils.getTime(dtEndTime).getTime(), patrolLegDate.getEndTime().getTime())){
//					//no changes made
//					return;
//				}
//				editor.getPatrolEditor().save(patrolLegDate);
//				PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_DATES_LEG, patrolLegDate);
//			}
//		});

		
		c = toolkit.createComposite(timeInfo);
		c.setLayout(new GridLayout(2, false));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		toolkit.createLabel(c, "RestMinutes:");
		restMinutes = toolkit.createText(c, "0", SWT.BORDER); //$NON-NLS-1$
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd.widthHint = 30;
		restMinutes.setLayoutData(gd);
//		restMinutes.addFocusListener(new FocusListener() {
//			private int oldValue; 
//			
//			@Override
//			public void focusLost(FocusEvent e) {
//				try{
//					int x = Integer.parseInt(restMinutes.getText());
//					if (patrolLegDate.getRestMinutes() != null && x == patrolLegDate.getRestMinutes()){
//						return;
//					}
//					if (x < 0){
//						throw new Exception("Rest minutes cannot be negative."); //$NON-NLS-1$
//					}
//				}catch (Exception ex){
//					restMinutes.setText(String.valueOf(oldValue));
//					MessageDialog.openWarning(Display.getCurrent().getActiveShell(), Messages.PatrolLegDayInputComposite_Error_DialogTitle, Messages.PatrolLegDayInputComposite_InvalidRestMinutes_DialogMessage1);
//					Display.getCurrent().asyncExec(new Runnable() {
//						@Override
//						public void run() {
//							restMinutes.setFocus();
//						}
//					});
//					
//				}
//				updateTotalHours();
//				editor.getPatrolEditor().save(patrolLegDate);
//				PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_DATES_LEG, patrolLegDate);
//			}
//			
//			@Override
//			public void focusGained(FocusEvent e) {
//				oldValue = Integer.parseInt(restMinutes.getText());
//			}
//		});
		
		
		c = toolkit.createComposite(timeInfo);
		c.setLayout(new GridLayout(2, false));
		c.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		toolkit.createLabel(c, "Total Hours:");
		lblTotalHours = toolkit.createLabel(c, "InvalidTotalHours");
		okayFont = lblTotalHours.getFont();
		
		FontData fd = okayFont.getFontData()[0];
		fd.setStyle(SWT.BOLD);
		errorFont = new Font(Display.getDefault(), fd);
		
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.widthHint = 30;
		lblTotalHours.setLayoutData(gd);

		Composite trackComp = toolkit.createComposite(mainComposite);
		trackComp.setLayout(new GridLayout(3, false));
		
		
		toolkit.createLabel(trackComp, "Distance Travelled (km):");
		txtDistance = toolkit.createText(trackComp, "0", SWT.NONE); //$NON-NLS-1$
		txtDistance.setEditable(false);
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
		gd.widthHint = 50;
		txtDistance.setLayoutData(gd);

		toolkit.createLabel(trackComp, "Tracks:");
		Table trTable = toolkit.createTable(trackComp, SWT.V_SCROLL | SWT.H_SCROLL);
		trackTable = new TableViewer(trTable);
		trackTable.setContentProvider(ArrayContentProvider.getInstance());
		trackTable.setLabelProvider(new MissionTrackLabelProvider());
		trTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		((GridData)trTable.getLayoutData()).minimumHeight = 40;
		((GridData)trTable.getLayoutData()).heightHint = 40;
		lnkEditTrack = toolkit.createHyperlink(trackComp, "edit", SWT.NONE);
		lnkEditTrack.addHyperlinkListener(new HyperlinkAdapter(){
			public void linkActivated(HyperlinkEvent e) {
				showImportWaypointWizard();
			}
		});
		
		Composite observationHcomp = toolkit.createComposite(mainComposite);
		observationHcomp.setLayout(new GridLayout(2, false));
		toolkit.createLabel(observationHcomp, "Observations/Waypoints");
		lnkImportWaypoints = toolkit.createHyperlink(observationHcomp, "Import Waypoints ...", SWT.NONE);
		lnkImportWaypoints.addHyperlinkListener(new HyperlinkAdapter(){
			public void linkActivated(HyperlinkEvent e) {
				showImportWaypointWizard();
			}
		});

		
		observationTable = new TableViewer(mainComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
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
		btnAddWaypoint = toolkit.createButton(buttonComp, "Add Waypoint", SWT.PUSH);
		btnAddWaypoint.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addWaypoint();
			}
		});
		
		
		btnDeleteWaypoint = toolkit.createButton(buttonComp, "Delete Waypoint", SWT.PUSH);
		btnDeleteWaypoint.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				deleteSelectedWaypoints();
			}
		});
		
		btnMoveWaypoint = toolkit.createButton(buttonComp, "Move Waypoint", SWT.PUSH);
		btnMoveWaypoint.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				moveSelectedWaypoints();
			}
		});
		
		SurveyEventHandler.getInstance().addListener(EventType.MISSION_MODIFIED, trackListener);
		SurveyEventHandler.getInstance().addListener(EventType.MISSION_MODIFIED, waypointListener);
		updateTotalHours();
		
		return mainComposite;
	}

	private void setupObservationTable() {
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = observationTable.getTable().computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		observationTable.getTable().setLayoutData(gd);
		observationTable.getTable().setLinesVisible(true);
		observationTable.getTable().setHeaderVisible(true);
		observationTable.setContentProvider(new ObservableListContentProvider());
		
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
		
		final WaypointSorter waypointSorter = new WaypointSorter(observationTable);
		observationTable.setComparator(waypointSorter);
		for (int i = 0; i < OtColumn.values().length; i++) {
			final OtColumn columntype = OtColumn.values()[i];
//			if (!editor.getPatrolEditor().getOptions().getTrackDistanceDirection() && 
//					(columntype == OtColumn.DIRECTION || columntype == OtColumn.DISTANCE)){
//				continue;
//			}
			
			final TableViewerColumn column = new TableViewerColumn(observationTable,SWT.NONE);
			column.setLabelProvider(new ObsrvationTableLabelProvider(columntype));
			column.getColumn().setText(columntype.guiName);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(false);
			column.getColumn().setWidth(25);

			if(columntype != OtColumn.EAST && columntype != OtColumn.NORTH) {
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

	public void dispose() {
		SurveyEventHandler.getInstance().removeListener(EventType.MISSION_MODIFIED, trackListener);
		SurveyEventHandler.getInstance().removeListener(EventType.MISSION_MODIFIED, waypointListener);
	}
	
	public void setData(Mission data, Date date) {
		this.mission = data;
		
		Calendar cal = Calendar.getInstance();
		if (data.getStartDate() != date) {
			cal.setTime(data.getStartDate());
			dtStartTime.setTime(cal.get(Calendar.HOUR_OF_DAY),
					cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
		}else{
			dtStartTime.setTime(0,0,0);
		}
		if (data.getEndDate() != date) {
			cal.setTime(data.getEndDate());
			dtEndTime.setTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
					cal.get(Calendar.SECOND));
		}else{
			dtEndTime.setTime(23,59,59);
		}
		//TODO:
//		if (data.getRestMinutes() == null) {
//			restMinutes.setText("0"); //$NON-NLS-1$
//		} else {
//			restMinutes.setText(String.valueOf(data.getRestMinutes()));
//		}
//
//		this.lblTotalHours.setText(String.valueOf(data.getHoursWorked()));

		trackTable.setInput(mission.getTracks().toArray());
		
		if (data.getWaypoints() == null) {
			data.setWaypoints(new ArrayList<SurveyWaypoint>());
		}
		input = new ArrayList<SurveyWaypoint>();
		for (SurveyWaypoint p : data.getWaypoints()) {
			if (SmartUtils.isSameDate(p.getWaypoint().getDateTime(), date)) {
				input.add(p);
			}
		}
		WritableList inputList = new WritableList(input, SurveyWaypoint.class);
		observationTable.setInput(inputList);
		observationTable.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (editor.getMissionEditor().canEdit() == null) {
					boolean enabled = !((IStructuredSelection)observationTable.getSelection()).isEmpty();
					btnDeleteWaypoint.setEnabled(enabled);
					
//					if (patrolLegDate.getPatrolLeg().getPatrol().getLegs().size() > 1 || patrolLegDate.getPatrolLeg().getPatrolLegDays().size() > 1){
//						btnMoveWaypoint.setEnabled(enabled);	
//					}else{
//						btnMoveWaypoint.setEnabled(false);
//					}
				}
			}
		});
		
//		this.viewTrackPoints.setEnabled( this.patrolLegDate.getTrack() != null );
				
		updateTotalHours();
//		updateDistance();
		
		btnMoveWaypoint.setEnabled(false);
		btnDeleteWaypoint.setEnabled(false);
		
//		if (editor.getPatrolEditor().canEdit() != null){
//			dtEndTime.setEnabled(false);
//			dtStartTime.setEnabled(false);
//			restMinutes.setEditable(false);
//			restMinutes.setEnabled(false);
//			
//			btnAddWaypoint.setVisible(false);
//			btnDeleteWaypoint.setVisible(false);
//			btnMoveWaypoint.setVisible(false);
//			
//			importTrack.setVisible(false);
//			lblImportWaypoints.setVisible(false);
//			
//		}
	
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
		for (Iterator<SurveyWaypoint> iterator = mission.getWaypoints().iterator(); iterator.hasNext();) {
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
	}
	
	protected void showImportWaypointWizard() {
		// TODO Auto-generated method stub
		
	}

	protected void updateTotalHours() {
		// TODO Auto-generated method stub
		
	}

	public void refreshTable() {
		observationTable.refresh();
	}
	
	protected void addWaypoint() {
		double y = 0, x = 0;
		int id = -1;
		Date last = null;
		for (Iterator<SurveyWaypoint> iterator = input.iterator(); iterator.hasNext();) {
			SurveyWaypoint e = iterator.next();
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
			add = new AddWaypointDialog(Display.getCurrent().getActiveShell(), editor.getMissionEditor().getAvailableProjections());
		}else{
			add = new AddWaypointDialog(Display.getCurrent().getActiveShell(), y, x, id+1, editor.getMissionEditor().getAvailableProjections());
		}
		if (add.open() == Window.OK){
			SurveyWaypoint wp = add.getWaypoint();
			wp.setMission(mission);
			
			wp.getWaypoint().setDateTime(SmartUtils.getDatePart(editor.getDay(), false));
			
			mission.getWaypoints().add(wp);
			input.add(wp);
			
			editor.getMissionEditor().save(Collections.singleton(wp));
			SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, mission);
		}
	}

	protected void deleteSelectedWaypoints() {
		boolean doDel = MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Delete Waypoints", "Are you sure you want to delete the selected waypoints? This action cannot be undone.");
		if (!doDel){
			return;
		}
		IStructuredSelection selection = ((IStructuredSelection)observationTable.getSelection());
		ArrayList<SurveyWaypoint> deleted = new ArrayList<SurveyWaypoint>();
		
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			SurveyWaypoint w = (SurveyWaypoint) iterator.next();
			if (mission.getWaypoints().remove(w)){
				input.remove(w);
				w.setMission(null);
				deleted.add(w);
			}	
		}
		
		//delete waypoints
		Job j = editor.getMissionEditor().delete(deleted);
		j.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				//once the job is completed we can fire this event
				Display.getDefault().syncExec(new Runnable() {
					
					@Override
					public void run() {
						SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, mission);
					}
				});
			}
		});
	}

	protected void moveSelectedWaypoints() {
		// TODO Auto-generated method stub
		
	}
	
	private String getWaypointValueAsString(SurveyWaypoint element, OtColumn column) {

		Waypoint wp = element.getWaypoint();
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
				return "(None)";
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
				return "(None)";
			} else {
				return MessageFormat.format("{0,number,integer} Files", wpCnt);
			}
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
				//TODO:
				//waypoint.setDateTime(SmartUtils.combineDateTime(patrolLegDate.getDate(), new Time(((Date)value).getTime())));
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
			editor.getMissionEditor().save(Collections.singleton((SurveyWaypoint)element));
		}
		observationTable.refresh();
	}
	
	/**
	 * ColumnLabelProvider
	 * @author elitvin
	 * @since 3.0.0
	 */
	private class ObsrvationTableLabelProvider extends ColumnLabelProvider {

		private OtColumn column = null;

		public ObsrvationTableLabelProvider(OtColumn column) {
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
			case OBSERVATION:	return observationEditor;
			default:
				break;
			}
			return null;
		}

		@Override
		protected boolean canEdit(Object element) {
			//TODO:
//			if (MissionDayComposite.this.editor.getPatrolEditor().canEdit() != null){
//				return false;
//			}
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
