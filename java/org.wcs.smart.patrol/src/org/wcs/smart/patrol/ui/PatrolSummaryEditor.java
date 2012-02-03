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

import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
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
import org.wcs.smart.patrol.internal.ui.ArmedComposite;
import org.wcs.smart.patrol.internal.ui.ObjectiveComposite;
import org.wcs.smart.patrol.internal.ui.PatrolItemComposite;
import org.wcs.smart.patrol.internal.ui.PatrolMandateComposite;
import org.wcs.smart.patrol.internal.ui.PatrolTransportComposite;
import org.wcs.smart.patrol.internal.ui.StationComposite;
import org.wcs.smart.patrol.internal.ui.TeamComposite;
import org.wcs.smart.patrol.internal.ui.createpatrol.EmployeeLabelProvider;
import org.wcs.smart.patrol.internal.ui.editpatrol.EditPatrolItemDialog;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.ui.PatrolLegDayLabelProvider.PatrolLegDayColumn;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
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
	
	private Button btnArmed;
	
	private Hyperlink editMandate;
	private Hyperlink editStation;
	private Hyperlink editTeam;
	private Hyperlink editArmed;
	private Hyperlink editObjective;
	private Hyperlink editObjectiveRating;
	private Hyperlink editEmployee;
	private Hyperlink editTransport;

	private TableViewer employeeList;
	private Scale sclObjRating;

	private Form frmPatrolSummary;

	private Hyperlink editDates;

	private Text txtStartDate;

	private Text txtEndDate;
	private Text txtTransport;

	private TableViewer tblPatrolData;
//
//	private Text txtPilot;
//
//	private Text txtPatrolLeader;
	
	private PatrolEditor editor;
	
	
	
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
//		frmPatrolSummary.setText("Patrol Summary ");
		
		GridLayout layout = new GridLayout(1, true);
		frmPatrolSummary.getBody().setLayout(layout);
		
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
		
		
		Label lblPatrolId = toolkit.createLabel(right, "Patrol ID:");
		txtPatrolId = toolkit.createFormText(right, false);
		txtPatrolId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		txtPatrolId.setData(FormToolkit.KEY_DRAW_BORDER, null);
		
		Label lblPatrolType = toolkit.createLabel(left, "Patrol Type:");
		txtPatrolType = toolkit.createFormText(left, false);
		txtPatrolType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		Label lbl = null;
		
		if (editor.getPatrol().getLegs().size() <=1 ){
			lbl = toolkit.createLabel(left, "Transportation Type:");
			txtTransport = toolkit.createText(left, "", SWT.NONE);
			txtTransport.setEditable(false);
			txtTransport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			editTransport = toolkit.createHyperlink(left, EDIT_LABEL, SWT.WRAP);
			editTransport.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					showEditDialog(new PatrolTransportComposite());
				}
			});
		}
		
		lbl = toolkit.createLabel(left, "Armed?:");
		btnArmed = toolkit.createButton(left, null, SWT.CHECK);
		btnArmed.setEnabled(false);
		btnArmed.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		editArmed = toolkit.createHyperlink(left, EDIT_LABEL, SWT.WRAP);
		editArmed.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				showEditDialog(new ArmedComposite());
			}
		});
		
		
		lbl = toolkit.createLabel(left, "Mandate:");
		txtMandate = toolkit.createText(left, "AIR/MARINE/LAND", SWT.NONE);
		txtMandate.setEditable(false);
		txtMandate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		editMandate = toolkit.createHyperlink(left, EDIT_LABEL, SWT.WRAP);
		editMandate.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				showEditDialog(new PatrolMandateComposite());
			}
		});
		
		lbl = toolkit.createLabel(left, "Team:");
		txtTeam= toolkit.createText(left, "");
		txtTeam .setEditable(false);
		txtTeam .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		editTeam = toolkit.createHyperlink(left, EDIT_LABEL, SWT.WRAP);
		editTeam.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				showEditDialog(new TeamComposite());
			}
		});
		
		lbl = toolkit.createLabel(left, "Station:");
		txtStation = toolkit.createText(left, "");
		txtStation .setEditable(false);
		txtStation .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		editStation = toolkit.createHyperlink(left, EDIT_LABEL, SWT.WRAP);
		editStation.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				showEditDialog(new StationComposite());
			}
		});

