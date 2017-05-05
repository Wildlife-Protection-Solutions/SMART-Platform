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
package org.wcs.smart.cybertracker.properties;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Dialog for creating new CyberTracker profile.
 * 
 * @author Evgeniy
 *
 */
public class CreateNewProfileOpDialog extends TitleAreaDialog {
	
	private enum CreateProfileOption {
		BLANK,
		PROFILE
	}

	private CreateProfileOption option = CreateProfileOption.BLANK;
	private Button opBlank, opProfile;
	private ComboViewer cbProfile;
	private CyberTrackerPropertiesProfile initProfile;
	private String name = null;
	private CyberTrackerPropertiesProfile profileTemplate = null;
	private Text txtName;
	private List<CyberTrackerPropertiesProfile> prolfileList;
	
	protected CreateNewProfileOpDialog(Shell parentShell, List<CyberTrackerPropertiesProfile> prolfileList) {
		super(parentShell);
		this.prolfileList = prolfileList;
	}

	protected void okPressed() {
		name = txtName.getText();
		IStructuredSelection selection = (IStructuredSelection)cbProfile.getSelection();
		profileTemplate = !selection.isEmpty() ? (CyberTrackerPropertiesProfile) selection.getFirstElement() : null;
		String error = validate();
		setErrorMessage(error);
		if (error == null) {
			super.okPressed();
		}
	}
	
	private String validate() {
		if (CreateProfileOption.PROFILE.equals(option) && cbProfile.getSelection().isEmpty()) {
			return Messages.CreateNewProfileOpDialog_ProfileTemplate_Required;
		}
		return null;
	}

	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout(3, false));
		panel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 20;
		Label lblName = new Label(panel, SWT.NONE);
		lblName.setText(Messages.CreateNewProfileOpDialog_Name);
		
		txtName = new Text(panel, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		txtName.setText(Messages.CreateNewProfileOpDialog_DefaultProfileName);
		
		Label lblOp = new Label(panel, SWT.NONE);
		lblOp.setText(Messages.CreateNewProfileOpDialog_Template);
		
		opBlank = new Button(panel, SWT.RADIO);
		opBlank.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
		opBlank.setSelection(true);
		opBlank.setText(Messages.CreateNewProfileOpDialog_Blank);
		opBlank.setToolTipText(Messages.CreateNewProfileOpDialog_Blank_Tooltip);
		opBlank.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				optionChanged(CreateProfileOption.BLANK);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		
		new Label(panel, SWT.NONE);

		opProfile = new Button(panel, SWT.RADIO);
		opProfile.setText(Messages.CreateNewProfileOpDialog_Profile);
		opProfile.setToolTipText(Messages.CreateNewProfileOpDialog_Profile_Tooltip);
		opProfile.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				optionChanged(CreateProfileOption.PROFILE);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});

		cbProfile = new ComboViewer(panel, SWT.READ_ONLY);
		cbProfile.getControl().setEnabled(false);
		cbProfile.getControl().setToolTipText(Messages.CreateNewProfileOpDialog_Profile_Tooltip);
		cbProfile.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cbProfile.setContentProvider(ArrayContentProvider.getInstance());
		cbProfile.setLabelProvider(new CtProfileLabelProvider());
 		cbProfile.setInput(prolfileList);
		cbProfile.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				//changesMade();
			}
		});
		
		
		getShell().setText(Messages.CreateNewProfileOpDialog_Title);
		setTitle(Messages.CreateNewProfileOpDialog_Title);
		setMessage(Messages.CreateNewProfileOpDialog_Message);
		
		return parent;
	}

	protected void optionChanged(CreateProfileOption op) {
		option = op;
		cbProfile.getControl().setEnabled(CreateProfileOption.PROFILE.equals(option));
	}
	
	public CyberTrackerPropertiesProfile getProfile() throws Exception {
		switch (option) {
		case BLANK:
			return CyberTrackerProfileFactory.createUsingDefaults(name);
		case PROFILE:
		{
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
			pmd.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.CyberTrackerPropertiesDialog_LoadProfile_Task, 1);
					Session s = HibernateManager.openSession();
					s.beginTransaction();
					try {
						CyberTrackerPropertiesProfile fullProfile = (CyberTrackerPropertiesProfile) s.get(CyberTrackerPropertiesProfile.class, profileTemplate.getUuid());
						initProfile = CyberTrackerProfileFactory.createProfileClone(fullProfile, name, monitor);
					} catch (Exception ex) {
						SmartPlugIn.displayLog(Messages.CyberTrackerPropertiesDialog_LoadProfile_Error, ex);
					} finally {
						s.getTransaction().rollback();
						s.close();
					}
				}
			});
			return initProfile;
		}
		}
		//this line should never be reached
		throw new IllegalStateException("Unknown template option for creaing a CyberTracker properties profile: " + option); //$NON-NLS-1$
	}
	
}
