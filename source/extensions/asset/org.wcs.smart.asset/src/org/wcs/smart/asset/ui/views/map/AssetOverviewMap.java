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
package org.wcs.smart.asset.ui.views.map;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.geotools.styling.Style;
import org.hibernate.Session;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.style.sld.SLDContent;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.AssetSecurityManager;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.map.engine.OverviewmapColumnEngine;
import org.wcs.smart.asset.map.engine.StatusEngine;
import org.wcs.smart.asset.model.AssetMapStyle;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn.GroupByOption;
import org.wcs.smart.asset.ui.views.map.udig.AssetStationSummaryGeoResource;
import org.wcs.smart.asset.ui.views.map.udig.AssetStationSummaryService;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.udig.style.StyleManager;
import org.wcs.smart.ui.SectionHeader;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SharedUtils;

/**
 * Overview map for asset view
 * @author Emily
 *
 */
public class AssetOverviewMap extends SmartMapEditorPart implements IEditorPart{

	public static final String ID = "org.wcs.smart.asset.overviewmap"; //$NON-NLS-1$

	private static final String SAVE_STYLES = Messages.AssetOverviewMap_SaveStyle;
	private static final String MANAGE_STYLES = Messages.AssetOverviewMap_ManageStyle;
	
	public static IEditorInput OVERVIEW_MAP_INPUT = new IEditorInput() {		
		@Override
		public <T> T getAdapter(Class<T> adapter) { return null; }
		@Override
		public String getToolTipText() { return null; }
		@Override
		public IPersistableElement getPersistable() { return null; }
		@Override
		public String getName() { return Messages.AssetOverviewMap_Title; }
		@Override
		public ImageDescriptor getImageDescriptor() { return null; }
		@Override
		public boolean exists() { return false; }
	};
	
	private TableComboViewer cmbStyles;
	
	private DateFilterDropDownComposite dateFilters;
	private TableViewer summaryTable;
	private AssetSummaryTable assetTable;
	private AssetMapColumnConfiguration tableConfiguration;
	private Composite tableComposite;
	private Composite statusTableComposite;
	private Composite assetTableComposite;
	private AssetStationSummaryService service;
	private StatusCanvas canvas;
	
	private List<ILayer> mapLayers;
	private GroupByOption currentGroupByOption = GroupByOption.STATION;
	private AssetMapStyle lastStyle = null;
	
	private IEclipseContext parentContext;
	private List<EventHandler> handlers = null;
	private boolean fireStyleChange = true;
	
