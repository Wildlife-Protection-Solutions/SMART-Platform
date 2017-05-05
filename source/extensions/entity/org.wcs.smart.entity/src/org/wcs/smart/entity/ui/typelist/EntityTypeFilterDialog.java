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
package org.wcs.smart.entity.ui.typelist;

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
import org.wcs.smart.common.filter.SmartFilterDialog;
import org.wcs.smart.common.filter.StringFilterComposite;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.model.Status;
import org.wcs.smart.entity.ui.EntityTypeFilter;
import org.wcs.smart.entity.ui.IEntityTypeFilteringView;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Simple filter dialog for entity types.
 * @author Emily
 *
 */
public class EntityTypeFilterDialog extends SmartFilterDialog {
	//current filter
	private EntityTypeFilter currentFilter;
	
	private StringFilterComposite idFilterCmp;
	
	private Button btnFilterTypes;
	private Button btnIncludeAllTypes;
	private Button btnFilterStatus;
	private Button btnIncludeAllStatus;
	
	private CheckboxTableViewer typeTableViewer;
	private CheckboxTableViewer statusTableViewer;
	
	/**
	 * Create the dialog.
	 * @param parent parent shell
	 * @param filter the filter to update
	 */
	public EntityTypeFilterDialog(Shell parent, IEntityTypeFilteringView view) {
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
		if (btnFilterTypes.getSelection()){
			Object[] values = typeTableViewer.getCheckedElements();
			EntityType.Type[] types = new EntityType.Type[values.length];
			for (int i = 0; i < values.length; i ++){
				types[i] = ((EntityType.Type) values[i]);
			}
			this.currentFilter.setEntityTypes(types);
		}else{
			this.currentFilter.setEntityTypes(null);
		}
		
		if (btnFilterStatus.getSelection()){
			Object[] values = statusTableViewer.getCheckedElements();
			Status[] status= new Status[values.length];
			for (int i = 0; i < values.length; i ++){
				status[i] = ((Status) values[i]);
			}
			this.currentFilter.setEntityStatus(status);
		}else{
			this.currentFilter.setEntityStatus(null);
		}
		
		currentFilter.setEntityTypeStringFilter(
				idFilterCmp.getComparisonForModel(),
				idFilterCmp.getFilterValueForModel(),
				idFilterCmp.getSelectedField());
		
	}

	/**
	 * Updates the widgets with the values from the current filter
	 */
	@Override
	protected void updateControlsValues(){
		//patrol type
		boolean enabled = currentFilter.getEntityTypeFilters() != null;
		btnFilterTypes.setSelection(enabled);
		btnIncludeAllTypes.setSelection(!enabled);
		typeTableViewer.getTable().setEnabled(enabled);
		if (enabled){
			typeTableViewer.setCheckedElements(currentFilter.getEntityTypeFilters());
		}else{
			typeTableViewer.setAllChecked(true);
		}
		
		//status filter
		enabled = currentFilter.getEntityTypeStatus() != null;
		btnFilterStatus.setSelection(enabled);
		btnIncludeAllStatus.setSelection(!enabled);
		statusTableViewer.getTable().setEnabled(enabled);
		if (enabled){
			statusTableViewer.setCheckedElements(currentFilter.getEntityTypeStatus());
		}else{
			statusTableViewer.setAllChecked(true);
		}
		
		//id/name filter
		idFilterCmp.applyState(
				currentFilter.getStringComparator(),
				currentFilter.getSearchText(),
				currentFilter.getSearchField());
	}
	/**
	 * Create contents of the dialog.
	 */
	protected Control createDialogArea(Composite parent) {
		final Composite filter = (Composite)super.createDialogArea(parent);
		setMessage(Messages.EntityTypeFilterDialog_DialogMessage);
		setTitle(Messages.EntityTypeFilterDialog_DialogTitle);
		getShell().setText(Messages.EntityTypeFilterDialog_DialogTitle);
		
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			Composite composite = new Composite((Composite) filter, SWT.NONE);
			composite.setLayout(new GridLayout(1, false));
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

			Composite status = createGroupComposite(Messages.EntityTypeFilterDialog_StatusGroupTitle, composite);
			createStatusComposite(session, status);
			
			Composite type = createGroupComposite(Messages.EntityTypeFilterDialog_EntityTypeGroupName, composite);
			createTypeComposite(session, type);
			
			Composite patrolIdComp = createGroupComposite(Messages.EntityTypeFilterDialog_IdNameGroupName, composite);
			idFilterCmp = new StringFilterComposite(
					patrolIdComp, SWT.NONE,  EntityTypeFilter.SEARCH_FIELDS);
			
			idFilterCmp.setIncludeAllRadioLabel(Messages.EntityTypeFilterDialog_IncludeAll);
			idFilterCmp.setFilterRadioLabel(Messages.EntityTypeFilterDialog_FilterLabel);
			
			updateControlsValues();
		} finally {
			session.getTransaction().rollback();
			session.close();
		}
		
		return filter;

	}

	/*
	 * Creates the type filter section
	 */
	private Composite createTypeComposite(Session session, Composite parent) {
		Composite typeComp = new Composite(parent, SWT.NONE);
		typeComp.setLayout(new GridLayout(1, false));
		typeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		btnIncludeAllTypes = new Button(typeComp, SWT.RADIO);
		btnIncludeAllTypes.setText(Messages.EntityTypeFilterDialog_IncludeAllTypes);
		
		btnFilterTypes = new Button(typeComp, SWT.RADIO);
		btnFilterTypes.setText(Messages.EntityTypeFilterDialog_FilterTypesLabel);
		
		typeTableViewer = CheckboxTableViewer.newCheckList(typeComp, SWT.BORDER | SWT.FULL_SELECTION);
		typeTableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));		
		typeTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		typeTableViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof EntityType.Type){
					return ((EntityType.Type) element).getGuiName(Locale.getDefault());
				}
				return super.getText(element);
			}
		});
		typeTableViewer.setInput(EntityType.Type.values());
		
		btnFilterTypes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				typeTableViewer.getTable().setEnabled(true);
			}
		});
		btnIncludeAllTypes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				typeTableViewer.getTable().setEnabled(false);
			}
		});
		return typeComp;
	}
	

	/*
	 * Creates the status filter section
	 */
	private Composite createStatusComposite(Session session, Composite parent) {
		Composite typeComp = new Composite(parent, SWT.NONE);
		typeComp.setLayout(new GridLayout(1, false));
		typeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnIncludeAllStatus = new Button(typeComp, SWT.RADIO);
		btnIncludeAllStatus.setText(Messages.EntityTypeFilterDialog_IncludeAllStatus);
		
		btnFilterStatus = new Button(typeComp, SWT.RADIO);
		btnFilterStatus.setText(Messages.EntityTypeFilterDialog_FilterStatus);
		
		statusTableViewer = CheckboxTableViewer.newCheckList(typeComp, SWT.BORDER | SWT.FULL_SELECTION);
		statusTableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));		
		statusTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		statusTableViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof Status){
					return ((Status) element).getGuiName(Locale.getDefault());
				}
				return super.getText(element);
			}
		});
		statusTableViewer.setInput(Status.values());
		

		btnFilterStatus.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				statusTableViewer.getTable().setEnabled(true);
			}
		});
		btnIncludeAllStatus.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				statusTableViewer.getTable().setEnabled(false);
			}
		});
		return typeComp;
	}
}