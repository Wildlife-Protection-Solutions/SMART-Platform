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
package org.wcs.smart.asset.ui.config;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
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
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
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
import org.eclipse.swt.widgets.Widget;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.AssetTypeManager;
import org.wcs.smart.asset.model.AbstractAssetTypeAttributeMapping;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.model.AssetTypeAttribute;
import org.wcs.smart.asset.model.AssetTypeDeploymentAttribute;
import org.wcs.smart.asset.ui.AttributeLabelProvider;
import org.wcs.smart.asset.ui.SelectAttributeDialog;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.common.control.IconComposite;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.ca.properties.NameKeyComposite;
import org.wcs.smart.ui.ca.properties.NameKeyComposite.IChangeListener;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for editing asset types.
 * 
 * @author Emily
 *
 */
public class AssetTypeDialog extends TitleAreaDialog {

	private AssetType type;
	private NameKeyComposite nameKeyInfo;
	private IconComposite icon;
	private List<AssetType> assetTypeSiblings;
	private TableViewer treeAttributes;
	private TableViewer treeDeployAttributes;
	
//	private MenuItem editItem;
//	private MenuItem deleteItem;
//	private MenuItem addItem;
//	
//	private Button btnAdd;
//	private Button btnDelete;
//	private Button btnEdit;
//	private Button btnMoveUp;
//	private Button btnMoveDown;
		
	private List<AbstractAssetTypeAttributeMapping> assetAttributeList = new ArrayList<>();
	private List<AbstractAssetTypeAttributeMapping> assetDeploymentList = new ArrayList<>();
	
	@Inject
	private IEventBroker broker;
	@Inject IEclipseContext context;
	
	@Inject
	public AssetTypeDialog(Shell parentShell, AssetType type) {
		super(parentShell);
		this.type = type;
	}

	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		return new Point(p.x,650);
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
	
