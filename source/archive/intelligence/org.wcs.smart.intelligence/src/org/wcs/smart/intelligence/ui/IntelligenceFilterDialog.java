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
package org.wcs.smart.intelligence.ui;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.SmartFilterDialog;
import org.wcs.smart.common.filter.StringFilterComposite;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.ui.IntelligenceViewFilter.SortBy;

/**
 * Filter dialog for filtering intelligences displayed in the intelligence list view.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceFilterDialog extends SmartFilterDialog {

	private IntelligenceViewFilter currentFilter;
	
	private DateFilterComposite receivedDateCmp;
	private DateFilterComposite relevantDateCmp;
	private StringFilterComposite nameCmp;
	private ComboViewer sortByViewer;

	public IntelligenceFilterDialog(Shell parentShell, IIntelligenceFilteringView view) {
		super(parentShell, view);
		currentFilter = view.getFilter();
	}

	/**
	 * Create contents of the dialog.
	 */
	protected Control createDialogArea(Composite parent) {
		final Composite filter = (Composite)super.createDialogArea(parent);
		setMessage(Messages.IntelligenceFilterDialog_Message);
		setTitle(Messages.IntelligenceFilterDialog_PageTitle);
		
		Composite composite = new Composite((Composite) filter, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Composite receivedGrp = createGroupComposite(Messages.IntelligenceFilterDialog_ReceivedGroup_Label, composite);
		receivedDateCmp = new DateFilterComposite(receivedGrp, SWT.NONE, this);
		
		Composite relevantGrp = createGroupComposite(Messages.IntelligenceFilterDialog_RelevantGroup_Label, composite);
		relevantDateCmp = new DateFilterComposite(relevantGrp, SWT.NONE, this) {
			@Override
			protected DateFilter[] getDefaultDateViewerInput() {
				return new DateFilter[] {
						DateFilter.NEXT_30_DAYS,
						DateFilter.NEXT_60_DAYS,
						DateFilter.LAST_30_DAYS,
						DateFilter.LAST_60_DAYS,
						DateFilter.YEAR_TO_DATE,
						DateFilter.MONTH_TO_DATE,
						DateFilter.CUSTOM
				};
			}
			@Override
			protected ISelection getDefaultDateViewerSelection() {
				return new StructuredSelection(DateFilter.NEXT_30_DAYS);
			}
		};

		Composite nameGrp = createGroupComposite(Messages.IntelligenceFilterDialog_NameGroup_Label, composite);
		nameCmp = new StringFilterComposite(nameGrp, SWT.NONE, new StringFilterComposite.TextField[]{new StringFilterComposite.TextField(Messages.IntelligenceFilterDialog_NameValue_Label, "name")}); //$NON-NLS-1$

		Composite grpSort = createGroupComposite(Messages.IntelligenceFilterDialog_SortByGroup, composite);
		grpSort.setLayout(new GridLayout(2, false));
		Label lbl = new Label(grpSort, SWT.NONE);
		lbl.setText(Messages.IntelligenceFilterDialog_SortByLabel);
		
		sortByViewer = new ComboViewer(grpSort, SWT.READ_ONLY | SWT.DROP_DOWN);
		sortByViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		sortByViewer.setContentProvider(ArrayContentProvider.getInstance());
		sortByViewer.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof IntelligenceViewFilter.SortBy){
					return ((IntelligenceViewFilter.SortBy) element).getGuiName();
				}
				return super.getText(element);
			}
		});
		sortByViewer.setInput(IntelligenceViewFilter.SortBy.values());
		
		updateControlsValues();
		
		return filter;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(Messages.IntelligenceFilterDialog_Title);
	}
	
	@Override
	protected void updateFilterModel() {
		currentFilter.setReceivedDateFilter(receivedDateCmp.getDateFilterForModel(),
				receivedDateCmp.getStartDateForModel(), receivedDateCmp.getEndDateForModel());

		currentFilter.setRelevantDateFilter(relevantDateCmp.getDateFilterForModel(),
				relevantDateCmp.getStartDateForModel(), relevantDateCmp.getEndDateForModel());
		
		currentFilter.setNameFilter(nameCmp.getComparisonForModel(), nameCmp.getFilterValueForModel());
		
		currentFilter.setSortByField((SortBy) ((IStructuredSelection)sortByViewer.getSelection()).iterator().next());
	}

	@Override
	protected void updateControlsValues() {
		receivedDateCmp.applyState(currentFilter.getReceivedDateFilter(),
				currentFilter.getReceivedDateStart(), currentFilter.getReceivedDateEnd());

		relevantDateCmp.applyState(currentFilter.getRelevantDateFilter(),
				currentFilter.getRelevantDateStart(), currentFilter.getRelevantDateEnd());
		
		nameCmp.applyState(currentFilter.getNameComparison(), currentFilter.getName(), null);
		
		sortByViewer.setSelection(new StructuredSelection(currentFilter.getSortByField()));
	}

	@Override
	protected void resetFilterModel() {
		currentFilter.resetDefaults();
	}

}
