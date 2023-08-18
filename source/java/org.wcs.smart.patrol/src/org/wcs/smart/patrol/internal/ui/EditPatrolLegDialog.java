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

import java.text.Collator;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.createpatrol.EmployeeLabelProvider;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.ui.NamedIconItemLabelProvider;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for editing a patrol leg.
 * <p>Users can edit the leg id, start date, end date,
 * members list, leader and transportation type.</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class EditPatrolLegDialog extends SmartStyledTitleDialog{
	
	private DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
	private DateTimeFormatter dateTimeFormatter =DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
	
	private PatrolLeg editLeg;
	
	private DateTime startDate;
	private DateTime startTime;
	private Button opStart;
	private Button opCustom;
	
	private DateTime endDate;
	private DateTime endTime;
	private Button opEnd;
	private Button opEndCustom;
	
	private ArrayList<Employee> employeeList;
	private ArrayList<Employee> employeeListA;

	private Label lblGroupAPilot;
	private ComboViewer groupALeader;
	private ComboViewer groupAPilot;
	private TableComboViewer cmbTransportTypeA;
	private TableComboViewer cmbMandateA;
	private List<Employee> patrolMembers;
	
	private List<PatrolTransportType> typeOps;
	private List<PatrolMandate> mandateOps;
	
	private LocalDate patrolEndDate;
	private LocalDate patrolStartDate;
	
	private Text txtLegId;
	
	/**
	 * Creates a new edit leg dialog 
	 * 
	 * @param parentShell parent shell
	 * @param patrolLeg leg to edit
	 * @param patrolMembers list of possible patrol members
	 * @param typeOps transport type options 
	 * @param patrolStartDate patrol start date 
	 * @param patrolEndDate patrol end date
	 */
	public EditPatrolLegDialog(Shell parentShell, PatrolLeg patrolLeg,  List<Employee> patrolMembers, 
			List<PatrolTransportType> typeOps, List<PatrolMandate> mandateOps, LocalDate patrolStartDate, LocalDate patrolEndDate) {
		super(parentShell);
		this.editLeg = patrolLeg;
		this.patrolMembers = patrolMembers;
		this.typeOps = typeOps;
		this.mandateOps = mandateOps;
		this.patrolEndDate = patrolEndDate;
		this.patrolStartDate = patrolStartDate;
	}

	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	private void sortList(ArrayList<Employee> list){
		Collections.sort(list, new Comparator<Employee>(){
			@Override
			public int compare(Employee o1, Employee o2) {
				return Collator.getInstance().compare(
						SmartLabelProvider.getFullLabel(o1), SmartLabelProvider.getFullLabel(o2));
			}});
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Label lbl;
		parent = (Composite) super.createDialogArea(parent);
		
		employeeList = new ArrayList<Employee>();
		employeeList.addAll(patrolMembers);
		employeeListA = new ArrayList<Employee>();
		for (PatrolLegMember member: editLeg.getMembers()){
			employeeListA.add(member.getMember());
			employeeList.remove(member.getMember());
		}
		sortList(employeeList);
		sortList(employeeListA);
		
		Composite patrolIdComp = new Composite(parent, SWT.NONE);
		patrolIdComp.setLayout(new GridLayout(2, false));
		patrolIdComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		lbl = new Label(patrolIdComp, SWT.NONE);
		lbl.setText(Messages.EditPatrolLegDialog_LegId_Label );
		txtLegId = new Text(patrolIdComp, SWT.BORDER);
		txtLegId.setTextLimit(PatrolLeg.ID_MAX_SIZE);
		txtLegId.setText(editLeg.getId());
		txtLegId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		createStartTimeComposite(parent);
		createEndTimeComposite(parent);
		//Employee List on the Left Side of Window
		Composite compEmployees = new Composite(parent, SWT.NONE);
		compEmployees.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		compEmployees.setLayout(new GridLayout(2, false));
		
		final TableViewer emplList = new TableViewer(compEmployees, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		emplList.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)emplList.getTable().getLayoutData()).heightHint = 100;
		emplList.setLabelProvider(new EmployeeLabelProvider());
		emplList.setContentProvider(ArrayContentProvider.getInstance());
		emplList.setInput(employeeList);
		
		Composite right = new Composite(compEmployees, SWT.NONE);
		right.setLayout(new GridLayout(2, false));
		right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		cmbMandateA = createMandateComboViewer(right);
		
		cmbTransportTypeA = createTransportTypeComboViewer(right);
	
		lbl = new Label(right, SWT.NONE);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		lbl.setText(Messages.EditPatrolLegDialog_Members_Label);
		
		createEmployeeButtonPanelAndTable(right, employeeListA, emplList);
		
		Composite leaderComp = new Composite(right, SWT.NONE);
		leaderComp.setLayout(new GridLayout(2, false));
		leaderComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		Label lblGroupALeader = new Label(leaderComp, SWT.NONE);
		lblGroupALeader.setText(Messages.EditPatrolLegDialog_GroupALeader_Label);
		groupALeader = createLeaderPilot(leaderComp, employeeListA, editLeg.getLeader().getMember());

		lblGroupAPilot = new Label(leaderComp, SWT.NONE);
		lblGroupAPilot.setText(Messages.EditPatrolLegDialog_GroupAPilot_Label);
		Employee pilotA = editLeg.getPilot() != null ? editLeg.getPilot().getMember() : null;
		groupAPilot = createLeaderPilot(leaderComp, employeeListA, pilotA);
		
		cmbTransportTypeA.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateGroupAPilotState();
			}
		});
		updateGroupAPilotState();
		
		setMessage(Messages.EditPatrolLegDialog_DialogMessage);
		getShell().setText(Messages.EditPatrolLegDialog_DialogTitle);
		setTitle(MessageFormat.format(Messages.EditPatrolLegDialog_DialogTitle2, editLeg.getId()));
		return parent;
	}
	
	protected void updateGroupAPilotState() {
		PatrolTransportType ptt = (PatrolTransportType) ((IStructuredSelection)this.cmbTransportTypeA.getSelection()).getFirstElement();
		boolean showPilot = ptt != null && ptt.getPatrolType() != null && ptt.getPatrolType().requiresPilot();
		lblGroupAPilot.setVisible(showPilot);
		groupAPilot.getCombo().setVisible(showPilot);
	}

	/*
	 * Create a combo viewer for selecting patrol leader/pilot
	 */
	private ComboViewer createLeaderPilot(Composite parent, List<Employee> employeeList, Employee defaultValue){
		ComboViewer cmb = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmb.setLabelProvider(new EmployeeLabelProvider());
		cmb.setContentProvider(ArrayContentProvider.getInstance());
		cmb.setInput(employeeList);
		cmb.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		
		if (defaultValue != null) {
			cmb.setSelection(new StructuredSelection(defaultValue));
		}
		
		return cmb;
	}


	/*
	 * Create table viewer for patrol members list
	 */
	private TableViewer createEmployeeButtonPanelAndTable(Composite parent, List<Employee> list, final TableViewer employeeTableViewer){
		
		Composite btn = new Composite(parent, SWT.NONE);
		btn.setLayout(new GridLayout(1, false));
		btn.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		Button btnAddA = new Button(btn, SWT.PUSH);
		btnAddA.setText("->"); //$NON-NLS-1$
		btnAddA.setToolTipText(Messages.EditPatrolLegDialog_AddEmployees_ToolTip);
		
		Button btnRemoveA = new Button(btn, SWT.PUSH);
		btnRemoveA.setText("<-"); //$NON-NLS-1$
		btnRemoveA.setToolTipText(Messages.EditPatrolLegDialog_RemoveEmployees_ToolTip);
		
		
		final TableViewer groupList = new TableViewer(parent, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		groupList.setLabelProvider(new EmployeeLabelProvider());
		groupList.setContentProvider(ArrayContentProvider.getInstance());
		groupList.setInput(list);
		groupList.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)groupList.getTable().getLayoutData()).heightHint = 100;
		
		btnAddA.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Iterator<?> iterator = ((IStructuredSelection)employeeTableViewer.getSelection()).iterator(); iterator.hasNext();) {
					Employee type = (Employee) iterator.next();
					employeeList.remove(type);
					employeeListA.add(type);
					
				}
				sortList(employeeListA);
				
				employeeTableViewer.refresh();
				groupList.refresh();
				groupALeader.refresh();
				if (groupAPilot != null){
					groupAPilot.refresh();
				}
				
			}
		});
		btnRemoveA.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Iterator<?> iterator = ((IStructuredSelection)groupList.getSelection()).iterator(); iterator.hasNext();) {
					Employee type = (Employee) iterator.next();
					employeeList.add(type);
					employeeListA.remove(type);
					
				}
				sortList(employeeList);
				employeeTableViewer.refresh();
				groupList.refresh();
				groupALeader.refresh();
				if (groupAPilot != null){
					groupAPilot.refresh();
				}
			}
		});
		return groupList;
	}
	/*
	 * Transport type combo viewer
	 */
	private TableComboViewer createTransportTypeComboViewer(Composite parent){
		Composite ttype = new Composite(parent, SWT.NONE);
		ttype.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		ttype.setLayout(new GridLayout(2, false));
		
		Label lbl = new Label(ttype, SWT.NONE);
		lbl.setText(Messages.EditPatrolLegDialog_TransportType_Label);
		
		TableComboViewer cmbTransportType = new TableComboViewer(ttype, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
		cmbTransportType.setLabelProvider(new NamedIconItemLabelProvider(IconManager.Size.ICON));
		cmbTransportType.setContentProvider(ArrayContentProvider.getInstance());
		cmbTransportType.setInput( typeOps );
		cmbTransportType.setSelection(new StructuredSelection(editLeg.getType()));
		cmbTransportType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return cmbTransportType;
	}
	
	/*
	 * Transport type combo viewer
	 */
	private TableComboViewer createMandateComboViewer(Composite parent){
		Composite ttype = new Composite(parent, SWT.NONE);
		ttype.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		ttype.setLayout(new GridLayout(2, false));
		
		Label lbl = new Label(ttype, SWT.NONE);
		lbl.setText(Messages.EditPatrolLegDialog_MandateLabel);
		
		TableComboViewer cmb = new TableComboViewer(ttype, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
		cmb.setLabelProvider(new NamedIconItemLabelProvider(IconManager.Size.ICON));
		cmb.setContentProvider(ArrayContentProvider.getInstance());
		cmb.setInput( mandateOps );
		if (editLeg.getMandate() != null)
			cmb.setSelection(new StructuredSelection(editLeg.getMandate()));
		cmb.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return cmb;
	}
	

	/*
	 * Start time 
	 */
	private void createStartTimeComposite(Composite parent) {
		
		Composite timecomp = new Composite(parent, SWT.NONE);
		timecomp.setLayout(new GridLayout(2, false));
		timecomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(timecomp, SWT.NONE);
		lbl.setText(Messages.EditPatrolLegDialog_LegStartDate_Label);
		startDate = new DateTime(timecomp, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER | SWT.LONG);
		startDate.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		startDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validate();
			}
			
		});
		SmartUtils.initDateTimeWidget(startDate, editLeg.getStartDate());
		
		lbl = new Label(timecomp, SWT.NONE);
		lbl.setText(Messages.EditPatrolLegDialog_LegStartTime_Label);
		
		Composite opComp = new Composite(timecomp, SWT.NONE);
		opComp.setLayout(new GridLayout(3, false));
		
		SelectionAdapter opAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				startTime.setEnabled(opCustom.getSelection());
			}
		};
		opStart = new Button(opComp, SWT.RADIO);
		opStart.setText(Messages.EditPatrolLegDialog_Op_StartOfDay);
		opStart.addSelectionListener(opAdapter);
		
		opCustom = new Button(opComp, SWT.RADIO);
		opCustom.setText(Messages.EditPatrolLegDialog_OpStartTimeCutom);
		opCustom.addSelectionListener(opAdapter);
		
		LocalTime legstartTime = editLeg.getPatrolLegDays().get(0).getStartTime();
		
		startTime = new DateTime(opComp, SWT.TIME | SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER);
		startTime.setEnabled(false);
		SmartUtils.initDateTimeWidget(startTime, legstartTime);
		
		startTime.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validate();
			}
		});
		
		boolean enabled = (legstartTime.getHour() == 0 && legstartTime.getMinute() == 0 && legstartTime.getSecond() == 0);
		
		opStart.setSelection(enabled);
		opCustom.setSelection(!enabled);
		startTime.setEnabled(!enabled);
	}
	
	private void createEndTimeComposite(Composite parent) {
		
		Composite timecomp = new Composite(parent, SWT.NONE);
		timecomp.setLayout(new GridLayout(2, false));
		timecomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(timecomp, SWT.NONE);
		lbl.setText(Messages.EditPatrolLegDialog_LegEndDate_Label);
		endDate = new DateTime(timecomp, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER | SWT.LONG);
		endDate.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		endDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validate();
			}
			
		});
		SmartUtils.initDateTimeWidget(endDate, editLeg.getEndDate());
		
		lbl = new Label(timecomp, SWT.NONE);
		lbl.setText(Messages.EditPatrolLegDialog_LegEndTime_Label);
		
		Composite opComp = new Composite(timecomp, SWT.NONE);
		opComp.setLayout(new GridLayout(3, false));
		
		SelectionAdapter opAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				endTime.setEnabled(opEndCustom.getSelection());
			}
		};
		opEnd = new Button(opComp, SWT.RADIO);
		opEnd.setText(Messages.EditPatrolLegDialog_Op_EndOfDay);
		opEnd.addSelectionListener(opAdapter);
		
		opEndCustom = new Button(opComp, SWT.RADIO);
		opEndCustom.setText(Messages.EditPatrolLegDialog_OpEndTimeCustom);
		opEndCustom.addSelectionListener(opAdapter);
		
		LocalTime etime = editLeg.getPatrolLegDays().get(editLeg.getPatrolLegDays().size() - 1).getEndTime();
		
		endTime = new DateTime(opComp, SWT.TIME | SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER);
		SmartUtils.initDateTimeWidget(endTime, etime);
		
		endTime.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				validate();
			}
		});
		
		boolean enabled = etime.getHour() == 23 && etime.getMinute() == 59 && etime.getSecond() == 59;
		
		opEnd.setSelection(enabled);
		opEndCustom.setSelection(!enabled);
		endTime.setEnabled(!enabled);
	}
	
	/**
	 * Valids the input and updates the error message
	 */
	private void validate(){
		String error = getValidationErrorString();
		setErrorMessage(error);
		getButton(OK).setEnabled(error == null);
	}
	/**
	 * Validate the dates checking:
	 * <li>leg start between patrol start and end dates</li>
	 * <li>leg end between patrol start and end dates</li>
	 * <li>leg end before the leg start</li> 
	 * 
	 * @return
	 */
	private String validateDates(){
		
		LocalDate legStartDate = SmartUtils.toDate(startDate);
		LocalTime legStartTime = LocalTime.MIN;
		if (opCustom.getSelection()){
			legStartTime = SmartUtils.toTime(startTime);
		}
		
		//update end date & Time
		//long etime = SmartUtils.getDate(endDate).getTime();
		LocalDate legEndDate = SmartUtils.toDate(endDate);
		LocalTime legEndTime = LocalTime.MAX;
		if (opEndCustom.getSelection()){
			legEndTime = SmartUtils.toTime(endTime);
		}
		
		LocalDateTime legStart = LocalDateTime.of(legStartDate,  legStartTime);
		LocalDateTime legEnd = LocalDateTime.of(legEndDate,  legEndTime);
		
		if (legStart.toLocalDate().isBefore(patrolStartDate)){
			return MessageFormat.format(
					Messages.EditPatrolLegDialog_Error_StartAfterPStart,
					new Object[]{ dateFormatter.format(patrolStartDate)});
		}
		if (legStart.toLocalDate().isAfter(patrolEndDate) ){
			return MessageFormat.format(
					Messages.EditPatrolLegDialog_Error_StartBeforePEnd,
					new Object[]{ dateFormatter.format(patrolEndDate)});
		}
		if (legEnd.toLocalDate().isBefore(patrolStartDate)){
			return MessageFormat.format(
					Messages.EditPatrolLegDialog_Error_EndAfterPStart,
					new Object[]{ dateFormatter.format(patrolStartDate)});
		}
		if (legEnd.toLocalDate().isAfter(patrolEndDate)){
			return MessageFormat.format(
					Messages.EditPatrolLegDialog_Error_EndBeforePStart,
					new Object[]{ dateFormatter.format(patrolEndDate)});
		}
		if (legEnd.isBefore(legStart)){
			return MessageFormat.format(
					Messages.EditPatrolLegDialog_Error_StartBeforeEnd,
					new Object[]{ dateTimeFormatter.format(legStart), dateTimeFormatter.format(legEnd)});
		}
		return null;
	}
	
	/*
	 * validates the input and return error string
	 */
	private String getValidationErrorString(){
		if (txtLegId.getText().trim().isEmpty()){
			return Messages.EditPatrolLegDialog_Error_EmptyId;
		}
		if (employeeListA.size() == 0){
			return Messages.EditPatrolLegDialog_Error_NoEmployees;
		}
		
		String dateErr = validateDates();
		if (dateErr != null){
			return dateErr;
		}
		
		if (this.groupALeader.getSelection().isEmpty()){
			return Messages.EditPatrolLegDialog_Error_NoLeader;
		}
		if (this.cmbMandateA.getSelection().isEmpty()){
			return Messages.EditPatrolLegDialog_MandateRequired;
		}
		PatrolTransportType pttA = (PatrolTransportType) ((IStructuredSelection)this.cmbTransportTypeA.getSelection()).getFirstElement();
		if (this.groupAPilot != null && pttA.getPatrolType().requiresPilot() && this.groupAPilot.getSelection().isEmpty()){
			return Messages.EditPatrolLegDialog_Error_NoPilot;
		}		
		return null;
	}
	
	/**
	 * Performs a validation then updates the patrol leg with the new values.
	 */
	@Override
	protected void okPressed() {
		String err = getValidationErrorString();
		if (err != null){
			setErrorMessage(err);
			return;
		}
		
		//update id
		editLeg.setId(txtLegId.getText().trim());
		
		//start
		LocalDate sdate = SmartUtils.toDate(startDate);
		LocalTime stime = LocalTime.MIN;
		if (opCustom.getSelection()){
			stime= SmartUtils.toTime(startTime);
		}
		editLeg.setStartDate(sdate);
		editLeg.getPatrolLegDays().get(0).setStartTime(stime);
		
		
		//end
		LocalDate edate = SmartUtils.toDate(endDate);
		LocalTime etime = LocalTime.MAX;
		if (opEndCustom.getSelection()){
			etime = SmartUtils.toTime(endTime);
		}
		editLeg.setEndDate(edate);
		editLeg.getPatrolLegDays().get(editLeg.getPatrolLegDays().size() - 1).setEndTime(etime);
		
		//update transport type
		editLeg.setType((PatrolTransportType) ((IStructuredSelection)this.cmbTransportTypeA.getSelection()).getFirstElement());
		editLeg.setMandate((PatrolMandate) ((IStructuredSelection)this.cmbMandateA.getSelection()).getFirstElement());
		
		//update leader
		Employee leaderA =   (Employee) ((IStructuredSelection)this.groupALeader.getSelection()).getFirstElement();
		
		//update pilot
		Employee pilotA = null;
		if (this.groupAPilot != null && editLeg.getType().getPatrolType().requiresPilot()){
			pilotA =   (Employee) ((IStructuredSelection)this.groupAPilot.getSelection()).getFirstElement();
		}	
		
		//update members
		editLeg.getMembers().clear();
		for (Iterator<?> iterator = this.employeeListA.iterator(); iterator.hasNext();) {
			Employee type = (Employee) iterator.next();
			PatrolLegMember member = new PatrolLegMember();
			member.setPatrolLeg(editLeg);
			member.setMember(type);
			if (type.equals(leaderA)){
				member.setIsLeader(true);
			}
			if (pilotA != null && type.equals(pilotA)){
				member.setIsPilot(true);
			}	
			editLeg.getMembers().add(member);
		}
		
		super.okPressed();
		
	}
	
}
