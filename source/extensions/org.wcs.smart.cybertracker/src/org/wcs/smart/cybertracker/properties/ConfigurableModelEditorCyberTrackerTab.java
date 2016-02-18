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

import java.util.Collections;
import java.util.List;

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

	@Override
	public void setDialog(ConfigurableModelEditDialog dialog) {
		this.dialog = dialog;
	}
	
	@Override
	public String getTabName() {
		return Messages.ConfigurableModelEditorCyberTrackerTab_TabName;
	}

	@Override
	public Composite createTabContent(Composite parent) {
		cmProfile = CyberTrackerHibernateManager.getAssociatedCmProfile(dialog.getSession(), dialog.getModel());

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
 		cbProfile.setInput(getProfilesList());
 		cbProfile.setSelection(new StructuredSelection(cmProfile.getProfile()));
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
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.ConfigurableModelEditorCyberTrackerTab_ProfileDetails);
		
		ctPropCmp = new CyberTrackerPropertiesComposite(main);
		ctPropCmp.setReadOnly(true);
		ctPropCmp.addPropsChangeListener(new IPropsChangeListener(){
			@Override
			public void changesMade() {
				//we should appear here only in case of error in implementation for read-only mode!
				SmartPlugIn.displayLog("Changes to profile are not allowed here. Edit the profile to make changes.", null); //$NON-NLS-1$
			}
		});
		ctPropCmp.populateValuesFromObj(cmProfile.getProfile());
		
		return main;
	}

	private List<CyberTrackerPropertiesProfile> getProfilesList() {
		if (profileList != null) {
			for (CyberTrackerPropertiesProfile p : profileList) {
				dialog.getSession().evict(p); //we need fresh objects, not cached ones
			}
		}
		profileList = CyberTrackerHibernateManager.getPropertiesProfiles(dialog.getSession());
		Collections.sort(profileList, new CtProfileDefaultNameComparator());
		return profileList;
	}

	private void reloadData() {
		CyberTrackerPropertiesProfile prevSelection = getSelectedProfile();
		List<CyberTrackerPropertiesProfile> list = getProfilesList();
		cbProfile.setInput(list);
		//try to restore previous selection, otherwise select associated profile
		if (list.contains(prevSelection)) {
	 		cbProfile.setSelection(new StructuredSelection(prevSelection)); //this will trigger profileChanged() call
		} else {
			dialog.getSession().evict(cmProfile); //required as cmProfile contains reference to removed profile and is still attached to session
			cmProfile = CyberTrackerHibernateManager.getAssociatedCmProfile(dialog.getSession(), dialog.getModel());
	 		cbProfile.setSelection(new StructuredSelection(cmProfile.getProfile())); //this will trigger profileChanged() call
		}
	}
	
	protected CyberTrackerPropertiesProfile getSelectedProfile() {
		IStructuredSelection selection = (IStructuredSelection) cbProfile.getSelection();
		return (!selection.isEmpty() && selection.getFirstElement() instanceof CyberTrackerPropertiesProfile) ?
				(CyberTrackerPropertiesProfile) selection.getFirstElement() : null;
	}

	protected void profileChanged() {
		CyberTrackerPropertiesProfile p = getSelectedProfile();
		if (p != null) {
			cmProfile.setProfile(p);
			ctPropCmp.populateValuesFromObj(p);
		}
		dialog.notifyChangesMade();
	}

	protected void manageProfiles() {
		Dialog d = new ManageProfilesDialog(dialog.getShell());
		d.open();
		reloadData();
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
			reloadData();
		}
	}

	protected void editProfile() {
		CyberTrackerPropertiesProfile p = getSelectedProfile();
		if (p != null) {
			Dialog d = new CyberTrackerPropertiesDialog(dialog.getShell(), p);
			d.open();
			reloadData();
		}
	}

	@Override
	public void performSave(Session s) {
		s.saveOrUpdate(cmProfile);
	}
}
