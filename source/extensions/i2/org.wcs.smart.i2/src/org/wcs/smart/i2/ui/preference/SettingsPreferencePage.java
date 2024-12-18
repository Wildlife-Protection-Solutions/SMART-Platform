/*
 * Copyright (C) 2016 Wildlife Conservation Society
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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.birt.report.designer.ui.widget.ComboBoxCellEditor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
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
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelConfigurationOption;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for configuration menu labels
 * 
 * @author Emily
 *
 */
public class SettingsPreferencePage extends PreferencePage implements IIntelPreferencePage{

	private TableViewer viewer;
	private List<Name> items = null;
	private List<Name> toDelete = null;
	
	
	public SettingsPreferencePage() {
		super(Messages.SettingsPreferencePage_PageName, Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_CONFIGURE));
		noDefaultAndApplyButton();
		toDelete = new ArrayList<>();
	}

	protected void save() {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for(Name n : toDelete) {
					if (n.op.getUuid() != null) session.remove(n.op);
				}
				session.flush();
				for (Name n : items) {
					if (n.op.getUuid() != null) {
						n.op = session.merge(n.op);
					}
				}
				session.flush();
				for (Name n : items) {
					if (n.op.getUuid() == null) {
						session.persist(n.op);
					}
				}
				session.getTransaction().commit();
				toDelete.clear();
			}catch (Exception ex ){
				session.getTransaction().rollback();
				Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
				
			}
		}
	}
	
	
	private void modified(){
		setErrorMessage(null);
	
		int cnt = 0;
		for (Name item : items) {
			if (item.isDefault()) cnt ++;
		}
		if (cnt > 1) {
			setErrorMessage(Messages.ConfigurationDialog_SingleDefaultRequired);
			return;
		}else {
			HashSet<String> lang = new HashSet<>();
			for (Name item : items) {
				String code = item.getLanguage();
				if (code != null) {
					boolean fnd = false;
					for (String s : Locale.getISOLanguages()) {
						if (s.equals(code)) {
							fnd = true;
							break;
						}
					}
					if (!fnd) {
						setErrorMessage(MessageFormat.format(Messages.ConfigurationDialog_InvalidLanguageCode,item.getLanguage()));
						return;
					}
				}
				
				if (lang.contains(item.getLanguage())) {
					setErrorMessage(MessageFormat.format(Messages.ConfigurationDialog_DuplicateLanguage,item.getLanguage()));
					return;
				}
				lang.add(item.getLanguage());
			}
		}
		save();
	}
	
	
	@Override
	protected Control createContents(Composite parent) {
		
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite c = SmartUiUtils.createHeaderLabel(parent, Messages.ConfigurationDialog_MenuLabel);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		Label l = new Label(parent, SWT.WRAP);
		l.setText(Messages.ConfigurationDialog_MenuInfo);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridData)l.getLayoutData()).widthHint = 200;
		
		viewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(true);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		TableViewerColumn colLanguage = new TableViewerColumn(viewer,  SWT.NONE);
		colLanguage.getColumn().setText(Messages.ConfigurationDialog_LanguageColumn);
		colLanguage.getColumn().setWidth(200);
		colLanguage.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof Name) {
					String l = ((Name)element).getLanguage();
					if (l == null) return Messages.ConfigurationDialog_defaultLabel;
					for (Locale x : Locale.getAvailableLocales()) {
						if (x.getLanguage().equals(l)) return l + " (" + x.getDisplayLanguage() + ") "; //$NON-NLS-1$ //$NON-NLS-2$
					}
					return l;
				}
				return super.getText(element);
			}
		});
		final String[] langs = new String[Locale.getISOLanguages().length+1];
		langs[0] = ""; //$NON-NLS-1$
		for (int i = 0; i < Locale.getISOLanguages().length; i ++) {
			Locale tmp = Locale.forLanguageTag(Locale.getISOLanguages()[i]);
			StringBuilder sb = new StringBuilder();
			sb.append(Locale.getISOLanguages()[i]);
			if (tmp != null) sb.append(" (" + tmp.getDisplayName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			langs[i+1] = sb.toString();
			
		}
		colLanguage.setEditingSupport(new EditingSupport(colLanguage.getViewer()) {
			@Override
			protected CellEditor getCellEditor(Object element) {
				
				return new ComboBoxCellEditor(viewer.getTable(), langs);
			}

			@Override
			protected boolean canEdit(Object element) {
				return element instanceof Name;
			}

			@Override
			protected Object getValue(Object element) {
				if (element instanceof Name) {
					return ((Name) element).getLanguage();
				}
				return null;
			}

			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof Name) {
					if (value.toString().isEmpty()) {
						((Name)element).op.setKey(IntelConfigurationOption.MENU_NAME_KEY);
					}else {
						value = value.toString().subSequence(0, 2);
						((Name)element).op.setKey(IntelConfigurationOption.MENU_NAME_KEY + "." + value.toString()); //$NON-NLS-1$
					}
					viewer.refresh();
					modified();
				}
			}
			
		});
		
		TableViewerColumn colLabel = new TableViewerColumn(viewer,  SWT.NONE);
		colLabel.getColumn().setText(Messages.ConfigurationDialog_LabelLabel);
		colLabel.getColumn().setWidth(200);
		colLabel.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof Name) {
					return ((Name)element).getName();
				}
				return super.getText(element);
			}
		});
		colLabel.setEditingSupport(new EditingSupport(colLabel.getViewer()) {
			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(viewer.getTable());
			}

			@Override
			protected boolean canEdit(Object element) {
				return element instanceof Name;
			}

			@Override
			protected Object getValue(Object element) {
				if (element instanceof Name) {
					return ((Name) element).getName();
				}
				return null;
			}

			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof Name) {
					((Name)element).op.setValue(value.toString());
					viewer.refresh();
					modified();
				}
			}
			
		});
		
		
		Composite buttonPanel = new Composite(parent, SWT.NONE);
		buttonPanel.setLayout(new GridLayout());
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Button btnAdd = new Button(buttonPanel, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAdd.addListener(SWT.Selection, e->add());
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		btnAdd.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		Button btnDelete = new Button(buttonPanel, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDelete.addListener(SWT.Selection, e->delete());
		btnDelete.setEnabled(false);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		btnDelete.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		Menu m = new Menu(viewer.getControl());
		
		MenuItem miAdd = new MenuItem(m, SWT.PUSH);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.addListener(SWT.Selection, e->add());
		
		MenuItem miDelete = new MenuItem(m, SWT.PUSH);
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.addListener(SWT.Selection, e->delete());
		miDelete.setEnabled(false);
		
		viewer.getControl().setMenu(m);
		
		viewer.addSelectionChangedListener(e->{
			boolean canDelete = false;
			for (Iterator<?> iterator = viewer.getStructuredSelection().iterator(); iterator.hasNext();) {
				Object x = iterator.next();
				if (x instanceof Name) {
					canDelete = true;
					break;
				}
			}
			
			miDelete.setEnabled(canDelete);
			btnDelete.setEnabled(canDelete);
		});
		
		loadConfigs.schedule();
		SmartUiUtils.makeTransparent(parent);
		setMessage(Messages.SettingsPreferencePage_PageTitle);
		l.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK));

		return parent;
	}

	private void add() {
		if (items == null) return;
		IntelConfigurationOption op = new IntelConfigurationOption();
		op.setConservationArea(SmartDB.getCurrentConservationArea());
		op.setValue(Messages.ConfigurationDialog_DefaultName);
		op.setKey(IntelConfigurationOption.MENU_NAME_KEY);
		items.add(new Name(op));
		viewer.refresh();
		modified();
	}
	
	private void delete() {
		for (Iterator<?> iterator = viewer.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof Name) {
				toDelete.add((Name)x);
				items.remove((Name)x);
			}
		}
		viewer.refresh();
		modified();
	}
	
	
	private class Name{
		public IntelConfigurationOption op;
		
		public Name(IntelConfigurationOption o) {
			this.op = o;
		}
		
		public boolean isDefault() {
			return op.getKey().equals(IntelConfigurationOption.MENU_NAME_KEY);
		}
		public String getName() {
			return op.getValue();
		}
		public String getLanguage() {
			if (op.getKey().equals(IntelConfigurationOption.MENU_NAME_KEY)) return null;
			return op.getKey().substring(IntelConfigurationOption.MENU_NAME_KEY.length() + 1);
		}
	}

	private Job loadConfigs = new Job(Messages.ConfigurationDialog_JobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Name> items = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				List<IntelConfigurationOption> options = QueryFactory.buildQuery(session, IntelConfigurationOption.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
				for (IntelConfigurationOption o : options) {
					if (o.getKey().startsWith(IntelConfigurationOption.MENU_NAME_KEY)) {
						items.add(new Name(o));
					}
				}
			}
			SettingsPreferencePage.this.items = items;
			Display.getDefault().syncExec(()->{
				viewer.setInput(SettingsPreferencePage.this.items);
			});
			return Status.OK_STATUS;
		}
		
	};

	@Override
	public void refresh() {
	}
	
}