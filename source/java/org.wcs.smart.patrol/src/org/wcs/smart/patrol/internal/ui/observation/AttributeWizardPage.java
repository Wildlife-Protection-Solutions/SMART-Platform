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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;

/**
 * Wizard page to collect attribute information.
 * @author Emily
 * @since 1.0.0
 */
public class AttributeWizardPage extends WizardPage implements IObservationWizardPage{

	public static final String PAGE_NAME = "Attribute Wizard Page";

	private TableViewer attributeTable = null;
	private WritableList observations;
	
	private Category currentCategory;
	private List<Attribute> catAttributes;
	
	private ObservationSummaryWizardPage nextPage = null;
	
	/**
	 * @param pageName
	 */
	protected AttributeWizardPage(Wizard wizard) {
		super(PAGE_NAME);
		wizard.addPage(this);
	}

	/**
	 * Listener that listens for changes to the attribute table;
	 * updates the table viewer and validates the changes.
	 */
	private IAttributeTableChangeListener tableChange = new IAttributeTableChangeListener() {
		@Override
		public void updated() {
			attributeTable.refresh();
			validate();
		}
	};
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		setControl(main);
		
		currentCategory = ((ObservationWizard)getWizard()).getCurrentObservation();
		
		catAttributes = new ArrayList<Attribute>();
		currentCategory.getAllAttribute(catAttributes, true);
		
		Collection<WaypointObservation> currentObservations = ((ObservationWizard)getWizard()).getWaypointObservation(currentCategory);
		
		observations = new WritableList();
		if (currentObservations == null){
			//	by default add a single Observation
			addObservation(currentCategory);
		}else{
			observations.addAll(currentObservations);
		}
		
		attributeTable = AttributeTable.createAttributeTable(true, main, currentCategory, tableChange);
		attributeTable.setInput(observations);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
//		gd.heightHint = attributeTable.getTable().computeSize(SWT.DEFAULT , SWT.DEFAULT).y;
		attributeTable.getTable().setLayoutData(gd);
		
		if (currentCategory.getIsMultiple()){
			Composite buttonPanel = new Composite(main, SWT.NONE);
			buttonPanel.setLayout(new GridLayout(1, false));
			buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false,
					false));

			Button btnAdd = new Button(buttonPanel, SWT.NONE);
			btnAdd.setText("Add");
			btnAdd.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					addObservation(currentCategory);
					validate();
					
				}
			});
			
			Button btnDelete = new Button(buttonPanel, SWT.NONE);
			btnDelete.setText("Delete");
			btnDelete.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e){
					for (Iterator<?> iterator = ((IStructuredSelection)attributeTable.getSelection()).iterator(); iterator.hasNext();) {
						WaypointObservation type = (WaypointObservation) iterator.next();
						observations.remove(type);
					}
					validate();
				}
			});
			
			
		}
		attributeTable.refresh();
		validate();
		((ObservationWizard)getWizard()).setCanFinish(false);
		setMessage("Enter the observation attributes for " + currentCategory.getFullCategoryName() + ".  Add multiple rows if required.");
	}
	
	private void addObservation(Category cat){
		WaypointObservation observation = new WaypointObservation();
		observation.setCategory(cat);
		this.observations.add(observation);
		//this is required so hibernate merge options doesn't throw  A collection with cascade="all-delete-orphan" was no longer referenced by the owning entity instance
		observation.setAttributes(new ArrayList<WaypointObservationAttribute>());
	}
	

	
	/**
	 * error string or null if validation successful
	 * @return
	 */
	private void validate(){
		String error = getAttributeErrorMessage();
		super.setErrorMessage(error);
		if (error == null){
			super.setPageComplete(true);
		}else{
			super.setPageComplete(false);
		}
	}
	
	
	/**
	 * Validates the attributes in the given observation.
	 * 
	 * @return 
	 */
	private String getAttributeErrorMessage(){
		Set<Attribute> required = new HashSet<Attribute>();
		
		for (Attribute att: catAttributes){
			if (att.getIsRequired()){
				required.add(att);
			}
		};
		
		for (Iterator<?> iterator = observations.iterator(); iterator.hasNext();) {
			WaypointObservation type = (WaypointObservation) iterator.next();
			for (Attribute att: required){
				WaypointObservationAttribute oatt = type.findAttribute(att);
				if (oatt == null){
					return att.getName() + " must be provided.";
				}
			}
			if (type.getAttributes() != null){
				for (WaypointObservationAttribute att : type.getAttributes()){
					String val = att.validate();
					if (val != null){
						return val;
					}
				}
			}
		}
		return null;
	}
	

	
	/**
	 * Next page is always the summary wizard page.
	 * @see org.eclipse.jface.wizard.WizardPage#getNextPage()
	 */
	@Override
    public IWizardPage getNextPage() {
		if (nextPage == null){
			nextPage = new ObservationSummaryWizardPage((Wizard)getWizard());
		}
		return nextPage;
    }
	
	/**
	 * Updates the current waypoint observations
	 * @see org.wcs.smart.patrol.internal.ui.observation.IObservationWizardPage#beforeMoveNext()
	 */
	@Override
	public boolean beforeMoveNext(IWizardPage target) {
		if (!(target instanceof ObservationWizardPage )){
			((ObservationWizard)getWizard()).setWaypointObservation(currentCategory, observations);
		}
		return true;
	}

	
	
}