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
package org.wcs.smart.cybertracker.navigation.ui;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
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
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.NavigationLayer;
import org.wcs.smart.cybertracker.model.NavigationTarget;
import org.wcs.smart.cybertracker.navigation.ExportNavigationManager;
import org.wcs.smart.cybertracker.navigation.INavigationLayerProperty;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for managing navigation layers
 *  
 * @author Emily
 * @since 7.0.0
 *
 */
public class NavigationLayersDialog extends SmartStyledTitleDialog {

	private TableViewer tblViewer;
	private Composite detailsSection;
	
	private List<INavigationLayerProperty> propertyproviders;
	
	@Inject
	private IEclipseContext context;
	
	public NavigationLayersDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void okPressed() {
		if (getSelection().isEmpty()) return;
		if (!exportNavigation()) return;
		super.okPressed();
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
		propertyproviders = ExportNavigationManager.INSTANCE.getPackageProperties();
		propertyproviders.forEach(pp->ContextInjectionFactory.inject(pp, context));
		
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout());
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		parent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		ToolBar tb = new ToolBar(parent, SWT.FLAT | SWT.RIGHT);
		tb.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		tb.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		ToolItem tiExport = new ToolItem(tb, SWT.PUSH | SWT.RIGHT);
		tiExport.setText(DialogConstants.EXPORT_BUTTON_TEXT);
		tiExport.setToolTipText(Messages.NavigationLayersDialog_exporttooltip);
		tiExport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EXPORT_ICON));
		tiExport.addListener(SWT.Selection, e->{
			exportNavigation();
			refresh();
		});
		tiExport.setEnabled(false);
		
		ToolItem tiAdd = new ToolItem(tb, SWT.PUSH );
		tiAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		tiAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		tiAdd.setToolTipText(Messages.NavigationLayersDialog_createtooltip);
		tiAdd.addListener(SWT.Selection, e->addNavigation());
		
		ToolItem tiDup = new ToolItem(tb, SWT.PUSH);
		tiDup.setText(Messages.NavigationLayersDialog_Copy);
		tiDup.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CREATECOPY_ICON));
		tiDup.setToolTipText(Messages.NavigationLayersDialog_copytooltip);
		tiDup.addListener(SWT.Selection, e->duplicateNavigation());
		tiDup.setEnabled(false);
		
		ToolItem tiEdit = new ToolItem(tb, SWT.PUSH);
		tiEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		tiEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		tiEdit.setToolTipText(Messages.NavigationLayersDialog_editooltip);
		tiEdit.addListener(SWT.Selection, e->editNavigation());
		tiEdit.setEnabled(false);
		
		ToolItem tiDelete = new ToolItem(tb, SWT.PUSH);
		tiDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiDelete.setToolTipText(Messages.NavigationLayersDialog_deletetooltip);
		tiDelete.addListener(SWT.Selection, e->deletePackage());
		tiDelete.setEnabled(false);
		
		SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sash.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		Composite t = new Composite(sash, SWT.NONE);
		TableColumnLayout tcl = new TableColumnLayout();
		t.setLayout(tcl);
		
		tblViewer = new TableViewer(t, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		tblViewer.setContentProvider(ArrayContentProvider.getInstance());
		tblViewer.getTable().setHeaderVisible(true);
		tblViewer.addDoubleClickListener(e->{
			editNavigation();
		});
	
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
			tcl.setColumnData(gc.getColumn(), new ColumnWeightData(100));		
		}
		for (INavigationLayerProperty pp : propertyproviders) {
			TableViewerColumn gc = new TableViewerColumn(tblViewer, SWT.NONE);
			gc.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof NavigationLayer)
						return pp.getValue((NavigationLayer)element);
					return super.getText(element);
				}
				
			});
			gc.getColumn().setText(pp.getName());
			tcl.setColumnData(gc.getColumn(), new ColumnWeightData(100, 150, true));
		
			pp.addPropertyUpdatedListener(() -> {
				Display.getDefault().asyncExec(() -> {
					if (!tblViewer.getControl().isDisposed())
						tblViewer.refresh();
				});
			});
		}
		
		
		Menu mnu = new Menu(tblViewer.getControl());
		
		MenuItem miExport = new MenuItem(mnu, SWT.PUSH);
		miExport.setText(DialogConstants.EXPORT_BUTTON_TEXT); 
		miExport.addListener(SWT.Selection, e->{
			exportNavigation();
			refresh();
		});
		miExport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EXPORT_ICON));
		miExport.setEnabled(false);
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		MenuItem miAdd = new MenuItem(mnu, SWT.PUSH);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT + "..."); //$NON-NLS-1$
		miAdd.addListener(SWT.Selection, e->addNavigation());
		
		MenuItem miDup = new MenuItem(mnu, SWT.PUSH);
		miDup.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CREATECOPY_ICON));
		miDup.setText(Messages.NavigationLayersDialog_CopyMenu);
		miDup.addListener(SWT.Selection, e->duplicateNavigation());
		miDup.setEnabled(false);
		
		MenuItem miEdit = new MenuItem(mnu, SWT.PUSH);
		miEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEdit.setText(DialogConstants.EDIT_BUTTON_TEXT + "..."); //$NON-NLS-1$
		miEdit.addListener(SWT.Selection, e->editNavigation());
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
			miDup.setEnabled(canEdit);
			miEdit.setEnabled(canEdit);
			miDelete.setEnabled(canEdit);
			tiDup.setEnabled(canEdit);
			tiEdit.setEnabled(canEdit);
			tiDelete.setEnabled(canEdit);
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
		
		setMessage(Messages.NavigationLayersDialog_DialogMessage);
		setTitle(Messages.NavigationLayersDialog_DialogTitle);
		getShell().setText(Messages.NavigationLayersDialog_ShellTitle);
		
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
		if (!(x instanceof NavigationLayer)) return;
		NavigationLayer ctpackage = (NavigationLayer)x;
		
		Label l = new Label(detailsSection, SWT.NONE);
		l.setText(ctpackage.getName());
		l.setBackground(l.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		FontData fd = l.getFont().getFontData()[0];
		fd.setHeight(fd.getHeight() + 1);
		fd.setStyle(SWT.BOLD);
		Font f = new Font(l.getDisplay(),fd);
		l.addListener(SWT.Dispose, e->f.dispose());
		l.setFont(f);
		
		l = new Label(detailsSection, SWT.NONE);
		l.setText(Messages.NavigationLayersDialog_TargetList);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite c = new Composite(detailsSection, SWT.NONE);
		c.setBackground(c.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		c.setLayout(new GridLayout());
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)c.getLayout()).verticalSpacing = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		((GridLayout)c.getLayout()).marginWidth = 5;
		for (NavigationTarget t : ctpackage.getTargetsAsJson()) {
			l = new Label(c, SWT.NONE);
			l.setText(t.getId());
			l.setBackground(l.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		}
		detailsSection.layout(true);
	}
	
	private List<NavigationLayer> getSelection(){
		List<NavigationLayer> items = new ArrayList<>();
		for (Iterator<?> iterator = tblViewer.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object type = (Object) iterator.next();
			if (type instanceof NavigationLayer) {
				items.add((NavigationLayer)type);
			}
		}
		return items;
	}
	
	
	private void addNavigation() {
		NavigationLayer nav = new NavigationLayer();
		nav.setCreatedDate(LocalDate.now());
		nav.setLastModifiedBy(SmartDB.getCurrentEmployee());
		nav.setLastModifiedDate(nav.getCreatedDate());
		nav.setName(Messages.NavigationLayersDialog_DefaultLayerName);
		nav.setConservationArea(SmartDB.getCurrentConservationArea());

		editNavigation(nav);
	}
	
	private void duplicateNavigation() {
		List<NavigationLayer> items = getSelection();
		if (items.isEmpty()) return;
		
		NavigationLayer tocopy = items.get(0);
		NavigationLayer copy = new NavigationLayer();
		copy.setName(MessageFormat.format(Messages.NavigationLayersDialog_DefaultLayerCopy, tocopy.getName()));
		copy.setTargets(Arrays.copyOf(tocopy.getTargets(), tocopy.getTargets().length));
		copy.setConservationArea(tocopy.getConservationArea());
		copy.setCreatedDate(LocalDate.now());
		copy.setLastModifiedBy(SmartDB.getCurrentEmployee());
		copy.setLastModifiedDate(copy.getCreatedDate());
		editNavigation(copy);
	}
	
	private boolean exportNavigation() {
		List<NavigationLayer> items = getSelection();
		
		try {
			ExportNavigationManager.INSTANCE.doExport(items, context);
			return true;
		} catch (IOException e) {
			CyberTrackerPlugIn.log(e.getMessage(),  e);
		}
		return false;
	}
	
	private void editNavigation() {
		List<NavigationLayer> items = getSelection();
		if (items.isEmpty()) return;
		editNavigation(items.get(0));
	}
	
	private void editNavigation(NavigationLayer item) {
		NavigationLayerDialog dialog = new NavigationLayerDialog(getShell(), item);
		ContextInjectionFactory.inject(dialog, context);
		dialog.open();
		refresh();
	}
	
	private void deletePackage() {
		List<NavigationLayer> items = getSelection();
		if (items.isEmpty()) return;
		if (!MessageDialog.openQuestion(getShell(), Messages.NavigationLayersDialog_DeleteTitle, MessageFormat.format(Messages.NavigationLayersDialog_DeleteConfirm, items.size()))){
			return;
		}
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (NavigationLayer p : items) {
					session.remove(p);
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
		NAME (Messages.NavigationLayersDialog_NameColumn),
		LAST_MODIFIED(Messages.NavigationLayersDialog_LastModifiedColumn);
		
		String guiName;
		
		Column(String name) {
			this.guiName = name;
		}
		
		public Image getImage(Object x) {
			if (!(x instanceof NavigationLayer)) return null;
			if (this == NAME) return CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.ICON_NAVIGATION);
			return null;
		}
		
		public String getValue(Object x) {
			if (!(x instanceof NavigationLayer)) return ""; //$NON-NLS-1$
			NavigationLayer pp = (NavigationLayer)x;
			
			if (this == NAME) return pp.getName();
			if (this == LAST_MODIFIED) return  DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(pp.getLastModifiedDate());

			return ""; //$NON-NLS-1$
		}
	}
	
	private Job loadItems = new Job("Load ct packages") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<NavigationLayer> packages = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				packages.addAll(QueryFactory.buildQuery(session, NavigationLayer.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
				packages.forEach(p->p.getLastModifiedBy().getFamilyName());
			}
			packages.sort((a,b)->a.getName().compareTo(b.getName()));
			Display.getDefault().asyncExec(()->{
				tblViewer.setInput(packages);				
			});
			
			return Status.OK_STATUS;
		}
		
	};
	
}
