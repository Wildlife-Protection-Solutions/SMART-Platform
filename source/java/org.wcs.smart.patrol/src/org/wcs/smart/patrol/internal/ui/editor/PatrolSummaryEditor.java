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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
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
import org.wcs.smart.patrol.internal.ui.ArmedComposite;
import org.wcs.smart.patrol.internal.ui.CommentComposite;
import org.wcs.smart.patrol.internal.ui.DateComposite;
import org.wcs.smart.patrol.internal.ui.EmployeeLeaderPilotComposite;
import org.wcs.smart.patrol.internal.ui.ObjectiveComposite;
import org.wcs.smart.patrol.internal.ui.PatrolItemComposite;
import org.wcs.smart.patrol.internal.ui.PatrolLegsComposite;
import org.wcs.smart.patrol.internal.ui.PatrolMandateComposite;
import org.wcs.smart.patrol.internal.ui.PatrolTransportComposite;
import org.wcs.smart.patrol.internal.ui.StationComposite;
import org.wcs.smart.patrol.internal.ui.TeamComposite;
import org.wcs.smart.patrol.internal.ui.createpatrol.EmployeeLabelProvider;
import org.wcs.smart.patrol.internal.ui.editor.PatrolLegDayLabelProvider.PatrolLegDayColumn;
import org.wcs.smart.patrol.internal.ui.editpatrol.EditPatrolItemDialog;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;

/**
 * Editor part for displaying and editing patrol information.
 * @author Emily
 * @since 1.0.0
 */
public class PatrolSummaryEditor extends EditorPart {

	/**
	 * 
	 */
	private static final String EDIT_LABEL = "edit";

	public static final String ID = "org.wcs.smart.patrol.ui.PatrolSummaryEditor"; //$NON-NLS-1$

	private boolean isDirty = false;
	
	
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

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
	
	private Hyperlink editObjective;
	private Hyperlink editComment;
	private Hyperlink editEmployee;
	private Hyperlink editDates;
	private TableViewer employeeList;
	private Form frmPatrolSummary;

	private PatrolEditor editor;
	private TableViewer tblPatrolData;
	
	private boolean isMulti = false;
	
	private HashMap<PatrolLegDayColumn, TableViewerColumn> tableColumns = new HashMap<PatrolLegDayColumn, TableViewerColumn>();
	
	/**
	 * Creates a new summary editor page
	 * @param editor parent editor
	 */
	public PatrolSummaryEditor(PatrolEditor editor) {
		super.setPartName("Summary");
		this.editor = editor;
	}

