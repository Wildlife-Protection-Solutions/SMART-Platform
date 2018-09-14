package org.wcs.smart.icon.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.common.control.NameKeyDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

public class IconPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final int SIZE = 50;
	
	private ListViewer lstIconsets;
	private IconTable imageTable;
	
	
	private List<IconSet> sets = null;
	private List<Icon> icons = null;
	
	private Composite iconComp;
	private TableViewer tblIcons;
	
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
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.BORDER);
		main.setLayout(new GridLayout());
		
		TabFolder tabs = new TabFolder(main, SWT.TOP );
		tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TabItem itemSets = new TabItem(tabs, SWT.NONE);
		itemSets.setText("Icon Sets");
		itemSets.setControl(createSetTab(tabs));
		
		TabItem itemIcons = new TabItem(tabs, SWT.NONE);
		itemIcons.setText("Icons");
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
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		
		tblIcons = new TableViewer(panel, SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER | SWT.VIRTUAL);
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
		emptycolumn.getColumn().setText("");
		emptycolumn.getColumn().setWidth(0);
		emptycolumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return null;
			}
		});
		
		
		TableViewerColumn colName = new TableViewerColumn(tblIcons, SWT.NONE);
		colName.getColumn().setText("Name");
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
				public String getText(Object element) {
					return null;
				}
				public Image getImage(Object element) {
					if (element instanceof Icon) {
						IconFile ff = ((Icon)element).getIconFile(s);
						try {
							return SmartUtils.readSvg(getShell().getDisplay(), ff.getAttachmentFile().toPath(), SIZE);
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
		
		iconComp.layout(true);
	}
	
	private Composite createSetTab(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		
		Composite top = new Composite(main, SWT.NONE);
		top.setLayout(new GridLayout(2, false));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lstIconsets = new ListViewer(top, SWT.BORDER);
		lstIconsets.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lstIconsets.getControl().getLayoutData()).heightHint = 100;
		lstIconsets.setContentProvider(ArrayContentProvider.getInstance());
		lstIconsets.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof IconSet) return ((IconSet) element).getName();
				return super.getText(element);
			}
		});
		lstIconsets.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		ToolBar tools = new ToolBar(top, SWT.FLAT | SWT.VERTICAL);
		tools.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		ToolItem tiAdd = new ToolItem(tools, SWT.PUSH);
		tiAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		tiAdd.setToolTipText("create a new icon set");
		tiAdd.addListener(SWT.Selection, e->addIconSet());
		
		ToolItem tiDelete = new ToolItem(tools, SWT.PUSH);
		tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiDelete.setToolTipText("delete a new icon set");
		tiDelete.addListener(SWT.Selection, e->deleteIconSet());
		
		imageTable = new IconTable(top, SWT.BORDER);
		imageTable.setThumbnailSize(50);
		imageTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		lstIconsets.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object item = lstIconsets.getStructuredSelection().getFirstElement();
				if (item == null) return;
				if (!(item instanceof IconSet)) return;
				
				final IconSet iset = (IconSet)item;
				Job j = new Job("loading icons") {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						List<IconFile> files = new ArrayList<>();
						try(Session session = HibernateManager.openSession()){
							files.addAll(QueryFactory.buildQuery(session,  IconFile.class, new Object[] {"iconSet", iset}).list());
							files.forEach(f->{
								f.getIcon().getName();
								f.computeFileLocation(session);
							});
						}
						
						Display.getDefault().asyncExec(()->{
							imageTable.setAttachments(files);
						});
						return Status.OK_STATUS;
					}
					
				};
				j.schedule();
			}
		});
		
		
		
		return main;
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
				return "New Icon Set";
			}
		};
		
		if (dialog.open() == Window.OK) {
			try(Session session = HibernateManager.openSession()){
				session.beginTransaction();
				session.saveOrUpdate(newIconSet);
				session.getTransaction().commit();
				
				sets.add(newIconSet);
				lstIconsets.refresh();
			}catch (Exception ex) {
				SmartPlugIn.displayLog("Unable to add icon set: " + ex.getMessage(),  ex);
			}
		}
	}
	
	private void deleteIconSet() {
		Object item = lstIconsets.getStructuredSelection().getFirstElement();
		if (item == null) return;
		if (!(item instanceof IconSet)) return;
		IconSet set = (IconSet)item;
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			
			session.createQuery("DELETE FROM IconFile WHERE iconSet = :set")
				.setParameter("set",  set)
				.executeUpdate();
			
			session.delete(set);
			
			session.getTransaction().commit();
			
			sets.remove(set);
			lstIconsets.refresh();
		}catch (Exception ex) {
			SmartPlugIn.displayLog(MessageFormat.format("Unable to delete icon set {0}: {1}", set.getName(), ex.getMessage()), ex);
		}
	}
	

	
	private Job loadJob = new Job("loading icon sets") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			sets = new ArrayList<>();
			icons = new ArrayList<>();
			
			try(Session session = HibernateManager.openSession()){
				sets.addAll(QueryFactory.buildQuery(session, IconSet.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list());
				icons.addAll(QueryFactory.buildQuery(session, Icon.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list());
				
				icons.forEach(f->f.getFiles().forEach(c->c.computeFileLocation(session)));
			}
			
			
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
}
