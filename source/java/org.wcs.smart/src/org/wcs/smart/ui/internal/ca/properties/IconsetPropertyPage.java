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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.IconFKManager;
import org.wcs.smart.ca.IconManager;
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
import org.wcs.smart.ui.IconSelectionDialog;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;



/**
 * Property page for managing iconsets
 * 
 * @author egouge
 * @since 1.0.0
 */
public class IconsetPropertyPage extends SmartStyledTitleDialog {

	private static final String CURRENT_SET_KEY = "CURRENT"; //$NON-NLS-1$
	
	private ListViewer lstIconsets;
	private IconTable imageTable;
	
	private Composite iconComp;
	private Composite libraryIconComp;
	
	private TableViewer tblIcons;
	private TableViewer tblLibraryIcons;
	
	private List<Path> toDeleteFiles = new ArrayList<>();
	
	private List<IconSet> sets = null;
	private List<Icon> caicons = null;
		
	private Composite main = null;
	
	private Session session;
	private boolean isDirty = false;
	
	private String txtFilterText = null;
	
	private ImageRegistry imgr ;
	
	private ViewerFilter iconFilter = new ViewerFilter() {
		
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (txtFilterText == null || txtFilterText.isEmpty()) return true;
			
			if (element instanceof Icon) {
				return ((Icon)element).getName().toLowerCase().contains(txtFilterText);
			}
			return false;
		}
	};
	
	private String txtLibraryFilterText = null;
	
	private ViewerFilter libraryIconFilter = new ViewerFilter() {
		
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (txtLibraryFilterText == null || txtLibraryFilterText.isEmpty()) return true;
			
			if (element instanceof Icon) {
				return ((Icon)element).getName().toLowerCase().contains(txtLibraryFilterText);
			}
			return false;
		}
	};
	
	
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
	public boolean close() {
		if (isDirty) {
			MessageDialog md = new MessageDialog(getShell(), Messages.IconsetPropertyPage_Closedialog, null, Messages.IconsetPropertyPage_CloseMessage, MessageDialog.QUESTION_WITH_CANCEL, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL},0);
			int ret = md.open();
			if (ret == 2){
				//cancel
				return false;
			}else if (ret == 0){
				//yes
				if (!doSave()){
					return false;
				}else{
					setReturnCode(IDialogConstants.OK_ID);
				}
			}
		}else {
			session.getTransaction().rollback();
		}

		session.close();
		return super.close();
	}
	
	@Override
	protected void okPressed() {
		doSave();		
	}
	
	private boolean doSave() {
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, (monitor)->{
				try {
					monitor.beginTask(DialogConstants.SAVING, -1);
					session.getTransaction().commit();
					IconFKManager.INSTANCE.createIconFkConstraints(session);
					
					Display.getDefault().asyncExec(()->setDirty(false));
					
					//wait here to let any filestore events to be processed before
					//we start another transaction and remove fk constraints
					//which will prevent inserting into change log table (if installed)
					//500 wasn't long enough in my test case
					Thread.sleep(1000);
					
					session.getTransaction().begin();
					IconFKManager.INSTANCE.dropIconFkConstraints(session);
				}catch (Exception ex) {
					throw new InvocationTargetException(ex);
				}
			});
		}catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.IconsetPropertyPage_SaveError + ex.getMessage(), ex);
			return false;
		}
		return true;
		
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
	
		imgr = new ImageRegistry();
		parent.addListener(SWT.Dispose, e->imgr.dispose());
		//clear all CA thumbnails
		IconManager.INSTANCE.clearThumbnails();
				
		session = HibernateManager.openSession(new AttachmentInterceptor());
		
		session.beginTransaction();
			
		IconFKManager.INSTANCE.dropIconFkConstraints(session);

		main = new Composite(parent, SWT.NONE);
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
//		itemIcons.setText(Messages.IconPreferencePage_IconsTab);
		itemIcons.setText(Messages.IconsetPropertyPage_CAIconTabl);
		iconComp = createIconsTab(tabs);
		itemIcons.setControl(iconComp);

		
		CTabItem libraryIcons = new CTabItem(tabs, SWT.NONE);
