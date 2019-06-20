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
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;

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
	
	private ToolItem tiCreate, tiEdit, tiDelete;
	private MenuItem miCreate, miEdit, miDelete;
	
	
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

		Composite tableComposite = new Composite(main, SWT.NONE);
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		tableComposite.setLayout(tableColumnLayout);
		tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		profilesViewer = new TableViewer(tableComposite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		profilesViewer.setContentProvider(ArrayContentProvider.getInstance());
		
		TableViewerColumn column = new TableViewerColumn(profilesViewer, SWT.NONE);
		column.setLabelProvider(new CtProfileLabelProvider());
		
		profilesViewer.setInput(getProfilesList());
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
		tableColumnLayout.setColumnData(profilesViewer.getTable().getColumn(0), new ColumnWeightData(1));
		
		ToolBar tb = new ToolBar(main, SWT.VERTICAL | SWT.FLAT | SWT.RIGHT);
		tb.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		tiCreate = new ToolItem(tb, SWT.PUSH);
		tiCreate.setText(DialogConstants.ADD_BUTTON_TEXT);
		tiCreate.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		tiCreate.addListener(SWT.Selection, e->createNewProfile());
		
		tiEdit = new ToolItem(tb, SWT.PUSH);
		tiEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		tiEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		tiEdit.addListener(SWT.Selection, e->editCurrentProfile());
		tiEdit.setEnabled(false);
		
		tiDelete = new ToolItem(tb, SWT.PUSH);
		tiDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiDelete.addListener(SWT.Selection, e->deleteCurrentProfile());
		tiDelete.setEnabled(false);
		
		Menu menu = new Menu(profilesViewer.getControl());
		
		miCreate = new MenuItem(menu, SWT.PUSH);
		miCreate.setText(DialogConstants.ADD_BUTTON_TEXT);
		miCreate.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miCreate.addListener(SWT.Selection, e->createNewProfile());
		
		miEdit = new MenuItem(menu, SWT.PUSH);
		miEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		miEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEdit.setEnabled(false);
		miEdit.addListener(SWT.Selection, e->editCurrentProfile());
		
		miDelete = new MenuItem(menu, SWT.PUSH);
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.addListener(SWT.Selection, e->deleteCurrentProfile());
		miDelete.setEnabled(false);
		
		
		profilesViewer.getControl().setMenu(menu);
		
		updateState();
		setTitle(Messages.ManageProfilesDialog_Title);
		setMessage(Messages.ManageProfilesDialog_Message);
		super.setTitleImage(CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.CT_WIZARD_BANNER));
		
		return main;
	}
	
	protected void updateState() {
		CyberTrackerPropertiesProfile p = getSelectedProfile();
		tiEdit.setEnabled(p != null);
		tiDelete.setEnabled(p != null && !p.isDefault());
		miEdit.setEnabled(p != null);
		miDelete.setEnabled(p != null && !p.isDefault());
	}
	
	private void reloadData() {
		profilesViewer.setInput(getProfilesList());
		
		profilesViewer.refresh();
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
					
					try(Session session = HibernateManager.openSession()){
						session.beginTransaction();
						try {
							CyberTrackerHibernateManager.deleteProfile(session, p);
							session.getTransaction().commit();							
						}catch (Exception ex){
							session.getTransaction().rollback();
							SmartPlugIn.displayLog(Messages.ManageProfilesDialog_DeleteTask_Error, ex);
						}
					} finally {
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
					try(Session session = HibernateManager.openSession()){
						session.beginTransaction();
						try {
							profileList.addAll(CyberTrackerHibernateManager.getPropertiesProfiles(session));
							Collections.sort(profileList, new CtProfileDefaultNameComparator());
						} catch (Exception ex) {
							SmartPlugIn.displayLog(Messages.ManageProfilesDialog_LoadProfileList_Error, ex);
						} finally {
							session.getTransaction().rollback();
						}
					}
				}
			});
		} catch (Exception e) {
			SmartPlugIn.displayLog(Messages.ManageProfilesDialog_LoadProfileList_Error, e);
			return Collections.emptyList();
		}
		return profileList;
	}

	@Override
	protected boolean performSave() {
		return true;
	}

}
