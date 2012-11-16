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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.patrol.internal.ui.observation.field.AttributeFieldFactory;
import org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;
import org.wcs.smart.util.SmartUtils;

/**
 * Wizard page for entering attribute information about a specific category.
 * <p>
 * For categories that can only have a single observation this displays
 * only the attribute fields.
 * </p>
 * For categories that can have multiple observation this displays the
 * attribute fields, a button panel for adding observations, and a
 * table of added observations.
 * </p>
 * @author egouge
 *
 */
public class AttributeWizardPage extends WizardPage implements IObservationWizardPage{

	public static final String PAGE_NAME = "Attribute Wizard Page";

	/* current categorry & attribute list */
	private Category currentCategory;
	private List<Attribute> catAttributes;
		
	/* for multi-observation categories */
	private TableViewer attributeTable = null;
	private WritableList observations;
	private Button btnUpdate = null;
	private WaypointObservation editingOb = null;	//observation being edited
	
	private List<IAttributeField<?>> attributeFields = null;
	private WizardPage nextPage = null;
	
	//list of categories 
	private int index;	//current index in category list being processed
	
	//bold font
	private Font boldLabelFont = null;
	/**
	 * @param pageName
	 */
	protected AttributeWizardPage(Wizard wizard, int index) {
		super(PAGE_NAME);
		wizard.addPage(this);
		this.index = index;
	}
	

