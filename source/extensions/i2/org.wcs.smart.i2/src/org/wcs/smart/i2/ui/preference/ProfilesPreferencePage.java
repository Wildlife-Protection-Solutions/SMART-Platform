/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.preference;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.ui.ProfileLabelProvider;
import org.wcs.smart.i2.ui.dialogs.ProfileDialog;
import org.wcs.smart.ui.properties.DialogConstants;

import com.ibm.icu.text.MessageFormat;

/**
 * Preference page for managing profiles
 * 
 * @author Emily
 *
 */
public class ProfilesPreferencePage extends PreferencePage implements IIntelPreferencePage{
	
	
	private TableViewer tblConfigurations;
	private List<IntelProfile> configs;
	private ProfileLabelProvider lblProvider = new ProfileLabelProvider();
	
	public ProfilesPreferencePage() {
		super();
		noDefaultAndApplyButton();
		setTitle("Profiles");
	}
	
	private Button createButton(Composite parent, String text, String icon) {
		Button btn = new Button(parent, SWT.PUSH);
		btn.setImage(SmartPlugIn.getDefault().getImageRegistry().get(icon));
		btn.setText(text);
		btn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btn.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		return btn;
	}
	
	private MenuItem createMenuItem(Menu parent, String text, String icon) {
		MenuItem btn = new MenuItem(parent, SWT.PUSH);
		btn.setImage(SmartPlugIn.getDefault().getImageRegistry().get(icon));
		btn.setText(text);
		return btn;
	}
	
	private void add() {
		if (configs == null) return;
		IntelProfile c = new IntelProfile();
		c.setConservationArea(SmartDB.getCurrentConservationArea());
		c.setName("New Profile");
		c.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), c.getName());
		c.updateName(SmartDB.getCurrentLanguage(), c.getName());
		
		ProfileDialog pd = new ProfileDialog(getShell(), c, configs);
		pd.open();
		refresh();
	}
	
	private void edit() {
		if (configs == null) return;
		Object x = tblConfigurations.getStructuredSelection().getFirstElement();
		if (!(x instanceof IntelProfile)) return;
		
		IntelProfile c = (IntelProfile)x;
		ProfileDialog pd = new ProfileDialog(getShell(), c, configs);
		pd.open();
		refresh();
	}
	
	private void delete() {
		Object x = tblConfigurations.getStructuredSelection().getFirstElement();
		if (!(x instanceof IntelProfile)) return;
		
		IntelProfile p = (IntelProfile)x;
		
		if (!MessageDialog.openConfirm(getShell(), "Delete", 
				MessageFormat.format("Are you sure you want to delete the profile {0}?  This will delete all data associated with this profile and cannot be undone.", p.getName()))){
			return;
		}
		
		//confirm password
		InputDialog confirm = new InputDialog(getShell(), Messages.RecordsView_DeleteTitle, 
				"Enter your password to continue", "",null){ 
			@Override
			protected void okPressed() {
				if (!HibernateManager.validatePassword(getText().getText(), SmartDB.getCurrentEmployee())){
					setErrorMessage(Messages.DeleteRecordHandler_InvalidPassword);
				}else{
					setReturnCode(OK);
					close();
				}
			}
			
			@Override
			protected int getInputTextStyle() {
				return super.getInputTextStyle() | SWT.PASSWORD;
			}
		};
		if (confirm.open() != Window.OK) return ;
			
		try(Session session = HibernateManager.openSession()){
			ProfilesManager.INSTANCE.canDelete(p, session);
			
			session.getTransaction().begin();
			try {
				ProfilesManager.INSTANCE.deleteProfile(p, session);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				throw ex;
			}
		}catch (Exception ex) {
			Intelligence2PlugIn.displayLog("Unable to delete profile." + "\n\n" + ex.getMessage(), ex);
		}
		refresh();
	}
	
	@Override
	public void refresh() {
		tblConfigurations.setInput(new String[] {DialogConstants.LOADING_TEXT});
		loadConfigJob.schedule();
	}
	
	private Job loadConfigJob = new Job("profiles") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<IntelProfile> temp = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				temp.addAll(ProfilesManager.INSTANCE.getProfiles(session, false));
			}
			ProfilesPreferencePage.this.configs = temp;
			configs.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
			Display.getDefault().asyncExec(()->{
				tblConfigurations.setInput(configs);
				tblConfigurations.getControl().getParent().layout(true);
			});
			return Status.OK_STATUS;
		}
		
	};

	@Override
	protected Control createContents(Composite parent) {
		
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout(2, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite wrapper = new Composite(part, SWT.NONE);
		wrapper.setLayout(new TableColumnLayout());
		wrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblConfigurations = new TableViewer(wrapper, SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		tblConfigurations.setContentProvider(ArrayContentProvider.getInstance());
		tblConfigurations.addDoubleClickListener(e->edit());
	
		
		TableViewerColumn namecol = new TableViewerColumn(tblConfigurations, SWT.NONE);
		namecol.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object other) { return ""; }
		});
		namecol.setLabelProvider(lblProvider);
		((TableColumnLayout)wrapper.getLayout()).setColumnData(namecol.getColumn(), new ColumnWeightData(1));
		
		Composite btnpanel = new Composite(part, SWT.NONE);
		btnpanel.setLayout(new GridLayout());
		((GridLayout)btnpanel.getLayout()).marginWidth = 0;
		((GridLayout)btnpanel.getLayout()).marginHeight = 0;
		btnpanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Button btnAdd = createButton(btnpanel, DialogConstants.ADD_BUTTON_TEXT, SmartPlugIn.ADD_ICON);
		btnAdd.addListener(SWT.Selection, e->add());
		
		Button btnEdit = createButton(btnpanel, DialogConstants.EDIT_BUTTON_TEXT, SmartPlugIn.EDIT_ICON);
		btnEdit.addListener(SWT.Selection, e->edit());
		
		Button btnDelete = createButton(btnpanel, DialogConstants.DELETE_BUTTON_TEXT, SmartPlugIn.DELETE_ICON);
		btnDelete.addListener(SWT.Selection, e->delete());
		
		Menu mnu = new Menu(tblConfigurations.getControl());
		tblConfigurations.getControl().setMenu(mnu);
		MenuItem miAdd = createMenuItem(mnu, DialogConstants.ADD_BUTTON_TEXT, SmartPlugIn.ADD_ICON);
		miAdd.addListener(SWT.Selection, e->add());

		MenuItem miEdit = createMenuItem(mnu, DialogConstants.EDIT_BUTTON_TEXT, SmartPlugIn.EDIT_ICON);
		miEdit.addListener(SWT.Selection, e->edit());
		
		MenuItem miDelete = createMenuItem(mnu, DialogConstants.DELETE_BUTTON_TEXT, SmartPlugIn.DELETE_ICON);
		miDelete.addListener(SWT.Selection, e->delete());

		btnEdit.setEnabled(false);
		btnDelete.setEnabled(false);
		miEdit.setEnabled(false);
		miDelete.setEnabled(false);
		
		tblConfigurations.addSelectionChangedListener(e->{
			btnEdit.setEnabled(!tblConfigurations.getSelection().isEmpty());
			btnDelete.setEnabled(!tblConfigurations.getSelection().isEmpty());
			miEdit.setEnabled(!tblConfigurations.getSelection().isEmpty());
			miDelete.setEnabled(!tblConfigurations.getSelection().isEmpty());
		});
		
		
		setMessage("Profile Configurations");
		
		refresh();
		SmartUiUtils.makeTransparent(parent);

		return parent;
	}

	
	
}
