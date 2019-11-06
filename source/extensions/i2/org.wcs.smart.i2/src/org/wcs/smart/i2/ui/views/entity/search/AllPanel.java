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
package org.wcs.smart.i2.ui.views.entity.search;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.InternalEntityManager;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.search.AllEntitySearch;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.Resources;
import org.wcs.smart.i2.ui.dialogs.ExportEntityToFileDialog;
import org.wcs.smart.i2.ui.editors.record.RecordEditor;
import org.wcs.smart.i2.ui.entity.exporter.EntityRelationshipExportDialog;
import org.wcs.smart.i2.ui.handler.CompareEntitiesHandler;
import org.wcs.smart.i2.ui.handler.NewRecordHandler;
import org.wcs.smart.i2.ui.handler.OpenEntityHandler;
import org.wcs.smart.i2.ui.views.EntitySearchView;
import org.wcs.smart.i2.ui.views.IntelEntitySelectionTransfer;
import org.wcs.smart.i2.ui.views.entity.search.AllEntityContentProvider.EntityTableData;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.E3Utils;
import org.wcs.smart.util.UuidUtils;

/**
 * All entity table panel for entity search view
 * 
 * @author Emily
 *
 */
public class AllPanel extends Composite {

	/*
	 * Preference store key for the last entity search run
	 */
	private static final String LAST_SEARCH_KEY_PREFIX = "org.wcs.smart.i2.ui.views.entity.search.all"; //$NON-NLS-1$
	
	@Inject
	private IEclipseContext context;
	private EntitySearchView view;
	
	//ui components
	private FormToolkit toolkit;
	private Composite tableComposite;
	private TableViewer entityTable;
	private EntitySearchPanel searchPanel;
	private Label cntLabel;
			
	//search object
	private AllEntitySearch entitySearch;
	private boolean needsRefresh = false;
	
	public AllPanel(Composite parent, EntitySearchView view, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.view = view;
		this.toolkit = toolkit;
		entitySearch = new AllEntitySearch(null);
		
		addDisposeListener(e->{
			toolkit.dispose();
			
			try(Session session = HibernateManager.openSession()){
				try {
					session.beginTransaction();
					session.createNativeQuery("DROP TABLE " + AllEntityContentProvider.DB_NAME_NAME); //$NON-NLS-1$
					session.getTransaction().commit();
				}catch (Exception ex) {
					//don't worry about it;
					ex.printStackTrace();
				}
			}
			if (entitySearch != null) {
				//save current search to preference store to reload when application is restarted
				entitySearch.setFilterString(getFilterString());
				String toSave = entitySearch.serialize();	
				Intelligence2PlugIn.getDefault().getPreferenceStore().setValue(getPreferenceStoreKey(), toSave);
			}
			
		});
		
		createContents();
	}

