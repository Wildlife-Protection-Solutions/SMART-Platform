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
package org.wcs.smart.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SaveObjectsJob;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog that allows to edit name for any {@link NamedItem}.
 *
 * @author elitvin
 * @since 6.0.0
 */
public class CreateEditNamedItemDialog extends TitleAreaDialog {

	private TranslateNameComposite tnc;
	private NamedItem item;
	
	private boolean isDirty = false;
	
	/**
	 * @param parentShell parent shell
	 * @param item item to update
	 */
	public CreateEditNamedItemDialog(Shell parentShell, NamedItem item) {
		super(parentShell);
		this.item = reloadItem(item);
	}
	
	private NamedItem reloadItem(NamedItem i) {
		if (i.getUuid() == null) {
			return i;
		}
		
		final NamedItem[] result = new NamedItem[]{i};
		Job j = new Job(Messages.CreateEditNamedItemDialog_ReloadObjectJob_Title) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try(Session s = HibernateManager.openSession()) {
					result[0] = s.get(i.getClass(), i.getUuid());
					result[0].getNames().size(); //load lazy items
				}
				return Status.OK_STATUS;
			}
		};
		
		j.schedule();
		try {
			j.join();
		} catch (InterruptedException e) {
			SmartPlugIn.displayError(Messages.CreateEditNamedItemDialog_ReloadObjectJob_Error, e);
		}
		
		return result[0];
	}

	protected boolean validate() {
		String err = tnc.getError();
		boolean ok = err == null;
		setErrorMessage(err);
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null){
			if (isDirty){
				btn.setEnabled(ok);
			}else{
				btn.setEnabled(false);
			}
		}
		return ok;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent){
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(item.getUuid() == null);
		getButton(IDialogConstants.OK_ID).setText(DialogConstants.SAVE_TEXT);
	}
	
	@Override
	public boolean close(){
		getButtonBar().setFocus();
		return super.close();
	}
	
	
	protected boolean save(){
		if (!validate() || !saveToDatabase()){
			return false;
		}
		;
		setDirty(false);
		return true;
	}
	
	protected boolean saveToDatabase() {
		Job saveObjJob = new SaveObjectsJob(Messages.CreateEditNamedItemDialog_SaveObjectJob_Title, item);
		saveObjJob.schedule();
		try {
			saveObjJob.join();
		} catch (InterruptedException e) {
			SmartPlugIn.displayError(Messages.CreateEditNamedItemDialog_SaveObjectJob_Error, e);
			return false;
		}
		return true;
	}

	@Override
	protected void okPressed() {
		if (save()){
			super.okPressed();
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		composite = new Composite(composite, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(2, false));
		
		createTranslateFieldLabel(composite);
		
		tnc = new TranslateNameComposite(composite, item) {
			@Override
			protected void handleChanged() {
				super.handleChanged();
				setDirty(true);
			}
		};
		tnc.setCurrentLanguage(SmartDB.getCurrentLanguage());
		
		return composite;
	}

	protected void createTranslateFieldLabel(Composite parent) {
		Label lblName = new Label(parent, SWT.NONE);
		lblName.setText(Messages.CreateEditNamedItemDialog_Name);
	}
	
	protected boolean isResizable() {
		return true;
	}
	
	protected void setDirty(boolean isDirty){
		this.isDirty = isDirty;
		validate();
	}
	
}
