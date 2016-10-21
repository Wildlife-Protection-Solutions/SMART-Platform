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
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.LocalSelectionTransfer;
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
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
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
import org.wcs.smart.i2.RelationshipTypeManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRelationshipGroup;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.model.IntelRelationshipTypeAttribute;
import org.wcs.smart.i2.ui.AttributeLabelProvider;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.IconComposite;
import org.wcs.smart.i2.ui.NamedItemViewerFilter;
import org.wcs.smart.i2.ui.RelationshipGroupLabelProvider;
import org.wcs.smart.ui.ca.properties.NameKeyComposite;
import org.wcs.smart.ui.ca.properties.NameKeyComposite.IChangeListener;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

/**
 * Dialog for editing relationship types.
 * 
 * @author Emily
 *
 */
public class RelationshipTypeDialog extends TitleAreaDialog {

	private static final String NEW_GROUP = "<Create New Group...>";
	
	@Inject
	private IEventBroker eventBroker;
	@Inject
	private IEclipseContext context;
	
	private IntelRelationshipType type;
	private NameKeyComposite nameKeyInfo;
	private IconComposite icon;
	private List<IntelRelationshipType> entityTypeSiblings;
	private TableViewer tblAttributes;
	
	private ComboViewer cmbSrcType;
	private ComboViewer cmbTrgType;
	private ComboViewer cmbGroup;
	
	private ControlDecoration cdSrcType;
	private ControlDecoration cdTrgType;
	
	private MenuItem editItem;
	private MenuItem deleteItem;
	private MenuItem addItem;
	
	private Button btnAdd;
	private Button btnDelete;
	private Button btnEdit;
	private Button btnMoveUp;
	private Button btnMoveDown;
	
	
	private List<IntelRelationshipTypeAttribute> attributeList = new ArrayList<IntelRelationshipTypeAttribute>();
	