	private String getPreferenceStoreKey() {
		return LAST_SEARCH_KEY_PREFIX + "." + UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid()); //$NON-NLS-1$
	}
	
	public Composite createResultsComposite(Composite parent) {
		tableComposite = toolkit.createComposite(parent);
		
		tableComposite.setLayout(new GridLayout());
		((GridLayout)tableComposite.getLayout()).marginWidth = 0;
		((GridLayout)tableComposite.getLayout()).marginHeight = 0;
		
		//only do search when this panel is viewed
		String toLoad = Intelligence2PlugIn.getDefault().getPreferenceStore().getString(getPreferenceStoreKey());
		if (toLoad != null && !toLoad.isEmpty()) {
			AllEntitySearch load = AllEntitySearch.parse(toLoad, Collections.singleton(SmartDB.getCurrentConservationArea()));
			if (load != null && (load.getQueryColumns() != null || !load.getFilterString().isEmpty())) {
				//if load is not empty; then load it and display filter panel
				this.entitySearch = load;
				searchPanel.initPanel(load.getFilterString());
			}
		}
		
		
		Listener firstSearch = new Listener() {
			@Override
			public void handleEvent(Event event) {
				tableComposite.removeListener(SWT.Paint, this);
				refresh(0);
			}};
		Listener otherRefresh = new Listener() {
				@Override
				public void handleEvent(Event event) {
					if (needsRefresh) refreshJob.schedule();
				}};
		tableComposite.addListener(SWT.Paint, firstSearch);
		tableComposite.addListener(SWT.Paint, otherRefresh);
		
		return tableComposite;
	}
	
	private void createContents() {
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		searchPanel = new EntitySearchPanel(this) {
			@Override
			public void saveSearch() {
				entitySearch.setFilterString(getQueryString());
				view.saveSearch(entitySearch);
			}
			
			@Override
			public void doSearch() {
				if (entityTable == null || entityTable.getControl().isDisposed()) {
					refresh(0);
				}else {
					((AllEntityContentProvider)entityTable.getContentProvider()).setFilter(getQueryString());
				}
			}
			
			@Override
			void clearPanel() {
				super.clearPanel();
				doSearch();
			}
		};
		searchPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		ToolItem refreshItem = new ToolItem(searchPanel.getToolbar(), SWT.PUSH);
		refreshItem.addListener(SWT.Selection, e->searchPanel.doSearch());
		refreshItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RUN));
		refreshItem.setToolTipText(Messages.AllPanel_loadresultstooltip);
		
		ToolItem configureItem = new ToolItem(searchPanel.getToolbar(), SWT.PUSH);
		configureItem.addListener(SWT.Selection, e->configureTable());
		configureItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_CONFIGURE));
		configureItem.setToolTipText(Messages.AllPanel_configtabletooltip);
		
	}
	
	
	/**
	 * Initialize the panel with the contents from the saved search
	 * @param search
	 */
	public void initPanel(AllEntitySearch search) {
		this.entitySearch = search;
		searchPanel.initPanel(search.getFilterString());
		createTable();
		searchPanel.doSearch();
	}
	
	/**
	 * Clears the table and reruns the search
	 */
	public void refresh(long delay) {
		if (tableComposite == null) return; 
		for (Control c : tableComposite.getChildren()) c.dispose();
		toolkit.createLabel(tableComposite, DialogConstants.LOADING_TEXT);
		tableComposite.layout();
		
		if (tableComposite.isVisible()) {
			refreshJob.schedule();
		}else {
			//don't refresh immediately - only refresh when it becomes visible
			//which is done via a paint event
			needsRefresh = true;
		}
	}

	/*
	 * displays dialog for configuring table columns
	 */
	private void configureTable() {
		ColumnSelectorDialog dialog = new ColumnSelectorDialog(getShell(), entitySearch.getQueryColumns());
		if (dialog.open() != Window.OK) return;
		
		entitySearch.setQueryColumns(dialog.getVisibleColumns());
		createTable();
	}
	
	/*
	 * recreates the table with the existing data/content provider
	 */
	private void createTable() {
		if (entityTable == null || entityTable.getControl().isDisposed()) return;
		EntityTableData data = (EntityTableData) entityTable.getInput();
		AllEntityContentProvider provider = (AllEntityContentProvider) entityTable.getContentProvider();
		createTable(data, provider);	
	}
	
	/*
	 * recreates the table with the provided data/content provider
	 */
	//must be called from display thread
	private void createTable(EntityTableData data, AllEntityContentProvider provider) {
		if (tableComposite.isDisposed()) return;
		for (Control c : tableComposite.getChildren()) c.dispose();

		
		entityTable = new TableViewer(tableComposite, SWT.BORDER | SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.MULTI);
		entityTable.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		entityTable.setContentProvider(provider);
		entityTable.getTable().setHeaderVisible(true);
		entityTable.getTable().setLinesVisible(true);
		entityTable.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				openEntities();
			}
		});
		
		TableViewerColumn idColumn = new TableViewerColumn(entityTable, SWT.NONE);
		idColumn.getColumn().setText(Messages.AllPanel_EntityIdColumn);
		idColumn.getColumn().setWidth(150);
		
		idColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element == null) return "..."; //$NON-NLS-1$
				String keyId = ((EntityTableRowItem)element).getId();
				
				Object value =  ((EntityTableRowItem)element).getAttributeValue(keyId);
				IntelAttribute attribute = null;
				for (IntelAttribute aa : data.getAttributes()) {
					if (aa.getKeyId().equals(keyId)) {
						attribute = aa;
						break;
					}
				}
				if (value == null || attribute == null) return ""; //$NON-NLS-1$
				switch(attribute.getType()) {
				case BOOLEAN:
					if (((Double)value) < 0.5) return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
					return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
				case DATE:
					return DateFormat.getDateInstance().format(java.sql.Date.valueOf((String)value));
				case EMPLOYEE:
				case LIST:
				case NUMERIC:
				case POSITION:
				case TEXT:
					return value.toString();
				}
				
				return ((EntityTableRowItem)element).getId();
			}
		});
		
		TableViewerColumn profileColumn = new TableViewerColumn(entityTable, SWT.NONE);
		profileColumn.getColumn().setText("Profile");
		profileColumn.getColumn().setWidth(50);
		profileColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element == null) return "...";
				return ((EntityTableRowItem)element).getProfileName();
			}
			public Image getImage(Object element) {
				if (element == null) return null;
				return Resources.INSTANCE.getProfileImage(((EntityTableRowItem)element).getProfileUuid());
			}
		});
		
		TableViewerColumn typeColumn = new TableViewerColumn(entityTable, SWT.NONE);
		typeColumn.getColumn().setText(Messages.AllPanel_EntityTypeColumn);
		typeColumn.getColumn().setWidth(150);
		typeColumn.getColumn().addListener(SWT.Selection, e->{
			provider.setSortColumn(AllEntityContentProvider.COL_ENTITY_TYPE_NAME);
			entityTable.getTable().setSortColumn(typeColumn.getColumn());
			entityTable.getTable().setSortDirection(provider.getSortDirection());
		});
		typeColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element == null) return "..."; //$NON-NLS-1$
				return ((EntityTableRowItem)element).getType();
			}
		});
		
		for (IntelAttribute attribute : data.getAttributes()) {
			if (entitySearch.getQueryColumns() != null && !entitySearch.getQueryColumns().contains(attribute.getKeyId())) continue;
			
			TableViewerColumn aColumn = new TableViewerColumn(entityTable, SWT.NONE);
			aColumn.getColumn().setText(attribute.getName());
			aColumn.getColumn().setWidth(150);
			aColumn.getColumn().addListener(SWT.Selection, e->{
				provider.setSortColumn(attribute.getKeyId());
				entityTable.getTable().setSortColumn(aColumn.getColumn());
				entityTable.getTable().setSortDirection(provider.getSortDirection());
			});
			
			aColumn.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					if (element == null) return "..."; //$NON-NLS-1$
					Object value =  ((EntityTableRowItem)element).getAttributeValue(attribute.getKeyId());
					if (value == null) return ""; //$NON-NLS-1$
					switch(attribute.getType()) {
					case BOOLEAN:
						if (((Double)value) < 0.5) return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
						return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
					case DATE:
						return DateFormat.getDateInstance().format((Date)value);
					case EMPLOYEE:
					case LIST:
					case NUMERIC:
					case POSITION:
					case TEXT:
						return value.toString();
					}
					return element.toString();
				}
			});
		}

		cntLabel = new Label(tableComposite, SWT.NONE);
		cntLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cntLabel.setText(DialogConstants.LOADING_TEXT);
		
		tableComposite.layout(true);
		entityTable.setInput(data);
		
		//create menu
		createMenu(entityTable.getTable());
		addDragListeners();
	}
	
	private void addDragListeners() {
		final IntelEntitySelectionTransfer trans = IntelEntitySelectionTransfer.getTransfer();
		DragSourceAdapter listener = new DragSourceAdapter() {
			
			@Override
			public void dragStart(DragSourceEvent event) {
				List<IntelEntity> items = new ArrayList<>();
				for (EntityTableRowItem i : getCurrentSelection()) {
					IntelEntity temp = new IntelEntity() {
						public String getIdAttributeAsText() {
							return i.getId();
						}
					};
					temp.setUuid(i.getEntityUuid());
					
					IntelProfile profiletemp = new IntelProfile();
					profiletemp.setUuid(i.getProfileUuid());
					profiletemp.setName(i.getProfileName());
					
					temp.setProfile(profiletemp);
					
					items.add(temp);
				}
				IntelEntitySelectionTransfer.getTransfer().setSelection(new StructuredSelection(items));				
			}
			@Override
			public void dragSetData(DragSourceEvent event) {
				if (IntelEntitySelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
					List<IntelEntity> items = new ArrayList<>();
					for (EntityTableRowItem i : getCurrentSelection()) {
						IntelEntity temp = new IntelEntity();
						temp.setUuid(i.getEntityUuid());
						items.add(temp);
					}
					
					event.data = new StructuredSelection(items);
				}
			}
			@Override
			public void dragFinished(DragSourceEvent event) {
				IntelEntitySelectionTransfer.getTransfer().setSelection(null);
			}
		};
		DragSource dragSource = new DragSource(entityTable.getControl(), DND.DROP_LINK);
		dragSource.setTransfer(new Transfer[]{trans});
		dragSource.addDragListener(listener);
	}
	
	public List<Object> getEntities(int limit){
		return ((AllEntityContentProvider)entityTable.getContentProvider()).getAllDataItems(limit);
	}
	
	public List<EntityTableRowItem> getCurrentSelection(){
		ArrayList<EntityTableRowItem> selections = new ArrayList<>();
		if (entityTable == null || entityTable.getTable().isDisposed()) return selections;
		
		for (Iterator<?> iterator = entityTable.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof EntityTableRowItem) {
				selections.add((EntityTableRowItem) x);
			}	
		}
		return selections;
	}
	
	public List<IntelEntity> getCurrentEntities(){
		List<IntelEntity> ies = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			for (EntityTableRowItem item : getCurrentSelection()) {
				IntelEntity ie = session.get(IntelEntity.class, item.getEntityUuid());
				if (ie != null) {
					ie.getIdAttributeAsText();
					ies.add(ie);
				}
			}
		}
		return ies;
	}
	private void openEntities(){
		if (!getCurrentSelection().isEmpty()){
			List<IntelEntity> toOpen = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				for (EntityTableRowItem item : getCurrentSelection()) {
					UUID entityUuid = ((EntityTableRowItem)item).getEntityUuid();
					IntelEntity temp = null;
					temp = session.get(IntelEntity.class, entityUuid);
					if (temp != null) {
						temp.getIdAttributeAsText();
						temp.getEntityType();
						toOpen.add(temp);
					}
					
				}
			}
			toOpen.forEach(e->(new OpenEntityHandler()).openEntity(e, context));
		}
	}
	
	private void exportEntity(){
		if (getCurrentSelection().isEmpty()) return;
		EntityTableRowItem item = getCurrentSelection().get(0);
		
		IntelEntity ie = null;
		try(Session session = HibernateManager.openSession()){
			ie = session.get(IntelEntity.class, item.getEntityUuid());
			if (ie == null) return;
			ie.getIdAttributeAsText();
		}
		EntityRelationshipExportDialog dialog = new EntityRelationshipExportDialog(ie, getShell());
		dialog.open();
	}
	
	private void exportEntityToFile(boolean filter){
		List<UUID> toexport = null;
		if (filter) {
			toexport = getCurrentSelection().stream().map(e->e.getEntityUuid()).collect(Collectors.toList());
			if (toexport.isEmpty()) return;
		}else {
			//get all uuids from content provider
			if (entityTable == null || entityTable.getTable().isDisposed()) return;
			toexport = ((AllEntityContentProvider)entityTable.getContentProvider()).getAllDataItems();
		}
		ExportEntityToFileDialog dialog = new ExportEntityToFileDialog(getShell(), toexport);
		dialog.open();
	}
		
	private void createMenu(Control parent){
		Menu menu = new Menu(parent);
		
		MenuItem mnuOpen = new MenuItem(menu, SWT.PUSH);
		mnuOpen.setText(Messages.EntitySearchResultTable_OpenItem);
		mnuOpen.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openEntities();
			}
		});
		
		MenuItem mnuCompare = new MenuItem(menu, SWT.PUSH);
		mnuCompare.setText(Messages.EntitySearchResultTable_CompareItem);
		mnuCompare.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				try{
					List<IntelEntity> ies = new ArrayList<>();
					try(Session session = HibernateManager.openSession()){
						for (EntityTableRowItem item : getCurrentSelection()) {
							IntelEntity ie = session.get(IntelEntity.class, item.getEntityUuid());
							if (ie != null) {
								ie.getIdAttributeAsText();
								ies.add(ie);
							}
						}
					}
					(new CompareEntitiesHandler()).compare(ies, context.get(EPartService.class));
				}catch (Exception ex){
					MessageDialog.openInformation(getShell(), Messages.EntitySearchResultTable_CompareErrorDialogTitle, ex.getMessage());
				}
			}
		});
		
		new MenuItem(menu, SWT.SEPARATOR);
		
		MenuItem mnuPrint = new MenuItem(menu, SWT.PUSH);
		mnuPrint.setText(Messages.EntitySearchResultTable_PrintMenuItem);
		mnuPrint.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_PDF));
		mnuPrint.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<UUID> uuids = getCurrentSelection().stream().map(row->row.getEntityUuid()).collect(Collectors.toList());
				InternalEntityManager.INSTANCE.printEntities(getShell(), uuids);
			}
		});
		
		MenuItem miExport = new MenuItem(menu, SWT.CASCADE);
		miExport.setText(Messages.EntitySearchResultTable_ExportMenu);
		
		Menu exportMenu  = new Menu(miExport);
		miExport.setMenu(exportMenu);
		
		MenuItem mnuExport = new MenuItem(exportMenu, SWT.PUSH);
		mnuExport.setText(Messages.EntitySearchResultTable_ExportMenuItem2);
		mnuExport.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ENTITY_EXPORT));
		mnuExport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportEntity();
			}
		});
		
		MenuItem mnuExportXml = new MenuItem(exportMenu, SWT.PUSH);
		mnuExportXml.setText(Messages.EntitySearchResultTable_ExportToXML);
		mnuExportXml.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportEntityToFile(true);
			}
		});
		
		MenuItem mnuExportXml2 = new MenuItem(exportMenu, SWT.PUSH);
		mnuExportXml2.setText(Messages.EntitySearchResultTable_ExportAllEntitiesToXml);
		mnuExportXml2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportEntityToFile(false);
			}
		});
		
		if (IntelSecurityManager.INSTANCE.canDeleteEntityAny()){
			MenuItem mnuDelete = new MenuItem(menu, SWT.PUSH);
			mnuDelete.setText(Messages.EntitySearchResultTable_DeleteMenuItem);
			mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			mnuDelete.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					List<UUID> uuids = getCurrentSelection().stream().map(item->item.getEntityUuid()).collect(Collectors.toList());
					InternalEntityManager.INSTANCE.deleteEntities(getShell(), context.get(EPartService.class), context.get(IEventBroker.class), uuids);
				}
			});
		}
		
		
		MenuItem mnuWorkingset = null;
		if (IntelSecurityManager.INSTANCE.canEditWorkingSet()) {
			new MenuItem(menu, SWT.SEPARATOR);
			mnuWorkingset = new MenuItem(menu, SWT.PUSH);
			mnuWorkingset.setText(Messages.EntitySearchResultTable_AddToWsMenuItem);
			mnuWorkingset.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_NEW));
			mnuWorkingset.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					WorkingSetManager.INSTANCE.addEntityToActiveWorkingSet(getCurrentEntities(), context);
				}
			});
		}
		
		MenuItem fmnuWorkingset = mnuWorkingset;
		menu.addMenuListener(new MenuListener() {
			private MenuItem mnuAddToRecord = null;
			private Menu subRecord = null;
			@Override
			public void menuShown(MenuEvent e) {
				boolean hasSelection = !getCurrentSelection().isEmpty();
				mnuOpen.setEnabled(hasSelection);
				mnuPrint.setEnabled(hasSelection);
				if (fmnuWorkingset != null) fmnuWorkingset.setEnabled(hasSelection && WorkingSetManager.INSTANCE.isSet());
				mnuCompare.setEnabled(getCurrentSelection().size() > 0);
				
				if (mnuAddToRecord == null || mnuAddToRecord.isDisposed()){
					mnuAddToRecord = new MenuItem(menu, SWT.CASCADE);
					mnuAddToRecord.setText(Messages.EntitySearchResultTable_AddToRecordMenuItem);
				
					subRecord = new Menu(menu);
					mnuAddToRecord.setMenu(subRecord);
				}
				if (subRecord != null && !subRecord.isDisposed()){
					for (MenuItem mi : subRecord.getItems()){
						mi.dispose();
					}
				}
				
				if (IntelSecurityManager.INSTANCE.canCreateRecordAny()) {
					MenuItem createRecord = new MenuItem(subRecord, SWT.PUSH);
					createRecord.setText(Messages.AllPanel_CreateRecord);
					createRecord.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
					createRecord.addListener(SWT.Selection, cr->{
						List<UUID> uuids = new ArrayList<>();
						getCurrentSelection().forEach(entity->uuids.add(entity.getEntityUuid()));
						IEclipseContext kid = context.createChild();
						kid.set(NewRecordHandler.ENTITY_UUID_LINK, uuids);
						(new NewRecordHandler()).createNewRecord(kid);
					});
				}
				
				if (IntelSecurityManager.INSTANCE.canEditRecordAny()) {
					Collection<MPart> parts = context.get(EPartService.class).getParts();
					boolean first = false;
					for (MPart p : parts){
						if (E3Utils.isCompatibilityEditor(p)){
							Object editor = E3Utils.getSourceObject(p);
							if (editor instanceof RecordEditor && ((RecordEditor)editor).getEditMode()){
								if (!first) {
									new MenuItem(subRecord, SWT.SEPARATOR);
									first = true;
								}
								MenuItem relate = new MenuItem(subRecord, SWT.PUSH);
								relate.setText( ((RecordEditor)editor).getRecord().getTitle()  );
								relate.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent e) {
										if (!getCurrentSelection().isEmpty()){
											for (IntelEntity entity : getCurrentEntities()){
												((RecordEditor)editor).linkEntity(entity);
											}
										}
									}
								});
							}
						}
					}
				}
				if (subRecord == null || subRecord.getItemCount() == 0){
					mnuAddToRecord.dispose();
				}
				
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
//		parent.setMenu(menu);
		List<Control> kids = new ArrayList<Control>();
		kids.add(parent);
		while(!kids.isEmpty()){
			Control c = kids.remove(0);
			c.setMenu(menu);
			if (c instanceof Composite){
				for (Control cc : ((Composite)c).getChildren()){
					kids.add(cc);
				}
			}
		}
	}
	
	private String getFilterString() {
		return searchPanel.getQueryString();
	}
	
	private Job refreshJob = new Job(Messages.AllPanel_refreshJobName) {

		private String filterString = null;
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			needsRefresh = false;
			AllEntityContentProvider provider = new AllEntityContentProvider();
			
			EntityTableData data = provider.generateData();
			provider.addListener(e->{
				EntityTableData thisdata = (EntityTableData)e.data;
				if (cntLabel.isDisposed()) return;
				if (thisdata != null) {
					cntLabel.setText(MessageFormat.format(Messages.AllPanel_CountLabel, thisdata.getDisplayCount(), thisdata.getTotalCount()));
				}else {
					cntLabel.setText(""); //$NON-NLS-1$
				}
				
			});
			
			
			Display.getDefault().syncExec(()->{filterString = getFilterString();});
			
			if (filterString != null && !filterString.isEmpty()) {
				provider.setFilter(filterString);
			}
			
			if (data == null) {
				Display.getDefault().syncExec(()->{
					toolkit.createLabel(tableComposite, Messages.AllPanel_ErrorLabel);
					tableComposite.layout(true);
				});
				return Status.OK_STATUS;

			}
			Display.getDefault().syncExec(()->{
				createTable(data, provider);
				
			});
			
			return Status.OK_STATUS;
		}
		
	};


}