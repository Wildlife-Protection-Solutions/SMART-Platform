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

import java.sql.Time;
import java.text.Collator;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolEventManager.EventType;
import org.wcs.smart.patrol.PatrolEventManager.IPatrolEventListener;
import org.wcs.smart.patrol.PatrolUtils;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.ArmedComposite;
import org.wcs.smart.patrol.internal.ui.CommentComposite;
import org.wcs.smart.patrol.internal.ui.DateComposite;
import org.wcs.smart.patrol.internal.ui.EmployeeLeaderPilotComposite;
import org.wcs.smart.patrol.internal.ui.ObjectiveComposite;
import org.wcs.smart.patrol.internal.ui.PatrolIdComposite;
import org.wcs.smart.patrol.internal.ui.PatrolItemComposite;
import org.wcs.smart.patrol.internal.ui.PatrolLegsComposite;
import org.wcs.smart.patrol.internal.ui.PatrolMandateComposite;
import org.wcs.smart.patrol.internal.ui.PatrolTransportComposite;
import org.wcs.smart.patrol.internal.ui.createpatrol.EmployeeLabelProvider;
import org.wcs.smart.patrol.internal.ui.editor.PatrolLegDayLabelProvider.PatrolLegDayColumn;
import org.wcs.smart.patrol.internal.ui.editpatrol.EditPatrolItemDialog;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.ui.PatrolEditor;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.patrol.ui.StationComposite;
import org.wcs.smart.patrol.ui.TeamComposite;

/**
 * Editor part for displaying and editing patrol information.
 * @author Emily
 * @since 1.0.0
 */
public class PatrolSummaryEditor extends EditorPart {

	/**
	 * 
	 */
	private static final String EDIT_LABEL = PatrolUtils.EDIT_LINK_TEXT;
	private static final int WIDTH_HINT = 50;	//width hint for fields
	private static final int EMPLOYEE_LIST_HEIGHT_HINT = 50;
	
	public static final String ID = "org.wcs.smart.patrol.ui.PatrolSummaryEditor"; //$NON-NLS-1$

	private boolean isDirty = false;
	private FormText txtPatrolType;
	private FormText txtPatrolId;
	private Text txtMandate;
	private Text txtStation;
	private Text txtTeam;
	private Text txtObjective;
	private Text txtComment;
	private Text txtStartDate;
	private Text txtEndDate;
	private Text txtTransport;
	
	private Button btnArmed;
	
	private Hyperlink editId;
	private Hyperlink editObjective;
	private Hyperlink editComment;
	private Hyperlink editEmployee;
	private Hyperlink editDates;
	private TableViewer employeeList;
	private Form frmPatrolSummary;

	private PatrolEditor editor;
	private TableViewer tblPatrolData;
	private FormToolkit toolkit;
	
	private Label lblStats;
	
	private boolean isMulti = false;
	
	private HashMap<PatrolLegDayColumn, TableViewerColumn> tableColumns = new HashMap<PatrolLegDayColumn, TableViewerColumn>();

	/**
	 * listener for patrol change events.
	 */
	private IPatrolEventListener modifyListener = new IPatrolEventListener(){
		@Override
		public void eventFired(int attributeChanged, Object source) {
			initValues();
			PatrolEditorInput input = ((PatrolEditorInput) getEditorInput());
			input.setId(editor.getPatrol().getId());
			editor.updatePartName();
		}
	};

	/**
	 * Creates a new summary editor page
	 * @param editor parent editor
	 */
	public PatrolSummaryEditor(PatrolEditor editor) {
		PatrolEventManager.getInstance().addListener(EventType.PATROL_MODIFIED, modifyListener);
		super.setPartName(Messages.PatrolSummaryEditor_Summary_TabName);
		this.editor = editor;
	}

	@Override
	public void dispose() {
		if (toolkit != null){
			toolkit.dispose();
			toolkit = null;
		}
		PatrolEventManager.getInstance().removeListener(EventType.PATROL_MODIFIED, modifyListener);
		super.dispose();
	}
	
