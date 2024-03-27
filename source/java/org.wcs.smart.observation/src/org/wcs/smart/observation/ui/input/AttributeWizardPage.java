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

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
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

	/* current category & attribute list */
	private Category thisCategory;
	private List<Attribute> catAttributes;
	
	private boolean attsModified = false;
	private ScrolledComposite scComp ;
	private WaypointObservation editingOb = null;
	
	/* for multi-observation categories */
	private TableViewer attributeTable = null;
	
	private Button btnUpdate = null;
	private Button btnAdd;
	private Button btnLastDefault;
	
	private List<IAttributeField<?>> attributeFields = null;
	private WizardPage nextPage = null;
	
	//bold font
	private Font boldLabelFont = null;
	private boolean requiresObservation = false;
	private AttachmentPreviewTagComposite attachmentViewer;
	private ArrayList<ISmartAttachment> currentAttachments ;

	
	private ObservationWizard getWizardInternal() {
		return ((ObservationWizard)getWizard());
	}
	/**
	 * @param pageName
	 */
	protected AttributeWizardPage(Wizard wizard, Category category) {
		super(PAGE_NAME);
		wizard.addPage(this);
		this.thisCategory = category;
	}
	
	protected AttributeWizardPage(Wizard wizard, WaypointObservation toEdit) {
		super(PAGE_NAME);
		wizard.addPage(this);
		this.editingOb = toEdit;
		this.thisCategory = toEdit.getCategory();
	}
	
	private void validate(){
		boolean canComplete = true;
		if (requiresObservation){
			canComplete = false;
			for (WaypointObservation wo : getWizardInternal().getWaypoint().getAllObservations()) {
				if (wo.getCategory().equals(thisCategory)) {
					canComplete = true;
					break;
				}
			}
		}
		
		getWizardInternal().setCanFinish(canComplete && getNextPage() instanceof ObservationSummaryWizardPage);
		setPageComplete(canComplete);
		if (editingOb == null) {
			setDefaultButton(btnAdd);
		}else {
			setDefaultButton(btnUpdate);
		}
	}

	
	private void setDefaultButton(Button btn) {
		if (btn == null) return ;
	
		//the color scheme on MAC causes white text on white background
		//with default button
		//RE: #3672
		if (SystemUtils.IS_OS_MAC) {
			if (btnLastDefault != null) {
				btnLastDefault.setBackground(btn.getBackground());
			}
			this.btnLastDefault = btn;
			btn.setBackground(null);
		}
		
		getWizard().getContainer().getShell().setDefaultButton(btn);
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
		parent.setBackgroundMode(SWT.INHERIT_FORCE);
		
		//parent.setLayout(new GridLayout(1,false));
		Composite page = new Composite(parent, SWT.NONE);
		page.setLayout(new GridLayout(1,false));
		page.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		setControl(page);

		try(Session session = HibernateManager.openSession()){
			thisCategory = session.get(Category.class, thisCategory.getUuid());
			thisCategory.getFullCategoryName();
			thisCategory.getName();
			
			catAttributes = findAttributes(thisCategory, session);
			
			requiresObservation = false;
			if (thisCategory.getIsMultiple()){
				for (Attribute a : catAttributes){
					if (a.getIsRequired()){
						requiresObservation = true;
						break;
					}
				}
			}
		}
	
		Composite top = new Composite(page, SWT.NONE);
		top.setLayout(new GridLayout(1,false));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label lbl = null;
		
		//get existing observations 		
		Composite header = new Composite(top, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.horizontalSpacing = gl.verticalSpacing = gl.marginBottom = gl.marginTop = gl.marginHeight = gl.marginWidth = gl.marginLeft = gl.marginRight = 0;
		
		header.setLayout(gl);
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lbl = new Label(header, SWT.WRAP);
		lbl.setText( SmartUtils.formatStringForLabel(thisCategory.getName()));
		lbl.setFont(getBoldFont(lbl));
		lbl.setLayoutData(new GridData(SWT.FILL,SWT.FILL, true, false));
		((GridData)lbl.getLayoutData()).widthHint = 200;
		
		if (getWizardInternal().getCategoryCount() != 0) {
			lbl = new Label(header, SWT.NONE);
			lbl.setText(MessageFormat.format(Messages.AttributeWizardPage_PageNumberLabel, new Object[]{getWizardInternal().getCategoryIndex(thisCategory)+1, getWizardInternal().getCategoryCount()}));
			lbl.setLayoutData(new GridData(SWT.RIGHT,SWT.FILL, false, false));
		}
		
		lbl = new Label(top, SWT.SEPARATOR | SWT.HORIZONTAL);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		scComp = new ScrolledComposite(top, SWT.V_SCROLL);
		scComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)scComp.getLayoutData()).heightHint = 200;
		
		final Composite main = new Composite(scComp, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		
		scComp.setContent(main);
		scComp.setExpandVertical(true);
		scComp.setExpandHorizontal(true);
		
		createAttributeFields(main);
		
		
		Collection<WaypointObservation> categoryObservations = new ArrayList<>();
		Waypoint wp = getWizardInternal().getWaypoint();
		for (WaypointObservationGroup g : wp.getObservationGroups()) {
			for (WaypointObservation wo : g.getObservations()) {
				if (wo.getCategory().equals(thisCategory)) categoryObservations.add(wo);
			}
		}
		if (!thisCategory.getIsMultiple()){
			if (categoryObservations != null && categoryObservations.size() > 0){
				WaypointObservation ob = categoryObservations.iterator().next();
				editObservation(ob);
			}
		}else{
			//create button panel
			Composite buttons= new Composite(top, SWT.NONE);
			buttons.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			GridLayout gl2 = new GridLayout(2, false);
			gl2.marginWidth = gl2.marginHeight = 0;
			buttons.setLayout(gl2);
			
//			Label l = new Label(buttons, SWT.SEPARATOR | SWT.HORIZONTAL);
//			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
			btnAdd = new Button(buttons, SWT.PUSH);
			btnAdd.setBackground(buttons.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			btnAdd.setText(Messages.AttributeWizardPage_AddObservation_Button1);
			btnAdd.setLayoutData(new GridData(SWT.RIGHT , SWT.FILL, true, false));
			btnAdd.addListener(SWT.Selection, e->addObservation());
			
			btnUpdate = new Button(buttons, SWT.PUSH);
			btnUpdate.setBackground(buttons.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			btnUpdate.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
			btnUpdate.setText(Messages.AttributeWizardPage_UpdateObsButton1);
			btnUpdate.setLayoutData(new GridData(SWT.RIGHT , SWT.FILL, true, false));
			btnUpdate.setEnabled(false);
			btnUpdate.addListener(SWT.Selection, e->updateObservation());
			
			//multiple observations
			Composite bottomPanel = new Composite(page, SWT.NONE);
			bottomPanel.setLayout(new GridLayout(1, false));
			bottomPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridLayout)bottomPanel.getLayout()).marginWidth = 0;
			((GridLayout)bottomPanel.getLayout()).marginHeight = 0;
			
			lbl = new Label(bottomPanel, SWT.HORIZONTAL | SWT.SEPARATOR);
			lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)lbl.getLayoutData()).verticalIndent = 10;
			
			createObservationTable(bottomPanel, categoryObservations);
			if (categoryObservations != null){
				AttributeTable.resizeColumns(attributeTable);
				attributeTable.refresh();
			}
			
		}
		page.pack();
		page.layout(true);
		
		scComp.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		setTitle(Messages.AttributeWizardPage_PageTitle);
		setMessage(MessageFormat.format(Messages.AttributeWizardPage_PageMessage, new Object[]{thisCategory.getFullCategoryName(true)}));
		
		if (editingOb != null) {
			editObservation(editingOb);
		}
		validate();
	}
	
	/*
	 * Finds all the attributes associated with the given category
	 */
	private List<Attribute> findAttributes(Category category, Session session){
		List<Attribute> catAttributes = new ArrayList<Attribute>();
		category.getAllAttribute(catAttributes, true);
		
		catAttributes.forEach(ca->{
			if (ca.getAttributeList() != null) {
				ca.getAttributeList().forEach(l->l.getName());
			}
			if (ca.getActiveTreeNodes() != null) {
				List<AttributeTreeNode> nodes = new ArrayList<>();
				nodes.addAll(ca.getActiveTreeNodes());
				while(!nodes.isEmpty()) {
					AttributeTreeNode n = nodes.remove(0);
					n.getName();
					if (n.getActiveChildren() != null) nodes.addAll(n.getActiveChildren());
				}
			}
		});
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
	
	private void mergeObservation(WaypointObservation toKeep, WaypointObservation toCopy) {
		//merge attributes
		List<WaypointObservationAttribute> newAttributes = toCopy.getAttributes();
		List<WaypointObservationAttribute> toDelete = new ArrayList<>();
		for (WaypointObservationAttribute newAtt : toKeep.getAttributes()) {
			WaypointObservationAttribute newValue = null;
			for (WaypointObservationAttribute a : newAttributes) {
				if (a.getAttribute().equals(newAtt.getAttribute())) {
					newValue = a;
					break;
				}
			}
			if (newValue != null) {
				newAttributes.remove(newValue);
				newAtt.setAttributeValue(newValue.getAttributeValue());
			}else {
				toDelete.add(newAtt);
			}
		}
		for (WaypointObservationAttribute n : newAttributes) {
			n.setObservation(toKeep);
			toKeep.getAttributes().add(n);
		}
		toKeep.getAttributes().removeAll(toDelete);
		
		//merge attachments
		List<ObservationAttachment> toRemove = new ArrayList<>();
		for (ObservationAttachment a : toKeep.getAttachments()) {
			if (!toCopy.getAttachments().contains(a)) toRemove.add(a);
		}
		toKeep.getAttachments().removeAll(toRemove);
		toRemove.forEach(e->getWizardInternal().removeAttachment(e));
		
		for (ObservationAttachment a : toCopy.getAttachments()) {
			if (!toKeep.getAttachments().contains(a)) {
				toKeep.getAttachments().add(a);
				Path temp = a.getCopyFromLocation() == null ? a.getAttachmentFile() : null;
				a.setObservation(toKeep);
				if (temp != null) a.computeFileLocation(temp);
			}				
		}
		toKeep.getAttachments().forEach(e->{
			Path temp = e.getCopyFromLocation() == null ? e.getAttachmentFile() : null;
			e.setObservation(toKeep);
			if (temp != null) e.computeFileLocation(temp);
			
		});
		
	}
	/*
	 * updates an observation 
	 */
	private boolean updateObservation(){
		WaypointObservation wo = createObservationAndClear();
		if (wo != null){
			mergeObservation(editingOb, wo);
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
	@SuppressWarnings("unchecked")
	private boolean addObservation(){
		WaypointObservation wo = createObservationAndClear();		
		if (wo != null){
			getWizardInternal().addObservation(wo);
			((List<WaypointObservation>)attributeTable.getInput()).add(wo);
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
	private void createObservationTable(Composite parent, Collection<WaypointObservation> catobservations){
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2,false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)comp.getLayout()).marginWidth = 0;
		((GridLayout)comp.getLayout()).marginHeight = 0;
		
		Label lbl = new Label(comp, SWT.WRAP);
		lbl.setText(SmartUtils.formatStringForLabel(
				MessageFormat.format(Messages.AttributeWizardPage_CategoryObservations_Label, new Object[]{thisCategory.getName()})));
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridData)lbl.getLayoutData()).widthHint = 200;
		lbl.setFont(getBoldFont(lbl));
		
		attributeTable = AttributeTable.createAttributeTable(comp, catAttributes);
		attributeTable.setContentProvider(ArrayContentProvider.getInstance());
		attributeTable.setInput(catobservations);
		attributeTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)attributeTable.getTable().getLayoutData()).heightHint = 90;
		((GridData)attributeTable.getTable().getLayoutData()).widthHint = 300;

		Menu mnu = new Menu(attributeTable.getControl());
		
		MenuItem editItem = new MenuItem(mnu, SWT.PUSH);
		editItem.setText(DialogConstants.EDIT_BUTTON_TEXT);
		editItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		editItem.addListener(SWT.Selection, e->editObservationBtn());
		
		MenuItem deleteItem = new MenuItem(mnu, SWT.PUSH);
		deleteItem.setText(DialogConstants.DELETE_BUTTON_TEXT);
		deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteItem.addListener(SWT.Selection, e->deleteObservationBtn());
		
		attributeTable.getControl().setMenu(mnu);
		
		ToolBar tb = new ToolBar(comp, SWT.FLAT | SWT.VERTICAL);
		tb.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		ToolItem tiEdit = new ToolItem(tb, SWT.PUSH);
		tiEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		tiEdit.setToolTipText(Messages.AttributeWizardPage_editObsTooltip);
		tiEdit.addListener(SWT.Selection,e->editObservationBtn());
		
		ToolItem tiDelete = new ToolItem(tb, SWT.PUSH);
		tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiDelete.setToolTipText(Messages.AttributeWizardPage_deleteObsTooltip);
		tiDelete.addListener(SWT.Selection, e->deleteObservationBtn());

		tiDelete.setEnabled(false);
		tiEdit.setEnabled(false);
		editItem.setEnabled(false);
		deleteItem.setEnabled(false);
		attributeTable.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				tiDelete.setEnabled(!attributeTable.getSelection().isEmpty());
				tiEdit.setEnabled(!attributeTable.getSelection().isEmpty());
				editItem.setEnabled(!attributeTable.getSelection().isEmpty());
				deleteItem.setEnabled(!attributeTable.getSelection().isEmpty());
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	private void deleteObservationBtn() {
		IStructuredSelection sel = attributeTable.getStructuredSelection();
		if (!sel.isEmpty()){
			for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
				WaypointObservation type = (WaypointObservation) iterator.next();
				getWizardInternal().removeObservation(type);
				((List<WaypointObservation>)attributeTable.getInput()).remove(type);
			}
			attributeTable.refresh();
			clearEditObservation();
		}	
		validate();
	}
	
	private void editObservationBtn() {
		if (observationModified()){
			MessageDialog md = getSaveObservationDialog();
			int ret = md.open();
			if (ret == 0){
				if (!addOrUpdateObservation()){
					return;
				}
			}else if (ret == 2){
				//cancel
				return;
			}
		}
		WaypointObservation wo = (WaypointObservation) attributeTable.getStructuredSelection().getFirstElement();
		if (wo != null){
			editObservation(wo);
		}
	}
	/*
	 * edits a waypoint observation
	 */
	private void editObservation(WaypointObservation wo){
		editingOb = wo;
		if (btnUpdate != null){
			btnUpdate.setEnabled(true);
			setDefaultButton(btnUpdate);
		}
		for (IAttributeField<?> field : attributeFields){
			WaypointObservationAttribute value = (wo.findAttribute(field.getAttribute()));
			if(value != null){
				field.setValue(value.getAttributeValue());
			}else{
				field.clear();
			}
		}

		currentAttachments = new ArrayList<>();
		if (editingOb.getAttachments() != null){
			currentAttachments.addAll(editingOb.getAttachments());
		}
		attachmentViewer.setInput(currentAttachments);
		attsModified = false;
		
		scComp.layout(true, true);
		
		if (attributeTable != null){
			attributeTable.getControl().setData(AttributeTable.EDITING_OBS_KEY, wo);
			attributeTable.reveal(wo);
			attributeTable.refresh();
		}
		
	}
	
	/*
	 * clears the current editing observations
	 */
	private void clearEditObservation(){
		this.editingOb = null;
		this.btnUpdate.setEnabled(false);
		this.currentAttachments = new ArrayList<>();;
		attachmentViewer.setInput(currentAttachments);
		attsModified = false;
		setDefaultButton(btnAdd);
		if (attributeTable != null){
			attributeTable.getControl().setData(AttributeTable.EDITING_OBS_KEY, null);
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
			field.addResizeListener(e->scComp.setMinSize(scComp.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT)));
		}
		if (attributeFields.size() > 0){
			attributeFields.get(0).setFocus();
		}
		
		
		//attachments
		Label lbl = new Label(cattribute, SWT.NONE);
		lbl.setText(Messages.AttributeWizardPage_AttachmentsLabel);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		((GridData)lbl.getLayoutData()).verticalIndent = 2;
		
		
		attachmentViewer = new AttachmentPreviewTagComposite(cattribute, 
				getWizardInternal().getTags(), e->addAttachment(), e->deleteAttachment());
				
		this.currentAttachments = new ArrayList<>();
		attachmentViewer.setInput(currentAttachments);
	}
	
	private void deleteAttachment() {
		attachmentViewer.deleteAttachments(currentAttachments);
		attsModified = true;
	}
	
	private void addAttachment() {		
		attachmentViewer.addAttachment(()->new ObservationAttachment(), currentAttachments);
		attsModified = true;
	}
	/*
	 * creates a waypoint observation from the given
	 * attribute fields then clears the attribute fields
	 */
	private WaypointObservation createObservationAndClear(){
		WaypointObservation wo = new WaypointObservation();
		wo.setCategory(thisCategory);
		wo.setAttributes(new ArrayList<WaypointObservationAttribute>());
		
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
				att.setAttributeValue(field.getValue());
				if (att.getAttribute().getType() == Attribute.AttributeType.MLIST &&
						att.getAttributeListItems() == null) {
					att.setAttributeListItems(new ArrayList<>());
				}
				wo.getAttributes().add(att);
			}
		}

		// update the attachments
		wo.setAttachments(new ArrayList<ObservationAttachment>());
		
		for (ISmartAttachment a : currentAttachments){
			try{
				Path temp = a.getCopyFromLocation() == null ? a.getAttachmentFile() : null;
				((ObservationAttachment)a).setObservation(wo);
				if (temp != null) a.computeFileLocation(temp);
				wo.getAttachments().add((ObservationAttachment)a);
			}catch (Exception ex){
				ObservationPlugIn.displayLog(ex.getMessage(), ex);
			}
		}
		
		//clear all fields
		currentAttachments = new ArrayList<>();
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
			if (getWizardInternal().hasMoreCategories()) {
				Category cat = getWizardInternal().getNextCategory();
				nextPage = new AttributeWizardPage(getWizardInternal(), cat);
			}else {
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
			return true;
		}
		
		if (!thisCategory.getIsMultiple()){
			//create single observation
			WaypointObservation wo = createObservationAndClear();
			if (wo == null){
				return false;
			}else{
				WaypointObservation existing = null;
				for (WaypointObservation ewo : getWizardInternal().getWaypoint().getAllObservations()) {
					if (ewo.getCategory().equals(wo.getCategory())) {
						existing = ewo;
						break;
					}
				}
				if (existing == null) {
					getWizardInternal().addObservation(wo);
				}else {
					//merge existing with wo
					mergeObservation(existing, wo);
				}
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
			boolean ok = false;
			for (WaypointObservation wo  : getWizardInternal().getWaypoint().getAllObservations()) {
				if (wo.getCategory().equals(thisCategory)) {
					ok = true;
					break;
				}
			}
			if (!ok){
				if (!MessageDialog.openQuestion(getShell(), Messages.AttributeWizardPage_DataObservations_DialogTitle, Messages.AttributeWizardPage_DataObservations_DialogMessage)){
					//cancel and let the user enter data
					return false;
				}
			}
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