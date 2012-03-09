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
package org.wcs.smart.patrol.internal.ui.views;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.PatrolType;

/**
 * Filter dialog for filtering the patrols displayed in the patrol list view.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolFilterDialog extends TitleAreaDialog {

	private static final int APPLY_ID = 4;
	private static final int DEFAULTS_ID = 8;
	//current filter
	private PatrolViewFilter currentFilter;
	private PatrolListView view;
	
	//date filter
	private Button btnFilterDate;
	private Button btnIncludeAllDate;
	private ComboViewer dateViewer;
	private Label lblStartDateAnd;
	private Label lblStartDateBetween;
	
	//type filter
	private Button btnFilterTypes;
	private Button btnIncludeAllTypes;
	private CheckboxTableViewer patrolTypeTableViewer;
	private DateTime dtEnd;
	private DateTime dtStart;
	
	//id filter
	private Button btnFilterIds;
	private ComboViewer pidComparatorViewer;
	private Text txtPatrolIdFilter;
	private Button btnIncludeAllIds;
	private Label lblPatrolId;
	

	

	/**
	 * Create the dialog.
	 * @param parent parent shell
	 * @param filter the filter to update
	 */
	public PatrolFilterDialog(Shell parent, PatrolListView view) {
		super(parent);
		currentFilter = view.getFilter();
		this.view = view;
	}
	
	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Patrol View Filter");
	}
	
	@Override
	protected void okPressed() {
		updateFilter();
		applyUpdates();
		super.okPressed();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, APPLY_ID, "Apply", false);
		createButton(parent, DEFAULTS_ID, "Reset", false);
		super.createButtonsForButtonBar(parent);		
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == DEFAULTS_ID){
			currentFilter.setDefaults();
			updateValues();
		}else if (buttonId == APPLY_ID){
			updateFilter();
			applyUpdates();
		}
		super.buttonPressed(buttonId);
	}
	
	private void applyUpdates(){
		this.view.updateContent();
	}
	/*
	 * Updates the current filter with the values from the 
	 * user
	 */
	private void updateFilter(){
		if (btnFilterDate.getSelection()){
			PatrolViewFilter.DateFilter df = (PatrolViewFilter.DateFilter) ((IStructuredSelection)dateViewer.getSelection()).getFirstElement();
			if (df == PatrolViewFilter.DateFilter.CUSTOM){
				this.currentFilter.setDateFilter(df,SmartPlugIn.getDate(dtStart), SmartPlugIn.getDate(dtEnd));
			}else{
				this.currentFilter.setDateFilter(df, null, null);
			}
		}else{
			this.currentFilter.setDateFilter(null, null, null);
		}
		
		if (btnFilterTypes.getSelection()){
			Object[] values = patrolTypeTableViewer.getCheckedElements();
			PatrolType.Type[] types = new PatrolType.Type[values.length];
			for (int i = 0; i < values.length; i ++){
				types[i] = ((PatrolType.Type) values[i]);
			}
			this.currentFilter.setPatrolTypes(types);
		}else{
			this.currentFilter.setPatrolTypes(null);
		}
		
		if (btnFilterIds.getSelection()){
			this.currentFilter.setPatrolIdFilter(  (PatrolViewFilter.StringComparison)((IStructuredSelection)this.pidComparatorViewer.getSelection()).getFirstElement()  , txtPatrolIdFilter.getText());
		}else{
			this.currentFilter.setPatrolIdFilter(null, null);
		}
	}

	/**
	 * Updates the widgets with the values from the current filter
	 */
	private void updateValues(){
		//patrol type
		boolean enabled = currentFilter.getPatrolTypeFilters() != null;
		btnFilterTypes.setSelection(enabled);
		btnIncludeAllTypes.setSelection(!enabled);
		patrolTypeTableViewer.getTable().setEnabled(enabled);
		if (enabled){
			patrolTypeTableViewer.setCheckedElements(currentFilter.getPatrolTypeFilters());
		}else{
			patrolTypeTableViewer.setAllChecked(true);
		}
		
		//date 
		enabled = currentFilter.getDateFilter() != null;
		btnFilterDate.setSelection(enabled);
		btnIncludeAllDate.setSelection(!enabled);
		dateViewer.getControl().setEnabled(enabled);
		dtStart.setEnabled(false);
		dtEnd.setEnabled(false);
		lblStartDateAnd.setEnabled(false);
		lblStartDateBetween.setEnabled(false);
		
		if (enabled){
			dateViewer.setSelection(new StructuredSelection(currentFilter.getDateFilter()));
			if (currentFilter.getDateFilter() == PatrolViewFilter.DateFilter.CUSTOM){
				GregorianCalendar cal = SmartPlugIn.convertDate(currentFilter.getStartDate());
				dtStart.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
				cal = SmartPlugIn.convertDate(currentFilter.getEndDate());
				dtEnd.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
		
				dtStart.setEnabled(true);
				dtEnd.setEnabled(true);
				lblStartDateAnd.setEnabled(true);
				lblStartDateBetween.setEnabled(true);
			}
		}else{
			dateViewer.setSelection(new StructuredSelection(PatrolViewFilter.DateFilter.LAST_30_DAYS));
		}
		
		// patrol id
		enabled = currentFilter.getPatrolIdComparator() != null && currentFilter.getPatrolIdFilter()  != null;
		btnFilterIds.setSelection(enabled);
		btnIncludeAllIds.setSelection(!enabled);
		txtPatrolIdFilter.setEnabled(enabled);
		pidComparatorViewer.getControl().setEnabled(enabled);
		lblPatrolId.setEnabled(enabled);
		if (enabled){
			txtPatrolIdFilter.setText(currentFilter.getPatrolIdFilter());
			pidComparatorViewer.setSelection(new StructuredSelection(currentFilter.getPatrolIdComparator()));
		}else{
			pidComparatorViewer.setSelection(new StructuredSelection(PatrolViewFilter.StringComparison.CONTAINS));
		}
		
		
	}
	/**
	 * Create contents of the dialog.
	 */
	protected Control createDialogArea(Composite parent) {
		final Composite filter = (Composite)super.createDialogArea(parent);
		setMessage("Filter the patrols shown in the Patrol List View.");
		
		Session session = HibernateManager.openSession();

		try {
			Composite composite = new Composite((Composite) filter, SWT.NONE);
			composite.setLayout(new GridLayout(1, false));
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

			Composite dateFilterExpComp = createGroupComposite("Patrol Dates", composite);
			createDateFilter(dateFilterExpComp);

			Composite patrolType = createGroupComposite("Patrol Type", composite);
			createPatrolType(session, patrolType);

			Composite patrolIdComp = createGroupComposite("Patrol Id", composite);
			createIdFilter(patrolIdComp);
			
			updateValues();
		} finally {
			session.close();
		}
		
		return filter;

	}

	/*
	 * Creates the patrol type filter section
	 */
	private Composite createPatrolType(Session session, Composite parent) {
		Composite patrolTypeComp = new Composite(parent, SWT.NONE);
		patrolTypeComp.setLayout(new GridLayout(1, false));
		patrolTypeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnIncludeAllTypes = new Button(patrolTypeComp, SWT.RADIO);
		btnIncludeAllTypes.setText("Include All Patrol Type");
		
		btnFilterTypes = new Button(patrolTypeComp, SWT.RADIO);
		btnFilterTypes.setText("Filter Patrol Types:");
		
		patrolTypeTableViewer = CheckboxTableViewer.newCheckList(patrolTypeComp, SWT.BORDER | SWT.FULL_SELECTION);
		patrolTypeTableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));		
		patrolTypeTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		patrolTypeTableViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof PatrolType){
					return ((PatrolType) element).getType().getGuiName();
				}
				return super.getText(element);
			}
		});
		Object[] pts = PatrolHibernateManager.getActivePatrolTypes(SmartDB.getCurrentConservationArea(), session).toArray();
		PatrolType.Type [] pt = new PatrolType.Type[pts.length];
		for (int i = 0; i < pts.length; i ++){
			pt[i] = ((PatrolType)pts[i]).getType();
		}
		patrolTypeTableViewer.setInput(pt);
		
		
		btnFilterTypes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				patrolTypeTableViewer.getTable().setEnabled(true);
			}
		});
		btnIncludeAllTypes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				patrolTypeTableViewer.getTable().setEnabled(false);
			}
		});
		return patrolTypeComp;
	}
	
	/*
	 * Creates the date filter section
	 */
	private Composite createDateFilter(Composite parent){
		Composite dateComp = new Composite(parent, SWT.NONE);
		dateComp.setLayout(new GridLayout(1, false));
		dateComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		btnIncludeAllDate = new Button(dateComp, SWT.RADIO);
		btnIncludeAllDate.setText("Include All Dates");
		
		btnFilterDate = new Button(dateComp, SWT.RADIO);
		btnFilterDate.setText("Filter Dates:");
	
		Composite comp = new Composite(dateComp, SWT.NONE);
		comp.setLayout(new GridLayout(5, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		dateViewer = new ComboViewer(comp, SWT.READ_ONLY);
		dateViewer.setContentProvider(ArrayContentProvider.getInstance());
		dateViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof PatrolViewFilter.DateFilter){
					return ((PatrolViewFilter.DateFilter)element).getGuiName();
				}
				return super.getText(element);
			}
		});
		dateViewer.setInput(PatrolViewFilter.DateFilter.values());
		dateViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				PatrolViewFilter.DateFilter ff = (PatrolViewFilter.DateFilter)((StructuredSelection)dateViewer.getSelection()).getFirstElement();
				boolean enabled =  (ff != null && ff == PatrolViewFilter.DateFilter.CUSTOM);
				dtStart.setEnabled(enabled);
				dtEnd.setEnabled(enabled);
				lblStartDateAnd.setEnabled(enabled);
				lblStartDateBetween.setEnabled(enabled);
			}
		});
		lblStartDateBetween = new Label(comp, SWT.NONE);
		lblStartDateBetween.setText("Contains Date Between");
		
		
		dtStart = new DateTime(comp, SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
		lblStartDateAnd = new Label(comp, SWT.NONE);
		lblStartDateAnd.setText(" and ");
		dtEnd = new DateTime(comp, SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
		
		btnFilterDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dateViewer.getCombo().setEnabled(true);
			}
		});
		btnIncludeAllDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dateViewer.getCombo().setEnabled(false);
			}
		});
		return dateComp;
	}
	

	/*
	 * Creates the id filter section
	 */
	private Composite createIdFilter(Composite parent){
		Composite idComp = new Composite(parent, SWT.NONE);
		idComp.setLayout(new GridLayout(1, false));
		idComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		btnIncludeAllIds = new Button(idComp, SWT.RADIO);
		btnIncludeAllIds.setText("Include All Patrol Id");
		
		btnFilterIds = new Button(idComp, SWT.RADIO);
		btnFilterIds.setText("Filter Patrol Id:");
	
		
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		comp.setLayout(new GridLayout(3, false));
		lblPatrolId = new Label(comp, SWT.NONE);
		lblPatrolId.setText("Patrol Id ");
		
		pidComparatorViewer = new ComboViewer(comp, SWT.READ_ONLY);
		pidComparatorViewer.setContentProvider(ArrayContentProvider.getInstance());
		pidComparatorViewer.setInput(PatrolViewFilter.StringComparison.values());
		pidComparatorViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof PatrolViewFilter.StringComparison){
					return ((PatrolViewFilter.StringComparison)element).getGuiName();
				}
				return super.getText(element);
			}
		});
		
		txtPatrolIdFilter = new Text(comp, SWT.BORDER);
		txtPatrolIdFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnFilterIds.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pidComparatorViewer.getCombo().setEnabled(true);
				txtPatrolIdFilter.setEnabled(true);
				lblPatrolId.setEnabled(true);
			}
		});
		btnIncludeAllIds.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pidComparatorViewer.getCombo().setEnabled(false);
				txtPatrolIdFilter.setEnabled(false);
				lblPatrolId.setEnabled(false);
			}
		});
			
		return idComp;
	
	}
	
	private Composite createGroupComposite(String title, Composite parent){
		Group comp = new Group(parent,  SWT.NONE);
		comp.setText(title);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		comp.setLayout(new GridLayout(1, false));
		return comp;
	}
	
	
	@Override
	protected boolean isResizable() {
		return true;
	}
}
