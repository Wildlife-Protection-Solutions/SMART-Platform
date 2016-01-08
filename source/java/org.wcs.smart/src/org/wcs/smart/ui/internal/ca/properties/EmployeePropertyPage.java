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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.PermissionManager;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.export.config.impl.EmployeeCsvExportConfig;
import org.wcs.smart.export.config.impl.EmployeeCsvImportConfig;
import org.wcs.smart.export.dialog.CsvCaImportDialog;
import org.wcs.smart.export.dialog.CsvExportDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.internal.ca.EmployeeDialog;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

/**
 * Dialog for managing conservation area
 * employees.
 * 
 * @author Emily Gouge
 *
 */
public class EmployeePropertyPage extends AbstractPropertyJHeaderDialog{

	/* ui components */
	private TableViewer tblEmployee;
	private FilterComposite txtFilter;
	private Composite container;
	private Button btnDelete;
	
	private Set<EmployeeTableViewerColumn> tableColumns = new HashSet<EmployeeTableViewerColumn>();
	
	private EmployeeNameFilter nameFilter;
	private EmployeeActiveFilter activeFilter;

	/* agencies and rank lists */
	private List<Employee> employees = null;
	private List<Agency> agencies;
	  
	private ConservationArea currentCa = null;
	EmployeeViewSorter sorter = new EmployeeViewSorter();
	
	/*
	 * columns of agency table
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
		SMART_USER( SmartLabelProvider.EMP_SMART_USER),
		SMART_USER_LEVEL( SmartLabelProvider.EMP_SMART_USER_LEVEL),
		EMPLOYEMENT_DATE( SmartLabelProvider.EMP_EMPLOYEMENT_DATE),
		EMPLOYEMENT_ENDDATE( SmartLabelProvider.EMP_EMPLOYEMENT_ENDDATE),
		DATE_CREATED( SmartLabelProvider.EMP_DATE_CREATED);		
		
		String name;
		
		EmployeeColumn(String name){
			this.name = name;
			
		}
	};
	
	@Override
	public boolean close(){
		return super.close();
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
	/**
	 * Creates a new agency and rank property page
	 */
	public EmployeePropertyPage(Shell parent) {
		super(parent, Messages.EmployeePropertyPage_Dialog_Title);

		//load the current ca
		this.currentCa = (ConservationArea) getSession().load(ConservationArea.class, SmartDB.getCurrentConservationArea().getUuid());
	}

	@Override
	public Composite createContent(Composite parent) {
		container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(2, false));
		

		nameFilter = new EmployeeNameFilter();
		activeFilter = new EmployeeActiveFilter();
		
