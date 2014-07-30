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
package org.wcs.smart.er.ui.missionattribute;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.ca.properties.AttributeItemDialog;
import org.wcs.smart.ui.ca.properties.NameKeyComposite;
import org.wcs.smart.ui.ca.properties.NameKeyComposite.IChangeListener;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for editing specific mission attribute.
 * 
 * @author Emily
 *
 */
public class EditMissionAttributeDialog extends TitleAreaDialog implements SelectionListener{
	
	
	private Collection<? extends NamedKeyItem> siblings;	//attributes that are siblings to the current attribute being updated/created
	private MissionAttribute toUpdate;	//attribute to update
	
	private NameKeyComposite nameKeyControls;
	
	private ComboViewer cmbType;
	private TableViewer lstViewer;
	
	private Button btnAdd;
	private Button btnEdit;
	private Button btnDelete;
	
	private Composite listPanel;
	
	private HashMap<Language, String> copyNames;
	private List<MissionAttributeListItem> copyItems;
	
	private Session session;

	/**
	 * creates a new dialog
	 * 
	 * @param parentShell
	 * @param toUpdate Attribute to update
	 * @param siblings Sibling attributes to the attribute being updated
	 * @param defaultLang the current language being modified
	 * @param currentSession active hibernate session
	 */
	public EditMissionAttributeDialog(Shell parentShell,  
			MissionAttribute toUpdate,  
			Collection<? extends NamedKeyItem> siblings,
			Session session) {
		
		super(parentShell);
		this.session = session;
		this.toUpdate = toUpdate;
		
		this.siblings = new ArrayList<NamedKeyItem>(siblings);
		this.siblings.remove(toUpdate);

		copyNames = new HashMap<Language, String>();
		for (org.wcs.smart.ca.Label l : toUpdate.getNames()){
			copyNames.put(l.getLanguage(), l.getValue());
		}
		
		copyItems = new ArrayList<MissionAttributeListItem>();
		if (toUpdate.getAttributeList() != null){
			for (MissionAttributeListItem i : toUpdate.getAttributeList()){
				MissionAttributeListItem item = new MissionAttributeListItem();
				item.setKeyId( i.getKeyId() );
				item.setAttribute(toUpdate);
				item.setListOrder(i.getListOrder());
				item.setName(i.getName());
				for (org.wcs.smart.ca.Label l : i.getNames()){
					item.updateName(l.getLanguage(), l.getValue());
				}
				copyItems.add(item);
			}
		}
		
		
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	/**
	 * Create contents of the dialog.
	 */
	@Override
	public Control createDialogArea(Composite parent){
		Composite myparent = (Composite) super.createDialogArea(parent);
			
		Composite composite = new Composite(myparent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(3, false));

		/* Type */
		Label lblNewLabel_2 = new Label(composite, SWT.NONE);
		lblNewLabel_2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel_2.setText("Type:");
		
		cmbType = new ComboViewer(composite, SWT.SIMPLE | SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbType.setContentProvider(ArrayContentProvider.getInstance());
		cmbType.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				return ((Attribute.AttributeType)element).name();
			}
		});
		cmbType.setInput(new Object[]{
				Attribute.AttributeType.NUMERIC,
				Attribute.AttributeType.TEXT,
				Attribute.AttributeType.LIST
		});
		
		Combo combo = cmbType.getCombo();
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		if (toUpdate.getUuid() != null){
			combo.setEnabled(false);
		}
		