//		itemIcons.setText(Messages.IconPreferencePage_IconsTab);
		libraryIcons.setText(Messages.IconsetPropertyPage_SMARTIconTab);
		libraryIconComp = createLibraryIconsTab(tabs);
		libraryIcons.setControl(libraryIconComp);
		
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

	private Composite createLibraryIconsTab(Composite parent) {
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
	
	private void createFilterText(Composite parent, Consumer<String> onChange) {
		FilterComposite txtFilter = new FilterComposite(parent, SWT.NONE);
		txtFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtFilter.addChangeListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
					String filter = txtFilter.getPatternFilter();
				if (filter != null) filter = filter.toLowerCase().trim();
				onChange.accept(filter);
			}
		});		
	}
	
	private void createIconTable(List<IconSet> sets, List<Icon> icons) {
		for (Control c : iconComp.getChildren()) c.dispose();
		
		Composite panel = new Composite(iconComp, SWT.NONE);
		panel.setLayout(new GridLayout());
//		((GridLayout)panel.getLayout()).marginWidth = 0;
//		((GridLayout)panel.getLayout()).marginHeight = 0;
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
		
		createFilterText(panel, str->{
			txtFilterText = str;
			tblIcons.refresh();
		});
		
		tblIcons = new TableViewer(panel, SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL);
		tblIcons.setFilters(new ViewerFilter[] {iconFilter});
		tblIcons.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblIcons.setContentProvider(ArrayContentProvider.getInstance());
		
		tblIcons.getTable().setHeaderVisible(true);
		tblIcons.getTable().setLinesVisible(false);
		tblIcons.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editIcon();
			}
		});
		tblIcons.getTable().addListener(SWT.KeyDown, e->{
			//scroll to first item that start with key e.character
			Object in = tblIcons.getInput();
			if (!(in instanceof List)) return;
			List<?> c = (List<?>) in;
			e.doit = false;
			
			Object selection = tblIcons.getStructuredSelection().getFirstElement();
			
			int startIndex = 0;
			if (selection != null) {
				int a = c.indexOf(selection);
				if (a >= 0) startIndex = ( a + 1 ) % c.size();
			}
			
			char toFind = Character.toLowerCase(e.character);
			int index = startIndex;
			while(true) {
				Object item =  c.get(index);
				if (item instanceof Icon) {
					char cc = Character.toLowerCase( ((Icon) item).getName().charAt(0) );
					if (toFind == cc) {
						tblIcons.getTable().setSelection(index);
						tblIcons.getTable().showSelection();
						return;
					}
				}
				
				index = (index + 1) % c.size();	
				if (index == startIndex) break;
			}
		});
		
		
		TableViewerColumn emptycolumn = new TableViewerColumn(tblIcons, SWT.NONE);
		emptycolumn.getColumn().setText(""); //$NON-NLS-1$
		emptycolumn.getColumn().setWidth(0);
		emptycolumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return null;
			}
		});
		
		
		TableViewerColumn colName = new TableViewerColumn(tblIcons, SWT.NONE);
		colName.getColumn().setText(Messages.IconSelectionDialog_NameColumn);
		colName.getColumn().setWidth(150);
		colName.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof Icon) return ((Icon)element).getName();
				return super.getText(element);
			}
		});
		
		
		for (IconSet s : sets) {
			TableViewerColumn colIcon = new TableViewerColumn(tblIcons, SWT.DEFAULT);
			
			colIcon.getColumn().setText(s.getName());
			colIcon.setLabelProvider(new ColumnLabelProvider() {
				
				@Override
				public String getText(Object element) {
					return null;
				}

				@Override
				public Image getImage(Object element) {
					if (element instanceof Icon) {
						return getImageIcon((Icon)element, s);
					}
					return null;
				}
			});
			colIcon.getColumn().pack();
			if (colIcon.getColumn().getWidth() < IconManager.Size.MEDIUM.size) { colIcon.getColumn().setWidth(IconManager.Size.MEDIUM.size); }
		}
		tblIcons.setItemCount(1);
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
	
	
	private void createLibraryIconTable(List<IconSet> sets, List<Icon> icons) {
		for (Control c : libraryIconComp.getChildren()) c.dispose();
		
		Composite panel = new Composite(libraryIconComp, SWT.NONE);
		panel.setLayout(new GridLayout());
//		((GridLayout)panel.getLayout()).marginWidth = 0;
//		((GridLayout)panel.getLayout()).marginHeight = 0;
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createFilterText(panel, str->{
			txtLibraryFilterText = str;
			tblLibraryIcons.refresh();
		});
		
		tblLibraryIcons = new TableViewer(panel, SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL);
		tblLibraryIcons.setFilters(new ViewerFilter[] {libraryIconFilter});
		tblLibraryIcons.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblLibraryIcons.setContentProvider(ArrayContentProvider.getInstance());
		
		tblLibraryIcons.getTable().setHeaderVisible(true);
		tblLibraryIcons.getTable().setLinesVisible(false);
		tblLibraryIcons.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				okPressed();
			}
		});
		tblLibraryIcons.getTable().addListener(SWT.KeyDown, e->{
			//scroll to first item that start with key e.character
			Object in = tblLibraryIcons.getInput();
			if (!(in instanceof List)) return;
			List<?> c = (List<?>) in;
			e.doit = false;
			
			Object selection = tblLibraryIcons.getStructuredSelection().getFirstElement();
			
			int startIndex = 0;
			if (selection != null) {
				int a = c.indexOf(selection);
				if (a >= 0) startIndex = ( a + 1 ) % c.size();
			}
			
			char toFind = Character.toLowerCase(e.character);
			int index = startIndex;
			while(true) {
				Object item =  c.get(index);
				if (item instanceof Icon) {
					char cc = Character.toLowerCase( ((Icon) item).getName().charAt(0) );
					if (toFind == cc) {
						tblLibraryIcons.getTable().setSelection(index);
						tblLibraryIcons.getTable().showSelection();
						return;
					}
				}
				
				index = (index + 1) % c.size();	
				if (index == startIndex) break;
			}
		});
		
		
		TableViewerColumn emptycolumn = new TableViewerColumn(tblLibraryIcons, SWT.NONE);
		emptycolumn.getColumn().setText(""); //$NON-NLS-1$
		emptycolumn.getColumn().setWidth(0);
		emptycolumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return null;
			}
		});
		
		
		TableViewerColumn colName = new TableViewerColumn(tblLibraryIcons, SWT.NONE);
		colName.getColumn().setText(Messages.IconSelectionDialog_NameColumn);
		colName.getColumn().setWidth(150);
		colName.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof Icon) {
					for (Icon c : caicons) {
						if (c.getKeyId().equals(((Icon)element).getKeyId())) {
							return ((Icon)element).getName() + "**"; //$NON-NLS-1$
						}
					}
					return ((Icon)element).getName();
				}
				return super.getText(element);
			}
		});
		
		
		for (IconSet s : sets) {
			TableViewerColumn colIcon = new TableViewerColumn(tblLibraryIcons, SWT.DEFAULT);
			
			colIcon.getColumn().setText(s.getName());
			colIcon.setLabelProvider(new ColumnLabelProvider() {
				
				@Override
				public String getText(Object element) {
					return null;
				}
				
				@Override
				public Image getImage(Object element) {
					if (element instanceof Icon) {
						return getImageIcon((Icon)element, s);						
					}
					return null;
				}
			});
			colIcon.getColumn().pack();
			if (colIcon.getColumn().getWidth() < IconManager.Size.MEDIUM.size) { colIcon.getColumn().setWidth(IconManager.Size.MEDIUM.size); }
		}
		tblLibraryIcons.setItemCount(1);
		tblLibraryIcons.setUseHashlookup(true);
		tblLibraryIcons.setInput(icons);

		Label lbl = new Label(panel, SWT.NONE);
		lbl.setText(Messages.IconsetPropertyPage_IconInCa);
		libraryIconComp.layout(true);
		
		Menu tmp = new Menu(tblIcons.getControl());
		
		MenuItem miAddIcon = new MenuItem(tmp, SWT.PUSH);
		miAddIcon.setText(Messages.IconsetPropertyPage_AddToCaBtn);
		miAddIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAddIcon.addListener(SWT.Selection, e->addIcon(tblLibraryIcons.getStructuredSelection()));
				
		tblLibraryIcons.getControl().setMenu(tmp);
	}
	
	private Image getImageIcon(Icon element, IconSet s) {
		IconFile ff = ((Icon)element).getIconFile(s);
		if (ff == null) return null;
		String key = ff.getUuid().toString();
		Image i = imgr.get(key);
		if (i != null) return i;
		byte[] data = IconManager.INSTANCE.getThumbnailFile(ff, IconManager.Size.MEDIUM);
		if (data == null) return null;
		
		try(InputStream ins = new ByteArrayInputStream(data)){
			Image img2 = new Image(Display.getDefault(), ins);
			imgr.put(key, img2);
			return img2;
		}catch (Exception ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		return null;
	}
	
	private void editIcon() {
		Object x = tblIcons.getStructuredSelection().getFirstElement();
		if (!(x instanceof Icon)) return;
		Icon toEdit = (Icon)x;
		
		IconSelectionDialog dialog = new IconSelectionDialog(getShell(), toEdit, sets);
		dialog.setSession(session);
		if (dialog.open() != Window.OK) return;
		toEdit.getFiles().forEach(f->{
			if(f.getUuid() == null) session.persist(f);
		});
		
		Icon updatedIcon = session.merge(toEdit);
		int index = caicons.indexOf(toEdit);
		caicons.remove(toEdit);
		caicons.add(index, updatedIcon);
		
		updatedIcon.getFiles().forEach(f->{
			IconManager.INSTANCE.clearThumbnailFile(f);
			imgr.remove(f.getUuid().toString());
		});
		
		setDirty(true);
		tblIcons.refresh();
		selectIconSet(true);
	}
	
	private void addIcon() {
		IconSelectionDialog dialog = new IconSelectionDialog(getShell(), IconSelectionDialog.Type.NEW, sets);
		dialog.setSession(session);
		if (dialog.open() != Window.OK) return;
		
		Icon icon = dialog.getSelectedIcon();
		if (icon == null) return;
		
		icon = HibernateManager.saveOrMerge(session, icon);
		
		setDirty(true);
		caicons.add(icon);
		sortIcons();
		tblIcons.setItemCount(caicons.size());
		tblIcons.refresh();
		selectIconSet(true);
	}
	
	private void addIcon(IStructuredSelection selection) {
		
		for (Object x : selection) {
			if (!(x instanceof Icon)) return;
			
			Icon copy = (Icon)x;
			boolean exists = false;
			for(Icon c : caicons) {
				if (c.getKeyId().equals(copy.getKeyId())) {
					exists = true;
					break;
				}
			}
			if (exists) continue;
			
			Icon newIcon = new Icon();
			newIcon.setConservationArea(SmartDB.getCurrentConservationArea());
			newIcon.setKeyId(copy.getKeyId());
			newIcon.setName(copy.getName());
			newIcon.updateName(SmartDB.getCurrentLanguage(), copy.getName());
			newIcon.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), copy.getName());
			newIcon.setFiles(new ArrayList<>());
			
			for (IconSet set : sets) {
				IconFile copyfile = copy.getIconFile(set);
				
				IconFile newFile = new IconFile();
				newFile.setIcon(newIcon);
				newIcon.getFiles().add(newFile);
				newFile.setIconSet(set);
				newFile.setFilename(copyfile.getFilename());
			}
			session.persist(newIcon);
			setDirty(true);
			caicons.add(newIcon);
		}
		
		sortIcons();
		tblIcons.setItemCount(caicons.size());
		tblIcons.refresh();
		tblLibraryIcons.refresh();
		tblLibraryIcons.setSelection(null);
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
				importer.importIcons(ca, sets, caicons, iconFile, iconDirectory);
				
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
				
				importer.getIcons().forEach(i->session.merge(i));
				session.flush();
				setDirty(true);
				//add any new icons
				for (Icon c : importer.getIcons()) {
					if (!caicons.contains(c)) caicons.add(c);
				}				
				sortIcons();
				tblIcons.setItemCount(caicons.size());
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
						
						IconFKManager.INSTANCE.setIconFksToNull(session, toDelete);
						
						toDelete.forEach(e->{
							
							//there seems to be a bug in apache derby
							//that causes all values passed to the trigger to be null
							//as a result we get ca_uuid cannot be null error
							//this only appears to be a problem for 
							//re: #3401
//							session.createMutationQuery("UPDATE Attribute SET icon = null WHERE icon = :icon") //$NON-NLS-1$
//								.setParameter("icon", e) //$NON-NLS-1$
//								.executeUpdate();
//							session.createMutationQuery("UPDATE PatrolAttribute SET icon = null WHERE icon = :icon") //$NON-NLS-1$
//							.setParameter("icon", e) //$NON-NLS-1$
//							.executeUpdate();
//							
							session.remove(e);
							
							session.flush();
							monitor.worked(1);
						});
						
						caicons.removeAll(toDelete);
						toDelete.forEach(i->{
							IconManager.INSTANCE.clearThumbnailFiles(i);
							i.getFiles().forEach(rr->imgr.remove(rr.getUuid().toString()));
						});
						monitor.done();
						
					}
				});
			}catch (Exception ex) {
				SmartPlugIn.log(ex.getMessage(), ex);
			}
			
			setDirty(true);
			
			tblIcons.setItemCount(caicons.size());
			tblIcons.refresh();
			tblIcons.setSelection(null);
			tblLibraryIcons.refresh();
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
					session.merge(e);
				}
			}else if (e.getIsDefault()) {
				e.setIsDefault(false);
				session.merge(e);
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
		for (Icon c : caicons) {
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
		
		NewIconSetDialog dialog = new NewIconSetDialog(getShell(),newIconSet, sets, caicons, session);
		if (dialog.open() != Window.OK) return;
		
		toDeleteFiles.addAll(dialog.getDeleteFiles());
		setDirty(true);
		if (sets.isEmpty()) newIconSet.setIsDefault(true);
		sets.add(newIconSet);
		lstIconsets.refresh();
		createIconTable(sets, caicons);
	}
	
	private void deleteIconSet() {
		Object item = lstIconsets.getStructuredSelection().getFirstElement();
		if (item == null) return;
		if (!(item instanceof IconSet)) return;
		IconSet set = (IconSet)item;
		
		if (!MessageDialog.openConfirm(getShell(), Messages.IconPreferencePage_DeleteDialogTitle, MessageFormat.format(Messages.IconPreferencePage_DeleteSetMsg, set.getName()))) return;
		
		for (Icon i : caicons) {
			IconFile f = i.getIconFile(set);
			if (f != null) {
				i.getFiles().remove(f);
				session.remove(f);
			}
		}
		session.flush();
		session.remove(set);
		session.flush();
		setDirty(true);
		
		sets.remove(set);
		if (set.isDefault() && !sets.isEmpty()) {
			sets.get(0).setIsDefault(true);
		}
		
		lstIconsets.refresh();
		createIconTable(sets, caicons);
		selectIconSet(true);
		
		IconManager.INSTANCE.clearThumbnails();
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
		session.merge(toEdit);
		setDirty(true);
		lstIconsets.refresh();
		createIconTable(sets, caicons);
	}
	
	private void sortIcons() {
		Collections.sort(caicons);
	}
	private Job loadJob = new Job(Messages.IconPreferencePage_LoadJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			sets = new ArrayList<>();
			caicons = new ArrayList<>();
			
			sets.addAll(QueryFactory.buildQuery(session, IconSet.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
			caicons.addAll(QueryFactory.buildQuery(session, Icon.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
			
			sortIcons();
			
			List<Icon> libraryIcons = IconManager.INSTANCE.getSystemIcons(session);
			libraryIcons.forEach(e->e.getFiles().forEach(f->f.setUuid(UUID.randomUUID())));
			libraryIcons.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
						
			Display.getDefault().asyncExec(()->{
				lstIconsets.setInput(sets);
				createIconTable(sets, caicons);
				createLibraryIconTable(sets, libraryIcons);
				if (!sets.isEmpty()) {
					lstIconsets.setSelection(new StructuredSelection(sets.get(0)));
				}
				main.layout(true, true);
			});
			return Status.OK_STATUS;
		}
		
	};
	
	
	
	
}
