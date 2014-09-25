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
package org.wcs.smart.er.ui.mission.export;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
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

/**
 * Filter dialog for filtering the patrols displayed in the patrol list view.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class MissionFilterDialog extends SmartFilterDialog {

	//current filter
	private MissionViewFilter currentFilter;
	
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
	public MissionFilterDialog(Shell parent, IMissionFilteringView view) {
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
		this.currentFilter.setDateFilter(dateFilterCmp.getDateFilterForModel(), dateFilterCmp.getStartDateForModel(), dateFilterCmp.getEndDateForModel());
		
	}

	/**
	 * Updates the widgets with the values from the current filter
	 */
	@Override
	protected void updateControlsValues(){
		//date 
		dateFilterCmp.applyState(currentFilter.getDateFilter(), currentFilter.getStartDate(), currentFilter.getEndDate());
		
	}
	/**
	 * Create contents of the dialog.
	 */
	protected Control createDialogArea(Composite parent) {
		final Composite filter = (Composite)super.createDialogArea(parent);
		setMessage("Filter the Missions");
		setTitle("Mission Filter");
		getShell().setText("Mission Filter");
		
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			Composite composite = new Composite((Composite) filter, SWT.NONE);
			composite.setLayout(new GridLayout(1, false));
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

			Composite dateFilterExpComp = createGroupComposite("Mission Dates", composite);
			dateFilterCmp = new DateFilterComposite(dateFilterExpComp, SWT.NONE, this);
			
			updateControlsValues();
		} finally {
			session.getTransaction().rollback();
			session.close();
		}
		
		return filter;

	}
	
}