	private Composite outline;
	/**
	 * Create contents of the editor part.
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		toolkit.setBorderStyle(SWT.BORDER);

		outline = toolkit.createComposite(parent);
		outline.setLayout(new GridLayout());
		outline.setLayoutData(new GridData(SWT.FILL, SWT.FILL ,true, true));
		
		toolkit.paintBordersFor(outline);
		
		frmPatrolSummary = toolkit.createForm(outline);
		frmPatrolSummary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		GridLayout layout = new GridLayout(1, true);
		frmPatrolSummary.getBody().setLayout(layout);
		
		String canEdit = editor.canEdit();
		if (canEdit != null){
			Composite warning = toolkit.createComposite(frmPatrolSummary.getBody());
			warning.setLayout(new GridLayout(2, false));
			Label lblImage = toolkit.createLabel(warning, null, SWT.NONE);
			Image x = editor.getSite().getWorkbenchWindow().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
			lblImage.setImage(x);
			Label lblWarning = toolkit.createLabel(warning, "", SWT.NONE); //$NON-NLS-1$
			lblWarning.setText(MessageFormat.format(Messages.PatrolSummaryEditor_Error_CannotEdit, new Object[]{ canEdit }));
		}
		
		SashForm sashForm = new SashForm(frmPatrolSummary.getBody(), SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(sashForm);
		
		Section patrolSection = toolkit.createSection(sashForm, Section.TITLE_BAR | Section.EXPANDED );
		patrolSection.setText(Messages.PatrolSummaryEditor_PatrolInfo_SectionHeader);
		patrolSection.setDescription(Messages.PatrolSummaryEditor_PatrolInfo_SectionDescription);
		patrolSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		ScrolledComposite scrolltop = new ScrolledComposite(patrolSection, SWT.V_SCROLL | SWT.H_SCROLL);
		scrolltop.setLayout(new GridLayout());
		scrolltop.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrolltop.setExpandHorizontal(true);
		scrolltop.setExpandVertical(true);
				
		patrolSection.setClient(scrolltop);
		
		Composite top = toolkit.createComposite(scrolltop);
		top.setLayout(new GridLayout(2, true));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrolltop.setContent(top);
		
		Composite left = toolkit.createComposite(top, SWT.NONE);
		GridLayout leftLayout = new GridLayout(3, false);
		leftLayout.verticalSpacing = 10;
		left.setLayout(leftLayout);
		left.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)left.getLayout()).marginRight = 10;
		
		Composite right = toolkit.createComposite(top, SWT.NONE);
		GridLayout rightLayout = new GridLayout(3, false);
		rightLayout.horizontalSpacing = 15;
		rightLayout.verticalSpacing = 20;
		right.setLayout(rightLayout);
		right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)right.getLayout()).marginLeft = 10;
		
		/* left side */		
		toolkit.createLabel(left, Messages.PatrolSummaryEditor_PatrolType_Label);
		txtPatrolType = toolkit.createFormText(left, false);
		txtPatrolType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		Label lbl = null;
		
		if (editor.getPatrol().getLegs().size() <= 1 ){
			lbl = toolkit.createLabel(left, Messages.PatrolSummaryEditor_TransportType_Label);
			txtTransport = toolkit.createText(left, "", SWT.NONE); //$NON-NLS-1$
			txtTransport.setEditable(false);
			txtTransport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			createEditLink(toolkit, left, new PatrolTransportComposite());
		}
		