		combo.addSelectionListener(new SelectionAdapter() {	
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (getType() == Attribute.AttributeType.LIST){
					listPanel.setVisible(true);
				}else{
					listPanel.setVisible(false);
				}
				validate();
			}
		});
		
		nameKeyControls = new NameKeyComposite();
		nameKeyControls.createControls(composite, true, toUpdate.getUuid() == null, new IChangeListener() {
			
			@Override
			public void itemModified() {
				((AttributeLabelProvider)lstViewer.getLabelProvider()).setLanguage(nameKeyControls.getSelectedLanguage());
				lstViewer.refresh();
				
				validate();
			}
		});
		
		listPanel = new Composite(composite, SWT.NONE);
		listPanel.setLayout(new GridLayout(2, false));
		listPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		
		lstViewer = new TableViewer(listPanel,SWT.BORDER);
		lstViewer.setContentProvider(ArrayContentProvider.getInstance());
		lstViewer.setLabelProvider(new AttributeLabelProvider());
		lstViewer.setInput(copyItems);
		lstViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editListItem();
			}
		});
		
		Composite buttonPnl = new Composite(listPanel, SWT.NONE);
		buttonPnl.setLayout(new GridLayout(1, false));
		buttonPnl.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		btnAdd = new Button(buttonPnl, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnAdd.addSelectionListener(this);
		
		btnEdit = new Button(buttonPnl, SWT.PUSH);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnEdit.addSelectionListener(this);
		
		btnDelete = new Button(buttonPnl, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnDelete.addSelectionListener(this);
		
		
		if (toUpdate.getKeyId() == null){
			getShell().setText("New Mission Attribute");
			setTitle("New Mission Attribute");
			setMessage("Create a new attribute for use with survey missions");
		}else{
			getShell().setText("Edit Mission Attribute");
			setTitle(toUpdate.getName());
			setMessage("Modify the mission attribute.");
		}

		//init fields
		nameKeyControls.initFields(toUpdate, siblings, SmartDB.getCurrentConservationArea().getDefaultLanguage());
		cmbType.setSelection(new StructuredSelection(toUpdate.getType()));
		listPanel.setVisible(toUpdate.getType() == AttributeType.LIST);
		
		return composite;
	}
	
	
	private void validate(){
		boolean error = nameKeyControls.validate();

		if (getButton(OK) != null){
			getButton(OK).setEnabled(!error);
		}
	}
	
	
	private Attribute.AttributeType getType(){
		return (AttributeType) ((IStructuredSelection)cmbType.getSelection()).getFirstElement();
	}
	
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.FINISH_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		
		validate();
	}

	/*
	 * Creates a control decoration for a wizard page field.
	 */
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	/*
	 * updates the attribute to update with the
	 * information in the attribute panel
	 */
	private void updateAttribute(){
		nameKeyControls.updateFields(toUpdate);
		
		toUpdate.setType(getType());
		
		//remove existing items
		if (toUpdate.getAttributeList() != null){
			for (MissionAttributeListItem mi : toUpdate.getAttributeList()){
				mi.setAttribute(null);
			}
			toUpdate.getAttributeList().clear();
		}
		
		if (toUpdate.getType() == AttributeType.LIST){
			if (toUpdate.getAttributeList() == null){
				toUpdate.setAttributeList(new ArrayList<MissionAttributeListItem>());
			}
			//TODO:: add new items
			for (MissionAttributeListItem li : copyItems){
				toUpdate.getAttributeList().add(li);
				li.setAttribute(toUpdate);
			}
		}
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			setReturnCode(OK);
			updateAttribute();
		} else if (IDialogConstants.CANCEL_ID == buttonId) {
			setReturnCode(CANCEL);
		}
		
		close();
	}
	
	private void addListItem(){
		MissionAttributeListItem item = new MissionAttributeListItem();
		AttributeItemDialog dialog = new AttributeItemDialog(getShell(), item, copyItems,nameKeyControls.getSelectedLanguage());
		if (dialog.open() == OK){
			copyItems.add(item);
			lstViewer.refresh();
		}
		
	}
	private void deleteListItem(){
		MissionAttributeListItem mi = (MissionAttributeListItem)((IStructuredSelection)lstViewer.getSelection()).getFirstElement();
		if (mi == null){
			return;
		}
		if (!MessageDialog.openQuestion(getShell(), "Delete", MessageFormat.format("Are you sure you want to delete the list item {0}?", new Object[]{mi.getName()}))){
			return;
		}
		
		try{
			if (mi.getUuid() == null || DeleteManager.canDelete(mi, session)){
				copyItems.remove(mi);
				lstViewer.refresh();		
			}
		}catch (Exception ex){
			MessageDialog.openError(getShell(), "Delete", MessageFormat.format("The list item {0} cannot be removed.", new Object[]{mi.getName()}) + "\n\n" + ex.getMessage());
		}
		
	}
	
	private void editListItem(){
		MissionAttributeListItem mi = (MissionAttributeListItem)((IStructuredSelection)lstViewer.getSelection()).getFirstElement();
		if (mi == null){
			return;
		}
		List<MissionAttributeListItem> siblings = new ArrayList<MissionAttributeListItem>();
		siblings.addAll(copyItems);
		siblings.remove(mi);
		AttributeItemDialog dialog = new AttributeItemDialog(getShell(), mi, siblings,nameKeyControls.getSelectedLanguage());
		if (dialog.open() == OK){
			lstViewer.refresh();
		}
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.getSource() == btnEdit){
			editListItem();
		}else if (e.getSource() == btnAdd){
			addListItem();
		}else if (e.getSource() == btnDelete){
			deleteListItem();
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		
	}
}
