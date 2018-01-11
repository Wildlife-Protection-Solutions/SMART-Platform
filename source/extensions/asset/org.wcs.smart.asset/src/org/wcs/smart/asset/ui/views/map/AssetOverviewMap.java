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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPersistableElement;
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
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn.GroupByOption;
import org.wcs.smart.asset.ui.views.map.udig.AssetStationSummaryService;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.ui.properties.DialogConstants;

public class AssetOverviewMap extends SmartMapEditorPart implements IEditorPart{

	public static final String ID = "org.wcs.smart.asset.overviewmap"; //$NON-NLS-1$

	public static IEditorInput OVERVIEW_MAP_INPUT = new IEditorInput() {
		
		@Override
		public <T> T getAdapter(Class<T> adapter) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public String getToolTipText() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public IPersistableElement getPersistable() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public String getName() {
			return "Asset Overview Map";
		}
		
		@Override
		public ImageDescriptor getImageDescriptor() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public boolean exists() {
			// TODO Auto-generated method stub
			return false;
		}
	};
	
	private DateFilterDropDownComposite dateFilters;
	private TableViewer summaryTable;
	private AssetMapColumnConfiguration tableConfiguration;
	private Composite tableComposite;
	private AssetStationSummaryService service;
	
	private GroupByOption currentGroupByOption = GroupByOption.STATION;
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
				computeStatisticsJob.schedule(500);
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
		
		Composite innerPart = toolkit.createComposite(mapPart, SWT.NONE);
		innerPart.setLayout(new GridLayout());
		innerPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		super.createPartControl(innerPart);
		
		Composite tablePart = toolkit.createComposite(sash);
		tablePart.setLayout(new GridLayout());
		tablePart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)tablePart.getLayout()).verticalSpacing = 0;
		
		tableComposite = toolkit.createComposite(tablePart);
		tableComposite.setLayout(new GridLayout());
		tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)tablePart.getLayout()).verticalSpacing = 0;
		((GridLayout)tablePart.getLayout()).marginWidth = 0;
		((GridLayout)tablePart.getLayout()).marginHeight = 0;
		
		
		Hyperlink hlConfigure = toolkit.createHyperlink(tablePart, "configure...", SWT.NONE);
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
				
				ColumnListDialog dialog = new ColumnListDialog(tablePart.getShell(), clone);
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
		summaryTable.setInput(DialogConstants.LOADING_TEXT);
		summaryTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		for (OverviewTableColumnWrapper column : tableConfiguration.getColumns()) {
			TableViewerColumn tColumn = new TableViewerColumn(summaryTable, SWT.NONE);
			tColumn.getColumn().setText(column.getColumn().getName());
			tColumn.setLabelProvider(new ColumnLabelProvider() {
				public String getText(Object element) {
					if (element instanceof StationData) return column.getColumn().getType().asString(column.getColumn().getValue((StationData) element));
					return super.getText(element);
				}
			});			
			column.setUiColumn(tColumn.getColumn());
			column.setVisible(column.isVisible());
		}
	}
	
	
	
	@Override
	public EditorPart getParentEditor() {
		return this;
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
				summaryTable.setInput(new String[] {DialogConstants.LOADING_TEXT});
				
				start[0] = dateFilters.getDateFilter().getStartDate();
				end[0] = dateFilters.getDateFilter().getEndDate();
			});
			
			
			if (tableConfiguration.getColumns().isEmpty()) return Status.OK_STATUS;
			
			if (service == null) {
				//TODO: concurrent & cleanup service
				service = new AssetStationSummaryService(tableConfiguration.getColumns().stream().map(e->e.getColumn()).collect(Collectors.toList()));
				List<IGeoResource> resources = new ArrayList<>();
				try {
					resources.addAll(service.resources(monitor));	
				}catch (Exception ex) {
					AssetPlugIn.log(ex.getMessage(), ex);
					//TODO: display error to user
				}
				AddLayersCommand addCmd = new AddLayersCommand(resources);
				getMap().sendCommandASync(addCmd);
			}
			
			Date[] dFilters = null;
			if (start[0] != null) {
				if (end[0] == null) {
					//TODO: make this future too?
					end[0] = new Date();
				}
				dFilters = new Date[] {start[0], end[0]};
			}
					
			
			final List<StationData> data = StatisticComputer.computeStatistics(tableConfiguration.getColumns().stream().map(e->e.getColumn()).collect(Collectors.toList()), dFilters, currentGroupByOption);
			
			Display.getDefault().syncExec(()->{
				if (summaryTable.getTable().isDisposed()) return;
				
				summaryTable.setInput(data);
				service.setData(data);
				tableConfiguration.getColumns().stream().filter(e->e.isVisible()).forEach(e->e.getTableColumn().pack());
				getMap().getRenderManager().refresh(null);
			});
			return Status.OK_STATUS;
		}
		
	};
}