		lbl = toolkit.createLabel(left, Messages.PatrolSummaryEditor_Armed_Label);
		btnArmed = toolkit.createButton(left, null, SWT.CHECK);
		btnArmed.setEnabled(false);
		btnArmed.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(toolkit, left, new ArmedComposite()); 
		
		
		lbl = toolkit.createLabel(left, Messages.PatrolSummaryEditor_Mandate_Label);
		txtMandate = toolkit.createText(left, "", SWT.NONE); //$NON-NLS-1$
		txtMandate.setEditable(false);
		txtMandate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtMandate.getLayoutData()).widthHint = WIDTH_HINT;
		createEditLink(toolkit, left, new PatrolMandateComposite()); 
		
		lbl = toolkit.createLabel(left, Messages.PatrolSummaryEditor_Team_Label);
		txtTeam= toolkit.createText(left, ""); //$NON-NLS-1$
		txtTeam .setEditable(false);
		txtTeam .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtTeam.getLayoutData()).widthHint = WIDTH_HINT;
		createEditLink(toolkit, left, new TeamComposite()); 
	
		lbl = toolkit.createLabel(left, Messages.PatrolSummaryEditor_Station_Label);
		txtStation = toolkit.createText(left, ""); //$NON-NLS-1$
		txtStation .setEditable(false);
		txtStation .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtStation.getLayoutData()).widthHint = WIDTH_HINT;
		createEditLink(toolkit, left, new StationComposite()); 

		lbl = toolkit.createLabel(left, Messages.PatrolSummaryEditor_Member_Label);
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		
		Table employeeTable = toolkit.createTable(left, SWT.V_SCROLL | SWT.H_SCROLL);
		employeeList = new TableViewer(employeeTable);
		employeeList.setContentProvider(ArrayContentProvider.getInstance());
		employeeList.setLabelProvider(new EmployeeLabelProvider());
		employeeTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		//note: layout data on employeeTable is set up below
		
		/* right side */
		toolkit.createLabel(right, Messages.PatrolSummaryEditor_PatrolId_Label);
		txtPatrolId = toolkit.createFormText(right, false);
		txtPatrolId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtPatrolId.getLayoutData()).widthHint = WIDTH_HINT;
		txtPatrolId.setData(FormToolkit.KEY_DRAW_BORDER, null);
		editId = createEditLink(toolkit, right, new PatrolIdComposite());
		editId.setLayoutData(new GridData(SWT.END, SWT.BOTTOM, false, false, 1, 1));
		
		lbl = toolkit.createLabel(right, Messages.PatrolSummaryEditor_Objective_Label);
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		txtObjective = toolkit.createText(right, "", SWT.MULTI | SWT.WRAP | SWT.V_SCROLL); //$NON-NLS-1$
		txtObjective .setEditable(false);
		txtObjective .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		((GridData)txtObjective.getLayoutData()).widthHint = WIDTH_HINT;
		((GridData)txtObjective.getLayoutData()).heightHint = 100;
		editObjective = createEditLink(toolkit, right, new ObjectiveComposite());
		editObjective.setLayoutData(new GridData(SWT.END, SWT.BOTTOM, false, false, 1, 1));
		
		lbl = toolkit.createLabel(right, Messages.PatrolSummaryEditor_Comment_Label);
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		txtComment = toolkit.createText(right, "", SWT.MULTI | SWT.WRAP | SWT.V_SCROLL); //$NON-NLS-1$
		txtComment.setEditable(false);
		txtComment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		((GridData)txtComment.getLayoutData()).widthHint = WIDTH_HINT;
		((GridData)txtComment.getLayoutData()).heightHint = 100;
		editComment = createEditLink(toolkit, right, new CommentComposite());
		editComment.setLayoutData(new GridData(SWT.END, SWT.BOTTOM, false, false,1,1));

		/* ----- Patrol Days / Legs Section ------- */
		Section dataSection = toolkit.createSection(sashForm, Section.TITLE_BAR | Section.EXPANDED  );
		dataSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dataSection.setText(Messages.PatrolSummaryEditor_PatrolData_SectionName);
		dataSection.setDescription(Messages.PatrolSummaryEditor_PatrolData_SectionDescription);
		
		dataSection.setLayout(new GridLayout(1, false));
		Composite compData = toolkit.createComposite(dataSection, SWT.NONE );
		compData.setLayout(new GridLayout(1, false));
		compData.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite comp = toolkit.createComposite(compData, SWT.NONE );
		comp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		comp.setLayout(new GridLayout(6, false));
		toolkit.createLabel(comp, Messages.PatrolSummaryEditor_StartDate_Label);
		txtStartDate = toolkit.createText(comp, ""); //$NON-NLS-1$
		txtStartDate.setEditable(false);
		
		txtStartDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		lbl = toolkit.createLabel(comp, Messages.PatrolSummaryEditor_EndDate_Label);
		
		txtEndDate = toolkit.createText(comp, ""); //$NON-NLS-1$
		txtEndDate .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtEndDate.setEditable(false);
		
		editDates = createEditLink(toolkit, comp, null);
		editDates.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				if (!isMulti){
					if (showEditDialog(new DateComposite())){
						editor.createDayPages();
					}
				}else{
					//multi leg
					if (showEditDialog(new PatrolLegsComposite(true))){
						editor.createDayPages();
					}
				}
			}
		});
		
		Button btnUpdateTime = toolkit.createButton(comp, Messages.PatrolSummaryEditor_Button_UpdateTime, SWT.PUSH);
		btnUpdateTime.setToolTipText(Messages.PatrolSummaryEditor_Button_UpdateTime_Tooltip);
		btnUpdateTime.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		btnUpdateTime.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (MessageDialog.openConfirm(Display.getDefault().getActiveShell(), Messages.PatrolSummaryEditor_ConfDialog_UpdateTime_Title, Messages.PatrolSummaryEditor_ConfDialog_UpdateTime_Message)) {
					updateTimeWithWpData();
				}
			}
		});
		
		if (editor.getPatrol().getLegs().size() <=1 ){
			//single leg patrol
			//update employee section as we edit it here
			isMulti = false;
			employeeTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
			editEmployee = createEditLink(toolkit, left, new EmployeeLeaderPilotComposite());
			editEmployee.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		}else{
			//multi-day patrol
			isMulti = true;
			employeeTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
			lbl = toolkit.createLabel(top, Messages.PatrolSummaryEditor_MultiLegPatrol_Label,SWT.WRAP);
			lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		}
		((GridData)employeeTable.getLayoutData()).widthHint = WIDTH_HINT;
		((GridData)employeeTable.getLayoutData()).heightHint = EMPLOYEE_LIST_HEIGHT_HINT;
	
		/* --- Patrol days table  ---*/
		Composite compTable = toolkit.createComposite(compData);
		compTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		TableColumnLayout tableLayout = new TableColumnLayout();
		compTable.setLayout(tableLayout);
		
		tblPatrolData = new TableViewer(compTable, SWT.BORDER| SWT.FULL_SELECTION);
		toolkit.adapt(tblPatrolData.getTable());
		
		tblPatrolData.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblPatrolData.getTable().setLinesVisible(true);
		tblPatrolData.getTable().setHeaderVisible(true);
		((GridData)tblPatrolData.getTable().getLayoutData()).heightHint = 100;
		
		for (int i = 0; i < PatrolLegDayColumn.values().length; i++){
			PatrolLegDayColumn columntype = PatrolLegDayColumn.values()[i];
			TableViewerColumn column = new TableViewerColumn(tblPatrolData,SWT.NONE);
			column.setLabelProvider(new PatrolLegDayLabelProvider(columntype));
			column.getColumn().setText(columntype.name);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(false);
			TableColumnLayout collayout = (TableColumnLayout) tblPatrolData.getTable().getParent().getLayout();
			collayout.setColumnData(column.getColumn(), new ColumnWeightData(columntype.weight, ColumnWeightData.MINIMUM_WIDTH, true));
			tableColumns.put(columntype, column);
		}
		
		
		tblPatrolData.setContentProvider(ArrayContentProvider.getInstance());
		tblPatrolData.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				PatrolLegDay pld = (PatrolLegDay)((StructuredSelection)tblPatrolData.getSelection()).getFirstElement();
				if (pld != null){
					PatrolDayEditorInput input = new PatrolDayEditorInput(pld.getDate());
					IEditorPart[] parts = editor.findEditors(input);
					if (parts.length == 0){
						SmartPatrolPlugIn.displayLog(Messages.PatrolSummaryEditor_Error_CouldNotFindEditor, null);
					}else{
						editor.setActiveEditor(parts[0]);
					}
				}
				
			}
		});
		dataSection.setClient(compData);

		Composite statsCmp = toolkit.createComposite(compData, SWT.NONE );
		statsCmp.setLayoutData(new GridData(SWT.TRAIL, SWT.TOP, true, false));
		statsCmp.setLayout(new GridLayout(1, false));
		lblStats = toolkit.createLabel(statsCmp, ""); //$NON-NLS-1$
		
		Point p = top.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		scrolltop.setMinSize(p.x, p.y+20);
		
		sashForm.setWeights(new int[]{70,30});
		
		
		initValues();
	}

	/**
	 * Creates an edit hyperlink button
	 * @param tolkit toolkit
	 * @param parent parent composite
	 * @param partEditor editor to use
	 * @return hyperlink created
	 */
	private Hyperlink createEditLink(FormToolkit toolkit, Composite parent, final PatrolItemComposite partEditor ){
		Hyperlink editLink = toolkit.createHyperlink(parent, EDIT_LABEL, SWT.WRAP);
		
		if (editor.canEdit() != null){
			editLink.setEnabled(false);
			editLink.setVisible(false);
		}else {
			if (partEditor != null){
				editLink.setToolTipText(MessageFormat.format(Messages.PatrolSummaryEditor_Edit_Tooltip, new Object[]{partEditor.getTitle()}));
			}
		}
		
		if (partEditor != null){
			editLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					showEditDialog(partEditor);
				}
			});
		}
		return editLink;
	}
	
	
	/**
	 * Displays and edit dialog for editing a particular
	 * patrol element.
	 * 
	 * @param comp patrol item editor composite
	 * @return  true if changes made, false otherwise
	 */
	private boolean showEditDialog(final PatrolItemComposite comp){
		
		int ret = -1;
		try{
			final EditPatrolItemDialog editDialog = new EditPatrolItemDialog(getEditorSite().getShell(), comp, editor.getPatrol());
			ret = editDialog.open();
		}finally{
		}
		
		return ret == IDialogConstants.OK_ID;
	}
	
	
	@Override
	public void setFocus() {
		txtPatrolId.setFocus();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		//nothing to do
	}

	@Override
	public void doSaveAs() {
		// Do the Save As operation
	}

	/**
	 * NOTE: Similar logic for single leg is located in {@link PatrolLegDayInputComposite}
	 */
	protected void updateTimeWithWpData() {
		List<PatrolLegDay> updatedLegDays = new ArrayList<>();
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			Patrol patrol = editor.getPatrol();
			session.update(patrol);
			for (PatrolLeg leg : patrol.getLegs()) {
				for (PatrolLegDay pld : leg.getPatrolLegDays()) {
					List<PatrolWaypoint> wps = pld.getWaypoints();
					if (wps.isEmpty()) continue;
					List<Date> dates = wps.stream().map(pwp -> pwp.getWaypoint().getDateTime()).collect(Collectors.toList());
					Date minDate = Collections.min(dates);
					Date mmaxDate = Collections.max(dates);
					pld.setStartTime(new Time(minDate.getTime()));
					pld.setEndTime(new Time(mmaxDate.getTime()));
					session.saveOrUpdate(pld);
					updatedLegDays.add(pld);
				}
			}
			session.getTransaction().commit();
		} catch (Exception ex) {
			if (session.getTransaction().isActive()){
				session.getTransaction().rollback();
			}
			SmartPatrolPlugIn.displayLog(Messages.PatrolEditor_Error_SavingPatrol + ex.getLocalizedMessage(), ex);
			return;
		} finally {
			session.close();
		}
		//fire events
		for (PatrolLegDay pld : updatedLegDays) {
			PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_DATES_LEG, pld);
		}
	}

	/**
	 * Updates the widgets with the value from the patrol.
	 */
	private void initValues(){
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			Patrol patrol = editor.getPatrol();
			session.update(patrol);
			frmPatrolSummary.setText(editor.getPatrol().getId());
			
			txtPatrolId.setText(patrol.getId(), false, false);
			txtPatrolType.setText(patrol.getPatrolType().getGuiName(Locale.getDefault()), false, false);
			if (patrol.getStation() == null) {
				txtStation.setText(Messages.PatrolSummaryEditor_NoStationLabel);
			} else {
				txtStation.setText(patrol.getStation().getName());
			}
			if (patrol.getMandate() != null) {
				txtMandate.setText(patrol.getMandate().getName());
			} else {
				txtMandate.setText(Messages.PatrolSummaryEditor_NoManadateLabel);
			}
			if (patrol.getComment() != null){
				txtComment.setText(patrol.getComment());
			}else{
				txtComment.setText(""); //$NON-NLS-1$
			}
			btnArmed.setSelection(patrol.isArmed());

			if (patrol.getTeam() != null) {
				txtTeam.setText(patrol.getTeam().getName());
			} else {
				txtTeam.setText(Messages.PatrolSummaryEditor_NoTeamLabel);
			}

			if (patrol.getObjective() != null) {
				txtObjective.setText(patrol.getObjective());
			} else {
				txtObjective.setText(""); //$NON-NLS-1$
			}

			Set<Employee> allEmployee = new HashSet<Employee>();
			Set<Employee> leaders = new HashSet<Employee>();
			Set<Employee> pilots = new HashSet<Employee>();
			for (int i = 0; i < patrol.getLegs().size(); i++) {
				session.update(patrol.getLegs().get(i));
				List<PatrolLegMember> members = patrol.getLegs().get(i)
						.getMembers();
				for (PatrolLegMember mem : members) {
					allEmployee.add(mem.getMember());
					if (mem.getIsLeader()) {
						leaders.add(mem.getMember());
					}
					if (mem.getIsPilot()) {
						pilots.add(mem.getMember());
					}
				}
			}
			
			
			((EmployeeLabelProvider) employeeList.getLabelProvider())
					.setLeaders(leaders);
			((EmployeeLabelProvider) employeeList.getLabelProvider())
					.setPilots(pilots);
			
			Employee[] employeeArray = allEmployee.toArray(new Employee[allEmployee.size()]);
			Arrays.sort(employeeArray, new Comparator<Employee>(){
				@Override
				public int compare(Employee o1, Employee o2) {
					return Collator.getInstance().compare(
							org.wcs.smart.ui.SmartLabelProvider.getFullLabel(o1), 
							org.wcs.smart.ui.SmartLabelProvider.getFullLabel(o2));
				}});
			employeeList.setInput(employeeArray);

			updateDateTable();
			if (!isMulti){
				txtTransport.setText(patrol.getFirstLeg().getType().getName());
			}
		}finally{
			session.getTransaction().rollback();
			session.close();
		}
	}
	
	private void updateDateTable(){
		final Patrol patrol = editor.getPatrol();
		final List<PatrolLegDay> input = new ArrayList<PatrolLegDay>();
		if (!isMulti){
			//multi leg patrol
			input.addAll(patrol.getFirstLeg().getPatrolLegDays());
		} else {
			for (PatrolLeg leg : patrol.getLegs()) {
				for (PatrolLegDay pld : leg.getPatrolLegDays()) {
					input.add(pld);
				}
			}
			//sort input 
			Collections.sort(input, new Comparator<PatrolLegDay>(){
				@Override
				public int compare(PatrolLegDay d1, PatrolLegDay d2) {
					int val = d1.getDate().compareTo(d2.getDate());
					if (val == 0){
						val = d1.getStartTime().compareTo(d2.getStartTime());
						if (val == 0){
							return Collator.getInstance().compare(d1.getPatrolLeg().getId(), d2.getPatrolLeg().getId());
						}
						return val;
					}else{
						return val;
					}
				}});
		}
		for(PatrolLegDay pld : input){
			for (PatrolLegMember employee: pld.getPatrolLeg().getMembers()){
				org.wcs.smart.ui.SmartLabelProvider.getFullLabel(employee.getMember());
			}
		}
		
		tblPatrolData.getTable().getDisplay().syncExec(new Runnable(){
			@Override
			public void run() {
				//update dates
				txtStartDate.setText(DateFormat.getDateInstance(DateFormat.LONG)
						.format(patrol.getStartDate()));
				txtEndDate.setText(DateFormat.getDateInstance(DateFormat.LONG)
						.format(patrol.getEndDate()));
				
				
				tblPatrolData.setInput(input);
				updateTableLayout();

				tblPatrolData.refresh();
			}});
			
		updateOverallStatistics();

	}
	
	private void updateTableLayout(){
		if (!isMulti){
			//hide all multi columns
			TableColumnLayout collayout = (TableColumnLayout) tblPatrolData.getTable().getParent().getLayout();
			for (Iterator<Entry<PatrolLegDayColumn, TableViewerColumn>> iterator = tableColumns.entrySet().iterator(); iterator.hasNext();) {
				Entry<PatrolLegDayColumn, TableViewerColumn> info = (Entry<PatrolLegDayColumn, TableViewerColumn>) iterator.next();
				if (info.getKey().multi) {
					collayout.setColumnData(info.getValue().getColumn(), new ColumnWeightData(0, 0, false));
				}
			}
			if (editEmployee != null) editEmployee.setVisible(true);
		}else{
			TableColumnLayout collayout = (TableColumnLayout) tblPatrolData.getTable().getParent().getLayout();
			for (Iterator<Entry<PatrolLegDayColumn, TableViewerColumn>> iterator = tableColumns.entrySet().iterator(); iterator.hasNext();) {
				Entry<PatrolLegDayColumn, TableViewerColumn> info = (Entry<PatrolLegDayColumn, TableViewerColumn>) iterator.next();
				if (info.getKey().multi) {
					collayout.setColumnData(info.getValue().getColumn(), new ColumnWeightData(1, 50, true));
				}
			}
			//multi patrol; show pilot if applicable
			if (!editor.getPatrol().hasPilot()) {
				TableViewerColumn tcolumn = tableColumns.get(PatrolLegDayColumn.PILOT);
				collayout.setColumnData(tcolumn.getColumn(),new ColumnWeightData(0, 0, false));
			}
			if (editEmployee != null) editEmployee.setVisible(false);
		}
		
		tblPatrolData.getTable().getParent().layout(true,true);
	}

	private void updateOverallStatistics() {
		final Patrol patrol = editor.getPatrol();
		float distance = 0;
		double totalTime = 0;
		double activeTime = 0;
		for (PatrolLeg leg : patrol.getLegs()) {
			for (PatrolLegDay pld : leg.getPatrolLegDays()) {
				Track track = pld.getTrack();
				if (track != null && track.getDistance() != null) {
					distance += track.getDistance();
				}
				totalTime += pld.getPatrolHoursWorked();
				activeTime += pld.getFieldHoursWorked();
			}
		}
		final String statText = MessageFormat.format(Messages.PatrolSummaryEditor_OverallStatistics, String.valueOf(distance), PatrolEditor.formatTimeRange(totalTime), PatrolEditor.formatTimeRange(activeTime));
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				if (!lblStats.isDisposed()) {
					lblStats.setText(statText);
					lblStats.getParent().layout(true, true);
				}
			}
		});
	}

	/**
	 * Refresh the patrol summary table.
	 * <p>May be called from outside the display thread.</p>
	 */
	public void refreshPatrolSummaryTable(){
		if (!tblPatrolData.getTable().isDisposed()){
			isMulti = editor.getPatrol().getLegs().size() > 1;
			updateDateTable();
		}
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (!(input instanceof PatrolEditorInput)){
			throw new RuntimeException("Invalid editor input."); //$NON-NLS-1$
		}
		setSite(site);
		setInput(input);
	}

	public void setDirty(boolean isDirty){
		this.isDirty = isDirty;
	}
	
	@Override
	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

}

