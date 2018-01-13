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
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.map.engine.OverviewmapColumnEngine;
import org.wcs.smart.asset.map.engine.StatusEngine;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn.GroupByOption;
import org.wcs.smart.asset.ui.views.map.udig.AssetStationSummaryService;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
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

	public static IEditorInput OVERVIEW_MAP_INPUT = new IEditorInput() {		
		@Override
		public <T> T getAdapter(Class<T> adapter) { return null; }
		@Override
		public String getToolTipText() { return null; }
		@Override
		public IPersistableElement getPersistable() { return null; }
		@Override
		public String getName() { return "Asset Overview Map"; }
		@Override
		public ImageDescriptor getImageDescriptor() { return null; }
		@Override
		public boolean exists() { return false; }
	};
	
	private DateFilterDropDownComposite dateFilters;
	private TableViewer summaryTable;
	private TableViewer statusTable;
	private AssetMapColumnConfiguration tableConfiguration;
	private Composite tableComposite;
	private Composite statusTableComposite;
	private AssetStationSummaryService service;
	private StatusCanvas canvas;
	
	private GroupByOption currentGroupByOption = GroupByOption.STATION;
	
	private OverviewmapColumnEngine statEngine = new OverviewmapColumnEngine() {
		
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
	
	/** Creates the map
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		SashForm sash = new SashForm(parent,  SWT.VERTICAL);
		
		Composite mapPart = toolkit.createComposite(sash, SWT.NONE);
		mapPart.setLayout(new GridLayout());
		mapPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)mapPart.getLayout()).marginWidth = 0;
		((GridLayout)mapPart.getLayout()).marginHeight = 0;
		((GridLayout)mapPart.getLayout()).verticalSpacing = 0;
		
		Composite headerPart = toolkit.createComposite(mapPart, SWT.NONE);
		headerPart.setLayout(new GridLayout(2, false));
		headerPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)headerPart.getLayout()).marginHeight  = 0;
		((GridLayout)headerPart.getLayout()).marginTop = 5;
		((GridLayout)headerPart.getLayout()).marginBottom = 1;
				
		CCombo combo = new CCombo(headerPart, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
		combo.setBackground(combo.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		ComboViewer cmbSummarizeBy = new ComboViewer(combo);
		cmbSummarizeBy.getControl().setToolTipText("Option for grouping results");
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
		
		statusTableComposite = toolkit.createComposite(stackPart);
		statusTableComposite.setLayout(new GridLayout());
		statusTableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)statusTableComposite.getLayout()).verticalSpacing = 0;
		((GridLayout)statusTableComposite.getLayout()).marginWidth = 0;
		((GridLayout)statusTableComposite.getLayout()).marginHeight = 0;
		
		((StackLayout)stackPart.getLayout()).topControl = tableComposite;
		createStatusTablePart();
		
		Composite bottomLinks = toolkit.createComposite(bottomPart);
		bottomLinks.setLayout(new GridLayout(3, false));
		bottomLinks.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		((GridLayout)bottomLinks.getLayout()).marginWidth = 0;
		((GridLayout)bottomLinks.getLayout()).marginHeight = 0;
		
		Hyperlink lnkSummary = toolkit.createHyperlink(bottomLinks, "Summary Table", SWT.NONE);
		lnkSummary.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		lnkSummary.addHyperlinkListener(new HyperlinkAdapter() {			
			public void linkActivated(HyperlinkEvent e) {
				((StackLayout)stackPart.getLayout()).topControl = tableComposite;
				stackPart.layout();
			}
		});
		
		Hyperlink lnkStatus = toolkit.createHyperlink(bottomLinks, "Status Table", SWT.NONE);
		lnkStatus.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,true, false));
		lnkStatus.addHyperlinkListener(new HyperlinkAdapter() {			
			public void linkActivated(HyperlinkEvent e) {
				((StackLayout)stackPart.getLayout()).topControl = statusTableComposite;
				stackPart.layout();
			}
		});
		
		
		Hyperlink hlConfigure = toolkit.createHyperlink(bottomLinks, "configure...", SWT.NONE);
		hlConfigure.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER,true, false));
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
				
		sash.setWeights(new int[] {8,2});
		
		
		LoadDefaultLayersJob loadBasemap = new LoadDefaultLayersJob(getMap(),true);
		loadBasemap.schedule();
		
		loadTableJob.schedule();
		configureStatusTableJob.schedule();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (service != null) {
			service.dispose(new NullProgressMonitor());
			CatalogPlugin.getDefault().getLocalCatalog().remove(service);
		}
	}
	
	private void createTablePart() {
		for (Control c : tableComposite.getChildren()) c.dispose();
		
		summaryTable = new TableViewer(tableComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		summaryTable.getTable().setHeaderVisible(true);
		summaryTable.getTable().setLinesVisible(true);
		summaryTable.setContentProvider(ArrayContentProvider.getInstance());
		summaryTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
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
						return "Error";
					}else {
						return DialogConstants.LOADING_TEXT;
					}
				}
			});			
			column.setUiColumn(tColumn.getColumn());
			column.setVisible(column.isVisible());
		}
	}
	
	private void createStatusTablePart() {
		canvas = new StatusCanvas(statusTableComposite);
		canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));		
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
	}

	Job loadTableJob = new Job("configure table and compute statistics") {
		
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
	
	
	Job computeStatisticsJob = new Job("compute statistics") {

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
					AddLayersCommand addCmd = new AddLayersCommand(resources);
					getMap().sendCommandASync(addCmd);
					service.setData(statEngine.getData());
				}catch (Exception ex) {
					AssetPlugIn.displayLog("Unable to add summary layer to map." + ex.getMessage(), ex);
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
	
	
	Job configureStatusTableJob = new Job("configure status table") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {

			final Date[] start = new Date[] {null};
			final Date[] end = new Date[] {null};
			
			Display.getDefault().syncExec(()->{
//				if (statusTable.getTable().isDisposed()) return;
				if (canvas.isDisposed()) return;
				if (dateFilters.getDateFilter() == DateFilter.CUSTOM) {
					start[0] = dateFilters.getCustomStartDate();
					end[0] = dateFilters.getCustomEndDate();
				}else {
					start[0] = dateFilters.getDateFilter().getStartDate();
					end[0] = dateFilters.getDateFilter().getEndDate();	
				}
				
				
//				.setInput(DialogConstants.LOADING_TEXT);
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
				data = (new StatusEngine()).computeStatus(session, dFilters, currentGroupByOption);
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
			
//			final Long fsstart = sstart;
//			final Long fsend = send;
			
			final LocalDate fsstart = LocalDate.ofEpochDay(sstart);
			final LocalDate fsend = LocalDate.ofEpochDay(send);
			final HashMap<Object, Set<Long>> fdata = data;
			Display.getDefault().syncExec(()->{
				canvas.setData(fdata.entrySet(), fsstart, fsend);
				canvas.redraw();
//				if (statusTable.getTable().isDisposed()) return;
//				
//				for (TableColumn column : statusTable.getTable().getColumns()) column.dispose();
//				
//				TableViewerColumn idColumn = new TableViewerColumn(statusTable, SWT.NONE);
//				idColumn.getColumn().setText("Id");
//				idColumn.getColumn().setWidth(100);
//				idColumn.setLabelProvider(new ColumnLabelProvider() {
//					@Override
//					public String getText(Object element) {
//						if (element instanceof Entry) {
//							Object x = ((Entry)element).getKey();
//							if (x instanceof AssetStation) return ((AssetStation)x).getId();
//							if (x instanceof AssetStationLocation) return ((AssetStationLocation)x).getId();
//						}
//						return "";
//					}
//				});
//				
//				
//				DateTimeFormatter form = DateTimeFormatter.ofPattern("dd");
//				
//				LocalDate startDate = LocalDate.ofEpochDay(fsstart);
//				LocalDate endDate = LocalDate.ofEpochDay(fsend);
//				
//				while(!startDate.isAfter(endDate)) {
//					
//					TableViewerColumn dateColumn = new TableViewerColumn(statusTable, SWT.NONE);
//					dateColumn.getColumn().setText(form.format(startDate));
//					dateColumn.getColumn().setToolTipText(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(startDate));
//					dateColumn.getColumn().setWidth(32);
//					
//					long ftime = startDate.toEpochDay();
//					dateColumn.setLabelProvider(new ColumnLabelProvider() {
//						@Override
//						public String getText(Object element) {
//							return "";
//						}
//						@Override
//						public Color getBackground(Object element) {
//							if (element instanceof Entry) {
//								if (((Set<Long>)((Entry)element).getValue()).contains(ftime)) {
//									return Display.getDefault().getSystemColor(SWT.COLOR_GREEN);
//								}else {
//									return Display.getDefault().getSystemColor(SWT.COLOR_RED);
//								}
//							}
//								
//							return null;
//						}
//					});
//					startDate = startDate.plus(1, ChronoUnit.DAYS);
//					if (monitor.isCanceled()) return ;
//					
//				}
//				statusTable.setInput(fdata.entrySet());
			});
			

			return Status.OK_STATUS;
		}
		
	};
}