	@Override
	protected void okPressed() {
		boolean isNew = type.getUuid() == null;
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try {
				s.saveOrUpdate(type);
				
				for (AbstractAssetTypeAttributeMapping a : assetAttributeList){
					if (!type.getAssetAttributes().contains(a)) type.getAssetAttributes().add((AssetTypeAttribute)a);
				}
				for (AbstractAssetTypeAttributeMapping a : assetDeploymentList) {
					if (!type.getAssetDeploymentAttributes().contains(a)) type.getAssetDeploymentAttributes().add((AssetTypeDeploymentAttribute)a);
				}
				
				List<AssetTypeAttribute> toDelete = new ArrayList<AssetTypeAttribute>();
				for (AssetTypeAttribute a : type.getAssetAttributes()){
					if (!assetAttributeList.contains(a)){
						//delete any asset attribute value associations
						Query<?> qDelete = s.createQuery("DELETE FROM AssetAttributeValue WHERE id.attribute = :att AND id.asset IN ( FROM Asset e WHERE e.assetType = :assetType ) "); //$NON-NLS-1$
						qDelete.setParameter("att", a.getAttribute()); //$NON-NLS-1$
						qDelete.setParameter("assetType", type); //$NON-NLS-1$
						qDelete.executeUpdate();
						toDelete.add(a);
					}
				}
				type.getAssetAttributes().removeAll(toDelete);
				for (int i = 0; i < assetAttributeList.size(); i ++) {
					assetAttributeList.get(i).setOrder(i);
				}

				List<AssetTypeDeploymentAttribute> toDelete2 = new ArrayList<>();
				for (AssetTypeDeploymentAttribute a : type.getAssetDeploymentAttributes()){
					if (!assetDeploymentList.contains(a)){
						//delete any asset deployment attribute value associations
						Query<?> qDelete = s.createQuery("DELETE FROM AssetDeploymentAttributeValue WHERE id.attribute = :att AND id.assetDeployment IN ( FROM AssetDeployment e WHERE e.asset.assetType = :assetType ) "); //$NON-NLS-1$
						qDelete.setParameter("att", a.getAttribute()); //$NON-NLS-1$
						qDelete.setParameter("assetType", type); //$NON-NLS-1$
						qDelete.executeUpdate();
						toDelete2.add(a);
					}
				}
				type.getAssetDeploymentAttributes().removeAll(toDelete2);
				for (int i = 0; i < assetDeploymentList.size(); i ++) {
					assetDeploymentList.get(i).setOrder(i);
				}
				
				s.flush();
				s.getTransaction().commit();
			}catch (Exception ex){
				if (s.getTransaction().isActive())s.getTransaction().rollback();
				AssetPlugIn.displayLog("Unable to save changes:" + ex.getMessage(), ex);
				return;
			}
		}
		if (isNew){
			broker.send(AssetEvents.ASSETTYPE_NEW, type);
		}else{
			broker.send(AssetEvents.ASSETTYPE_MODIFIED, type);
		}
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT,true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		
		initFields();
	}
	
	private void attributeSelectionModified(TableViewer viewer, Widget[] controls){
		boolean ok = false;
		if (!viewer.getSelection().isEmpty()){
			IStructuredSelection sel = (IStructuredSelection)viewer.getSelection();
			for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
				Object next = iterator.next();
				if (next instanceof AbstractAssetTypeAttributeMapping){
					ok = true;
					break;
				}
				
			}
		}
		for (Widget w : controls) {
			if (w instanceof Control) ((Control)w).setEnabled(ok);
			if (w instanceof MenuItem) ((MenuItem)w).setEnabled(ok);
		}
	}
	
	private void modified(){
		boolean isError = false;
		if (nameKeyInfo.validate()){
			isError = true;
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
		l.setText("Asset Attributes:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		l.setToolTipText("These attributes will be collected for each Asset of this type");
		
		Composite attributeComp = new Composite(parent, SWT.NONE);
		attributeComp.setLayout(new GridLayout(2, false));
		attributeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		treeAttributes = createAttributesComposite(attributeComp, assetAttributeList);
		
		l = new Label(parent, SWT.NONE);
		l.setText("Deployment Attributes:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		l.setToolTipText("These attributes will be collected for each Asset Deployment of this type");
		
		Composite attributedDeployComp = new Composite(parent, SWT.NONE);
		attributedDeployComp.setLayout(new GridLayout(2, false));
		attributedDeployComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		treeDeployAttributes = createAttributesComposite(attributedDeployComp, assetDeploymentList);
		
		setTitle("Asset Type");
		getShell().setText("Asset Type");
		setMessage("Configure asset types");
		
		return parent;
	}
	
	private TableViewer createAttributesComposite(Composite parent, List<AbstractAssetTypeAttributeMapping> attributeList) {
		TableViewer treeAttributes = new TableViewer(parent, SWT.BORDER | SWT.MULTI);
		treeAttributes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		treeAttributes.setContentProvider(ArrayContentProvider.getInstance());
		treeAttributes.setLabelProvider(new AttributeLabelProvider());		
		
		treeAttributes.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editAttribute(treeAttributes, attributeList);
				
			}
		});

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
				List<Object> items = new ArrayList<>();
				for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
					Object obj = iterator.next();
					if (obj.equals(getCurrentTarget())) continue;
					items.add(0,obj);
				}
				
				for (Object obj: items){	
					if (obj.equals(getCurrentTarget())) return false;
					if (obj instanceof AbstractAssetTypeAttributeMapping){
						
						int loc = getCurrentLocation();
						attributeList.remove(obj);
						int targetIndex = attributeList.indexOf(getCurrentTarget());					
						if (loc == LOCATION_AFTER){
							targetIndex ++;
						}
						if (targetIndex < 0) targetIndex = 0;
						if (targetIndex > attributeList.size()) targetIndex = attributeList.size();
						attributeList.add(targetIndex, (AbstractAssetTypeAttributeMapping)obj);
						getViewer().refresh();
						modified();
					}
				}
				return true;
			}

			@Override
			public boolean validateDrop(Object target, int operation, TransferData transferType) {
				Object moving = ((IStructuredSelection)LocalSelectionTransfer.getTransfer().getSelection()).getFirstElement();
				if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType) &&
						operation == DND.DROP_MOVE && getCurrentTarget() != null){
					if (moving instanceof AbstractAssetTypeAttributeMapping){
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
		
		MenuItem addItem = new MenuItem(listMenu, SWT.DEFAULT);
		addItem.setText("Add Attribute");
		addItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		addItem.setEnabled(true);
		addItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addAttribute(treeAttributes, attributeList);
			}
		});
		
		MenuItem editItem = new MenuItem(listMenu, SWT.DEFAULT);
		editItem.setText(DialogConstants.EDIT_BUTTON_TEXT);
		editItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.RENAME_ICON));
		editItem.setEnabled(false);
		editItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editAttribute(treeAttributes, attributeList);	
			}
		});		
		
		MenuItem deleteItem = new MenuItem(listMenu, SWT.DEFAULT);
		deleteItem.setText(DialogConstants.DELETE_BUTTON_TEXT);
		deleteItem.setEnabled(false);
		deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeAttributes(treeAttributes, attributeList);	
			}
		});
		
		
		Composite buttonComp = new Composite(parent, SWT.NONE);
		buttonComp.setLayout(new GridLayout());
		buttonComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridLayout)buttonComp.getLayout()).marginHeight = 0;
		
		Button btnAdd = new Button(buttonComp, SWT.NONE);
		btnAdd.setText("Add Attribute");
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addAttribute(treeAttributes, attributeList);
			}
		});
		
		Button btnEdit = new Button(buttonComp, SWT.NONE);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnEdit.setEnabled(false);
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editAttribute(treeAttributes, attributeList);
			}
		});
		
		Button btnDelete = new Button(buttonComp, SWT.NONE);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnDelete.setEnabled(false);
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeAttributes(treeAttributes, attributeList);
			}
		});
		
		Label s = new Label(buttonComp, SWT.HORIZONTAL | SWT.SEPARATOR);
		s.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Button btnMoveUp = new Button(buttonComp, SWT.NONE);
		btnMoveUp.setText("Move Down");
		btnMoveUp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnMoveUp.setEnabled(false);
		btnMoveUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				moveAttribute(treeAttributes, SWT.UP, attributeList);
			}
		});
		
		Button btnMoveDown = new Button(buttonComp, SWT.NONE);
		btnMoveDown.setText("Move Up");
		btnMoveDown.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnMoveDown.setEnabled(false);
		btnMoveDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				moveAttribute(treeAttributes, SWT.DOWN, attributeList);
			}
		});
		
		treeAttributes.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				attributeSelectionModified(treeAttributes, new Widget[] {btnMoveDown, btnMoveUp, deleteItem, editItem, btnDelete, btnEdit});
			}
		});
		return treeAttributes;
		
	}
	private void moveAttribute(TableViewer viewer, int direction, List<AbstractAssetTypeAttributeMapping> attributes){
		for (Iterator<?> iterator = ((IStructuredSelection) viewer.getSelection()).iterator(); iterator.hasNext();) {
			Object toMove = iterator.next();
			if (toMove instanceof AbstractAssetTypeAttributeMapping){			
				int index = attributes.indexOf(toMove);
				if (direction == SWT.UP){
					index ++;
					if(index >= attributes.size()){
						index = attributes.size() - 1;
					}
				}else if (direction == SWT.DOWN){
					index --;
					if(index < 0) index = 0;
				}
				attributes.remove(toMove);
				attributes.add(index, (AbstractAssetTypeAttributeMapping)toMove);
			}
		}
		modified();
		viewer.refresh();
	}
	
	private void addAttribute( TableViewer viewer, List<AbstractAssetTypeAttributeMapping> attributes){
		SelectAttributeDialog dialog = new SelectAttributeDialog(getShell(), MessageFormat.format("Add attributes for asset type {0}", type.getName()));
		ContextInjectionFactory.inject(dialog, context);
		if (dialog.open() == Window.OK){
			if (attributes == assetAttributeList) {
				for (AssetAttribute ia : dialog.getSelectedAttributes()){
					AssetTypeAttribute a  = new AssetTypeAttribute();
					a.setAttribute(ia);
					a.setAssetType(type);
					if (!attributes.contains(a)) attributes.add(a);
				}
			}else if (attributes == assetDeploymentList) {
				for (AssetAttribute ia : dialog.getSelectedAttributes()){
					AssetTypeDeploymentAttribute a  = new AssetTypeDeploymentAttribute();
					a.setAttribute(ia);
					a.setAssetType(type);
					if (!attributes.contains(a)) attributes.add(a);
				}
			}
			viewer.refresh();
			modified();
		}
	}
	
	private void editAttribute( TableViewer viewer, List<AbstractAssetTypeAttributeMapping> attributes ){
		Object x = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
		if (x instanceof AbstractAssetTypeAttributeMapping){
			AbstractAssetTypeAttributeMapping attribute = (AbstractAssetTypeAttributeMapping)x;
			AttributeDialog.showAttributeDialog(getShell(), attribute.getAttribute(), context); 
			//refresh
			viewer.refresh();
			modified();
		}
	}

	
	private void removeAttributes(  TableViewer viewer, List<AbstractAssetTypeAttributeMapping> attributes ){
		IStructuredSelection items = (IStructuredSelection)viewer.getSelection();
		final List<AbstractAssetTypeAttributeMapping> toDelete = new ArrayList<>();
		
		for (Iterator<?> iterator = items.iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof AbstractAssetTypeAttributeMapping){
				toDelete.add((AbstractAssetTypeAttributeMapping)x);
			}
		}
		
		final List<String> warnings = new ArrayList<String>();
		final List<AbstractAssetTypeAttributeMapping> aToDelete = new ArrayList<>();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress(){
	
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					try(Session session = HibernateManager.openSession()){

						for (AbstractAssetTypeAttributeMapping x : toDelete){
							try{
								DeleteManager.canDelete(x, session);
								aToDelete.add(x);
							}catch (Exception ex){
								warnings.add(MessageFormat.format("The attribute {0} cannot be removed. {1}", x.getAttribute().getName(), ex.getMessage()));
							}
						}
					}
				}
				
			});
		}catch (Exception ex){
			AssetPlugIn.log(ex.getMessage(), ex);
			warnings.add(ex.getMessage());
		}
		if(!warnings.isEmpty()){
			WarningDialog wd = new WarningDialog(getShell(), "Warnings", "Cannot remove selected attributes.", warnings);
			wd.open();
		}
		
		if (aToDelete.size() > 0 ){
			StringBuilder sb = new StringBuilder();
			for (AbstractAssetTypeAttributeMapping d: aToDelete){
				sb.append(d.getAttribute().getName());
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			if (MessageDialog.openConfirm(getShell(), "Remove Attributes", MessageFormat.format("Are you sure you want to delete the attributes {0}? All attribute information associated with assets & deployments will also be removed.", sb.toString()))){
				attributes.removeAll(aToDelete);
				
			}
		}
		
		viewer.refresh();
		modified();
	}
	
	
	private void initFields(){
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
		pmd.run(true, false, new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor smonitor) throws InvocationTargetException,
					InterruptedException {
				SubMonitor monitor = SubMonitor.convert(smonitor);
				monitor.beginTask("Loading Asset Type Details", 3);
				
				monitor.worked(1);
				try(Session s = HibernateManager.openSession()){

					if (type.getUuid() != null){
						type = (AssetType) s.get(AssetType.class, type.getUuid());
						type.getNames().size();
						
						SubMonitor kid1 = monitor.newChild(1);
						kid1.beginTask("Loading Attributes...", type.getAssetAttributes().size() + type.getAssetDeploymentAttributes().size());
						for (AssetTypeAttribute a : type.getAssetAttributes()){
							a.getAttribute().getNames().size();
							monitor.subTask(MessageFormat.format("Loading {0}", a.getAttribute().getName()));
							kid1.worked(1);
						}
						for (AssetTypeDeploymentAttribute a : type.getAssetDeploymentAttributes()){
							a.getAttribute().getNames().size();
							monitor.subTask(MessageFormat.format("Loading {0}", a.getAttribute().getName()));
							kid1.worked(1);
						}
						
						
					}
					monitor.subTask("Loading Asset Type");
					assetTypeSiblings = AssetTypeManager.INSTANCE.getAssetTypes(s, SmartDB.getCurrentConservationArea());
					assetTypeSiblings.remove(type);
					monitor.worked(1);
				
				}
				
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						if (type.getIcon() != null){
							icon.setImage(type.getIcon());
						}
						assetAttributeList.addAll(type.getAssetAttributes());
						treeAttributes.setInput(assetAttributeList);
						
						assetDeploymentList.addAll(type.getAssetDeploymentAttributes());
						treeDeployAttributes.setInput(assetDeploymentList);
						
						nameKeyInfo.initFields(type, assetTypeSiblings, SmartDB.getCurrentConservationArea().getDefaultLanguage());					
						getButton(IDialogConstants.OK_ID).setEnabled(type.getUuid() == null);
						
						treeAttributes.refresh();
						treeDeployAttributes.refresh();
					}
				});
				monitor.worked(1);
				monitor.done();
			}
		});
		}catch (Exception ex){
			AssetPlugIn.displayLog(MessageFormat.format("Unable to load asset type: {0}", ex.getMessage()), ex);
		}
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
}