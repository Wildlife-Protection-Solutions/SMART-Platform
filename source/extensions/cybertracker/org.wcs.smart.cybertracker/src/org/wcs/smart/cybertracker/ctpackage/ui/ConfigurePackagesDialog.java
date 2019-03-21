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
package org.wcs.smart.cybertracker.ctpackage.ui;

import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
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
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for listing and managing all ct packages.
 * 
 * @author Emily
 *
 */
public class ConfigurePackagesDialog extends TitleAreaDialog {


	private TableViewer tblViewer;
	
	private Composite detailsSection;
	private List<ICtPackagePropertyProvider> propertyproviders;
	
	@Inject
	private IEclipseContext context;
	
	public ConfigurePackagesDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void okPressed() {
		if (getSelection().isEmpty()) return;
		if (!exportPackages()) return;
	}

	@Override
	public Point getInitialSize() {
		return new Point(900,500);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button btnExport = createButton(parent, IDialogConstants.OK_ID, DialogConstants.EXPORT_BUTTON_TEXT, true);
		btnExport.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);		
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		propertyproviders = CtPackageExtensionPointManager.INSTANCE.getPackageProperties();
		propertyproviders.forEach(pp->ContextInjectionFactory.inject(pp, context));
		
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout());
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		parent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		ToolBar tb = new ToolBar(parent, SWT.FLAT);
		tb.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		tb.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		ToolItem tiAdd = new ToolItem(tb, SWT.PUSH);
		tiAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		tiAdd.setToolTipText("create a new CyberTracker package");
		tiAdd.addListener(SWT.Selection, e->addPackage());
		
		ToolItem tiDup = new ToolItem(tb, SWT.PUSH);
		tiDup.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CREATECOPY_ICON));
		tiDup.setToolTipText("create a new CyberTracker package copying settings from selected package");
		tiDup.addListener(SWT.Selection, e->duplicatePackage());
		tiDup.setEnabled(false);
		
		ToolItem tiEdit = new ToolItem(tb, SWT.PUSH);
		tiEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		tiEdit.setToolTipText("edit CyberTracker package");
		tiEdit.addListener(SWT.Selection, e->editPackage());
		tiEdit.setEnabled(false);
		
		ToolItem tiDelete = new ToolItem(tb, SWT.PUSH);
		tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiDelete.setToolTipText("delete CyberTracker package");
		tiDelete.addListener(SWT.Selection, e->deletePackage());
		tiDelete.setEnabled(false);
		
		SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sash.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		tblViewer = new TableViewer(sash, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		tblViewer.setContentProvider(ArrayContentProvider.getInstance());
		tblViewer.getTable().setHeaderVisible(true);
		for (Column c : Column.values()) {
			TableViewerColumn gc = new TableViewerColumn(tblViewer, SWT.NONE);
			gc.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return c.getValue(element);
				}
				public Image getImage(Object element) {
					return c.getImage(element); 
				}
			});
			gc.getColumn().setText(c.guiName);
			gc.getColumn().setWidth(200);
		}
		for (ICtPackagePropertyProvider pp : propertyproviders) {
			for (ICtPackageProperty prop : pp.getProperties()) {
				if (!prop.showInSummaryTable()) continue;
				TableViewerColumn gc = new TableViewerColumn(tblViewer, SWT.NONE);
				gc.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ICtPackage)
							return prop.getValue((ICtPackage)element);
						return super.getText(element);
					}
					
				});
				gc.getColumn().setText(prop.getName());
				gc.getColumn().setWidth(200);
			}
			pp.addPropertyUpdatedListener(()->{
				Display.getDefault().asyncExec(()->tblViewer.refresh());	
			});
		}
		Menu mnu = new Menu(tblViewer.getControl());
		
		MenuItem miExport = new MenuItem(mnu, SWT.PUSH);
		miExport.setText("Export...");
		miExport.addListener(SWT.Selection, e->okPressed());
		miExport.setEnabled(false);
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		MenuItem miAdd = new MenuItem(mnu, SWT.PUSH);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.setText("New...");
		miAdd.addListener(SWT.Selection, e->addPackage());
		
		MenuItem miDup = new MenuItem(mnu, SWT.PUSH);
		miDup.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CREATECOPY_ICON));
		miDup.setText("Create Copy...");
		miDup.addListener(SWT.Selection, e->duplicatePackage());
		miDup.setEnabled(false);
		
		MenuItem miEdit = new MenuItem(mnu, SWT.PUSH);
		miEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEdit.setText(DialogConstants.EDIT_BUTTON_TEXT + "...");
		miEdit.addListener(SWT.Selection, e->editPackage());
		miEdit.setEnabled(false);
		
		MenuItem miDelete = new MenuItem(mnu, SWT.PUSH);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.addListener(SWT.Selection, e->deletePackage());
		miDelete.setEnabled(false);
		
		tblViewer.getControl().setMenu(mnu);
		
		tblViewer.addSelectionChangedListener(e->{
			boolean canEdit = !tblViewer.getSelection().isEmpty();
			miExport.setEnabled(canEdit);
//			miCreate.setEnabled(canEdit);
			miDup.setEnabled(canEdit);
			miEdit.setEnabled(canEdit);
			miDelete.setEnabled(canEdit);
			tiDup.setEnabled(canEdit);
			tiEdit.setEnabled(canEdit);
			tiDelete.setEnabled(canEdit);
//			for (MenuItem i : others) {
//				i.setEnabled(canEdit);
//			}
			
			getButton(IDialogConstants.OK_ID).setEnabled(canEdit);
			updateDetails();
		});
		tblViewer.setInput(DialogConstants.LOADING_TEXT);
		tblViewer.getTable().setFocus();
		
		detailsSection = new Composite(sash, SWT.BORDER);
		detailsSection.setLayout(new GridLayout());
		detailsSection.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		sash.setWeights(new int[] {7,3});
		
		loadItems.schedule();
		
		setMessage("Export and manage the CyberTracker packages");
		setTitle("SMART Cybertracker Packages");
		getShell().setText("SMART Cybertracker Packages");
		
		return parent;
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
	

	private void updateDetails() {
		for (Control c : detailsSection.getChildren()) c.dispose();
		Object x = tblViewer.getStructuredSelection().getFirstElement();
		if (x == null) return;
		if (!(x instanceof ICtPackage)) return;
		ICtPackage ctpackage = (ICtPackage)x;
		ICtPackageConfigurator cc = CtPackageExtensionPointManager.INSTANCE.findManager(ctpackage).createConfigurator();
		
		Composite details = cc.createDetails(detailsSection, ctpackage, propertyproviders);
		
		details.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		detailsSection.layout(true);
	}
	
	private List<ICtPackage> getSelection(){
		List<ICtPackage> items = new ArrayList<>();
		for (Iterator<Object> iterator = tblViewer.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object type = (Object) iterator.next();
			if (type instanceof ICtPackage) {
				items.add((ICtPackage)type);
			}
		}
		return items;
	}
	
	
	private void addPackage() {
		//TODO: force single package
		//make users select the type then create new items
		Collection<ICtPackageManager> types = CtPackageExtensionPointManager.INSTANCE.getPackageManagers();
		if (types.isEmpty()) return;
		
		ICtPackageManager m = null;
		if (types.size() == 1) {
			//can only create this one type
			m = types.iterator().next();
		}else {
			SelectionDialog<ICtPackageManager> dialog = new SelectionDialog<>(getShell(), types);
			if (dialog.open() != Window.OK) return;
			if (dialog.getSelection() == null) return;
			m = (ICtPackageManager) dialog.getSelection();
		}
		editPackage(m.createPackage());
	}
	
	private void duplicatePackage() {
		List<ICtPackage> items = getSelection();
		if (items.isEmpty()) return;
		
		ICtPackage newpackage = items.get(0).copy();
		editPackage(newpackage);
	}
	
	private boolean exportPackages() {
		ExportCtPackageManager mm = new ExportCtPackageManager(getShell());
		try {
			return mm.doExport(getSelection(), context);
		} catch (IOException ex) {
			CyberTrackerPlugIn.displayError("Error", "Error exporting CyberTracker packages: " + ex.getMessage(), ex);
			return false;
		}finally {
			propertyproviders.forEach(pp->pp.refresh());
			tblViewer.refresh();
			updateDetails();
		}
	}
	
	private void editPackage() {
		List<ICtPackage> items = getSelection();
		if (items.isEmpty()) return;
		editPackage(items.get(0));
	}
	
	private void editPackage(ICtPackage item) {
		ConfigurePackageDialog dialog = new ConfigurePackageDialog(getShell(), item);
		dialog.open();
		refresh();
	}
	
	private void deletePackage() {
		List<ICtPackage> items = getSelection();
		if (items.isEmpty()) return;
		if (!MessageDialog.openQuestion(getShell(), "Delete", MessageFormat.format("Are you sure you want to delete the {0} selected packages.  This action cannot be undone.", items.size()))){
			return;
		}
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (ICtPackage p : items) {
					CtPackageExtensionPointManager.INSTANCE.findManager(p).deletePackage(p, session);
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				CyberTrackerPlugIn.displayError("Delete Error", "Unable to delete selected configurations: " + ex.getMessage(), ex);
				session.getTransaction().rollback();
			}
			
		}
		refresh();
	}
	
	private void refresh() {
		loadItems.schedule();
		propertyproviders.forEach(pp->pp.refresh());
		updateDetails();
	}
	
	private enum Column{
		TYPE ("Type"),
		NAME ("Name"),
		PACKAGE_DATE("Local Package Date");
		
		
		String guiName;
		
		Column(String name) {
			this.guiName = name;
		}
		
		public Image getImage(Object x) {
			if (this != TYPE) return null;
			if (!(x instanceof ICtPackage)) return null;
			ICtPackage pp = (ICtPackage)x;
			return CtPackageExtensionPointManager.INSTANCE.findManager(pp).getTypeImage();
		}
		
		public String getValue(Object x) {
			if (!(x instanceof ICtPackage)) return "";
			ICtPackage pp = (ICtPackage)x;
			
			if (this == NAME) return pp.getName();
			if (this == TYPE) return "";//CtPackageExtensionPointManager.INSTANCE.findManager(pp).getTypeName();
			if (this == PACKAGE_DATE) {
				//search filestore for package and display date
				try {
					Path ctPackageFile = pp.getLocalFile();
					if (ctPackageFile == null) {
						return "No Package";
					}else {
						String name = ctPackageFile.getFileName().toString();
						int index = name.indexOf('.', name.indexOf('.') + 1) + 1;
						String date = name.substring(index, index + 17);
						SimpleDateFormat sdf = new SimpleDateFormat(ICtPackage.PACKAGE_DATE_FORMAT);
						return DateFormat.getDateTimeInstance().format(sdf.parse(date));
					}
				}catch (Exception ex) {
					CyberTrackerPlugIn.log(ex.getMessage(), ex);
					return "unknown";
				}
			}
			return "";
		}
	}
	
	private Job loadItems = new Job("Load ct packages") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Collection<ICtPackageManager> managers = CtPackageExtensionPointManager.INSTANCE.getPackageManagers();
			List<ICtPackage> packages = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				for (ICtPackageManager m : managers) {
					packages.addAll(m.getPackages(session));
				}
				packages.forEach(p->p.getConservationArea().getFileDataStoreLocation());
			}
			packages.sort((a,b)->a.getName().compareTo(b.getName()));
			Display.getDefault().asyncExec(()->{
				tblViewer.setInput(packages);
				
				for (TableColumn tc: tblViewer.getTable().getColumns()) {
					tc.pack();
					tc.setWidth((int)(tc.getWidth() * 1.1));					
				}
				
			});
			
			return Status.OK_STATUS;
		}
		
	};
	
	private class SelectionDialog<T> extends Dialog{

		private T selection;
		private TableViewer cmbItems;
		private Collection<T> items;
		
		public SelectionDialog(Shell parentShell, Collection<T> items) {
			super(parentShell);
			this.items = items;
		}
		
		@Override
		protected void okPressed() {
			selection = (T)cmbItems.getStructuredSelection().getFirstElement();
			super.okPressed();
		}
		@Override
		public Point getInitialSize() {
			Point p = super.getInitialSize();
			if (p.x < 400) p.x = 400;
			if (p.y < 200) p.y = 300;
			return p;
		}
		
		@Override
		public boolean isResizable() {
			return true;
		}
		
		public T getSelection() {
			return selection;
		}

		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
			Button btnOk = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
			btnOk.setEnabled(false);
		}
		
		@Override
		protected Control createDialogArea(Composite parent) {
			parent = (Composite) super.createDialogArea(parent);
			parent.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
			
			Composite main = new Composite(parent, SWT.NONE);
			main.setLayout(new GridLayout());
			main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			main.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			
			Label l = new Label(main, SWT.NONE);
			l.setText("Select the type of package to create:");
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			l.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			
			Composite temp = new Composite(main, SWT.NONE);
			temp.setLayout(new TableColumnLayout());
			temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			cmbItems = new TableViewer(temp, SWT.FULL_SELECTION | SWT.BORDER);
			cmbItems.getTable().setHeaderVisible(false);
			cmbItems.getTable().setLinesVisible(false);
			
			cmbItems.setContentProvider(ArrayContentProvider.getInstance());
			
			TableViewerColumn tc = new TableViewerColumn(cmbItems, SWT.NONE);
			tc.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof ICtPackageManager) return ((ICtPackageManager) element).getTypeIdentifier();
					return super.getText(element);
				}
				
				@Override
				public Image getImage(Object element) {
					if (element instanceof ICtPackageManager) return ((ICtPackageManager) element).getTypeImage();
					return super.getImage(element);
				}
			});
			((TableColumnLayout)temp.getLayout()).setColumnData(tc.getColumn(), new ColumnWeightData(10));
			
			cmbItems.setInput(items);
			cmbItems.addSelectionChangedListener(e->getButton(IDialogConstants.OK_ID).setEnabled(!cmbItems.getSelection().isEmpty()));
			cmbItems.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent event) {
					okPressed();
				}
			});
			
			getShell().setText("Cybertracker Package Type");
			return parent;
		}
	}
}
