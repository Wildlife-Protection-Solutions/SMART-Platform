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
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
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
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.SmartStyledDialog;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**	
 * Dialog for listing and managing all ct packages.
 * 
 * @author Emily
 *
 */
public class ConfigurePackagesDialog extends SmartStyledTitleDialog {


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
		
		ToolBar tb = new ToolBar(parent, SWT.FLAT | SWT.RIGHT);
		tb.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		tb.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		ToolItem tiExport = new ToolItem(tb, SWT.PUSH);
		tiExport.setToolTipText(Messages.ConfigurePackagesDialog_exporttooltip);
		tiExport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EXPORT_ICON));
		tiExport.setText(DialogConstants.EXPORT_BUTTON_TEXT);
		tiExport.addListener(SWT.Selection, e->exportPackages());
		tiExport.setEnabled(false);
		
		ToolItem tiAdd = new ToolItem(tb, SWT.PUSH);
		tiAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		tiAdd.setToolTipText(Messages.ConfigurePackagesDialog_addtooltip);
		tiAdd.addListener(SWT.Selection, e->addPackage());
		tiAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		
		ToolItem tiDup = new ToolItem(tb, SWT.PUSH);
		tiDup.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CREATECOPY_ICON));
		tiDup.setToolTipText(Messages.ConfigurePackagesDialog_duplicatetooltip);
		tiDup.addListener(SWT.Selection, e->duplicatePackage());
		tiDup.setEnabled(false);
		tiDup.setText(Messages.ConfigurePackagesDialog_CreateCopyLabel);
		
		ToolItem tiEdit = new ToolItem(tb, SWT.PUSH);
		tiEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		tiEdit.setToolTipText(Messages.ConfigurePackagesDialog_edittooltip);
		tiEdit.addListener(SWT.Selection, e->editPackage());
		tiEdit.setEnabled(false);
		tiEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		
		ToolItem tiDelete = new ToolItem(tb, SWT.PUSH);
		tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiDelete.setToolTipText(Messages.ConfigurePackagesDialog_deleteTooltip);
		tiDelete.addListener(SWT.Selection, e->deletePackage());
		tiDelete.setEnabled(false);
		tiDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		
		SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sash.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		Composite t = new Composite(sash, SWT.NONE);
		TableColumnLayout tcl = new TableColumnLayout();
		t.setLayout(tcl);
		
		tblViewer = new TableViewer(t, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
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
			
			if (c == Column.TYPE) {
				tcl.setColumnData(gc.getColumn(), new ColumnPixelData(40,false));
			}else {
				tcl.setColumnData(gc.getColumn(), new ColumnWeightData(100, 150, true));
			}
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
				
				gc.getColumn().setText(prop.getLongName());
				tcl.setColumnData(gc.getColumn(), new ColumnWeightData(100, 150, true));
			}
			pp.addPropertyUpdatedListener(()->{
				Display.getDefault().asyncExec(()->{
					if (!tblViewer.getControl().isDisposed()) {
						tblViewer.refresh(true);
						updateDetails();
					}
				});	
			});
		}
		
		
		Menu mnu = new Menu(tblViewer.getControl());
		
		MenuItem miExport = new MenuItem(mnu, SWT.PUSH);
		miExport.setText(DialogConstants.EXPORT_BUTTON_TEXT); 
		miExport.addListener(SWT.Selection, e->okPressed());
		miExport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EXPORT_ICON));
		miExport.setEnabled(false);
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		MenuItem miAdd = new MenuItem(mnu, SWT.PUSH);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.setText(Messages.ConfigurePackagesDialog_AddMenuItem);
		miAdd.addListener(SWT.Selection, e->addPackage());
		
		MenuItem miDup = new MenuItem(mnu, SWT.PUSH);
		miDup.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CREATECOPY_ICON));
		miDup.setText(Messages.ConfigurePackagesDialog_CopyLabel);
		miDup.addListener(SWT.Selection, e->duplicatePackage());
		miDup.setEnabled(false);
		
		MenuItem miEdit = new MenuItem(mnu, SWT.PUSH);
		miEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEdit.setText(DialogConstants.EDIT_BUTTON_TEXT + "..."); //$NON-NLS-1$
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
			tiExport.setEnabled(canEdit);
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
		
		setMessage(Messages.ConfigurePackagesDialog_ShellMessage);
		setTitle(Messages.ConfigurePackagesDialog_ShellTitle);
		getShell().setText(Messages.ConfigurePackagesDialog_ShellTitle);

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
		for (Iterator<?> iterator = tblViewer.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object type = (Object) iterator.next();
			if (type instanceof ICtPackage) {
				items.add((ICtPackage)type);
			}
		}
		return items;
	}
	
	
	private void addPackage() {
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
		
		ICtPackage newpackage = null;
		try(Session session = HibernateManager.openSession()){
			ICtPackage tocopy = session.get(items.get(0).getClass(), items.get(0).getUuid());
			Hibernate.initialize(tocopy);
			newpackage = tocopy.copy();
		}
		editPackage(newpackage);
	}
	
	private boolean exportPackages() {
		ExportCtPackageManager mm = new ExportCtPackageManager(getShell());
		try {
			return mm.doExport(getSelection(), context);
		} catch (IOException ex) {
			CyberTrackerPlugIn.displayError(Messages.ConfigurePackagesDialog_ErrorTitle, Messages.ConfigurePackagesDialog_ErrorMessage + ex.getMessage(), ex);
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
		ContextInjectionFactory.inject(dialog, context);
		dialog.open();
		refresh();
	}
	
	private void deletePackage() {
		List<ICtPackage> items = getSelection();
		if (items.isEmpty()) return;
		if (!MessageDialog.openQuestion(getShell(), Messages.ConfigurePackagesDialog_DeleteTitle, MessageFormat.format(Messages.ConfigurePackagesDialog_DeleteConfirm, items.size()))){
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
				CyberTrackerPlugIn.displayError(Messages.ConfigurePackagesDialog_DeleteErrorTitle, Messages.ConfigurePackagesDialog_DeleteErrorMsg + ex.getMessage(), ex);
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
		TYPE (Messages.ConfigurePackagesDialog_TypeColumn),
		NAME (Messages.ConfigurePackagesDialog_NameColumn),
		PACKAGE_DATE(Messages.ConfigurePackagesDialog_DateColumn);
		
		
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
			if (!(x instanceof ICtPackage)) return ""; //$NON-NLS-1$
			ICtPackage pp = (ICtPackage)x;
			
			if (this == NAME) return pp.getName();
			if (this == TYPE) return "";//CtPackageExtensionPointManager.INSTANCE.findManager(pp).getTypeName(); //$NON-NLS-1$
			if (this == PACKAGE_DATE) {
				//search filestore for package and display date
				try {
					Path ctPackageFile = pp.getLocalFile();
					if (ctPackageFile == null) {
						return Messages.ConfigurePackagesDialog_NoPackageLabel;
					}else {
						String name = ctPackageFile.getFileName().toString();
						int index = name.indexOf('.', name.indexOf('.') + 1) + 1;
						String date = name.substring(index, index + ICtPackage.PACKAGE_DATE_FORMAT.length());
						DateTimeFormatter sdf = DateTimeFormatter.ofPattern(ICtPackage.PACKAGE_DATE_FORMAT);
						return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(LocalDateTime.parse(date,sdf));
					}
				}catch (Exception ex) {
					CyberTrackerPlugIn.log(ex.getMessage(), ex);
					return Messages.ConfigurePackagesDialog_Unknown;
				}
			}
			return ""; //$NON-NLS-1$
		}
	}
	
	private Job loadItems = new Job("Load ct packages") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Collection<ICtPackageManager> managers = CtPackageExtensionPointManager.INSTANCE.getPackageManagers();
			List<ICtPackage> packages = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				for (ICtPackageManager m : managers) {
					packages.addAll(m.getPackages(session));
				}
				packages.forEach(p->{
				
					p.getConservationArea().getFileDataStoreLocation();
					if (p instanceof AbstractCtPackage) {
						((AbstractCtPackage)p).getMetadataValues().forEach(md->{
							if (md.getUuidList() != null) md.getUuidList().forEach(ui->ui.getUuidValue());
						});
					}
				});
			}
			packages.sort((a,b)->a.getName().compareTo(b.getName()));
			Display.getDefault().asyncExec(()->{
				tblViewer.setInput(packages);	
				getShell().layout(true, true);
			});
			
			return Status.OK_STATUS;
		}
		
	};
	
	private class SelectionDialog<T> extends SmartStyledDialog{

		private T selection;
		private TableViewer cmbItems;
		private Collection<T> items;
		
		public SelectionDialog(Shell parentShell, Collection<T> items) {
			super(parentShell);
			this.items = items;
		}
		
		@SuppressWarnings("unchecked")
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
			l.setText(Messages.ConfigurePackagesDialog_PackageTypeMsg);
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
					if (element instanceof ICtPackageManager) return ((ICtPackageManager) element).getTypeName();
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
			
			getShell().setText(Messages.ConfigurePackagesDialog_PackageTypeTitle);
			return parent;
		}
	}
}
