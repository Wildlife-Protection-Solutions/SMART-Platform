/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.views.asset;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
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
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.AssetUtils;
import org.wcs.smart.asset.engine.StatisticsEngine;
import org.wcs.smart.asset.engine.StatisticsEngine.Statistic;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetDeploymentAttributeValue;
import org.wcs.smart.asset.model.AssetTypeDeploymentAttribute;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.model.AssetWaypointSource;
import org.wcs.smart.asset.ui.handler.OpenStationHandler;
import org.wcs.smart.asset.ui.handler.OpenStationLocationHandler;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Asset deployment page for asset editor
 * @author Emily
 *
 */
public class AssetDeploymentPage {

	@Inject
	private IEclipseContext parentContext;
	
	private TableViewer tblDeployments;
	
	private Map<IAssetSummary, Label> assetSummaryValues;

	private List<AssetDeploymentWrapper> allDeployments;
	
	private AssetEditor parentEditor;

	private ScrolledComposite scrollDetails;
	private Composite detailsPane;
	private List<AssetTypeDeploymentAttribute> allDeploymentAttributes;
	
	public AssetDeploymentPage(AssetEditor parent) {
		this.parentEditor = parent;
	}
	
	public Composite createDeploymentsSection(Composite parent, FormToolkit toolkit) {
		
		Composite panel = toolkit.createComposite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		panel.addListener(SWT.Dispose, e->computeDeploymentStats.cancel());
		
		Composite summaryPanel = toolkit.createComposite(panel, SWT.BORDER);
		summaryPanel.setLayout(new GridLayout(2, false));
		summaryPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = toolkit.createLabel(summaryPanel, "Summary");
		FontData fd = l.getFont().getFontData()[0];
		fd.setHeight(fd.getHeight() + 1);
		fd.setStyle(SWT.BOLD);
		Font boldFont = new Font(l.getDisplay(), fd);
		l.setFont(boldFont);
		l.addListener(SWT.Dispose, e->boldFont.dispose());
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		IAssetSummary[] summaryValues = new IAssetSummary[] {TimeInFieldAssetSummary.INSTANCE, IncidentAssetSummary.INSTANCE};
		assetSummaryValues = new HashMap<>();
		for (IAssetSummary s : summaryValues) {
			toolkit.createLabel(summaryPanel, s.getSummaryName());
			
			Label sv = toolkit.createLabel(summaryPanel, "");
			sv.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			sv.setData(s);
			
			assetSummaryValues.put(s,  sv);
		}
		
		Composite historyPanel = toolkit.createComposite(panel, SWT.BORDER);
		historyPanel.setLayout(new GridLayout());
		historyPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)historyPanel.getLayout()).marginWidth = 0;
		((GridLayout)historyPanel.getLayout()).marginHeight = 0;
		
		Composite headerPanel = toolkit.createComposite(historyPanel, SWT.NONE);
		headerPanel.setLayout(new GridLayout(2, false));
		headerPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)headerPanel.getLayout()).marginHeight = 0;
		((GridLayout)headerPanel.getLayout()).marginTop = 5;

		
		l = toolkit.createLabel(headerPanel, "Asset Deployments");
		fd = l.getFont().getFontData()[0];
		fd.setHeight(fd.getHeight() + 1);
		fd.setStyle(SWT.BOLD);
		Font boldFont1 = new Font(l.getDisplay(), fd);
		l.setFont(boldFont1);
		l.addListener(SWT.Dispose, e->boldFont1.dispose());
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		ToolItem itemDelete = null;
		ToolItem itemEdit = null;
		if (!parentEditor.getAsset().getIsRetired()) {
			ToolBar toolbar = new ToolBar(headerPanel, SWT.FLAT);
			toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			
			itemDelete = new ToolItem(toolbar, SWT.PUSH);
			itemDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			itemDelete.setToolTipText("delete selected deployments");
			itemDelete.addListener(SWT.Selection, e->deleteSelectedDeployments());
			itemDelete.setEnabled(false);
			
			itemEdit = new ToolItem(toolbar, SWT.PUSH);
			itemEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
			itemEdit.setToolTipText("edit selected deployments");
			itemEdit.addListener(SWT.Selection, e->editSelectedDeployments());
			itemEdit.setEnabled(false);
			
			ToolItem itemAdd = new ToolItem(toolbar, SWT.PUSH);
			itemAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			itemAdd.setToolTipText("create a new asset deployment");
			itemAdd.addListener(SWT.Selection, e->addDeployment());
			
		}
		
		SashForm bodyPanel = new SashForm(historyPanel, SWT.HORIZONTAL);
