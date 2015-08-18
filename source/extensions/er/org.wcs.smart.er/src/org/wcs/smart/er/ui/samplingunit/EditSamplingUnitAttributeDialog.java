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
package org.wcs.smart.er.ui.samplingunit;

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
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
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
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.ca.properties.AttributeItemDialog;
import org.wcs.smart.ui.ca.properties.NameKeyComposite;
import org.wcs.smart.ui.ca.properties.NameKeyComposite.IChangeListener;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for editing specific sampling unit attribute.
 * 
 * <p>Does not update the database.  If using this dialog you
 * must update the database.</p>
 * 
 * @author Emily
 *
 */
public class EditSamplingUnitAttributeDialog extends TitleAreaDialog implements SelectionListener {
	
	
	private Collection<? extends NamedKeyItem> siblings;	//attributes that are siblings to the current attribute being updated/created
	private SamplingUnitAttribute toUpdate;	//attribute to update
	
	private NameKeyComposite nameKeyControls;
	
	private ComboViewer cmbType;
	
	private HashMap<Language, String> copyNames;
	
	private TableViewer lstViewer;
	private Button btnAdd;
	private Button btnEdit;
	private Button btnDelete;
	private Button btnUp;
	private Button btnDown;
	private Composite listPanel;
	private List<SamplingUnitAttributeListItem> copyItems;

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
	public EditSamplingUnitAttributeDialog(Shell parentShell,  
			SamplingUnitAttribute toUpdate,  
			Collection<? extends NamedKeyItem> siblings,
			Session session) {
		
		super(parentShell);
		this.toUpdate = toUpdate;
		this.session = session;
		
		this.siblings = new ArrayList<NamedKeyItem>(siblings);
		this.siblings.remove(toUpdate);

		copyNames = new HashMap<Language, String>();
		for (org.wcs.smart.ca.Label l : toUpdate.getNames()){
			copyNames.put(l.getLanguage(), l.getValue());
		}
		
		copyItems = new ArrayList<SamplingUnitAttributeListItem>();
		if (toUpdate.getAttributeList() != null){
			for (SamplingUnitAttributeListItem i : toUpdate.getAttributeList()){
				SamplingUnitAttributeListItem item = new SamplingUnitAttributeListItem();
				item.setKeyId( i.getKeyId() );
				item.setAttribute(toUpdate);
				item.setListOrder(i.getListOrder());
				item.setName(i.getName());
				for (org.wcs.smart.ca.Label l : i.getNames()){
					item.updateName(l.getLanguage(), l.getValue());
				}
				item.setUuid(i.getUuid());
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
		lblNewLabel_2.setText(Messages.EditSamplingUnitAttributeDialog_TypeLabel);
		
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
				((SamplingUnitLabelProvider)lstViewer.getLabelProvider()).setLanguage(nameKeyControls.getSelectedLanguage());
				lstViewer.refresh();
				
				validate();	
			}
		});
		
		createListPanel(composite);
		
		if (toUpdate.getKeyId() == null){
			getShell().setText(Messages.EditSamplingUnitAttributeDialog_NewTitle);
			setTitle(Messages.EditSamplingUnitAttributeDialog_NewTitle);
			setMessage(Messages.EditSamplingUnitAttributeDialog_NewMessage);
		}else{
			getShell().setText(Messages.EditSamplingUnitAttributeDialog_EditTitle);
			setTitle(toUpdate.getName());
			setMessage(Messages.EditSamplingUnitAttributeDialog_EditMessage);
		}

		//init fields
		nameKeyControls.initFields(toUpdate, siblings, SmartDB.getCurrentConservationArea().getDefaultLanguage());
		cmbType.setSelection(new StructuredSelection(toUpdate.getType()));
		listPanel.setVisible(toUpdate.getType() == AttributeType.LIST);

