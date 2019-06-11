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
package org.wcs.smart.ui.internal.ca.properties;

import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.wcs.smart.PermissionManager;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.EmployeeTeam;
import org.wcs.smart.ca.EmployeeTeamMember;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.export.config.impl.EmployeeCsvExportConfig;
import org.wcs.smart.export.config.impl.EmployeeCsvImportConfig;
import org.wcs.smart.export.dialog.CsvCaImportDialog;
import org.wcs.smart.export.dialog.CsvExportDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.CreateEditNamedItemDialog;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.SmartStyledDialog;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.internal.ca.EmployeeDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

/**
 * Dialog for managing conservation area
 * employees.
 * 
 * @author Emily Gouge
 *
 */
public class EmployeePropertyPage extends SmartStyledTitleDialog{

	private TableViewer tblEmployee;
	private FilterComposite txtFilter;
	private TableViewer lstTeams, lstMembers;
	
	private List<Employee> employees;
	private List<Agency> agencies;
	private List<EmployeeTeam> teams;
	
	private MenuItem mnuDelete;
	private ToolItem tiDelete;
	
	private EmployeeViewSorter sorter = new EmployeeViewSorter();
	private EmployeeNameFilter nameFilter = new EmployeeNameFilter();
	private ViewerFilter activeFilter = new ViewerFilter() {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return ((Employee)element).isActive();
		}
	};
				
	/*
	 * columns of employee table
	 */
	private enum EmployeeColumn{
		IS_ACTIVE( SmartLabelProvider.EMP_IS_ACTIVE),
		ID (SmartLabelProvider.EMP_ID),
		FAMILY_NAME( SmartLabelProvider.EMP_FAMILY_NAME),
		GIVEN_NAME( SmartLabelProvider.EMP_GIVEN_NAME),
		GENDER( SmartLabelProvider.EMP_GENDER),
		BIRTHDATE( SmartLabelProvider.EMP_BIRTHDATE),
		AGENCY( SmartLabelProvider.EMP_AGENCY),
		RANK( SmartLabelProvider.EMP_RANK),
		TEAMS(SmartLabelProvider.EMP_TEAMS),
		SMART_USER( SmartLabelProvider.EMP_SMART_USER),
		SMART_USER_LEVEL( SmartLabelProvider.EMP_SMART_USER_LEVEL),
		EMPLOYEMENT_DATE( SmartLabelProvider.EMP_EMPLOYEMENT_DATE),
		EMPLOYEMENT_ENDDATE( SmartLabelProvider.EMP_EMPLOYEMENT_ENDDATE),
		DATE_CREATED( SmartLabelProvider.EMP_DATE_CREATED);		
		
		public String name;
		
		EmployeeColumn(String name){
			this.name = name;
		}
		public String getText(Employee element) {
			switch (this){
			case ID: return element.getId();
			case FAMILY_NAME: return element.getFamilyName();
			case GIVEN_NAME: return element.getGivenName();
			case AGENCY:
				if (element.getAgency() == null){
					return null;
				}
				return element.getAgency().getName();
			case RANK:
				if (element.getRank() == null){
					return null;
				}
				return element.getRank().getName();
			case GENDER: return String.valueOf(element.getGender());
			case BIRTHDATE: return element.getBirthDate() == null ? "" : DateFormat.getDateInstance().format(element.getBirthDate()); //$NON-NLS-1$
			case DATE_CREATED: return DateFormat.getDateInstance().format(element.getDateCreated());
			case EMPLOYEMENT_DATE: return DateFormat.getDateInstance().format(element.getStartEmploymentDate());
			case EMPLOYEMENT_ENDDATE: 
				if (element.getEndEmploymentDate() == null){
					return ""; //$NON-NLS-1$
				}
				return DateFormat.getDateInstance().format(element.getEndEmploymentDate());
			case IS_ACTIVE:
				if (element.getEndEmploymentDate() == null){
					return Messages.EmployeePropertyPage_ActiveFlag;
				}else{
					return Messages.EmployeePropertyPage_InActiveFlag;
				}
			case SMART_USER: return element.getSmartUserId();
			case SMART_USER_LEVEL: 
				if (element.getSmartUserLevelKeys() == null){
					return null;
				}
				return element.getSmartUserLevelKeys();
			case TEAMS:
				if (element.getEmployeeTeams().isEmpty()) return ""; //$NON-NLS-1$
				StringBuilder sb = new StringBuilder();
				for (EmployeeTeamMember m : element.getEmployeeTeams()) {
					sb.append(m.getTeam().getName());
					sb.append(", "); //$NON-NLS-1$
				}
				return sb.substring(0,sb.length() - 2);
			}
			return ""; //$NON-NLS-1$
		}
	
		private int compare(Employee e1, Employee e2) {
			switch(this) {
				case BIRTHDATE: return e1.getBirthDate().compareTo(e2.getBirthDate());
				case DATE_CREATED: return e1.getDateCreated().compareTo(e2.getDateCreated());
				case EMPLOYEMENT_DATE: return e1.getStartEmploymentDate().compareTo(e2.getStartEmploymentDate());
				case EMPLOYEMENT_ENDDATE:
					if (e1.getEndEmploymentDate() == null && e2.getEndEmploymentDate() == null) return 0;
					if (e1.getEndEmploymentDate() == null) return 1;
					if (e2.getEndEmploymentDate() == null) return -1;					
					return e1.getEndEmploymentDate().compareTo(e2.getEndEmploymentDate());		
				default:
					String s1 = getText(e1);
					String s2 = getText(e2);
					if (s1 == null && s2 == null) return 0;
					if (s1 == null) return 1;
					if (s2 == null) return -1;
					return Collator.getInstance().compare(s1, s2);
			}
			
		}
	};
	
	public EmployeePropertyPage(Shell parent) {
		super(parent);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.CLOSE_ID,IDialogConstants.CLOSE_LABEL, false);
		getButton(IDialogConstants.CLOSE_ID).setFocus();	
		super.setReturnCode(IDialogConstants.CLOSE_ID);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.CLOSE_ID == buttonId) close();
	}
	
	@Override 
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		
		Rectangle r = getShell().getMonitor().getBounds();
		if (p.x < r.width / 2){
			p.x  = r.width / 2;
		}
		if (p.y < r.height / 2){
			p.y  = r.height / 2;
		}
		return new Point(r.width / 2, r.height / 2);
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);
		
		CTabFolder items = new CTabFolder(composite,  SWT.NONE);
		items.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		CTabItem listItem = new CTabItem(items, SWT.NONE);
		listItem.setText(Messages.EmployeePropertyPage_EmployeeTabLbl);
		
		Composite employeeComp = createEmployeeArea(items);
		listItem.setControl(employeeComp);
		
		
		CTabItem teamItem = new CTabItem(items, SWT.NONE);
		teamItem.setText(Messages.EmployeePropertyPage_TeamsTabLbl);
		Composite teamComp = createTeamArea(items);
		teamItem.setControl(teamComp);
		
		refresh();
		return composite;
	}
	
	
	
	public Composite createEmployeeArea(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		area.setLayout(new GridLayout(2, false));

		
		txtFilter = new FilterComposite(area, SWT.NONE);
		txtFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtFilter.addChangeListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
				nameFilter.setSearchText(txtFilter.getPatternFilter());
				tblEmployee.refresh();	
			}
		});
		
		ToolBar tb = new ToolBar(area, SWT.FLAT | SWT.RIGHT);
		tb.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		ToolItem tiNew = new ToolItem(tb, SWT.PUSH);
		tiNew.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		tiNew.setToolTipText(Messages.EmployeePropertyPage_addemployeetooltip);
		tiNew.setText(DialogConstants.ADD_BUTTON_TEXT);
		tiNew.addListener(SWT.Selection,  e->createNewEmployee());
		
		ToolItem tiEdit = new ToolItem(tb, SWT.PUSH);
		tiEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		tiEdit.setToolTipText(Messages.EmployeePropertyPage_editemployeetooltip);
		tiEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		tiEdit.addListener(SWT.Selection,  e->editEmployee());
		tiEdit.setSelection(false);
		
		if (PermissionManager.INSTANCE.canDelete(Employee.class)){
			tiDelete = new ToolItem(tb, SWT.PUSH);
			tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			tiDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
			tiDelete.setToolTipText(Messages.EmployeePropertyPage_deleteemployeetooltip);
			tiDelete.addListener(SWT.Selection,  e->deleteEmployee());
			tiDelete.setSelection(false);
		}
		
		new ToolItem(tb, SWT.SEPARATOR);
		
		ToolItem tiImport = new ToolItem(tb, SWT.PUSH);
		tiImport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.IMPORT_ICON));
		tiImport.setToolTipText(Messages.EmployeePropertyPage_importemployeetooltip);
		tiImport.setText(DialogConstants.IMPORT_BUTTON_TEXT);
		tiImport.addListener(SWT.Selection,  e->importEmployees());
		tiImport.setSelection(false);
		
		ToolItem tiExport = new ToolItem(tb, SWT.PUSH);
		tiExport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EXPORT_ICON));
		tiExport.setToolTipText(Messages.EmployeePropertyPage_exportemployeetooltip);
		tiExport.setText(DialogConstants.EXPORT_BUTTON_TEXT);
		tiExport.addListener(SWT.Selection,  e->{
			CsvExportDialog dialog = new CsvExportDialog(getShell(), new EmployeeCsvExportConfig());
			dialog.open();
		});
		tiExport.setSelection(false);
		
		tblEmployee = createEmployeeTableViewer(area);
		tblEmployee.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		tblEmployee.setFilters(new ViewerFilter[]{nameFilter, activeFilter});
		tblEmployee.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editEmployee();
			}
		});
		
		Menu mnu = new Menu(tblEmployee.getControl());
		
		MenuItem mnuNew = new MenuItem(mnu, SWT.PUSH);
		mnuNew.setText(Messages.EmployeePropertyPage_CreateNew_Button);
		mnuNew.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuNew.addListener(SWT.Selection, e->createNewEmployee());
		
		new MenuItem(mnu, SWT.SEPARATOR);
				
		MenuItem mnuEdit = new MenuItem(mnu, SWT.PUSH);
		mnuEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		mnuEdit.addListener(SWT.Selection, e->editEmployee());
		
		mnuDelete = null;
		if (PermissionManager.INSTANCE.canDelete(Employee.class)){
			mnuDelete = new MenuItem(mnu, SWT.PUSH);
			mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
			mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			mnuDelete.addListener(SWT.Selection, e->deleteEmployee());
		}
		tblEmployee.getControl().setMenu(mnu);
		
		
		
		
		final Button chActive = new Button(area, SWT.CHECK);
		chActive.setText(Messages.EmployeePropertyPage_Op_IncludeInActive);
		chActive.setSelection(false);
		chActive.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 2, 1));
		chActive.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (chActive.getSelection()){
					tblEmployee.removeFilter(activeFilter);
				}else{
					tblEmployee.addFilter(activeFilter);
				}
			}
		});
		
		tblEmployee.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean isenabled = !tblEmployee.getStructuredSelection().isEmpty();
				mnuEdit.setEnabled(isenabled);
				if (mnuDelete != null) mnuDelete.setEnabled(isenabled);
				tiEdit.setEnabled(isenabled);
				if (mnuDelete != null) tiDelete.setEnabled(isenabled);
			}
		});
		
		getShell().setText(Messages.EmployeePropertyPage_PageTitle);
		setTitle(Messages.EmployeePropertyPage_PageTitle);
		setMessage(Messages.EmployeePropertyPage_DialogMessage);
		
		return area;
	}
	
	public Composite createTeamArea(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		area.setLayout(new GridLayout(2, true));
		
		Composite h1 = SmartUiUtils.createHeaderLabel(area, Messages.EmployeePropertyPage_TeamsHeader);
		((GridLayout)h1.getLayout()).numColumns = 2;
		((GridLayout)h1.getLayout()).marginHeight = 0;
		((GridLayout)h1.getLayout()).verticalSpacing = 0;
		
		Composite h2 = SmartUiUtils.createHeaderLabel(area, Messages.EmployeePropertyPage_Membersheader);
		((GridLayout)h2.getLayout()).numColumns = 2;
		((GridLayout)h2.getLayout()).marginHeight = 0;
		((GridLayout)h2.getLayout()).verticalSpacing = 0;
		
		ToolBar tbTeams = new ToolBar(h1, SWT.FLAT);
		tbTeams.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		
		ToolItem tiAddTeam = new ToolItem(tbTeams, SWT.PUSH);
		tiAddTeam.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		tiAddTeam.setToolTipText(Messages.EmployeePropertyPage_createteamtooltip);
		tiAddTeam.addListener(SWT.Selection, e->addTeam());
		
		ToolItem tiEditTeam = new ToolItem(tbTeams, SWT.PUSH);
		tiEditTeam.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		tiEditTeam.setToolTipText(Messages.EmployeePropertyPage_editteamtooltip);
		tiEditTeam.setEnabled(false);
		tiEditTeam.addListener(SWT.Selection, e->editTeam());
		
		ToolItem tiDeleteTeam = new ToolItem(tbTeams, SWT.PUSH);
		tiDeleteTeam.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiDeleteTeam.setToolTipText(Messages.EmployeePropertyPage_deleteteamtooltip);
		tiDeleteTeam.setEnabled(false);
		tiDeleteTeam.addListener(SWT.Selection, e->deleteTeam());
		
		ToolBar tbMembers = new ToolBar(h2, SWT.FLAT);
		tbMembers.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		
		ToolItem tiAddMember = new ToolItem(tbMembers, SWT.PUSH);
		tiAddMember.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		tiAddMember.setToolTipText(Messages.EmployeePropertyPage_addmembertooltip);
		tiAddMember.addListener(SWT.Selection, e->addTeamMembers());
		tiAddMember.setEnabled(false);
		
		ToolItem tiDeleteMember = new ToolItem(tbMembers, SWT.PUSH);
		tiDeleteMember.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiDeleteMember.setToolTipText(Messages.EmployeePropertyPage_deletemembertooltip);
		tiDeleteMember.setEnabled(false);
		tiDeleteMember.addListener(SWT.Selection, e->deleteTeamMembers());
		
		Composite outer = new Composite(area, SWT.NONE);
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		outer.setLayout(new TableColumnLayout());
		lstTeams = new TableViewer(outer, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER);
		lstTeams.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstTeams.setContentProvider(ArrayContentProvider.getInstance());
		TableViewerColumn tc1 = new TableViewerColumn(lstTeams, SWT.NONE);
		tc1.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof EmployeeTeam) return ((EmployeeTeam)element).getName();
				return super.getText(element);
			}
		});
		((TableColumnLayout)outer.getLayout()).setColumnData(tc1.getColumn(), new ColumnWeightData(1));
		int height = (int) (lstTeams.getControl().getFont().getFontData()[0].height * 3);
		lstTeams.getControl().addListener(SWT.MeasureItem, e->{e.height = height;});
		Menu teamMenu = new Menu(lstTeams.getControl());
		lstTeams.getControl().setMenu(teamMenu);
		
		MenuItem miAddTeam = new MenuItem(teamMenu, SWT.PUSH);
		miAddTeam.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAddTeam.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAddTeam.addListener(SWT.Selection, e->addTeam());
		
		MenuItem miEditTeam = new MenuItem(teamMenu, SWT.PUSH);
		miEditTeam.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEditTeam.setText(DialogConstants.EDIT_BUTTON_TEXT);
		miEditTeam.addListener(SWT.Selection, e->editTeam());
		miEditTeam.setEnabled(false);
		
		MenuItem miDeleteTeam = new MenuItem(teamMenu, SWT.PUSH);
		miDeleteTeam.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDeleteTeam.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDeleteTeam.addListener(SWT.Selection, e->deleteTeam());
		miDeleteTeam.setEnabled(false);

		outer = new Composite(area, SWT.NONE);
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		outer.setLayout(new TableColumnLayout());
		lstMembers = new TableViewer(outer, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER);
		lstMembers.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstMembers.setContentProvider(ArrayContentProvider.getInstance());
		tc1 = new TableViewerColumn(lstMembers, SWT.NONE);
		tc1.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof EmployeeTeamMember) return SmartLabelProvider.getFullLabel(((EmployeeTeamMember)element).getEmployee());
				return super.getText(element);
			}
		});
		((TableColumnLayout)outer.getLayout()).setColumnData(tc1.getColumn(), new ColumnWeightData(1));
		
		Menu memberMenu = new Menu(lstMembers.getControl());
		lstMembers.getControl().setMenu(memberMenu);
		
		MenuItem miAddMember = new MenuItem(memberMenu, SWT.PUSH);
		miAddMember.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAddMember.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAddMember.addListener(SWT.Selection, e->addTeamMembers());
		
		MenuItem miDeleteMemeber = new MenuItem(memberMenu, SWT.PUSH);
		miDeleteMemeber.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDeleteMemeber.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDeleteMemeber.setEnabled(false);
		miDeleteMemeber.addListener(SWT.Selection, e->deleteTeamMembers());

		
		lstMembers.addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean isActive = !lstMembers.getStructuredSelection().isEmpty();
				
				tiAddMember.setEnabled(isActive);
				tiDeleteMember.setEnabled(isActive);
				miAddMember.setEnabled(isActive);
				miDeleteMemeber.setEnabled(isActive);
			}
		});
		lstTeams.addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean isActive = !lstTeams.getStructuredSelection().isEmpty();
				
				tiEditTeam.setEnabled(isActive);
				tiDeleteTeam.setEnabled(isActive);
				miEditTeam.setEnabled(isActive);
				miDeleteTeam.setEnabled(isActive);
				tiAddMember.setEnabled(isActive);
				miAddMember.setEnabled(isActive);
				Object i = lstTeams.getStructuredSelection().getFirstElement();
				if (i instanceof EmployeeTeam) {
					lstMembers.setInput(((EmployeeTeam) i).getMembers());
					String name = ((EmployeeTeam)i).getName();
					if (name.length() > 40) name = name.substring(0,40) + "..."; //$NON-NLS-1$
					((Label)h2.getChildren()[0]).setText(Messages.EmployeePropertyPage_MembersHeader + name);
					h2.layout(true);
				}
			}
		});
		
		return area;
	}
	
	private void addTeamMembers() {
		
		Object o = lstTeams.getStructuredSelection().getFirstElement();
		
		if (!(o instanceof EmployeeTeam)) return;
		EmployeeTeam team = (EmployeeTeam)o;
		EmployeeSelectDialog dialog = new EmployeeSelectDialog(getShell());
		if (dialog.open() != Window.OK) return;
		Set<Employee> current = team.getMembers().stream().map(m->m.getEmployee()).collect(Collectors.toSet());
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				List<EmployeeTeamMember> newMembers = new ArrayList<>();
				for (Employee e : dialog.employees) {
					if (current.contains(e)) continue;
					EmployeeTeamMember m = new EmployeeTeamMember();
					m.setEmployee(e);
					m.setTeam(team);
					newMembers.add(m);
					session.save(m);
				}
				session.getTransaction().commit();
				team.getMembers().addAll(newMembers);
			}catch (Exception ex) {
				SmartPlugIn.displayLog(Messages.EmployeePropertyPage_addmemberserror + "\n\n" + ex.getMessage(), ex);  //$NON-NLS-1$
				try {
					session.getTransaction().rollback();
				}catch (Exception ex2) {
					SmartPlugIn.log(ex2.getMessage(), ex2);
				}
				
			}
		}
		lstMembers.refresh();
		refresh();
	}
	
	private void deleteTeamMembers() {
		List<EmployeeTeamMember> members = new ArrayList<>();
		for (Iterator<?> iterator = lstMembers.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object item = (Object) iterator.next();
			if (item instanceof EmployeeTeamMember) members.add((EmployeeTeamMember)item);
		}
		if (members.isEmpty()) return;
		
		EmployeeTeam team = members.get(0).getTeam();
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (EmployeeTeamMember m : members) {
					session.delete(m);
					team.getMembers().remove(m);
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				SmartPlugIn.displayLog(Messages.EmployeePropertyPage_deletemembererror + "\n\n" + ex.getMessage(), ex);  //$NON-NLS-1$
				try {
					session.getTransaction().rollback();
				}catch (Exception ex2) {
					SmartPlugIn.log(ex2.getMessage(),ex2);
				}
			}
		}
		lstMembers.refresh();
		refresh();
	}
	private void importEmployees() {
		CsvCaImportDialog dialog = new CsvCaImportDialog(getShell(), new EmployeeCsvImportConfig());
		int ret = dialog.open();
		if (ret != IDialogConstants.OK_ID) return;
		refresh();
		
	}
	private void addTeam() {
		EmployeeTeam newTeam = new EmployeeTeam();
		newTeam.setNames(new HashSet<>());
		newTeam.setConservationArea(SmartDB.getCurrentConservationArea());
		newTeam.setMembers(new ArrayList<>());
		
		CreateEditNamedItemDialog dialog = new CreateEditNamedItemDialog(getShell(), newTeam) {
			protected Control createDialogArea(Composite parent) {
				Control c = super.createDialogArea(parent);
				setTitle(Messages.EmployeePropertyPage_NewTeamTitle);
				getShell().setText(Messages.EmployeePropertyPage_newteamtext);
				setMessage(Messages.EmployeePropertyPage_newteammessage);
				return c;
			}
		};
		if (dialog.open() != Window.OK) return;
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				session.saveOrUpdate(newTeam);
				session.getTransaction().commit();
			}catch (Exception ex) {
				SmartPlugIn.displayLog(Messages.EmployeePropertyPage_newteamerror + "\n\n" + ex.getMessage(), ex);  //$NON-NLS-1$
				try {
					session.getTransaction().rollback();
				}catch (Exception ex2) {
					SmartPlugIn.log(ex2.getMessage(), ex2);
				}
			}
		}
		refresh();
	}
	
	private void editTeam() {
		Object x = lstTeams.getStructuredSelection().getFirstElement();
		if (!(x instanceof EmployeeTeam)) return;
		EmployeeTeam editTeam = (EmployeeTeam)x;
		CreateEditNamedItemDialog dialog = new CreateEditNamedItemDialog(getShell(), editTeam) {
			protected Control createDialogArea(Composite parent) {
				Control c = super.createDialogArea(parent);
				setTitle(Messages.EmployeePropertyPage_editteamtitle);
				getShell().setText(Messages.EmployeePropertyPage_editteamtext);
				setMessage(Messages.EmployeePropertyPage_editteammessage);
				return c;
			}
		};
				
		if (dialog.open() != Window.OK) return;
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				session.update(editTeam);
				session.getTransaction().commit();
			}catch (Exception ex) {
				SmartPlugIn.displayLog(Messages.EmployeePropertyPage_editteamerror + "\n\n" + ex.getMessage(), ex);  //$NON-NLS-1$
				try {
					session.getTransaction().rollback();
				}catch (Exception ex2) {
					SmartPlugIn.log(ex2.getMessage(), ex2);
				}
			}
		}
		refresh();
	}
	
	private void deleteTeam() {
		List<EmployeeTeam> teams = new ArrayList<>();
		for (Iterator<?> iterator = lstTeams.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object item = (Object) iterator.next();
			if (item instanceof EmployeeTeam) teams.add((EmployeeTeam)item);
		}
		if (teams.isEmpty()) return;
		
		if (!MessageDialog.openQuestion(getShell(), Messages.EmployeePropertyPage_deletetitle, MessageFormat.format(Messages.EmployeePropertyPage_deleteconfirm, teams.size()))) return;
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (EmployeeTeam t : teams) session.delete(t);
				session.getTransaction().commit();
			}catch (Exception ex) {
				SmartPlugIn.displayLog(Messages.EmployeePropertyPage_deleteerror + "\n\n" + ex.getMessage(), ex);  //$NON-NLS-1$
				try {
					session.getTransaction().rollback();
				}catch (Exception ex2) {
					SmartPlugIn.log(ex2.getMessage(), ex2);
				}
			}
		}
		refresh();
	}
	
	
	private TableViewer createEmployeeTableViewer(Composite parent){
		TableViewer tableViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL | SWT.MULTI);

		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		tableViewer.getTable().setLayoutData(layoutData);
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);
	
		for (EmployeeColumn colum : EmployeeColumn.values()){
			boolean add = true;
			if (colum == EmployeeColumn.SMART_USER || colum ==  EmployeeColumn.SMART_USER_LEVEL){
				add = PermissionManager.INSTANCE.canConfigureSmartUser(); 
			}
			if (add)  new EmployeeViewerColumn(colum, tableViewer);
		}
		
		tableViewer.setComparator(sorter);
		
		return tableViewer;
	}
	
	
	
	/**
	 * creates a new employee and prompts
	 * user for employee information
	 */
	private void createNewEmployee(){
		EmployeeDialog dia = new EmployeeDialog(getShell(), null, SmartDB.getCurrentConservationArea(), agencies);
		dia.open();
		refresh();
	}
	
	/**
	 * displays dialog for editing employee information
	 */
	private void editEmployee(){
		/* get employee to edit */
		IStructuredSelection sec = (IStructuredSelection)tblEmployee.getSelection();
		if (sec.isEmpty()){
			return;
		}
		Employee e = (Employee)sec.getFirstElement();
		EmployeeDialog dia = new EmployeeDialog(getShell(), e, SmartDB.getCurrentConservationArea(), agencies);
		dia.open();		
		refresh();
	}
	
	private void refresh() {
		loadDetails.schedule();
	}
	
	/**
	 * deletes selected employee
	 */
	private void deleteEmployee(){
		/* get employee to edit */
		IStructuredSelection sec = (IStructuredSelection)tblEmployee.getSelection();
		if (sec.isEmpty()){
			return;
		}
		@SuppressWarnings("unchecked")
		final List<Employee> toDelete = sec.toList();
		
		String message = null;
		if (toDelete.size() == 1){
			message = MessageFormat.format(
					Messages.EmployeePropertyPage_DeleteEmployee_DialogMessage, 
					new Object[]{SmartLabelProvider.getFullLabel(toDelete.get(0))});
		}else{
			message = MessageFormat.format(Messages.EmployeePropertyPage_ConfirmDeleteMulti, new Object[]{toDelete.size()});
		}
		if (!MessageDialog.openConfirm(getShell(), 
				Messages.EmployeePropertyPage_8,
				message
				)){
			return;
		}
		ProgressMonitorDialog pd = new ProgressMonitorDialog(getShell());
		final boolean[] restart = {false};
		
		try {
			pd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.EmployeePropertyPage_ProgessDeleteEmployee, toDelete.size());
					try(Session s = HibernateManager.openSession()){					
						Transaction tx = s.beginTransaction();
						try{
							for (Employee del : toDelete){
								del = (Employee) s.get(Employee.class, del.getUuid()); //reload employee see #2178
								if (del == null) continue; //employee not found cannot remove
								
								monitor.subTask(SmartLabelProvider.getFullLabel(del));
								String deleteError = null;
								try{
									//first run before delete 
									ConservationAreaManager.getInstance().fireEmployeeBeforeDelete(del, s);
									
									//validate delete
									if (!DeleteManager.canDelete(del, s)){
										deleteError = MessageFormat.format(Messages.EmployeePropertyPage_CouldNotDeleteEmployee, new Object[]{SmartLabelProvider.getFullLabel(del)});
									}else{
										//delete
										if (del.equals(SmartDB.getCurrentEmployee())){
											restart[0] = true;
										}
										s.delete(del);
										employees.remove(del);
									}
								}catch (Exception ex){
									deleteError = MessageFormat.format(Messages.EmployeePropertyPage_CouldNotDeleteEmployee + "\n\n" + ex.getLocalizedMessage(), new Object[]{SmartLabelProvider.getFullLabel(del)}); //$NON-NLS-1$
									SmartPlugIn.log(ex.getMessage(), ex);
								}
								
								if (deleteError != null){
									final String errormsg = deleteError;
									Display.getDefault().syncExec(new Runnable(){
										@Override
										public void run() {
											MessageDialog.openInformation(getShell(), Messages.EmployeePropertyPage_8, errormsg);
										}});
									
								}
								monitor.worked(1);
							}
							monitor.subTask(Messages.EmployeePropertyPage_ProgressCommitChanges);
							tx.commit();
						}catch ( final Exception ex){
							try{
								tx.rollback();
							}catch (Exception ex2){
								SmartPlugIn.log("Error rolling back transaction", ex2); //$NON-NLS-1$
							}
							Display.getDefault().syncExec(new Runnable(){
	
								@Override
								public void run() {
									SmartPlugIn.displayLog(Messages.EmployeePropertyPage_Error_CannotDeleteEmployee + "\n\n" + ex.getLocalizedMessage(), ex);			 //$NON-NLS-1$
									
								}});
						}	
					}
				}
			});
		} catch (Exception e) {
			SmartPlugIn.displayLog(e.getLocalizedMessage(), e);
		}
			
		if (restart[0]){
			//restart
			PlatformUI.getWorkbench().restart();
			return;
		}
		tblEmployee.refresh();
	}
	
	private Job loadDetails = new Job(Messages.EmployeePropertyPage_loadingjob) {

		IStructuredSelection  teamSelection;
		IStructuredSelection  empSelection;
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			
			Display.getDefault().syncExec(()->{
				teamSelection = lstTeams.getStructuredSelection();
				empSelection = tblEmployee.getStructuredSelection();
			});
			try(Session s = HibernateManager.openSession()){	
				
				employees = QueryFactory.buildQuery(s, Employee.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}) //$NON-NLS-1$
						.getResultList();
				
				employees.forEach(e -> {
					if (e.getAgency() != null) e.getAgency().getName();
					if (e.getRank() != null) e.getRank().getName();
					e.getEmployeeTeams().forEach(et->et.getTeam().getName());
				});
				
				agencies = QueryFactory.buildQuery(s, Agency.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
				agencies.forEach(a->a.getRanks().forEach(r->r.getName()));
				
				teams = QueryFactory.buildQuery(s, EmployeeTeam.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
				teams.forEach(team->team.getMembers().forEach(m->m.getEmployee().getGivenName()));
			}
			
			Display.getDefault().syncExec(()->{
				tblEmployee.getControl().setVisible(false);
				tblEmployee.setInput(employees);
				tblEmployee.refresh();
				
				for (TableColumn tc : tblEmployee.getTable().getColumns()) {
					tc.pack();
					int width = tc.getWidth();
					if (width > 300) tc.setWidth(200);
				}
				tblEmployee.getControl().setVisible(true);
				
				lstTeams.setInput(teams);
				lstTeams.setSelection(teamSelection);
				tblEmployee.setSelection(empSelection);
			});
			return Status.OK_STATUS;
		}
		
	};
	
	private class EmployeeNameFilter extends ViewerFilter {

		private String searchString;

		public void setSearchText(String s) {
			// Search must be a substring of the existing value
			this.searchString = s;
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (searchString == null || searchString.length() == 0) {
				return true;
			}
			String search = ".*" + Pattern.quote(searchString.toLowerCase()) + ".*"; //$NON-NLS-1$ //$NON-NLS-2$
			
			Employee p = (Employee) element;
			Pattern ptn = Pattern.compile(search, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			if (ptn.matcher(p.getGivenName()).matches()){
				return true;
			}
			if (ptn.matcher(p.getFamilyName()).matches()){
				return true;
			}
			return false;
		}
	}
	
	private class EmployeeViewerColumn  {
		
		private TableViewerColumn tc;
		
		public EmployeeViewerColumn(EmployeeColumn c, TableViewer tv) {
			this.tc = new TableViewerColumn(tv, SWT.NONE);
			
			this.tc.getColumn().setText(c.name);
			this.tc.getColumn().setMoveable(true);
			this.tc.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof Employee) return c.getText((Employee)element);
					return super.getText(element);
				}
				@Override
				public Color getForeground(Object element) {
					if (((Employee)element).getEndEmploymentDate() == null){
						return getShell().getDisplay().getSystemColor(SWT.COLOR_BLACK);
					} else {
						return getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY);
					}
				}
			});
			this.tc.getColumn().pack();
			
			
			this.tc.getColumn().addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e){
					EmployeePropertyPage.this.sorter.setSortColumn(c, tc.getColumn());
				}
				
			});	
		}
		
	}
	private class EmployeeViewSorter extends ViewerComparator{
		EmployeeColumn index = null;
		private int direction = SWT.UP;
		
		public void setSortColumn(EmployeeColumn index, TableColumn tc){
			
			if (index == this.index){
				if (direction == SWT.DOWN){
					direction = SWT.UP;
				}else{
					direction = SWT.DOWN;
				}
			}
			this.index = index;
			EmployeePropertyPage.this.tblEmployee.getTable().setSortDirection(direction);
			EmployeePropertyPage.this.tblEmployee.getTable().setSortColumn(tc);
			EmployeePropertyPage.this.tblEmployee.refresh();
		}
		
		@Override
		public int compare(Viewer viewer, Object object1, Object object2){
			if (index == null) return 0;
			
			int idx = index.compare((Employee)object1, (Employee)object2);
			if (direction == SWT.UP) return idx;
			return -1 * idx;

		}
	};
	
	private class EmployeeSelectDialog extends SmartStyledDialog{

		private List<Employee> employees;
		private CheckboxTableViewer tblViewer;
		
		protected EmployeeSelectDialog(Shell parent) {
			super(parent);
		}
		
		@Override
		public void okPressed() {
			employees = new ArrayList<>();
			for (Object o : tblViewer.getCheckedElements()) {
				if (o instanceof Employee) employees.add((Employee)o);
			}
			super.okPressed();
		}
		
		@Override
		public Point getInitialSize() {
			return new Point(350, 400);
		}
		
		@Override
		public Control createDialogArea(Composite parent) {
			Composite main = (Composite) super.createDialogArea(parent);
			
			tblViewer = CheckboxTableViewer.newCheckList(main, SWT.BORDER | SWT.MULTI);
			tblViewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof Employee) return SmartLabelProvider.getFullLabel((Employee)element);
					return super.getText(element);
				}
			});
			tblViewer.setContentProvider(ArrayContentProvider.getInstance());
			tblViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			tblViewer.getTable().addKeyListener(new KeyAdapter() {
				//spacebar check
				@Override
				public void keyPressed(KeyEvent e) {
					if (tblViewer.getSelection().isEmpty()){
						return;
					}
					if (e.keyCode == SWT.SPACE){
						IStructuredSelection selection = ((IStructuredSelection)tblViewer.getSelection());
						selection.getFirstElement();
						boolean value = tblViewer.getChecked(selection.getFirstElement() );
						for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
							Object tp = (Object) iterator.next();
							tblViewer.setChecked(tp, !value);
						}
						e.doit = false;
								
					}
					
				}
			});
			Job load = new Job(Messages.EmployeePropertyPage_loadingjob2) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					List<Employee> items = new ArrayList<>();
					try(Session session = HibernateManager.openSession()){
						items.addAll(HibernateManager.getActiveEmployees(SmartDB.getCurrentConservationArea(), session));
					}
					Display.getDefault().syncExec(()->{
						tblViewer.setInput(items);
					});
					return Status.OK_STATUS;
				}
				
			};
			load.schedule();
			getShell().setText(Messages.EmployeePropertyPage_shelltext);
			return main;
		}
		
	}
}
