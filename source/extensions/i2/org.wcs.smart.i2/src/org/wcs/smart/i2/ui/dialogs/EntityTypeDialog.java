/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.EntityTypeManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.ui.AttributeLabelProvider;
import org.wcs.smart.i2.ui.IconComposite;
import org.wcs.smart.i2.ui.NamedItemViewerFilter;
import org.wcs.smart.ui.ca.properties.NameKeyComposite;
import org.wcs.smart.ui.ca.properties.NameKeyComposite.IChangeListener;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

/**
 * Dialog for editing entity types.
 * 
 * @author Emily
 *
 */
public class EntityTypeDialog extends TitleAreaDialog {

	private IntelEntityType type;
	private NameKeyComposite nameKeyInfo;
	private IconComposite icon;
	private List<IntelEntityType> entityTypeSiblings;
	private TableViewer tblAttributes;
	private ComboViewer idAttribute;
	
	private MenuItem editItem;
	private MenuItem deleteItem;
	private MenuItem addItem;
	
	private Button btnAdd;
	private Button btnDelete;
	private Button btnEdit;
	
	private ControlDecoration cdList;
	private ControlDecoration cdId;
	
	private List<IntelEntityTypeAttribute> attributeList = new ArrayList<IntelEntityTypeAttribute>();
	
	@Inject
	private IEventBroker broker;
	
	public EntityTypeDialog(Shell parentShell, IntelEntityType type) {
		super(parentShell);
		this.type = type;
	}

	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		return new Point(p.x,(int)(p.y*1.4));
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
	
