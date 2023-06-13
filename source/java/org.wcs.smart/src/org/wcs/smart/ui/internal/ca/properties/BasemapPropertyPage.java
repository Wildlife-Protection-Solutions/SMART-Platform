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

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.map.internal.BasemapInterceptor;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.LanguageViewer;


/**
 * Property page for displaying basemaps, picking a deafult
 * and deleting basemaps.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class BasemapPropertyPage extends AbstractPropertyJHeaderDialog {
	
	private Button btnRename, btnDefault, btnDelete;
	private MenuItem setDefault, renameItem, deleteItem;
	private LanguageViewer langViewer;
	private TableViewer lstBasemaps;
	private List<BasemapDefinition> basemaps;
	private List<BasemapDefinition> itemsToDelete;
	
	public BasemapPropertyPage(Shell parent) {
		super(parent, Messages.BasemapPropertyPage_Dialog_Title);
		itemsToDelete = new ArrayList<>();
	}

	private Session openSession(){
		return HibernateManager.openSession(new BasemapInterceptor());
	}
	
	@Override
	public boolean close(){
		if (changesMade){
			if (!validateSave()){
				return false;
			}
		}
		changesMade = false;
		return super.close();  
	}

	/**
	 * updates the udig service
	 * and determines which of the layers have features (are set) and which are underfined (not set) 
	 * 
	 * @param updated if the udig service needs to be reset; otherwise the existing service will be used
	 */
	private void loadData() {
		
		try(Session s = openSession()){
			basemaps = HibernateManager.getBasemaps(s);
			Collections.sort(basemaps, new Comparator<Object>(){
				@Override
				public int compare(Object o1, Object o2) {
					return Collator.getInstance().compare(
							((BasemapDefinition)o1).getName(), 
							((BasemapDefinition)o2).getName());
				}});
			lstBasemaps.setInput(basemaps);
			if (basemaps.size() > 0){
				lstBasemaps.setSelection(new StructuredSelection(basemaps.get(0)));
			}
		}
	}

	/**
	 * @see
	 * org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent
	 * (org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		//make sure we close any existing open session to
		//ensure the basemap interceptor session is opened
		HibernateManager.openSession().close();	
		
		setTitle(Messages.BasemapPropertyPage_PageName);
		setMessage(Messages.BasemapPropertyPage_Dialog_Message);

		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));

		Composite left = new Composite(comp, SWT.NONE);
		left.setLayout(new GridLayout(1, false));
		left.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		langViewer = new LanguageViewer(left, SWT.NONE, SmartDB.getCurrentConservationArea());
		langViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		langViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				lstBasemaps.refresh();
			}
		});
		
		Composite tableComp = new Composite(left, SWT.NONE);
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tableComp.getLayoutData()).heightHint = 50;
		((GridData)tableComp.getLayoutData()).widthHint = 100;
		
		lstBasemaps = new TableViewer(tableComp, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		lstBasemaps.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		lstBasemaps.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof BasemapDefinition){
					String name = ((BasemapDefinition) element).findNameNull(langViewer.getCurrentSelection());
					if (name == null){
						name = ((BasemapDefinition) element).getName();
					}
					if (((BasemapDefinition) element).getIsDefault()){
						name = name + " " + Messages.BasemapPropertyPage_DefaultLabel; //$NON-NLS-1$
					}
					return name;
				}
				return super.getText(element);
			}
		});
		lstBasemaps.setContentProvider(ArrayContentProvider.getInstance());
		lstBasemaps.setInput(new String[]{Messages.BasemapPropertyPage_Progress_Loading});
		lstBasemaps.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateUi();
			}
		});
		
		TableColumn singleColumn = new TableColumn(lstBasemaps.getTable(), SWT.NONE);
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		tableColumnLayout.setColumnData(singleColumn, new ColumnWeightData(100));
		tableComp.setLayout(tableColumnLayout);
		
		Menu mnu = new Menu(lstBasemaps.getControl());
				
		setDefault = new MenuItem(mnu, SWT.PUSH);
		setDefault.setText(Messages.BasemapPropertyPage_SetDefaultButton);
		setDefault.addListener(SWT.Selection, e->setdefault());

		new MenuItem(mnu, SWT.SEPARATOR);
		
		renameItem = new MenuItem(mnu, SWT.PUSH);
		renameItem.setText(Messages.BasemapPropertyPage_RenameButton);
		renameItem.addListener(SWT.Selection, e->rename());
		renameItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		
		deleteItem = new MenuItem(mnu, SWT.PUSH);
		deleteItem.setText(DialogConstants.DELETE_BUTTON_TEXT);
		deleteItem.addListener(SWT.Selection, e->delete());
		deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		
		lstBasemaps.getControl().setMenu(mnu);
		
		Composite compButtons = new Composite(comp, SWT.NONE);
		compButtons.setLayout(new GridLayout(1, false));
		compButtons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		btnDefault = new Button(compButtons, SWT.PUSH);
		btnDefault.setText(Messages.BasemapPropertyPage_SetDefaultButton);
		btnDefault.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnDefault.addListener(SWT.Selection, e->setdefault());
		btnDefault.setBackground(compButtons.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		btnRename = new Button(compButtons, SWT.PUSH);
		btnRename.setText(Messages.BasemapPropertyPage_RenameButton);
		btnRename.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnRename.addListener(SWT.Selection, e->rename());
		btnRename.setBackground(compButtons.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnRename.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		
		btnDelete = new Button(compButtons, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnDelete.addListener(SWT.Selection,e->delete());
		btnDelete.setBackground(compButtons.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		
		loadData();
		return comp;
	}
	
	private void setdefault() {
		IStructuredSelection sel = (IStructuredSelection) lstBasemaps.getSelection();
		if (sel.isEmpty() ) return;
		
		BasemapDefinition newDefault = (BasemapDefinition) sel.getFirstElement();
		for (BasemapDefinition def : basemaps){
			if (def.getIsDefault() && !def.equals(newDefault)){
				def.setIsDefault(false);
				lstBasemaps.refresh(def);
			}
		}
		newDefault.setIsDefault(true);
		setChangesMade(true);
		lstBasemaps.refresh(newDefault);
	}

	private void delete() {
		IStructuredSelection sel = (IStructuredSelection) lstBasemaps.getSelection();
		if (sel.isEmpty() ) return;
		BasemapDefinition toDelete = (BasemapDefinition) sel.getFirstElement();
		
		if (!MessageDialog.openConfirm(getShell(), Messages.BasemapPropertyPage_ConfirmDeleteTitle, MessageFormat.format(Messages.BasemapPropertyPage_ConfirmDeleteMessage, new Object[]{toDelete.getName()}))){
			return;
		}
		
		itemsToDelete.add(toDelete);
		basemaps.remove(toDelete);
		
		lstBasemaps.refresh();
		setChangesMade(true);
	}
	
	private void rename() {
		IStructuredSelection sel = (IStructuredSelection) lstBasemaps.getSelection();
		if (sel.isEmpty() ) return;
		BasemapDefinition toEdit = (BasemapDefinition) sel.getFirstElement();
		
		TranslateSimpleListItemDialog dialog = new TranslateSimpleListItemDialog(getShell(), toEdit);
		if (dialog.open() ==  Window.OK){
			setChangesMade(true);
			lstBasemaps.refresh();
		}
	}
	
	private void updateUi(){
		boolean isEmpty = lstBasemaps.getSelection().isEmpty();
		btnDefault.setEnabled(!isEmpty);
		btnDelete.setEnabled(!isEmpty);
		btnRename.setEnabled(!isEmpty);
		
		setDefault.setEnabled(!isEmpty);
		renameItem.setEnabled(!isEmpty);
		deleteItem.setEnabled(!isEmpty);
	}
	
	/*
	 * @see
	 * org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#performSave
	 * ()
	 */
	@Override
	protected boolean performSave() {
		try(Session s = openSession()){
			s.beginTransaction();
			try{
				basemaps.forEach(b -> s.merge(b));
				for (BasemapDefinition d : itemsToDelete) {
					s.remove( s.get(BasemapDefinition.class, d.getUuid()));
				}
				s.getTransaction().commit();
				setChangesMade(false);
				itemsToDelete.clear();
			}catch (Exception ex){
				s.getTransaction().rollback();
				SmartPlugIn.displayLog(Messages.BasemapPropertyPage_Error_CouldNotSave + ex.getLocalizedMessage(), ex);
				return false;
			}
		}
		return true;
	}
}