	/**
	 * Create contents of the editor part.
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {

		Composite container = toolkit.createComposite(parent, SWT.NONE);

		toolkit.paintBordersFor(container);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		

		
		frmPatrolSummary = toolkit.createForm(container);
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
			Label lblWarning = toolkit.createLabel(warning, "", SWT.NONE);
			lblWarning.setText("This patrol cannot be modified: " + canEdit + ". Please contact administrator if editing is required.");
			
		}
		
		Section patrolSection = toolkit.createSection(frmPatrolSummary.getBody(), Section.TITLE_BAR | Section.EXPANDED  );
		patrolSection.setText("Patrol Information");
		patrolSection.setDescription("Details about patrol");
		patrolSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite top = toolkit.createComposite(patrolSection, SWT.NONE);
		top.setLayout(new GridLayout(2, false));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		patrolSection.setClient(top);
		
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
		
		
		toolkit.createLabel(right, "Patrol ID:");
		txtPatrolId = toolkit.createFormText(right, false);
		txtPatrolId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		txtPatrolId.setData(FormToolkit.KEY_DRAW_BORDER, null);
		
		toolkit.createLabel(left, "Patrol Type:");
		txtPatrolType = toolkit.createFormText(left, false);
		txtPatrolType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		Label lbl = null;
		
		if (editor.getPatrol().getLegs().size() <=1 ){
			lbl = toolkit.createLabel(left, "Transportation Type:");
			txtTransport = toolkit.createText(left, "", SWT.NONE);
			txtTransport.setEditable(false);
			txtTransport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			createEditLink(toolkit, left, new PatrolTransportComposite());
		}
		
		lbl = toolkit.createLabel(left, "Armed?:");
		btnArmed = toolkit.createButton(left, null, SWT.CHECK);
		btnArmed.setEnabled(false);
		btnArmed.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(toolkit, left, new ArmedComposite()); 
		
		
		lbl = toolkit.createLabel(left, "Mandate:");
		txtMandate = toolkit.createText(left, "AIR/MARINE/LAND", SWT.NONE);
		txtMandate.setEditable(false);
		txtMandate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		 createEditLink(toolkit, left, new PatrolMandateComposite()); 
		
		lbl = toolkit.createLabel(left, "Team:");
		txtTeam= toolkit.createText(left, "");
		txtTeam .setEditable(false);
		txtTeam .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		 createEditLink(toolkit, left, new TeamComposite()); 
	
		lbl = toolkit.createLabel(left, "Station:");
		txtStation = toolkit.createText(left, "");
		txtStation .setEditable(false);
		txtStation .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(toolkit, left, new StationComposite()); 

		lbl = toolkit.createLabel(right, "Objective:");
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		txtObjective = toolkit.createText(right, "", SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		txtObjective .setEditable(false);
		txtObjective .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		((GridData)txtObjective.getLayoutData()).heightHint=80;
		((GridData)txtObjective.getLayoutData()).widthHint=100;
		editObjective = createEditLink(toolkit, right, new ObjectiveComposite());
		editObjective.setLayoutData(new GridData(SWT.END, SWT.BOTTOM, false, false, 1, 1));
		
		
		lbl = toolkit.createLabel(right, "Comment:");
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		txtComment = toolkit.createText(right, "", SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		txtComment.setEditable(false);
		txtComment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		((GridData)txtComment.getLayoutData()).heightHint=80;
		((GridData)txtComment.getLayoutData()).widthHint=100;
		editComment = createEditLink(toolkit, right, new CommentComposite());
		editComment.setLayoutData(new GridData(SWT.END, SWT.BOTTOM, false, false,1,1));
		
		
		lbl = toolkit.createLabel(left, "Members:");
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		
		Table employeeTable = toolkit.createTable(left, SWT.V_SCROLL | SWT.H_SCROLL);
		employeeList = new TableViewer(employeeTable);
		employeeList.setContentProvider(ArrayContentProvider.getInstance());
		employeeList.setLabelProvider(new EmployeeLabelProvider());
		employeeTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		/* ----- Patrol Days / Legs Section ------- */
		Section dataSection = toolkit.createSection(frmPatrolSummary.getBody(), Section.TITLE_BAR | Section.EXPANDED  );
		dataSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dataSection.setText("Patrol Data");
		dataSection.setDescription("Details about patrol tracks and waypoints");
		
		dataSection.setLayout(new GridLayout(1, false));
		Composite compData = toolkit.createComposite(dataSection, SWT.NONE );
		compData.setLayout(new GridLayout(1, false));

		Composite comp = toolkit.createComposite(compData, SWT.NONE );
		comp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		comp.setLayout(new GridLayout(5, false));
		toolkit.createLabel(comp, "Start Date:");
		txtStartDate = toolkit.createText(comp, "");
		txtStartDate.setEditable(false);
		
		txtStartDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		lbl = toolkit.createLabel(comp, "End Date:");
		
