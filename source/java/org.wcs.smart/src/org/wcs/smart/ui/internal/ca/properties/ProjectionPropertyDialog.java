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
package org.wcs.smart.ui.internal.ca.properties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.geotools.referencing.CRS;
import org.hibernate.Transaction;
import org.locationtech.udig.ui.CRSChooserDialog;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for modifying conservation area projection options.
 * @author egouge
 *
 */
public class ProjectionPropertyDialog extends AbstractPropertyJHeaderDialog implements SelectionListener{

	private ListViewer lstViewer;
	private List<Projection> projections;
	
	private Button btnAdd;
	private Button btnRemove;
	private Button btnEdit;
	private Button btnDefault;
	
	private Transaction currentTransaction = null;
	
	public ProjectionPropertyDialog(Shell parent) {
		super(parent, Messages.ProjectionPropertyDialog_Dialog_Title);
		
	}

	/**
	 * Starts a new transaction and opens the dialog
	 */
	@Override
	public int open(){
		currentTransaction = getSession().beginTransaction();
		return super.open();
	}
	
	@Override
	protected Composite createContent(Composite parent) {
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.ProjectionPropertyDialog_ProjectionList_Label);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		
		lstViewer = new ListViewer(main, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		lstViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)lstViewer.getList().getLayoutData()).widthHint = 350;
		((GridData)lstViewer.getList().getLayoutData()).heightHint = 100;
		lstViewer.setContentProvider(ArrayContentProvider.getInstance());
		lstViewer.setLabelProvider(new LabelProvider(){
			
			@Override
			public String getText(Object element){
				if (element instanceof Projection){
					String text = ((Projection)element).getName();
					if (((Projection) element).getIsDefault()){
						text += Messages.ProjectionPropertyDialog_DefaultProjection_text;
					}
					return text;
				}
				return super.getText(element);
			}
		});
		lstViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean enabled = false;
				if (!lstViewer.getSelection().isEmpty()){
					enabled = Projection.class.isAssignableFrom( ((IStructuredSelection)lstViewer.getSelection()).getFirstElement().getClass()) ;
				}
				btnEdit.setEnabled(enabled);
				btnRemove.setEnabled(enabled);
				btnDefault.setEnabled(enabled);
			}
		});
		lstViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editSelected();
			}
		});
		projections = new ArrayList<Projection>(HibernateManager.getCaProjectionList(getSession()));
		lstViewer.setInput(projections);
		
		Composite buttonPnl = new Composite(main, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		gl.marginHeight = gl.marginTop = gl.marginBottom = 0;
		buttonPnl.setLayout(gl);
		buttonPnl.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnAdd = new Button(buttonPnl, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnAdd.addSelectionListener(this);
		
		btnRemove = new Button(buttonPnl, SWT.PUSH);
		btnRemove.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnRemove.addSelectionListener(this);
		btnRemove.setEnabled(false);
		
		btnEdit = new Button(buttonPnl, SWT.PUSH);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnEdit.addSelectionListener(this);
		btnEdit.setEnabled(false);
		
		btnDefault = new Button(buttonPnl, SWT.PUSH);
		btnDefault.setText(Messages.ProjectionPropertyDialog_SetDefault_Button);
		btnDefault.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnDefault.addSelectionListener(this);
		btnDefault.setEnabled(false);
		
		getShell().setText(Messages.ProjectionPropertyDialog_Dialog_Name);
		setTitle(Messages.ProjectionPropertyDialog_PageTitle);
		setMessage(Messages.ProjectionPropertyDialog_Dialog_Message);
		return main;
	}

	@Override
	protected boolean performSave() {
		try{
			currentTransaction.commit();
		}catch (Exception ex){
			SmartPlugIn.displayLog(Messages.ProjectionPropertyDialog_Error_CouldNotSave + ex.getLocalizedMessage(), ex);
			return false;
		}
		ConservationAreaManager.getInstance().fireProjectionListModified();
		currentTransaction = getSession().beginTransaction();
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		super.setChangesMade(false);
		return true;
	}
	
	private void listModified(){
		super.setChangesMade(true);
		boolean enabled = true;
		if (projections.size() == 0){
			setErrorMessage(Messages.ProjectionPropertyDialog_Error_AtLeastOneProjectionRequired);
			enabled = false;
		}else{
			setErrorMessage(null);
		}
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);		
		lstViewer.refresh();
		
	}
	
	
	private void add(){
		CRSChooserDialog dialog = new CRSChooserDialog(getShell(), null);
		if (dialog.open() == IDialogConstants.OK_ID){
			CoordinateReferenceSystem crs = dialog.getResult();
			if (crs != null){
				Projection prj = new Projection();
				prj.setConservationArea(SmartDB.getCurrentConservationArea());
				String code = Messages.ProjectionPropertyDialog_UnknownCode;
				try{
					code = CRS.lookupIdentifier(crs.getName().getAuthority(), crs, true);
				}catch (Exception ex){
				
				}
				prj.setName(crs.getName().getCode() + " [" + crs.getName().getCodeSpace() + ": " + code + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				prj.setDefinition(crs.toWKT());
				getSession().save(prj);
				projections.add(prj);
				listModified();
			}else{
				SmartPlugIn.displayLog(Messages.ProjectionPropertyDialog_ProjectionParseError,null);				
			}
		}
	}
	
	private void removeSelected(){
		if (!MessageDialog.openConfirm(getShell(), Messages.ProjectionPropertyDialog_ConfirmDeleteTitle, Messages.ProjectionPropertyDialog_ConfirmDeleteMessage)){
			return;
		}
		IStructuredSelection selection= (IStructuredSelection) lstViewer.getSelection();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object type = (Object) iterator.next();
			if (Projection.class.isAssignableFrom(type.getClass())){
				Projection p = (Projection)type;
				projections.remove(p);
				getSession().delete(p);
			}
		}
		listModified();
	}
	
	private void editSelected(){
		IStructuredSelection selection= (IStructuredSelection) lstViewer.getSelection();
		if (selection.isEmpty()){
			return;
		}
		Object element = selection.getFirstElement();
		if (Projection.class.isAssignableFrom(element.getClass())){
			Projection toEdit = (Projection) element;
			EditProjectionDialog di = new EditProjectionDialog(getShell(), toEdit);
			if (di.open() == IDialogConstants.OK_ID){
				lstViewer.refresh(toEdit);
				listModified();
			}
		}
		
	}

	private void setDefault(){
		IStructuredSelection selection= (IStructuredSelection) lstViewer.getSelection();
		if (selection.isEmpty()){
			return;
		}
		for (Iterator<?> iterator = projections.iterator(); iterator.hasNext();) {
			Projection p = (Projection) iterator.next();
			p.setIsDefault(false);
		}
		
		Object element = selection.getFirstElement();
		if (Projection.class.isAssignableFrom(element.getClass())){
			((Projection)element).setIsDefault(true);
		}
		lstViewer.refresh();
		listModified();
	}
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.widget == btnRemove){
			removeSelected();
		}else if (e.widget == btnAdd){
			add();
		}else if (e.widget == btnEdit){
			editSelected();
		}else if (e.widget == btnDefault){
			setDefault();
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {		
	}

}

