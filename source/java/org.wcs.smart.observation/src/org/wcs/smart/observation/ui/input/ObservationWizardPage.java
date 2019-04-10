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
package org.wcs.smart.observation.ui.input;

import java.util.List;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.PatternFilter;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.dataentry.dialog.ConfigurableModelLabelProvider;
import org.wcs.smart.dataentry.dialog.ConfigurableModelTreeContentProvider;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.ui.properties.DataModelContentProvider;
import org.wcs.smart.ui.properties.DataModelLabelProvider;

/**
 * First page of observation wizard dialog with the
 * data model category is shown and users can
 * selected categories.
 * 
 * @author egouge
 *
 */
public class ObservationWizardPage extends WizardPage implements IObservationWizardPage{

	public static final String PAGE_NAME = Messages.ObservationWizardPage_PageName;
	
	private SearchTree searchTree = null;
	private boolean moveNext = true;
	
	/**
	 * @param pageName
	 */
	protected ObservationWizardPage(Wizard wizard) {
		super(PAGE_NAME);
		wizard.addPage(this);
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		parent.setBackgroundMode(SWT.INHERIT_FORCE);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		PatternFilter patternFilter = new PatternFilter();
		
		searchTree = new SearchTree(main,  SWT.H_SCROLL | SWT.V_SCROLL  | SWT.MULTI, patternFilter, ((ObservationWizard)getWizard()).getConfigurableModel() != null);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 400;
		searchTree.setLayoutData(gd);
		TreeViewer dmTreeViewer = searchTree.getViewer();
		dmTreeViewer.setContentProvider(new DataModelContentProvider(true, true));
		dmTreeViewer.setLabelProvider(new DataModelLabelProvider());
		dmTreeViewer.setAutoExpandLevel(3);
		dmTreeViewer.setInput(  ((ObservationWizard)getWizard()).getDataModel() ); 

		TreeViewer cmTreeViewer = searchTree.getCmViewer();
		if (cmTreeViewer != null){
			cmTreeViewer.setContentProvider(new ConfigurableModelTreeContentProvider(true, false));
			cmTreeViewer.setLabelProvider(new ConfigurableModelLabelProvider());
			cmTreeViewer.setAutoExpandLevel(3);
			cmTreeViewer.setInput(  ((ObservationWizard)getWizard()).getConfigurableModel() );
		}
		
		searchTree.selectedList.addAll( ((ObservationWizard)getWizard()).getCategoriesToProcess() );
		searchTree.listViewer.refresh(true);
		searchTree.addChangeListener(new SearchTree.IChangeListener() {
			
			@Override
			public void listModified() {
				setPageComplete(searchTree.selectedList.size() > 0);
				boolean canFinish = canFinish();
				((ObservationWizard)getWizard()).setCanFinish(canFinish);
				getWizard().getContainer().updateButtons();
			}
		});
		

		
		setMessage(Messages.ObservationWizardPage_PageMessage);
		setTitle(Messages.ObservationWizardPage_PageTitle);
		
		setPageComplete(searchTree.selectedList.size() > 0);
		((ObservationWizard)getWizard()).setCanFinish(canFinish());
		setControl(main);
	}
	
	/**
	 * If there are additional observations then the previous page
	 * is the summary page.  Otherwise there is no previous page.
	 */
	@Override
	public IWizardPage getPreviousPage() {
		moveNext = false;
		if (((ObservationWizard)getWizard()).getAllObservations().size() > 0){
			return new ObservationSummaryWizardPage((Wizard) getWizard());
		}
		return null;
	}
	
	/**
	 * The wizard can immediately finish if the categories selected
	 * don't have any attribute data to enter.
	 * 
	 * @return
	 */
	private boolean canFinish(){
		return false;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.IObservationWizardPage#beforeShow()
	 */
	@Override
	public void beforeShow(){
		if (searchTree != null && !searchTree.isDisposed()){
			setPageComplete(searchTree.selectedList.size() > 0);
			((ObservationWizard)getWizard()).setCanFinish(canFinish());
		}
	}
	
	/**
	 * The next page is either the attribute page
	 * or the summary page if the current selected attribute
	 * has no attributes.
	 */
	@Override
    public IWizardPage getNextPage() {
		moveNext = true;
		List<Category> categories = searchTree.getSelectedItems();
		if (categories.size() > 0){
			((ObservationWizard)getWizard()).setCategoriesToProcess(categories);
			Category first = ((ObservationWizard)getWizard()).getNextCategory();
			return new AttributeWizardPage( (Wizard)getWizard(), first);
		}
		return null;
    }
	
	/**
	 * Update the wizard current observation before moving to the next page.
	 * 
	 * @see org.wcs.smart.patrol.internal.ui.observation.IObservationWizardPage#beforeMoveNext()
	 */
	@Override
	public boolean beforeMoveNext(IWizardPage targetPage) {
		if (!moveNext && targetPage instanceof ObservationSummaryWizardPage){
			//moving backwards to summary page
			return true;
		}
		
		if (searchTree.getSelectedItems().size() == 0){
			return false;
		}		
		return true;
	}
	
}