	private OverviewmapColumnEngine statEngine = new OverviewmapColumnEngine(SmartDB.getCurrentConservationArea()) {
		
		public void refreshData() { 
			Display.getDefault().syncExec(()->{
				if (summaryTable.getTable().isDisposed()) return;
				summaryTable.refresh();
			});
		}
		
		public void finish() { 
			Display.getDefault().syncExec(()->{
				if (summaryTable.getTable().isDisposed()) return;
				summaryTable.setInput(getData());
				summaryTable.refresh();
				getMap().getRenderManager().refresh(null);
			});
		}
	};
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		parentContext = (IEclipseContext) getSite().getService(IEclipseContext.class);
	}
	
	/** Creates the map
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		SashForm sash = new SashForm(parent,  SWT.VERTICAL);
		
		Composite mapPart = toolkit.createComposite(sash);
		mapPart.setLayout(new GridLayout());
		mapPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)mapPart.getLayout()).marginWidth = 0;
		((GridLayout)mapPart.getLayout()).marginHeight = 0;
		((GridLayout)mapPart.getLayout()).verticalSpacing = 0;
		
		Composite headerPart = toolkit.createComposite(mapPart, SWT.NONE);
		headerPart.setLayout(new GridLayout(3, false));
		headerPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)headerPart.getLayout()).marginHeight  = 0;
		((GridLayout)headerPart.getLayout()).marginTop = 5;
		((GridLayout)headerPart.getLayout()).marginBottom = 1;
				
		CCombo combo = new CCombo(headerPart, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
		combo.setBackground(combo.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		ComboViewer cmbSummarizeBy = new ComboViewer(combo);
		cmbSummarizeBy.getControl().setToolTipText(Messages.AssetOverviewMap_GroupingTooltip);
		cmbSummarizeBy.setContentProvider(ArrayContentProvider.getInstance());
		cmbSummarizeBy.setInput(IOverviewTableColumn.GroupByOption.values());
		cmbSummarizeBy.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((IOverviewTableColumn.GroupByOption)element).name();
			}
		});
		cmbSummarizeBy.setSelection(new StructuredSelection(currentGroupByOption));
		cmbSummarizeBy.addSelectionChangedListener(new ISelectionChangedListener() {		
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				currentGroupByOption = (GroupByOption) cmbSummarizeBy.getStructuredSelection().getFirstElement();
				refresh();
			}
		});
		
		dateFilters = new DateFilterDropDownComposite(headerPart, new DateFilter[] {
				DateFilterComposite.DateFilter.LAST_30_DAYS,
				DateFilterComposite.DateFilter.LAST_60_DAYS,
				DateFilterComposite.DateFilter.YEAR_TO_DATE,
				DateFilterComposite.DateFilter.LAST_YEAR,
				DateFilterComposite.DateFilter.LAST_5_YEARS,
				DateFilterComposite.DateFilter.CUSTOM,
				DateFilterComposite.DateFilter.ALL
		}, DateFilterComposite.DateFilter.ALL, true);
		dateFilters.addChangeListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				refresh();
			}
		});
		
		cmbStyles = new TableComboViewer(headerPart, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
		cmbStyles.getControl().setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		((GridData)(cmbStyles.getControl().getLayoutData())).widthHint = cmbSummarizeBy.getControl().getBounds().width;
		cmbStyles.getControl().setToolTipText(Messages.AssetOverviewMap_editStyleTooltip);
		cmbStyles.setContentProvider(ArrayContentProvider.getInstance());
		cmbStyles.setInput(IOverviewTableColumn.GroupByOption.values());
		cmbStyles.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof AssetMapStyle) return ((AssetMapStyle) element).getName();
				return super.getText(element);
			}
			public Image getImage(Object element) {
				if (element instanceof DefaultAssetMapStyle) return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.STYLE_ICON); //return AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_STYLE_DEFAULT);
				if (element instanceof AssetMapStyle) return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.STYLE_ICON);
				if (element == MANAGE_STYLES) return AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_SETTINGS);
				if (element == SAVE_STYLES) return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.SAVE_ICON);
				return null;
				
					
			}
		});
		cmbStyles.setInput(DialogConstants.LOADING_TEXT);
		cmbStyles.addSelectionChangedListener(new ISelectionChangedListener() {
			boolean doSelection = false;
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (!fireStyleChange) return;
				if (doSelection) return;
				doSelection = true;
				try {
					updateStyle();
				}finally {
					doSelection = false;
				}
			}
		});	
		
		Composite innerPart = toolkit.createComposite(mapPart, SWT.NONE);
		innerPart.setLayout(new GridLayout());
		innerPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		super.createPartControl(innerPart);
		
		
		Composite bottomPart = toolkit.createComposite(sash);
		bottomPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		bottomPart.setLayout(new GridLayout());
		((GridLayout)bottomPart.getLayout()).marginWidth = 0;
		((GridLayout)bottomPart.getLayout()).marginHeight = 0;
		
		Composite stackPart = toolkit.createComposite(bottomPart);
		stackPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		stackPart.setLayout(new StackLayout());
		
		tableComposite = toolkit.createComposite(stackPart);
		tableComposite.setLayout(new GridLayout());
		tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)tableComposite.getLayout()).verticalSpacing = 0;
		((GridLayout)tableComposite.getLayout()).marginWidth = 0;
		((GridLayout)tableComposite.getLayout()).marginHeight = 0;
		((GridLayout)tableComposite.getLayout()).marginTop = 1;
		
		tableComposite.addListener(SWT.Paint, e->{
			e.gc.setForeground(stackPart.getDisplay().getSystemColor(SWT.COLOR_GRAY));
			e.gc.drawLine(0, 0, tableComposite.getSize().x, 0);
		});
		
		statusTableComposite = toolkit.createComposite(stackPart);
		statusTableComposite.setLayout(new GridLayout());
		statusTableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)statusTableComposite.getLayout()).verticalSpacing = 0;
		((GridLayout)statusTableComposite.getLayout()).marginWidth = 0;
		((GridLayout)statusTableComposite.getLayout()).marginHeight = 0;
		((GridLayout)statusTableComposite.getLayout()).marginTop = 1;
		statusTableComposite.addListener(SWT.Paint, e->{
			e.gc.setForeground(stackPart.getDisplay().getSystemColor(SWT.COLOR_GRAY));
			e.gc.drawLine(0, 0, statusTableComposite.getSize().x, 0);
		});
		
		assetTableComposite = toolkit.createComposite(stackPart);
		assetTableComposite.setLayout(new GridLayout());
		assetTableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)assetTableComposite.getLayout()).verticalSpacing = 0;
		((GridLayout)assetTableComposite.getLayout()).marginWidth = 0;
		((GridLayout)assetTableComposite.getLayout()).marginHeight = 0;
		((GridLayout)assetTableComposite.getLayout()).marginTop = 1;
		assetTableComposite.addListener(SWT.Paint, e->{
			e.gc.setForeground(stackPart.getDisplay().getSystemColor(SWT.COLOR_GRAY));
			e.gc.drawLine(0, 0, assetTableComposite.getSize().x, 0);
		});
		
		((StackLayout)stackPart.getLayout()).topControl = tableComposite;
		
		createStatusTablePart();
		createAssetTablePart();
		
		Composite bottomLinks = toolkit.createComposite(bottomPart);
		bottomLinks.setLayout(new GridLayout(2, false));
		((GridLayout)bottomLinks.getLayout()).marginWidth = 0;
		((GridLayout)bottomLinks.getLayout()).marginHeight = 0;
		bottomLinks.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		SectionHeader header = new SectionHeader(bottomLinks, SWT.NONE,
				new String[] {Messages.AssetOverviewMap_SummaryTableSectionName, 
						Messages.AssetOverviewMap_StatTableSectionName,
						Messages.AssetOverviewMap_SensorTable},
				new Listener[] {
						e->{
							((StackLayout)stackPart.getLayout()).topControl = tableComposite;
							stackPart.layout();
						},
						e->{
							((StackLayout)stackPart.getLayout()).topControl = statusTableComposite;
							stackPart.layout();
						},
						e->{
							((StackLayout)stackPart.getLayout()).topControl = assetTableComposite;
							stackPart.layout();
						}
				});
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		header.selectPanel(0);
		if (AssetSecurityManager.INSTANCE.canConfigureAssetOverviewMap()) {
			Hyperlink hlConfigure = toolkit.createHyperlink(bottomLinks, Messages.AssetOverviewMap_configureLink, SWT.NONE);
			hlConfigure.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER,false, false));
			hlConfigure.addHyperlinkListener(new IHyperlinkListener() {			
				@Override
				public void linkExited(HyperlinkEvent e) {}
				
				@Override
				public void linkEntered(HyperlinkEvent e) {}
				
				@Override
				public void linkActivated(HyperlinkEvent e) {
					if (tableConfiguration == null) return;
					
					AssetMapColumnConfiguration clone = new AssetMapColumnConfiguration();
					try(Session s = HibernateManager.openSession()){
						clone.loadColumnConfiguration(s);
					}
					
					ColumnListDialog dialog = new ColumnListDialog(summaryTable.getControl().getShell(), clone);
					if (dialog.open() == Window.OK) {
						//something was changed; we need to reload and recompute column values
						loadTableJob.schedule();
					}
				}
			});
		}
		sash.setWeights(new int[] {8,2});
		
		
		LoadDefaultLayersJob loadBasemap = new LoadDefaultLayersJob(getMap(),true);
		loadBasemap.schedule();
		
		loadStylesJob.schedule();
		loadTableJob.schedule();
		configureStatusTableJob.schedule();
		refreshAssetTable.schedule();
		
		//EVENTS
		handlers = new ArrayList<>();
		EventHandler refreshHandler =  event->{
			refresh();
			if (event.getTopic().equals(SmartPlugIn.E4_DATABASE_CHANGED_EVENT)) loadStylesJob.schedule(500);
		};
		handlers.add(refreshHandler);
		parentContext.get(IEventBroker.class).subscribe(SmartPlugIn.E4_DATABASE_CHANGED_EVENT, refreshHandler);
		parentContext.get(IEventBroker.class).subscribe(AssetEvents.ASSETDEPLOYMENT_ALL, refreshHandler);
		parentContext.get(IEventBroker.class).subscribe(AssetEvents.ASSETDATA, refreshHandler);
		parentContext.get(IEventBroker.class).subscribe(AssetEvents.ASSETSTATION_ALL, refreshHandler);
		parentContext.get(IEventBroker.class).subscribe(AssetEvents.ASSETSTATIONLOCATION_ALL, refreshHandler);
	}

	@Override
	public void dispose() {
		
		if (service != null) {
			service.dispose(new NullProgressMonitor());
			try {
				CatalogPlugin.getDefault().getLocalCatalog().remove(service);
			}catch (Throwable t) {
				
			}
		}
		super.dispose();
		if (handlers != null) {
			for(EventHandler h : handlers) {
				parentContext.get(IEventBroker.class).unsubscribe(h);
			}
		}
		this.handlers = null;
	}
	
	private void createTablePart() {
		for (Control c : tableComposite.getChildren()) c.dispose();
		
		summaryTable = new TableViewer(tableComposite, SWT.FULL_SELECTION | SWT.MULTI );
		summaryTable.getTable().setHeaderVisible(true);
		summaryTable.getTable().setLinesVisible(true);
		summaryTable.setContentProvider(ArrayContentProvider.getInstance());
		summaryTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		ColumnViewerToolTipSupport.enableFor(summaryTable);
		
		for (OverviewTableColumnWrapper column : tableConfiguration.getColumns()) {
			TableViewerColumn tColumn = new TableViewerColumn(summaryTable, SWT.NONE);
			tColumn.getColumn().setText(column.getColumn().getName());
			tColumn.setLabelProvider(new ColumnLabelProvider() {
				public String getText(Object element) {
					if (statEngine.isComputed(column.getColumn())) {
						if (element instanceof StationData) {
							Object value = column.getColumn().getValue((StationData) element);
						
							return column.getColumn().getType().asString(value);
						}
						return Messages.AssetOverviewMap_ErrorValue;
					}else {
						return DialogConstants.LOADING_TEXT;
					}
				}
				
				public String getToolTipText(Object element) {
					if (statEngine.isComputed(column.getColumn())) {
						if (element instanceof StationData) {
							Object value = column.getColumn().getValue((StationData) element);
						
							return column.getColumn().getType().asTooltip(value);
						}
						return Messages.AssetOverviewMap_ErrorValue;
					}else {
						return DialogConstants.LOADING_TEXT;
					}
				}
			});			
			column.setUiColumn(tColumn.getColumn());
			column.setVisible(column.isVisible());
		}
	}
	
	private void manageStyles() {
		MapStyleListDialog dialog = new MapStyleListDialog(getSite().getShell());
		if (dialog.open() == Window.OK) {
			loadStylesJob.schedule();
		}
	}
	
	private void saveStyle() {
		if (mapLayers == null || mapLayers.isEmpty()) return;
		AssetMapStyle toUpdate = null;
		if (lastStyle != null && !(lastStyle instanceof DefaultAssetMapStyle)) {
			toUpdate = lastStyle;
		}
		StyleBlackboard sb = (StyleBlackboard)mapLayers.get(0).getStyleBlackboard();
		MapStyleDialog dialog = new MapStyleDialog(getSite().getShell(), toUpdate, sb);
		if (dialog.open() == Window.OK) {
			lastStyle = dialog.getModifiedStyle();
			cmbStyles.setInput(DialogConstants.LOADING_TEXT);
			loadStylesJob.schedule();
		}
	}
	
	private void updateStyle() {
		if (mapLayers == null || mapLayers.isEmpty()) return;
		
		Object x = cmbStyles.getStructuredSelection().getFirstElement();
		if (x == SAVE_STYLES) {
			saveStyle();
			cmbStyles.setSelection(new StructuredSelection(lastStyle));
			return;
		}else if (x == MANAGE_STYLES) {
			manageStyles();
			cmbStyles.setSelection(new StructuredSelection(lastStyle));
			return;
		}
		AssetMapStyle style = null;
		if (x instanceof AssetMapStyle) {
			style = (AssetMapStyle)x;
		}else {
			cmbStyles.setSelection(new StructuredSelection(lastStyle));
			return;
		}
		lastStyle = style;
		
		if (style instanceof DefaultAssetMapStyle) {
			final DefaultAssetMapStyle fstyle = (DefaultAssetMapStyle)style;
			mapLayers.forEach(layer->{
				layer.getStyleBlackboard().clear();
				layer.getStyleBlackboard().put(SLDContent.ID, fstyle.getStyle() );
				layer.refresh(null);
			});
			return;
		}else {
			//parse style string and update layer
			try {
				StyleBlackboard sBoard = StyleManager.INSTANCE.fromString(style.getStyleString());
				mapLayers.forEach(layer->{
					layer.getStyleBlackboard().clear();
					layer.getStyleBlackboard().addAll(sBoard);
					layer.refresh(null);
				});
			}catch (Exception ex) {
				AssetPlugIn.displayLog(Messages.AssetOverviewMap_StyleParseError + ex.getMessage(), ex);
			}
		}
	}
	
	private void createStatusTablePart() {
		canvas = new StatusCanvas(statusTableComposite);
		canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));		
	}
	
	private void createAssetTablePart() {
		assetTable = new AssetSummaryTable(assetTableComposite);
		assetTable.getViewer().getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	@Override
	public EditorPart getParentEditor() {
		return this;
	}
	
	private void refresh() {
		computeStatisticsJob.cancel();
		configureStatusTableJob.cancel();
		computeStatisticsJob.schedule(500);
		configureStatusTableJob.schedule(500);		
		refreshAssetTable.cancel();
		refreshAssetTable.schedule(500);
	}

	private List<DefaultAssetMapStyle> getDefaultMapStyles(){
		DefaultAssetMapStyle statusStyle = new DefaultAssetMapStyle();
		statusStyle.setName(Messages.AssetOverviewMap_StatusStyleName + "*"); //$NON-NLS-1$
		statusStyle.setConservationArea(SmartDB.getCurrentConservationArea());
		statusStyle.setStyleString(null);
		statusStyle.setStyle(AssetStationSummaryGeoResource.getDefaultLayerStyle());
		
		return Collections.singletonList(statusStyle);
	}
	
	private Job loadStylesJob = new Job(Messages.AssetOverviewMap_loadingstyleJobName) {
		
		private boolean first = true;
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Object> styles = new ArrayList<>();
			styles.addAll(getDefaultMapStyles());
			if (lastStyle == null) lastStyle = (AssetMapStyle) styles.get(0);
			try(Session session = HibernateManager.openSession()){
				styles.addAll(QueryFactory.buildQuery(session, AssetMapStyle.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
			}
			if (AssetSecurityManager.INSTANCE.canConfigureAssetOverviewMap()) {
				styles.add(Messages.AssetOverviewMap_ActionsSection);
				styles.add(SAVE_STYLES);
				styles.add(MANAGE_STYLES);
			}
			Display.getDefault().syncExec(()->{
				cmbStyles.setInput(styles);
				try {
					fireStyleChange = first == true;			
					if (lastStyle != null && styles.contains(lastStyle)) {
						cmbStyles.setSelection(new StructuredSelection(lastStyle));
					}else {
						cmbStyles.setSelection(new StructuredSelection(styles.get(0)));
					}
				}finally {
					first = false;
					fireStyleChange = true;
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	
	Job loadTableJob = new Job(Messages.AssetOverviewMap_configureTableJobName) {
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			tableConfiguration = new AssetMapColumnConfiguration();
			try(Session session = HibernateManager.openSession()){
				tableConfiguration.loadColumnConfiguration(session);
			}
			
			Display.getDefault().syncExec(()->{
				createTablePart();
				tableComposite.layout(true);
			});
			
			computeStatisticsJob.schedule();
			return Status.OK_STATUS;
		}
		
	};
	
	
	Job computeStatisticsJob = new Job(Messages.AssetOverviewMap_computeStatusJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {

			final Date[] start = new Date[] {null};
			final Date[] end = new Date[] {null};
			
			Display.getDefault().syncExec(()->{
				if (summaryTable.getTable().isDisposed()) return;
				if (dateFilters.getDateFilter() == DateFilter.CUSTOM) {
					start[0] = dateFilters.getCustomStartDate();
					end[0] = dateFilters.getCustomEndDate();
				}else {
					start[0] = dateFilters.getDateFilter().getStartDate();
					end[0] = dateFilters.getDateFilter().getEndDate();	
				}
				summaryTable.setInput(statEngine.getData());
			});
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;
			
			if (tableConfiguration.getColumns().isEmpty()) return Status.OK_STATUS;
			
			if (service == null) {
				service = new AssetStationSummaryService(tableConfiguration.getColumns().stream().map(e->e.getColumn()).collect(Collectors.toList()));
				List<IGeoResource> resources = new ArrayList<>();
				try {
					resources.addAll(service.resources(monitor));
					mapLayers = new ArrayList<>();
					AddLayersCommand addCmd = new AddLayersCommand(resources) {
						 public void run( IProgressMonitor monitor ) throws Exception {
							 super.run(monitor);
							 mapLayers.addAll(getLayers());
						 }
					};
					getMap().sendCommandASync(addCmd);
					service.setData(statEngine.getData());
				}catch (Exception ex) {
					AssetPlugIn.displayLog(Messages.AssetOverviewMap_SummaryMapLayerError + ex.getMessage(), ex);
				}
			}
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;
			Date[] dFilters = null;
			if (start[0] != null) {
				if (end[0] == null) {
					end[0] = new Date();
				}
				
				dFilters = new Date[] {
						SharedUtils.getDatePart(start[0], false),
						SharedUtils.getDatePart(end[0], true),
				};
			}
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;
			statEngine.computeStatistics(tableConfiguration.getColumns().stream().map(e->e.getColumn()).collect(Collectors.toList()), dFilters, currentGroupByOption, monitor);

			return Status.OK_STATUS;
		}
		
	};
	
	
	Job configureStatusTableJob = new Job(Messages.AssetOverviewMap_configureStatsTableJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {

			final Date[] start = new Date[] {null};
			final Date[] end = new Date[] {null};
			
			Display.getDefault().syncExec(()->{
				if (canvas.isDisposed()) return;
				if (dateFilters.getDateFilter() == DateFilter.CUSTOM) {
					start[0] = dateFilters.getCustomStartDate();
					end[0] = dateFilters.getCustomEndDate();
				}else {
					start[0] = dateFilters.getDateFilter().getStartDate();
					end[0] = dateFilters.getDateFilter().getEndDate();	
				}
			});
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;	
			Date[] dFilters = null;
			if (start[0] != null) {
				if (end[0] == null) {
					end[0] = new Date();
				}
				
				dFilters = new Date[] {
						SharedUtils.getDatePart(start[0], false),
						SharedUtils.getDatePart(end[0], true),
				};
			}
		
			HashMap<Object, Set<Long>> data = new HashMap<>();
			try(Session session = HibernateManager.openSession()){
				data = (new StatusEngine()).computeStatus(session, dFilters, SmartDB.getCurrentConservationArea(), currentGroupByOption);
			}
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;
			
			Long sstart = LocalDate.now().toEpochDay();
			Long send  = LocalDate.now().toEpochDay();
			
			if (dFilters == null) {
				for (Set<Long> d : data.values()) {
					for (Long l : d) {
						if ( l < sstart) sstart = l;
					}
				}
			}else {
				sstart = new java.sql.Date(dFilters[0].getTime()).toLocalDate().toEpochDay();
				send = new java.sql.Date(dFilters[1].getTime()).toLocalDate().toEpochDay();
			}
			final LocalDate fsstart = LocalDate.ofEpochDay(sstart);
			final LocalDate fsend = LocalDate.ofEpochDay(send);
			final HashMap<Object, Set<Long>> fdata = data;
			Display.getDefault().syncExec(()->{
				canvas.setData(fdata.entrySet(), fsstart, fsend);
				canvas.redraw();
			});
			

			return Status.OK_STATUS;
		}
		
	};
	
	
	private Job refreshAssetTable = new Job(Messages.AssetOverviewMap_RefreshJob) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try(Session session = HibernateManager.openSession()){
				assetTable.configureTable(session);
			}
			return Status.OK_STATUS;
		}
		
	};
	
	class DefaultAssetMapStyle extends AssetMapStyle {
		private static final long serialVersionUID = 1L;
		
		private Style style;
		
		public DefaultAssetMapStyle() {
			super();
		}
		
		public void setStyle(Style style) {
			this.style = style;
		}
		public Style getStyle() {
			return this.style;
		}
	}
}
