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

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.ConfigurableModelCtPropertiesProfile;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.properties.CyberTrackerPropertiesComposite.IPropsChangeListener;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditDialog;
import org.wcs.smart.dataentry.dialog.IConfigurableModelEditorTabContent;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Tab with CyberTracker properties content for configurable model.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConfigurableModelEditorCyberTrackerTab implements IConfigurableModelEditorTabContent {

	private ConfigurableModelEditDialog dialog;
	
	private ComboViewer cbProfile;
	private CyberTrackerPropertiesComposite ctPropCmp;
	private ConfigurableModelCtPropertiesProfile cmProfile;
	private List<CyberTrackerPropertiesProfile> profileList;
	private boolean fireEvent = true;

	private Label lblProfileName;
	
	@Override
	public void setDialog(ConfigurableModelEditDialog dialog) {
		this.dialog = dialog;
	}
	
	@Override
	public String getTabName() {
		return Messages.ConfigurableModelEditorCyberTrackerTab_TabName;
	}

	private void initProfile() {
		Session s = dialog.getSession();
		cmProfile = CyberTrackerHibernateManager.getAssociatedCmProfile(s, dialog.getModel());

		ConfigurableModel clonedFrom = dialog.getClonedFrom();
		if (clonedFrom != null) {
			ConfigurableModelCtPropertiesProfile profileToClone = CyberTrackerHibernateManager.getAssociatedCmProfile(s, clonedFrom);
			cmProfile.setProfile(profileToClone.getProfile());
		}

		loadProfilesList();
	}

	private void loadProfilesList(){
		CyberTrackerPropertiesProfile prevSelection = getSelectedProfile();
		
		Session s = dialog.getSession();
		profileList = CyberTrackerHibernateManager.getPropertiesProfiles(s);
		Collections.sort(profileList, new CtProfileDefaultNameComparator());
			
		cbProfile.setInput(profileList);
		if (prevSelection != null){
			if (profileList.contains(prevSelection)){
				fireEvent = false;
				try{
					cbProfile.setSelection(new StructuredSelection(prevSelection));
				}finally{
					fireEvent = true;
				}
			}else{
				//selected profile was deleted
				s.evict(cmProfile); //required as actual object in db will be removed by constraint
				cmProfile = CyberTrackerHibernateManager.getAssociatedCmProfile(s, dialog.getModel());
				cbProfile.setSelection(new StructuredSelection(profileList.get(0)));
			}
		}else{
			fireEvent = false;
			try{
				cbProfile.setSelection(new StructuredSelection(cmProfile.getProfile()));
			}finally{
				fireEvent = true;
			}
		}
	}
	
	@Override
	public Composite createTabContent(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout mainLayout = new GridLayout(1, false);
		mainLayout.marginBottom=0;
		mainLayout.marginHeight = 0;
		mainLayout.marginLeft = 0;
		mainLayout.marginRight = 0;
		mainLayout.marginTop = 0;
		mainLayout.marginWidth = 0;
		main.setLayout(mainLayout);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite controlCmp = new Composite(main, SWT.NONE);
		controlCmp.setLayout(new GridLayout(2, false));
		controlCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lblProfile = new Label(controlCmp, SWT.NONE);
		lblProfile.setText(Messages.ConfigurableModelEditorCyberTrackerTab_Profile);
		lblProfile.setToolTipText(Messages.ConfigurableModelEditorCyberTrackerTab_Profile_Tooltip);

		cbProfile = new ComboViewer(controlCmp, SWT.READ_ONLY);
		cbProfile.getControl().setToolTipText(Messages.ConfigurableModelEditorCyberTrackerTab_Profile_Tooltip);
		cbProfile.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cbProfile.setContentProvider(ArrayContentProvider.getInstance());
		cbProfile.setLabelProvider(new CtProfileLabelProvider());
 		cbProfile.setInput(Messages.ConfigurableModelEditorCyberTrackerTab_LoadingLabel);
 		
		cbProfile.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				profileChanged();
			}
		});

		Composite buttonsCmp = new Composite(main, SWT.NONE);
		buttonsCmp.setLayout(new GridLayout(3, false));
		buttonsCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnManage = new Button(buttonsCmp, SWT.PUSH);
		btnManage.setText(Messages.ConfigurableModelEditorCyberTrackerTab_Button_ManageProfiles);
		btnManage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				manageProfiles();
			}
		});
		
		Button btnCreate = new Button(buttonsCmp, SWT.PUSH);
		btnCreate.setText(Messages.ConfigurableModelEditorCyberTrackerTab_Button_CreateProfile);
		btnCreate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				createProfile();
			}
		});

		Button btnEdit = new Button(buttonsCmp, SWT.PUSH);
		btnEdit.setText(Messages.ConfigurableModelEditorCyberTrackerTab_Button_EditProfile);
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editProfile();
			}
		});
		
		Label lblSep = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
		lblSep.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lblProfileName = new Label(main, SWT.NONE);
		lblProfileName.setText(Messages.ConfigurableModelEditorCyberTrackerTab_ProfileDetails);
		lblProfileName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		ctPropCmp = new CyberTrackerPropertiesComposite(main);
		ctPropCmp.setReadOnly(true);
		ctPropCmp.addPropsChangeListener(new IPropsChangeListener(){
			@Override
			public void changesMade() {
				//we should appear here only in case of error in implementation for read-only mode!
				SmartPlugIn.displayLog("Changes to profile are not allowed here. Edit the profile to make changes.", null); //$NON-NLS-1$
			}
		});
		
		initProfile();
		return main;
	}

	protected CyberTrackerPropertiesProfile getSelectedProfile() {
		if (cbProfile == null) return null;
		
		IStructuredSelection selection = (IStructuredSelection) cbProfile.getSelection();
		return (!selection.isEmpty() && selection.getFirstElement() instanceof CyberTrackerPropertiesProfile) ?
				(CyberTrackerPropertiesProfile) selection.getFirstElement() : null;
	}

	protected void profileChanged() {
		final CyberTrackerPropertiesProfile p = getSelectedProfile();
		lblProfileName.setText(MessageFormat.format(Messages.ConfigurableModelEditorCyberTrackerTab_ProfileDetailsMessage, p.getName()));
		Job j = new Job(Messages.ConfigurableModelEditorCyberTrackerTab_profileLoadJobname){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// in it's own job in it's own session; this should be ok
				if (p != null) {
					CyberTrackerPropertiesProfile p2 = null;
					Session s = HibernateManager.openSession();
					try{
						 p2 = (CyberTrackerPropertiesProfile) s.load(CyberTrackerPropertiesProfile.class, p.getUuid());
						 p2.getOptions().size();
					}finally{
						s.close();
					}
					if (p2 != null){
						final CyberTrackerPropertiesProfile p3 = p2;
						Display.getDefault().syncExec(new Runnable(){

							@Override
							public void run() {
								ctPropCmp.populateValuesFromObj(p3);		
							}
							
						});
					}
				}
				return Status.OK_STATUS;
			}
			
		};
		j.setSystem(true);
		j.schedule();
		
		cmProfile.setProfile(p);
		if (fireEvent){
			dialog.notifyChangesMade();
		}
	}

	protected void manageProfiles() {
		Dialog d = new ManageProfilesDialog(dialog.getShell());
		d.open();
		loadProfilesList();
	}

	protected void createProfile() {
		CreateNewProfileOpDialog opDialog = new CreateNewProfileOpDialog(dialog.getShell(), profileList);
		if (opDialog.open() == Window.OK) {
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
			Dialog d = new CyberTrackerPropertiesDialog(dialog.getShell(), initProfile);
			d.open();
			
			//refresh list
			loadProfilesList();
		}
	}

	protected void editProfile() {
		CyberTrackerPropertiesProfile p = getSelectedProfile();
		if (p != null) {
			Dialog d = new CyberTrackerPropertiesDialog(dialog.getShell(), p);
			d.open();
			loadProfilesList();
		}
	}

	@Override
	public void performSave(Session s) {
		s.saveOrUpdate(cmProfile);
	}
	
	@Override
	public int getTabIndex() {
		return 100;
	}
	
}