/**
 * Label provider for patrol day summary table
 * @author Emily
 *
 */
class PatrolLegDayLabelProvider extends ColumnLabelProvider{
	
	public enum PatrolLegDayColumn{
		LEG(Messages.PatrolSummaryEditor_LegId_ColumnName, 1, true),
		DAY(Messages.PatrolSummaryEditor_LegDay_ColumnName, 1, false),
		START(Messages.PatrolSummaryEditor_LegStart_ColumnName, 1, false),
		END(Messages.PatrolSummaryEditor_LegEnd_ColumnName, 1, false),
		DISTANCE(Messages.PatrolSummaryEditor_LegDistance_ColumnName, 1, false),
		TOTALPATROLHOURS(Messages.PatrolSummaryEditor_LegTotalPatrolHours_ColumnName, 1, false),
		TOTALHOURSINFIELD(Messages.PatrolSummaryEditor_LegTotalActivePatrolHours_ColumnName, 1, false),
		TRANSPORT(Messages.PatrolSummaryEditor_LegTransport_ColumnName, 1, true),
		LEADER(Messages.PatrolSummaryEditor_LegLeader_ColumnName, 1, true),
		PILOT(Messages.PatrolSummaryEditor_LegPilot_ColumnName, 1, true);
		
		int weight;
		String name;
		boolean multi;
		private PatrolLegDayColumn(String name, int weight, boolean multi){
			this.name = name;
			this.weight = weight;
			this.multi = multi;
		}
	};
	
	
	private PatrolLegDayColumn column = null;
	private SimpleDateFormat dayOfWeekFormatter = new SimpleDateFormat("E"); //$NON-NLS-1$
	public PatrolLegDayLabelProvider(PatrolLegDayColumn column){
		this.column = column;
	}
	
	
	public String getText(Object element) {
		if (element instanceof PatrolLegDay){
			if (column == PatrolLegDayColumn.DAY){
				Date d = ((PatrolLegDay) element).getDate();
				return DateFormat.getDateInstance(DateFormat.MEDIUM).format(d) + " " + dayOfWeekFormatter.format(d) ; //$NON-NLS-1$
			}else if (column == PatrolLegDayColumn.DISTANCE){
				if (((PatrolLegDay) element).getTrack() != null){
					return String.valueOf( ((PatrolLegDay) element).getTrack().getDistance() );
				}else{
					return "0"; //$NON-NLS-1$
				}
			}else if (column == PatrolLegDayColumn.START){
				if (((PatrolLegDay) element).getStartTime() != null){
					return DateFormat.getTimeInstance(DateFormat.MEDIUM).format(((PatrolLegDay) element).getStartTime() );
				}else{
					return ""; //$NON-NLS-1$
				}
			}else if (column == PatrolLegDayColumn.END){
				if (((PatrolLegDay) element).getEndTime() != null){
					return DateFormat.getTimeInstance(DateFormat.MEDIUM).format(((PatrolLegDay) element).getEndTime() );
				}else{
					return ""; //$NON-NLS-1$
				}
			}else if (column == PatrolLegDayColumn.TOTALPATROLHOURS){
				double hrs = ((PatrolLegDay) element).getPatrolHoursWorked();
				return PatrolEditor.formatTimeRange(hrs);
			}else if (column == PatrolLegDayColumn.TOTALHOURSINFIELD){
				double hrs = ((PatrolLegDay) element).getFieldHoursWorked();
				return PatrolEditor.formatTimeRange(hrs);
			}else if (column == PatrolLegDayColumn.LEG){
				return ((PatrolLegDay)element).getPatrolLeg().getId();
			}else if (column == PatrolLegDayColumn.LEADER){
				if (((PatrolLegDay)element).getPatrolLeg().getLeader() == null){
					return ""; //$NON-NLS-1$
				}
				return org.wcs.smart.ui.SmartLabelProvider.getFullLabel(((PatrolLegDay)element).getPatrolLeg().getLeader().getMember());
			}else if (column == PatrolLegDayColumn.PILOT){
				if (((PatrolLegDay)element).getPatrolLeg().getPilot() != null){
					return org.wcs.smart.ui.SmartLabelProvider.getFullLabel(((PatrolLegDay)element).getPatrolLeg().getPilot().getMember());
				}
				return ""; //$NON-NLS-1$
			}else if (column == PatrolLegDayColumn.TRANSPORT){
				return ((PatrolLegDay)element).getPatrolLeg().getType().getName();
				
			}
		}
		return super.getText(element);
	}
}

