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

import java.text.Collator;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
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
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Sampling unit attribute dialog for adding/removing sampling unit attributes.
 * 
 * @author Emily
 *
 */
public class SamplingUnitAttributeDialog extends TitleAreaDialog implements SelectionListener{

	private TableViewer lstAttributes;
	private Button btnAdd;
	private Button btnDelete;
	private Button btnEdit;
	
	private List<SamplingUnitAttribute> attributes;
	
	private Session session;
	
	public SamplingUnitAttributeDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	protected void okPressed() {
		if (saveChanges()){
			session.getTransaction().begin();
		};
		
	}
	
	private boolean saveChanges(){
		try{
			//commit changes
			session.getTransaction().commit();			
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			return true;
		}catch (Exception ex){
			EcologicalRecordsPlugIn.displayLog(Messages.SamplingUnitAttributeDialog_SaveError + " \n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			return false;
		}
	}
	
	public boolean close(){
		if (getButton(IDialogConstants.OK_ID).isEnabled()){
			if (MessageDialog.openQuestion(getShell(), Messages.SamplingUnitAttributeDialog_CloseDialogTitle, Messages.SamplingUnitAttributeDialog_ConfirmExit)){
				if (!saveChanges()){
					return false;
				}
			}
		}
		if (session.getTransaction().isActive()){
			session.getTransaction().rollback();
		}
		session.close();
		return super.close();
	}
	
	protected Control createDialogArea(Composite parent) {
		session = HibernateManager.openSession();
		session.beginTransaction();
		
		Composite main = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lstAttributes = new TableViewer(main, SWT.BORDER);
		lstAttributes.setContentProvider(ArrayContentProvider.getInstance());
		lstAttributes.setLabelProvider(new SamplingUnitAttributeLabelProvider());
		lstAttributes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstAttributes.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editAttribute();	
			}
		});
		lstAttributes.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite btnComp = new Composite(main, SWT.NONE);
		btnComp.setLayout(new GridLayout(1, false));
		btnComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		btnAdd = new Button(btnComp, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnAdd.addSelectionListener(this);
		
		btnDelete = new Button(btnComp, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnDelete.addSelectionListener(this);
		
		btnEdit = new Button(btnComp, SWT.PUSH);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnEdit.addSelectionListener(this);
		
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
		
		EditSamplingUnitAttributeDialog dialog = new EditSamplingUnitAttributeDialog(
				getShell(), ma, attributes);
		if (dialog.open() == OK){
			attributes.add(ma);
			session.save(ma);
			sortAttributes();
			
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		}
	}

	private void sortAttributes(){
		Collections.sort(attributes, new Comparator<SamplingUnitAttribute>() {
			@Override
			public int compare(SamplingUnitAttribute o1,
					SamplingUnitAttribute o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}
		});
		lstAttributes.refresh();
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
		try{
			if (DeleteManager.canDelete(ma, session)){
				attributes.remove(ma);
				session.delete(ma);
				sortAttributes();
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
		}catch (Exception ex){
			MessageDialog.openError(getShell(), Messages.SamplingUnitAttributeDialog_DeleteDialogTitle, MessageFormat.format(Messages.SamplingUnitAttributeDialog_DeleteError, new Object[]{ma.getName()}) + " " + ex.getMessage()); //$NON-NLS-1$
		}
	}
	
	private void editAttribute(){
		SamplingUnitAttribute ma = (SamplingUnitAttribute) ((IStructuredSelection)lstAttributes.getSelection()).getFirstElement();
		if (ma != null){
			EditSamplingUnitAttributeDialog dialog = new EditSamplingUnitAttributeDialog(getShell(), ma, attributes);
			dialog.open();
			lstAttributes.refresh();
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		}
	}
	
	private void enableButtons(){
		boolean isSelected = lstAttributes.getSelection().isEmpty();
		
		btnDelete.setEnabled(!isSelected);
		btnEdit.setEnabled(!isSelected);
	}
	
	@SuppressWarnings("unchecked")
	private void initData(){
		attributes = session.createCriteria(SamplingUnitAttribute.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.list(); 
		lstAttributes.setInput(attributes);
		sortAttributes();
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.widget == btnAdd){
			addAttribute();
		}else if (e.widget == btnDelete){
			deleteAttribute();
		}else if (e.widget == btnEdit){
			editAttribute();
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}
}
