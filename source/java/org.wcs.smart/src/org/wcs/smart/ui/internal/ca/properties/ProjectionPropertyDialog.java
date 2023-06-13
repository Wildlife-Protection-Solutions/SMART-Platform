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
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
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
	private List<Projection> projectionsToDelete;
	
	private MenuItem miAdd, miDelete, miEdit;
	private ToolItem tiAdd, tiDelete, tiEdit;
	
	private ComboViewer projectionViewer = null;
	
	public ProjectionPropertyDialog(Shell parent) {
		super(parent, Messages.ProjectionPropertyDialog_Dialog_Title);
		projectionsToDelete = new ArrayList<>();
	}

	
	@Override
	protected Composite createContent(Composite parent) {
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.ProjectionPropertyDialog_ProjectionList_Label);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
		ToolBar tb = new ToolBar(main,SWT.HORIZONTAL | SWT.FLAT);
		tb.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		
		tiAdd = new ToolItem(tb, SWT.PUSH);
		tiAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		tiAdd.setToolTipText(DialogConstants.ADD_BUTTON_TEXT);
		tiAdd.addSelectionListener(this);
		
		tiEdit = new ToolItem(tb, SWT.PUSH);
		tiEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		tiEdit.setToolTipText(DialogConstants.EDIT_BUTTON_TEXT);
		tiEdit.addSelectionListener(this);
		tiEdit.setEnabled(false);
		
		tiDelete = new ToolItem(tb, SWT.PUSH);
		tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiDelete.setToolTipText(DialogConstants.DELETE_BUTTON_TEXT);
		tiDelete.addSelectionListener(this);
		tiDelete.setEnabled(false);
		
		
		lstViewer = new ListViewer(main, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		lstViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		((GridData)lstViewer.getList().getLayoutData()).widthHint = 350;
		((GridData)lstViewer.getList().getLayoutData()).heightHint = 100;
		lstViewer.setContentProvider(ArrayContentProvider.getInstance());
		LabelProvider prjLabelProvider = new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof Projection){
					return ((Projection)element).getName();
				}
				return super.getText(element);
			}
		};
		lstViewer.setLabelProvider(prjLabelProvider);
		lstViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean enabled = false;
				if (!lstViewer.getSelection().isEmpty()){
					enabled = Projection.class.isAssignableFrom( ((IStructuredSelection)lstViewer.getSelection()).getFirstElement().getClass()) ;
				}
				tiEdit.setEnabled(enabled);
				tiDelete.setEnabled(enabled);
				miEdit.setEnabled(enabled);
				miDelete.setEnabled(enabled);
			}
		});
		lstViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editSelected();
			}
		});
		
		Menu mnu = new Menu(lstViewer.getControl());
		
		miAdd = new MenuItem(mnu, SWT.PUSH);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.addSelectionListener(this);
		
		miEdit = new MenuItem(mnu, SWT.PUSH);
		miEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		miEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEdit.addSelectionListener(this);
		miEdit.setEnabled(false);
		
		miDelete = new MenuItem(mnu, SWT.PUSH);
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.addSelectionListener(this);
		miDelete.setEnabled(false);
		
		lstViewer.getControl().setMenu(mnu);
		
		
		
		Composite prjComp = new Composite(main, SWT.NONE);
		GridLayout pgl = new GridLayout(2, false);
		pgl.marginWidth = 0;
		pgl.marginHeight = 10;
		prjComp.setLayout(pgl);
		prjComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		lbl = new Label(prjComp, SWT.NONE);
		lbl.setText(Messages.ProjectionPropertyDialog_ViewProjectionLbl1);
		lbl.setToolTipText(Messages.ProjectionPropertyDialog_ViewProjectionToolip1);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		projectionViewer = new ComboViewer(prjComp, SWT.READ_ONLY);
		projectionViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		projectionViewer.setLabelProvider(prjLabelProvider);
		
		projectionViewer.setContentProvider(ArrayContentProvider.getInstance());
		projectionViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				StructuredSelection sel = ((StructuredSelection)projectionViewer.getSelection());
				if (!sel.isEmpty()){
					Projection prj = (Projection)sel.getFirstElement();
					if (!prj.getIsDefault()){
						for (Projection p : projections){
							if (!p.equals(prj) && p.getIsDefault()){
								p.setIsDefault(false);
							}
						}
						prj.setIsDefault(true);
						ProjectionPropertyDialog.this.setChangesMade(true);
					}
				}
			}
		});
		
		//init data
		try(Session s = HibernateManager.openSession()){
			projections = new ArrayList<Projection>(HibernateManager.getCaProjectionList(s));
			lstViewer.setInput(projections);
			projectionViewer.setInput(projections);
		}
		for (Projection p : projections){
			if (p.getIsDefault()){
				projectionViewer.setSelection(new StructuredSelection(p));
				break;
			}
		}
		
		getShell().setText(Messages.ProjectionPropertyDialog_Dialog_Name);
		setTitle(Messages.ProjectionPropertyDialog_PageTitle1);
		setMessage(Messages.ProjectionPropertyDialog_Dialog_Message);
		return main;
	}

	@Override
	protected boolean performSave() {
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try{
				projectionsToDelete.forEach(p -> s.remove(p));
				projections.forEach(p -> s.merge(p));
				s.getTransaction().commit();
				projectionsToDelete.clear();
			}catch (Exception ex){
				s.getTransaction().rollback();
				SmartPlugIn.displayLog(Messages.ProjectionPropertyDialog_Error_CouldNotSave + ex.getLocalizedMessage(), ex);
				return false;
			}
		}
		ConservationAreaManager.getInstance().fireProjectionListModified();
		
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
		projectionViewer.refresh();
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
				projectionsToDelete.add(p);
				projections.remove(p);
				if (p.getIsDefault()){
					projectionViewer.setSelection(null);
				}
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
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.widget == tiDelete || e.widget == miDelete){
			removeSelected();
		}else if (e.widget == tiAdd || e.widget == miAdd){
			add();
		}else if (e.widget == tiEdit || e.widget == miEdit){
			editSelected();
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {		
	}

}

