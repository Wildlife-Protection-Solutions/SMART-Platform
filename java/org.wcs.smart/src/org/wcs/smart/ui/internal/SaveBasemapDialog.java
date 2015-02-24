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

import java.text.Collator;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.ca.Language;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.BasemapLabelProvider;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.util.SmartUtils;


/**
 * Dialog for saving basemaps.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SaveBasemapDialog  extends TitleAreaDialog {

	private Button btnOverwrite;
	private Button btnCreateNew;
	private Text txtName;
	private ListViewer lstBasemaps;
	
	private Label lblOverwrite;
	private Label lblCreateNew;
	
	private BasemapDefinition baseMap;
	private BasemapDefinition newBasemap;
	private Language defaultLanguage;
	
	/**
	 * @param parent
	 *            the parent shell
	 */
	public SaveBasemapDialog(Shell parent) {
		super(parent);
		
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, Messages.SaveBasemapDialog_SaveButton, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		validate();
	}

	/**
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		getShell().setText(Messages.SaveBasemapDialog_Title);
		setTitle(Messages.SaveBasemapDialog_Title);
		setMessage(Messages.SaveBasemapDialog_Message);

		Composite main = new Composite(parent, SWT.NONE);

		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		SelectionAdapter enableListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				enableElements();
			}
		};
		Listener validateListener = new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				validate();				
			}
		};
		
		btnCreateNew = new Button(main, SWT.RADIO);
		btnCreateNew.setSelection(false);
		btnCreateNew.setText(Messages.SaveBasemapDialog_CreateNewOp);
		btnCreateNew.addSelectionListener(enableListener);
		
		Composite compNew = new Composite(main, SWT.NONE);
		compNew.setLayout(new GridLayout(3, false));
		lblCreateNew = new Label(compNew, SWT.NONE);
		defaultLanguage = SmartDB.getCurrentConservationArea().getDefaultLanguage();
		if(defaultLanguage == null){
			defaultLanguage = SmartDB.getCurrentLanguage();
		}
		lblCreateNew.setText(Messages.SaveBasemapDialog_NameLabel + " [" + defaultLanguage.getCode() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
		lblCreateNew.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		txtName = new Text(compNew, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtName.addListener(SWT.Modify, validateListener);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalIndent = 20;
		compNew.setLayoutData(gd);

		Button btnTranslate = new Button(compNew, SWT.PUSH);
		btnTranslate.setText(Messages.SaveBasemapDialog_Button_Translate);
		btnTranslate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnTranslate.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				BasemapDefinition tmp = getNewBasemap();
				tmp.updateName(defaultLanguage, txtName.getText());
				
				TranslateSimpleListItemDialog d = new TranslateSimpleListItemDialog(getShell(), tmp);
				if (d.open() == TranslateSimpleListItemDialog.OK){
					txtName.setText(baseMap.findName(defaultLanguage));
				}
						
			}
		});
		
		
		btnOverwrite = new Button(main, SWT.RADIO);
		btnOverwrite.setSelection(false);
		btnOverwrite.setText(Messages.SaveBasemapDialog_OverwriteOp);
		btnOverwrite.addSelectionListener(enableListener);
		
		Composite compList = new Composite(main, SWT.NONE);
		compList.setLayout(new GridLayout(2, false));
		lblOverwrite = new Label(compList, SWT.NONE);
		lblOverwrite.setText(Messages.SaveBasemapDialog_OverwiteLabel);
		lblOverwrite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		lstBasemaps = new ListViewer(compList, SWT.DEFAULT | SWT.BORDER | SWT.SINGLE );
		lstBasemaps.setLabelProvider(new BasemapLabelProvider());
		lstBasemaps.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstBasemaps.setContentProvider(ArrayContentProvider.getInstance());
		lstBasemaps.setInput(new String[]{Messages.SaveBasemapDialog_Loading});
		lstBasemaps.getList().addListener(SWT.Selection, validateListener);
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalIndent = 20;
		gd.heightHint = 100;
		compList.setLayoutData(gd);
		
		
		btnCreateNew.setSelection(true);
		btnOverwrite.setSelection(false);
		enableElements();
		
		loadData();
		return main;
	}
	
	private BasemapDefinition getNewBasemap(){
		if (newBasemap == null){
			newBasemap = new BasemapDefinition();
			newBasemap.setConservationArea(SmartDB.getCurrentConservationArea());
			newBasemap.setName(txtName.getText());
			newBasemap.setIsDefault(false);
		}
		return newBasemap;
	}
	private void loadData(){
		Job loadData = new Job(Messages.SaveBasemapDialog_LoadDataJob){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Object[] data = null;
				
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					data = HibernateManager.getBasemaps(s).toArray();
				}finally{
					if (s.getTransaction().isActive()){
						s.getTransaction().commit();		
					}
					s.close();
				}
				Arrays.sort(data, new Comparator<Object>(){
					@Override
					public int compare(Object o1, Object o2) {
						return Collator.getInstance().compare(
								((BasemapDefinition)o1).getName(), 
								((BasemapDefinition)o2).getName());
					}});
				final Object[] data1 = data;
				Display.getDefault().asyncExec(new Runnable(){
					@Override
					public void run() {
						lstBasemaps.setInput(data1);
					}});
				return Status.OK_STATUS;
			}};
			loadData.schedule();
	}
	
	private void enableElements(){
		boolean enabled = btnCreateNew.getSelection();
		txtName.setEnabled(enabled);
		lblCreateNew.setEnabled(enabled);
		
		lstBasemaps.getList().setEnabled(!enabled);
		lblOverwrite.setEnabled(!enabled);
		
		validate();
	}

	/*
	 * Validate the user input
	 */
	private void validate() {
		boolean ok = true;
		setErrorMessage(null);
		if(btnOverwrite.getSelection()){
			
			IStructuredSelection sel = (IStructuredSelection) lstBasemaps.getSelection();
			if (sel.isEmpty() ){
				setErrorMessage(Messages.SaveBasemapDialog_Error_NoBasemap);
				ok = false;
			}
			baseMap = (BasemapDefinition) sel.getFirstElement();
		}else{
			String name = txtName.getText();
			if (name.trim().length() == 0){
				setErrorMessage(Messages.SaveBasemapDialog_Error_NoName);
				ok = false;
			}
			if (!SmartUtils.isSimpleString(name, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, org.wcs.smart.ca.Label.MAX_LENGTH )){
				setErrorMessage(
						MessageFormat.format(
								Messages.SaveBasemapDialog_Error_BasemapName,
								new Object[]{SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc, org.wcs.smart.ca.Label.MAX_LENGTH }));
				ok = false;
			}
			baseMap = getNewBasemap();
			getNewBasemap().updateName(defaultLanguage, name);
			baseMap.setName(name);
		}
		
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null){
			btn.setEnabled(ok);
		}
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