		txtFilter = new FilterComposite(container, SWT.NONE);
		txtFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2,1));
		txtFilter.addChangeListener(new ChangeListener() {	
			@Override
			public void stateChanged(ChangeEvent e) {
				nameFilter.setSearchText(txtFilter.getPatternFilter());
				tblEmployee.refresh();	
			}
		});
		
		tblEmployee = createEmployeeTableViewer(container);
		tblEmployee.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		((GridData)tblEmployee.getTable().getLayoutData()).heightHint = tblEmployee.getTable().computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		
		tblEmployee.setFilters(new ViewerFilter[]{nameFilter, activeFilter});
		tblEmployee.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editEmployee();
			}
		});
		
		final Button chActive = new Button(container, SWT.CHECK);
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
		
		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 2, 1));
		composite.setLayout(new FillLayout(SWT.HORIZONTAL));
		
		Button btnNew = new Button(composite, SWT.NONE);
		btnNew.setText(Messages.EmployeePropertyPage_CreateNew_Button);
		btnNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				createNewEmployee();
			}
			
		});	
		
		final Button btnEdit = new Button(composite, SWT.NONE);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setToolTipText(Messages.EmployeePropertyPage_Edit_ToolTip);
		btnEdit.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				editEmployee();
			}
		});
		btnEdit.setEnabled(false);
		
		if (PermissionManager.INSTANCE.canDelete(Employee.class)){
			btnDelete = new Button(composite, SWT.NONE);
			btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
			btnDelete.setToolTipText(Messages.EmployeePropertyPage_Delete_Tooltip);
			btnDelete.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e) {
					deleteEmployees();
				}
			});
			btnDelete.setEnabled(false);
		}
		
		tblEmployee.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnEdit.setEnabled( !tblEmployee.getSelection().isEmpty() );
				if (btnDelete != null) btnDelete.setEnabled( !tblEmployee.getSelection().isEmpty() );
			}
		});
		
	
		Button btnImport = new Button(composite, SWT.NONE);
		btnImport.setText(DialogConstants.IMPORT_BUTTON_TEXT);
		btnImport.setToolTipText(Messages.EmployeePropertyPage_ImportButtonTooltip);
		btnImport.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				CsvCaImportDialog dialog = new CsvCaImportDialog(getShell(), new EmployeeCsvImportConfig());
				int ret = dialog.open();
				if (ret == IDialogConstants.CANCEL_ID){
					return;
				}else{
					refreshEmployeeList();
				}
			}
		});
		
		Button btnExport = new Button(composite, SWT.NONE);
		btnExport.setText(DialogConstants.EXPORT_BUTTON_TEXT);
		btnExport.setToolTipText(Messages.EmployeePropertyPage_ExportButtonTooltip);
		btnExport.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				CsvExportDialog dialog = new CsvExportDialog(getShell(), new EmployeeCsvExportConfig());
				dialog.open();
			}
		});
		
		container.addPaintListener(new PaintListener() {
			boolean called =false;
			@Override
			public void paintControl(PaintEvent e) {
				if (called) return;
				resize();
				
			}
		});
		refreshEmployeeList();
		setTitle(Messages.EmployeePropertyPage_PageTitle);
		setMessage(Messages.EmployeePropertyPage_DIalog_Message);
		return container;
	}

	@SuppressWarnings("unchecked")
	private void refreshEmployeeList() {
		Session s = getSession();
		s.beginTransaction();
		try{
			employees = getSession().createCriteria(Employee.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list(); //$NON-NLS-1$
		}finally{
			s.getTransaction().rollback();
		}
		tblEmployee.setInput(employees);
		tblEmployee.refresh();

	}
	
	/*
	 * gets agencies for current conservation area
	 */
	private List<Agency> getAgencies(){
		if (agencies == null){
			getSession().beginTransaction();
			try{
				agencies = HibernateManager.getAgencies(SmartDB.getCurrentConservationArea(), getSession());
				Collections.sort(agencies, new Comparator<Object>(){
					@Override
					public int compare(Object o1, Object o2) {
						return Collator.getInstance().compare(((Agency)o1).getName(), ((Agency)o2).getName());
					}});
			}finally{
				getSession().getTransaction().rollback();
			}
		}
		return agencies;
	}
	
	
	/**
	 * creates a new employee and prompts
	 * user for employee information
	 */
	private void createNewEmployee(){
		EmployeeDialog dia = new EmployeeDialog(getShell(), null, currentCa, getAgencies(), getSession());
		dia.open();
		refreshEmployeeList();
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
		EmployeeDialog dia = new EmployeeDialog(getShell(), e, currentCa, getAgencies(), getSession());
		dia.open();		
		tblEmployee.refresh();
	}
	
	/**
	 * deletes selected employee
	 */
	private void deleteEmployees(){
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
					Session s = getSession();
					Transaction tx = s.beginTransaction();
					try{
						for (Employee del : toDelete){
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

	/**
	 * resizes the employee table components; must be run in ui thread
	 */
	private void resize(){
		GC gc = new GC(tblEmployee.getTable().getDisplay());
		gc.setFont(tblEmployee.getTable().getFont());
		
		for (Iterator<EmployeeTableViewerColumn> iterator = tableColumns.iterator(); iterator.hasNext();) {
			EmployeeTableViewerColumn type =  iterator.next();
			type.resize(gc);
			
		}
		gc.dispose();
	}
	
	/**
	 * Creates the agency table.
	 * 
	 */
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
			if (add){
				EmployeeTableViewerColumn col = new EmployeeTableViewerColumn(tableViewer, colum);
				col.setLabelProvider(new EmployeeDataColumnProvider(colum));
				tableColumns.add(col);
			}
		}
		
		tableViewer.setComparator(sorter);
		
		return tableViewer;
	}
	
	/**
	 * Gets the values for a given column and employee object
	 * @param col the column to get the value for
	 * @param element the employee object
	 * @return the requested employee property
	 */
	private String getValue(EmployeeColumn col, Employee element){
		switch (col){
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
			case BIRTHDATE: return DateFormat.getDateInstance().format(element.getBirthDate());
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
				if (element.getSmartUserLevel() == null){
					return null;
				}
				return element.getSmartUserLevel().name();
				
			
		}
		return ""; //$NON-NLS-1$
	}
	
	/*
	 * null safe compare string
	 */
	private int compareString(String s1, String s2){
		if (s1 == null && s2 == null){
			return 0;
		}
		if (s1 == null){
			return -1;
		}
		if (s2 == null){
			return 1;
		}
		return Collator.getInstance().compare(s1.toLowerCase(), s2.toLowerCase());
	}
	
	/*
	 * null safe compare date
	 */
	private int compareDate(Date s1, Date s2){
		if (s1 == null && s2 == null){
			return 0;
		}
		if (s1 == null){
			return -1;
		}
		if (s2 == null){
			return 1;
		}
		return s1.compareTo(s2);
	}
	/**
	 * Compares two employee objects by a given column.
	 * Used for sorting 
	 * 
	 * @param col
	 * @param e1
	 * @param e2
	 * @return
	 */
	private int compareValue(EmployeeColumn col, Employee e1, Employee e2){	
		switch (col){
			case ID: return compareString(e1.getId(),e2.getId());
			case FAMILY_NAME: return Collator.getInstance().compare(e1.getFamilyName(),e2.getFamilyName());
			case GIVEN_NAME: return Collator.getInstance().compare(e1.getGivenName(),e2.getGivenName()) ;	
			case AGENCY: return compareString(e1.getAgency() == null ? null : e1.getAgency().getName(), e2.getAgency() == null ? null : e2.getAgency().getName());
			case RANK:return compareString(e1.getRank() == null ? null : e1.getRank().getName(), e2.getRank() == null ? null : e2.getRank().getName());
			case GENDER: return e1.getGender() == e2.getGender() ? 0 : (e1.getGender() > e2.getGender() ? 1 : -1);
			case BIRTHDATE: return compareDate(e1.getBirthDate(), e2.getBirthDate());
			case DATE_CREATED: return compareDate(e1.getDateCreated(), e2.getDateCreated());
			case EMPLOYEMENT_DATE: return compareDate(e1.getStartEmploymentDate(), e2.getStartEmploymentDate());
			case EMPLOYEMENT_ENDDATE: return compareDate(e1.getEndEmploymentDate(), e2.getEndEmploymentDate());
			case IS_ACTIVE: 
				if ((e1.getEndEmploymentDate() == null && e2.getEndEmploymentDate() == null) ||
				 (e1.getEndEmploymentDate() != null && e2.getEndEmploymentDate() != null)){
					return 0; 
				}else if (e1.getEndEmploymentDate() == null && e2.getEndEmploymentDate() != null){
					return 1;
				}else if (e1.getEndEmploymentDate() != null && e2.getEndEmploymentDate() == null){
					return -1;
				}
			case SMART_USER: return compareString(e1.getSmartUserId(),e2.getSmartUserId());
			case SMART_USER_LEVEL: return compareString(e1.getSmartUserLevel() == null ? null : e1.getSmartUserLevel().name(), e2.getSmartUserLevel() == null? null:e2.getSmartUserLevel().name());			
		}
		return 0;
	}
	
	/**
	 * Does nothing - changes are made in employee dialog.
	 * 
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	@Override
	protected boolean  performSave() {
		//Does nothing
		return true;
	}

	
	/**
	 * A column in the employee table
	 */
	class EmployeeTableViewerColumn {
		private TableViewerColumn column;
		private EmployeeDataColumnProvider usageDataColumnProvider;
		private EmployeeColumn ecolumn;
		
		public EmployeeTableViewerColumn(final TableViewer viewer, EmployeeColumn ecolumn){
			this.ecolumn = ecolumn;
			column = new TableViewerColumn(viewer, SWT.NONE);
			setWidth(25);
			column.getColumn().setText(ecolumn.name);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(true);
			
			column.getColumn().addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e){
					EmployeePropertyPage.this.sorter.setSortColumn(EmployeeTableViewerColumn.this);
				}
				
			});	
		}
				
		public void resize(GC gc){
			int width = usageDataColumnProvider.getMaximumWidth(gc);
			width = Math.max(column.getColumn().getWidth(), width);
			column.getColumn().setWidth(width);
		}
		
		public void setWidth(int width){
			column.getColumn().setWidth(width);
		}
		
		public void setLabelProvider(EmployeeDataColumnProvider provider){
			column.setLabelProvider(provider);
			this.usageDataColumnProvider = provider;
		}
	}
	

	
	/**
	 * The {@link EmployeeDataColumnProvider} is a column label provider that
	 * includes some convenience methods.
	 */
	class EmployeeDataColumnProvider extends ColumnLabelProvider {
		
		EmployeeColumn column;
		
		public EmployeeDataColumnProvider(EmployeeColumn column){
			this.column = column;
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
		public int getMaximumWidth(GC gc){ //, Object[] events) {
			int width = 0;
			Point extent = gc.textExtent(column.name);
			width = extent.x;
			if (EmployeePropertyPage.this.employees != null) {
				int cnt = 0;
				for (Iterator<?> iterator = EmployeePropertyPage.this.employees
						.iterator(); iterator.hasNext();) {
					cnt++;
					if (cnt > 100)	//only size to the first 100 employees
						break;
					Employee e = (Employee) iterator.next();
					String str = getText(e);
					if (str != null) {
						int tmp = gc.textExtent(getText(e)).x;
						if (tmp > width) {
							width = tmp;
						}
					}
				}
			}
			return width + 20;
		}

		/**
		 * This method provides a foreground colour for the cell. The cell will
		 * be black if the filter includes the includes the provided
		 * {@link UsageDataEvent}, or gray if the filter excludes it.
		 */
		@Override
		public Color getForeground(Object element) {
			if (((Employee)element).getEndEmploymentDate() == null){
				return getShell().getDisplay().getSystemColor(SWT.COLOR_BLACK);
			} else {
				return getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY);
			}
		}

		@Override
		public String getText(Object element) {
			return EmployeePropertyPage.this.getValue(column, (Employee)element);
		}

		@Override
		public Image getImage(Object element) {
			return null;
		}


	}
	
	/**
	 * A sorter for employee data
	 *
	 */
	class EmployeeViewSorter extends ViewerComparator{
		EmployeeTableViewerColumn column = null;
		private int direction = SWT.UP;
		
		public void setSortColumn(EmployeeTableViewerColumn sort){
			
			if (column != null && sort.ecolumn == column.ecolumn){
				if (direction == SWT.DOWN){
					direction = SWT.UP;
				}else{
					direction = SWT.DOWN;
				}
			}
			this.column = sort;
			EmployeePropertyPage.this.tblEmployee.getTable().setSortDirection(direction);
			EmployeePropertyPage.this.tblEmployee.getTable().setSortColumn(column.column.getColumn());
			EmployeePropertyPage.this.tblEmployee.refresh();
		}
		
		@Override
		public int compare(Viewer viewer, Object object1, Object object2){
			if (column == null){
				//no sort column picked
				return 0;
			}
			if (direction == SWT.UP){
				return compareValue(column.ecolumn, (Employee)object1, (Employee)object2);
			}else{
				return -compareValue(column.ecolumn, (Employee)object1, (Employee)object2);
			}

		}
	};
	
	/**
	 * A employee name filter
	 *
	 */
	class EmployeeNameFilter extends ViewerFilter {

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
			if (p.getGivenName().toLowerCase().matches(search)){
				return true;
			}
			if (p.getFamilyName().toLowerCase().matches(search)){
				return true;
			}
			return false;
		}
	}
	/**
	 * A filter that filters based on if an employee is active
	 * or not.
	 *
	 */
	class EmployeeActiveFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			Employee p = (Employee) element;
			return p.isActive();
		}
	}

}
