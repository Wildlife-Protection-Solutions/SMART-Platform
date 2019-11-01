package org.wcs.smart.i2.ui.dialogs;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.ui.ProfileLabelProvider;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog lists all profile configurations 
 * 
 * @author Emily
 *
 */
public class ProfilesListDialog extends SmartStyledTitleDialog {

	private TableViewer tblConfigurations;
	private List<IntelProfile> configs;
	private ProfileLabelProvider lblProvider = new ProfileLabelProvider();
	
	public ProfilesListDialog(Shell parent) {
		super(parent);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);	
	}

	
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
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
		
		setTitle("Profile Configurations");
		setMessage("Manage profile configurations");
		
		refresh();
		
		return parent;
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
		
	}
	
	private void refresh() {
		tblConfigurations.setInput(new String[] {DialogConstants.LOADING_TEXT});
		loadConfigJob.schedule();
	}
	
	private Job loadConfigJob = new Job("profiles") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<IntelProfile> temp = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				temp.addAll(ProfilesManager.INSTANCE.getProfiles(session));
			}
			ProfilesListDialog.this.configs = temp;
			configs.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
			Display.getDefault().asyncExec(()->{
				tblConfigurations.setInput(configs);
				tblConfigurations.getControl().getParent().layout(true);
			});
			return Status.OK_STATUS;
		}
		
	};
}
