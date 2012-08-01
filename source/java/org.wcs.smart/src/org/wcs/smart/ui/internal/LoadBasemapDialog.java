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
package org.wcs.smart.ui.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Dialog for selecting basemap
 * 
 * @author egouge
 * @since 1.0.0
 */
public class LoadBasemapDialog extends TitleAreaDialog {

	private ListViewer lstBasemaps;
	private BasemapDefinition baseMap;
	private Button chUseDefault;
	
	private boolean setDefault = false;
	/**
	 * @param parent
	 *            the parent shell
	 */
	public LoadBasemapDialog(Shell parent) {
		super(parent);	
		
		
	}
	
	@Override
	protected void okPressed() {
		super.okPressed();
		if (setDefault){
			SmartPlugIn.getDefault().setBasemapSelection(baseMap);
		}
	}
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Load", true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		validate();
	}

	/**
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createDialogArea(Composite parent) {
		getShell().setText("Select Basemap");

		setMessage("Select the map to use as the basemap");

		Composite main = new Composite(parent, SWT.NONE);

		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Listener validateListener = new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				validate();				
			}
		};
		
		lstBasemaps = new ListViewer(main, SWT.DEFAULT | SWT.BORDER | SWT.SINGLE );
		lstBasemaps.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof BasemapDefinition){
					BasemapDefinition def=SmartPlugIn.getDefault().getBasemapSelection();
					if (def != null && def.equals(element)){					
						return ((BasemapDefinition)element).getName() + "  [Session Default]";
					}else{
						return ((BasemapDefinition)element).getName();	
					}
				}
				return super.getText(element);
			}
		});
		lstBasemaps.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstBasemaps.setContentProvider(ArrayContentProvider.getInstance());
		lstBasemaps.setInput(new String[]{"Loading"});
		lstBasemaps.getList().addListener(SWT.Selection, validateListener);
	
		chUseDefault = new Button(main, SWT.CHECK);
		chUseDefault.setText("Use as session default");
		chUseDefault.setSelection(false);
		
		
		loadData();
		return main;
	}
	
	private void loadData(){
		Job loadData = new Job("LoadData"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Object[] data = null;
				
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					String query = "FROM BasemapDefinition WHERE conservationArea = :ca and (isShared = 'true' or (isShared = 'false' and employee = :em))";
					Query q = s.createQuery(query);
					q.setParameter("ca", SmartDB.getCurrentConservationArea());
					q.setParameter("em", SmartDB.getCurrentEmployee());
					data = q.list().toArray();
				}finally{
					if (s.getTransaction().isActive()){
						s.getTransaction().commit();		
					}
					s.close();
				}
				final Object[] data1 = data;
				Display.getDefault().asyncExec(new Runnable(){
					@Override
					public void run() {
						if (lstBasemaps.getList().isDisposed()) return;
						lstBasemaps.setInput(data1);
						if (data1 != null && data1.length > 0){
							lstBasemaps.setSelection(new StructuredSelection(data1[0]));
						}
						validate();
					}});
				return Status.OK_STATUS;
			}};
			loadData.schedule();
	}


	/*
	 * Validate the user input
	 */
	private void validate() {
		boolean ok = true;
		setErrorMessage(null);
		IStructuredSelection sel = (IStructuredSelection) lstBasemaps.getSelection();
		if (sel.isEmpty() ){
			setErrorMessage("Basemap must be selected");
			ok = false;
		}
		baseMap = (BasemapDefinition) sel.getFirstElement();
		
		
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null){
			btn.setEnabled(ok);
		}
		setDefault = chUseDefault.getSelection();
	}
	
	public BasemapDefinition getBasemap(){
		return this.baseMap;
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 * @return <code>true</code>
	 */
	@Override
	public boolean isResizable() {
		return true;
	}
}
