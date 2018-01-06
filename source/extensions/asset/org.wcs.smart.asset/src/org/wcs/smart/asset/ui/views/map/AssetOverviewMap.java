package org.wcs.smart.asset.ui.views.map;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn.GroupByOption;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
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
	private TableViewerColumn idColumn;
	
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
		
		Composite headerPart = toolkit.createComposite(mapPart);
		headerPart.setLayout(new GridLayout(5, false));
		
		toolkit.createLabel(headerPart, "Date Range:");
		dateFilters = new DateFilterDropDownComposite(headerPart, new DateFilter[] {
				DateFilterComposite.DateFilter.LAST_30_DAYS,
				DateFilterComposite.DateFilter.LAST_60_DAYS,
				DateFilterComposite.DateFilter.YEAR_TO_DATE,
				DateFilterComposite.DateFilter.LAST_YEAR,
				DateFilterComposite.DateFilter.LAST_5_YEARS,
				DateFilterComposite.DateFilter.ALL
		}, DateFilterComposite.DateFilter.ALL);
		
		Label dateRange = toolkit.createLabel(headerPart, "");
		
		dateFilters.addChangeListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				dateRange.setText(dateFilters.getDateFilter().getLabel());
				headerPart.layout(true);
				computeStatisticsJob.schedule(500);
			}
		});
		
		toolkit.createLabel(headerPart, "Summarize By:");
		
		ComboViewer cmbSummarizeBy = new ComboViewer(headerPart, SWT.DROP_DOWN | SWT.READ_ONLY);
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
		
		Composite innerPart = toolkit.createComposite(mapPart, SWT.NONE);
		innerPart.setLayout(new GridLayout());
		innerPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		super.createPartControl(innerPart);
		
		Composite tablePart = toolkit.createComposite(sash);
		tablePart.setLayout(new GridLayout());
		tablePart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createTablePart(tablePart, toolkit);
		
		sash.setWeights(new int[] {90,10});
	}

	
	private void createTablePart(Composite parent, FormToolkit toolkit) {
	
		summaryTable = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		summaryTable.getTable().setHeaderVisible(true);
		summaryTable.getTable().setLinesVisible(true);
		summaryTable.setContentProvider(ArrayContentProvider.getInstance());
		summaryTable.setInput(DialogConstants.LOADING_TEXT);
		summaryTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		IOverviewTableColumn[] columns = FixedColumn.getFixedColumns();
		summaryTable.getTable().setData("COLUMN", columns);
		
		idColumn = new TableViewerColumn(summaryTable, SWT.NONE);
		idColumn.getColumn().setWidth(140);
		idColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof StationData) return ((StationData) element).getIdField();
				return super.getText(element);
			}
		});
		
		
		
		for (IOverviewTableColumn column : columns) {
			TableViewerColumn tColumn = new TableViewerColumn(summaryTable, SWT.NONE);
			tColumn.getColumn().setWidth(140);
			tColumn.getColumn().setText(column.getName());
			tColumn.setLabelProvider(new ColumnLabelProvider() {
				public String getText(Object element) {
					if (element instanceof StationData) return column.getValue((StationData) element);
					return super.getText(element);
				}
			});
		}
		
		computeStatisticsJob.schedule();
		
	}
	
	
	
	@Override
	public EditorPart getParentEditor() {
		return this;
	}

	Job computeStatisticsJob = new Job("compute statistics") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<IOverviewTableColumn> items = new ArrayList<>();
			final Date[] start = new Date[] {null};
			final Date[] end = new Date[] {null};
			Display.getDefault().syncExec(()->{
				if (summaryTable.getTable().isDisposed()) return;
				summaryTable.setInput(new String[] {DialogConstants.LOADING_TEXT});
				
				switch(currentGroupByOption) {
				case LOCATION:
					idColumn.getColumn().setText("Location ID");
					break;
				case STATION:
					idColumn.getColumn().setText("Station ID");
					break;
				};
				
				IOverviewTableColumn[] columns = (IOverviewTableColumn[]) summaryTable.getTable().getData("COLUMN");
				for (IOverviewTableColumn c : columns)items.add(c);
				
				start[0] = dateFilters.getDateFilter().getStartDate();
				end[0] = dateFilters.getDateFilter().getEndDate();
			});
			
			
			if (items.isEmpty()) return Status.OK_STATUS;
			
			
			
			Date[] dFilters = null;
			if (start[0] != null) {
				if (end[0] == null) {
					//TODO: make this future too?
					end[0] = new Date();
				}
				dFilters = new Date[] {start[0], end[0]};
			}
					
			
			final List<StationData> data = StatisticComputer.computeStatistics(items, dFilters, currentGroupByOption);
			
			Display.getDefault().syncExec(()->{
				if (summaryTable.getTable().isDisposed()) return;
				
				summaryTable.setInput(data);
			});
			return Status.OK_STATUS;
		}
		
	};
}
