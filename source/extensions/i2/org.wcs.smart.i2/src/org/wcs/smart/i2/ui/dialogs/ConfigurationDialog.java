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
package org.wcs.smart.i2.ui.dialogs;

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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
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
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelConfigurationOption;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for configuration menu labels
 * 
 * @author Emily
 *
 */
public class ConfigurationDialog extends TitleAreaDialog{

	private TableViewer viewer;
	private List<Name> items = null;
	private List<Name> toDelete = null;
	
	private boolean modified = false;
	public ConfigurationDialog(Shell parentShell) {
		super(parentShell);
		toDelete = new ArrayList<>();
	}

	@Override
	protected void okPressed() {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for(Name n : toDelete) {
					if (n.op.getUuid() != null) session.delete(n.op);
				}
				
				for (Name n : items) {
					session.saveOrUpdate(n.op);
				}
				session.getTransaction().commit();
				toDelete.clear();
				modified = true;
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			}catch (Exception ex ){
				session.getTransaction().rollback();
			}
		}
	}
	
	@Override
	protected void cancelPressed() {
		if (modified) {
			//TODO: update menu label
		}
		super.cancelPressed();
	}
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT,true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	private void modified(){
		setErrorMessage(null);
		boolean isError = false;
		
		int cnt = 0;
		for (Name item : items) {
			if (item.isDefault()) cnt ++;
		}
		if (cnt > 1) {
			isError = true;
			setErrorMessage("Only a single default value (value without language) can be provided.");
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
						isError = true;
						setErrorMessage(MessageFormat.format("Invalid language code: {0}",item.getLanguage()));
						break;
					}
				}
				if (lang.contains(item.getLanguage())) {
					isError = true;
					setErrorMessage(MessageFormat.format("Duplicate labels for language: {0}",item.getLanguage()));
					break;
				}
			}
		}
		getButton(IDialogConstants.OK_ID).setEnabled(!isError);
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(parent, SWT.NONE);
		l.setText("Menu Label:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		l = new Label(parent, SWT.WRAP);
		l.setText("Select no language to select the default label.  This is the value that will be displayed if a language specific value is not found. Remove all values if you want to use the modules default values.");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridData)l.getLayoutData()).widthHint = 200;
		
		viewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(true);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		TableViewerColumn colLanguage = new TableViewerColumn(viewer,  SWT.NONE);
		colLanguage.getColumn().setText("Language");
		colLanguage.getColumn().setWidth(200);
		colLanguage.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof Name) {
					String l = ((Name)element).getLanguage();
					if (l == null) return "<default>";
					for (Locale x : Locale.getAvailableLocales()) {
						if (x.getLanguage().equals(l)) return l + " (" + x.getDisplayLanguage() + ") ";
					}
					return l;
				}
				return super.getText(element);
			}
		});
		final String[] langs = new String[Locale.getISOLanguages().length+1];
		langs[0] = "";
		for (int i = 0; i < Locale.getISOLanguages().length; i ++) {
			langs[i+1] = Locale.getISOLanguages()[i];
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
						((Name)element).op.setKey(IntelConfigurationOption.MENU_NAME_KEY + "." + value.toString());
					}
					viewer.refresh();
					modified();
				}
			}
			
		});
		
		TableViewerColumn colLabel = new TableViewerColumn(viewer,  SWT.NONE);
		colLabel.getColumn().setText("Label");
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
		
		Button btnAdd = new Button(buttonPanel, SWT.NONE);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.addListener(SWT.Selection, e->add());
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Button btnDelete = new Button(buttonPanel, SWT.NONE);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.addListener(SWT.Selection, e->delete());
		btnDelete.setEnabled(false);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Menu m = new Menu(viewer.getControl());
		
		MenuItem miAdd = new MenuItem(m, SWT.PUSH);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(DialogConstants.ADD_BUTTON_TEXT));
		miAdd.addListener(SWT.Selection, e->add());
		
		MenuItem miDelete = new MenuItem(m, SWT.PUSH);
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(DialogConstants.DELETE_BUTTON_TEXT));
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
		
		setTitle("Configuration");
		setMessage("Configurate menu label for multi languages");
		getShell().setText("Configuration");
		return parent;
	}

	private void add() {
		if (items == null) return;
		IntelConfigurationOption op = new IntelConfigurationOption();
		op.setConservationArea(SmartDB.getCurrentConservationArea());
		op.setValue("Advanced Intelligence");
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
	
	@Override
	public boolean isResizable(){
		return true;
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

	private Job loadConfigs = new Job("loading configuration options") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Name> items = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				List<IntelConfigurationOption> options = QueryFactory.buildQuery(session, IntelConfigurationOption.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list();
				for (IntelConfigurationOption o : options) {
					if (o.getKey().startsWith(IntelConfigurationOption.MENU_NAME_KEY)) {
						items.add(new Name(o));
					}
				}
			}
			ConfigurationDialog.this.items = items;
			Display.getDefault().syncExec(()->{
				viewer.setInput(ConfigurationDialog.this.items);
			});
			return Status.OK_STATUS;
		}
		
	};
	
}