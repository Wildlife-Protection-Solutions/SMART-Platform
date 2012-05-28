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

import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
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
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
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
	public static final String ID = "org.wcs.smart.ca.EmployeePropertyPage";
	
	/* ui components */
	private TableViewer tblEmployee;
	private FilterComposite txtFilter;
	private Composite container;

	private Set<EmployeeTableViewerColumn> tableColumns = new HashSet<EmployeeTableViewerColumn>();
	
	private EmployeeNameFilter nameFilter;
	private EmployeeActiveFilter activeFilter;

	/* agencies and rank lists */
	private WritableList employees = null;
	private List<Agency> agencies;
	
	private Color gray = null;
	private Color black = null;
	  
	EmployeeViewSorter sorter = new EmployeeViewSorter();
	
	/*
	 * columns of agency table
	 */
	private enum EmployeeColumn{
		IS_ACTIVE( Employee.IS_ACTIVE),
		ID (Employee.ID),
		FAMILY_NAME( Employee.FAMILY_NAME),
		GIVEN_NAME( Employee.GIVEN_NAME),
		GENDER( Employee.GENDER),
		BIRTHDATE( Employee.BIRTHDATE),
		AGENCY( Employee.AGENCY),
		RANK( Employee.RANK),
		SMART_USER( Employee.SMART_USER),
		SMART_USER_LEVEL( Employee.SMART_USER_LEVEL),
		EMPLOYEMENT_DATE( Employee.EMPLOYEMENT_DATE),
		EMPLOYEMENT_ENDDATE( Employee.EMPLOYEMENT_ENDDATE),
		DATE_CREATED( Employee.DATE_CREATED);		
		
		String name;
		
		EmployeeColumn(String name){
			this.name = name;
			
		}
	};
	
	@Override
	public boolean close(){
		boolean canClose = super.close();
		if (canClose){
			black.dispose();
			gray.dispose();
		}
		return canClose;
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
	public EmployeePropertyPage() {
		super(Display.getCurrent().getActiveShell(), "Employee List");
	}

	@Override
	public Composite createContent(Composite parent) {
		
		gray = parent.getDisplay().getSystemColor(SWT.COLOR_GRAY);
		black = parent.getDisplay().getSystemColor(SWT.COLOR_BLACK);
		
		employees = new WritableList(ca.getEmployees(), Employee.class);
		
		container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(2, false));
		

		nameFilter = new EmployeeNameFilter();
		activeFilter = new EmployeeActiveFilter();
		
		txtFilter = new FilterComposite(container, SWT.BORDER);
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
		tblEmployee.setInput(employees);
		
		tblEmployee.setFilters(new ViewerFilter[]{nameFilter, activeFilter});
		
		final Button chActive = new Button(container, SWT.CHECK);
		chActive.setText("Include Inactive Employees");
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
		
		final Button btnEdit = new Button(composite, SWT.NONE);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setToolTipText("Edit the selected employee properties.");
		btnEdit.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				editEmployee();
			}
		});
		btnEdit.setEnabled(false);
		tblEmployee.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (!tblEmployee.getSelection().isEmpty()){
					btnEdit.setEnabled(true);
				}else{
					btnEdit.setEnabled(false);
				}
			}
		});
		
		Button btnNew = new Button(composite, SWT.NONE);
		btnNew.setText("Create New ...");
		btnNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				createNewEmployee();
			}
			
		});	
		Button btnImport = new Button(composite, SWT.NONE);
		btnImport.setText("Import ...");
		
		container.addPaintListener(new PaintListener() {
			boolean called =false;
			@Override
			public void paintControl(PaintEvent e) {
				if (called) return;
				resize();
				
			}
		});
		
		setMessage("Manage the employees.");
		return container;
	}

	/*
	 * gets agencies for current conservation area
	 */
	private List<Agency> getAgencies(){
		if (agencies == null){
			getSession().beginTransaction();
			agencies = HibernateManager.getAgencies(ca, getSession());
			getSession().getTransaction().rollback();
		}
		return agencies;
	}
	
	
	/**
	 * creates a new employee and prompts
	 * user for employee information
	 */
	private void createNewEmployee(){
		EmployeeDialog dia = new EmployeeDialog(getShell(), null, ca, getAgencies(), getSession());
		dia.open();
		tblEmployee.refresh();
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
		EmployeeDialog dia = new EmployeeDialog(getShell(), e, ca, getAgencies(), getSession());
		dia.open();		
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
		TableViewer tableViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL );

		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		tableViewer.getTable().setLayoutData(layoutData);
		tableViewer.setContentProvider(new ObservableListContentProvider());
		
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);
		
		for (int i = 0; i < EmployeeColumn.values().length; i ++){
			final EmployeeColumn colum = EmployeeColumn.values()[i];
			EmployeeTableViewerColumn col = new EmployeeTableViewerColumn(tableViewer, colum);
			col.setLabelProvider(new EmployeeDataColumnProvider(colum));
			tableColumns.add(col);
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
					return "";
				}
				return DateFormat.getDateInstance().format(element.getEndEmploymentDate());
			case IS_ACTIVE:
				if (element.getEndEmploymentDate() == null){
					return "Y";
				}else{
					return "N";
				}
			case SMART_USER: return element.getSmartUserId();
			case SMART_USER_LEVEL: 
				if (element.getSmartUserLevel() == null){
					return null;
				}
				return element.getSmartUserLevel().name();
				
			
		}
		return "";
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
		return s1.compareTo(s2);
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
			case FAMILY_NAME: return e1.getFamilyName().compareTo(e2.getFamilyName());
			case GIVEN_NAME: return e1.getGivenName().compareTo(e2.getGivenName()) ;	
			case AGENCY: return compareString(e1.getAgency() == null ? null : e1.getAgency().getName(), e2.getAgency() == null ? null : e2.getAgency().getName());
			case RANK:return compareString(e1.getAgency() == null ? null : e1.getAgency().getName(), e2.getRank() == null ? null : e2.getRank().getName());
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

