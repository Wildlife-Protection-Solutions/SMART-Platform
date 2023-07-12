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

import java.text.Collator;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.common.celleditor.DoubleCellEditor;
import org.wcs.smart.common.celleditor.TimeCellEditor;
import org.wcs.smart.gpx.GPSDataImport;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.ui.AttachmentCellEditor;
import org.wcs.smart.observation.ui.ObservationCellEditor;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolEventManager.EventType;
import org.wcs.smart.patrol.PatrolEventManager.IPatrolEventListener;
import org.wcs.smart.patrol.PatrolManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.importwp.PatrolImportGpsDataWizard;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.ui.PatrolEditor;
import org.wcs.smart.patrol.ui.PatrolTrackEditDialog;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.SmartStyledWizardDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite for editing patrol leg days data.  This includes modifying
 * the date/time; rest minutes; tracks and waypoints.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolLegDayInputComposite {

	private DateTime dtStartTime;
	private DateTime dtEndTime;
	private Text restMinutes;
	private Label lblTotalPatrolHours;
	private Label lblTotalFieldHours;

	private TableViewer observationTable;
	
	private PatrolDayEditor editor;
	private PatrolLegDay patrolLegDate;
	
	private Button btnAddWaypoint;
	private Button btnDeleteWaypoint;
	private Button btnMoveWaypoint;
	
	private MenuItem mnuDelete;
	private MenuItem mnuAdd;
	private MenuItem mnuMove;
	private MenuItem mnuEdit;
	
	private DoubleCellEditor doubleCellEditor;
	private DoubleCellEditor nullableDoubleCellEditor;
	private TimeCellEditor timeEditor;
	private AttachmentCellEditor attachmentEditor;
	private TextCellEditor commentEditor;
	private TextCellEditor idEditor;
	private ObservationCellEditor observationEditor;
	
	private WaypointSorter waypointSorter;
	
	private HashMap<OtColumn, TableViewerColumn> observationTableColumns;
	private Hyperlink viewTrackPoints;
	private Hyperlink importTrack;
	private Text txtDistance;
	
	private Font okayFont;
	private Font errorFont;
	
	private Projection prj;
	
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
				//reload the patrol leg day (for edit in the presentation viewer)
				PatrolLegDay pld = patrolLegDate;
				try(Session session = HibernateManager.openSession()){
					pld = session.get(PatrolLegDay.class, patrolLegDate.getUuid());
					pld.getPatrolLeg().getPatrol().getLegs().forEach(e->e.getPatrolLegDays().forEach(d->d.getWaypoints().size()));
					pld.getTracks().forEach(t->t.getDistance());
					pld.getWaypoints().forEach(pw->{
						try {
							ObservationHibernateManager.computeAttachmentLocations(pw.getWaypoint(), session);
						} catch (Exception e) {
							e.printStackTrace();
						}	
					});
					
					pld.getWaypoints().forEach(pw->{
						pw.getWaypoint().getAttachments().forEach(wa->{
							if (wa.getSignatureType() != null) wa.getSignatureType().getName();
						});
						pw.getWaypoint().getAllObservations().forEach(wo->{
							wo.getAttachments().forEach(wa->{
								if (wa.getSignatureType() != null) wa.getSignatureType().getName();	
							});
						});
					});
					setData(pld);
					
				
					//link it back into the main patrol editor object
					for (PatrolLeg pl : pld.getPatrolLeg().getPatrol().getLegs()) {
						int index = pl.getPatrolLegDays().indexOf(pld); 
						if (index < 0) continue;
						pld.setPatrolLeg(pl);
						pl.getPatrolLegDays().set(index, pld);
						
					}
				}
				PatrolLegDayInputComposite.this.patrolLegDate = pld;
				refreshObservationTable();
			}
			
		}
	};
	private IPatrolEventListener timeListener = new IPatrolEventListener() {
		@Override
		public void eventFired(int attributeChanged, Object source) {
			if (attributeChanged == PatrolEventManager.PATROL_DATES_LEG && source.equals(patrolLegDate) && !mainComposite.isDisposed()){
				mainComposite.getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						setData(patrolLegDate);
					}
				});
			}
			
		}
	};
	private Composite mainComposite;
	private Hyperlink lblImportWaypoints;
	
	
	protected enum OtColumn {
		ID(Messages.PatrolLegDayInputComposite_WaypointID_ColumnHeader, 1), 
		EAST(Messages.PatrolLegDayInputComposite_Longitude_ColumnHeader, 2), 
		NORTH(Messages.PatrolLegDayInputComposite_Latitude_ColumnHeader, 2), 
		TIME(Messages.PatrolLegDayInputComposite_Time_ColumnHeader, 2), 
		DIRECTION(Messages.PatrolLegDayInputComposite_Direction_ColumnHeader1, 1), 
		DISTANCE(Messages.PatrolLegDayInputComposite_Distance_ColumnHeader1, 1),
		PRJ(Messages.PatrolLegDayInputComposite_PrjLocationColumnHeader, 2),
		
		OBSERVATION(Messages.PatrolLegDayInputComposite_Observation_ColumnHeader, 4), 
		COMMENT(Messages.PatrolLegDayInputComposite_Comment_ColumnHeader, 3), 
		ATTACHMENTS(Messages.PatrolLegDayInputComposite_Attachment_ColumnHeader, 3),
		LAST_MODIFIED(Messages.PatrolLegDayInputComposite_LastUpdated_ColumnHeader, 3),
		LAST_MODIFIED_BY(Messages.PatrolLegDayInputComposite_LastUpdatedBy_ColumnHeader, 3),
		CM_MODEL(Messages.PatrolLegDayInputComposite_SourceCmColumnName, 3);

		protected String guiName;
		protected int weight;

		private OtColumn(String name, int weight) {
			this.guiName = name;
			this.weight = weight;
		}
	}

	
	public PatrolLegDayInputComposite(PatrolDayEditor editor, Projection viewProjection) {
		this.editor = editor;
		try{
			if (viewProjection != null) {
				prj = viewProjection;
				if (prj != null && prj.getParsedCoordinateReferenceSystem() == null){
					prj.setParsedCoordinateReferenceSystem( ReprojectUtils.stringToCrs(prj.getDefinition()) );
				}
			}
			if (prj == null){
				prj = new Projection();
				prj.setParsedCoordinateReferenceSystem(SmartDB.DATABASE_CRS);
				prj.setName(SmartDB.DATABASE_CRS.getName().toString());
			}
			
		}catch (Exception ex){
			SmartPatrolPlugIn.log(ex.getMessage(), ex);
		}
	}

	public void refreshObservationTable(){
		observationTable.refresh();
	}
	
	
	public void setData(PatrolLegDay data) {
		this.patrolLegDate = data;
		
		SmartUtils.initDateTimeWidget(dtStartTime, data.getStartTime() == null ? LocalTime.MIN : data.getStartTime());
		SmartUtils.initDateTimeWidget(dtEndTime, data.getEndTime() == null ? LocalTime.MAX : data.getEndTime());
		
		if (data.getRestMinutes() == null) {
			restMinutes.setText("0"); //$NON-NLS-1$
		} else {
			restMinutes.setText(String.valueOf(data.getRestMinutes()));
		}

		this.lblTotalPatrolHours.setText(String.valueOf(data.getPatrolHoursWorked()));
		this.lblTotalFieldHours.setText(String.valueOf(data.getFieldHoursWorked()));

		if (data.getWaypoints() == null){
			data.setWaypoints(new ArrayList<PatrolWaypoint>());
		}
		observationTable.setInput(data.getWaypoints());
		observationTable.getTable().setVisible(false);
		for (int i = 0; i < observationTable.getTable().getColumnCount(); i ++) {
			observationTable.getTable().getColumn(i).pack();
			if (observationTable.getTable().getColumn(i).getWidth() > 200) {
				observationTable.getTable().getColumn(i).setWidth(200);
			}
		}
		observationTable.getTable().setVisible(true);
		observationTable.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (editor.getPatrolEditor().canEdit() == null){
					boolean enabled = !((IStructuredSelection)observationTable.getSelection()).isEmpty();
					btnDeleteWaypoint.setEnabled(enabled);
					mnuDelete.setEnabled(enabled);
					mnuEdit.setEnabled(enabled);
					
					if (patrolLegDate.getPatrolLeg().getPatrol().getLegs().size() > 1 || patrolLegDate.getPatrolLeg().getPatrolLegDays().size() > 1){
						btnMoveWaypoint.setEnabled(enabled);
						mnuMove.setEnabled(enabled);
					}else{
						btnMoveWaypoint.setEnabled(false);
						mnuMove.setEnabled(false);
					}
				}
			}
		});
		
		this.viewTrackPoints.setEnabled( this.patrolLegDate.getTrack() != null );
				
		updateTotalHours();
		updateDistance();
		
		btnMoveWaypoint.setEnabled(false);
		btnDeleteWaypoint.setEnabled(false);
		mnuDelete.setEnabled(false);
		mnuMove.setEnabled(false);
		mnuEdit.setEnabled(false);
		
		if (editor.getPatrolEditor().canEdit() != null){
			dtEndTime.setEnabled(false);
			dtStartTime.setEnabled(false);
			restMinutes.setEditable(false);
			restMinutes.setEnabled(false);
			
			btnAddWaypoint.setVisible(false);
			btnDeleteWaypoint.setVisible(false);
			btnMoveWaypoint.setVisible(false);
			mnuDelete.setEnabled(false);
			mnuMove.setEnabled(false);
			mnuEdit.setEnabled(false);
			mnuAdd.setEnabled(false);
			
			importTrack.setVisible(false);
			lblImportWaypoints.setVisible(false);
		}
	}

	public Composite createComposite(Composite parent, FormToolkit toolkit) {
		
		mainComposite = toolkit.createComposite(parent, SWT.NONE);
		mainComposite.setLayout(new GridLayout(1, false));
		mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)mainComposite.getLayout()).marginWidth = 0;
		((GridLayout)mainComposite.getLayout()).marginHeight = 0;
		((GridLayout)mainComposite.getLayout()).verticalSpacing = 10;
		
		Composite timeInfo = toolkit.createComposite(mainComposite);
		timeInfo.setLayout(new GridLayout(4, false));
		((GridLayout) timeInfo.getLayout()).marginWidth = 0;
		((GridLayout) timeInfo.getLayout()).horizontalSpacing = 15;
		 
		timeInfo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Composite c = toolkit.createComposite(timeInfo);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		c.setLayout(new GridLayout(2, false));
		((GridLayout) c.getLayout()).marginWidth = 0;
		((GridLayout) c.getLayout()).marginHeight = 0;
		
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
				if (timeEqual(SmartUtils.toTime(dtStartTime), patrolLegDate.getStartTime())){
					//no changes made
					return;
				}
				editor.getPatrolEditor().save(patrolLegDate);
				PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_DATES_LEG, patrolLegDate);
			}
		});
		if (editor.getPatrolEditor().canEdit() == null) {
			Hyperlink btnUpdateTime = toolkit.createHyperlink(c, Messages.PatrolLegDayInputComposite_Button_UpdateTime_Text, SWT.NONE);
			btnUpdateTime.setToolTipText(Messages.PatrolLegDayInputComposite_Button_UpdateTime1_Tooltip);
			btnUpdateTime.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));
			btnUpdateTime.addHyperlinkListener(new IHyperlinkListener() {
				
				@Override
				public void linkExited(HyperlinkEvent e) {
				}
				
				@Override
				public void linkEntered(HyperlinkEvent e) {
				}
				
				@Override
				public void linkActivated(HyperlinkEvent e) {
					if (MessageDialog.openConfirm(Display.getDefault().getActiveShell(), Messages.PatrolLegDayInputComposite_ConfDialog_UpdateTime_Title, Messages.PatrolLegDayInputComposite_ConfDialog_UpdateTime_Message1)) {
						updateTimeWithWpData();
					}
				}
			});
		}
		
		c = toolkit.createComposite(timeInfo);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
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
				if (timeEqual(SmartUtils.toTime(dtEndTime), patrolLegDate.getEndTime())){
					//no changes made
					return;
				}
				editor.getPatrolEditor().save(patrolLegDate);
				PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_DATES_LEG, patrolLegDate);
			}
		});

		
		c = toolkit.createComposite(timeInfo);
		c.setLayout(new GridLayout(2, false));
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
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
		
		toolkit.createLabel(c, Messages.PatrolLegDayInputComposite_TotalPatrolHours_Label);
		lblTotalPatrolHours = toolkit.createLabel(c, Messages.PatrolLegDayInputComposite_InvalidTotalHoursPatrolled);
		toolkit.createLabel(c, Messages.PatrolLegDayInputComposite_TotalActivePatrolHours_Label);
		lblTotalFieldHours = toolkit.createLabel(c, Messages.PatrolLegDayInputComposite_InvalidTotalHoursPatrolled);
		okayFont = lblTotalFieldHours.getFont();
		
		FontData fd = okayFont.getFontData()[0];
		fd.setStyle(SWT.BOLD);
		errorFont = new Font(lblTotalFieldHours.getDisplay(), fd);
		
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.widthHint = 30;
		lblTotalPatrolHours.setLayoutData(gd);
		lblTotalFieldHours.setLayoutData(gd);

		
		
		Composite trackComp = toolkit.createComposite(mainComposite);
		trackComp.setLayout(new GridLayout(4, false));
		((GridLayout)trackComp.getLayout()).marginWidth = 0;
		((GridLayout)trackComp.getLayout()).marginHeight = 0;
		
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
				PatrolTrackEditDialog td = new PatrolTrackEditDialog(viewTrackPoints.getShell(), patrolLegDate, editor.getPatrolEditor().canEdit() == null);
				td.open();
				ApplicationGIS.getToolManager().setCurrentEditor(editor.getPatrolEditor());
			}
		});
		
		
		
		Composite observationHcomp = toolkit.createComposite(mainComposite);
		observationHcomp.setLayout(new GridLayout(2, false));
		((GridLayout)observationHcomp.getLayout()).marginWidth = 0;
		((GridLayout)observationHcomp.getLayout()).marginHeight = 0;
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
		
		Composite buttonComp = toolkit.createComposite(mainComposite);
		buttonComp.setLayout(new GridLayout(3, false));
		((GridLayout)buttonComp.getLayout()).marginWidth = 0;
		((GridLayout)buttonComp.getLayout()).marginHeight = 0;
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
		
		Menu mnu = new Menu(observationTable.getControl());
		
		mnuAdd = new MenuItem(mnu, SWT.PUSH);
		mnuAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuAdd.setText(Messages.PatrolLegDayInputComposite_AddWaypoint_Button);
		mnuAdd.addListener(SWT.Selection, e-> addWaypoint());
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		mnuEdit = new MenuItem(mnu, SWT.PUSH);
		mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		mnuEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		mnuEdit.addListener(SWT.Selection, e->{
			ViewerCell cell = (ViewerCell) observationTable.getControl().getData("ITEM"); //$NON-NLS-1$
			if (cell == null) return;
			observationTable.editElement(cell.getElement(), cell.getColumnIndex());
		});
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		mnuMove = new MenuItem(mnu, SWT.PUSH);
		mnuMove.setText(Messages.PatrolLegDayInputComposite_MoveWaypoint_Button);
		mnuMove.addListener(SWT.Selection, e-> moveSelectedWaypoints());
		
		mnuDelete = new MenuItem(mnu, SWT.PUSH);
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.setText(Messages.PatrolLegDayInputComposite_DeleteWaypoint_Button);
		mnuDelete.addListener(SWT.Selection, e-> deleteSelectedWaypoints());
		
		observationTable.getControl().addListener(SWT.MenuDetect, e->{
			Point pnt = observationTable.getControl().toControl(e.x, e.y);
			ViewerCell cell = observationTable.getCell(pnt);
			observationTable.getControl().setData("ITEM", cell); //$NON-NLS-1$
			mnu.setVisible(true);
		});
		
		PatrolEventManager.getInstance().addListener(EventType.PATROL_MODIFIED, trackListener);
		PatrolEventManager.getInstance().addListener(EventType.PATROL_MODIFIED, waypointListener);
		PatrolEventManager.getInstance().addListener(EventType.PATROL_MODIFIED, timeListener);
		updateTotalHours();
		return mainComposite;
	}
	
	private boolean timeEqual(LocalTime t1, LocalTime t2){
		return t1.equals(t2);
	}
	
	public void dispose(){
		PatrolEventManager.getInstance().removeListener(EventType.PATROL_MODIFIED, trackListener);
		PatrolEventManager.getInstance().removeListener(EventType.PATROL_MODIFIED, waypointListener);
		PatrolEventManager.getInstance().removeListener(EventType.PATROL_MODIFIED, timeListener);
		
		doubleCellEditor.dispose();
		nullableDoubleCellEditor.dispose();
		idEditor.dispose();
		timeEditor.dispose();
		attachmentEditor.dispose();
		commentEditor.dispose();
		observationEditor.dispose();
		doubleCellEditor = null;
		nullableDoubleCellEditor = null;
		idEditor = null;
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

	/**
	 * NOTE: Similar logic for all legs is located in {@link PatrolSummaryEditor} page
	 */
	protected void updateTimeWithWpData() {
		LocalDateTime[] dates = patrolLegDate.computeMinMaxDate();
		if (dates == null) return;
		
		patrolLegDate.setStartTime(dates[0].toLocalTime());
		patrolLegDate.setEndTime(dates[1].toLocalTime());
		setData(patrolLegDate);
		editor.getPatrolEditor().save(patrolLegDate);
		PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_DATES_LEG, patrolLegDate);
	}

	private void moveSelectedWaypoints(){
		MoveWaypointDialog dialog = new MoveWaypointDialog(mainComposite.getShell(), patrolLegDate.getPatrolLeg().getPatrol());
		if (dialog.open() != Window.OK ){
			return ;
		}
		ArrayList<PatrolWaypoint> deleted = new ArrayList<PatrolWaypoint>();
		ArrayList<PatrolWaypoint> added = new ArrayList<PatrolWaypoint>();
		
		final PatrolLegDay moveTo = dialog.getMoveToPosition();
		
		IStructuredSelection selection = observationTable.getStructuredSelection();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			PatrolWaypoint w = (PatrolWaypoint) iterator.next();
				
			if (patrolLegDate.getWaypoints().remove(w)) {
				PatrolWaypoint pw = new PatrolWaypoint();
				pw.setWaypoint(w.getWaypoint());
				pw.setPatrolLegDay(moveTo);
				moveTo.getWaypoints().add(pw);

				deleted.add(w);
				added.add(pw);
			}
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
		j.schedule();
		
		
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
	
	
	private void updateTotalHours(){
		if (this.patrolLegDate == null) return;
		
		double totalPatrolHoursTime = this.patrolLegDate.getPatrolHoursWorked();
		double totalFieldHoursTime = this.patrolLegDate.getFieldHoursWorked();
		
		lblTotalPatrolHours.setText(PatrolEditor.formatTimeRange(totalPatrolHoursTime));
		if (totalPatrolHoursTime < 0){
			lblTotalPatrolHours.setFont(errorFont);
			lblTotalPatrolHours.setForeground(lblTotalFieldHours.getDisplay().getSystemColor(SWT.COLOR_RED));
			lblTotalPatrolHours.setToolTipText(Messages.PatrolLegDayInputComposite_Error_StartTimeError_Tooltip);
		}else{
			lblTotalPatrolHours.setFont(okayFont);
			lblTotalPatrolHours.setForeground(lblTotalFieldHours.getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
			lblTotalPatrolHours.setToolTipText(null);
		}
		
		lblTotalFieldHours.setText(PatrolEditor.formatTimeRange(totalFieldHoursTime));
		if (totalFieldHoursTime < 0){
			lblTotalFieldHours.setFont(errorFont);
			lblTotalFieldHours.setForeground(lblTotalFieldHours.getDisplay().getSystemColor(SWT.COLOR_RED));
			lblTotalFieldHours.setToolTipText(Messages.PatrolLegDayInputComposite_Error_StartTimeError_Tooltip);
		}else{
			lblTotalFieldHours.setFont(okayFont);
			lblTotalFieldHours.setForeground(lblTotalFieldHours.getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
			lblTotalFieldHours.setToolTipText(null);
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
		idEditor = new TextCellEditor(observationTable.getTable(), SWT.SINGLE){
		    @Override
			protected Control createControl(Composite parent) {
		    	Control c = super.createControl(parent);
		    	text.setTextLimit(Waypoint.ID_MAX_LENGTH);
		    	return c;
		    }
		};
		
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
					(columntype == OtColumn.DIRECTION || columntype == OtColumn.DISTANCE || columntype == OtColumn.PRJ)){
				continue;
			}
			
			final TableViewerColumn column = new TableViewerColumn(observationTable,SWT.NONE);
			column.setLabelProvider(new ObsrvationTableLabelProvider(columntype));
			column.getColumn().setText(columntype.guiName);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(false);
			column.getColumn().setWidth(25);

			if(columntype == OtColumn.EAST || columntype == OtColumn.NORTH){
				column.getColumn().setToolTipText(prj.getName());
				if (PatrolManager.getInstance().canEditWaypointLocations() == null){
					column.setEditingSupport(new ObservationTableCellModifier(column.getViewer(), columntype));	
				}
			}else if (columntype != OtColumn.PRJ){
				column.setEditingSupport(new ObservationTableCellModifier(column.getViewer(), columntype));
			}
			if (columntype == OtColumn.DISTANCE) {
				column.getColumn().setToolTipText(Messages.PatrolLegDayInputComposite_distanceTooltip);
			}
			if (columntype == OtColumn.DIRECTION) {
				column.getColumn().setToolTipText(Messages.PatrolLegDayInputComposite_bearingTooltip);
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
		SmartStyledWizardDialog dialog = new SmartStyledWizardDialog(editor.getSite().getShell(), wizard);
		if (dialog.open() == Window.OK) {
			PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_TRACKS, this.patrolLegDate);
		}
	}
	

	private void showImportWaypointWizard(){
		//Show Create Patrol Wizard
		final PatrolImportGpsDataWizard wizard = new PatrolImportGpsDataWizard(this.patrolLegDate, GPSDataImport.ImportType.WAYPOINT);		
		SmartStyledWizardDialog dialog = new SmartStyledWizardDialog(editor.getSite().getShell(), wizard);
		if (dialog.open() == Window.OK) {
			updateTimeWithWpData();
		}
		
		
	}

	private void setWaypointValue(Object element, OtColumn column, Object value){		
		Waypoint waypoint = ((PatrolWaypoint)element).getWaypoint();
		boolean needSave = false;
		if (column == OtColumn.ID) {
			if (waypoint.getId().equals(((String)value).strip())) return; //no change
			waypoint.setId((String)value);
			needSave = true;
		} else if (column == OtColumn.EAST) {
			if (waypoint.getRawX() == ((Double)value).doubleValue()) return; // no change
			waypoint.setRawX((Double)value);
			needSave = true;
		} else if (column == OtColumn.NORTH) {
			if (waypoint.getRawY() == ((Double)value).doubleValue()) return; // no change
			waypoint.setRawY((Double)value);
			needSave = true;
		} else if (column == OtColumn.TIME) {
			if (value instanceof LocalTime){
				if (timeEqual(waypoint.getDateTime().toLocalTime(), ((LocalTime)value))) return; //no change
				waypoint.setDateTime(patrolLegDate.getDate().atTime((LocalTime)value));
				needSave = true;
			}
		} else if (column == OtColumn.DIRECTION) {
			if (value == null){
				if (waypoint.getDirection() != null) {
					waypoint.setDirection(null);
					needSave = true;
				}
			}else{
				if (waypoint.getDirection() != null && waypoint.getDirection().doubleValue() == ((Double)value).doubleValue()) return; //no change
				needSave = true;
				Double d = (Double)value;
				if (d < 0 || d >= 360) return;	//invalid value
				waypoint.setDirection(d.floatValue());
			}
		} else if (column == OtColumn.DISTANCE) {
			if (value == null){
				if (waypoint.getDistance() != null) {
					waypoint.setDistance(null);
					needSave = true;
				}
			}else{
				if (waypoint.getDistance() != null && waypoint.getDistance().doubleValue() == ((Double)value).doubleValue()) return; //no change
				needSave = true;
				Double d = (Double)value;
				if (d < 0) return;	//invalid value
				waypoint.setDistance( d.floatValue());
			}
			needSave = true;
		} else if (column == OtColumn.OBSERVATION) {
			//updated in cell editor
			needSave = false;
		} else if (column == OtColumn.COMMENT) {
			
			if (waypoint.getComment() == null && (value == null || ((String)value).trim().isEmpty())) return;
			if (waypoint.getComment() != null && waypoint.getComment().equals((String)value)) return; //no change;
			if (((String)value).trim().isEmpty()) {
				waypoint.setComment(null);
			}else {
				waypoint.setComment((String)value);
			}
			needSave = true;
		} else if (column == OtColumn.ATTACHMENTS) {
			if (value instanceof Waypoint) {
				needSave = true;
				waypoint = (Waypoint)value;
				((PatrolWaypoint)element).setWaypoint(waypoint);
			}
		}
		if (needSave){
			final Waypoint fwaypoint = waypoint; 
			IJobChangeListener listener = new IJobChangeListener() {
				@Override
				public void sleeping(IJobChangeEvent event) { }
				@Override
				public void scheduled(IJobChangeEvent event) { }
				@Override
				public void running(IJobChangeEvent event) { }
				@Override
				public void awake(IJobChangeEvent event) { }
				@Override
				public void aboutToRun(IJobChangeEvent event) { }
				
				@Override
				public void done(IJobChangeEvent event) {
					
					if (column == OtColumn.EAST || column == OtColumn.NORTH){
						//update map
						editor.getPatrolEditor().getMap().getRenderManager().refresh(null);
					}else if (column == OtColumn.ATTACHMENTS) {
						//update reference
						try(Session session = HibernateManager.openSession()){
							Waypoint waypoint = session.getReference(fwaypoint);
							((PatrolWaypoint)element).setWaypoint(waypoint);
							try {
								editor.getPatrolEditor().loadPatrolWaypointDetails(((PatrolWaypoint)element), session);
							} catch (Exception e) {
								SmartPatrolPlugIn.log(e.getMessage(), e);
							}
							
						}
									
					}
					Display.getDefault().asyncExec(()->observationTable.refresh());
				}

			};
			
			editor.getPatrolEditor().save(Collections.singleton((PatrolWaypoint)element), listener);
			
			
		}else {
			observationTable.refresh();
		}
		
	}
	
	private Object getWaypointValue(PatrolWaypoint element, OtColumn column) {

		Waypoint wp = ((PatrolWaypoint) element).getWaypoint();
		if (column == OtColumn.ID) {
			return wp.getId();
		} else if (column == OtColumn.EAST) {
			return wp.getRawX();
		} else if (column == OtColumn.NORTH) {
			return wp.getRawY();
		} else if (column == OtColumn.TIME) {
			return wp.getDateTime().toLocalTime();
		} else if (column == OtColumn.DIRECTION) {
			return wp.getDirection();
		} else if (column == OtColumn.DISTANCE) {
			return wp.getDistance();
		} else if (column == OtColumn.PRJ) {
			return wp.getX() +", " + wp.getY(); //$NON-NLS-1$
		} else if (column == OtColumn.OBSERVATION) {
			return wp;
		} else if (column == OtColumn.COMMENT) {
			if (wp.getComment() == null){
				return ""; //$NON-NLS-1$
			}
			return wp.getComment();
		} else if (column == OtColumn.ATTACHMENTS) {
			return wp;
		} else if (column == OtColumn.LAST_MODIFIED) {
			return wp.getLastModified();
		} else if (column == OtColumn.LAST_MODIFIED_BY) {
			if (wp.getLastModifiedBy() == null) return ""; //$NON-NLS-1$
			return SmartLabelProvider.getShortLabel(wp.getLastModifiedBy());
		} else if (column == OtColumn.CM_MODEL) {
			if (wp.getSourceConfigurableModel() == null) return ""; //$NON-NLS-1$
			return wp.getSourceConfigurableModel().getName();
		}
	
		return ""; //$NON-NLS-1$
	}
		
	private String getWaypointValueAsString(PatrolWaypoint element, OtColumn column) {

		Waypoint wp = ((PatrolWaypoint) element).getWaypoint();
		if (column == OtColumn.ID) {
			return wp.getId();
		} else if (column == OtColumn.EAST) {
			return String.valueOf(ReprojectUtils.transform(wp.getRawX(), wp.getRawY(), prj.getParsedCoordinateReferenceSystem()).getX());
		} else if (column == OtColumn.NORTH) {
			return String.valueOf(ReprojectUtils.transform(wp.getRawX(), wp.getRawY(), prj.getParsedCoordinateReferenceSystem()).getY());
		} else if (column == OtColumn.PRJ) {
			return wp.getX() +", " + wp.getY(); //$NON-NLS-1$
		} else if (column == OtColumn.TIME) {
			if (wp.getDateTime() != null) {
				return DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).format(wp.getDateTime().toLocalTime());
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
			String x = wp.getObservationsAsString();
			if (x == null) return Messages.PatrolLegDayInputComposite_NoObservationsLabel;
			return x;
		} else if (column == OtColumn.COMMENT) {
			if (wp.getComment() == null) {
				return ""; //$NON-NLS-1$
			}
			return wp.getComment();
		} else if (column == OtColumn.ATTACHMENTS) {
			int wpCnt = 0;
			for (WaypointObservation wo : wp.getAllObservations()) {
				if (wo.getAttachments() != null) wpCnt += wo.getAttachments().size();
			}
			if (wp.getAttachments() != null){
				wpCnt += wp.getAttachments().size();
			}
			if (wpCnt == 0 ) {
				return Messages.PatrolLegDayInputComposite_NoAttachmentments_ColumnLabel;
			} else {
				return MessageFormat.format(Messages.PatrolLegDayInputComposite_AttachmentColumnLabel, new Object[]{wpCnt});
			}
		} else if (column == OtColumn.LAST_MODIFIED) {
			if (wp.getLastModified() == null) return ""; //$NON-NLS-1$
			return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(wp.getLastModified());
		} else if (column == OtColumn.LAST_MODIFIED_BY) {
			if (wp.getLastModifiedBy() == null) return ""; //$NON-NLS-1$
			return SmartLabelProvider.getShortLabel(wp.getLastModifiedBy());
		}else if (column == OtColumn.CM_MODEL) {
			if (wp.getSourceConfigurableModel() == null) return ""; //$NON-NLS-1$
			return wp.getSourceConfigurableModel().getName();
		}
	

		return ""; //$NON-NLS-1$
	}
	
	/**
	 * update patrol leg day values 
	 * @param session
	 */
	public void updateLegDay() {
		
		patrolLegDate.setEndTime(SmartUtils.toTime(dtEndTime));
		patrolLegDate.setStartTime(SmartUtils.toTime(dtStartTime));
		
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
		String id = "-1"; //$NON-NLS-1$
		LocalTime last = null;
		for (Iterator<PatrolWaypoint> iterator = PatrolLegDayInputComposite.this.patrolLegDate.getWaypoints().iterator(); iterator.hasNext();) {
			PatrolWaypoint e = (PatrolWaypoint) iterator.next();
			LocalTime t = (LocalTime)getWaypointValue(e, OtColumn.TIME);
			
			if(last == null || t.isAfter(last) || t.equals(last)  ){
				y = (Double) getWaypointValue(e, OtColumn.NORTH);
				x = (Double) getWaypointValue(e, OtColumn.EAST);
				id = (String)getWaypointValue(e, OtColumn.ID);
				last = t;
			}
		}
		AddWaypointDialog add;
	
		if(x == 0 && y == 0){
			add = new AddWaypointDialog(mainComposite.getShell(), editor.getPatrolEditor().getAvailableProjections());
		}else{
			String nextid = String.valueOf(PatrolLegDayInputComposite.this.patrolLegDate.getWaypoints().size() + 1);
			try {
				nextid = String.valueOf(Integer.parseInt(id) + 1);
			}catch (Exception ex) {}
			
			add = new AddWaypointDialog(mainComposite.getShell(), y, x, nextid, editor.getPatrolEditor().getAvailableProjections());
		}
		if (add.open() == Window.OK){
			PatrolWaypoint wp = add.getWaypoint();
			wp.setPatrolLegDay(patrolLegDate);
			
			wp.getWaypoint().setDateTime(LocalDateTime.of(patrolLegDate.getDate(), LocalTime.MIN));
			
			patrolLegDate.getWaypoints().add(wp);
			
			IJobChangeListener listener = new IJobChangeListener() {
				@Override
				public void sleeping(IJobChangeEvent event) { }
				@Override
				public void scheduled(IJobChangeEvent event) { }
				@Override
				public void running(IJobChangeEvent event) { }
				@Override
				public void awake(IJobChangeEvent event) { }
				@Override
				public void aboutToRun(IJobChangeEvent event) { }
				@Override
				public void done(IJobChangeEvent event) {
					Display.getDefault().asyncExec(()->{
						PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_WAYPOINTS, patrolLegDate);
						observationTable.refresh();
					});
				}

			};
			
			editor.getPatrolEditor().save(Collections.singleton(wp), listener);
			
		}
	}
	
	/**
	 * If the patrol waypoint applies to the patrol leg day
	 * associated with this composite, it selects it in the table
	 * and returns the table control.  Otherwise returns null; 
	 * @param pw
	 * @return
	 */
	public Control selectWaypoint(PatrolWaypoint pw){
		if (pw.getPatrolLegDay().equals(patrolLegDate)){
			observationTable.getTable().setFocus();
			observationTable.setSelection(new StructuredSelection(pw));
			observationTable.getTable().showSelection();
			return observationTable.getControl();
		}
		return null;
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
				return idEditor;
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
							return Collator.getInstance().compare(SmartLabelProvider.getFullLabel(arg0).toUpperCase(), SmartLabelProvider.getFullLabel(arg1).toUpperCase());
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
			if (column == OtColumn.LAST_MODIFIED) return false;
			if (column == OtColumn.LAST_MODIFIED_BY) return false;
			if (column == OtColumn.CM_MODEL) return false;
			
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