		txtEndDate = toolkit.createText(comp, "");
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
			lbl = toolkit.createLabel(top, "This is a multi-leg patrol.  To change the patrol members or transport type use the edit button below in the Patrol Data section.");
			lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		}
		
		
		/* --- Patrol days table  ---*/
		Composite compTable = toolkit.createComposite(compData);
		compTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		TableColumnLayout tableLayout = new TableColumnLayout();
		compTable.setLayout(tableLayout);
		
		tblPatrolData = new TableViewer(compTable, SWT.BORDER| SWT.FULL_SELECTION);
		toolkit.adapt(tblPatrolData.getTable());
		
		tblPatrolData.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		tblPatrolData.getTable().setLinesVisible(true);
		tblPatrolData.getTable().setHeaderVisible(true);
		
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
		
		
		tblPatrolData.setContentProvider(new ObservableListContentProvider());
		tblPatrolData.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				PatrolLegDay pld = (PatrolLegDay)((StructuredSelection)tblPatrolData.getSelection()).getFirstElement();
				if (pld != null){
					PatrolDayEditorInput input = new PatrolDayEditorInput(pld.getDate());
					editor.setActiveEditor(editor.findEditors(input)[0]);
				}
				
			}
		});
		
		dataSection.setClient(compData);
		
		frmPatrolSummary.setText(editor.getPatrol().getId());
		initValues();
	}

	/**
	 * Creates an edit hyperlink button
	 * @param tolkit toolkit
	 * @param parent parent composite
	 * @param partEditor editor to use
	 * @return hyperlink created
	 */
	private Hyperlink createEditLink(FormToolkit tolkit, Composite parent, final PatrolItemComposite partEditor ){
		Hyperlink editLink = toolkit.createHyperlink(parent, EDIT_LABEL, SWT.WRAP);
		
		if (editor.canEdit() != null){
			editLink.setEnabled(false);
			editLink.setVisible(false);
		}else {
			if (partEditor != null){
				editLink.setToolTipText("Edit " + partEditor.getTitle());
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
		
		if (ret == IDialogConstants.OK_ID){
			PatrolEventManager.getInstance().patrolChanged(comp.getAttribute(), editor.getPatrol());
			this.initValues();
			return true;
		}
		return false;
	}
	
	
	@Override
	public void setFocus() {
		// Set the focus
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
	 * Updates the widges with the value from the patrol.
	 */
	private void initValues(){
		Session session = HibernateManager.openSession();
		try {
			Patrol patrol = editor.getPatrol();
			session.update(patrol);
			txtPatrolId.setText(patrol.getId(), false, false);
			txtPatrolType.setText(patrol.getPatrolType().getGuiName(), false, false);
			if (patrol.getStation() == null) {
				txtStation.setText("(none)");
			} else {
				txtStation.setText(patrol.getStation().getName());
			}
			if (patrol.getMandate() != null) {
				txtMandate.setText(patrol.getMandate().getName());
			} else {
				txtMandate.setText("(none)");
			}
			if (patrol.getComment() != null){
				txtComment.setText(patrol.getComment());
			}else{
				txtComment.setText("");
			}
			btnArmed.setSelection(patrol.isArmed());

			if (patrol.getTeam() != null) {
				txtTeam.setText(patrol.getTeam().getName());
			} else {
				txtTeam.setText("(none)");
			}

			if (patrol.getObjective() != null) {
				txtObjective.setText(patrol.getObjective());
			} else {
				txtObjective.setText("");
			}

//			if (patrol.getObjectiveRating() != null) {
//				sclObjRating.setSelection(patrol.getObjectiveRating());
//			}

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
			employeeList.setInput(allEmployee.toArray());

			txtStartDate.setText(DateFormat.getDateInstance(DateFormat.LONG)
					.format(patrol.getStartDate()));
			txtEndDate.setText(DateFormat.getDateInstance(DateFormat.LONG)
					.format(patrol.getEndDate()));

			//if (patrol.getLegs().size() <= 1) {
			if (!isMulti){
				//multi leg patrol
				WritableList input = new WritableList(patrol.getFirstLeg().getPatrolLegDays(), PatrolLeg.class);
				tblPatrolData.setInput(input);
				txtTransport.setText(patrol.getFirstLeg().getType().getName());

				TableColumnLayout collayout = (TableColumnLayout) tblPatrolData.getTable().getParent().getLayout();
				for (Iterator<Entry<PatrolLegDayColumn, TableViewerColumn>> iterator = tableColumns.entrySet().iterator(); iterator.hasNext();) {
					Entry<PatrolLegDayColumn, TableViewerColumn> info = (Entry<PatrolLegDayColumn, TableViewerColumn>) iterator.next();
					if (info.getKey().multi) {
						collayout.setColumnData(info.getValue().getColumn(), new ColumnWeightData(0, 0, false));
					}
				}
			} else {
				WritableList input = new WritableList();
				for (PatrolLeg leg : patrol.getLegs()) {
					for (PatrolLegDay pld : leg.getPatrolLegDays()) {
						input.add(pld);
					}
				}
				tblPatrolData.setInput(input);
				if (!patrol.hasPilot()) {
					TableViewerColumn tcolumn = tableColumns.get(PatrolLegDayColumn.PILOT);
					TableColumnLayout collayout = (TableColumnLayout) tblPatrolData.getTable().getParent().getLayout();
					collayout.setColumnData(tcolumn.getColumn(),new ColumnWeightData(0, 0, false));
				}
			}
		}finally{
			session.close();
		}
	}
	
	/**
	 * Refresh the patrol summary table.
	 */
	public void refreshPatrolSummaryTable(){
		tblPatrolData.refresh();
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (!(input instanceof PatrolEditorInput)){
			throw new RuntimeException("Invalid editor input.");
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
		LEG("Leg", 1, true),
		DAY("Day", 2, false),
		START("Start Time", 1, false),
		END("End Time", 1, false),
		DISTANCE("Distance", 1, false),
		HOURS("Hours", 1, false),
		TRANSPORT("Transport", 1, true),
		LEADER("Leader", 1, true),
		PILOT("Pilot", 1, true);
		
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
	private SimpleDateFormat dayOfWeekFormatter = new SimpleDateFormat("E");
	public PatrolLegDayLabelProvider(PatrolLegDayColumn column){
		this.column = column;
	}
	
	
	public String getText(Object element) {
		if (element instanceof PatrolLegDay){
			if (column == PatrolLegDayColumn.DAY){
				Date d = ((PatrolLegDay) element).getDate();
				return DateFormat.getDateInstance(DateFormat.MEDIUM).format(d) + " " + dayOfWeekFormatter.format(d) ;
			}else if (column == PatrolLegDayColumn.DISTANCE){
				if (((PatrolLegDay) element).getTrack() != null){
					return String.valueOf( ((PatrolLegDay) element).getTrack().getDistance() );
				}else{
					return "0";
				}
			}else if (column == PatrolLegDayColumn.START){
				if (((PatrolLegDay) element).getStartTime() != null){
					return DateFormat.getTimeInstance(DateFormat.MEDIUM).format(((PatrolLegDay) element).getStartTime() );
				}else{
					return "";
				}
			}else if (column == PatrolLegDayColumn.END){
				if (((PatrolLegDay) element).getEndTime() != null){
					return DateFormat.getTimeInstance(DateFormat.MEDIUM).format(((PatrolLegDay) element).getEndTime() );
				}else{
					return "";
				}
			}else if (column == PatrolLegDayColumn.HOURS){
				double hrs = ((PatrolLegDay) element).getHoursWorked();
				return PatrolEditor.REST_TIME_FORMATTER.format(hrs);
			}else if (column == PatrolLegDayColumn.LEG){
				return ((PatrolLegDay)element).getPatrolLeg().getId();
			}else if (column == PatrolLegDayColumn.LEADER){
				if (((PatrolLegDay)element).getPatrolLeg().getLeader() == null){
					return "";
				}
				return ((PatrolLegDay)element).getPatrolLeg().getLeader().getMember().getLabel();
			}else if (column == PatrolLegDayColumn.PILOT){
				if (((PatrolLegDay)element).getPatrolLeg().getPilot() != null){
					return ((PatrolLegDay)element).getPatrolLeg().getPilot().getMember().getLabel();
				}
				return "";
			}else if (column == PatrolLegDayColumn.TRANSPORT){
				return ((PatrolLegDay)element).getPatrolLeg().getType().getName();
				
			}
		}
		return super.getText(element);
	}
}

