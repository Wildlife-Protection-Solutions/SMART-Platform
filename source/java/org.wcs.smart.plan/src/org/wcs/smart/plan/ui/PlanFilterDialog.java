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
package org.wcs.smart.plan.ui;

import java.util.Locale;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.wcs.smart.plan.filter.PlanFilter;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.Plan;

/**
 * Dialog for filtering plans.
 * 
 * @author Emily
 *
 */
public class PlanFilterDialog extends SmartFilterDialog {

	private PlanFilter currentFilter;

	private DateFilterComposite dateFilterCmp;
	private StringFilterComposite planIdFilter;

	// type filter
	private Button btnFilterTypes;
	private Button btnIncludeAllTypes;
	private CheckboxTableViewer planTypeTableViewer;

	public PlanFilterDialog(Shell parent,
			IPlanFilterItem view) {
		super(parent, view);
		this.currentFilter = view.getPlanFilter();
	}

	@Override
	protected void updateFilterModel() {
		currentFilter.setDateFilter(dateFilterCmp.getDateFilterForModel(),
				dateFilterCmp.getStartDateForModel(),
				dateFilterCmp.getEndDateForModel());
		currentFilter.setPatrolIdFilter(planIdFilter.getComparisonForModel(),
				planIdFilter.getFilterValueForModel(), planIdFilter.getSelectedField());

		if (btnIncludeAllTypes.getSelection()){
			currentFilter.setPlanTypes(null);
		}else{
			Plan.PlanType types[] = new Plan.PlanType[planTypeTableViewer.getCheckedElements().length];
			System.arraycopy(planTypeTableViewer.getCheckedElements(), 0, types, 0, types.length);
			currentFilter.setPlanTypes(types);
		}
	}

	@Override
	protected void resetFilterModel() {
		currentFilter.setDefaults();
	}

	@Override
	protected void updateControlsValues() {
		// patrol type
		boolean enabled = currentFilter.getPlanTypeFilters() != null;
		btnFilterTypes.setSelection(enabled);
		btnIncludeAllTypes.setSelection(!enabled);
		planTypeTableViewer.getTable().setEnabled(enabled);
		if (enabled) {
			planTypeTableViewer.setCheckedElements(currentFilter
					.getPlanTypeFilters());
		} else {
			planTypeTableViewer.setAllChecked(true);
		}

		// date
		dateFilterCmp.applyState(currentFilter.getDateFilter(),
				currentFilter.getStartDate(), currentFilter.getEndDate());

		// patrol id
		planIdFilter.applyState(currentFilter.getPlanIdComparator(),
				currentFilter.getPlanIdFilter(), currentFilter.getSearchField());
	}

	/**
	 * Create contents of the dialog.
	 */
	protected Control createDialogArea(Composite parent) {
		final Composite filter = (Composite) super.createDialogArea(parent);
		setTitle(Messages.PlanFilterDialog_Title);
		setMessage(Messages.PlanFilterDialog_Message);
		getShell().setText(Messages.PlanFilterDialog_Title);

		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			Composite composite = new Composite((Composite) filter, SWT.NONE);
			composite.setLayout(new GridLayout(1, false));
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
					false));

			Composite dateFilterExpComp = createGroupComposite(
					Messages.PlanFilterDialog_PlanDateFilter_Label, composite);
			
			dateFilterCmp = new DateFilterComposite(dateFilterExpComp,
					SWT.NONE, this){
			
				@Override
				protected DateFilter[] getDefaultDateViewerInput() {
					return new DateFilter[] {
							DateFilter.RANGE_30_DAYS,
							DateFilter.RANGE_60_DAYS,
							DateFilter.CURRENT_YEAR,
							DateFilter.CURRENT_MONTH,
							DateFilter.CUSTOM
					};
				}
				@Override
				protected ISelection getDefaultDateViewerSelection() {
					return new StructuredSelection(DateFilter.RANGE_30_DAYS);
				}

			};

			Composite patrolType = createGroupComposite(Messages.PlanFilterDialog_PlanTypeFilter_Label,
					composite);
			createPatrolType(session, patrolType);

			Composite planIdComp = createGroupComposite(Messages.PlanFilterDialog_PlanIdName_Label, composite);
			
			planIdFilter = new StringFilterComposite(planIdComp, SWT.NONE, PlanFilter.SEARCH_FIELDS);
			planIdFilter.setIncludeAllRadioLabel(Messages.PlanFilterDialog_IncludeAll_Label);
			planIdFilter.setFilterRadioLabel(Messages.PlanFilterDialog_FilterIdName_Label);
			

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
		patrolTypeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));

		btnIncludeAllTypes = new Button(patrolTypeComp, SWT.RADIO);
		btnIncludeAllTypes.setText(Messages.PlanFilterDialog_IncludeAll_Label);

		btnFilterTypes = new Button(patrolTypeComp, SWT.RADIO);
		btnFilterTypes.setText(Messages.PlanFilterDialog_FilterType_Label);

		planTypeTableViewer = CheckboxTableViewer.newCheckList(patrolTypeComp,
				SWT.BORDER | SWT.FULL_SELECTION);
		planTypeTableViewer.getTable().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, false));
		planTypeTableViewer.setContentProvider(ArrayContentProvider
				.getInstance());
		planTypeTableViewer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof Plan.PlanType) {
					return ((Plan.PlanType) element).getGuiName(Locale.getDefault());
				}
				return super.getText(element);
			}
		});
		planTypeTableViewer.setInput(Plan.PlanType.values());

		btnFilterTypes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				planTypeTableViewer.getTable().setEnabled(true);
			}
		});
		btnIncludeAllTypes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				planTypeTableViewer.getTable().setEnabled(false);
			}
		});
		return patrolTypeComp;
	}
}
