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
package org.wcs.smart.patrol.internal.ui;

import java.sql.Time;
import java.text.Collator;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.createpatrol.EmployeeLabelProvider;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for splitting a patrol leg into two legs.
 * 
 * @since 1.0.0
 */
public class PatrolLegSplitDialog extends TitleAreaDialog{

	private PatrolLeg existingLeg;

	private DateTime startDate;
	private DateTime startTime;
	private Button opStart;
	private Button opCustom;
	
	private DateTime endDate;
	private DateTime endTime;
	private Button opEnd;
	private Button opEndCustom;
	
	private ArrayList<Employee> employees;
	private ArrayList<Employee> employeesA;
	private ArrayList<Employee> employeesB;

	private ComboViewer groupALeader;
	private ComboViewer groupAPilot;
	private ComboViewer groupBLeader;
	private ComboViewer groupBPilot;
	private ComboViewer cmbTransportTypeA;
	private ComboViewer cmbTransportTypeB;
	private List<PatrolTransportType> typeOps;
	private Collection<PatrolLeg> legsToUpdate;
	
	/**
	 * Creates a new dialog for splitting a patrol leg into multiple legs.
	 * 
	 * @param parentShell the parent dialog
	 * @param patrolLeg the patrol leg to split
	 * @param typeOps the transportation type options
	 * @param legsToUpdate the set of legs to update
	 */
	public PatrolLegSplitDialog(Shell parentShell, PatrolLeg patrolLeg, List<PatrolTransportType> typeOps, Collection<PatrolLeg> legsToUpdate ) {
		super(parentShell);
		this.existingLeg = patrolLeg;
		this.typeOps = typeOps;
		this.legsToUpdate = legsToUpdate;
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	private void sortList(List<Employee> list){
		Collections.sort(list, new Comparator<Employee>(){
			@Override
			public int compare(Employee arg0, Employee arg1) {
				return Collator.getInstance().compare(
						SmartLabelProvider.getFullLabel(arg0),
						SmartLabelProvider.getFullLabel(arg1));
			}});
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout gl = new GridLayout(1, false);
		
		gl.marginBottom = gl.marginTop = gl.verticalSpacing = 0;
		parent.setLayout(gl);
				
		employees = new ArrayList<Employee>();
		for (PatrolLegMember member: existingLeg.getMembers()){
			employees.add(member.getMember());
		}
		sortList(employees);
		employeesA = new ArrayList<Employee>();
		employeesB = new ArrayList<Employee>();
		
		
		Label lbl;
		createStartTimeComposite(parent);
		createEndTimeComposite(parent);
		//Employee List on the Left Side of Window
		Composite compEmployees = new Composite(parent, SWT.NONE);
		compEmployees.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		compEmployees.setLayout(new GridLayout(2, false));
		
		final TableViewer emplList = new TableViewer(compEmployees, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		emplList.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		emplList.setLabelProvider(new EmployeeLabelProvider());
		emplList.setContentProvider(ArrayContentProvider.getInstance());
		emplList.setInput(employees);
		
		Composite right = new Composite(compEmployees, SWT.NONE);
		right.setLayout(new GridLayout(1, false));
		right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		// -------- Group A -------------------
			
		
		Group groupA = createGroup(right, Messages.PatrolLegSplitDialog_GroupA_SectionLabel);
		
		
		ScrolledComposite sc = new ScrolledComposite(groupA, SWT.H_SCROLL | SWT.V_SCROLL);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		
		Composite gA = new Composite(sc, SWT.NONE);
		sc.setContent(gA);
		gA.setLayout(new GridLayout(2, false));
		
		cmbTransportTypeA = createTransportTypeComboViewer(gA);
	
		lbl = new Label(gA, SWT.NONE);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		lbl.setText(Messages.PatrolLegSplitDialog_GroupAMembers_Label);
		TableViewer groupAEmployees = createEmployeeButtonPanelAndTable(gA, employeesA, emplList);
		
		Composite leaderComp = new Composite(gA, SWT.NONE);
		leaderComp.setLayout(new GridLayout(2, false));
		leaderComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		groupALeader = createLeaderPilot(leaderComp, Messages.PatrolLegSplitDialog_GroupALeader_Label, (List<Employee>)groupAEmployees.getInput());
		if (existingLeg.getPatrol().hasPilot()){
			groupAPilot = createLeaderPilot(leaderComp, Messages.PatrolLegSplitDialog_GroupAPilot_Label, (List<Employee>)groupAEmployees.getInput());
		}
		sc.setMinSize(gA.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		// -------- Group B -------------------
		Group groupB = createGroup(right, Messages.PatrolLegSplitDialog_GroupB_SectionLabel);
		sc = new ScrolledComposite(groupB, SWT.H_SCROLL | SWT.V_SCROLL);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		
		Composite gB = new Composite(sc, SWT.NONE);
		sc.setContent(gB);
		gB.setLayout(new GridLayout(2, false));
				
		cmbTransportTypeB = createTransportTypeComboViewer(gB);
	
		lbl = new Label(gB, SWT.NONE);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		lbl.setText(Messages.PatrolLegSplitDialog_GroupBMembers_Label);
		
		TableViewer groupBEmployees = createEmployeeButtonPanelAndTable(gB, employeesB, emplList);
		
		leaderComp = new Composite(gB, SWT.NONE);
		leaderComp.setLayout(new GridLayout(2, false));
		leaderComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		groupBLeader = createLeaderPilot(leaderComp, Messages.PatrolLegSplitDialog_GroupBLeader_Label, (List<Employee>)groupBEmployees.getInput());
		if (existingLeg.getPatrol().hasPilot()){
			groupBPilot = createLeaderPilot(leaderComp, Messages.PatrolLegSplitDialog_GroupBPilot_Label, (List<Employee>)groupBEmployees.getInput());
		}
		sc.setMinSize(gB.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		setMessage(Messages.PatrolLegSplitDialog_Dialog_Message);
		super.getShell().setText(Messages.PatrolLegSplitDialog_Dialog_Title);
		setTitle(MessageFormat.format(Messages.PatrolLegSplitDialog_DialogTitle2, existingLeg.getId()));
		return parent;
	}
	
	/*
	 * create a leader or pilot combo viewer
	 */
	private ComboViewer createLeaderPilot(Composite parent, String name, List<Employee> employeeList){
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(name);
		ComboViewer cmbLeader = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbLeader.setLabelProvider(new EmployeeLabelProvider());
		cmbLeader.setContentProvider(ArrayContentProvider.getInstance());
		cmbLeader.setInput(employeeList);
		cmbLeader.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cmbLeader.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});
		return cmbLeader;
	}

	/*
	 * create an employee table and associated buttons
	 */
	private TableViewer createEmployeeButtonPanelAndTable(Composite parent,final ArrayList<Employee> input, final TableViewer employeeTableViewer){
		Composite btn = new Composite(parent, SWT.NONE);
		btn.setLayout(new GridLayout(1, false));
		btn.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		Button btnAddA = new Button(btn, SWT.PUSH);
		btnAddA.setText("->"); //$NON-NLS-1$
		btnAddA.setToolTipText(Messages.PatrolLegSplitDialog_AddEmployees_ToolTip);
		
		Button btnRemoveA = new Button(btn, SWT.PUSH);
		btnRemoveA.setText("<-"); //$NON-NLS-1$
		btnRemoveA.setToolTipText(Messages.PatrolLegSplitDialog_RemoveEmployees_Tooltip);
		
		final TableViewer groupList = new TableViewer(parent, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		groupList.setLabelProvider(new EmployeeLabelProvider());
		groupList.setContentProvider(ArrayContentProvider.getInstance());
		groupList.setInput(input);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true,true);
		groupList.getTable().setLayoutData(gd);
		
		btnAddA.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Iterator<?> iterator = ((IStructuredSelection)employeeTableViewer.getSelection()).iterator(); iterator.hasNext();) {
					Employee type = (Employee) iterator.next();
					employees.remove(type);
					input.add(type);
				}
				sortList(input);
				employeeTableViewer.refresh();
				groupList.refresh();
				groupALeader.refresh();
				groupBLeader.refresh();
				if (groupAPilot != null){
					groupAPilot.refresh();
				}
				if (groupBPilot != null){
					groupBPilot.refresh();
				}
				validate();
			}
		});
		btnRemoveA.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Iterator<?> iterator = ((IStructuredSelection)groupList.getSelection()).iterator(); iterator.hasNext();) {
					Employee type = (Employee) iterator.next();
					employees.add(type);
					input.remove(type);
				}
				sortList(employees);
				employeeTableViewer.refresh();
				groupList.refresh();
				groupALeader.refresh();
				groupBLeader.refresh();
				if (groupAPilot != null){
					groupAPilot.refresh();
				}
				if (groupBPilot != null){
					groupBPilot.refresh();
				}
				validate();
			}
		});
		return groupList;
	}
	/*
	 * create transport type combo viewer
	 */
	private ComboViewer createTransportTypeComboViewer(Composite parent){
		Composite ttype = new Composite(parent, SWT.NONE);
		ttype.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		ttype.setLayout(new GridLayout(2, false));
		
		Label lbl = new Label(ttype, SWT.NONE);
		lbl.setText(Messages.PatrolLegSplitDialog_TransportType_Label);
		ComboViewer cmbTransportType = new ComboViewer(ttype, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbTransportType.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				return ((PatrolTransportType)element).getName();
			}
		});
		cmbTransportType.setContentProvider(ArrayContentProvider.getInstance());
		
		cmbTransportType.setInput( typeOps );
		
		cmbTransportType.setSelection(new StructuredSelection(existingLeg.getType()));
		cmbTransportType.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		cmbTransportType.addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});
		return cmbTransportType;
	}
	/*
	 * create a split group
	 */
	private Group createGroup(Composite parent, String name){
		Group group = new Group(parent, SWT.NONE);	
		group.setText(name);
		group.setLayout(new FillLayout());
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return group;
	}

	/*
	 * create start time composite
	 */
	private void createStartTimeComposite(Composite parent) {
		
		Composite timecomp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginBottom = layout.marginTop = layout.verticalSpacing = 0;
		timecomp.setLayout(layout);
		timecomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(timecomp, SWT.NONE);
		lbl.setText(Messages.PatrolLegSplitDialog_DateofSplit_Label);
		startDate = new DateTime(timecomp, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER | SWT.LONG);
		startDate.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		startDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validate();
			}
			
		});
		SmartUtils.initDateDateTimeWidget(startDate, existingLeg.getStartDate());
		
		lbl = new Label(timecomp, SWT.NONE);
		lbl.setText(Messages.PatrolLegSplitDialog_TimeOfSplit_Label);
		
		Composite opComp = new Composite(timecomp, SWT.NONE);
		opComp.setLayout(new GridLayout(3, false));
		
		SelectionAdapter opAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				startTime.setEnabled(opCustom.getSelection());
				validate();
			}
		};
		opStart = new Button(opComp, SWT.RADIO);
		opStart.setText(Messages.PatrolLegSplitDialog_OpStartOfDay);
		opStart.setSelection(true);
		opStart.addSelectionListener(opAdapter);
		
		opCustom = new Button(opComp, SWT.RADIO);
		opCustom.setText(Messages.PatrolLegSplitDialog_OpCustom);
		opCustom.addSelectionListener(opAdapter);
		startTime = new DateTime(opComp, SWT.TIME | SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER);
		startTime.setEnabled(false);
		
		SmartUtils.initTimeDateTimeWidget(startTime, existingLeg.getStartDate());
		
		startTime.addSelectionListener(opAdapter);
		startTime.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				validate();
			}
		});
	}
	
	/*
	 * create end time composite
	 */
	private void createEndTimeComposite(Composite parent) {
		
		Composite timecomp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginBottom = layout.marginTop = layout.verticalSpacing = 0;
		timecomp.setLayout(layout);
		timecomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(timecomp, SWT.NONE);
		lbl.setText(Messages.PatrolLegSplitDialog_DateGroupsJoined_Label);
		endDate = new DateTime(timecomp, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER | SWT.LONG);
		endDate.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		endDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				 validate();
			}
			
		});
		SmartUtils.initDateDateTimeWidget(endDate, existingLeg.getEndDate());
		
		lbl = new Label(timecomp, SWT.NONE);
		lbl.setText(Messages.PatrolLegSplitDialog_TimeGroupsJoined_Label);
		
		Composite opComp = new Composite(timecomp, SWT.NONE);
		opComp.setLayout(new GridLayout(3, false));
		
		SelectionAdapter opAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				endTime.setEnabled(opEndCustom.getSelection());
			}
		};
		opEnd = new Button(opComp, SWT.RADIO);
		opEnd.setText(Messages.PatrolLegSplitDialog_OpEndOfDay);
		opEnd.setSelection(true);
		opEnd.addSelectionListener(opAdapter);
		
		opEndCustom = new Button(opComp, SWT.RADIO);
		opEndCustom.setText(Messages.PatrolLegSplitDialog_JoinedOpCustom);
		opEndCustom.addSelectionListener(opAdapter);
		endTime = new DateTime(opComp, SWT.TIME | SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER);
		endTime.setEnabled(false);
		
		SmartUtils.initTimeDateTimeWidget(endTime, existingLeg.getEndDate());

		endTime.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				validate();
			}
		});
	}
	
	/*
	 * validates the current import and sets error message
	 */
	private void validate(){
		String error = getValidationError();
		setErrorMessage(error);
		getButton(OK).setEnabled(error == null);
	}
	
	/*
	 * validates the current inpur
	 */
	private String getValidationError(){
//		if (employeeList.size() > 0){
//			return "All members must belong to Group A or Group B";
//		}
		if (employeesA.size() == 0){
			return Messages.PatrolLegSplitDialog_Error_GroupANoMembers;
		}
		if (employeesB.size() == 0){
			return Messages.PatrolLegSplitDialog_Error_GroupBNoMembers;
		}
		if (  ((IStructuredSelection)this.groupALeader.getSelection()).isEmpty() ){
			return Messages.PatrolLegSplitDialog_Error_GroupANoLeader;
		}
		if (  ((IStructuredSelection)this.groupBLeader.getSelection()).isEmpty() ){
			return Messages.PatrolLegSplitDialog_Error_GroupBNoLeader;
		}
		if (this.groupAPilot != null &&   ((IStructuredSelection)this.groupAPilot.getSelection()).isEmpty() ){
			return Messages.PatrolLegSplitDialog_Error_GroupANoPilot;
		}
		if (this.groupBPilot != null &&   ((IStructuredSelection)this.groupBPilot.getSelection()).isEmpty() ){
			return Messages.PatrolLegSplitDialog_Error_GroupBNoPilot;
		}
		
		if (  ((IStructuredSelection)this.cmbTransportTypeA.getSelection()).isEmpty() ){
			return Messages.PatrolLegSplitDialog_Error_GroupANoTransportType;
		}
		if (  ((IStructuredSelection)this.cmbTransportTypeB.getSelection()).isEmpty() ){
			return Messages.PatrolLegSplitDialog_Error_GroupBNoTransportType;
		}
		
		Date stime  = SmartUtils.getDate(startDate);
		if (opCustom.getSelection()){
			stime = SmartUtils.combineDateTime(stime, new Time (SmartUtils.getTime(startTime).getTime()));
//			stime += startTime.getHours() * 60 * 60 * 1000 + startTime.getMinutes() * 60 * 1000 + startTime.getSeconds() * 1000;
		}

		if (stime.before(existingLeg.getStartDate())){
			return MessageFormat.format(Messages.PatrolLegSplitDialog_Error_DateError_1,
				new Object[]{DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(existingLeg.getStartDate())});
		}
		if (stime.after(existingLeg.getEndDate())){
			return MessageFormat.format(Messages.PatrolLegSplitDialog_Error_DateError_2,
					new Object[]{DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(existingLeg.getEndDate())});
		}
		
		Date etime = SmartUtils.getDate(endDate);
		if (opEndCustom.getSelection()){
			etime = SmartUtils.combineDateTime(etime,  new Time (SmartUtils.getTime(endTime).getTime()));
//			etime += endTime.getHours() * 60 * 60 * 1000 + endTime.getMinutes() * 60 * 1000 + endTime.getSeconds() * 1000;
		}else{
			etime = SharedUtils.getDatePart(etime, true);
//			etime += 24 * 60 * 60 * 1000 - 1000;
		}

		if (etime.before(existingLeg.getStartDate())){
			return MessageFormat.format(
				Messages.PatrolLegSplitDialog_Error_DateError_3,
				new Object[]{DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(existingLeg.getStartDate())});
		}
		if (etime.after(existingLeg.getEndDate())){
			return MessageFormat.format(
				Messages.PatrolLegSplitDialog_Error_DateError_4,
				new Object[]{DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(existingLeg.getEndDate())});
		}
		if (etime.before(stime)){
			return Messages.PatrolLegSplitDialog_Error_DateError_5;
		}
		
		return null;
	}
	
	/**
	 * Creates new legs and updates existing legs.
	 */
	@Override
	protected void okPressed() {
		String err = getValidationError();
		if (err != null){
			setErrorMessage(err);
			return;
		}
		
		PatrolLeg legA = new PatrolLeg();
		PatrolLeg legB = new PatrolLeg();
		
		//dates		
		Date stime  = SmartUtils.getDate(startDate);
		if (opCustom.getSelection()){
			stime = SmartUtils.combineDateTime(stime, new Time (SmartUtils.getTime(startTime).getTime()));
		}

		Date etime = SmartUtils.getDate(endDate);
		if (opEndCustom.getSelection()){
			etime = SmartUtils.combineDateTime(etime,  new Time (SmartUtils.getTime(endTime).getTime()));
		}else{
			etime = SharedUtils.getDatePart(etime, true);
		}

		legA.setStartDate(stime);
		legB.setStartDate(stime);
		
		legA.setEndDate(etime);
		legB.setEndDate(etime);
		
		String legAId = existingLeg.getId() + Messages.PatrolLegSplitDialog_GroupA_LegIs_PostFix;
		if (legAId.length() > PatrolLeg.ID_MAX_SIZE){
			legAId = legAId.substring(0, PatrolLeg.ID_MAX_SIZE);
		}
		String legBId = existingLeg.getId() + Messages.PatrolLegSplitDialog_GroupB_LegId_Postfix;
		if (legBId.length() > PatrolLeg.ID_MAX_SIZE){
			legBId = legBId.substring(0, PatrolLeg.ID_MAX_SIZE);
		}
		
		legA.setId( legAId );
		legB.setId( legBId );
		
		
		legA.setType((PatrolTransportType) ((IStructuredSelection)this.cmbTransportTypeA.getSelection()).getFirstElement());
		legB.setType((PatrolTransportType) ((IStructuredSelection)this.cmbTransportTypeB.getSelection()).getFirstElement());
	
		Employee leaderA =   (Employee) ((IStructuredSelection)this.groupALeader.getSelection()).getFirstElement();
		Employee leaderB =   (Employee) ((IStructuredSelection)this.groupBLeader.getSelection()).getFirstElement();
		
		Employee pilotA = null;
		Employee pilotB = null;
		if (this.groupAPilot != null){
			pilotA =   (Employee) ((IStructuredSelection)this.groupAPilot.getSelection()).getFirstElement();
			pilotB =   (Employee) ((IStructuredSelection)this.groupBPilot.getSelection()).getFirstElement();
		}	
		
		legA.setMembers(new ArrayList<PatrolLegMember>());
		for (Iterator<?> iterator = this.employeesA.iterator(); iterator.hasNext();) {
			Employee type = (Employee) iterator.next();
			
			PatrolLegMember member =new PatrolLegMember();
			member.setPatrolLeg(legA);
			member.setMember(type);
			if (type.equals(leaderA)){
				member.setIsLeader(true);
			}
			if (pilotA != null && type.equals(pilotA)){
				member.setIsPilot(true);
			}	
			legA.getMembers().add(member);
		}
	
		legB.setMembers(new ArrayList<PatrolLegMember>());
		for (Iterator<?> iterator = this.employeesB.iterator(); iterator.hasNext();) {
			Employee type = (Employee) iterator.next();
			PatrolLegMember member = new PatrolLegMember();
			member.setPatrolLeg(legB);
			member.setMember(type);
			if (type.equals(leaderB)){
				member.setIsLeader(true);
			}
			if (pilotB != null && type.equals(pilotB)){
				member.setIsPilot(true);
			}	
			legB.getMembers().add(member);
		}
		
		legsToUpdate.add(legA);
		legsToUpdate.add(legB);
		legA.setPatrol(existingLeg.getPatrol());
		legB.setPatrol(existingLeg.getPatrol());
		
		if (! etime.equals( existingLeg.getEndDate() )){
			
			//we need to create another leg
			PatrolLeg legC = new PatrolLeg();
			legC.setStartDate(etime);
			legC.setEndDate(existingLeg.getEndDate());
			
			String legCId = existingLeg.getId() + Messages.PatrolLegSplitDialog_EndLeg_LegId_Postfix;
			if (legCId.length() > PatrolLeg.ID_MAX_SIZE){
				legCId = legCId.substring(0, PatrolLeg.ID_MAX_SIZE);
			}
			
			legC.setId( legCId );
			legC.setType(existingLeg.getType());
			legC.setMembers(new ArrayList<PatrolLegMember>());
			for (PatrolLegMember member : existingLeg.getMembers()){
				PatrolLegMember mem = member.clone();
				mem.setPatrolLeg(legC);
				legC.getMembers().add(mem);
			}
			legsToUpdate.add(legC);
			legC.setPatrol(existingLeg.getPatrol());
		}
		
		
		existingLeg.setEndDate(stime);
		if (existingLeg.getEndDate().getTime() - existingLeg.getStartDate().getTime() <= 1000){			
			//1 second
			legsToUpdate.remove(existingLeg);
			existingLeg.setPatrol(null);
			existingLeg = null;
		}
		
		super.okPressed();
		
	}
	
}
