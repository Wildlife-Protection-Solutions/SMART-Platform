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

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.ui.ca.datamodel.AttributeFieldFactory;
import org.wcs.smart.ui.ca.datamodel.IAttributeField;
import org.wcs.smart.ui.properties.DialogConstants;
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

	public static final String PAGE_NAME = Messages.AttributeWizardPage_PageName;

	/* current categorry & attribute list */
	private Category currentCategory;
	private List<Attribute> catAttributes;
	private List<ObservationAttachment> currentAttachments;
	private boolean attsModified = false;
	
	/* for multi-observation categories */
	private TableViewer attributeTable = null;
	private ArrayList<WaypointObservation> observations;
	private Button btnUpdate = null;
	private Button btnAdd;
	private WaypointObservation editingOb = null;	//observation being edited
	
	private List<IAttributeField<?>> attributeFields = null;
	private WizardPage nextPage = null;
	
	//list of categories 
	private int index;	//current index in category list being processed
	
	//bold font
	private Font boldLabelFont = null;
	private boolean requiresObservation = false;
	private ListViewer attachmentViewer;
	/**
	 * @param pageName
	 */
	protected AttributeWizardPage(Wizard wizard, int index) {
		super(PAGE_NAME);
		wizard.addPage(this);
		this.index = index;
	}
	
	private void validate(){
		boolean canComplete = true;
		if (requiresObservation){
			canComplete = observations.size() > 0;
		}
		
		((ObservationWizard)getWizard()).setCanFinish(canComplete && getNextPage() instanceof ObservationSummaryWizardPage);
		setPageComplete(canComplete);

		getWizard().getContainer().getShell().setDefaultButton(btnAdd);
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
		//parent.setLayout(new GridLayout(1,false));
		Composite page = new Composite(parent, SWT.NONE);
		page.setLayout(new GridLayout(1,false));
		page.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		setControl(page);
		
		this.currentCategory = ((ObservationWizard)getWizard()).getCategoryToProcess(index);
		catAttributes = findAttributes(currentCategory);
		requiresObservation = false;
		if (currentCategory.getIsMultiple()){
			for (Attribute a : catAttributes){
				if (a.getIsRequired()){
					requiresObservation = true;
					break;
				}
			}
		}
	
		Composite top = new Composite(page, SWT.NONE);
		top.setLayout(new GridLayout(1,false));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label lbl = null;
		
		
		
		//get existing observations 
		Collection<WaypointObservation> currentObservations = ((ObservationWizard)getWizard()).getWaypointObservation(currentCategory);
		
		Composite header = new Composite(top, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.horizontalSpacing = gl.verticalSpacing = gl.marginBottom = gl.marginTop = gl.marginHeight = gl.marginWidth = gl.marginLeft = gl.marginRight = 0;
		
		header.setLayout(gl);
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lbl = new Label(header, SWT.WRAP);
		lbl.setText( SmartUtils.formatStringForLabel(currentCategory.getName()));
		lbl.setFont(getBoldFont(lbl));
		lbl.setLayoutData(new GridData(SWT.FILL,SWT.FILL, true, false));
		((GridData)lbl.getLayoutData()).widthHint = 200;
		
		lbl = new Label(header, SWT.NONE);
		lbl.setText(MessageFormat.format(Messages.AttributeWizardPage_PageNumberLabel, new Object[]{(index+1),((ObservationWizard)getWizard()).getCategoryCount()}));
		lbl.setLayoutData(new GridData(SWT.RIGHT,SWT.FILL, false, false));
		
		lbl = new Label(top, SWT.SEPARATOR | SWT.HORIZONTAL);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		ScrolledComposite scComp = new ScrolledComposite(top, SWT.V_SCROLL);
		scComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)scComp.getLayoutData()).heightHint = 200;
		
		final Composite main = new Composite(scComp, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		
		scComp.setContent(main);
		scComp.setExpandVertical(true);
		scComp.setExpandHorizontal(true);
		
		createAttributeFields(main);
		
		if (!currentCategory.getIsMultiple()){
			if (currentObservations != null && currentObservations.size() > 0){
				WaypointObservation ob = currentObservations.iterator().next();
				editObservation(ob);
			}
		}else{
			//create button panel
			Composite buttons= new Composite(top, SWT.NONE);
			buttons.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			GridLayout gl2 = new GridLayout(2, false);
			gl2.marginWidth = gl2.marginHeight = 0;
			buttons.setLayout(gl2);
			
			Label l = new Label(buttons, SWT.SEPARATOR | SWT.HORIZONTAL);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
			btnUpdate = new Button(buttons, SWT.PUSH);
			btnUpdate.setText(Messages.AttributeWizardPage_UpdateObsButton);
			btnUpdate.setLayoutData(new GridData(SWT.RIGHT , SWT.FILL, true, false));
			btnUpdate.setEnabled(false);
			btnUpdate.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {					
					updateObservation();
				}
			});
			btnAdd = new Button(buttons, SWT.PUSH);
			btnAdd.setText(Messages.AttributeWizardPage_AddObservation_Button);
			btnAdd.setLayoutData(new GridData(SWT.RIGHT , SWT.FILL, true, false));
			btnAdd.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					addObservation();
				}
			});
			
			//multiple observations
			Composite bottomPanel = new Composite(page, SWT.NONE);
			bottomPanel.setLayout(new GridLayout(1, false));
			bottomPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
			
			
			//lbl = new Label(bottomPanel, SWT.NONE);
			//lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
			lbl = new Label(bottomPanel, SWT.HORIZONTAL | SWT.SEPARATOR);
			lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)lbl.getLayoutData()).verticalIndent = 10;
			
			createObservationTable(bottomPanel);
			if (currentObservations != null){
				this.observations.addAll(currentObservations);
				AttributeTable.resizeColumns(attributeTable);
				attributeTable.refresh();
			}
			
		}
		page.pack();
		page.layout(true);
		
		scComp.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		setTitle(Messages.AttributeWizardPage_PageTitle);
		setMessage(MessageFormat.format(Messages.AttributeWizardPage_PageMessage, new Object[]{currentCategory.getFullCategoryName()}));
		validate();
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
			boldLabelFont = new Font(lbl.getDisplay(), fd);
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
			observations.add(index, wo);
			((ObservationWizard)getWizard()).setFocusNextButton();
			((ObservationWizard)getWizard()).setModified();
			clearEditObservation();		
			validate();
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
			clearEditObservation();
			((ObservationWizard)getWizard()).setFocusNextButton();
			((ObservationWizard)getWizard()).setModified();
			validate();
			attributeTable.refresh();
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
		if (attsModified){
			return true;
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
		
		Label lbl = new Label(comp, SWT.WRAP);
		lbl.setText(SmartUtils.formatStringForLabel(
				MessageFormat.format(Messages.AttributeWizardPage_CategoryObservations_Label, new Object[]{currentCategory.getName()})));
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridData)lbl.getLayoutData()).widthHint = 200;
		lbl.setFont(getBoldFont(lbl));
		
		observations = new ArrayList<WaypointObservation>();
		attributeTable = AttributeTable.createAttributeTable(comp, currentCategory);
		attributeTable.setContentProvider(ArrayContentProvider.getInstance());
		attributeTable.setInput(observations);
		attributeTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)attributeTable.getTable().getLayoutData()).widthHint = 200;
		((GridData)attributeTable.getTable().getLayoutData()).heightHint = 90;
		
		Composite buttons = new Composite(comp, SWT.NONE);
		buttons.setLayout(new GridLayout(1, false));
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		final Button btnEdit = new Button(buttons, SWT.PUSH);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
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
					editObservation(wo);
				}
			}
		});
		final Button btnDelete = new Button(buttons, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
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
					clearEditObservation();
				}	
				validate();
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
	public void editObservation(WaypointObservation wo){
		editingOb = wo;
		if (btnUpdate != null){
			btnUpdate.setEnabled(true);
			getWizard().getContainer().getShell().setDefaultButton(btnUpdate);
		}
		for (IAttributeField<?> field : attributeFields){
			WaypointObservationAttribute value = (wo.findAttribute(field.getAttribute()));
			if(value != null){
				field.setValue(value.getAttributeValue());
			}else{
				field.clear();
			}
		}

		currentAttachments = new ArrayList<ObservationAttachment>();
		if (editingOb.getAttachments() != null){
			currentAttachments.addAll(editingOb.getAttachments());
		}
		attachmentViewer.setInput(currentAttachments);
		attsModified = false;
		
		if (attributeTable != null){
			((AttributeTable.AttributeTableLabelProvider)attributeTable.getLabelProvider()).setEditingObservation(wo);
			attributeTable.refresh();
		}
		
	}
	
	/*
	 * clears the current editing observations
	 */
	private void clearEditObservation(){
		this.editingOb = null;
		this.btnUpdate.setEnabled(false);
		this.currentAttachments = new ArrayList<ObservationAttachment>();;
		attachmentViewer.setInput(currentAttachments);
		attsModified = false;
		getWizard().getContainer().getShell().setDefaultButton(btnAdd);
		if (attributeTable != null){
			((AttributeTable.AttributeTableLabelProvider)attributeTable.getLabelProvider()).setEditingObservation(null);
			attributeTable.refresh();
		}
	}
	
	private MessageDialog getSaveObservationDialog(){
		return new MessageDialog(
				getShell(), 
				Messages.AttributeWizardPage_Warning_DialogTitle, 
				 null,
				Messages.AttributeWizardPage_SaveModificationsWarningMessage, 
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
		
		
		//attachments
		Label lbl = new Label(cattribute, SWT.NONE);
		lbl.setText(Messages.AttributeWizardPage_AttachmentsLabel);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		((GridData)lbl.getLayoutData()).verticalIndent = 2;
		
		Composite compAttach = new Composite(cattribute, SWT.NONE);
		compAttach.setLayout(new GridLayout(2, false));
		compAttach.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		attachmentViewer = new ListViewer(compAttach, SWT.BORDER | SWT.V_SCROLL);
		attachmentViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)attachmentViewer.getControl().getLayoutData()).heightHint = 50;
		attachmentViewer.setContentProvider(ArrayContentProvider.getInstance());
		attachmentViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				return ((ISmartAttachment)element).getFilename();
			}
		});
		attachmentViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				ISmartAttachment att = (ISmartAttachment) ((StructuredSelection)attachmentViewer.getSelection()).getFirstElement();
				if (att != null){
					AttachmentUtil.openAttachment(att);
				}
			}
		});
		
		this.currentAttachments = new ArrayList<ObservationAttachment>();
		attachmentViewer.setInput(this.currentAttachments);
		
		Composite btnPanel = new Composite(compAttach, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		btnPanel.setLayout(gl);
		btnPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Button btnAdd = new Button(btnPanel, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//open add dialog
				FileDialog fd = new FileDialog(getShell(), SWT.MULTI);
				
				String file = fd.open();
				if (file == null) {
					return;
				}
				for (int i = 0; i < fd.getFileNames().length; i ++){
					File f = new File(fd.getFilterPath() + File.separator +  fd.getFileNames()[i]);
					if (!f.exists()){
						ObservationPlugIn.displayLog(MessageFormat.format(Messages.AttributeWizardPage_FileNotFoundError, new Object[]{f.getAbsolutePath()}), null);
						return;
					}
					
					ObservationAttachment oa = new ObservationAttachment();
					oa.setCopyFromLocation(f);
					oa.setFilename(f.getName());
					currentAttachments.add(oa);
				}
				attachmentViewer.refresh();
				attsModified = true;
			}
		});
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Button btnRemove = new Button(btnPanel, SWT.PUSH);
		btnRemove.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ISmartAttachment att = (ISmartAttachment) ((StructuredSelection)attachmentViewer.getSelection()).getFirstElement();
				if (att != null){
					currentAttachments.remove(att);
					attsModified = true;
				}
				attachmentViewer.refresh();
			}
		});
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
	}

	/*
	 * creates a waypoint observation from the given
	 * attribute fields then clears the attribute fields
	 */
	private WaypointObservation createObservationAndClear(){
		WaypointObservation wo = new WaypointObservation();
		wo.setCategory(this.currentCategory);
		wo.setAttributes(new ArrayList<WaypointObservationAttribute>());
		wo.setWaypoint(  ((ObservationWizard)getWizard()).getWaypoint()  );
		
		for (IAttributeField<?> field : attributeFields){
			String err = field.validate();
			if (err != null){
				MessageDialog.openError(getShell(), Messages.AttributeWizardPage_Error_DialogTitle, Messages.AttributeWizardPage_CannotCreateObservationError_DialogMessage + err);
				return null;
			}
			//only store non-null values
			if (field.getValue() != null){
				WaypointObservationAttribute att = new WaypointObservationAttribute();
				att.setAttribute(field.getAttribute());
				att.setObservation(wo);
				
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
				}else if (field.getAttribute().getType() == AttributeType.DATE){
					att.setDateValue((Date)x);
				}
				wo.getAttributes().add(att);
			}
		}

		// update the attachments
		wo.setAttachments(new ArrayList<ObservationAttachment>());
		
		for (ObservationAttachment a : currentAttachments){
			try{
				a.setObservation(wo);
				wo.getAttachments().add(a);
			}catch (Exception ex){
				ObservationPlugIn.displayLog(ex.getMessage(), ex);
			}
		}
		
		//clear all fields
		currentAttachments = new ArrayList<ObservationAttachment>();
		attachmentViewer.setInput(currentAttachments);
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
				if (!MessageDialog.openQuestion(getShell(), Messages.AttributeWizardPage_DataObservations_DialogTitle, Messages.AttributeWizardPage_DataObservations_DialogMessage)){
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
		
		validate();
	}
}