//	/**
//	 * Saves all modifications to the employee list.
//	 * 
//	 * @return
//	 */
//	protected void performSave(){
//		//caComposite.updateConservationArea(ca);
//		Transaction tx = session.beginTransaction();
//		try{
//			for (Employee e : ca.getEmployees()) {
//				if (e.getId() == null){
//					HibernateManager.generateEmployeeId(e, session);
//				}
//				session.saveOrUpdate(e);	
//			}
//			tx.commit();
//		}catch (RuntimeException ex){
//			tx.rollback();
//			session.close();
//			SmartPlugIn.displayLog("Error saving employees: " + ex.getMessage(), ex);
//		}
//	}
//
//	

//	/**
//	 * Reverts all changes to the emploe list
//	 */
//	@Override
//	public void performDefaults() {
//		Transaction tx = session.beginTransaction();
//		try{
//			Set<Employee> toremove = new HashSet<Employee>();
//			for (Employee e : ca.getEmployees()) {
//				if (e.getUuid() == null){
//					toremove.add(e);
//				}else{
//					session.refresh(e);
//				}
//			}
//			ca.getEmployees().removeAll(toremove);	//added employees
//		}catch (RuntimeException ex){
//			tx.rollback();
//			session.close();
//			SmartPlugIn.displayLog("Error saving employees: " + ex.getMessage(), ex);
//		}
//		
//		employees.clear();
//		employees.addAll(ca.getEmployees());
//		tblEmployee.refresh();
//	}
	
	
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
			if (ecolumn == EmployeeColumn.IS_ACTIVE){
				column.getColumn().setWidth(20);
				return;
			}
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
			for (Iterator<Employee> iterator = EmployeePropertyPage.this.employees.iterator(); iterator.hasNext();) {
				Employee e = (Employee) iterator.next();
				String str = getText(e);
				if (str != null){
					int tmp = gc.textExtent(getText(e)).x;
					if ( tmp > width){
						width = tmp;
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
				return EmployeePropertyPage.this.black;
			} else {
				return EmployeePropertyPage.this.gray;
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
		private int direction = SWT.DOWN;
		
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
				return -compareValue(column.ecolumn, (Employee)object1, (Employee)object2);
			}else{
				return compareValue(column.ecolumn, (Employee)object1, (Employee)object2);
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
			String search = ".*" + searchString.toLowerCase() + ".*";
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
