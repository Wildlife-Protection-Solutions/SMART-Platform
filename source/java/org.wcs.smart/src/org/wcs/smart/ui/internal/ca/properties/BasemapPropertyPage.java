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

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Query;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Property page for displaying basemaps, picking a deafult
 * and deleting basemaps.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class BasemapPropertyPage extends AbstractPropertyJHeaderDialog {
	
	// buttons for modifying layers; these are ordered by Area.AreaType.values()
	private ListViewer lstBasemaps;
	private BasemapDefinition[] basemaps;
	
	public BasemapPropertyPage() {
		super(Display.getCurrent().getActiveShell(),
				Messages.BasemapPropertyPage_Dialog_Title);

	}

	@Override
	public boolean close(){
		if (changesMade){
			if (!validateSave()){
				return false;
			}
		}
		changesMade = false;
		getSession().getTransaction().rollback();
		return super.close();  
	}

	/**
	 * updates the udig service
	 * and determines which of the layers have features (are set) and which are underfined (not set) 
	 * 
	 * @param updated if the udig service needs to be reset; otherwise the existing service will be used
	 */
	private void loadData() {
		getSession().beginTransaction();
		List<?> data = HibernateManager.getBasemaps(getSession());
		basemaps = data.toArray(new BasemapDefinition[data.size()]);
		lstBasemaps.setInput(basemaps);
		if (basemaps.length > 0){
			lstBasemaps.setSelection(new StructuredSelection(basemaps[0]));
		}
	}

	/**
	 * @see
	 * org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent
	 * (org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		setMessage(Messages.BasemapPropertyPage_Dialog_Message);

		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));

		lstBasemaps = new ListViewer(comp, SWT.SINGLE | SWT.DEFAULT | SWT.BORDER);
		lstBasemaps.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstBasemaps.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof BasemapDefinition){
					if (((BasemapDefinition) element).getIsDefault()){
						return ((BasemapDefinition) element).getName() + Messages.BasemapPropertyPage_DefaultLabel;
					}else{
						return ((BasemapDefinition) element).getName();
					}
				}
				return super.getText(element);
			}
		});
		lstBasemaps.setContentProvider(ArrayContentProvider.getInstance());
		lstBasemaps.setInput(new String[]{Messages.BasemapPropertyPage_Progress_Loading});
		
		
		Composite compButtons = new Composite(comp, SWT.NONE);
		compButtons.setLayout(new GridLayout(1, false));
		compButtons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		Button btnSetDefault = new Button(compButtons, SWT.PUSH);
		btnSetDefault.setText(Messages.BasemapPropertyPage_SetDefaultButton);
		btnSetDefault.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnSetDefault.addSelectionListener(new SelectionAdapter() {		
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = (IStructuredSelection) lstBasemaps.getSelection();
				if (sel.isEmpty() ) return;
				
				BasemapDefinition newDefault = (BasemapDefinition) sel.getFirstElement();
				for (int i = 0; i < basemaps.length; i ++){
					if (basemaps[i].getIsDefault() && !basemaps[i].equals(newDefault)){
						basemaps[i].setIsDefault(false);
						lstBasemaps.refresh(basemaps[i]);
					}
				}
				newDefault.setIsDefault(true);
				setChangesMade(true);
				lstBasemaps.refresh(newDefault);
			}
		});
		
		Button btnDelete = new Button(compButtons, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnDelete.addSelectionListener(new SelectionAdapter() {		
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = (IStructuredSelection) lstBasemaps.getSelection();
				if (sel.isEmpty() ) return;
				BasemapDefinition toDelete = (BasemapDefinition) sel.getFirstElement();
				
				getSession().delete(toDelete);
				BasemapDefinition newlist[] = new BasemapDefinition[basemaps.length-1];
				int j = 0;
				for (int i = 0; i < basemaps.length; i ++){
					if (!basemaps[i].equals(toDelete)){
						newlist[j++]=basemaps[i];
					}
				}
				basemaps = newlist;
				lstBasemaps.setInput(basemaps);
				setChangesMade(true);
			}
		});
		
		loadData();
		return comp;
	}

	
	/*
	 * @see
	 * org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#performSave
	 * ()
	 */
	@Override
	protected boolean performSave() {
		try{
			getSession().getTransaction().commit();
			setChangesMade(false);
		}catch (Exception ex){
			SmartPlugIn.displayLog(getShell(), Messages.BasemapPropertyPage_Error_CouldNotSave + ex.getMessage(), ex);
			return false;
		}
		getSession().beginTransaction();
		return true;
	}
}