	private IntelEntityType initialSourceType;
	private IntelEntityType initialTargetType;
	
	
	private Job loadEntityType = new Job("load entity types"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<IntelEntityType> types = new ArrayList<IntelEntityType>();
			List<Object> groups = new ArrayList<Object>();
			Session s = HibernateManager.openSession();
			try{
				types.addAll(EntityTypeManager.INSTANCE.getEntityTypes(s, SmartDB.getCurrentConservationArea()));
				groups.addAll(RelationshipTypeManager.INSTANCE.getRelationshipGroups(s, SmartDB.getCurrentConservationArea()));

			}finally{
				s.close();
			}
			IntelEntityType any = new IntelEntityType();
			any.setName("<Any>");
			types.add(0, any);
			
			Collections.sort(groups, (a,b) -> Collator.getInstance().compare(((IntelRelationshipGroup)a).getName().toLowerCase(), ((IntelRelationshipGroup)b).getName().toLowerCase()));
			String noGroup = "";
			groups.add(0, noGroup);
			groups.add(NEW_GROUP);
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					cmbSrcType.setInput(types);
					cmbTrgType.setInput(types);
					cmbGroup.setInput(groups);
					
					if (type.getSourceEntityType() != null){
						cmbSrcType.setSelection(new StructuredSelection(type.getSourceEntityType()));
					}else{
						cmbSrcType.setSelection(new StructuredSelection(any));
					}
					
					if (type.getTargetEntityType() != null){
						cmbTrgType.setSelection(new StructuredSelection(type.getTargetEntityType()));
					}else{
						cmbTrgType.setSelection(new StructuredSelection(any));
					}
					if (type.getRelationshipGroup() != null){
						cmbGroup.setSelection(new StructuredSelection(type.getRelationshipGroup()));
					}else{
						cmbGroup.setSelection(new StructuredSelection(noGroup));
					}
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	public RelationshipTypeDialog(Shell parentShell, IntelRelationshipType type) {
		super(parentShell);
		this.type = type;
		
		this.initialSourceType = type.getSourceEntityType();
		this.initialTargetType = type.getTargetEntityType();
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
		boolean attributesModified = false;
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			s.saveOrUpdate(type);
			
			for (IntelRelationshipTypeAttribute a : attributeList){
				if (!type.getAttributes().contains(a)){
					type.getAttributes().add(a);
					attributesModified = true;
				}
			}
			List<IntelRelationshipTypeAttribute> toDelete = new ArrayList<IntelRelationshipTypeAttribute>();
			for (IntelRelationshipTypeAttribute a : type.getAttributes()){
				if (!attributeList.contains(a)){					
					//delete any entity attribute value associations
					Query qDelete = s.createQuery("DELETE FROM IntelEntityRelationshipAttributeValue WHERE id.attribute = :att AND id.relationship IN (FROM IntelEntityRelationship r WHERE r.relationshipType = :relationshipType ) "); //$NON-NLS-1$
					qDelete.setParameter("att", a.getAttribute()); //$NON-NLS-1$
					qDelete.setParameter("relationshipType", type); //$NON-NLS-1$
					qDelete.executeUpdate();
					toDelete.add(a);
					attributesModified = true;
				}
			}
			type.getAttributes().removeAll(toDelete);
			int order = 1;
			for (IntelRelationshipTypeAttribute a : attributeList){
				int index = type.getAttributes().indexOf(a);
				if (index >= 0){
					type.getAttributes().get(index).setOrder(order++);
				}
			}
			Collections.sort(type.getAttributes(), (a,b) -> Integer.compare(a.getOrder(), b.getOrder()));
			s.getTransaction().commit();
			
		}catch (Exception ex){
			if (s.getTransaction().isActive())s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog("Unable to save changes: " +ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		
		
		try{
		
			if (isNew){
				eventBroker.send(IntelEvents.RELATION_TYPE_NEW, type);
			}else{
				eventBroker.send(IntelEvents.RELATION_TYPE_MODIFIED, type);
			}
			
			
			if (attributesModified || !equals(initialSourceType, type.getSourceEntityType()) ||
					!equals(initialTargetType, type.getTargetEntityType())){
				Set<IntelEntityType> modifiedTypes = new HashSet<>();
				modifiedTypes.add(initialSourceType);
				modifiedTypes.add(initialTargetType);
				modifiedTypes.add(type.getSourceEntityType());
				modifiedTypes.add(type.getTargetEntityType());
				if (modifiedTypes.contains(null)){
					//we have to update all types as one is unknown
					List<IntelEntityType> types = (List<IntelEntityType>) cmbSrcType.getInput();
					types.forEach(t -> {if (t.getUuid() != null) { modifiedTypes.add(t); } });
					modifiedTypes.remove(null);
				}
				
				eventBroker.send(IntelEvents.ENTITY_TYPE_TEMPLATE_REFRESH, modifiedTypes);
			}
		}catch (Exception ex){
			//TODO:
		}
		this.initialSourceType = type.getSourceEntityType();
		this.initialTargetType = type.getTargetEntityType();
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		
	}

	private boolean equals(IntelEntityType t1, IntelEntityType t2){
		if (t1 == null && t2 == null) return true;
		if (t1 != null && t2 != null) return t1.equals(t2);
		return false;
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
		btnMoveDown.setEnabled(!tblAttributes.getSelection().isEmpty());
		btnMoveUp.setEnabled(!tblAttributes.getSelection().isEmpty());
	}
	
	private void modified(){
		
		
		boolean isError = false;
		if (nameKeyInfo.validate()){
			isError = true;
		}	
		
		cdSrcType.hide();
		if (cmbSrcType.getSelection().isEmpty()){
			isError = true;
			cdSrcType.setDescriptionText("A source entity type must be selected");
			cdSrcType.show();
		}else{
			if (! (((IStructuredSelection)cmbSrcType.getSelection()).getFirstElement() instanceof IntelEntityType)){
				isError = true;
				cdSrcType.setDescriptionText("A valid entity type must be selected for the source entity type.");
				cdSrcType.show();
			}
		}
		
		cdTrgType.hide();
		if (cmbTrgType.getSelection().isEmpty()){
			isError = true;
			cdTrgType.setDescriptionText("A target entity type must be selected");
			cdTrgType.show();
		}else{
			if (! (((IStructuredSelection)cmbTrgType.getSelection()).getFirstElement() instanceof IntelEntityType)){
				isError = true;
				cdTrgType.setDescriptionText("A valid entity type must be selected for the target entity type.");
				cdTrgType.show();
			}
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
		l.setText("Group:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		cmbGroup = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbGroup.setContentProvider(ArrayContentProvider.getInstance());
		cmbGroup.setLabelProvider(new RelationshipGroupLabelProvider());
		cmbGroup.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbGroup.setInput(new String[]{DialogConstants.LOADING_TEXT});
		cmbGroup.addSelectionChangedListener(new ISelectionChangedListener() {
			private boolean isNew = false;
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)cmbGroup.getSelection()).getFirstElement();
				if (x instanceof IntelRelationshipGroup){
					type.setRelationshipGroup((IntelRelationshipGroup) x);
				}else if (x == NEW_GROUP){
					if (isNew) return;
					isNew = true;
					try{
						//create a new group
						IntelRelationshipGroup group = new IntelRelationshipGroup();
						group.setConservationArea(SmartDB.getCurrentConservationArea());
						group.setRelationshipTypes(new ArrayList<IntelRelationshipType>());
						RelationshipGroupDialog ed = new RelationshipGroupDialog(getShell(), group);
						ed.open();
						if (group.getUuid() != null){
							List<Object> groups = (List<Object>) cmbGroup.getInput();
							groups.add(group);
							Collections.sort(groups, (a,b)->{
								if (a == NEW_GROUP) return 1;
								if (b == NEW_GROUP) return -1;
								if (a instanceof String ) return -1;
								if (b instanceof String ) return 1;
								if (a instanceof IntelRelationshipGroup && b instanceof IntelRelationshipGroup){
									return Collator.getInstance().compare( ((IntelRelationshipGroup)a).getName().toLowerCase(),((IntelRelationshipGroup)b).getName().toLowerCase());  
								}
								return 0;
							});
							cmbGroup.refresh();
							cmbGroup.setSelection(new StructuredSelection(group));
							
						}
					}finally{
						isNew = false;
					}
				}else{
					type.setRelationshipGroup(null);
				}
				modified();
			}
		});
		
		l = new Label(parent, SWT.NONE);
		l.setText("Source Entity Type:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		cmbSrcType = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbSrcType.setContentProvider(ArrayContentProvider.getInstance());
		cmbSrcType.setLabelProvider(new EntityTypeLabelProvider());
		cmbSrcType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbSrcType.setInput(new String[]{DialogConstants.LOADING_TEXT});
		cmbSrcType.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)cmbSrcType.getSelection()).getFirstElement();
				if (x instanceof IntelEntityType){
					if ( ((IntelEntityType)x).getUuid() == null){
						type.setSourceEntityType(null);
					}else{
						type.setSourceEntityType((IntelEntityType) x);
					}
				}
				modified();
			}
		});
		
		cdSrcType = createDecoration(cmbSrcType.getControl());
		
		l = new Label(parent, SWT.NONE);
		l.setText("Target Entity Type:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		cmbTrgType = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbTrgType.setContentProvider(ArrayContentProvider.getInstance());
		cmbTrgType.setLabelProvider(new EntityTypeLabelProvider());
		cmbTrgType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbTrgType.setInput(new String[]{DialogConstants.LOADING_TEXT});
		cmbTrgType.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)cmbTrgType.getSelection()).getFirstElement();
				if ( ((IntelEntityType)x).getUuid() == null){
					type.setTargetEntityType(null);
				}else{
					type.setTargetEntityType((IntelEntityType) x);
				}
				modified();
			}
		});
		
		cdTrgType = createDecoration(cmbTrgType.getControl());
		
		l = new Label(parent, SWT.NONE);
		l.setText("Attributes:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		
		
		Composite attributeComp = new Composite(parent, SWT.NONE);
		attributeComp.setLayout(new GridLayout(2, false));
		attributeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
				
		tblAttributes = new TableViewer(attributeComp, SWT.BORDER | SWT.MULTI);
		tblAttributes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblAttributes.setContentProvider(ArrayContentProvider.getInstance());
		tblAttributes.setLabelProvider(new AttributeLabelProvider());
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
		tblAttributes.addDragSupport(DND.DROP_MOVE, new Transfer[]{LocalSelectionTransfer.getTransfer()}, new DragSourceListener() {
			
			@Override
			public void dragStart(DragSourceEvent event) {
				LocalSelectionTransfer.getTransfer().setSelection(tblAttributes.getSelection());
				
			}
			
			@Override
			public void dragSetData(DragSourceEvent event) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
					event.data = tblAttributes.getSelection();
				}
			}
			
			@Override
			public void dragFinished(DragSourceEvent event) {
				LocalSelectionTransfer.getTransfer().setSelection(null);
			}
		});
		tblAttributes.addDropSupport(DND.DROP_MOVE, new Transfer[]{LocalSelectionTransfer.getTransfer()}, new ViewerDropAdapter(tblAttributes) {
			@Override
			public boolean performDrop(Object data) {
				StructuredSelection selection = (StructuredSelection)LocalSelectionTransfer.getTransfer().getSelection();
				if (selection == null){
					return false;
				}
				Object obj = selection.getFirstElement();
				if (obj.equals(getCurrentTarget())) return false;
				if (obj instanceof IntelRelationshipTypeAttribute){
					int loc = getCurrentLocation();
					attributeList.remove(obj);
					int targetIndex = attributeList.indexOf(getCurrentTarget());					
					if (loc == LOCATION_AFTER){
						targetIndex ++;
					}
					if (targetIndex < 0) targetIndex = 0;
					if (targetIndex > attributeList.size()) targetIndex = attributeList.size();
					attributeList.add(targetIndex, (IntelRelationshipTypeAttribute) obj);
					getViewer().refresh();
					modified();
				}
				return true;
			}

			@Override
			public boolean validateDrop(Object target, int operation, TransferData transferType) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType) &&
						operation == DND.DROP_MOVE && getCurrentTarget() != null){
					return true;
				}
				return false;
			}
			
		});
		
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
		
		Label s = new Label(buttonComp, SWT.HORIZONTAL | SWT.SEPARATOR);
		s.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		btnMoveUp = new Button(buttonComp, SWT.NONE);
		btnMoveUp.setText("Move Down");
		btnMoveUp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnMoveUp.setEnabled(false);
		btnMoveUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				moveAttribute(SWT.UP);
			}
		});
		
		btnMoveDown = new Button(buttonComp, SWT.NONE);
		btnMoveDown.setText("Move Up");
		btnMoveDown.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnMoveDown.setEnabled(false);
		btnMoveDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				moveAttribute(SWT.DOWN);
			}
		});
		
		setTitle("Relationship Type");
		getShell().setText("Relationship Type");
		setMessage("Configure relationship type");
		
		return parent;
	}
	
	private void moveAttribute(int direction){
		for (Iterator<?> iterator = ((IStructuredSelection) tblAttributes.getSelection()).iterator(); iterator.hasNext();) {
			IntelRelationshipTypeAttribute a = (IntelRelationshipTypeAttribute) iterator.next();
			
			int index = attributeList.indexOf(a);
			if (direction == SWT.UP){
				index ++;
				if(index >= attributeList.size()){
					index = attributeList.size() - 1;
				}
			}else if (direction == SWT.DOWN){
				index --;
				if(index < 0) index = 0;
			}
			
			attributeList.remove(a);
			attributeList.add(index, a);
		}
		modified();
		tblAttributes.refresh();
	}
	
	private void addAttribute(){
		AttributeListDialog dialog = new AttributeListDialog(getShell());
		if (dialog.open() == Window.OK){
			tblAttributes.refresh();
			modified();
		}
	}
	
	private void editAttribute(){
		Object x = ((IStructuredSelection)tblAttributes.getSelection()).getFirstElement();
		if (x instanceof IntelRelationshipTypeAttribute){
			IntelRelationshipTypeAttribute attribute = (IntelRelationshipTypeAttribute)x;
			AttributeDialog.showAttributeDialog(getShell(), attribute.getAttribute(), context);
			//refresh
			tblAttributes.refresh();
		}
	}

	
	private void removeAttributes(){
		IStructuredSelection items = (IStructuredSelection)tblAttributes.getSelection();
		final List<IntelRelationshipTypeAttribute> toDelete = new ArrayList<IntelRelationshipTypeAttribute>();
		for (Iterator<?> iterator = items.iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof IntelRelationshipTypeAttribute){
				toDelete.add((IntelRelationshipTypeAttribute) x);
			}
		}
		
		final List<String> warnings = new ArrayList<String>();
		final List<IntelRelationshipTypeAttribute> aToDelete = new ArrayList<IntelRelationshipTypeAttribute>();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress(){
	
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					Session session = HibernateManager.openSession();
					try{
						for (IntelRelationshipTypeAttribute x : toDelete){
							try{
								DeleteManager.canDelete(x, session);
								aToDelete.add(x);
							}catch (Exception ex){
								warnings.add(MessageFormat.format("The attribute {0} cannot be removed. {1}", ((IntelRelationshipTypeAttribute) x).getAttribute().getName(), ex.getMessage()));
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
			for (IntelRelationshipTypeAttribute d: aToDelete){
				sb.append(d.getAttribute().getName());
				sb.append(", ");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			if (MessageDialog.openConfirm(getShell(), "Remove Attributes", MessageFormat.format("Are you sure you want to delete the attributes {0}? \n All attribute information associated with entities will also be removed.", sb.toString()))){
				attributeList.removeAll(aToDelete);
				modified();
			}
		}
		
		tblAttributes.refresh();
	}
	
	
	private void initFields(){
		if (type.getIcon() != null){
			icon.setImage(type.getIcon());
		}
		attributeList.addAll(type.getAttributes());
		tblAttributes.setInput(attributeList);
		
		siblingsJob.setSystem(true);
		siblingsJob.schedule(0);
		
		loadEntityType.setSystem(true);
		loadEntityType.schedule(0);
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
				entityTypeSiblings = RelationshipTypeManager.INSTANCE.getRelationshipTypes(s, SmartDB.getCurrentConservationArea());
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
					IntelRelationshipTypeAttribute a  = new IntelRelationshipTypeAttribute();
					a.setAttribute((IntelAttribute) selection);
					a.setRelationshipType(RelationshipTypeDialog.this.type);
					if (!RelationshipTypeDialog.this.attributeList.contains(a)) RelationshipTypeDialog.this.attributeList.add(a);
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
			attributeList.setLabelProvider(new AttributeLabelProvider());
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
			if (type.getAttributes() == null) type.setAttributes(new ArrayList<IntelRelationshipTypeAttribute>());
			AttributeDialog.showAttributeDialog(getShell(), attribute, context);
			
			if (attribute.getUuid() != null){
				IntelRelationshipTypeAttribute eta = new IntelRelationshipTypeAttribute();
				eta.setAttribute(attribute);
				eta.setRelationshipType(type);
				if (!RelationshipTypeDialog.this.attributeList.contains(eta)) RelationshipTypeDialog.this.attributeList.add(eta);
				
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