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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * Dialog for managing CyberTracker profiles.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ManageProfilesDialog extends AbstractPropertyJHeaderDialog {

	private static final int DIALOG_WIDTH = 440;
	private static final int DIALOG_HEIGHT = 500;
	
	private TableViewer profilesViewer;
	
	private Button btnCreate;
	private Button btnEdit;
	private Button btnDelete;
	
	public ManageProfilesDialog(Shell parent) {
		super(parent, Messages.ManageProfilesDialog_Title);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(DIALOG_WIDTH, DIALOG_HEIGHT);
	}

	@Override
	protected Composite createContent(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		profilesViewer = new TableViewer(main, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		profilesViewer.setLabelProvider(new CtProfileLabelProvider());
		profilesViewer.setContentProvider(ArrayContentProvider.getInstance());
		profilesViewer.setInput(getProfilesList());
		profilesViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		profilesViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editCurrentProfile();
			}
		});
		profilesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateState();
			}
		});

		Composite btnCmp = new Composite(main, SWT.NONE);
		btnCmp.setLayout(new GridLayout(1, false));
		btnCmp.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		
		btnCreate = new Button(btnCmp, SWT.PUSH);
		btnCreate.setText(Messages.ManageProfilesDialog_Button_Create);
		btnCreate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				createNewProfile();
			}
		});

		btnEdit = new Button(btnCmp, SWT.PUSH);
		btnEdit.setText(Messages.ManageProfilesDialog_Button_Edit);
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editCurrentProfile();
			}
		});
		
		btnDelete = new Button(btnCmp, SWT.PUSH);
		btnDelete.setText(Messages.ManageProfilesDialog_Button_Delete);
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteCurrentProfile();
			}
		});

		updateState();
		setTitle(Messages.ManageProfilesDialog_Title);
		setMessage(Messages.ManageProfilesDialog_Message);
		super.setTitleImage(CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.CT_WIZARD_BANNER));
		
		return main;
	}
	
	protected void updateState() {
		CyberTrackerPropertiesProfile p = getSelectedProfile();
		btnEdit.setEnabled(p != null);
		btnDelete.setEnabled(p != null && !p.isDefault());
	}
	
	private void reloadData() {
		profilesViewer.setInput(getProfilesList());
		updateState();
	}
	
	protected CyberTrackerPropertiesProfile getSelectedProfile() {
		IStructuredSelection selection = (IStructuredSelection) profilesViewer.getSelection();
		return (!selection.isEmpty() && selection.getFirstElement() instanceof CyberTrackerPropertiesProfile) ?
				(CyberTrackerPropertiesProfile) selection.getFirstElement() : null;
	}

	protected void createNewProfile() {
		CreateNewProfileOpDialog opDialog = new CreateNewProfileOpDialog(getShell(), getProfilesList());
		if (opDialog.open() == Window.OK){
			CyberTrackerPropertiesProfile initProfile  = null;
		
			try{
				initProfile = opDialog.getProfile();
			}catch (Exception ex){
				SmartPlugIn.displayLog(Messages.ManageProfilesDialog_CreateProfile_Erorr + ex.getLocalizedMessage(), ex);
				return;
			}
			if (initProfile == null){
				//cancelled or invalid model
				return;
			}
			Dialog dialog = new CyberTrackerPropertiesDialog(getShell(), initProfile);
			dialog.open();
			
			//refresh list
			reloadData();
		}
	}

	protected void deleteCurrentProfile() {
		final CyberTrackerPropertiesProfile p = getSelectedProfile();
		if (p == null){
			return;
		}
		if (!MessageDialog.openConfirm(getShell(), Messages.ManageProfilesDialog_DeleteConfirmDialog_Title, MessageFormat.format(Messages.ManageProfilesDialog_DeleteConfirmDialog_Message, p.getName()))){
			return;
		}

		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.ManageProfilesDialog_DeleteTask_Name, 1);
					Session s = HibernateManager.openSession();
					s.beginTransaction();
					try {
						CyberTrackerHibernateManager.deleteProfile(s, p);
						s.getTransaction().commit();							
					}catch (Exception ex){
						s.getTransaction().rollback();
						SmartPlugIn.displayLog(Messages.ManageProfilesDialog_DeleteTask_Error, ex);
					} finally {
						s.close();
						monitor.done();
					}
				}
			});
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.ManageProfilesDialog_DeleteTask_Error, ex);
		}
		
		reloadData();
	}

	protected void editCurrentProfile() {
		Dialog dialog = new CyberTrackerPropertiesDialog(getShell(), getSelectedProfile());
		dialog.open();
		reloadData();
	}

	private List<CyberTrackerPropertiesProfile> getProfilesList() {
		final List<CyberTrackerPropertiesProfile> profileList = new ArrayList<CyberTrackerPropertiesProfile>();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.ManageProfilesDialog_LoadProfileList_Task, 1);
					Session s = HibernateManager.openSession();
					s.beginTransaction();
					try {
						profileList.addAll(CyberTrackerHibernateManager.getPropertiesProfiles(s));
					} catch (Exception ex) {
						SmartPlugIn.displayLog(Messages.ManageProfilesDialog_LoadProfileList_Error, ex);
					} finally {
						s.getTransaction().rollback();
						s.close();
					}
				}
			});
		} catch (Exception e) {
			SmartPlugIn.displayLog(Messages.ManageProfilesDialog_LoadProfileList_Error, e);
			return Collections.emptyList();
		}
		return profileList;
	}

	@Deprecated
	@Override
	public Session getSession() {
		throw new UnsupportedOperationException("It is not allowed to use session attached to this thread."); //$NON-NLS-1$
	}

	@Override
	protected boolean performSave() {
		return true;
	}

}
