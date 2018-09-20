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
package org.wcs.smart.icon.ui;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.control.NameKeyDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.internal.ca.properties.IconSelectionDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

import com.ibm.icu.text.Collator;

/**
 * Preference page for managing SMART icon sets and icons
 * @author Emily
 *
 */
public class IconPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String CURRENT_SET_KEY = "CURRENT"; //$NON-NLS-1$

	private static final int SIZE = 50;
	
	private ListViewer lstIconsets;
	private IconTable imageTable;
	
	private Composite iconComp;
	private TableViewer tblIcons;
	
	private List<Action> actions = new ArrayList<>();
	private List<Path> toDeleteFiles = new ArrayList<>();
	
	private List<IconSet> sets = null;
	private List<Icon> icons = null;
	
	//for creating new icon set from template
	private Object templateIconSet;
	
	public IconPreferencePage() {
	}

	public IconPreferencePage(String title) {
		super(title);
	}

	public IconPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void init(IWorkbench workbench) {

	}

    @Override
	public boolean performOk() {
    	try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
    		session.beginTransaction();
    		try {
    			for (Action a : actions) {
    				a.preformAction(session);
    				session.flush();
    			}
    			
	    		session.getTransaction().commit();
	    		actions.clear();
	    		
	    		for (Path p : toDeleteFiles) {
	    			try {
	    				Files.delete(p);
	    			}catch (Exception ex) {
	    				SmartPlugIn.log(ex.getMessage(),  ex);
	    			}
	    		}
	    		toDeleteFiles.clear();
    		}catch (Exception ex) {
    			SmartPlugIn.log(ex.getMessage(), ex);
    			session.getTransaction().rollback();
    			throw ex;
    		}
    	}catch (Exception ex) {
    		SmartPlugIn.displayError(MessageFormat.format(Messages.IconPreferencePage_SaveError,ex.getMessage()), ex);
    		return false;
    	}
        return true;
    }
    
	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.BORDER);
		main.setLayout(new GridLayout());
		
		TabFolder tabs = new TabFolder(main, SWT.TOP );
		tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TabItem itemSets = new TabItem(tabs, SWT.NONE);
		itemSets.setText(Messages.IconPreferencePage_IconSetsTab);
		itemSets.setControl(createSetTab(tabs));
		
		TabItem itemIcons = new TabItem(tabs, SWT.NONE);
		itemIcons.setText(Messages.IconPreferencePage_IconsTab);
		iconComp = createIconsTab(tabs);
		itemIcons.setControl(iconComp);

		loadJob.schedule();
		return main;
	}
	
	private Composite createIconsTab(Composite parent) {
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true ));

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
				if (element instanceof Icon) return ((Icon)element).getName();
				return super.getText(element);
			}
		});
		
		
		for (IconSet s : sets) {
			TableViewerColumn colIcon = new TableViewerColumn(tblIcons, SWT.NONE);
			colIcon.getColumn().setText(s.getName());
			colIcon.setLabelProvider(new ColumnLabelProvider() {
				private HashMap<Icon,Image> images = new HashMap<>();
				
				@Override
				public String getText(Object element) {
					return null;
				}
				
				@Override
				public void dispose() {
					super.dispose();
					images.values().forEach(e->e.dispose());
				}
				@Override
				public Image getImage(Object element) {
					if (images.containsKey(element))  return images.get(element);
					if (element instanceof Icon) {
						IconFile ff = ((Icon)element).getIconFile(s);
						if (ff == null) return null;
						try {
							File f = null;
							if (ff.getCopyFromLocation() != null) {
								f = ff.getCopyFromLocation();
							}else {
								f = ff.getAttachmentFile();
							}
							Image img = SmartUtils.getImage(f.toPath(),SIZE);
							images.put((Icon)element, img);
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
		actions.add(Action.createSaveAction(toEdit));
		tblIcons.refresh();
		selectIconSet(true);
	}
	
	private void addIcon() {
		IconSelectionDialog dialog = new IconSelectionDialog(getShell(), IconSelectionDialog.Type.NEW, sets);
		if (dialog.open() != Window.OK) return;
		
		Icon icon = dialog.getSelectedIcon();
		if (icon == null) return;
		actions.add(Action.createSaveAction(icon));
		icons.add(icon);
		tblIcons.setItemCount(icons.size());
		tblIcons.refresh();
		selectIconSet(true);
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
		
		
		toDelete.forEach(e->actions.add(Action.createDeleteAction(e)));
		icons.removeAll(toDelete);
		tblIcons.setItemCount(icons.size());
		tblIcons.refresh();
		selectIconSet(true);
	}
	
	private Composite createSetTab(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		
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
		imageTable.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
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
					actions.add(Action.createSaveAction(e));
				}
			}else if (e.getIsDefault()) {
				e.setIsDefault(false);
				actions.add(Action.createSaveAction(e));
			}
		});
		lstIconsets.refresh();
	}
	
	private void selectIconSet(boolean refresh) {
		Object item = lstIconsets.getStructuredSelection().getFirstElement();
		if (item == null) return;
		if (!(item instanceof IconSet)) return;
		
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
		List<IconSet> allSets = new ArrayList<>();
		allSets.addAll( sets );
		for (String fixed : IconSet.FIXED_KEYS) {
			IconSet t1 = new IconSet();
			t1.setKeyId(fixed);
			allSets.add(t1);
		}
		
		IconSet newIconSet = new IconSet();
		newIconSet.setConservationArea(SmartDB.getCurrentConservationArea());;
		newIconSet.setIsDefault(false);
		
		NameKeyDialog<IconSet> dialog = new NameKeyDialog<IconSet>(getShell(), newIconSet, allSets){
			@Override
			protected String getTitle(){
				return Messages.IconPreferencePage_NewIconDialogTitle;
			}
			
			public Point getInitialSize(){
				return new Point(400, 250);
			}
			
			@Override
			protected Control createDialogArea(Composite parent) {
				Composite u = (Composite)super.createDialogArea(parent);
				
				Composite addTo = (Composite) u.getChildren()[0];
				
				Label l = new Label(addTo, SWT.NONE);
				l.setText(Messages.IconPreferencePage_TemplateIconLabel);
				
				ComboViewer cmbViewer = new ComboViewer(addTo, SWT.DROP_DOWN | SWT.READ_ONLY);
				cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
				cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
				cmbViewer.setLabelProvider(new LabelProvider() {
					public String getText(Object element) {
						if (element instanceof IconSet) return ((IconSet) element).getName();
						return super.getText(element);
					}
				});
				List<Object> ins = new ArrayList<>();
				ins.add(""); //$NON-NLS-1$
				ins.addAll(sets);
				cmbViewer.setInput(ins);
				
				templateIconSet = null;
				cmbViewer.addSelectionChangedListener(e->templateIconSet=cmbViewer.getStructuredSelection().getFirstElement());
				return u;
			}
		};
		
		if (dialog.open() != Window.OK) return;
		
		actions.add(Action.createSaveAction(newIconSet));
			
		if (templateIconSet != null && templateIconSet instanceof IconSet) {
			for (Icon icon : icons) {
				IconFile copyIcon = icon.getIconFile((IconSet) templateIconSet);
				if (copyIcon != null) {
					IconFile newfile = new IconFile();
					newfile.setIcon(icon);
					newfile.setIconSet(newIconSet);
					try {
						if (copyIcon.isSystemIcon()) {
							newfile.setFilename(copyIcon.getFilename());
						}else {
							Path temp = Files.createTempFile("smart", "icon"); //$NON-NLS-1$ //$NON-NLS-2$
							try(OutputStream out = Files.newOutputStream(temp)){
								Files.copy(copyIcon.getAttachmentFile().toPath(), out);
							}
							newfile.setCopyFromLocation(temp.toFile());
							newfile.setFilename(copyIcon.getFilename());
							
							toDeleteFiles.add(temp);
						}
						icon.getFiles().add(newfile);
					}catch (Exception ex) {
						SmartPlugIn.displayLog(MessageFormat.format(Messages.IconPreferencePage_CopyError,  icon.getName()), ex);
					}
					actions.add(Action.createSaveAction(icon));
				}
			}
		}
		if (sets.isEmpty()) {
			newIconSet.setIsDefault(true);
		}
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
			}
		}
		actions.add(Action.createDeleteAction(set));
		
		sets.remove(set);
		if (set.isDefault() && !sets.isEmpty()) {
			sets.get(0).setIsDefault(true);
		}
		
		lstIconsets.refresh();
		createIconTable(sets, icons);
	}
	

	private void editIconSet() {
		if (sets == null) return;
		List<IconSet> allSets = new ArrayList<>();
		allSets.addAll( sets );
		for (String fixed : IconSet.FIXED_KEYS) {
			IconSet t1 = new IconSet();
			t1.setKeyId(fixed);
			allSets.add(t1);
		}
		
		Object item = lstIconsets.getStructuredSelection().getFirstElement();
		if (item == null) return;
		if (!(item instanceof IconSet)) return;
		IconSet toEdit = (IconSet)item;
		
		NameKeyDialog<IconSet> dialog = new NameKeyDialog<IconSet>(getShell(), toEdit, allSets){
			@Override
			protected String getTitle(){
				return Messages.IconPreferencePage_EditIconDialogTitle;
			}
		};
		
		if (dialog.open() != Window.OK) return;
		actions.add(Action.createSaveAction(toEdit));
		
		lstIconsets.refresh();
		createIconTable(sets, icons);
	}
	
	private Job loadJob = new Job(Messages.IconPreferencePage_LoadJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			sets = new ArrayList<>();
			icons = new ArrayList<>();
			
			try(Session session = HibernateManager.openSession()){
				sets.addAll(QueryFactory.buildQuery(session, IconSet.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
				icons.addAll(QueryFactory.buildQuery(session, Icon.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
				
				icons.forEach(f->{
					f.getFiles().forEach(c->c.computeFileLocation(session));
					f.getNames().size();
				});
				sets.forEach(s->s.getNames().size());
			}
			
			icons.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
			
			Display.getDefault().asyncExec(()->{
				lstIconsets.setInput(sets);
				if (!sets.isEmpty()) {
					lstIconsets.setSelection(new StructuredSelection(sets.get(0)));
				}
				if (!icons.isEmpty()) {
					createIconTable(sets, icons);
				}
				
			});
			return Status.OK_STATUS;
		}
		
	};
	
	private static class Action{
		public enum Type{
			SAVE,
			DELETE
		}
		
		public static Action createDeleteAction(UuidItem object) {
			return new Action(Type.DELETE, object);
		}
		
		public static Action createSaveAction(UuidItem object) {
			return new Action(Type.SAVE, object);
		}
		
		private Type type;
		private UuidItem object;
		
		private Action (Type type, UuidItem object) {
			this.type = type;
			this.object = object;
		}
		
		public void preformAction(Session session) { 
			if (type == Type.SAVE) {
				session.saveOrUpdate(object);
			}else if (type == Type.DELETE) {
				if (object.getUuid() == null) return;
				session.delete(object);
			}
		}
	}
}
