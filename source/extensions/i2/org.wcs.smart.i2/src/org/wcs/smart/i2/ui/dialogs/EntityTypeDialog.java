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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
import org.eclipse.jface.dialogs.InputDialog;
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
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
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
import org.eclipse.swt.graphics.Image;
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
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.EntityTypeManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.IntelReportManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelEntityTypeAttributeGroup;
import org.wcs.smart.i2.model.OtherAttributeGroup;
import org.wcs.smart.i2.ui.AttributeLabelProvider;
import org.wcs.smart.i2.ui.IconComposite;
import org.wcs.smart.i2.ui.NamedItemViewerFilter;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
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
	private TreeViewer treeAttributes;
	private ComboViewer idAttribute;
	
	private MenuItem editItem;
	private MenuItem deleteItem;
	private MenuItem addItem;
	private MenuItem addGroupItem;
	
	private Button btnAdd;
	private Button btnNewGroup;
	private Button btnDelete;
	private Button btnEdit;
	private Button btnMoveUp;
	private Button btnMoveDown;
	
	private ControlDecoration cdList;
	private ControlDecoration cdId;
	
	private List<IntelEntityTypeAttribute> attributeList = new ArrayList<IntelEntityTypeAttribute>();
	private List<IntelEntityTypeAttributeGroup> groups = new ArrayList<IntelEntityTypeAttributeGroup>();
	
	@Inject
	private IEventBroker broker;
	@Inject
	private IEclipseContext context;
	
	@Inject
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
	
	@Override
	public void cancelPressed(){
		if (getButton(IDialogConstants.OK_ID).isEnabled()){
			if (MessageDialog.openQuestion(getShell(), "Close", "Would you like to save changes before closing?")){
				okPressed();
			}
		}
		super.cancelPressed();
	}
	@SuppressWarnings("unchecked")
	@Override
	protected void okPressed() {
		boolean isNew = type.getUuid() == null;
		boolean attributesModified = false;
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			s.saveOrUpdate(type);
			
			//set order and update groups
			for (int i = 0; i < groups.size(); i ++){
				groups.get(i).setOrder(i);
				s.saveOrUpdate(groups.get(i));
			}
			
			for (IntelEntityTypeAttribute a : attributeList){
				if (!type.getAttributes().contains(a)){
					//new items 
					type.getAttributes().add(a);
					attributesModified = true;
				}
			}
			
			List<IntelEntityTypeAttribute> toDelete = new ArrayList<IntelEntityTypeAttribute>();
			for (IntelEntityTypeAttribute a : type.getAttributes()){
				if (!attributeList.contains(a)){
					//delete any entity attribute value associations
					Query qDelete = s.createQuery("DELETE FROM IntelEntityAttributeValue WHERE id.attribute = :att AND id.entity IN ( FROM IntelEntity e WHERE e.entityType = :entityType ) "); //$NON-NLS-1$
					qDelete.setParameter("att", a.getAttribute()); //$NON-NLS-1$
					qDelete.setParameter("entityType", type); //$NON-NLS-1$
					qDelete.executeUpdate();
					toDelete.add(a);
					attributesModified = true;
				}
			}
			type.getAttributes().removeAll(toDelete);
			
			HashMap<IntelEntityTypeAttributeGroup, Integer> orderCnt = new HashMap<>();
			for (IntelEntityTypeAttribute a : attributeList){
				Integer x = orderCnt.get(a.getAttributeGroup());
				if (x == null){
					x = 0;
				}
				x++;
				orderCnt.put(a.getAttributeGroup(), x);
				for (IntelEntityTypeAttribute aa : type.getAttributes()){
					if (aa.equals(a)){
						aa.setOrder(x);
						break;
					}
				}
				a.setOrder(x);
			}
			Collections.sort(type.getAttributes(), (a,b) -> Integer.compare(a.getOrder(), b.getOrder()));
			Collections.sort(groups, (a,b) -> Integer.compare(a.getOrder(), b.getOrder()));
			
			//remove groups
			List<IntelEntityTypeAttributeGroup> currentGroups = s.createCriteria(IntelEntityTypeAttributeGroup.class)
					.add(Restrictions.eq("entityType", type))
					.list();
			for (IntelEntityTypeAttributeGroup g : currentGroups){
				if (!groups.contains(g)){
					s.delete(g);
				}
			}
			s.flush();
			s.getTransaction().commit();
		}catch (Exception ex){
			if (s.getTransaction().isActive())s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog("Unable to save changes: " +ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		if (isNew){
			broker.send(IntelEvents.ENTITY_TYPE_NEW, type);
		}else{
			broker.send(IntelEvents.ENTITY_TYPE_MODIFIED, type);
			if (attributesModified){
				broker.send(IntelEvents.ENTITY_TYPE_TEMPLATE_REFRESH, type);
			}
			
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
		boolean ok = false;
		if (!treeAttributes.getSelection().isEmpty()){
			IStructuredSelection sel = (IStructuredSelection)treeAttributes.getSelection();
			for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
				Object next = iterator.next();
				if (next instanceof IntelEntityTypeAttribute || next instanceof IntelEntityTypeAttributeGroup){
					ok = true;
					break;
				}
				
			}
		}
		
		btnEdit.setEnabled(ok);
		btnDelete.setEnabled(ok);
		editItem.setEnabled(ok);
		deleteItem.setEnabled(ok);
		btnMoveUp.setEnabled(ok);
		btnMoveDown.setEnabled(ok);
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
		l.setText("ID Attribute:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		idAttribute = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		idAttribute.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		idAttribute.setContentProvider(ArrayContentProvider.getInstance());
		idAttribute.setLabelProvider(new AttributeLabelProvider());
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
		l.setText("Print Template:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		l = new Label(parent, SWT.NONE);
		l.setText(type.getBirtTemplate() == null ? "Not Configured" : type.getBirtTemplate());
		l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		
		Button btnEditTemplate = new Button(parent, SWT.PUSH);
		btnEditTemplate.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEditTemplate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnEditTemplate.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				cancelPressed();
				getParentShell().close();
				IntelReportManager.INSTANCE.editTemplate(type);
			}
			
		});
		
		l = new Label(parent, SWT.NONE);
		l.setText("Attributes:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		
		
		Composite attributeComp = new Composite(parent, SWT.NONE);
		attributeComp.setLayout(new GridLayout(2, false));
		attributeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
				
		treeAttributes = new TreeViewer(attributeComp, SWT.BORDER | SWT.MULTI);
		treeAttributes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		treeAttributes.setContentProvider(new AttributeTreeContentProvider());
		treeAttributes.setLabelProvider(new LabelProvider(){
			AttributeLabelProvider attribute = new AttributeLabelProvider();
			@Override
			public String getText(Object element){
				if (element instanceof IntelEntityTypeAttributeGroup){
					return ((IntelEntityTypeAttributeGroup) element).getName();
				}else if (element instanceof OtherAttributeGroup){
					return ((OtherAttributeGroup)element).getName();
				}
				return attribute.getText(element);		
			}
			
			@Override
			public Image getImage(Object element){
				if (element instanceof IntelEntityTypeAttribute){
					return attribute.getImage(element);		
				}else if (element instanceof IntelEntityTypeAttributeGroup || 
						element instanceof OtherAttributeGroup){
					return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ATTRIBUTE_GROUP);
				}
				return null;
			}
			
			@Override
			public void dispose(){
				attribute.dispose();
				super.dispose();
			}
			
		});
		treeAttributes.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				attributeSelectionModified();
			}
		});
		treeAttributes.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editAttribute();
				
			}
		});
		cdList = createDecoration(treeAttributes.getControl());
		int operations = DND.DROP_MOVE;
		Transfer[] transferTypes = new Transfer[]{LocalSelectionTransfer.getTransfer()};
		treeAttributes.addDragSupport(operations, transferTypes , new DragSourceListener() {
			
			@Override
			public void dragStart(DragSourceEvent event) {
				LocalSelectionTransfer.getTransfer().setSelection(treeAttributes.getSelection());				
			}
			
			@Override
			public void dragSetData(DragSourceEvent event) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
					event.data = treeAttributes.getSelection();
				}	
			}
			
			@Override
			public void dragFinished(DragSourceEvent event) {
				LocalSelectionTransfer.getTransfer().setSelection(null);
				
			}
		});
		
		treeAttributes.addDropSupport(operations, transferTypes, new ViewerDropAdapter(treeAttributes) {
			@Override
			public boolean performDrop(Object data) {
				StructuredSelection selection = (StructuredSelection)LocalSelectionTransfer.getTransfer().getSelection();
				if (selection == null){
					return false;
				}
				Object obj = selection.getFirstElement();
				if (obj.equals(getCurrentTarget())) return false;
				if (obj instanceof IntelEntityTypeAttribute){
					IntelEntityTypeAttribute target = (IntelEntityTypeAttribute)obj;
					if (getCurrentTarget() instanceof IntelEntityTypeAttributeGroup){
						target.setAttributeGroup( (IntelEntityTypeAttributeGroup)getCurrentTarget() );
					}else if (getCurrentTarget() instanceof IntelEntityTypeAttribute){
						target.setAttributeGroup(  ((IntelEntityTypeAttribute)getCurrentTarget()).getAttributeGroup());
					}else if (getCurrentTarget().equals(OtherAttributeGroup.INSTANCE)){
						//none object
						target.setAttributeGroup(null);
					}
					
					int loc = getCurrentLocation();
					attributeList.remove(obj);
					int targetIndex = attributeList.indexOf(getCurrentTarget());					
					if (loc == LOCATION_AFTER){
						targetIndex ++;
					}
					if (targetIndex < 0) targetIndex = 0;
					if (targetIndex > attributeList.size()) targetIndex = attributeList.size();
					attributeList.add(targetIndex, (IntelEntityTypeAttribute) obj);
					getViewer().refresh();
					modified();
				}
				if (obj instanceof IntelEntityTypeAttributeGroup){
					IntelEntityTypeAttributeGroup target = (IntelEntityTypeAttributeGroup)obj;
					int loc = getCurrentLocation();
					groups.remove(target);
					int targetIndex = groups.indexOf(getCurrentTarget());					
					if (loc == LOCATION_AFTER){
						targetIndex ++;
					}
					if (targetIndex < 0) targetIndex = 0;
					if (targetIndex > groups.size()) targetIndex = groups.size();
					groups.add(targetIndex, target);
					getViewer().refresh();
					modified();
				}
				return true;
			}

			@Override
			public boolean validateDrop(Object target, int operation, TransferData transferType) {
				Object moving = ((IStructuredSelection)LocalSelectionTransfer.getTransfer().getSelection()).getFirstElement();
				if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType) &&
						operation == DND.DROP_MOVE && getCurrentTarget() != null){
					if (moving instanceof IntelEntityTypeAttributeGroup){
						return target instanceof IntelEntityTypeAttributeGroup;
					}
					if (moving instanceof IntelEntityTypeAttribute || 
							moving instanceof IntelEntityTypeAttributeGroup){
						return true;
					}
					//do not move none object
					return false;
				}
				return false;
			}
			
		});
		
		Menu listMenu = new Menu(treeAttributes.getControl());
		treeAttributes.getControl().setMenu(listMenu);
		
		addItem = new MenuItem(listMenu, SWT.DEFAULT);
		addItem.setText("Add Attribute");
		addItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		addItem.setEnabled(true);
		addItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addAttribute();
			}
		});
		
		addGroupItem = new MenuItem(listMenu, SWT.DEFAULT);
		addGroupItem.setText("New Group...");
		addGroupItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ATTRIBUTE_GROUP_NEW));
		addGroupItem.setEnabled(true);
		addGroupItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addGroup();
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
		btnAdd.setText("Add Attribute");
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addAttribute();
			}
		});
		
		btnNewGroup = new Button(buttonComp, SWT.NONE);
		btnNewGroup.setText("New Group");
		btnNewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnNewGroup.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addGroup();
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
		setTitle("Entity Type");
		getShell().setText("Entity Type");
		setMessage("Configure entity type");
		
		return parent;
	}
	
	private void moveAttribute(int direction){
		for (Iterator<?> iterator = ((IStructuredSelection) treeAttributes.getSelection()).iterator(); iterator.hasNext();) {
			Object toMove = iterator.next();
			if (toMove instanceof IntelEntityTypeAttribute){
				IntelEntityTypeAttribute a = (IntelEntityTypeAttribute) toMove;
				
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
			}else if (toMove instanceof IntelEntityTypeAttributeGroup){
				IntelEntityTypeAttributeGroup a = (IntelEntityTypeAttributeGroup) toMove;
				
				int index = groups.indexOf(a);
				if (direction == SWT.UP){
					index ++;
					if(index >= groups.size()){
						index = groups.size() - 1;
					}
				}else if (direction == SWT.DOWN){
					index --;
					if(index < 0) index = 0;
				}
				
				groups.remove(a);
				groups.add(index, a);
			}
		}
		modified();
		treeAttributes.refresh();
	}
	
	private void addGroup(){
		InputDialog dialog = new InputDialog(getParentShell(), "New Group", "Name for group",
				"New Group", (text)-> text.trim().isEmpty() ? "Name cannot be empty" : null);
		if (dialog.open() == Window.OK){
			IntelEntityTypeAttributeGroup newGroup = new IntelEntityTypeAttributeGroup();
			newGroup.setEntityType(type);
			String name = dialog.getValue();
			newGroup.setName(name);
			newGroup.updateName(SmartDB.getCurrentLanguage(), name);
			newGroup.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), name);
		
			groups.add(newGroup);
			treeAttributes.refresh();
		}
	}
	
	private void addAttribute(){
		IntelEntityTypeAttributeGroup parent = null;
		if (!treeAttributes.getSelection().isEmpty()){
			Object x = ((IStructuredSelection)treeAttributes.getSelection()).getFirstElement();
			if (x instanceof IntelEntityTypeAttributeGroup){
				parent = (IntelEntityTypeAttributeGroup) x;
			}else if (x instanceof IntelEntityTypeAttribute){
				parent = ((IntelEntityTypeAttribute) x).getAttributeGroup();
			}
		}
		AttributeListDialog dialog = new AttributeListDialog(getShell(), parent);
		if (dialog.open() == Window.OK){
			treeAttributes.refresh();
			if (type.getIdAttribute() == null){
				type.setIdAttribute(attributeList.get(0).getAttribute());
			}
			refreshAttributeList();
			modified();
		}
	}
	
	private void editAttribute(){
		Object x = ((IStructuredSelection)treeAttributes.getSelection()).getFirstElement();
		if (x instanceof IntelEntityTypeAttribute){
			IntelEntityTypeAttribute attribute = (IntelEntityTypeAttribute)x;
			AttributeDialog.showAttributeDialog(getShell(), attribute.getAttribute(), context); 
			//refresh
			treeAttributes.refresh();
			refreshAttributeList();
		}
		if (x instanceof IntelEntityTypeAttributeGroup){
			IntelEntityTypeAttributeGroup toRename = (IntelEntityTypeAttributeGroup)x;
			TranslateSimpleListItemDialog dialog = new TranslateSimpleListItemDialog(getShell(), toRename);
			if (dialog.open() == Window.OK){
				treeAttributes.refresh();
				modified();
			}
		}
	}

	
	private void removeAttributes(){
		IStructuredSelection items = (IStructuredSelection)treeAttributes.getSelection();
		final List<IntelEntityTypeAttribute> toDelete = new ArrayList<IntelEntityTypeAttribute>();
		final List<IntelEntityTypeAttributeGroup> toDeleteGroups = new ArrayList<IntelEntityTypeAttributeGroup>();
		for (Iterator<?> iterator = items.iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof IntelEntityTypeAttribute){
				toDelete.add((IntelEntityTypeAttribute) x);
			}else if (x instanceof IntelEntityTypeAttributeGroup){
				IntelEntityTypeAttributeGroup group = (IntelEntityTypeAttributeGroup)x;
				toDeleteGroups.add(group);
				for (IntelEntityTypeAttribute a : attributeList){
					if (group.equals(a.getAttributeGroup())) toDelete.add(a);
				}
			}
		}
		
		final List<String> warnings = new ArrayList<String>();
		final List<IntelEntityTypeAttribute> aToDelete = new ArrayList<IntelEntityTypeAttribute>();
		final List<IntelEntityTypeAttributeGroup> gToDelete = new ArrayList<IntelEntityTypeAttributeGroup>(toDeleteGroups);	
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
								warnings.add(MessageFormat.format("The attribute {0} cannot be removed. {1}", x.getAttribute().getName(), ex.getMessage()));
								//cannot remove associated group
								if (x.getAttributeGroup() != null && gToDelete.contains(x.getAttributeGroup())){
									warnings.add(MessageFormat.format("The attribute group {0} cannot be removed. {1}", x.getAttributeGroup().getName(), ex.getMessage()));
									gToDelete.remove(x.getAttributeGroup());
								}
							}
						}	
						for (IntelEntityTypeAttributeGroup g : toDeleteGroups){
							if (!gToDelete.contains(g)) continue;
							try{
								DeleteManager.canDelete(g, session);
							}catch (Exception ex){
								gToDelete.remove(g);
								warnings.add(MessageFormat.format("The attribute group {0} cannot be removed. {1}", g.getName(), ex.getMessage()));
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
		
		if (aToDelete.size() > 0 || gToDelete.size() > 0){
			StringBuilder sb = new StringBuilder();
			for (IntelEntityTypeAttribute d: aToDelete){
				sb.append(d.getAttribute().getName());
				sb.append(", ");
			}
			for (IntelEntityTypeAttributeGroup d: gToDelete){
				sb.append(d.getName());
				sb.append(", ");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			if (MessageDialog.openConfirm(getShell(), "Remove Attributes / Groups", MessageFormat.format("Are you sure you want to delete the attributes (and groups) {0}? \n All attribute information associated with entities will also be removed.", sb.toString()))){
				attributeList.removeAll(aToDelete);
				groups.removeAll(gToDelete);
			}
		}
		
		treeAttributes.refresh();
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
		treeAttributes.setInput(attributeList);
		
		refreshAttributeList();
		siblingsJob.setSystem(true);
		siblingsJob.schedule(0);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	
	private Job siblingsJob = new Job("get siblings"){ //$NON-NLS-1$

		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			try{
				entityTypeSiblings = EntityTypeManager.INSTANCE.getEntityTypes(s, SmartDB.getCurrentConservationArea());
				entityTypeSiblings.remove(type);
				
				groups = s.createCriteria(IntelEntityTypeAttributeGroup.class)
						.add(Restrictions.eq("entityType", type))
						.addOrder(Order.asc("order"))
						.list();
				for (IntelEntityTypeAttributeGroup g : groups) g.getNames().size();
			}finally{
				s.close();
			}
			
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					nameKeyInfo.initFields(type, entityTypeSiblings, SmartDB.getCurrentConservationArea().getDefaultLanguage());					
					getButton(IDialogConstants.OK_ID).setEnabled(type.getUuid() == null);
					
					treeAttributes.refresh();
					treeAttributes.expandAll();
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	
	
	
	private class AttributeListDialog extends TitleAreaDialog{
		private CheckboxTableViewer attributeList;
		private NamedItemViewerFilter filter;
		private IntelEntityTypeAttributeGroup group;
		
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
		public AttributeListDialog(Shell parentShell, IntelEntityTypeAttributeGroup group) {
			super(parentShell);
			this.group = group;
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
					a.setAttributeGroup(group);
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
			AttributeDialog.showAttributeDialog(getShell(), attribute, context);
			
			if (attribute.getUuid() != null){
				IntelEntityTypeAttribute eta = new IntelEntityTypeAttribute();
				eta.setAttribute(attribute);
				eta.setEntityType(type);
				eta.setAttributeGroup(group);
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
	
	private class AttributeTreeContentProvider implements ITreeContentProvider{

		
		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			Object[] items = new Object[groups.size() + 1];
			for (int i = 0; i < groups.size(); i  ++){
				items[i] = groups.get(i);
			}
			items[items.length - 1] = OtherAttributeGroup.INSTANCE;
			return items;
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IntelEntityTypeAttributeGroup){
				IntelEntityTypeAttributeGroup g = (IntelEntityTypeAttributeGroup)parentElement;
				List<IntelEntityTypeAttribute> a = new ArrayList<IntelEntityTypeAttribute>();
				for (IntelEntityTypeAttribute aa : attributeList){
					if (g.equals(aa.getAttributeGroup())){
						a.add(aa);
					}
				}
				return a.toArray();
			}
			if (parentElement == OtherAttributeGroup.INSTANCE){
				List<IntelEntityTypeAttribute> a = new ArrayList<IntelEntityTypeAttribute>();
				for (IntelEntityTypeAttribute aa : attributeList){
					if (aa.getAttributeGroup() == null){
						a.add(aa);
					}
				}
				return a.toArray();
			}
			return null;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof IntelEntityTypeAttribute){
				IntelEntityTypeAttribute a = (IntelEntityTypeAttribute)element;
				if (a.getAttributeGroup() != null) return a.getAttributeGroup();
				return OtherAttributeGroup.INSTANCE;
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof IntelEntityTypeAttributeGroup || element == OtherAttributeGroup.INSTANCE;
		}
		
	}
}