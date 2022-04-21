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
package org.wcs.smart.ui.internal.ca.properties;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.ca.in.IconExporter;
import org.wcs.smart.ca.in.IconImporter;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.control.NameKeyDialog;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.icon.ui.IconTable;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;



/**
 * Property page for managing iconsets
 * 
 * @author egouge
 * @since 1.0.0
 */
public class IconsetPropertyPage extends SmartStyledTitleDialog {

	private static final String CURRENT_SET_KEY = "CURRENT"; //$NON-NLS-1$

	private static final int SIZE = 50;
	
	private ListViewer lstIconsets;
	private IconTable imageTable;
	
	private Composite iconComp;
	private TableViewer tblIcons;
	
	private List<Path> toDeleteFiles = new ArrayList<>();
	
	private List<IconSet> sets = null;
	private List<Icon> icons = null;
	
	private Composite main = null;
	
	private Session session;
	private boolean isDirty = false;
	
	public IconsetPropertyPage(Shell parent) {
		super(parent);
	}

	@Override
	public Point getInitialSize() {
		Point pnt = super.getInitialSize();
		pnt.y = 550;
		return pnt;
	}
	
	@Override
	protected void okPressed() {
		doSave();		
	}
	
	private boolean doSave() {
		try {
			session.getTransaction().commit();
			setDirty(false);
			session.getTransaction().begin();
			return true;
		}catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.IconsetPropertyPage_SaveError + ex.getMessage(), ex);
			return false;
		}
	}
	
	@Override
	protected void cancelPressed() {
		if (isDirty) {
			MessageDialog md = new MessageDialog(getShell(), Messages.IconsetPropertyPage_Closedialog, null, Messages.IconsetPropertyPage_CloseMessage, MessageDialog.QUESTION_WITH_CANCEL, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL},0);
			int ret = md.open();
			if (ret == 2){
				//cancel
				return;
			}else if (ret == 0){
				//yes
				if (!doSave()){
					return;
				}else{
					setReturnCode(IDialogConstants.OK_ID);
				}
			}
		}else {
			session.getTransaction().rollback();
		}
		session.close();
		super.cancelPressed();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button ok = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		ok.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		
	}
	
	private void setDirty(boolean dirty) {
		getButton(IDialogConstants.OK_ID).setEnabled(dirty);
		this.isDirty = dirty;
	}
	
	@Override
	public Control createDialogArea(Composite parent){
	
		session = HibernateManager.openSession(new AttachmentInterceptor());
		session.beginTransaction();
		
		main = new Composite(parent, SWT.BORDER);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		
		CTabFolder tabs = new CTabFolder(main, SWT.TOP | SWT.FLAT );
		tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		CTabItem itemSets = new CTabItem(tabs, SWT.NONE);
		itemSets.setText(Messages.IconPreferencePage_IconSetsTab);
		itemSets.setControl(createSetTab(tabs));
		
		CTabItem itemIcons = new CTabItem(tabs, SWT.NONE);
		itemIcons.setText(Messages.IconPreferencePage_IconsTab);
		iconComp = createIconsTab(tabs);
		itemIcons.setControl(iconComp);

		setTitle(Messages.IconsetPropertyPage_Title);
		getShell().setText(Messages.IconsetPropertyPage_Title);
		setMessage(Messages.IconsetPropertyPage_Message);
		loadJob.schedule();
		return main;
	}
	
	private Composite createIconsTab(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true ));
		main.setBackgroundMode(SWT.INHERIT_FORCE);
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		Label l = new Label(main, SWT.NONE);
		l.setText(DialogConstants.LOADING_TEXT);
		
		return main;
	}
	
	private void createIconTable(List<IconSet> sets, List<Icon> icons) {
		for (Control c : iconComp.getChildren()) c.dispose();
		
		Composite panel = new Composite(iconComp, SWT.NONE);
		panel.setLayout(new GridLayout());
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		ToolBar tb = new ToolBar(panel,  SWT.HORIZONTAL);
		tb.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		
		ToolItem exportIcon = new ToolItem(tb, SWT.PUSH);
		exportIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EXPORT_ICON));
		exportIcon.addListener(SWT.Selection, e->exportIcons());
		exportIcon.setToolTipText(Messages.IconsetPropertyPage_ExportIconTitle);
		
		ToolItem importIcon = new ToolItem(tb, SWT.PUSH);
		importIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.IMPORT_ICON));
		importIcon.addListener(SWT.Selection, e->importIcons());
		importIcon.setToolTipText(Messages.IconsetPropertyPage_ImportCustomICons);
		
		ToolItem addIcon = new ToolItem(tb, SWT.PUSH);
		addIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		addIcon.addListener(SWT.Selection, e->addIcon());
		addIcon.setToolTipText(Messages.IconPreferencePage_addicontooltip);
		
		ToolItem editIcon = new ToolItem(tb, SWT.PUSH);
		editIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		editIcon.addListener(SWT.Selection, e->editIcon());
		editIcon.setToolTipText(Messages.IconPreferencePage_editicontooltip);
		
		ToolItem deleteIcon = new ToolItem(tb, SWT.PUSH);
		deleteIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteIcon.addListener(SWT.Selection, e->deleteIcons());
		deleteIcon.setToolTipText(Messages.IconPreferencePage_deleteicontooltip);
		
		tblIcons = new TableViewer(panel, SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL);
		tblIcons.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblIcons.setContentProvider(new ILazyContentProvider() {
			List<?> data;
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				if (newInput instanceof List) {
					data = (List<?>) newInput;
				}else {
					data = Collections.singletonList(newInput);
				}
			}

			@Override
			public void updateElement(int index) {
				tblIcons.replace(data.get(index), index);
			}
		});
		
		
		tblIcons.getTable().setHeaderVisible(true);
		tblIcons.getTable().setLinesVisible(false);
		
		TableViewerColumn emptycolumn = new TableViewerColumn(tblIcons, SWT.NONE);
		emptycolumn.getColumn().setText(""); //$NON-NLS-1$
		emptycolumn.getColumn().setWidth(0);
		emptycolumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return null;
			}
		});
		
		
		TableViewerColumn colName = new TableViewerColumn(tblIcons, SWT.NONE);
		colName.getColumn().setText(Messages.IconPreferencePage_NameColumn);
		colName.getColumn().setWidth(150);
		colName.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof Icon) {
					Icon icn = (Icon)element;
					return icn.getName() + " (" + icn.getKeyId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				return super.getText(element);
			}
		});
		
		for (IconSet s : sets) {
			TableViewerColumn colIcon = new TableViewerColumn(tblIcons, SWT.NONE);
			colIcon.getColumn().setText(s.getName());
			colIcon.setLabelProvider(new ColumnLabelProvider() {
				private List<Image> images = new ArrayList<>();
				
				@Override
				public String getText(Object element) {
					return null;
				}
				
				@Override
				public void dispose() {
					super.dispose();
					images.forEach(e->e.dispose());
				}
				
				@Override
				public Image getImage(Object element) {
					if (element instanceof Icon) {
						IconFile ff = ((Icon)element).getIconFile(s);
						if (ff == null) return null;
						try {
							Path f = null;
							if (ff.getCopyFromLocation() != null) {
								f = ff.getCopyFromLocation();
							}else {
								f = ff.getAttachmentFile();
							}
							Image img = SmartUtils.getImage(f,SIZE);
							if (img != null) images.add(img);
							return img;
						}catch (Throwable t) {
							
						}
					}
					return null;
				}
			});
			colIcon.getColumn().pack();
			if (colIcon.getColumn().getWidth() < SIZE) { colIcon.getColumn().setWidth(SIZE); }
		}
		tblIcons.setItemCount(icons.size());
		tblIcons.setUseHashlookup(true);
		tblIcons.setInput(icons);

		Menu tmp = new Menu(tblIcons.getControl());
		
		MenuItem miAddIcon = new MenuItem(tmp, SWT.PUSH);
		miAddIcon.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAddIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAddIcon.addListener(SWT.Selection, e->addIcon());
		
		MenuItem miEditIcon = new MenuItem(tmp, SWT.PUSH);
		miEditIcon.setText(DialogConstants.EDIT_BUTTON_TEXT);
		miEditIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEditIcon.addListener(SWT.Selection, e->editIcon());
		
		MenuItem miDeleteIcon = new MenuItem(tmp, SWT.PUSH);
		miDeleteIcon.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDeleteIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDeleteIcon.addListener(SWT.Selection, e->deleteIcons());
		
		tblIcons.getControl().setMenu(tmp);
		
		iconComp.layout(true);
	}
	
	
	private void editIcon() {
		Object x = tblIcons.getStructuredSelection().getFirstElement();
		if (!(x instanceof Icon)) return;
		Icon toEdit = (Icon)x;
		
		IconSelectionDialog dialog = new IconSelectionDialog(getShell(), toEdit, sets);
		if (dialog.open() != Window.OK) return;
		session.saveOrUpdate(toEdit);
		
		setDirty(true);
		tblIcons.refresh();
		selectIconSet(true);
	}
	
	private void addIcon() {
		IconSelectionDialog dialog = new IconSelectionDialog(getShell(), IconSelectionDialog.Type.NEW, sets);
		if (dialog.open() != Window.OK) return;
		
		Icon icon = dialog.getSelectedIcon();
		
		if (icon == null) return;
		session.saveOrUpdate(icon);
		setDirty(true);
		icons.add(icon);
		sortIcons();
		tblIcons.setItemCount(icons.size());
		tblIcons.refresh();
		selectIconSet(true);
	}
	
	
	private void exportIcons() {
		if (isDirty) {
			MessageDialog.openWarning(getShell(), Messages.IconsetPropertyPage_ImportIconsTitle, Messages.IconsetPropertyPage_SaveRequired);
			return;
		}
		
		MessageDialog dialog = new MessageDialog(getShell(), Messages.IconsetPropertyPage_ExportIconTitle, null,
				Messages.IconsetPropertyPage_ExportFormat,
				MessageDialog.WARNING,
				0, Messages.IconsetPropertyPage_CSVFile, Messages.IconsetPropertyPage_ZilFile, IDialogConstants.CANCEL_LABEL);
		int r = dialog.open();
		if (r == 2) return;
		
		boolean iscsv = r == 0;
		
		
		FileDialog fd = new FileDialog(getShell(), SWT.SAVE);
		if (iscsv) {
			fd.setFilterExtensions(new String[] {"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			fd.setFilterNames(new String[] {DialogConstants.CSV_FILES, DialogConstants.ALL_FILES});
			fd.setFileName(SmartDB.getCurrentConservationArea().getId() + "_icons.csv"); //$NON-NLS-1$
		}else {
			fd.setFilterExtensions(new String[] {"*.zip", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			fd.setFilterNames(new String[] {DialogConstants.ZIP_FILES, DialogConstants.ALL_FILES});
			fd.setFileName(SmartDB.getCurrentConservationArea().getId() + "_icons.zip"); //$NON-NLS-1$
		}
		String file = fd.open();
		if (file == null) return;
		
		Path exportfile = Paths.get(file);
		if (Files.exists(exportfile)) {
			if (!MessageDialog.openQuestion(getShell(), Messages.IconsetPropertyPage_ExportIconTitle, MessageFormat.format(Messages.IconsetPropertyPage_FileExists, exportfile.toString()))) {
				return;
			}
		}
		
		ProgressMonitorDialog pdialog = new ProgressMonitorDialog(getShell());
		try {
			pdialog.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

					try {
						IconExporter exporter = new IconExporter();
						if (iscsv) {
							exporter.exportIconKeys(SmartDB.getCurrentConservationArea(), exportfile, session, monitor);
						}else {
							exporter.exportIconFiles(SmartDB.getCurrentConservationArea(), exportfile, session, monitor);
						}
						Display.getDefault().syncExec(()->{
							MessageDialog.openInformation(getShell(), Messages.IconsetPropertyPage_ExportIconTitle, 
									MessageFormat.format(Messages.IconsetPropertyPage_ExportComplete, exportfile.toString()));	
						});
					}catch (Exception ex) {
						throw new InvocationTargetException(ex);
					}
				}
			});
		}catch (Exception ex) {
			SmartPlugIn.displayLog(ex.getMessage(), ex);
		}
	}
	
	private void importIcons() {
		if (isDirty) {
			MessageDialog.openWarning(getShell(), Messages.IconsetPropertyPage_ImportIconsTitle, Messages.IconsetPropertyPage_SaveRequired);
			return;
		}
		
		BiFunction<Path,Path,Boolean> processor = (iconFile, iconDirectory)->{
			//TODO: - do in progress monitor??
			try {
				IconImporter importer = new IconImporter();
				ConservationArea ca = session.get(ConservationArea.class, SmartDB.getCurrentConservationArea().getUuid());
				importer.importIcons(ca, sets, icons, iconFile, iconDirectory);
				
				if (importer.getIcons().isEmpty()) {
					MessageDialog.openWarning(getShell(), Messages.IconsetPropertyPage_ImportIconsTitle, Messages.IconsetPropertyPage_NoIconsFound);
					return false;
				}
				
				if (!importer.getWarnings().isEmpty()) {
					WarningDialog wdialog = new WarningDialog(getShell(), 
							Messages.IconsetPropertyPage_ImportIconsTitle, Messages.IconsetPropertyPage_IconImportWarn,
							importer.getWarnings(), 
							new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
					if (wdialog.open() == 1) {
						//no 
						return false;
					}
				}
				
				importer.getIcons().forEach(i->session.saveOrUpdate(i));
				session.flush();
				setDirty(true);
				//add any new icons
				for (Icon c : importer.getIcons()) {
					if (!icons.contains(c)) icons.add(c);
				}				
				sortIcons();
				tblIcons.setItemCount(icons.size());
				tblIcons.refresh();
				selectIconSet(true);
				
				MessageDialog.openInformation(getShell(), Messages.IconsetPropertyPage_ImportIconsTitle, Messages.IconsetPropertyPage_IconsImportedMsg);
				return true;
				
			}catch (Exception ex) {
				SmartPlugIn.displayLog(ex.getMessage(), ex);
				return false;
			}
		};
		
		ImportIconDialog dialog = new ImportIconDialog(getShell(), session, processor);
		if (dialog.open() != Window.OK) return;
		
	}
	
	
	
	private void deleteIcons() {
		if (tblIcons == null || tblIcons.getControl().isDisposed()) return;
		List<Icon> toDelete = new ArrayList<>();
		
		for (Iterator<?> iterator = tblIcons.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof Icon) toDelete.add((Icon) x);			
		}
		if (toDelete.isEmpty()) return;
		
		if (!MessageDialog.openConfirm(getShell(), Messages.IconPreferencePage_DeleteTitle,  MessageFormat.format(Messages.IconPreferencePage_DeleteMsg, toDelete.size()))) return;
		tblIcons.getTable().setRedraw(false);
		try {
			ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
			try {
				dialog.run(true, false, new IRunnableWithProgress() {
					
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						monitor.beginTask(Messages.IconsetPropertyPage_deleteicontask, toDelete.size()+1);
						toDelete.forEach(e->{
							
							//there seems to be a bug in apache derby
							//that causes all values passed to the trigger to be null
							//as a result we get ca_uuid cannot be null error
							//this only appears to be a problem for Attribute icons
							//so we specifically delete them here.
							//re: #3401
							session.createQuery("UPDATE Attribute SET icon = null WHERE icon = :icon") //$NON-NLS-1$
								.setParameter("icon", e) //$NON-NLS-1$
								.executeUpdate();
							session.delete(e);
							session.flush();
							monitor.worked(1);
						});
						
						icons.removeAll(toDelete);
						monitor.done();
						
					}
				});
			}catch (Exception ex) {
				SmartPlugIn.log(ex.getMessage(), ex);
			}
			
			setDirty(true);
			
			tblIcons.setItemCount(icons.size());
			tblIcons.refresh();
			tblIcons.setSelection(null);
			selectIconSet(true);
		}finally {
			tblIcons.getTable().setRedraw(true);
		}
	}
	
	private Composite createSetTab(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		
		Composite top = new Composite(main, SWT.NONE);
		top.setLayout(new GridLayout(2, false));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lstIconsets = new ListViewer(top, SWT.BORDER | SWT.V_SCROLL);
		lstIconsets.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lstIconsets.getControl().getLayoutData()).heightHint = 100;
		lstIconsets.setContentProvider(ArrayContentProvider.getInstance());
		lstIconsets.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof IconSet) {
					IconSet s = (IconSet)element;
					StringBuilder sb = new StringBuilder();
					sb.append(s.getName());
					if (s.getIsDefault()) {
						sb.append(Messages.IconPreferencePage_DefaultLabel);
					}
					return sb.toString();
				}
				return super.getText(element);
			}
		});
		lstIconsets.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		ToolBar tools = new ToolBar(top, SWT.FLAT | SWT.VERTICAL);
		tools.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		ToolItem tiAdd = new ToolItem(tools, SWT.PUSH);
		tiAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		tiAdd.setToolTipText(Messages.IconPreferencePage_newiconsettooltip);
		tiAdd.addListener(SWT.Selection, e->addIconSet());
		
		ToolItem tiEdit = new ToolItem(tools, SWT.PUSH);
		tiEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		tiEdit.setToolTipText(Messages.IconPreferencePage_editiconsettooltip);
		tiEdit.addListener(SWT.Selection, e->editIconSet());
		
		ToolItem tiDelete = new ToolItem(tools, SWT.PUSH);
		tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiDelete.setToolTipText(Messages.IconPreferencePage_deleteiconsettooltip);
		tiDelete.addListener(SWT.Selection, e->deleteIconSet());
		
		imageTable = new IconTable(top, SWT.BORDER);
		imageTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		lstIconsets.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				selectIconSet(false);
			}
		});
		
		Menu tmp = new Menu(lstIconsets.getControl());
		
		MenuItem miAdd = new MenuItem(tmp, SWT.PUSH);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.addListener(SWT.Selection, e->addIconSet());
		new MenuItem(tmp, SWT.SEPARATOR);
		MenuItem miSetDefault = new MenuItem(tmp, SWT.PUSH);
		miSetDefault.setText(Messages.IconPreferencePage_MakeDefault);
		miSetDefault.addListener(SWT.Selection, e->setDefault());
		
		MenuItem meEdit = new MenuItem(tmp, SWT.PUSH);
		meEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		meEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		meEdit.addListener(SWT.Selection, e->editIconSet());
		new MenuItem(tmp, SWT.SEPARATOR);
		MenuItem miDelete = new MenuItem(tmp, SWT.PUSH);
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.addListener(SWT.Selection, e->deleteIconSet());
		
		lstIconsets.getControl().setMenu(tmp);
		
		return main;
	}

	private void setDefault() {
		Object item = lstIconsets.getStructuredSelection().getFirstElement();
		if (item == null) return;
		if (!(item instanceof IconSet)) return;
		sets.forEach(e->{
			if (e.equals(item)) {
				if (!e.getIsDefault()) {
					e.setIsDefault(true);
					session.saveOrUpdate(e);
				}
			}else if (e.getIsDefault()) {
				e.setIsDefault(false);
				session.saveOrUpdate(e);
			}
			setDirty(true);
		});
		lstIconsets.refresh();
	}
	
	private void selectIconSet(boolean refresh) {
		Object item = lstIconsets.getStructuredSelection().getFirstElement();
		if (item == null || !(item instanceof IconSet)) {
			imageTable.setAttachments(Collections.emptyList());
			return;
		}
		
		
		if (!refresh && item.equals(lstIconsets.getData(CURRENT_SET_KEY))) return;
		lstIconsets.setData(CURRENT_SET_KEY, item);
		
		List<IconFile> files = new ArrayList<>();
		for (Icon c : icons) {
			IconFile iconfile = c.getIconFile((IconSet)item);
			if (iconfile != null) files.add(iconfile);
		}
		imageTable.setAttachments(files);
	}
	
	
	private void addIconSet() {
		if (sets == null) return;
		
		IconSet newIconSet = new IconSet();
		newIconSet.setConservationArea(SmartDB.getCurrentConservationArea());
		newIconSet.setIsDefault(false);
		
		NewIconSetDialog dialog = new NewIconSetDialog(getShell(),newIconSet, sets, icons, session);
		if (dialog.open() != Window.OK) return;
		
		toDeleteFiles.addAll(dialog.getDeleteFiles());
		setDirty(true);
		if (sets.isEmpty()) newIconSet.setIsDefault(true);
		sets.add(newIconSet);
		lstIconsets.refresh();
		createIconTable(sets, icons);
	}
	
	private void deleteIconSet() {
		Object item = lstIconsets.getStructuredSelection().getFirstElement();
		if (item == null) return;
		if (!(item instanceof IconSet)) return;
		IconSet set = (IconSet)item;
		
		if (!MessageDialog.openConfirm(getShell(), Messages.IconPreferencePage_DeleteDialogTitle, MessageFormat.format(Messages.IconPreferencePage_DeleteSetMsg, set.getName()))) return;
		
		for (Icon i : icons) {
			IconFile f = i.getIconFile(set);
			if (f != null) {
				i.getFiles().remove(f);
				session.delete(f);
			}
		}
		session.flush();
		session.delete(set);
		session.flush();
		setDirty(true);
		
		sets.remove(set);
		if (set.isDefault() && !sets.isEmpty()) {
			sets.get(0).setIsDefault(true);
		}
		
		lstIconsets.refresh();
		createIconTable(sets, icons);
		selectIconSet(true);
	}
	

	private void editIconSet() {
		if (sets == null) return;
		
		Object item = lstIconsets.getStructuredSelection().getFirstElement();
		if (item == null) return;
		if (!(item instanceof IconSet)) return;
		IconSet toEdit = (IconSet)item;
		
		NameKeyDialog<IconSet> dialog = new NameKeyDialog<IconSet>(getShell(), toEdit, sets){
			@Override
			protected String getTitle(){
				return Messages.IconPreferencePage_EditIconDialogTitle;
			}
		};
		
		if (dialog.open() != Window.OK) return;
		session.saveOrUpdate(toEdit);
		setDirty(true);
		lstIconsets.refresh();
		createIconTable(sets, icons);
	}
	
	private void sortIcons() {
		icons.sort((a,b)->java.text.Collator.getInstance().compare(a.getName(), b.getName()));
	}
	private Job loadJob = new Job(Messages.IconPreferencePage_LoadJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			sets = new ArrayList<>();
			icons = new ArrayList<>();
			
			sets.addAll(QueryFactory.buildQuery(session, IconSet.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
			icons.addAll(QueryFactory.buildQuery(session, Icon.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
			
			icons.forEach(icn->icn.getFiles().forEach(iconfile->iconfile.computeFileLocation(session)));
						
			sortIcons();
			
			Display.getDefault().asyncExec(()->{
				lstIconsets.setInput(sets);
				if (!sets.isEmpty()) {
					lstIconsets.setSelection(new StructuredSelection(sets.get(0)));
				}
				if (!icons.isEmpty()) {
					createIconTable(sets, icons);
				}
				main.layout(true, true);
			});
			return Status.OK_STATUS;
		}
		
	};
}
