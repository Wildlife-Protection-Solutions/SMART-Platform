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
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Patrol attribute dialog for adding/removing custom patrol attributes.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class PatrolAttributeDialog extends SmartStyledTitleDialog implements SelectionListener{

	private TableViewer lstAttributes;
	
	private Button btnAdd, btnDelete, btnEdit, btnDisable;
	private MenuItem miAdd, miDelete, miEdit, miDisable;
	
	private List<PatrolAttribute> attributes;
	
	private boolean modified;
	
	public PatrolAttributeDialog(Shell parentShell) {
		super(parentShell);
		this.modified = false;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
	}

	@Override
	public void cancelPressed() {
		if (modified) {
			//fire change so patrol editors refresh
			PatrolEventManager.getInstance().customAttributesModified();
		}
		super.cancelPressed();
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite wrapper = new Composite(main, SWT.NONE);
		wrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lstAttributes = new TableViewer(wrapper, SWT.BORDER | SWT.V_SCROLL);
		lstAttributes.setContentProvider(ArrayContentProvider.getInstance());
		lstAttributes.setLabelProvider(new AttributeLabelProvider(IconManager.Size.SMALL));
		lstAttributes.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editAttribute();	
			}
		});
		lstAttributes.setInput(new String[] {DialogConstants.LOADING_TEXT});
		TableColumn tc = new TableColumn(lstAttributes.getTable(), SWT.NONE);
		TableColumnLayout layout = new TableColumnLayout();
		layout.setColumnData(tc, new ColumnWeightData(100));
		wrapper.setLayout(layout);
		
		Composite composite = new Composite(main, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false,1, 1));
		((GridLayout)composite.getLayout()).marginWidth = 0;
		((GridLayout)composite.getLayout()).marginHeight = 0;

		btnAdd = new Button(composite, SWT.NONE);
		btnAdd.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,1, 1));
		btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.addSelectionListener(this);
		
		btnEdit = new Button(composite, SWT.NONE);
		btnEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEdit.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1,1));
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setEnabled(false);
		btnEdit.addSelectionListener(this);
		
		btnDisable = new Button(composite, SWT.NONE);
		btnDisable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnDisable.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDisable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
		btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
		btnDisable.setEnabled(false);
		btnDisable.addSelectionListener(this);
		
		btnDelete = new Button(composite, SWT.NONE);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,false, 1, 1));
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDelete.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setEnabled(false);
		btnDelete.addSelectionListener(this);
		
		Menu mnu = new Menu(lstAttributes.getControl());
		
		miAdd = new MenuItem(mnu, SWT.PUSH);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.addSelectionListener(this);
		
		miEdit = new MenuItem(mnu, SWT.PUSH);
		miEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		miEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEdit.addSelectionListener(this);
		
		miDisable = new MenuItem(mnu, SWT.PUSH);
		miDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
		miDisable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
		miDisable.addSelectionListener(this);
		
		miDelete = new MenuItem(mnu, SWT.PUSH);
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.addSelectionListener(this);
		
		lstAttributes.getControl().setMenu(mnu);
		
		lstAttributes.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				enableButtons();
			}
		});
		
		enableButtons();
		
		setTitle(Messages.PatrolAttributeDialog_title);
		setMessage(Messages.PatrolAttributeDialog_message);
		getShell().setText(Messages.PatrolAttributeDialog_shelltitle);
		
		loadAttributesJob.schedule();
		
		return main;
	}
	
	private void addAttribute(){
		PatrolAttribute ma = new PatrolAttribute();
		ma.setType(AttributeType.LIST);
		ma.setConservationArea(SmartDB.getCurrentConservationArea());
		ma.setIsActive(true);
		EditPatrolAttributeDialog dialog = new EditPatrolAttributeDialog(getShell(), ma, attributes);
		dialog.open();
		loadAttributesJob.schedule();
		this.modified = true;
	}

	
	private void deleteAttribute(){
		PatrolAttribute ma = (PatrolAttribute)((IStructuredSelection)lstAttributes.getSelection()).getFirstElement();
		if (ma == null){
			return;
		}
		
		if (!MessageDialog.openQuestion(getParentShell(), Messages.PatrolAttributeDialog_deletedialogheader, MessageFormat.format(Messages.PatrolAttributeDialog_deletedialogmessage, new Object[]{ma.getName()}) )){
			//do not delete
			return;
		}
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				if (DeleteManager.canDelete(ma, session)){
					session.createMutationQuery("DELETE FROM PatrolAttributeValue WHERE id.patrolAttribute = :attribute") //$NON-NLS-1$
						.setParameter("attribute", ma) //$NON-NLS-1$
						.executeUpdate();
					
					session.remove(ma);
					session.flush();
				}
				session.getTransaction().commit();
			}catch (Exception ex){
				try {
					if (session.getTransaction().isActive()) session.getTransaction().rollback();
				}catch (Exception ex2) {
					SmartPatrolPlugIn.log(ex2.getMessage(), ex2);
				}
				throw ex;
			}
		}catch (Exception ex) {
			SmartPatrolPlugIn.displayLog(MessageFormat.format(Messages.PatrolAttributeDialog_deleteerror, new Object[]{ma.getName()}) + " " + ex.getMessage(), ex);  //$NON-NLS-1$
		}
		loadAttributesJob.schedule();
		this.modified = true;
	}
	
	private void editAttribute(){
		PatrolAttribute ma = (PatrolAttribute) ((IStructuredSelection)lstAttributes.getSelection()).getFirstElement();
		if (ma != null){
			EditPatrolAttributeDialog dialog = new EditPatrolAttributeDialog(getShell(), ma, attributes);
			dialog.open();
			loadAttributesJob.schedule();
			this.modified = true;
		}
	}
	
	private void enableButtons(){
		boolean isSelected = lstAttributes.getSelection().isEmpty();
		
		btnDelete.setEnabled(!isSelected);
		btnEdit.setEnabled(!isSelected);
		btnDisable.setEnabled(!isSelected);
		
		miDelete.setEnabled(!isSelected);
		miEdit.setEnabled(!isSelected);
		miDisable.setEnabled(!isSelected);
		
		if (!isSelected) {
			PatrolAttribute pa = (PatrolAttribute) lstAttributes.getStructuredSelection().getFirstElement();
			
			if (pa.getIsActive()) {
				btnDisable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
				btnDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
				btnDisable.setToolTipText(Messages.PatrolAttributeDialog_disabletooltiptext);
				miDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
				miDisable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));				
			}else {
				btnDisable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLE_ICON));
				btnDisable.setToolTipText(Messages.PatrolAttributeDialog_enabletooltiptext);
				btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
				miDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
				miDisable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLE_ICON));
			}
		}
	}
	

	@Override
	public boolean isResizable(){
		return true;
	}

	private void changeState() {
		Object x = lstAttributes.getStructuredSelection().getFirstElement();
		if (x == null || !(x instanceof PatrolAttribute)) return;
		
		PatrolAttribute ps = (PatrolAttribute)x;
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				ps.setIsActive( !ps.getIsActive() );
				HibernateManager.saveOrMerge(session, ps);
				session.getTransaction().commit();
			}catch (Exception ex) {
				try {
					if (session.getTransaction().isActive()) session.getTransaction().rollback();
				}catch (Exception ex2) {
					SmartPatrolPlugIn.log(ex2.getMessage(), ex2);
				}
				throw ex;
			}
		}catch (Exception ex) {
			SmartPatrolPlugIn.displayLog(Messages.PatrolAttributeDialog_errorupdatingstate + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
		}
		loadAttributesJob.schedule();
		this.modified = true;
	}
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.widget == btnAdd || e.widget == miAdd){
			addAttribute();
		}else if (e.widget == btnDelete || e.widget == miDelete){
			deleteAttribute();
		}else if (e.widget == btnEdit || e.widget == miEdit){
			editAttribute();
		}else if (e.widget == miDisable || e.widget == btnDisable) {
			changeState();
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}
	
	private Job loadAttributesJob = new Job(Messages.PatrolAttributeDialog_loadjobname) {

		ISelection selection;
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			Display.getDefault().syncExec(()->{
				if (lstAttributes.getControl().isDisposed()) return;
				selection = lstAttributes.getSelection();
				lstAttributes.setInput(new String[] {DialogConstants.LOADING_TEXT});
			});
			
			try(Session session = HibernateManager.openSession()){
				attributes = QueryFactory.buildQuery(session, PatrolAttribute.class, "conservationArea", SmartDB.getCurrentConservationArea()).getResultList(); //$NON-NLS-1$
				attributes.forEach(e->{
					e.getName();
					if (e.getIcon() != null) e.getIcon().getFiles().forEach(f->{
						f.getIconSet().getName();
						f.computeFileLocation(session);	
					});
				});
			}
			Collections.sort(attributes);
			
			Display.getDefault().syncExec(()->{
				if (lstAttributes.getControl().isDisposed()) return;
				((AttributeLabelProvider)lstAttributes.getLabelProvider()).clearImageCache();
				lstAttributes.setInput(attributes);
				lstAttributes.setSelection(selection);
				lstAttributes.getControl().getParent().layout(true);
			});
			return Status.OK_STATUS;
		}
		
	};
}
