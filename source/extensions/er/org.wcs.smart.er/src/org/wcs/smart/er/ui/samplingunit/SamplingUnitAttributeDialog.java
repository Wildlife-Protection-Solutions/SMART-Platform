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
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.NamedIconItemLabelProvider;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Sampling unit attribute dialog for adding/removing sampling unit attributes.
 * 
 * @author Emily
 *
 */
public class SamplingUnitAttributeDialog extends SmartStyledTitleDialog implements SelectionListener{

	private TableViewer lstAttributes;
	private Button btnAdd;
	private Button btnDelete;
	private Button btnEdit;
	
	private MenuItem miAdd, miDelete, miEdit;
	
	private List<SamplingUnitAttribute> attributes;
	
	
	public SamplingUnitAttributeDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
	}
	
	
	protected Control createDialogArea(Composite parent) {
	
		Composite main = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite wrapper = new Composite(main, SWT.NONE);
		wrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lstAttributes = new TableViewer(wrapper, SWT.BORDER);
		lstAttributes.setContentProvider(ArrayContentProvider.getInstance());
		lstAttributes.setLabelProvider(new NamedIconItemLabelProvider(IconManager.Size.MEDIUM));
		lstAttributes.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editAttribute();	
			}
		});
		
		TableColumn tc = new TableColumn(lstAttributes.getTable(), SWT.NONE);
		TableColumnLayout layout = new TableColumnLayout();
		layout.setColumnData(tc, new ColumnWeightData(100));
		wrapper.setLayout(layout);
		
		Composite btnComp = new Composite(main, SWT.NONE);
		btnComp.setLayout(new GridLayout(1, false));
		btnComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		btnAdd = new Button(btnComp, SWT.PUSH);
		btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAdd.setBackground(btnComp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnAdd.addSelectionListener(this);
		
		btnEdit = new Button(btnComp, SWT.PUSH);
		btnEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEdit.setBackground(btnComp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnEdit.addSelectionListener(this);
		
		btnDelete = new Button(btnComp, SWT.PUSH);
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDelete.setBackground(btnComp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
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
		
		initData();
		enableButtons();
		
		setTitle(Messages.SamplingUnitAttributeDialog_Title);
		setMessage(Messages.SamplingUnitAttributeDialog_Message);
		getShell().setText(Messages.SamplingUnitAttributeDialog_Title);
		return main;
	}
	
	private void addAttribute(){
		
		SamplingUnitAttribute ma = new SamplingUnitAttribute();
		ma.setType(AttributeType.TEXT);
		ma.setConservationArea(SmartDB.getCurrentConservationArea());
		ma.setAttributeList(new ArrayList<>());
		
		EditSamplingUnitAttributeDialog dialog = new EditSamplingUnitAttributeDialog(
				getShell(), ma, attributes);
		dialog.open();
		initData();		
	}

	private void deleteAttribute(){
		SamplingUnitAttribute ma = (SamplingUnitAttribute)((IStructuredSelection)lstAttributes.getSelection()).getFirstElement();
		if (ma == null){
			return;
		}
		
		if (!MessageDialog.openQuestion(getParentShell(), Messages.SamplingUnitAttributeDialog_DeleteDialogTitle, MessageFormat.format(Messages.SamplingUnitAttributeDialog_ConfirmDelete, new Object[]{ma.getName()}) )){
			//do not delete
			return;
		}
		try(Session session = HibernateManager.openSession()){
			if (DeleteManager.canDelete(ma, session)){
				session.beginTransaction();
				try {
					session.remove(ma);
					session.getTransaction().commit();
				}catch(Exception ex){
					session.getTransaction().rollback();
					throw ex;
				}
				
				attributes = QueryFactory.buildQuery(session, SamplingUnitAttribute.class, 
						"conservationArea", SmartDB.getCurrentConservationArea()).getResultList(); //$NON-NLS-1$
				Collections.sort(attributes);
				lstAttributes.setInput(attributes);

			}
		}catch (Exception ex){
			MessageDialog.openError(getShell(), Messages.SamplingUnitAttributeDialog_DeleteDialogTitle, MessageFormat.format(Messages.SamplingUnitAttributeDialog_DeleteError, new Object[]{ma.getName()}) + "\n" + ex.getMessage()); //$NON-NLS-1$
		}
	}
	
	private void editAttribute(){
		SamplingUnitAttribute ma = (SamplingUnitAttribute) ((IStructuredSelection)lstAttributes.getSelection()).getFirstElement();
		if (ma != null){
			EditSamplingUnitAttributeDialog dialog = new EditSamplingUnitAttributeDialog(getShell(), ma, attributes);
			dialog.open();
			initData();
		}
	}
	
	
	private void enableButtons(){
		boolean isSelected = lstAttributes.getSelection().isEmpty();
		
		btnDelete.setEnabled(!isSelected);
		btnEdit.setEnabled(!isSelected);
		
		miDelete.setEnabled(!isSelected);
		miEdit.setEnabled(!isSelected);
	}
	
	private void initData(){
		try(Session session = HibernateManager.openSession()){
			attributes = QueryFactory.buildQuery(session, SamplingUnitAttribute.class, 
					"conservationArea", SmartDB.getCurrentConservationArea()).getResultList(); //$NON-NLS-1$
			Collections.sort(attributes);
			lstAttributes.setInput(attributes);
			
			((NamedIconItemLabelProvider)lstAttributes.getLabelProvider()).clearCachedImages();			
			lstAttributes.refresh();
		}
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.widget == btnAdd || e.widget == miAdd){
			addAttribute();
		}else if (e.widget == btnDelete || e.widget == miDelete){
			deleteAttribute();
		}else if (e.widget == btnEdit || e.widget == miEdit){
			editAttribute();
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}
}
