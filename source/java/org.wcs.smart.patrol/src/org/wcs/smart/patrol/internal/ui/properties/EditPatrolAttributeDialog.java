/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.patrol.internal.ui.properties;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributeListItem;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.ca.properties.AttributeItemDialog;
import org.wcs.smart.ui.ca.properties.NameKeyComposite;
import org.wcs.smart.ui.ca.properties.NameKeyComposite.IChangeListener;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for editing custom patrol attributes.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class EditPatrolAttributeDialog extends SmartStyledTitleDialog implements SelectionListener{
	
	
	private Collection<? extends NamedKeyItem> siblings;	//attributes that are siblings to the current attribute being updated/created
	
	private NameKeyComposite nameKeyControls;
	
	private ComboViewer cmbType;
	private TableViewer lstViewer;
	
	private ToolItem tiAdd, tiEdit, tiDelete, tiUp, tiDown;
	
	private Composite listPanel;
	
	private PatrolAttribute pAttribute;
	
	private List<PatrolAttributeListItem> deleted;
	
	private boolean isDirty = false;

	/**
	 * creates a new dialog
	 * 
	 * @param parentShell
	 * @param toUpdate Attribute to update
	 * @param siblings Sibling attributes to the attribute being updated
	 * @param defaultLang the current language being modified
	 * @param currentSession active hibernate session
	 */
	public EditPatrolAttributeDialog(Shell parentShell,  
			PatrolAttribute toUpdate,  
			Collection<? extends NamedKeyItem> siblings) {
		
		super(parentShell);
		deleted = new ArrayList<>();
		this.pAttribute = toUpdate;
		
		this.siblings = new ArrayList<NamedKeyItem>(siblings);
		this.siblings.remove(toUpdate);
	}
	
	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		if (p.x > 600){
			p.x = 600;
		}
		return p;
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
		lblNewLabel_2.setText(Messages.EditPatrolAttributeDialog_typelabel);
		
		cmbType = new ComboViewer(composite, SWT.SIMPLE | SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbType.setContentProvider(ArrayContentProvider.getInstance());
		cmbType.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				return ((Attribute.AttributeType)element).getName(Locale.getDefault());
			}
		});
		cmbType.setInput(new Object[]{
				Attribute.AttributeType.NUMERIC,
				Attribute.AttributeType.TEXT,
				Attribute.AttributeType.LIST,
				Attribute.AttributeType.BOOLEAN,
				Attribute.AttributeType.DATE,
		});
		
		Combo combo = cmbType.getCombo();
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		if (pAttribute.getUuid() != null) combo.setEnabled(false);
		
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
		nameKeyControls.createControls(composite, true, pAttribute.getUuid() == null, new IChangeListener() {
			
			@Override
			public void itemModified() {
				if (lstViewer != null) {
					((AttributeLabelProvider)lstViewer.getLabelProvider()).setLanguage(nameKeyControls.getSelectedLanguage());
					lstViewer.refresh();
				}
				setDirty(true);
			}
		});				
		
		if (pAttribute.getUuid() == null || pAttribute.getType() == AttributeType.LIST) {
			listPanel = new Composite(composite, SWT.NONE);
			listPanel.setLayout(new GridLayout(2, false));
			listPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
			
			Composite wrapper = new Composite(listPanel, SWT.NONE);
			wrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			lstViewer = new TableViewer(wrapper,SWT.BORDER);
			lstViewer.setContentProvider(ArrayContentProvider.getInstance());
			lstViewer.setLabelProvider(new AttributeLabelProvider());
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
			
			TableColumn tc = new TableColumn(lstViewer.getTable(), SWT.NONE);
			TableColumnLayout layout = new TableColumnLayout();
			layout.setColumnData(tc, new ColumnWeightData(100));
			wrapper.setLayout(layout);
			
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
					if (target instanceof PatrolAttributeListItem){
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
					
					PatrolAttributeListItem target = (PatrolAttributeListItem)getCurrentTarget();
					if (target.equals(obj)){
						return false;
					}
					int index = pAttribute.getAttributeList().indexOf(obj);
					int toIndex = pAttribute.getAttributeList().indexOf(target);
					pAttribute.getAttributeList().remove(index);
					pAttribute.getAttributeList().add(toIndex, (PatrolAttributeListItem)obj);		
					reorder();
					setDirty(true);
					return true;
				}
			};
			lstViewer.addDropSupport(operations, transferTypes,dropAdapter);
			
			ToolBar bar = new ToolBar(listPanel, SWT.VERTICAL | SWT.FLAT);
			bar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
	
			tiAdd = new ToolItem(bar, SWT.PUSH);
			tiAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			tiAdd.setToolTipText(Messages.EditPatrolAttributeDialog_addlistitemtooltip);
			tiAdd.addSelectionListener(this);
			
			tiEdit = new ToolItem(bar, SWT.PUSH);
			tiEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
			tiEdit.setToolTipText(Messages.EditPatrolAttributeDialog_editlistitemtooltip);
			tiEdit.addSelectionListener(this);
			
			tiDelete = new ToolItem(bar, SWT.PUSH);
			tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			tiDelete.setToolTipText(Messages.EditPatrolAttributeDialog_deletelistitemtooltip);
			tiDelete.addSelectionListener(this);
	
			new ToolItem(bar, SWT.SEPARATOR);
			
			tiUp = new ToolItem(bar, SWT.PUSH);
			tiUp.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EXPORT_ICON));
			tiUp.setToolTipText(Messages.EditPatrolAttributeDialog_moveuptooltip);
			tiUp.addSelectionListener(this);
			
			tiDown = new ToolItem(bar, SWT.PUSH);
			tiDown.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.IMPORT_ICON));
			tiDown.setToolTipText(Messages.EditPatrolAttributeDialog_movedowntooltip);
			tiDown.addSelectionListener(this);
		}
		
		if (pAttribute.getKeyId() == null){
			getShell().setText(Messages.EditPatrolAttributeDialog_newshelltext);
			setTitle(Messages.EditPatrolAttributeDialog_newshelltitle);
			setMessage(Messages.EditPatrolAttributeDialog_newshellmessage);
		}else{
			getShell().setText(Messages.EditPatrolAttributeDialog_editshelltext);
			setMessage(Messages.EditPatrolAttributeDialog_editshellmessage);
		}

		initValues.schedule();
		
		return composite;
	}
	
	
	private void validate(){
		boolean error = nameKeyControls.validate();

		if (getButton(OK) != null){
			getButton(OK).setEnabled(isDirty && !error);
		}
	}
	
	private void configureButtons(){
		if (lstViewer == null) return;
		boolean isEnabled = lstViewer.getSelection().isEmpty();
		tiDelete.setEnabled(!isEnabled);
		tiEdit.setEnabled(!isEnabled);
		tiDown.setEnabled(!isEnabled);
		tiUp.setEnabled(!isEnabled);
	}
	
	private Attribute.AttributeType getType(){
		return (AttributeType) ((IStructuredSelection)cmbType.getSelection()).getFirstElement();
	}
	
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
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
	private boolean save(){
		nameKeyControls.updateFields(pAttribute);
		String name = pAttribute.findNameNull(SmartDB.getCurrentLanguage());
		if (name == null){
			name = pAttribute.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
		}
		pAttribute.setName(name);
		pAttribute.setType(getType());
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (PatrolAttributeListItem delete : deleted) {
					//remove any references to this item
					session.createQuery("DELETE FROM PatrolAttributeValue WHERE attributeListItem = :item") //$NON-NLS-1$
						.setParameter("item", delete) //$NON-NLS-1$
						.executeUpdate();
				}
				session.saveOrUpdate(pAttribute);
				session.getTransaction().commit();
				isDirty = false;
			}catch (Exception ex) {
				try {
					if (session.getTransaction().isActive()) session.getTransaction().rollback();
				}catch (Exception ex2) {
					SmartPatrolPlugIn.displayLog(ex2.getMessage(), ex2);
				}
				throw ex;
			}
		}catch (Exception ex) {
			SmartPatrolPlugIn.displayLog(Messages.EditPatrolAttributeDialog_saveerror + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			return false;
		}
		configureButtons();
		validate();
		return true;
		
	}
	
	@Override
	public void okPressed() {
		save();
	}
		
	@Override
	public void cancelPressed() {
		if (isDirty) {
			if (MessageDialog.openQuestion(getShell(), Messages.EditPatrolAttributeDialog_savetitle, Messages.EditPatrolAttributeDialog_savemessage)) {
				save();
			}
		}
		super.cancelPressed();
		
	}
		
	
	private void addListItem(){
		PatrolAttributeListItem item = new PatrolAttributeListItem();
		AttributeItemDialog dialog = new AttributeItemDialog(getShell(), item, pAttribute.getAttributeList(), nameKeyControls.getSelectedLanguage());
		if (dialog.open() == OK){
			item.setListOrder(pAttribute.getAttributeList().size());
			item.setAttribute(pAttribute);
			pAttribute.getAttributeList().add(item);
			lstViewer.refresh();
			setDirty(true);
		}
		
	}
	private void deleteListItem(){
		PatrolAttributeListItem mi = (PatrolAttributeListItem)((IStructuredSelection)lstViewer.getSelection()).getFirstElement();
		if (mi == null){
			return;
		}
		if (!MessageDialog.openQuestion(getShell(), Messages.EditPatrolAttributeDialog_deletetitle, MessageFormat.format(Messages.EditPatrolAttributeDialog_deletemessage, new Object[]{mi.getName()}))){
			return;
		}
		
		try(Session session = HibernateManager.openSession()){
			if (mi.getUuid() == null || DeleteManager.canDelete(mi, session)){
				pAttribute.getAttributeList().remove(mi);
				reorder();
				lstViewer.refresh();		
				setDirty(true);
				deleted.add(mi);
			}
		}catch (Exception ex){
			MessageDialog.openError(getShell(), Messages.EditPatrolAttributeDialog_deleteerrortitle, MessageFormat.format(Messages.EditPatrolAttributeDialog_deleteerrormsg, new Object[]{mi.getName()}) + "\n\n" + ex.getMessage());  //$NON-NLS-1$
		}
		
	}
	
	private void reorder(){
		int i = 0;
		for (PatrolAttributeListItem order : pAttribute.getAttributeList()){
			order.setListOrder(i++);
		}
	}
	
	private void editListItem(){
		PatrolAttributeListItem mi = (PatrolAttributeListItem)((IStructuredSelection)lstViewer.getSelection()).getFirstElement();
		if (mi == null){
			return;
		}
		List<PatrolAttributeListItem> siblings = new ArrayList<PatrolAttributeListItem>();
		siblings.addAll(pAttribute.getAttributeList());
		siblings.remove(mi);
		AttributeItemDialog dialog = new AttributeItemDialog(getShell(), mi, siblings,nameKeyControls.getSelectedLanguage());
		if (dialog.open() == OK){
			lstViewer.refresh();
			setDirty(true);
		}
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.getSource() == tiEdit){
			editListItem();
		}else if (e.getSource() == tiAdd){
			addListItem();
		}else if (e.getSource() == tiDelete){
			deleteListItem();
		}else if (e.getSource() == tiUp){
			PatrolAttributeListItem mi = (PatrolAttributeListItem)((IStructuredSelection)lstViewer.getSelection()).getFirstElement();
			if (mi == null){
				return;
			}
			int index = pAttribute.getAttributeList().indexOf(mi);
			index --;
			if (index < 0) index = 0;
			pAttribute.getAttributeList().remove(mi);
			pAttribute.getAttributeList().add(index, mi);
			reorder();
			lstViewer.refresh();
			setDirty(true);
			
		}else if (e.getSource() == tiDown){
			PatrolAttributeListItem mi = (PatrolAttributeListItem)((IStructuredSelection)lstViewer.getSelection()).getFirstElement();
			if (mi == null){
				return;
			}
			int index = pAttribute.getAttributeList().indexOf(mi);
			index ++;
			pAttribute.getAttributeList().remove(mi);
			if (index > pAttribute.getAttributeList().size()){
				pAttribute.getAttributeList().add(mi);
			}else{
				pAttribute.getAttributeList().add(index, mi);
			}
			reorder();
			lstViewer.refresh();
			setDirty(true);
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		
	}
	
	private void setDirty(boolean dirty) {
		this.isDirty = dirty;
		validate();
	}
	
	private Job initValues = new Job(Messages.EditPatrolAttributeDialog_jobname) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (pAttribute.getUuid() != null) {
				try(Session session = HibernateManager.openSession()){
					pAttribute = session.get(PatrolAttribute.class, pAttribute.getUuid());
					pAttribute.getNames().forEach(e->e.getValue());
					if (pAttribute.getAttributeList() != null) {
						pAttribute.getAttributeList().forEach(at->{
							at.getNames().forEach(n->n.getValue());
						});
					}
				}
			}
		
			Display.getDefault().syncExec(()->{
				nameKeyControls.initFields(pAttribute, siblings, SmartDB.getCurrentConservationArea().getDefaultLanguage());
				cmbType.setSelection(new StructuredSelection(pAttribute.getType()));
				
				if (pAttribute.getType() == AttributeType.LIST) {
					if (pAttribute.getAttributeList() == null) pAttribute.setAttributeList(new ArrayList<>());
					lstViewer.setInput(pAttribute.getAttributeList());	
				}
				if (pAttribute.getUuid() != null) {
					setTitle(pAttribute.getName());
				}
				
				configureButtons();
				setDirty(false);
			});
			return Status.OK_STATUS;
		}
		
	};
}