//		lbl = toolkit.createLabel(right, "");
//		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3,1));
		
		lbl = toolkit.createLabel(right, "Objective:");
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		txtObjective = toolkit.createText(right, "", SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		txtObjective .setEditable(false);
		txtObjective .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		((GridData)txtObjective.getLayoutData()).heightHint=80;
		((GridData)txtObjective.getLayoutData()).widthHint=100;
		editObjective = toolkit.createHyperlink(right, EDIT_LABEL, SWT.WRAP);
		editObjective .setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false, 1, 1));
		editObjective.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				showEditDialog(new ObjectiveComposite());
			}
		});
		
		lbl = toolkit.createLabel(right, "Objective Rating:");
		sclObjRating = new Scale(right, SWT.NONE);
		toolkit.adapt(sclObjRating, false, false);
		sclObjRating.setEnabled(false);
		sclObjRating.setMaximum(1);
		sclObjRating.setMaximum(5);
		sclObjRating.setIncrement(1);
		sclObjRating.setPageIncrement(1);
		editObjectiveRating = toolkit.createHyperlink(right, EDIT_LABEL, SWT.WRAP);
		editObjectiveRating.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				showEditDialog(new ObjectiveComposite());
			}
		});
		
		lbl = toolkit.createLabel(left, "Members:");
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		
		Table employeeTable = toolkit.createTable(left, SWT.V_SCROLL | SWT.H_SCROLL);
		employeeList = new TableViewer(employeeTable);
		employeeList.setContentProvider(ArrayContentProvider.getInstance());
		employeeList.setLabelProvider(new EmployeeLabelProvider());
		
		
		employeeTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		
		//editEmployee = toolkit.createHyperlink(left, EDIT_LABEL, SWT.WRAP);
		Section dataSection = toolkit.createSection(frmPatrolSummary.getBody(), Section.TITLE_BAR | Section.EXPANDED  );
		dataSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dataSection.setText("Patrol Data");
		dataSection.setDescription("Details about patrol tracks and waypoints");
		
		
		if (editor.getPatrol().getLegs().size() <=1 ){
			//single leg patrol
			dataSection.setLayout(new GridLayout(1, false));
			Composite compData = toolkit.createComposite(dataSection, SWT.NONE );
			compData.setLayout(new GridLayout(1, false));
			
			//update employee section as we edit it here
			employeeTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
			editEmployee = toolkit.createHyperlink(left, EDIT_LABEL, SWT.WRAP);
			editEmployee.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
			
			
			
			Composite comp = toolkit.createComposite(compData, SWT.NONE );
			comp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			comp.setLayout(new GridLayout(5, false));
			toolkit.createLabel(comp, "Start Date:");
			txtStartDate = toolkit.createText(comp, "");
			
			txtStartDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			lbl = toolkit.createLabel(comp, "End Date:");
			
			txtEndDate = toolkit.createText(comp, "");
			txtEndDate .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			editDates = toolkit.createHyperlink(comp, EDIT_LABEL, SWT.WRAP);
			
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
			}
			
			
			tblPatrolData.setContentProvider(ArrayContentProvider.getInstance());
			dataSection.setClient(compData);
		}else{
			//multi-day patrol
			employeeTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		}
		
		
		frmPatrolSummary.setText(editor.getPatrol().getId());
		initValues();
	}

	
	private void showEditDialog(PatrolItemComposite comp){
		
		Session session = HibernateManager.openSession();
		try{
			session.update(editor.getPatrol());
			EditPatrolItemDialog editDialog = new EditPatrolItemDialog(getEditorSite().getShell(), comp, editor.getPatrol(), session);
			editDialog.open();
		}finally{
			if(session.isOpen()){
				session.close();
			}
		}
		this.initValues();
	}
	
	
	@Override
	public void setFocus() {
		// Set the focus
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		
		
		
	}

	@Override
	public void doSaveAs() {
		// Do the Save As operation
	}

	private void initValues(){
		Session session = HibernateManager.openSession();	
		try{
		Patrol patrol = editor.getPatrol();
		session.update(patrol);
		txtPatrolId.setText(patrol.getId(), false, false);
		txtPatrolType.setText(patrol.getPatrolType().getGuiName(), false, false);
		if (patrol.getStation() == null){
			txtStation.setText("(none)");
		}else{
			txtStation.setText(patrol.getStation().getName());
		}
		if (patrol.getMandate() != null){
			txtMandate.setText(patrol.getMandate().getName());
		}else{
			txtMandate.setText("(none)");
		}
		
		btnArmed.setSelection(patrol.isArmed());
		
		if (patrol.getTeam() != null){
			txtTeam.setText(patrol.getTeam().getName());
		}else{
			txtTeam.setText("(none)");
		}
		
		if (patrol.getObjective() != null){
			txtObjective.setText(patrol.getObjective());
		}else{
			txtObjective.setText("");
		}
		
		if (patrol.getObjectiveRating() != null){
			sclObjRating.setSelection(patrol.getObjectiveRating());
		}
		
		Set<Employee> allEmployee = new HashSet<Employee>();
		Set<Employee> leaders = new HashSet<Employee>();
		Set<Employee> pilots= new HashSet<Employee>();
		for (int i = 0; i < patrol.getLegs().size(); i ++){
			session.update(patrol.getLegs().get(i));
			List<PatrolLegMember> members = patrol.getLegs().get(i).getMembers();
			for (PatrolLegMember mem : members){
				allEmployee.add(mem.getMember());
				if(mem.getIsLeader()){
					leaders.add( mem.getMember() );
				}
				if (mem.getIsPilot()){
					pilots.add(mem.getMember());
				}
			}
		}
		((EmployeeLabelProvider)employeeList.getLabelProvider()).setLeaders(leaders);
		((EmployeeLabelProvider)employeeList.getLabelProvider()).setPilots(pilots);
		employeeList.setInput(allEmployee.toArray());
		
		
		if (patrol.getLegs().size() <= 1){
			
			
			txtStartDate.setText(DateFormat.getDateInstance(DateFormat.LONG).format(patrol.getStartDate()));
			txtEndDate.setText(DateFormat.getDateInstance(DateFormat.LONG).format(patrol.getEndDate()));
			
			PatrolLeg first = patrol.getFirstLeg();
			
			tblPatrolData.setInput(first.getPatrolLegDays().toArray());
			
//			txtPatrolLeader.setText(first.getLeader().getMember().getLabel());
//			if (patrol.hasPilot()){
//				txtPilot.setText(first.getPilot().getMember().getLabel());
//			}
			
			txtTransport.setText(first.getType().getName());
		}}finally{
			session.close();
		}
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

class PatrolLegDayLabelProvider extends ColumnLabelProvider{
	
	public enum PatrolLegDayColumn{
		DAY("Day", 2),
		START("Start Time", 1),
		END("End Time", 1),
		DISTANCE("Distance", 1),
		HOURS("Hours", 1);
		
		int weight;
		String name;
		private PatrolLegDayColumn(String name, int weight){
			this.name = name;
			this.weight = weight;
		}
	};
	
	
	private PatrolLegDayColumn column = null;
	public PatrolLegDayLabelProvider(PatrolLegDayColumn column){
		this.column = column;
	}
	
	
	public String getText(Object element) {
		if (element instanceof PatrolLegDay){
			if (column == PatrolLegDayColumn.DAY){
				return DateFormat.getDateInstance(DateFormat.MEDIUM).format(((PatrolLegDay) element).getDate());
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
				if (((PatrolLegDay) element).getEndTime() != null && ((PatrolLegDay)element).getStartTime() != null){
					long hrs = ((PatrolLegDay) element).getEndTime().getTime() - ((PatrolLegDay) element).getStartTime().getTime();
					DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(hrs));
				}else{
					return "0";
				}
			}
		}
		return super.getText(element);
	}
}