//		bodyPanel.setLayout(new GridLayout(2, false));
		bodyPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
//		((GridLayout)bodyPanel.getLayout()).marginHeight = 0;
//		((GridLayout)bodyPanel.getLayout()).marginBottom = 5;
		
		tblDeployments = new TableViewer(bodyPanel, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		tblDeployments.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblDeployments.setContentProvider(ArrayContentProvider.getInstance());
		tblDeployments.getTable().setHeaderVisible(true);
		tblDeployments.getTable().setLinesVisible(true);
		
		tblDeployments.getTable().addListener(SWT.MouseDoubleClick, new Listener(){
			@Override
			public void handleEvent(Event event) {
				ViewerCell cell = tblDeployments.getCell(new Point(event.x, event.y));
				if (cell == null) return;
				int colIndex = cell.getColumnIndex();
				if (colIndex == AssetDeploymentTableColumn.FixedColumn.STATION.ordinal()){
					AssetDeployment d = ((AssetDeploymentWrapper) cell.getElement()).getDeployment();
					(new OpenStationHandler()).openStation(d.getStationLocation().getStation());
				}else if (colIndex == AssetDeploymentTableColumn.FixedColumn.LOCATION.ordinal()){
					AssetDeployment d = ((AssetDeploymentWrapper) cell.getElement()).getDeployment();
					(new OpenStationLocationHandler()).openStationLocation(d.getStationLocation());
				}else {
					if (!parentEditor.getAsset().getIsRetired()) editSelectedDeployments();
				}
			}
					
		});
		
		if (!parentEditor.getAsset().getIsRetired()) {
			final ToolItem fitemDelete = itemDelete;
			final ToolItem fitemEdit = itemEdit;
			tblDeployments.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					fitemDelete.setEnabled(!tblDeployments.getSelection().isEmpty());
					fitemEdit.setEnabled(!tblDeployments.getSelection().isEmpty());
				}
			});
			
			Menu mnuDeployments = new Menu(tblDeployments.getControl());
			
			MenuItem mnuAdd = new MenuItem(mnuDeployments, SWT.PUSH);
			mnuAdd.setText("New");
			mnuAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			mnuAdd.addListener(SWT.Selection, e-> addDeployment());
					
			MenuItem mnuEdit = new MenuItem(mnuDeployments, SWT.PUSH);
			mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
			mnuEdit.addListener(SWT.Selection, e-> editSelectedDeployments());
			mnuEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
			
			MenuItem mnuDelete = new MenuItem(mnuDeployments, SWT.PUSH);
			mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			mnuDelete.addListener(SWT.Selection, e->deleteSelectedDeployments());
			mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
			
			tblDeployments.getControl().setMenu(mnuDeployments);
			
			mnuDeployments.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent e) {
					mnuEdit.setEnabled(!tblDeployments.getSelection().isEmpty());
					mnuDelete.setEnabled(!tblDeployments.getSelection().isEmpty());
				}
				
				@Override
				public void menuHidden(MenuEvent e) { }
			});
		}
		
		Composite detailsPaneOuter = new Composite(bodyPanel, SWT.BORDER);
		detailsPaneOuter.setLayout(new GridLayout());
		detailsPaneOuter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		//((GridData)detailsPaneOuter.getLayoutData()).widthHint = 200;
		bodyPanel.setWeights(new int[] {70,30});
		
		scrollDetails = new ScrolledComposite(detailsPaneOuter, SWT.V_SCROLL | SWT.H_SCROLL);
		toolkit.adapt(scrollDetails);
		
		scrollDetails.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrollDetails.setExpandHorizontal(true);
		scrollDetails.setExpandVertical(true);
		detailsPane = toolkit.createComposite(scrollDetails, SWT.NONE);
		scrollDetails.setContent(detailsPane);
		
		detailsPane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblDeployments.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)tblDeployments.getSelection()).getFirstElement();
				if (x instanceof AssetDeploymentWrapper) {
					updateDetailsPane(((AssetDeploymentWrapper)x).getDeployment(), toolkit);
				}
			}
		});
		initializePanel(parentEditor.getAsset());
		refreshSummaryStatistics();
		
		return panel;
	}
	
	private void updateDetailsPane(AssetDeployment deployment, FormToolkit toolkit) {
		if (detailsPane == null) return;
		
		for (Control c : detailsPane.getChildren()) c.dispose();
		
		detailsPane.setLayout(new GridLayout(2, false));
		
		Label l = toolkit.createLabel(detailsPane, MessageFormat.format("{0} ({1})", deployment.getStationLocation().getId(), deployment.getStationLocation().getStation().getId()));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Composite dateDetails = toolkit.createComposite(detailsPane);
		dateDetails.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		dateDetails.setLayout(new GridLayout(3, false));
		
		l = toolkit.createLabel(dateDetails, DateFormat.getDateInstance().format(deployment.getStartDate()) + "\n" + DateFormat.getTimeInstance().format(deployment.getStartDate()));
		l = toolkit.createLabel(dateDetails, "   -   ");
		if (deployment.getEndDate() == null) {
			l = toolkit.createLabel(dateDetails, "Current");
		}else {
			l = toolkit.createLabel(dateDetails, DateFormat.getDateInstance().format(deployment.getEndDate()) + "\n" + DateFormat.getTimeInstance().format(deployment.getEndDate()));
		}

		l = toolkit.createLabel(detailsPane, "", SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		if (allDeploymentAttributes != null) {
			for (AssetTypeDeploymentAttribute a : allDeploymentAttributes) {
				l = toolkit.createLabel(detailsPane, a.getAttribute().getName() + ":");
				l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
				AssetDeploymentAttributeValue value = null;
				for (AssetDeploymentAttributeValue v : deployment.getAttributeValues()) {
					if (v.getAttribute().equals(a.getAttribute())) {
						value = v;
						break;
					}
				}
				if (value != null) {
					toolkit.createLabel(detailsPane, value.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS));
				}else {
					toolkit.createLabel(detailsPane, "");
				}
			}
		}
		detailsPane.layout(true);
		
		scrollDetails.setMinSize(detailsPane.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	
	
	public void initializePanel(Asset asset) {
		loadHistoryDataJob.setSystem(true);
		loadHistoryDataJob.schedule();
		refreshSummaryStatistics();
	}
	
	public void refreshSummaryStatistics() {
//		refreshSummaryStatsJob.setSystem(true);
		refreshSummaryStatsJob.schedule();
	}
	
	private void addDeployment() {
		AssetDeployment newDeployment = new AssetDeployment();
		newDeployment.setAsset(parentEditor.getAsset());
		newDeployment.setStartDate(new Date());
		newDeployment.setAssetWaypoints(new ArrayList<>());
		AssetDeploymentDialog dialog = new AssetDeploymentDialog(parentEditor.getSite().getShell(), newDeployment, allDeployments);
		ContextInjectionFactory.inject(dialog, parentContext);
		if (dialog.open() != Window.OK) return;
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				session.saveOrUpdate(newDeployment);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.displayLog("Unable to save changes to asset deployments.  Close editor and try again. " + ex.getMessage(), ex);
			}
		}
		parentEditor.fireAssetModified(false);
		allDeployments.add(new AssetDeploymentWrapper(newDeployment));
		sortDeployments();
		tblDeployments.refresh();
		refreshSummaryStatistics();
		
	}
	
	private void deleteSelectedDeployments() {
		if (tblDeployments == null) return;
		List<AssetDeploymentWrapper> toDelete = new ArrayList<>();
		for (Iterator<?> iterator = ((IStructuredSelection)tblDeployments.getSelection()).iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof AssetDeploymentWrapper) {
				toDelete.add((AssetDeploymentWrapper)x);			
			}
		}
		if (toDelete.isEmpty()) return;
		
		if (!MessageDialog.openQuestion(parentEditor.getSite().getShell(), "Delete Deployment Records", 
				MessageFormat.format("Are you sure you want to delete the {0} selected asset deployments? This will delete all data associated with these deployments including all images and observations.", toDelete.size()))){
			return;
		}
		
		//confirm password
		if (!AssetUtils.confirmPassword(parentEditor.getSite().getShell(), "Delete Asset Deployment", "Confirm your password to delete the selected deployment records.")) {
			return;
		}
		
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
			session.beginTransaction();
			try {
				for (AssetDeploymentWrapper deploy : toDelete) {
					//delete deployment & all associated waypoints/observations
					AssetDeployment d = session.get(AssetDeployment.class, deploy.getDeployment().getUuid());
					List<AssetWaypoint> dd = new ArrayList<>(d.getAssetWaypoints());
					d.getAssetWaypoints().clear();
					
					for (AssetWaypoint aw : dd) {
						session.delete(aw);
					}
					session.flush();
					
					//delete any waypoints not associated with asset waypoint
					try (ScrollableResults scroll = session.createQuery("FROM Waypoint ww WHERE source = :source and ww not in (SELECT waypoint FROM AssetWaypoint)").setParameter("source", AssetWaypointSource.KEY).scroll()){
						while(scroll.next()) {
							Waypoint wp = (Waypoint)scroll.get(0);
							session.delete(wp);
						}
					}
					
					session.flush();
					session.delete(d);
				}
				
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.displayLog("Unable to save changes to asset deployments.  Close editor and try again. " + ex.getMessage(), ex);
			}
		}
		parentEditor.fireAssetModified(false);
		allDeployments.removeAll(toDelete);
		tblDeployments.refresh();
		refreshSummaryStatistics();
		
	}
	
	private void editSelectedDeployments() {
		if (tblDeployments == null) return;
		Object toEdit = ((IStructuredSelection)tblDeployments.getSelection()).getFirstElement();
		if (toEdit == null) return;
		if (!(toEdit instanceof AssetDeploymentWrapper)) return;
		
		AssetDeployment toUpdate = ((AssetDeploymentWrapper)toEdit).getDeployment();
		AssetDeploymentDialog dialog = new AssetDeploymentDialog(parentEditor.getSite().getShell(), toUpdate, allDeployments);
		ContextInjectionFactory.inject(dialog, parentContext);
		if (dialog.open() != Window.OK) return;
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				session.saveOrUpdate(toUpdate);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.displayLog("Unable to save changes to asset deployments.  Close editor and try again. " + ex.getMessage(), ex);
			}
		}
		parentEditor.fireAssetModified(false);
		sortDeployments();
		tblDeployments.refresh();
		refreshSummaryStatistics();
	}
	
	private void sortDeployments() {
		allDeployments.sort((a,b)->{
			if (a.getDeployment().getEndDate() == null) return -1;
			if (b.getDeployment().getEndDate() == null) return 1;
			return b.getDeployment().getStartDate().compareTo(a.getDeployment().getStartDate());
		});
	}
	
	private Job refreshSummaryStatsJob = new Job("loading asset history data") {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (assetSummaryValues == null) return org.eclipse.core.runtime.Status.OK_STATUS; 
			
			final Map<IAssetSummary, Label> copy = new HashMap<>();
			copy.putAll(assetSummaryValues);
			
			HashMap<Label, String> values = new HashMap<>();
			try(Session s = HibernateManager.openSession()){
				for (Entry<IAssetSummary, Label> item : copy.entrySet()) {
					String result = item.getKey().getSummaryValue(parentEditor.getAsset(), s);
					values.put(item.getValue(), result);
				}			
			}
			Display.getDefault().syncExec(()->{
				for (Entry<Label, String> value : values.entrySet()) {
					String text = value.getValue();
					String tooltip = null;
					if (parentEditor.isDirty()) {
						text = text + "**";
						tooltip = "save changes to refresh statistics";
					}
					value.getKey().setText(text);
					value.getKey().setToolTipText(tooltip);
				}

			});
			return org.eclipse.core.runtime.Status.OK_STATUS;
		}
	};
	
	Job loadHistoryDataJob = new Job("loading asset deployment data") {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			List<AssetDeploymentTableColumn> tableColumns = new ArrayList<>();
			allDeployments = new ArrayList<>();
			Asset asset = parentEditor.getAsset();
			allDeploymentAttributes = new ArrayList<>();
			
			try(Session s = HibernateManager.openSession()){
				
				//compute table columns
				tableColumns.addAll(AssetDeploymentTableColumn.getTableColumns(asset, parentEditor.currentCrs, s));
				
				//TODO: add a date filter to list
				//get deployment data
				if (asset.getUuid() != null) {
					List<AssetDeployment> temp = QueryFactory.buildQuery(s,AssetDeployment.class, 
							new Object[] {"asset", asset}).list(); //$NON-NLS-1$
					temp.forEach(a->allDeployments.add(new AssetDeploymentWrapper(a)));
				}
				allDeployments.forEach(d->{
					d.getDeployment().getStationLocation().getId();
					d.getDeployment().getStationLocation().getStation().getId();
					for (AssetDeploymentAttributeValue v : d.getDeployment().getAttributeValues()) {
						v.getAttributeValueAsString(Locale.getDefault(), parentEditor.currentCrs);
					}
				});
				
				if (asset.getUuid() != null) {
					allDeploymentAttributes.addAll(QueryFactory.buildQuery(s, AssetTypeDeploymentAttribute.class, "id.assetType", asset.getAssetType()).list());
				}
				allDeploymentAttributes.forEach(e->e.getAttribute().getUuid());
			}
			//sort data
			sortDeployments();
			
			//update ui
			Display.getDefault().syncExec(()->{
				for(TableColumn tc : tblDeployments.getTable().getColumns()) tc.dispose();
				for (AssetDeploymentTableColumn column : tableColumns) {
					TableViewerColumn c = new TableViewerColumn(tblDeployments, SWT.NONE);
					c.getColumn().setText(column.getColumnName());
					c.setLabelProvider(column);
				}
				tblDeployments.setInput(allDeployments);
				for (TableColumn c : tblDeployments.getTable().getColumns()) {
					c.pack();
					c.setWidth(c.getWidth() + 20);
				}
				
			});
			computeDeploymentStats.schedule();
			return org.eclipse.core.runtime.Status.OK_STATUS;
		}
	};
	
	
	Job computeDeploymentStats = new Job("compute deployment stats") {
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (allDeployments == null) return Status.OK_STATUS;
			try(Session s = HibernateManager.openSession()){
				for (AssetDeploymentWrapper d : allDeployments) {
					
					Map<Statistic, Object> stats = StatisticsEngine.INSTANCE.computeStatistics(Collections.singleton(StatisticsEngine.Statistic.NUMBER_INCIDENTS), d.getDeployment());
					d.addStatistic(stats);
					
					Display.getDefault().syncExec(()->{
						if (tblDeployments.getTable().isDisposed()) return;
						tblDeployments.refresh(d, true);
					});
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
				}
			}
			return org.eclipse.core.runtime.Status.OK_STATUS;
		}
	};
}
