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
package org.wcs.smart.ui.ca.datamodel;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.user.UserLevelManager;

import com.ibm.icu.text.MessageFormat;

/**
 * 
 * Dialog to allow users to merge one or more conservation area data models
 * with a data model from a file.
 * 
 * @author Emily
 *
 */
public class DataModelMergeDialog extends TitleAreaDialog{

	private CheckboxTableViewer lstCas;
	private String file;
	private List<ConservationArea> selectedConservationAreas;
	
	public DataModelMergeDialog(Shell parentShell) {
		super(parentShell);
	}
	
	public void okPressed() {
		file = txtFile.getText();
		
		if (btnCurrent.getSelection()) {
			selectedConservationAreas = Collections.singletonList(SmartDB.getCurrentConservationArea());
		}else {
			selectedConservationAreas = new ArrayList<>();
			for (Object x : lstCas.getCheckedElements()) {
				if (x instanceof ConservationArea) selectedConservationAreas.add((ConservationArea)x);
			}
		}
		super.okPressed();
	}
	
	@Override
	public void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite)super.createDialogArea(parent);
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.DataModelMergeDialog_SelectFileMsg);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite fileSelector = new Composite(main, SWT.NONE);
		fileSelector.setLayout(new GridLayout(2, false));
		((GridLayout)fileSelector.getLayout()).marginWidth = 0;
		((GridLayout)fileSelector.getLayout()).marginHeight = 0;
		fileSelector.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)fileSelector.getLayoutData()).horizontalIndent = 10;
		
		txtFile = new Text(fileSelector, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtFile.addListener(SWT.Modify, e->validate());
		
		Button btnBrowse = new Button(fileSelector, SWT.PUSH);
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnBrowse.setText("..."); //$NON-NLS-1$
		btnBrowse.addListener(SWT.Selection, e->{
			FileDialog fd = new FileDialog(getShell());
			fd.setText(txtFile.getText());
			fd.setFilterExtensions(new String[] {"*.xml", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			fd.setFilterNames(new String[] {Messages.DataModelMergeDialog_xmlfilelbl, Messages.DataModelMergeDialog_allfileslbl});
			String newFile = fd.open();
			if (newFile != null) {
				txtFile.setText(newFile);
			}
		});
				
		l = new Label(main, SWT.NONE);
		l.setText(Messages.DataModelMergeDialog_CaMsg);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).verticalIndent = 10;
		
		Composite caComposite = new Composite(main, SWT.NONE);
		caComposite.setLayout(new GridLayout());
		caComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)caComposite.getLayoutData()).horizontalIndent = 10;
		
		btnCurrent = new Button(caComposite, SWT.RADIO);
		btnCurrent.setText(MessageFormat.format(Messages.DataModelMergeDialog_CurrentCaOp,SmartDB.getCurrentConservationArea().getNameLabel()));
		btnCurrent.setSelection(true);
		btnCurrent.addListener(SWT.Selection, e->configureElements());
		btnCurrent.addListener(SWT.Selection, e-> validate());
		
		btnSelected = new Button(caComposite, SWT.RADIO);
		btnSelected.setText(Messages.DataModelMergeDialog_SelectedCaOp);
		btnSelected.addListener(SWT.Selection, e->configureElements());
		btnSelected.addListener(SWT.Selection, e-> validate());
		
		lstCas = CheckboxTableViewer.newCheckList(caComposite, SWT.BORDER);
		lstCas.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)lstCas.getControl().getLayoutData()).heightHint = 80;
		lstCas.addCheckStateListener(e-> validate());
		lstCas.setContentProvider(ArrayContentProvider.getInstance());
		lstCas.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ConservationArea) return ((ConservationArea)element).getNameLabel();
				return super.getText(element);
			}
		});
		lstCas.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		setTitle(Messages.DataModelMergeDialog_Title);
		setMessage(Messages.DataModelMergeDialog_Message);
		getShell().setText(Messages.DataModelMergeDialog_ShellTitle);
		
		loadCas.setSystem(true);
		loadCas.schedule();
		
		configureElements();
				
		return parent;
	}
	
	/**
	 * Gets the selected data model file
	 * @return
	 */
	public String getFile() {
		return file;
	}
	
	/**
	 * Gets the selected conservation areas to update
	 * @return
	 */
	public List<ConservationArea> getSelectedConservationAreas(){
		return selectedConservationAreas;		
	}
	
	@Override
	public void setErrorMessage(String message) {
		super.setErrorMessage(message);
		if (getButton(IDialogConstants.OK_ID) != null) getButton(IDialogConstants.OK_ID).setEnabled(message == null);
	}
	
	private void validate() {
		setErrorMessage(null);
		if (txtFile.getText().isEmpty()) {
			setErrorMessage(Messages.DataModelMergeDialog_ValidFileRequiredErr);
			return;
		}
		if (btnSelected.getSelection()) {
			boolean found = false;
			for (Object x : lstCas.getCheckedElements()) {
				if (x instanceof ConservationArea) found = true;
				if (found) break;
			}
			if (!found) {
				setErrorMessage(Messages.DataModelMergeDialog_CaRequiredErr);
			}
		}
	}
	private void configureElements() {
		lstCas.getControl().setEnabled(btnSelected.getSelection());
	}
	@Override
	public boolean isResizable() {
		return true;
	}
	
	private Job loadCas = new Job("load conservation areas") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<ConservationArea> conservationAreas = new ArrayList<ConservationArea>();
			try(Session session = HibernateManager.openSession()){
				List<Employee> employees = QueryFactory.buildQuery(session, Employee.class, "smartUserId", SmartDB.getCurrentEmployee().getSmartUserId()).list(); //$NON-NLS-1$
				for (Employee e : employees) {
					if (e.getConservationArea().getIsCcaa()) continue;
					if (UserLevelManager.INSTANCE.supportsUser(e, UserLevelManager.ADMIN)) {
						//TODO: do we also want to check the password
						e.getConservationArea().getNameLabel();
						conservationAreas.add(e.getConservationArea());
					}
				}
			}
			conservationAreas.sort((a,b)->{
				if (a.equals(b)) return 0;
				if (a.equals(SmartDB.getCurrentConservationArea())) return -1;
				if (b.equals(SmartDB.getCurrentConservationArea())) return 1;
				return Collator.getInstance().compare(a.getNameLabel(), b.getNameLabel());
			});
				
			Display.getDefault().syncExec(()->{
				if (conservationAreas.isEmpty()) {
					lstCas.setInput(MessageFormat.format(Messages.DataModelMergeDialog_UsernameErr, SmartDB.getCurrentEmployee().getSmartUserId()));
				}else {
					lstCas.setInput(conservationAreas);
					lstCas.setCheckedElements(new Object[] {SmartDB.getCurrentConservationArea()});
				}
			});
			return Status.OK_STATUS;
		} 
		
	};
	private Button btnSelected;
	private Button btnCurrent;
	private Text txtFile;
}
