/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.ui.internal.preference;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.hibernate.Session;
import org.wcs.smart.QuickLinkManager;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.QuickLink;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.LanguageLabelProvider;
import org.wcs.smart.user.UserLevelManager;

/**
 * Preference page for configuring quick links that show up in quick link menu
 * 
 * @author Emily
 *
 */
public class QuickLinkPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private static final String MOVE_UP = Messages.QuickLinkPreferencePage_MoveUp;
	private static final String MOVE_DOWN = Messages.QuickLinkPreferencePage_MoveDown;
	
	private List<QuickLink> userLinks = new ArrayList<>();
	private List<QuickLink> systemLinks = new ArrayList<>();
	private List<QuickLink> removedLinks = new ArrayList<>();
	private List<Language> allLanguages = new ArrayList<>();
	
	private TableViewer userViewer ;
	private TableViewer systemViewer ;
	
	private Job loadDataJob = new Job("load links") { //$NON-NLS-1$
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try(Session session = HibernateManager.openSession()){
				List<Language> ls = QueryFactory.buildQuery(session, Language.class, 
						new Object[] {"ca", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
				
				QuickLinkPreferencePage.this.allLanguages.addAll(ls);
				
				List<QuickLink> links = QuickLinkManager.INSTANCE.getQuickLinks(session);
				for (QuickLink l : links) {
					l.getNames().size();
					if (l.getEmployee() == null) {
						QuickLinkPreferencePage.this.systemLinks.add(l);
					}else if (l.getEmployee().equals(SmartDB.getCurrentEmployee())) {
						QuickLinkPreferencePage.this.userLinks.add(l);
					}
				}
			}
			Display.getDefault().asyncExec(()->{
				userViewer.refresh();
				systemViewer.refresh();
			});
			return Status.OK_STATUS;
		}
	};
	
	public QuickLinkPreferencePage() {
		this(null);
	}

	public QuickLinkPreferencePage(String title) {
		this(title, null);
	}

	public QuickLinkPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
		noDefaultButton();
	}

	@Override
	public boolean performOk() {
		this.save();
		QuickLinkManager.INSTANCE.reset();
		return super.performOk();
	}
	
	private boolean canModifySystem() {	
		return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), 
				UserLevelManager.ADMIN, UserLevelManager.MANAGER);
	}
	
	@Override
	public void init(IWorkbench workbench) {
		
	}

	
	@Override
	protected Control createContents(Composite parent) {

		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.QuickLinkPreferencePage_UserLinks);
		
		Composite userArea = new Composite(main, SWT.NONE);
		userArea.setLayout(new GridLayout(2, false));
		((GridLayout)userArea.getLayout()).marginWidth = 0;
		((GridLayout)userArea.getLayout()).marginHeight = 0;
		userArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		userViewer = createTableViewer(userArea);
		userViewer.setInput(this.userLinks);
		
		Composite btnArea = new Composite(userArea, SWT.NONE);
		btnArea.setLayout(new GridLayout());
		((GridLayout)btnArea.getLayout()).marginWidth = 0;
		((GridLayout)btnArea.getLayout()).marginHeight = 0;
		btnArea.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Button btnAddUser = new Button(btnArea, SWT.PUSH);
		btnAddUser.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAddUser.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAddUser.addListener(SWT.Selection, e->addUser());
		btnAddUser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Button btnEditUser = new Button(btnArea, SWT.PUSH);
		btnEditUser.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEditUser.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEditUser.addListener(SWT.Selection, e->QuickLinkPreferencePage.this.edit(userViewer.getStructuredSelection().getFirstElement()));
		btnEditUser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnDeleteUser = new Button(btnArea, SWT.PUSH);
		btnDeleteUser.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDeleteUser.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDeleteUser.addListener(SWT.Selection, e->QuickLinkPreferencePage.this.delete(userViewer.getStructuredSelection().getFirstElement()));
		btnDeleteUser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(btnArea, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnMoveUp = new Button(btnArea, SWT.PUSH);
		btnMoveUp.setText(MOVE_UP);
		btnMoveUp.addListener(SWT.Selection, e->QuickLinkPreferencePage.this.move(userViewer.getStructuredSelection().getFirstElement(), -1));
		btnMoveUp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnMoveDown = new Button(btnArea, SWT.PUSH);
		btnMoveDown.setText(MOVE_DOWN);
		btnMoveDown.addListener(SWT.Selection, e->QuickLinkPreferencePage.this.move(userViewer.getStructuredSelection().getFirstElement(), 1));
		btnMoveDown.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Menu mnuUser = new Menu(userViewer.getControl());
		userViewer.getControl().setMenu(mnuUser);
		
		MenuItem miAddUser = new MenuItem(mnuUser, SWT.PUSH);
		miAddUser.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAddUser.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAddUser.addListener(SWT.Selection, e->addUser());
		
		MenuItem miEditUser = new MenuItem(mnuUser, SWT.PUSH);
		miEditUser.setText(DialogConstants.EDIT_BUTTON_TEXT);
		miEditUser.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEditUser.addListener(SWT.Selection, e->QuickLinkPreferencePage.this.edit(userViewer.getStructuredSelection().getFirstElement()));
		
		MenuItem miDeleteUser = new MenuItem(mnuUser, SWT.PUSH);
		miDeleteUser.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDeleteUser.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDeleteUser.addListener(SWT.Selection, e->QuickLinkPreferencePage.this.delete(userViewer.getStructuredSelection().getFirstElement()));
		
		new MenuItem(mnuUser, SWT.SEPARATOR );
		
		MenuItem miMoveUp = new MenuItem(mnuUser, SWT.PUSH);
		miMoveUp.setText(MOVE_UP);
		miMoveUp.addListener(SWT.Selection, e->QuickLinkPreferencePage.this.move(userViewer.getStructuredSelection().getFirstElement(), -1));
		
		MenuItem miMoveDown = new MenuItem(mnuUser, SWT.PUSH);
		miMoveDown.setText(MOVE_DOWN);
		miMoveDown.addListener(SWT.Selection, e->QuickLinkPreferencePage.this.move(userViewer.getStructuredSelection().getFirstElement(), 1));
		
		userViewer.addSelectionChangedListener(e->{
			miEditUser.setEnabled(!userViewer.getStructuredSelection().isEmpty());
			btnEditUser.setEnabled(!userViewer.getStructuredSelection().isEmpty());
			miDeleteUser.setEnabled(!userViewer.getStructuredSelection().isEmpty());
			btnDeleteUser.setEnabled(!userViewer.getStructuredSelection().isEmpty());
			miMoveUp.setEnabled(!userViewer.getStructuredSelection().isEmpty());
			btnMoveUp.setEnabled(!userViewer.getStructuredSelection().isEmpty());
			miMoveDown.setEnabled(!userViewer.getStructuredSelection().isEmpty());
			btnMoveDown.setEnabled(!userViewer.getStructuredSelection().isEmpty());
		});
		miEditUser.setEnabled(!userViewer.getStructuredSelection().isEmpty());
		btnEditUser.setEnabled(!userViewer.getStructuredSelection().isEmpty());
		miDeleteUser.setEnabled(!userViewer.getStructuredSelection().isEmpty());
		btnDeleteUser.setEnabled(!userViewer.getStructuredSelection().isEmpty());
		btnMoveUp.setEnabled(!userViewer.getStructuredSelection().isEmpty());
		miMoveUp.setEnabled(!userViewer.getStructuredSelection().isEmpty());
		btnMoveDown.setEnabled(!userViewer.getStructuredSelection().isEmpty());
		miMoveDown.setEnabled(!userViewer.getStructuredSelection().isEmpty());
		
		l = new Label(main, SWT.NONE);
		l.setText(Messages.QuickLinkPreferencePage_SystemLinks);
		
		Composite systemArea = new Composite(main, SWT.NONE);
		systemArea.setLayout(new GridLayout(2, false));
		((GridLayout)systemArea.getLayout()).marginWidth = 0;
		((GridLayout)systemArea.getLayout()).marginHeight = 0;
		systemArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		systemViewer = createTableViewer(systemArea);
		systemViewer.setInput(this.systemLinks);
		
		if (canModifySystem()) {
		
			btnArea = new Composite(systemArea, SWT.NONE);
			btnArea.setLayout(new GridLayout());
			((GridLayout)btnArea.getLayout()).marginWidth = 0;
			((GridLayout)btnArea.getLayout()).marginHeight = 0;
			btnArea.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
			
			Button btnAddSystem = new Button(btnArea, SWT.PUSH);
			btnAddSystem.setText(DialogConstants.ADD_BUTTON_TEXT);
			btnAddSystem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			btnAddSystem.addListener(SWT.Selection, e->addSystem());
			btnAddSystem.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	
			Button btnEditSystem = new Button(btnArea, SWT.PUSH);
			btnEditSystem.setText(DialogConstants.EDIT_BUTTON_TEXT);
			btnEditSystem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
			btnEditSystem.addListener(SWT.Selection, e->QuickLinkPreferencePage.this.edit(systemViewer.getStructuredSelection().getFirstElement()));
			btnEditSystem.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Button btnDeleteSystem = new Button(btnArea, SWT.PUSH);
			btnDeleteSystem.setText(DialogConstants.DELETE_BUTTON_TEXT);
			btnDeleteSystem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			btnDeleteSystem.addListener(SWT.Selection, e->QuickLinkPreferencePage.this.delete(systemViewer.getStructuredSelection().getFirstElement()));
			btnDeleteSystem.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			l = new Label(btnArea, SWT.SEPARATOR | SWT.HORIZONTAL);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Button btnMoveUpSystem = new Button(btnArea, SWT.PUSH);
			btnMoveUpSystem.setText(MOVE_UP);
			btnMoveUpSystem.addListener(SWT.Selection, e->QuickLinkPreferencePage.this.move(systemViewer.getStructuredSelection().getFirstElement(), -1));
			btnMoveUpSystem.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Button btnMoveDownSystem = new Button(btnArea, SWT.PUSH);
			btnMoveDownSystem.setText(MOVE_DOWN);
			btnMoveDownSystem.addListener(SWT.Selection, e->QuickLinkPreferencePage.this.move(systemViewer.getStructuredSelection().getFirstElement(), 1));
			btnMoveDownSystem.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			
			Menu mnuSystem = new Menu(systemViewer.getControl());
			systemViewer.getControl().setMenu(mnuSystem);
			
			MenuItem miAddSystem = new MenuItem(mnuSystem, SWT.PUSH);
			miAddSystem.setText(DialogConstants.ADD_BUTTON_TEXT);
			miAddSystem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			miAddSystem.addListener(SWT.Selection, e->addSystem());
			
			MenuItem miEditSystem = new MenuItem(mnuSystem, SWT.PUSH);
			miEditSystem.setText(DialogConstants.EDIT_BUTTON_TEXT);
			miEditSystem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
			miEditSystem.addListener(SWT.Selection, e->QuickLinkPreferencePage.this.edit(systemViewer.getStructuredSelection().getFirstElement()));
			
			MenuItem miDeleteSystem = new MenuItem(mnuSystem, SWT.PUSH);
			miDeleteSystem.setText(DialogConstants.DELETE_BUTTON_TEXT);
			miDeleteSystem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			miDeleteSystem.addListener(SWT.Selection, e->QuickLinkPreferencePage.this.delete(systemViewer.getStructuredSelection().getFirstElement()));
			
			new MenuItem(mnuSystem, SWT.SEPARATOR );
			
			MenuItem miMoveUpSystem = new MenuItem(mnuSystem, SWT.PUSH);
			miMoveUpSystem.setText(MOVE_UP);
			miMoveUpSystem.addListener(SWT.Selection, e->QuickLinkPreferencePage.this.move(systemViewer.getStructuredSelection().getFirstElement(), -1));
			
			MenuItem miMoveDownSystem = new MenuItem(mnuSystem, SWT.PUSH);
			miMoveDownSystem.setText(MOVE_DOWN);
			miMoveDownSystem.addListener(SWT.Selection, e->QuickLinkPreferencePage.this.move(systemViewer.getStructuredSelection().getFirstElement(), 1));
			
			
			systemViewer.addSelectionChangedListener(e->{
				miEditSystem.setEnabled(!systemViewer.getStructuredSelection().isEmpty());
				btnEditSystem.setEnabled(!systemViewer.getStructuredSelection().isEmpty());
				miDeleteSystem.setEnabled(!systemViewer.getStructuredSelection().isEmpty());
				btnDeleteSystem.setEnabled(!systemViewer.getStructuredSelection().isEmpty());
				
				miMoveUpSystem.setEnabled(!systemViewer.getStructuredSelection().isEmpty());
				btnMoveUpSystem.setEnabled(!systemViewer.getStructuredSelection().isEmpty());
				miMoveDownSystem.setEnabled(!systemViewer.getStructuredSelection().isEmpty());
				btnMoveDownSystem.setEnabled(!systemViewer.getStructuredSelection().isEmpty());
			});
			miEditSystem.setEnabled(!systemViewer.getStructuredSelection().isEmpty());
			btnEditSystem.setEnabled(!systemViewer.getStructuredSelection().isEmpty());
			miDeleteSystem.setEnabled(!systemViewer.getStructuredSelection().isEmpty());
			btnDeleteSystem.setEnabled(!systemViewer.getStructuredSelection().isEmpty());
			miMoveUpSystem.setEnabled(!systemViewer.getStructuredSelection().isEmpty());
			btnMoveUpSystem.setEnabled(!systemViewer.getStructuredSelection().isEmpty());
			miMoveDownSystem.setEnabled(!systemViewer.getStructuredSelection().isEmpty());
			btnMoveDownSystem.setEnabled(!systemViewer.getStructuredSelection().isEmpty());
		}
		
		
		loadDataJob.schedule();
		return main;
	}
	
	private void addUser() {
		QuickLink ql = new QuickLink();
		ql.setConservationArea(SmartDB.getCurrentConservationArea());
		ql.setEmployee(SmartDB.getCurrentEmployee());
		
		add(ql);
	}
	
	private void addSystem() {
		QuickLink ql = new QuickLink();
		ql.setConservationArea(SmartDB.getCurrentConservationArea());
		add(ql);
	}
	
	private void add(QuickLink ql) {

		EditQuickLinkDialog dialog = new EditQuickLinkDialog(this.getShell(), ql, allLanguages);
		if (dialog.open() != Window.OK) return;
		
		if (ql.getEmployee() == null) {
			ql.setUiOrder(this.systemLinks.size() + 1);
			this.systemLinks.add(ql);
		}
		if (ql.getEmployee() != null) {
			ql.setUiOrder(this.userLinks.size() + 1);
			this.userLinks.add(ql);
		}
		
		this.systemViewer.refresh();
		this.userViewer.refresh();
	}
	
	private void move(Object link, int dir) {
		
		if (link == null) return;
		if (!(link instanceof QuickLink)) return;
		QuickLink ql = (QuickLink)link;
		
		List<QuickLink> items = null;
		if (this.userLinks.contains(ql)) {
			items = this.userLinks;
		}else if (this.systemLinks.contains(ql)) {
			items = this.systemLinks;
		}
		
		if (items == null) return;
		
		
		int index = items.indexOf(ql);
		if (!items.remove(ql)) return;
		
		int newindex = index + dir;
		if (newindex < 0) newindex = 0;
		if (newindex > items.size()) newindex = items.size();
		items.add(newindex, ql);
		for (int i = 0; i < items.size(); i ++) items.get(i).setUiOrder(i+1);
		
		this.systemViewer.refresh();
		this.userViewer.refresh();
	}
	
	private void save() {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (QuickLink l : this.userLinks) session.saveOrUpdate(l);
				for (QuickLink l : this.systemLinks) session.saveOrUpdate(l);
				for (QuickLink l : this.removedLinks) session.delete(l);
				session.getTransaction().commit();
			}catch (Exception ex) {
				MessageDialog.openError(getShell(), DialogConstants.ERROR_STRING, MessageFormat.format(Messages.QuickLinkPreferencePage_SaveError, ex.getMessage()));
				loadDataJob.schedule();
				return;
			}
		}
	}
	private void edit(Object link) {
		if (link == null) return;
		if (!(link instanceof QuickLink)) return;
		QuickLink ql = (QuickLink)link;
		if (ql.getEmployee() != null && !canModifySystem()) return;
		
		EditQuickLinkDialog dialog = new EditQuickLinkDialog(this.getShell(), ql, allLanguages);
		if (dialog.open() != Window.OK) return;
		
		this.systemViewer.refresh();
		this.userViewer.refresh();
	}
	
	private void delete(Object link) {
		if (link == null) return;
		if (!(link instanceof QuickLink)) return;
		QuickLink ql = (QuickLink)link;
		if (ql.getEmployee() != null && !canModifySystem()) return;
		
		userLinks.remove(ql);
		systemLinks.remove(ql);
		removedLinks.add(ql);
		
		this.systemViewer.refresh();
		this.userViewer.refresh();
		
	}

	private TableViewer createTableViewer(Composite parent) {
		Composite composite2 = new Composite(parent, SWT.NONE);
		composite2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		//((GridData)composite2.getLayoutData()).heightHint = 150;

		TableColumnLayout tableLayout = new TableColumnLayout();
		composite2.setLayout(tableLayout);
		
		TableViewer tableViewer = new TableViewer(composite2, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);

		TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		viewerColumn.getColumn().setText(Messages.QuickLinkPreferencePage_NameCol);
		viewerColumn.getColumn().setResizable(true);
		viewerColumn.getColumn().setMoveable(true);

		tableLayout.setColumnData(viewerColumn.getColumn(), new ColumnWeightData(1, ColumnWeightData.MINIMUM_WIDTH, true));

		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof QuickLink) return ((QuickLink)element).getName();
				return super.getText(element);
			}
		});
		
		viewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		viewerColumn.getColumn().setText(Messages.QuickLinkPreferencePage_UrlCol);
		viewerColumn.getColumn().setResizable(true);
		viewerColumn.getColumn().setMoveable(true);

		tableLayout.setColumnData(viewerColumn.getColumn(), new ColumnWeightData(2, ColumnWeightData.MINIMUM_WIDTH, true));

		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof QuickLink) return ((QuickLink)element).getUrl();
				return super.getText(element);
			}
		});		
		
		
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);
		
		return tableViewer;
	}
	
	
	class EditQuickLinkDialog extends SmartStyledTitleDialog{

		private QuickLink link;
		private QuickLink copy;
		
		private Text txtUrl;
		private TableViewer tblNames;
				
		protected EditQuickLinkDialog(Shell parent, QuickLink link, List<Language> allLangs) {
			super(parent);
			this.link = link;
			copy = new QuickLink();
			copy.setUrl(link.getUrl());
			for (org.wcs.smart.ca.Label ll : link.getNames()) {
				copy.updateName(ll.getLanguage(), ll.getValue());				
			}
			for (Language l : allLangs) {
				if (copy.findNameNull(l) == null) {
					copy.updateName(l, ""); //$NON-NLS-1$
				}
			}
		}
		
		@Override
		protected void createButtonsForButtonBar(Composite parent){
			super.createButtonsForButtonBar(parent);
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			getButton(IDialogConstants.OK_ID).setText(DialogConstants.SAVE_TEXT);
		}
		
		public void okPressed() {
			if (getErrorMessage() != null) return;
			this.link.setUrl(txtUrl.getText().trim());
			
			Collection<org.wcs.smart.ca.Label> names = (Collection<org.wcs.smart.ca.Label>) tblNames.getInput();
			
			for (org.wcs.smart.ca.Label ll : names) {
				if (ll.getValue().trim().isBlank()) {
					
					org.wcs.smart.ca.Label toRemove = null;
					for (org.wcs.smart.ca.Label r : link.getNames()) {
						if (r.getLanguage().equals(ll.getLanguage())) {
							toRemove = r;
							break;
						}
					}
					if (toRemove != null) link.getNames().remove(toRemove);
				}else {
					link.updateName(ll.getLanguage(), ll.getValue().trim());
				}
			}
			
			if (link.findNameNull(SmartDB.getCurrentConservationArea().getDefaultLanguage()) == null) {
				link.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), link.getUrl());
			}
			link.setName(link.findName(SmartDB.getCurrentLanguage()));

			super.okPressed();
		}
		
		private void setDirty() {
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		}
		

		
		@Override
		protected Control createDialogArea(Composite parent) {
			setTitle(Messages.QuickLinkPreferencePage_Title);
			setMessage(Messages.QuickLinkPreferencePage_Message);
			Composite composite = (Composite) super.createDialogArea(parent);
			composite = new Composite(composite, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			composite.setLayout(new GridLayout(2, false));
			
			Label l = new Label(composite, SWT.NONE);
			l.setText(Messages.QuickLinkPreferencePage_NameLbl);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
			Composite temp = new Composite(composite, SWT.NONE);
			temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
			TableColumnLayout tableLayout = new TableColumnLayout();
			temp.setLayout(tableLayout);
			
			
			tblNames = new TableViewer(temp, SWT.FULL_SELECTION | SWT.BORDER);
			
			TableViewerColumn viewerColumn = new TableViewerColumn(tblNames,SWT.NONE);
			TableColumn column = viewerColumn.getColumn();
			tableLayout.setColumnData(column, new ColumnWeightData(34,ColumnWeightData.MINIMUM_WIDTH, true));
			column.setText(Messages.TranslateSimpleListItemDialog_LanguageLabel);
			column.setResizable(true);
			column.setMoveable(true);
			
			viewerColumn.setLabelProvider(new ColumnLabelProvider(){
				LanguageLabelProvider ll = new LanguageLabelProvider();
				@Override
				public String getText(Object element) {
					return ll.getText(((org.wcs.smart.ca.Label)element).getLanguage());
				}
				 
			});
			
			viewerColumn = new TableViewerColumn(tblNames,SWT.NONE);
			column = viewerColumn.getColumn();
			tableLayout.setColumnData(column, new ColumnWeightData(66,ColumnWeightData.MINIMUM_WIDTH, true));
			column.setText(Messages.TranslateSimpleListItemDialog_NameLabel);
			column.setResizable(true);
			column.setMoveable(true);
			viewerColumn.setLabelProvider(new ColumnLabelProvider(){
				@Override
				public String getText(Object element) {
					String x = ((org.wcs.smart.ca.Label)element).getValue();
					if (x == null){
						return ""; //$NON-NLS-1$
					}
					return x;
				}			 
			});
			
			viewerColumn.setEditingSupport(new EditingSupport(tblNames){

				@Override
				protected CellEditor getCellEditor(Object element) {
					return new TextCellEditor(tblNames.getTable());
				}

				@Override
				protected boolean canEdit(Object element) {
					return true;
				}

				@Override
				protected Object getValue(Object element) {
					return ((org.wcs.smart.ca.Label)element).getValue();
				}

				@Override
				protected void setValue(Object element, Object value) {
					String newValue = (String)value;
					String oldValue = ((org.wcs.smart.ca.Label)element).getValue();
					if (newValue.equals(oldValue)){
						//nothing to update
						return;
					}
					((org.wcs.smart.ca.Label)element).setValue((String)value);
					tblNames.refresh();
					EditQuickLinkDialog.this.setDirty();
				}
				
			});
	
			tblNames.setContentProvider(ArrayContentProvider.getInstance());
			tblNames.setInput(copy.getNames());
			tblNames.getTable().setHeaderVisible(true);
			tblNames.getTable().setLinesVisible(true);
			
			
			l = new Label(composite, SWT.NONE);
			l.setText(Messages.QuickLinkPreferencePage_UrlLbl);
			
			txtUrl = new Text(composite, SWT.BORDER);
			txtUrl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			if (copy.getUrl() != null) txtUrl.setText(copy.getUrl());
			txtUrl.addListener(SWT.Modify, e->setDirty());
		
		
			
			return composite;
		}
	}
	
}
