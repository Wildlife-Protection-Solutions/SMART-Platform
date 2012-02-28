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
package org.wcs.smart.patrol.internal.ui.observation;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ui.properties.DataModelContentProvider;
import org.wcs.smart.ui.properties.DataModelLabelProvider;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author Emily
 * @since 1.0.0
 */
public class ObservationWizardPage extends WizardPage implements IObservationWizardPage{

	public static final String PAGE_NAME = "Observation Wizard Page";

	private boolean isNext = true;
	
	/**
	 * @param pageName
	 */
	protected ObservationWizardPage(Wizard wizard) {
		super(PAGE_NAME);
		wizard.addPage(this);
	}
	
	
	private TreeViewer dmTreeViewer = null;
	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		PatternFilter patternFilter = new PatternFilter(){			
			protected boolean isChildMatch(Viewer viewer, Object element) {
				Object parent = ((DataModelContentProvider)((TreeViewer)viewer).getContentProvider()).getParent(element);
				if (parent != null) {
					return (isLeafMatch(viewer, parent) ? true : isChildMatch(viewer, parent));
				}
				return false;
			}

			@Override
			protected boolean isLeafMatch(Viewer viewer, Object element) {
				String labelText = ((DataModelLabelProvider) ((TreeViewer) viewer).getLabelProvider()).getText(element);
				if (labelText == null) {
					return false;
				}
				return (wordMatches(labelText) ? true : isChildMatch(viewer,element));
			}
			
		};
		FilteredTree fTree = new FilteredTree(main, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL, patternFilter, true);
		dmTreeViewer = fTree.getViewer();
		dmTreeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dmTreeViewer.setContentProvider(new DataModelContentProvider(true, true));
		dmTreeViewer.setLabelProvider(new DataModelLabelProvider());
		dmTreeViewer.setAutoExpandLevel(3);
		dmTreeViewer.setInput(  ((ObservationWizard)getWizard()).getDataModel() ); 
		
		dmTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object o = ((IStructuredSelection)dmTreeViewer.getSelection()).getFirstElement();
				if (o instanceof Category){
					setPageComplete(true);
				}else{
					setPageComplete(false);
				}
				
			}
		});
		setMessage("Select the type of observation made at the waypoint.  If multiple observations were observed select one here, additional observations can be made later.");
		super.setPageComplete(false);
		((ObservationWizard)getWizard()).setCanFinish(false);
		setControl(main);
	}
	
	@Override
	public IWizardPage getPreviousPage() {
		isNext = false;
		if (((ObservationWizard)getWizard()).getAllObservations().size() > 0){
			if (nextPage == null){
				nextPage = new ObservationSummaryWizardPage((Wizard) getWizard());
			}
			return nextPage;
		}
		return null;
		
	}
	
	private ObservationSummaryWizardPage nextPage;
	private AttributeWizardPage nextPageAtt;
	
	@Override
    public IWizardPage getNextPage() {
		isNext = true;
		Object o = ((IStructuredSelection)dmTreeViewer.getSelection()).getFirstElement();
		if (o instanceof Category && ((Category)o).hasAttributes()  ){
			if (nextPageAtt == null){
				nextPageAtt = new AttributeWizardPage((Wizard)getWizard());
			}
			return nextPageAtt;
		}else{
			if (nextPage == null){
				nextPage = new ObservationSummaryWizardPage((Wizard) getWizard());
			}
			return nextPage;
			
		}
    }
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.patrol.internal.ui.observation.IObservationWizardPage#beforeMoveNext()
	 */
	@Override
	public boolean beforeMoveNext(IWizardPage targetPage) {
		if (!isNext && targetPage instanceof ObservationSummaryWizardPage){
			((ObservationWizard)getWizard()).setCurrentObservation(null);
			//we are moving backward to summary page
			return true;
		}
		Object o = ((IStructuredSelection)dmTreeViewer.getSelection()).getFirstElement();
		if (o instanceof Category){
			if (((Category) o).getChildren() != null && ((Category)o).getChildren().size() > 0 ){
				if (MessageDialog.openQuestion(getWizard().getContainer().getShell(), "Observation", "The observation category " + ((Category)o).getName() + " you have selected contains " + ((Category)o).getChildren().size() + " sub-categories.  If possible you should select one of these sub-categories.  Do you want to select a sub-category?")){
					dmTreeViewer.setExpandedState(o, true);
					return false;
				}
			}
			if (o instanceof Category && ((Category)o).hasAttributes() ){
				((ObservationWizard)getWizard()).setCurrentObservation((Category)o);
			}else{
				((ObservationWizard)getWizard()).setWaypointObservation((Category)o, null);
			}
			return true;
		}
		return false;
	}

	
	
}
