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

import java.util.Locale;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.SmartFilterDialog;
import org.wcs.smart.common.filter.StringFilterComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.ui.LabelConstants;

/**
 * Filter dialog for filtering the patrols displayed in the patrol list view.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolFilterDialog extends SmartFilterDialog {

	//current filter
	private PatrolViewFilter currentFilter;
	
	private DateFilterComposite dateFilterCmp;
	private StringFilterComposite patrolIdFilterCmp;
	
	//type filter
	private Button btnFilterTypes;
	private Button btnIncludeAllTypes;
	private CheckboxTableViewer patrolTypeTableViewer;
	
	/**
	 * Create the dialog.
	 * @param parent parent shell
	 * @param filter the filter to update
	 */
	public PatrolFilterDialog(Shell parent, IPatrolFilteringView view) {
		super(parent, view);
		currentFilter = view.getFilter();
	}

	
	@Override
	protected void resetFilterModel() {
		currentFilter.setDefaults();
	}
	
	/*
	 * Updates the current filter with the values from the user
	 */
	@Override
	protected void updateFilterModel(){
		this.currentFilter.setDateFilter(dateFilterCmp.getDateFilterForModel(),
				dateFilterCmp.getStartDateForModel(), dateFilterCmp.getEndDateForModel());
		
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
		
		currentFilter.setPatrolIdFilter(patrolIdFilterCmp.getComparisonForModel(), 
				patrolIdFilterCmp.getFilterValueForModel());
	}

	/**
	 * Updates the widgets with the values from the current filter
	 */
	@Override
	protected void updateControlsValues(){
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
		dateFilterCmp.applyState(currentFilter.getDateFilter(), currentFilter.getStartDate(), currentFilter.getEndDate());
		
		//patrol id
		patrolIdFilterCmp.applyState(currentFilter.getPatrolIdComparator(), currentFilter.getPatrolIdFilter(), null);
	}
	/**
	 * Create contents of the dialog.
	 */
	protected Control createDialogArea(Composite parent) {
		final Composite filter = (Composite)super.createDialogArea(parent);
		setMessage(Messages.PatrolFilterDialog_DialogMessage);
		setTitle(Messages.PatrolFilterDialog_DialogTitle);
		getShell().setText(Messages.PatrolFilterDialog_DialogTitle);
		
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			Composite composite = new Composite((Composite) filter, SWT.NONE);
			composite.setLayout(new GridLayout(1, false));
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

			Composite dateFilterExpComp = createGroupComposite(Messages.PatrolFilterDialog_PatrolDatesGroupLabel, composite);
			dateFilterCmp = new DateFilterComposite(dateFilterExpComp, SWT.NONE, this);

			Composite patrolType = createGroupComposite(Messages.PatrolFilterDialog_PatrolTypesGroupLabel, composite);
			createPatrolType(session, patrolType);

			Composite patrolIdComp = createGroupComposite(Messages.PatrolFilterDialog_PatrolIdGroupLabel, composite);
			patrolIdFilterCmp = new StringFilterComposite(patrolIdComp, SWT.NONE, new StringFilterComposite.TextField[]{new StringFilterComposite.TextField(Messages.PatrolFilterDialog_PatrolIdLabel, "id")}); //$NON-NLS-1$
			patrolIdFilterCmp.setIncludeAllRadioLabel(Messages.PatrolFilterDialog_OpIncludeAllPatrolsIdsLabel);
			patrolIdFilterCmp.setFilterRadioLabel(Messages.PatrolFilterDialog_OpFilterPatrolIdLabel);
			
			updateControlsValues();
		} finally {
			session.getTransaction().rollback();
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
		btnIncludeAllTypes.setText(Messages.PatrolFilterDialog_OpIncludeAllTypesLabel);
		
		btnFilterTypes = new Button(patrolTypeComp, SWT.RADIO);
		btnFilterTypes.setText(Messages.PatrolFilterDialog_OpFilterTypesLabel);
		
		patrolTypeTableViewer = CheckboxTableViewer.newCheckList(patrolTypeComp, SWT.BORDER | SWT.FULL_SELECTION);
		patrolTypeTableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));		
		patrolTypeTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		patrolTypeTableViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof PatrolType){
					return ((PatrolType) element).getType().getGuiName(Locale.getDefault());
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
	
}
