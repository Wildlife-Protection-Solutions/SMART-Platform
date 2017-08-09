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
package org.wcs.smart.i2.ui.views;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.export.dialog.CsvExportDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.IntelSecurityManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelRecord;
//import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.ui.DeleteRecordHandler;
import org.wcs.smart.i2.ui.RecordLabelProvider;
import org.wcs.smart.i2.ui.SectionTabHeader;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;
import org.wcs.smart.i2.ui.entity.exporter.RecordCsvExporter;
import org.wcs.smart.i2.ui.handler.OpenRecordHandler;
import org.wcs.smart.i2.ui.views.RecordsViewContentProvider.GroupBy;
import org.wcs.smart.i2.ui.views.RecordsViewContentProvider.SortBy;
import org.wcs.smart.i2.xml.RecordXmlExporter;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;
import org.wcs.smart.util.SmartUtils;


/**
 * View for displaying records 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class RecordsView {

	public static final String ID = "org.wcs.smart.i2.ui.view.records"; //$NON-NLS-1$
	
	private static final String DIR_PREF_KEY = ID + ".export.dir"; //$NON-NLS-1$

	
	private final Color LIST_HIGHLIGHT_COLOR = SmartUtils.getListHighlightColor(Display.getDefault());
	private final Color LIST_SELECTION_COLOR = SmartUtils.getListSelectedColor(Display.getDefault());
	
	@Inject
	private IEclipseContext context;
	
	public RecordsView() {
		super();
	}

	private TableViewer lstInProgress;
	private TableViewer lstNewRecords;
	private TreeViewer lstAllRecords;
	
	private RecordViewerFilter filter;
	private BasicRecordSearchPanel basicSearchPnl;

	
	private List<RecordLabelProvider> labelProviders = new ArrayList<>();
	
	private ISelectionChangedListener selectOne = new ISelectionChangedListener() {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			if (event.getSelection().isEmpty()) return;
			
			for (Viewer viewer : new Viewer[]{lstInProgress, lstNewRecords, lstAllRecords}){
				if (event.getSelectionProvider() != viewer){
					viewer.setSelection(null);
				}
			}
			
		}
	};
   
	@PostConstruct
	public void createPartControl(final Composite parent) {
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginHeight = 0;
		((GridLayout)parent.getLayout()).marginWidth = 0;
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		Composite thisParent = toolkit.createComposite(parent);
		thisParent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
 		thisParent.setLayout(new GridLayout());
 		((GridLayout)thisParent.getLayout()).marginWidth = 0;
 		((GridLayout)thisParent.getLayout()).marginHeight = 0;
 		
 		
		IDoubleClickListener openListener = new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object x = ((IStructuredSelection)event.getSelection()).getFirstElement();
				if (x instanceof IntelRecordProxy) {
					(new OpenRecordHandler()).openRecord( ((IntelRecordProxy)x).asRecord(), false);
				}else if (x instanceof IntelRecord){
					(new OpenRecordHandler()).openRecord((IntelRecord)x, false);
				}else if (x instanceof RecordEditorInput){
					(new OpenRecordHandler()).openRecord((RecordEditorInput)x, false);
				}
				
			}
		};
		
		SectionTabHeader tabList = new SectionTabHeader(new String[]{Messages.RecordsView_unprocessedSection, Messages.RecordsView_inprogressSection, Messages.RecordsView_allSection, Messages.RecordsView_basicSection}, thisParent, toolkit, thisParent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		tabList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		((GridData)tabList.getLayoutData()).verticalIndent = 2;
		
		Composite tabPart = toolkit.createComposite(thisParent, SWT.NONE);
		tabPart.setLayout(new StackLayout());
		tabPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite newRecords = toolkit.createComposite(tabPart);
		newRecords.setLayout(new GridLayout());
		RecordLabelProvider provider = new RecordLabelProvider();
		labelProviders.add(provider);
		lstNewRecords = new TableViewer(newRecords, SWT.V_SCROLL | SWT.H_SCROLL| SWT.MULTI | SWT.BORDER);
		lstNewRecords.setContentProvider(new RecordsViewContentProvider());
		lstNewRecords.setLabelProvider(new RecordsViewLabelProvider());
		lstNewRecords.setInput(new String[]{DialogConstants.LOADING_TEXT});
		lstNewRecords.addDoubleClickListener(openListener);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		lstNewRecords.getControl().setLayoutData(gd);
		lstNewRecords.addSelectionChangedListener(selectOne);
		
		Composite inProgress = toolkit.createComposite(tabPart);
		provider = new RecordLabelProvider();
		labelProviders.add(provider);
		inProgress.setLayout(new GridLayout());
		lstInProgress = new TableViewer(inProgress, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER);
	
		lstInProgress.setContentProvider(new RecordsViewContentProvider());
		lstInProgress.setLabelProvider(new RecordsViewLabelProvider());
		lstInProgress.setInput(new String[]{DialogConstants.LOADING_TEXT});
		lstInProgress.addDoubleClickListener(openListener);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		lstInProgress.getControl().setLayoutData(gd);
		lstInProgress.addSelectionChangedListener(selectOne);
		
		
		Composite allRecords = toolkit.createComposite(tabPart);
		allRecords.setLayout(new GridLayout());
		((GridLayout)allRecords.getLayout()).marginWidth = 0;
 		((GridLayout)allRecords.getLayout()).marginHeight = 0;
 		
		Composite allRecordsSection = toolkit.createComposite(allRecords);
		allRecordsSection.setLayout(new GridLayout());
		allRecordsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		FilterComposite typeFilter = new FilterComposite(allRecordsSection, SWT.NONE);
		typeFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		typeFilter.addChangeListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (typeFilter.getPatternFilter() == null || typeFilter.getPatternFilter().isEmpty()){
					lstAllRecords.removeFilter(filter);
					filter = null;
				}else{
					if (filter == null){
						filter = new RecordViewerFilter();
						filter.setFilterString(typeFilter.getPatternFilter());
						lstAllRecords.addFilter(filter);
					}else{
						filter.setFilterString(typeFilter.getPatternFilter());
						lstAllRecords.refresh();
					}
				}
			}
		});
		
		lstAllRecords = new TreeViewer(allRecordsSection, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);		
		lstAllRecords.setContentProvider(new RecordsViewContentProvider());
		lstAllRecords.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) { return ""; }; //$NON-NLS-1$
			@Override
			public Image getImage(Object element) { return null; };
		});
		
		lstAllRecords.setInput(new String[]{DialogConstants.LOADING_TEXT});
		
		final RecordsViewLabelProvider lblprovider = new RecordsViewLabelProvider(true);
		lstAllRecords.getTree().addListener(SWT.MeasureItem, new Listener() {
	 		public void handleEvent(Event event) {
	 			TreeItem item = (TreeItem)event.item;
	 			Image trailingImage = lblprovider.getImage(item.getData());
	 			String txt = lblprovider.getText(item.getData());
	 			int width = 0;
	 			int height = 0;
	 			if (trailingImage != null) {
	 				width += trailingImage.getBounds().width;
	 				height = trailingImage.getBounds().height;
	 			}
	 			width += event.gc.stringExtent(txt).x + 1;
	 			height = Math.max(height,  event.gc.stringExtent(txt).y);
	 			event.width = width;
	 			event.height = height;
	 		}
	 	});
	 	
		lstAllRecords.getTree().addListener(SWT.PaintItem, new Listener() {
			public void handleEvent(Event event) {
				TreeItem item = (TreeItem) event.item;
				Image trailingImage = lblprovider.getImage(item.getData());
				int offset = 0;
				Color c = event.gc.getBackground();
				if (trailingImage != null) {
					int x = event.x + event.width;
					int itemHeight = lstAllRecords.getTree().getItemHeight();
					int imageHeight = trailingImage.getBounds().height;
					int y = event.y + (itemHeight - imageHeight) / 2;
					event.gc.drawImage(trailingImage, x, y);
					offset = x + trailingImage.getBounds().width;
				}
				if ((event.detail & SWT.SELECTED) == SWT.SELECTED) {
					c = LIST_SELECTION_COLOR;
				}else if ( (event.detail & SWT.HOT) == SWT.HOT) {
					c = LIST_HIGHLIGHT_COLOR;
				}
				String text = lblprovider.getText(item.getData());
				event.gc.setBackground(c);
				event.gc.drawText(text, offset+1, event.y+1);
				
			}
		});
		
		lstAllRecords.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstAllRecords.addDoubleClickListener(openListener);
		lstAllRecords.addSelectionChangedListener(selectOne);

		Composite basicSearch = toolkit.createComposite(tabPart);
		basicSearch.setLayout(new GridLayout());
		((GridLayout)basicSearch.getLayout()).marginWidth = 0;
 		((GridLayout)basicSearch.getLayout()).marginHeight = 0;
		basicSearchPnl = new BasicRecordSearchPanel(basicSearch, toolkit, context);
		basicSearchPnl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tabList.setContent(new Composite[]{newRecords,inProgress,allRecords, basicSearch}, tabPart);
		tabList.selectTab(0);
		
		createMenu(lstAllRecords);
		createMenu(lstInProgress);
		createMenu(lstNewRecords);
		
		//add drag and drop support
		lstAllRecords.addDragSupport(DND.DROP_LINK,new Transfer[]{IntelRecordSelectionTransfer.getTransfer()}, new DragSourceAdapter(){
			@Override
			public void dragStart(DragSourceEvent event) {
				IntelRecordSelectionTransfer.getTransfer().setSelection(lstAllRecords.getSelection());				
			}
			@Override
			public void dragSetData(DragSourceEvent event) {
				if (IntelRecordSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
					event.data = lstAllRecords.getSelection();
				}
			}
			@Override
			public void dragFinished(DragSourceEvent event) {
				IntelRecordSelectionTransfer.getTransfer().setSelection(null);
			}
		});
		//add drag and drop support
		lstInProgress.addDragSupport(DND.DROP_LINK,new Transfer[]{IntelRecordSelectionTransfer.getTransfer()}, new DragSourceAdapter(){
			@Override
			public void dragStart(DragSourceEvent event) {
				IntelRecordSelectionTransfer.getTransfer().setSelection(lstInProgress.getSelection());				
			}
			@Override
			public void dragSetData(DragSourceEvent event) {
				if (IntelRecordSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
					event.data = lstInProgress.getSelection();
				}
			}
			@Override
			public void dragFinished(DragSourceEvent event) {
				IntelRecordSelectionTransfer.getTransfer().setSelection(null);
			}
		});
		lstNewRecords.addDragSupport(DND.DROP_LINK,new Transfer[]{IntelRecordSelectionTransfer.getTransfer()}, new DragSourceAdapter(){	
			@Override
			public void dragStart(DragSourceEvent event) {
				IntelRecordSelectionTransfer.getTransfer().setSelection(lstNewRecords.getSelection());				
			}
			@Override
			public void dragSetData(DragSourceEvent event) {
				if (IntelRecordSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
					event.data = lstNewRecords.getSelection();
				}
			}
			@Override
			public void dragFinished(DragSourceEvent event) {
				IntelRecordSelectionTransfer.getTransfer().setSelection(null);
			}
		});
		loadRecordsJob.schedule(0);
	}

	private void createMenu(StructuredViewer viewer){
		Menu m = new Menu(viewer.getControl());
		viewer.getControl().setMenu(m);
	
		MenuItem mi = new MenuItem(m, SWT.PUSH);
		mi.setText(Messages.RecordsView_OpenMenu);
		mi.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Iterator<?> iterator = ((IStructuredSelection)viewer.getSelection()).iterator(); iterator.hasNext();) {
					Object x = (Object) iterator.next();
					if (x instanceof IntelRecordProxy) {
						(new OpenRecordHandler()).openRecord( ((IntelRecordProxy)x).asRecord(), false);
					}else if (x instanceof IntelRecord){
						(new OpenRecordHandler()).openRecord((IntelRecord)x, false);
					}else if (x instanceof RecordEditorInput){
						(new OpenRecordHandler()).openRecord((RecordEditorInput)x, false);
					}
				}
			}
		});
		m.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				mi.setEnabled(!viewer.getSelection().isEmpty());
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});
		
		if (IntelSecurityManager.INSTANCE.canDeleteRecord()){
			MenuItem miDelete = new MenuItem(m, SWT.PUSH);
			miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
			miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			miDelete.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					List<Object> toDelete = new ArrayList<>();
					for (Iterator<?> iterator = ((IStructuredSelection)viewer.getSelection()).iterator(); iterator.hasNext();) {
						Object x = (Object) iterator.next();
						if (x instanceof IntelRecordProxy) {
							toDelete.add( ((IntelRecordProxy)x).asRecord() );
						}else if (x instanceof IntelRecord || x instanceof RecordEditorInput){
							toDelete.add(x);
						}
					}
					if ((new DeleteRecordHandler()).deleteRecords(toDelete, context)){
						refreshView();
					}
				}
			});
			
			m.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent e) {
					miDelete.setEnabled(!viewer.getSelection().isEmpty());
				}
				
				@Override
				public void menuHidden(MenuEvent e) {}
			});
		}
		new MenuItem(m, SWT.SEPARATOR);
		
		if (viewer instanceof TreeViewer) {
			MenuItem groupBy = new MenuItem(m, SWT.CASCADE);
			groupBy.setText(Messages.RecordsView_GroupByMenuItem);
			Menu groupByMenu = new Menu(groupBy);
			groupBy.setMenu(groupByMenu);
			for (RecordsViewContentProvider.GroupBy g : RecordsViewContentProvider.GroupBy.values()) {
				MenuItem gb = new MenuItem(groupByMenu, SWT.RADIO);
				gb.setText(g.getGuiName());
				if (g == GroupBy.NONE) {
					gb.setSelection(true);
				}
				gb.addListener(SWT.Selection, e->{
					for (MenuItem mi1 : groupByMenu.getItems()) mi1.setSelection(false);
					gb.setSelection(true);
					((RecordsViewContentProvider)lstAllRecords.getContentProvider()).setGroupBy(g);
					lstAllRecords.refresh();
					lstAllRecords.expandAll();
				});;
			}
		}
		
		MenuItem sortBy = new MenuItem(m, SWT.CASCADE);
		sortBy.setText(Messages.RecordsView_SortByMenuItem);
		Menu sortByMenu = new Menu(sortBy);
		sortBy.setMenu(sortByMenu);
		for (RecordsViewContentProvider.SortBy g : RecordsViewContentProvider.SortBy.values()) {
			MenuItem gb = new MenuItem(sortByMenu, SWT.RADIO);
			gb.setText(g.getGuiName());
			if (g == SortBy.DATE) {
				gb.setSelection(true);
			}
			gb.addListener(SWT.Selection, e->{
				for (MenuItem mi1 : sortByMenu.getItems()) mi1.setSelection(false);
				gb.setSelection(true);
				((RecordsViewContentProvider)viewer.getContentProvider()).setSortBy(g);
				viewer.refresh();
			});;
		}
		
		new MenuItem(m, SWT.SEPARATOR);
		
		if (IntelSecurityManager.INSTANCE.canViewWorkingSets()){
			MenuItem miAdd = new MenuItem(m, SWT.PUSH);
			miAdd.setText(Messages.RecordsView_AddToWs);
			miAdd.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_NEW));
			miAdd.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					List<RecordEditorInput> toAdd = new ArrayList<>();
					for (Iterator<?> iterator = ((IStructuredSelection)viewer.getSelection()).iterator(); iterator.hasNext();) {
						Object x = (Object) iterator.next();	
						if (x instanceof IntelRecordProxy) {
							toAdd.add( new RecordEditorInput(((IntelRecordProxy)x).asRecord()) );
						}else if (x instanceof IntelRecord){
							toAdd.add(new RecordEditorInput((IntelRecord) x));
						}else if (x instanceof RecordEditorInput){
							toAdd.add((RecordEditorInput) x);
							
						}
					}
					WorkingSetManager.INSTANCE.addRecordInputToActiveWorkingSetRecord(toAdd, context);
				}
			});
			m.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent e) {
					miAdd.setEnabled(!viewer.getSelection().isEmpty() && WorkingSetManager.INSTANCE.isSet());
				}
				
				@Override
				public void menuHidden(MenuEvent e) {}
			});
		}
		
		MenuItem miExport = new MenuItem(m, SWT.CASCADE);
		miExport.setText(Messages.RecordsView_ExportMenuOption);
		
		Menu exportMenu = new Menu(miExport);
		miExport.setMenu(exportMenu);
		MenuItem miExportCsv = new MenuItem(exportMenu, SWT.PUSH);
		miExportCsv.setText(Messages.RecordsView_ExportToCsv);
		miExportCsv.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<UUID> toExport = new ArrayList<UUID>();
				for (Iterator<?> iterator = ((IStructuredSelection)viewer.getSelection()).iterator(); iterator.hasNext();) {
					Object x = (Object) iterator.next();	
					if (x instanceof IntelRecordProxy) {
						toExport.add( ((IntelRecordProxy)x).getUuid() );
					}else if (x instanceof IntelRecord){
						toExport.add(((IntelRecord)x).getUuid());
					}else if (x instanceof RecordEditorInput){
						toExport.add(((RecordEditorInput)x).getUuid());
					}
				}
				doCsvExport(toExport);
			}
		});
		m.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				miExportCsv.setEnabled(!viewer.getSelection().isEmpty());
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});	
		
		MenuItem miExportXml = new MenuItem(exportMenu, SWT.PUSH);
		miExportXml.setText(Messages.RecordsView_ExportToXml);
		miExportXml.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<UUID> toExport = new ArrayList<UUID>();
				for (Iterator<?> iterator = ((IStructuredSelection)viewer.getSelection()).iterator(); iterator.hasNext();) {
					Object x = (Object) iterator.next();	
					if (x instanceof IntelRecordProxy) {
						toExport.add( ((IntelRecordProxy)x).getUuid() );
					}else if (x instanceof IntelRecord){
						toExport.add(((IntelRecord)x).getUuid());
					}else if (x instanceof RecordEditorInput){
						toExport.add(((RecordEditorInput)x).getUuid());
					}
				}
				doXmlExport(toExport);
			}
		});
		m.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				miExportXml.setEnabled(!viewer.getSelection().isEmpty());
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});	
		
		
	}
	
	public void refreshView(){
		lstAllRecords.setInput(new String[]{DialogConstants.LOADING_TEXT});
		lstInProgress.setInput(new String[]{DialogConstants.LOADING_TEXT});
		lstNewRecords.setInput(new String[]{DialogConstants.LOADING_TEXT});
		loadRecordsJob.schedule();
	}

	private void doXmlExport(List<UUID> toExport){
		if (toExport == null  || toExport.isEmpty()) return;
		
		//need to get a file
		DirectoryDialog dd = new DirectoryDialog(context.get(Shell.class));
		String ppath = Intelligence2PlugIn.getDefault().getPreferenceStore().getString(DIR_PREF_KEY);
		if (ppath != null){
			dd.setFilterPath(ppath);
		}
		dd.setText(Messages.RecordsView_SelectFolder);
		dd.setMessage(Messages.RecordsView_DdMessage );
		String path = dd.open();
		if (path == null) return;
		Intelligence2PlugIn.getDefault().getPreferenceStore().putValue(DIR_PREF_KEY, path);
		
		final Path folder = Paths.get(path);
		if (!Files.exists(folder)){
			if (!MessageDialog.openQuestion(Display.getDefault().getActiveShell(), Messages.RecordsView_CreateDirTitle,
					MessageFormat.format(Messages.RecordsView_CreateDirMessage, folder.toString()))){
				return;
			}
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(context.get(Shell.class));
		try{
			pmd.run(true,  true, new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				SubMonitor progress = SubMonitor.convert(monitor, Messages.RecordsView_XmlTaskName, toExport.size());
				
				RecordXmlExporter exporter = new RecordXmlExporter(folder);
				int cnt = 0;
				for (UUID record : toExport){
					try{
						if (exporter.exportRecord(record, progress.newChild(1))) cnt ++;
					}catch (Exception ex){
						Display.getDefault().syncExec(()->{
							MessageDialog.openError(context.get(Shell.class), Messages.RecordsView_ErrorTitle, Messages.RecordsView_ErrorMessage + ex.getMessage());
						});
						Intelligence2PlugIn.log(ex.getMessage(), ex);
					}
					if (progress.isCanceled()) break;
				}
				
				if (progress.isCanceled()){
					Display.getDefault().syncExec(()->{
						MessageDialog.openInformation(context.get(Shell.class), Messages.RecordsView_CancelledTitle, Messages.RecordsView_CanclledUser);
					});
				}else{
					
					final int totalCnt = cnt;
					Display.getDefault().syncExec(()->{
						MessageDialog.openInformation(context.get(Shell.class), Messages.RecordsView_ExportCompleteTitle, MessageFormat.format(Messages.RecordsView_ExportCompleteMessage, totalCnt, toExport.size(), folder.toString()));
					});
				}
				
			}
			});
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(Messages.RecordsView_ErrorMessage2 + ex.getMessage(), ex);
		}		
	}
	
	private void doCsvExport(List<UUID> toExport){
		if (toExport == null  || toExport.isEmpty()) return;
		RecordCsvExporter exporter = new RecordCsvExporter(toExport);
		CsvExportDialog dialog = new CsvExportDialog(context.get(Shell.class), exporter.createExportConfiguration());
		dialog.open();		
	}
	
	@Inject
	@Optional
	private void recordModified(@UIEventTopic(IntelEvents.RECORD_ALL) Object records){
		loadRecordsJob.schedule();
	}

	@Inject
	@Optional
	private void recordSourcesModified(@UIEventTopic(IntelEvents.RECORD_SOURCE_ALL) Object value){
		loadRecordsJob.schedule();
		basicSearchPnl.refreshSource();
	}
	
	@Optional
	@Inject
	private void dbModified(@UIEventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object data){
		refreshView();
	}
	
	@Focus
	public void setFocus() {
		lstInProgress.getControl().setFocus();
	}

	@PreDestroy
	public void dispose() {
		LIST_HIGHLIGHT_COLOR.dispose();
		LIST_SELECTION_COLOR.dispose();
	}
	
	public static class RecordsViewWrapper extends DIViewPart<RecordsView>{
		public RecordsViewWrapper() {
			super(RecordsView.class);
		}
	}

	Job loadRecordsJob = new Job(Messages.RecordsView_LoadingIntelRecordsJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			// -- load record source images --
			final List<IntelRecordSource> sources = new ArrayList<>();
			try(Session s = HibernateManager.openSession()){
				sources.addAll(QueryFactory.buildQuery(s, IntelRecordSource.class, "conservationArea", SmartDB.getCurrentConservationArea()).getResultList()); //$NON-NLS-1$					
			}
			
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					HashMap<IntelRecordSource, Image> srcImages = new HashMap<>();
					
					for (IntelRecordSource src : sources){
						try{
							if (src.getIconAsImage() != null){
								Image i = AWTSWTImageUtils.convertToSWTImage(src.getIconAsImage());
								srcImages.put(src, i);
							}
						}catch(Exception ex){
							Intelligence2PlugIn.log(ex.getMessage(),  ex);
						}
					}
					
					
					if (lstAllRecords.getControl().isDisposed()) return;
					for (RecordLabelProvider l : labelProviders){
						l.setSourceImages(srcImages);
					}					
				}
			});
			
			// -- load new and inprogress images --
			final List<IntelRecordProxy> inProgress = new ArrayList<IntelRecordProxy>();
			final List<IntelRecordProxy> newRecords = new ArrayList<IntelRecordProxy>();
			
			try(Session s = HibernateManager.openSession()){
				
				CriteriaBuilder cb = s.getCriteriaBuilder();
				CriteriaQuery<IntelRecord> c = cb.createQuery(IntelRecord.class);
				Root<IntelRecord> from = c.from(IntelRecord.class);
				c.where(cb.and(
						cb.equal(from.get("conservationArea"), SmartDB.getCurrentConservationArea()), //$NON-NLS-1$
						cb.or(
								cb.equal(from.get("status"), IntelRecord.Status.PROCESSING), //$NON-NLS-1$
								cb.equal(from.get("status"), IntelRecord.Status.NEW) //$NON-NLS-1$
								)
						));
				c.orderBy(cb.asc(from.get("dateCreated"))); //$NON-NLS-1$
				List<IntelRecord> records = s.createQuery(c).getResultList();
				for (IntelRecord r : records){
					if (r.getRecordSource() != null){
						r.getRecordSource().getIcon();
					}
					IntelRecordProxy proxy = null;
					proxy = new IntelRecordProxy(r.getTitle(), r.getUuid(), r.getRecordSource(), r.getStatus());
					if (r.getStatus() == IntelRecord.Status.PROCESSING){
						inProgress.add(proxy);
					}else if (r.getStatus() == IntelRecord.Status.NEW){
						newRecords.add(proxy);
					}
					if (proxy != null) proxy.setDate(computeDate(r));
				}
			}
			
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (lstInProgress.getControl().isDisposed()) return;
					lstInProgress.setInput(inProgress);
					if (lstNewRecords.getControl().isDisposed()) return;
					lstNewRecords.setInput(newRecords);
				}
			});
			
			//--load all records --
			
			final List<IntelRecordProxy> allRecords = new ArrayList<IntelRecordProxy>();
			try(Session s = HibernateManager.openSession()){
				Query<?> q = s.createQuery("SELECT title, uuid, dateCreated, recordSource.uuid, status FROM IntelRecord WHERE conservationArea = :ca ORDER BY dateModified desc"); //$NON-NLS-1$
				q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
				List<?> items = q.list();
				for (Object it : items){
					Object[] item = (Object[])it;
					IntelRecordProxy r = new IntelRecordProxy((String)item[0], (UUID)item[1], item[3] == null ? null : s.get(IntelRecordSource.class,(UUID)item[3]), (IntelRecord.Status)item[4]);
					allRecords.add(r);
					r.setDate(computeDate(s.get(IntelRecord.class, r.getUuid())));
				}
				
			}
			allRecords.sort((x,y)->-1 * x.getDate().compareTo(y.getDate()));
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (lstAllRecords.getControl().isDisposed()) return;
					lstAllRecords.setInput(allRecords);
				}
			});
			
			

			
			return Status.OK_STATUS;
		}
		
		private Date computeDate(IntelRecord r) {
			Date d = null;
			if (r.getLocations() != null) {
				for (IntelLocation l : r.getLocations()) {
					if (d == null || l.getDateTime().after(d)) {
						d = l.getDateTime();
					}
				}
			}
			if (d == null && r.getAttributes() != null) {
				for (IntelRecordAttributeValue v : r.getAttributes()) {
					if (v.getAttribute().getAttribute().getType() == AttributeType.DATE) {
						Date d2 = v.getDateValue();
						if (d2 != null) {
							if (d == null || d2.after(d)) {
								d = d2;
							}
						}
					}
				}
			}
			if (d == null) {
				d = r.getDateCreated();
			}
			return d;
		}
		
	};
	
	class RecordViewerFilter extends ViewerFilter{

		private String filterString;
		
		public void setFilterString(String filterString){
			this.filterString = ".*" + filterString.toUpperCase() + ".*"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (filterString == null || filterString.isEmpty()) return true;
			if (!(element instanceof IntelRecordProxy)) return true;
			IntelRecordProxy in = (IntelRecordProxy)element;
			if (in.getTitle().toUpperCase().matches(filterString)) return true;
			if (DateFormat.getDateInstance().format(in.getDate()).toUpperCase().matches(filterString)) return true;
			return false;
		}
		
	}
}