	@Override
	public void dispose(){
		if (attributeFields != null){
			for (IAttributeField<?> field : attributeFields){
				field.dispose();
			}
			attributeFields = null;
		}
		
		if(boldLabelFont != null){
			boldLabelFont.dispose();
		}
		
		super.dispose();
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		this.currentCategory = ((ObservationWizard)getWizard()).getCategoryToProcess(index);
		catAttributes = findAttributes(currentCategory);

	
		Label lbl = null;
		ScrolledComposite scComp = new ScrolledComposite(parent, SWT.V_SCROLL);
		
		Composite main = new Composite(scComp, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		scComp.setContent(main);
		scComp.setExpandVertical(true);
		scComp.setExpandHorizontal(true);
		setControl(scComp);
		
		//get existing observations 
		Collection<WaypointObservation> currentObservations = ((ObservationWizard)getWizard()).getWaypointObservation(currentCategory);
		
		Composite header = new Composite(main, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.horizontalSpacing = gl.verticalSpacing = gl.marginBottom = gl.marginTop = gl.marginHeight = gl.marginWidth = gl.marginLeft = gl.marginRight = 0;
		header.setLayout(gl);
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lbl = new Label(header, SWT.NONE);
		lbl.setText( SmartUtils.formatStringForLabel(currentCategory.getName()));
		lbl.setFont(getBoldFont(lbl));
		lbl.setLayoutData(new GridData(SWT.FILL,SWT.FILL, true, false));
		
		lbl = new Label(header, SWT.NONE);
		lbl.setText("Page " + (index+1) + " of " + ((ObservationWizard)getWizard()).getCategoryCount());
		lbl.setLayoutData(new GridData(SWT.RIGHT,SWT.FILL, false, false));
		
		lbl = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		createAttributeFields(main);
		if (!currentCategory.getIsMultiple()){
			if (currentObservations != null && currentObservations.size() > 0){
				WaypointObservation ob = currentObservations.iterator().next();
				editWaypointObservation(ob);
			}
		}else{
			//multiple observations

			//create button panel
			Composite buttons= new Composite(main, SWT.NONE);
			buttons.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			buttons.setLayout(new GridLayout(2, false));
			
			btnUpdate = new Button(buttons, SWT.PUSH);
			btnUpdate.setText("Update Observation");
			btnUpdate.setLayoutData(new GridData(SWT.RIGHT , SWT.FILL, true, false));
			btnUpdate.setEnabled(false);
			btnUpdate.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {					
					updateObservation();
				}
			});
			Button btnAdd = new Button(buttons, SWT.PUSH);
			btnAdd.setText("Add Observation");
			btnAdd.setLayoutData(new GridData(SWT.RIGHT , SWT.FILL, true, false));
			btnAdd.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					addObservation();
				}
			});
			
			lbl = new Label(main, SWT.NONE);
			lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
			lbl = new Label(main, SWT.HORIZONTAL | SWT.SEPARATOR);
			lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)lbl.getLayoutData()).verticalIndent = 10;
			
			createObservationTable(main);
			if (currentObservations != null){
				this.observations.addAll(currentObservations);
				AttributeTable.resizeColumns(attributeTable);
			}
			
		}
		scComp.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		setMessage("Enter the observation attributes for " + currentCategory.getFullCategoryName() + ".  Add multiple rows if required.");
	}
	
	/*
	 * Finds all the attributes associated with the given category
	 */
	private List<Attribute> findAttributes(Category category){
		List<Attribute> catAttributes = new ArrayList<Attribute>();
		category.getAllAttribute(catAttributes, true);
		return catAttributes;
	}
	
	/*
	 * gets the bold font
	 */
	private Font getBoldFont(Label lbl){
		if (boldLabelFont == null){
			FontData fd = lbl.getFont().getFontData()[0];
			fd.setStyle(SWT.BOLD);
			boldLabelFont = new Font(Display.getDefault(), fd);
		}
		return boldLabelFont;
	}
	
	/*
	 * updates an observation 
	 */
	private boolean updateObservation(){
		WaypointObservation wo = createObservationAndClear();
		if (wo != null){
			int index = observations.indexOf(editingOb);
			if (index < 0){
				index = 0;
			}
			observations.remove(editingOb);
			editingOb = null;
			observations.add(index, wo);
			attributeTable.refresh();
			btnUpdate.setEnabled(false);
			((ObservationWizard)getWizard()).setFocusNextButton();
			
			((AttributeTable.AttributeTableLabelProvider)attributeTable.getLabelProvider()).setEditingObservation(null);
			attributeTable.refresh();
			return true;
		}
		return false;
	}
	
	/*
	 * adds a new observation
	 */
	private boolean addObservation(){
		WaypointObservation wo = createObservationAndClear();
		if (wo != null){
			observations.add(wo);
			attributeTable.refresh();
			btnUpdate.setEnabled(false);
			((ObservationWizard)getWizard()).setFocusNextButton();
			return true;
		}
		return false;
	}
	
	/*
	 * adds new observation or updates existing observation
	 * being edited.
	 */
	private boolean addOrUpdateObservation(){
		if (editingOb == null){
			return addObservation();
		}else{
			return updateObservation();
		}
	}
	
	/*
	 * true if any attribute field has been modified
	 * since the last observation added
	 */
	private boolean observationModified(){
		for (IAttributeField<?> field : attributeFields){
			if (field.isModified()){
				return true;
			}
		}
		return false;
	}
	
	/*
	 * Creates the observation table for multiple observations
	 */
	private void createObservationTable(Composite parent){
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2,false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label lbl = new Label(comp, SWT.NONE);
		lbl.setText(SmartUtils.formatStringForLabel(currentCategory.getName()) + " Observations: ");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		lbl.setFont(getBoldFont(lbl));
		
		observations = new WritableList();
		attributeTable = AttributeTable.createAttributeTable(comp, currentCategory);
		attributeTable.setContentProvider(new ObservableListContentProvider());
		attributeTable.setInput(observations);
		attributeTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)attributeTable.getTable().getLayoutData()).widthHint = 200;
		
		Composite buttons = new Composite(comp, SWT.NONE);
		buttons.setLayout(new GridLayout(1, false));
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		final Button btnEdit = new Button(buttons, SWT.PUSH);
		btnEdit.setText("Edit");
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnEdit.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				if (observationModified()){
					MessageDialog md = getSaveObservationDialog();
					int ret = md.open();
					if (ret == 0){
						if (!addOrUpdateObservation()){
							return;
						}
					}else if (ret == 2){			//cancel
						return;
					}
				}
				WaypointObservation wo = (WaypointObservation) ((IStructuredSelection)attributeTable.getSelection()).getFirstElement();
				if (wo != null){
					editWaypointObservation(wo);
				}
			}
		});
		final Button btnDelete = new Button(buttons, SWT.PUSH);
		btnDelete.setText("Delete");
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = (IStructuredSelection)attributeTable.getSelection();
				if (!sel.isEmpty()){
					for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
						WaypointObservation type = (WaypointObservation) iterator.next();
						observations.remove(type);
					}
					attributeTable.refresh();
				}	
			}
		});
		btnDelete.setEnabled(false);
		btnEdit.setEnabled(false);
		attributeTable.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnDelete.setEnabled(!attributeTable.getSelection().isEmpty());
				btnEdit.setEnabled(!attributeTable.getSelection().isEmpty());
			}
		});
	}
	/*
	 * edits a waypoint boservation
	 */
	public void editWaypointObservation(WaypointObservation wo){
		editingOb = wo;
		if (btnUpdate != null){
			btnUpdate.setEnabled(true);
		}
		for (IAttributeField<?> field : attributeFields){
			WaypointObservationAttribute value = (wo.findAttribute(field.getAttribute()));
			if(value != null){
				field.setValue(value.getAttributeValue());
			}else{
				field.clear();
			}
		}
		if (attributeTable != null){
			((AttributeTable.AttributeTableLabelProvider)attributeTable.getLabelProvider()).setEditingObservation(wo);
			attributeTable.refresh();
		}
	}
	
	private MessageDialog getSaveObservationDialog(){
		return new MessageDialog(
				getShell(), 
				"Waring", 
				 null,
				"You have made modifications to the current observation.  Would you like to save these changes before proceeding?", 
				MessageDialog.QUESTION, 
				new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL}, 
				0);
	
	}
	
	/*
	 * creates attribute fields
	 */
	private void createAttributeFields(Composite parent){
		Composite cattribute = new Composite(parent, SWT.NONE);
		cattribute.setLayout(new GridLayout(2, false));
		cattribute.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		attributeFields = new ArrayList<IAttributeField<?>>();
		for (Attribute att: catAttributes){
			IAttributeField<?> field = AttributeFieldFactory.findAttributeField(att);
			attributeFields.add(field);
			field.createComposite(cattribute);
		}
		if (attributeFields.size() > 0){
			attributeFields.get(0).setFocus();
		}
	}

	/*
	 * creates a waypoint observation from the given
	 * attribute fields then clears the attribute fields
	 */
	private WaypointObservation createObservationAndClear(){
		WaypointObservation wo = new WaypointObservation();
		wo.setCategory(this.currentCategory);
		wo.setAttributes(new ArrayList<WaypointObservationAttribute>());
		
		for (IAttributeField<?> field : attributeFields){
			String err = field.validate();
			if (err != null){
				MessageDialog.openError(getShell(), "Error", "Cannot create observation: " + err);
				return null;
			}
			WaypointObservationAttribute att = new WaypointObservationAttribute();
			att.setAttribute(field.getAttribute());
			att.setObservation(wo);
			if (field.getValue() != null){
				Object x = field.getValue();
				if (field.getAttribute().getType() == AttributeType.BOOLEAN){
					if ((Boolean)x){
						att.setNumberValue(1d);
					}else{
						att.setNumberValue(0d);
					}
				}else if (field.getAttribute().getType() == AttributeType.LIST){
					att.setAttributeListItem((AttributeListItem)x);
				}else if (field.getAttribute().getType() == AttributeType.TREE){
					att.setAttributeTreeNode((AttributeTreeNode)x);
				}else if (field.getAttribute().getType() == AttributeType.TEXT){
					att.setStringValue((String)x);
				}else if (field.getAttribute().getType() == AttributeType.NUMERIC){
					att.setNumberValue((Double)x);
				}
			}
			wo.getAttributes().add(att);
		}
		for (IAttributeField<?> field : attributeFields){
			field.clear();
		}
		return wo;
	}
		/**
	 * Next page is always the summary wizard page.
	 * @see org.eclipse.jface.wizard.WizardPage#getNextPage()
	 */
	@Override
    public IWizardPage getNextPage() {
		if (nextPage == null){
			if (index + 1 < ((ObservationWizard)getWizard()).getCategoryCount()){
				nextPage = new AttributeWizardPage((Wizard)getWizard(), index + 1);
			}else{
				nextPage = new ObservationSummaryWizardPage((Wizard)getWizard());
			}
		}
		return nextPage;
    }
	
	/**
	 * Updates the current waypoint observations before moving to the next page
	 * @see org.wcs.smart.patrol.internal.ui.observation.IObservationWizardPage#beforeMoveNext()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean beforeMoveNext(IWizardPage target) {
		if (target != null && target != nextPage){		
			//we are moving backwards
			//we don't want to save anything
			((ObservationWizard)getWizard()).clearWorkingObservations();
			return true;
		}
		
		if (!currentCategory.getIsMultiple()){
			//create single observation
			WaypointObservation wo = createObservationAndClear();
			if (wo == null){
				return false;
			}else{
				ArrayList<WaypointObservation> obs = new ArrayList<WaypointObservation>();
				obs.add(wo);
				((ObservationWizard)getWizard()).setWaypointObservation(currentCategory, obs);
				return true;
			}
		}else{
			//save multiple observations
			if (observationModified()){
				//prompt if they want to save it first
				MessageDialog md = getSaveObservationDialog();
				int ret = md.open();
				if (ret == 0){
					if (!addOrUpdateObservation()){
						return false;
					}
				}else if (ret == 2){
					//cancel
					return false;
				}
			}
			if (observations.size() == 0){
				if (!MessageDialog.openQuestion(getShell(), "No Data", "You have not entered any observation details are you sure you want to continue?")){
					//cancel and let the user enter data
					return false;
				}
			}
			((ObservationWizard)getWizard()).setWaypointObservation(currentCategory, observations);
		}
		
		return true;
	}

	
	/**
	 * Updates the wizard buttons before the page is displayed
	 * @see org.wcs.smart.patrol.internal.ui.observation.IObservationWizardPage#beforeShow()
	 */
	@Override
	public void beforeShow() {
		boolean canFinish = getNextPage() instanceof ObservationSummaryWizardPage;
		((ObservationWizard)getWizard()).setCanFinish(canFinish);
		getWizard().getContainer().updateButtons();
	}
}