	protected void okPressed() {
		boolean isNew = type.getUuid() == null;
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			s.saveOrUpdate(type);
			
			for (IntelEntityTypeAttribute a : attributeList){
				if (!type.getAttributes().contains(a)){
					s.saveOrUpdate(a);
					type.getAttributes().add(a);
				}
			}
			
			for (IntelEntityTypeAttribute a : type.getAttributes()){
				if (!attributeList.contains(a)){
					//delete any entity attribute value associations
					Query qDelete = s.createQuery("DELETE FROM IntelEntityAttributeValue WHERE id.attribute = :att"); //$NON-NLS-1$
					qDelete.setParameter("att", a.getAttribute()); //$NON-NLS-1$
					qDelete.executeUpdate();
							
					s.delete(a);
				}
			}
			
			
			s.getTransaction().commit();
			
		}catch (Exception ex){
			if (s.getTransaction().isActive())s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog("Unable to save changes: " +ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		if (isNew){
			IntelEvents.fireNewEntityType(type, broker);
		}else{
			IntelEvents.fireModifiedEntityType(type, broker);
		}
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT,true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		
		initFields();
	}
	
	private void attributeSelectionModified(){
		btnEdit.setEnabled(!tblAttributes.getSelection().isEmpty());
		btnDelete.setEnabled(!tblAttributes.getSelection().isEmpty());
		editItem.setEnabled(!tblAttributes.getSelection().isEmpty());
		deleteItem.setEnabled(!tblAttributes.getSelection().isEmpty());
	}
	
	private void modified(){
		
		
		boolean isError = false;
		if (nameKeyInfo.validate()){
			isError = true;
		}
		
		cdList.hide();
		if (attributeList.isEmpty()){
			isError = true;
			cdList.setDescriptionText("At least one attribute must exist.");
			cdList.show();
		}
		cdId.hide();
		if (type.getIdAttribute() == null){
			isError = true;
			cdId.setDescriptionText("One attribute must be selected as the identifier for the attribute.");
			cdId.show();
		}
		
		getButton(IDialogConstants.OK_ID).setEnabled(!isError);
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(3, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		nameKeyInfo = new NameKeyComposite();
		nameKeyInfo.createControls(parent, true, type.getUuid() == null, new IChangeListener() {
			@Override
			public void itemModified() {
				if (!nameKeyInfo.validate()){
					nameKeyInfo.updateFields(type);
				}
				modified();
			}
		});
		
		Label l = new Label(parent, SWT.NONE);
		l.setText("Icon:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		icon = new IconComposite(parent);
		icon.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		icon.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				modified();
				type.setIcon(icon.getImage());
			}
		});
	
		l = new Label(parent, SWT.NONE);
		l.setText("Id Attribute:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		
		idAttribute = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		idAttribute.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		idAttribute.setContentProvider(ArrayContentProvider.getInstance());
		idAttribute.setLabelProvider(AttributeLabelProvider.INSTANCE);
		idAttribute.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)idAttribute.getSelection()).getFirstElement();
				if (x instanceof IntelEntityTypeAttribute){
					type.setIdAttribute(((IntelEntityTypeAttribute) x).getAttribute());
				}else if (x instanceof IntelAttribute){
					type.setIdAttribute(((IntelAttribute) x));
				}
				modified();
			}
		});
		cdId= createDecoration(idAttribute.getControl());
		
		l = new Label(parent, SWT.NONE);
		l.setText("Attributes:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		
		
		Composite attributeComp = new Composite(parent, SWT.NONE);
		attributeComp.setLayout(new GridLayout(2, false));
		attributeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
				
		tblAttributes = new TableViewer(attributeComp, SWT.BORDER | SWT.MULTI);
		tblAttributes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblAttributes.setContentProvider(ArrayContentProvider.getInstance());
		tblAttributes.setLabelProvider(AttributeLabelProvider.INSTANCE);
		tblAttributes.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				attributeSelectionModified();
			}
		});
		tblAttributes.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editAttribute();
				
			}
		});
		cdList = createDecoration(tblAttributes.getControl());
		
		Menu listMenu = new Menu(tblAttributes.getControl());
		tblAttributes.getControl().setMenu(listMenu);
		
		addItem = new MenuItem(listMenu, SWT.DEFAULT);
		addItem.setText(DialogConstants.ADD_BUTTON_TEXT);
		addItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		addItem.setEnabled(true);
		addItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addAttribute();
			}
		});
		
		editItem = new MenuItem(listMenu, SWT.DEFAULT);
		editItem.setText(DialogConstants.EDIT_BUTTON_TEXT);
		editItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.RENAME_ICON));
		editItem.setEnabled(false);
		editItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editAttribute();	
			}
		});		
		
		deleteItem = new MenuItem(listMenu, SWT.DEFAULT);
		deleteItem.setText(DialogConstants.DELETE_BUTTON_TEXT);
		deleteItem.setEnabled(false);
		deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeAttributes();	
			}
		});
		
		
		Composite buttonComp = new Composite(attributeComp, SWT.NONE);
		buttonComp.setLayout(new GridLayout());
		buttonComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		btnAdd = new Button(buttonComp, SWT.NONE);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addAttribute();
			}
		});
		
		btnEdit = new Button(buttonComp, SWT.NONE);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnEdit.setEnabled(false);
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editAttribute();
			}
		});
		
		btnDelete = new Button(buttonComp, SWT.NONE);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnDelete.setEnabled(false);
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeAttributes();
			}
		});
		setTitle("Entity Type");
		getShell().setText("Entity Type");
		setMessage("Configure entity type");
		
		return parent;
	}
	
	private void addAttribute(){
		AttributeListDialog dialog = new AttributeListDialog(getShell());
		if (dialog.open() == Window.OK){
			tblAttributes.refresh();
			if (type.getIdAttribute() == null){
				type.setIdAttribute(attributeList.get(0).getAttribute());
			}
			refreshAttributeList();
			modified();
		}
	}
	
	private void editAttribute(){
		Object x = ((IStructuredSelection)tblAttributes.getSelection()).getFirstElement();
		if (x instanceof IntelEntityTypeAttribute){
			IntelEntityTypeAttribute attribute = (IntelEntityTypeAttribute)x;
			
			AttributeDialog ad = new AttributeDialog(getShell(), attribute.getAttribute());
			ad.open();
			
			//refresh
			tblAttributes.refresh();
			refreshAttributeList();
		}
	}

	
	private void removeAttributes(){
		IStructuredSelection items = (IStructuredSelection)tblAttributes.getSelection();
		final List<IntelEntityTypeAttribute> toDelete = new ArrayList<IntelEntityTypeAttribute>();
		for (Iterator<?> iterator = items.iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof IntelEntityTypeAttribute){
				toDelete.add((IntelEntityTypeAttribute) x);
			}
		}
		
		final List<String> warnings = new ArrayList<String>();
		final List<IntelEntityTypeAttribute> aToDelete = new ArrayList<IntelEntityTypeAttribute>();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress(){
	
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					Session session = HibernateManager.openSession();
					try{
						for (IntelEntityTypeAttribute x : toDelete){
							try{
								DeleteManager.canDelete(x, session);
								aToDelete.add(x);
							}catch (Exception ex){
								warnings.add(MessageFormat.format("The attribute {0} cannot be removed. {1}", ((IntelEntityTypeAttribute) x).getAttribute().getName(), ex.getMessage()));
							}
						}	
					}finally{
						session.close();
					}
				}
				
			});
		}catch (Exception ex){
			Intelligence2PlugIn.log(ex.getMessage(), ex);
			warnings.add(ex.getMessage());
		}
		if(!warnings.isEmpty()){
			WarningDialog wd = new WarningDialog(getShell(), "Warnings","Cannot remove selected attributes.", warnings);
			wd.open();
		}
		
		if (aToDelete.size() > 0){
			StringBuilder sb = new StringBuilder();
			for (IntelEntityTypeAttribute d: aToDelete){
				sb.append(d.getAttribute().getName());
				sb.append(", ");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			if (MessageDialog.openConfirm(getShell(), "Remove Attributes", MessageFormat.format("Are you sure you want to delete the attributes {0}? \n All attribute information associated with entities will also be removed.", sb.toString()))){
				attributeList.removeAll(aToDelete);
			}
		}
		
		tblAttributes.refresh();
		refreshAttributeList();
	}
	
	private void refreshAttributeList(){
		List<IntelAttribute> idAttributes = new ArrayList<IntelAttribute>();
		for (IntelEntityTypeAttribute a : attributeList){
			idAttributes.add(a.getAttribute());
		}
		idAttribute.setInput(idAttributes);
		if (type.getIdAttribute() != null){
			boolean contains = false;
			for (IntelEntityTypeAttribute a : attributeList){
				if (a.getAttribute().equals(type.getIdAttribute())){
					contains = true;
					break;
				}
			}
			if (!contains){
				type.setIdAttribute(null);
				idAttribute.setSelection(null);
				modified();
			}else{
				idAttribute.setSelection(new StructuredSelection(type.getIdAttribute()));
			}
		}else{
			idAttribute.setSelection(null);
		}
	}
	private void initFields(){
		if (type.getIcon() != null){
			icon.setImage(type.getIcon());
		}
		attributeList.addAll(type.getAttributes());
		tblAttributes.setInput(attributeList);
		
		refreshAttributeList();
		siblingsJob.setSystem(true);
		siblingsJob.schedule(0);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	
	private Job siblingsJob = new Job("get siblings"){ //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			try{
				entityTypeSiblings = EntityTypeManager.INSTANCE.getEntityTypes(s, SmartDB.getCurrentConservationArea());
				entityTypeSiblings.remove(type);
			}finally{
				s.close();
			}
			
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					nameKeyInfo.initFields(type, entityTypeSiblings, SmartDB.getCurrentConservationArea().getDefaultLanguage());					
					getButton(IDialogConstants.OK_ID).setEnabled(type.getUuid() == null);
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	
	
	
	private class AttributeListDialog extends TitleAreaDialog{
		private CheckboxTableViewer attributeList;
		private NamedItemViewerFilter filter;
		
		private Job loadAttributes = new LoadAttributesJob(){
			@Override
			public void afterLoad() {
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						attributeList.setInput(attributes);
					}					
				});
			}
			
		};
		public AttributeListDialog(Shell parentShell) {
			super(parentShell);
		}

		@Override
		protected Point getInitialSize() {
			Point p = super.getInitialSize();
			return new Point(p.x,(int)(p.y*1.4));
		}

		
		protected void okPressed() {
			for (Object selection : attributeList.getCheckedElements()){
				if (selection instanceof IntelAttribute){
					IntelEntityTypeAttribute a  = new IntelEntityTypeAttribute();
					a.setAttribute((IntelAttribute) selection);
					a.setEntityType(EntityTypeDialog.this.type);
					if (!EntityTypeDialog.this.attributeList.contains(a)) EntityTypeDialog.this.attributeList.add(a);
				}
			}
			super.okPressed();
		}

		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,true);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		}
		
		
		@Override
		protected Control createDialogArea(Composite parent) {
			parent = (Composite) super.createDialogArea(parent);
			parent = new Composite(parent, SWT.NONE);
			parent.setLayout(new GridLayout());
			parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			FilterComposite typeFilter = new FilterComposite(parent, SWT.NONE);
			typeFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			typeFilter.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					filter.setFilterString(typeFilter.getPatternFilter());
				}
			});
			
			attributeList = CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.MULTI);
			attributeList.setContentProvider(ArrayContentProvider.getInstance());
			attributeList.setLabelProvider(AttributeLabelProvider.INSTANCE);
			attributeList.setInput(new String[]{DialogConstants.LOADING_TEXT});
			attributeList.getControl().setFocus();
			attributeList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			attributeList.getTable().addKeyListener(new KeyAdapter() {
				//spacebar check
				@Override
				public void keyPressed(KeyEvent e) {
					if (attributeList.getSelection().isEmpty()){
						return;
					}
					if (e.keyCode == SWT.SPACE){
						IStructuredSelection selection = ((IStructuredSelection)attributeList.getSelection());
						selection.getFirstElement();
						boolean value = attributeList.getChecked(selection.getFirstElement() );
						for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
							Object tp = (Object) iterator.next();
							attributeList.setChecked(tp, !value);
						}
						e.doit = false;
								
					}
					
				}
			});
			filter = new NamedItemViewerFilter(attributeList);
			attributeList.setFilters(new ViewerFilter[]{filter});
			attributeList.addDoubleClickListener(new IDoubleClickListener() {
				
				@Override
				public void doubleClick(DoubleClickEvent event) {
					attributeList.setChecked( ((IStructuredSelection)attributeList.getSelection()).getFirstElement(), true );
					okPressed();
					
				}
			});
			Button btnNew = new Button(parent, SWT.PUSH);
			btnNew.setText("Create New Attribute");
			btnNew.addSelectionListener(new SelectionAdapter() {
				
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (newAttribute()){
						AttributeListDialog.this.okPressed();
					}
				}
			});
			setTitle("Entity Type Attributes");
			getShell().setText("Entity Type Attributes");
			setMessage(MessageFormat.format("Add attributes for entity type {0}", type.getName()));
			
			loadAttributes.setSystem(true);
			loadAttributes.schedule(0);
			
			return parent;
		}
		
		private boolean newAttribute(){
			IntelAttribute attribute = new IntelAttribute();
			attribute.setConservationArea(SmartDB.getCurrentConservationArea());
			attribute.setAttributeList(new ArrayList<IntelAttributeListItem>());
			if (type.getAttributes() == null) type.setAttributes(new ArrayList<IntelEntityTypeAttribute>());
			AttributeDialog ad = new AttributeDialog(getShell(), attribute);
			ad.open();
			
			if (attribute.getUuid() != null){
				IntelEntityTypeAttribute eta = new IntelEntityTypeAttribute();
				eta.setAttribute(attribute);
				eta.setEntityType(type);
				if (!EntityTypeDialog.this.attributeList.contains(eta)) EntityTypeDialog.this.attributeList.add(eta);
				
				attributeList.refresh();
				return true;
			}else{
				return false;
			}
		}
		
		
		@Override
		public boolean isResizable(){
			return true;
		}	
	}
}