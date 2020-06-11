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
package org.wcs.smart.incident.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.SmartFilterDialog;
import org.wcs.smart.common.filter.StringFilterComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.IncidentManager;
import org.wcs.smart.incident.internal.Messages;

/**
 * Filter dialog for filtering the incidents displayed in the incident list view.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class IncidentFilterDialog extends SmartFilterDialog {

	//current filter
	private IncidentFilter currentFilter;
	
	private DateFilterComposite dateFilterCmp;
	
	private StringFilterComposite incidentIdFilterCmp;
		
	private SourceFilterComposite sourceFilterCmp;
	
	/**
	 * Create the dialog.
	 * @param parent parent shell
	 * @param filter the filter to update
	 */
	public IncidentFilterDialog(Shell parent, IIncidentFilteringView view) {
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
		
		
		currentFilter.setIncidentIdFilter(incidentIdFilterCmp.getComparisonForModel(), 
				incidentIdFilterCmp.getFilterValueForModel());
		
		if (sourceFilterCmp != null) {
			currentFilter.getSourceIds().clear();
			currentFilter.getSourceIds().addAll(sourceFilterCmp.getIncidentFilter());
		}
	}

	/**
	 * Updates the widgets with the values from the current filter
	 */
	@Override
	protected void updateControlsValues(){
		//date 
		dateFilterCmp.applyState(currentFilter.getDateFilter(), currentFilter.getStartDate(), currentFilter.getEndDate());
		
		//incident id
		incidentIdFilterCmp.applyState(currentFilter.getIncidentIdComparator(), 
				currentFilter.getIncidentIdFilter(), null);
		
		//source filter
		if (sourceFilterCmp != null) {
			sourceFilterCmp.applyState(currentFilter.getSourceIds());
		}
	}
	/**
	 * Create contents of the dialog.
	 */
	protected Control createDialogArea(Composite parent) {
		final Composite filter = (Composite)super.createDialogArea(parent);
		setMessage(Messages.IncidentFilterDialog_FilterDialogMessage);
		setTitle(Messages.IncidentFilterDialog_DialogTitle);
		getShell().setText(Messages.IncidentFilterDialog_ShellTitle);
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				Composite composite = new Composite((Composite) filter, SWT.NONE);
				composite.setLayout(new GridLayout(1, false));
				composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
				Composite dateFilterExpComp = createGroupComposite(Messages.IncidentFilterDialog_DatesLabel, composite);
				dateFilterExpComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

				dateFilterCmp = new DateFilterComposite(dateFilterExpComp, SWT.NONE, this);
	
				Composite incidentIdComp = createGroupComposite(Messages.IncidentFilterDialog_IdLabel, composite);
				incidentIdComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				incidentIdFilterCmp = new StringFilterComposite(incidentIdComp, SWT.NONE,
						new StringFilterComposite.TextField[]{new StringFilterComposite.TextField(Messages.IncidentFilterDialog_IdOptionLabel, "id")}, //$NON-NLS-1$
						new StringFilterComposite.StringComparison[]{StringFilterComposite.StringComparison.EQUALS}); 
				incidentIdFilterCmp.setIncludeAllRadioLabel(Messages.IncidentFilterDialog_IncludeAllOption);
				incidentIdFilterCmp.setFilterRadioLabel(Messages.IncidentFilterDialog_FilterOptions);
				
				
				
				if (IncidentManager.getInstance().getIncidentProviders().size() > 1) {
					Composite incidentsourceComp = createGroupComposite(Messages.IncidentFilterDialog_SourceFilterSection, composite);
					incidentsourceComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
					
					sourceFilterCmp = new SourceFilterComposite(incidentsourceComp, SWT.NONE);
				}
				updateControlsValues();
			} finally {
				session.getTransaction().rollback();
			}
		}
		
		return filter;

	}

	
}