		return composite;
	}
	
	private void createListPanel(Composite parent){
		listPanel = new Composite(parent, SWT.NONE);
		listPanel.setLayout(new GridLayout(2, false));
		listPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		
		lstViewer = new TableViewer(listPanel,SWT.BORDER);
		lstViewer.setContentProvider(ArrayContentProvider.getInstance());
		lstViewer.setLabelProvider(new SamplingUnitLabelProvider());
		lstViewer.setInput(copyItems);
		lstViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)lstViewer.getControl().getLayoutData()).heightHint = 200;
		((GridData)lstViewer.getControl().getLayoutData()).widthHint = 300;
		lstViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				configureButtons();
			}
		});
		lstViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editListItem();
			}
		});
		
		/* drag and drop support */
		int operations = DND.DROP_MOVE;
		Transfer[] transferTypes = new Transfer[]{LocalSelectionTransfer.getTransfer()};
		lstViewer.addDragSupport(operations, transferTypes, new DragSourceListener() {
			@Override
			public void dragStart(DragSourceEvent event) {
				LocalSelectionTransfer.getTransfer().setSelection(lstViewer.getSelection());
				event.doit = true;
			}
			
			@Override
			public void dragSetData(DragSourceEvent event) {
				if (LocalSelectionTransfer.getTransfer()
						.isSupportedType(event.dataType)) {
					event.data = lstViewer.getSelection();
				}
			}
			
			@Override
			public void dragFinished(DragSourceEvent event) {
				LocalSelectionTransfer.getTransfer().setSelection(null);
				lstViewer.refresh();
			}
		});
		
		ViewerDropAdapter dropAdapter = new ViewerDropAdapter(lstViewer) {
			
			@Override
			public boolean validateDrop(Object target, int operation,
					TransferData transferType) {
				if (target instanceof SamplingUnitAttributeListItem){
					return true;
				}
				return false;
			}
			
			@Override
			public boolean performDrop(Object data) {
				StructuredSelection selection = (StructuredSelection)LocalSelectionTransfer.getTransfer().getSelection();
				if (selection == null){
					return false;
				}
				Object obj = selection.getFirstElement();
				
				SamplingUnitAttributeListItem target = (SamplingUnitAttributeListItem)getCurrentTarget();
				if (target.equals(obj)){
					return false;
				}
				int index = copyItems.indexOf(obj);
				int toIndex = copyItems.indexOf(target);
				copyItems.remove(index);
				copyItems.add(toIndex, (SamplingUnitAttributeListItem)obj);		
				reorder();
				return true;
			}
		};
		lstViewer.addDropSupport(operations, transferTypes,dropAdapter);
		
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
		
		Label ll = new Label(buttonPnl, SWT.SEPARATOR | SWT.HORIZONTAL);
		ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnUp = new Button(buttonPnl, SWT.PUSH);
		btnUp.setText(Messages.EditMissionAttributeDialog_MoveUpLabel);
		btnUp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnUp.addSelectionListener(this);
		
		btnDown = new Button(buttonPnl, SWT.PUSH);
		btnDown.setText(Messages.EditMissionAttributeDialog_MoveDownLabel);
		btnDown.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnDown.addSelectionListener(this);
		
		configureButtons();
		
	}
	
	private void reorder(){
		int i = 0;
		for (SamplingUnitAttributeListItem order : copyItems){
			order.setListOrder(i++);
		}
	}
	
	private void editListItem(){
		SamplingUnitAttributeListItem mi = (SamplingUnitAttributeListItem)((IStructuredSelection)lstViewer.getSelection()).getFirstElement();
		if (mi == null){
			return;
		}
		List<SamplingUnitAttributeListItem> siblings = new ArrayList<SamplingUnitAttributeListItem>();
		siblings.addAll(copyItems);
		siblings.remove(mi);
		AttributeItemDialog dialog = new AttributeItemDialog(getShell(), mi, siblings,nameKeyControls.getSelectedLanguage());
		if (dialog.open() == OK){
			lstViewer.refresh();
		}
	}
	
	private void configureButtons(){
		boolean isEnabled = lstViewer.getSelection().isEmpty();
		btnDelete.setEnabled(!isEnabled);
		btnEdit.setEnabled(!isEnabled);
		btnDown.setEnabled(!isEnabled);
		btnUp.setEnabled(!isEnabled);
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
		
		String name = toUpdate.findNameNull(SmartDB.getCurrentLanguage());
		if (name == null){
			name = toUpdate.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
		}
		toUpdate.setName(name);
		
		if (toUpdate.getType() == AttributeType.LIST){
			if (toUpdate.getAttributeList() == null){
				toUpdate.setAttributeList(new ArrayList<SamplingUnitAttributeListItem>());
			}
			
			List<SamplingUnitAttributeListItem> toDelete = new ArrayList<SamplingUnitAttributeListItem>();
		
			for (SamplingUnitAttributeListItem mi : toUpdate.getAttributeList()){
				SamplingUnitAttributeListItem  found = null;
				for (SamplingUnitAttributeListItem copy : copyItems){
					if (mi.getUuid().equals(copy.getUuid())){
						found = copy;
						break;
					}
				}
				if (found == null){
					toDelete.add(mi);
				}else{
					//copy info from copy to found
					mi.setAttribute(found.getAttribute());
					mi.setKeyId(found.getKeyId());
					mi.setListOrder(found.getListOrder());
					mi.setName(found.getName());
					
					for (org.wcs.smart.ca.Label l : found.getNames()){
						mi.updateName(l.getLanguage(), l.getValue());
					}
				}
				
			}
			for (SamplingUnitAttributeListItem d : toDelete){
				toUpdate.getAttributeList().remove(d);
				d.setAttribute(null);
			}
			for (SamplingUnitAttributeListItem copy : copyItems){
				if (copy.getUuid() == null){
					toUpdate.getAttributeList().add(copy);
					copy.setAttribute(toUpdate);
				}
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
		SamplingUnitAttributeListItem item = new SamplingUnitAttributeListItem();
		AttributeItemDialog dialog = new AttributeItemDialog(getShell(), item, copyItems,nameKeyControls.getSelectedLanguage());
		if (dialog.open() == OK){
			item.setListOrder(copyItems.size());
			copyItems.add(item);
			lstViewer.refresh();
		}
		
	}
	
	private void deleteListItem(){
		SamplingUnitAttributeListItem mi = (SamplingUnitAttributeListItem)((IStructuredSelection)lstViewer.getSelection()).getFirstElement();
		if (mi == null){
			return;
		}
		if (!MessageDialog.openQuestion(getShell(), Messages.EditMissionAttributeDialog_DeleteButton, MessageFormat.format(Messages.EditMissionAttributeDialog_DeleteMessage, new Object[]{mi.getName()}))){
			return;
		}
		
		try{
			if (mi.getUuid() == null || DeleteManager.canDelete(mi, session)){
				copyItems.remove(mi);
				reorder();
				lstViewer.refresh();		
			}
		}catch (Exception ex){
			MessageDialog.openError(getShell(), Messages.EditMissionAttributeDialog_DeleteDialotTitle, MessageFormat.format(Messages.EditMissionAttributeDialog_DeleteError, new Object[]{mi.getName()}) + "\n\n" + ex.getMessage()); //$NON-NLS-1$
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
		}else if (e.getSource() == btnUp){
			SamplingUnitAttributeListItem mi = (SamplingUnitAttributeListItem)((IStructuredSelection)lstViewer.getSelection()).getFirstElement();
			if (mi == null){
				return;
			}
			int index = copyItems.indexOf(mi);
			index --;
			if (index < 0) index = 0;
			copyItems.remove(mi);
			copyItems.add(index, mi);
			reorder();
			lstViewer.refresh();
			
		}else if (e.getSource() == btnDown){
			SamplingUnitAttributeListItem mi = (SamplingUnitAttributeListItem)((IStructuredSelection)lstViewer.getSelection()).getFirstElement();
			if (mi == null){
				return;
			}
			int index = copyItems.indexOf(mi);
			index ++;
			copyItems.remove(mi);
			if (index > copyItems.size()){
				copyItems.add(mi);
			}else{
				copyItems.add(index, mi);
			}
			reorder();
			lstViewer.refresh();
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		
	}
}
