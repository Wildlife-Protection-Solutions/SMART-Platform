package org.wcs.smart.asset.ui.views.asset;

import java.text.MessageFormat;
import java.util.ArrayList;
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
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetUtils;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetDeploymentAttributeValue;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SmartUtils;

public class AssetDeploymentPage {

	@Inject
	private IEclipseContext parentContext;
	
	private TableViewer tblDeployments;
	
	private Map<IAssetSummary, Label> assetSummaryValues;

	private List<AssetDeployment> modifiedDeployments;
	private List<AssetDeployment> toDeleteDeployments;
	private List<AssetDeployment> allDeployments;
	
	private AssetEditor parentEditor;
	
	public AssetDeploymentPage(AssetEditor parent) {
		this.parentEditor = parent;
		modifiedDeployments = new ArrayList<>();
		toDeleteDeployments = new ArrayList<>();
	}
	
	public List<AssetDeployment> getModifiedDeployments(){
		return this.modifiedDeployments;
	}
	
	public List<AssetDeployment> getDeletedDeployments(){
		return this.toDeleteDeployments;
	}
	
	
	
	public Composite createDeploymentsSection(Composite parent, FormToolkit toolkit) {
		
		Composite panel = toolkit.createComposite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
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
			Label sl = toolkit.createLabel(summaryPanel, s.getSummaryName());
			
			Label sv = toolkit.createLabel(summaryPanel, "");
			sv.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			sv.setData(s);
			
			assetSummaryValues.put(s,  sv);
		}
		
		Composite historyPanel = toolkit.createComposite(panel, SWT.BORDER);
		historyPanel.setLayout(new GridLayout(2, false));
		historyPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		l = toolkit.createLabel(historyPanel, "Asset Deployments");
		fd = l.getFont().getFontData()[0];
		fd.setHeight(fd.getHeight() + 1);
		fd.setStyle(SWT.BOLD);
		Font boldFont1 = new Font(l.getDisplay(), fd);
		l.setFont(boldFont1);
		l.addListener(SWT.Dispose, e->boldFont1.dispose());
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		ToolItem itemDelete = null;
		ToolItem itemEdit = null;
		if (!parentEditor.getAsset().getIsRetired()) {
			ToolBar toolbar = new ToolBar(historyPanel, SWT.FLAT);
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
		
		tblDeployments = new TableViewer(historyPanel, SWT.FULL_SELECTION | SWT.MULTI);
		tblDeployments.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		tblDeployments.setContentProvider(ArrayContentProvider.getInstance());
		tblDeployments.getTable().setHeaderVisible(true);
		tblDeployments.getTable().setLinesVisible(true);
		
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
			
			tblDeployments.addDoubleClickListener(new IDoubleClickListener() {
				
				@Override
				public void doubleClick(DoubleClickEvent event) {
					editSelectedDeployments();
				}
			});
			
			Menu mnuDeployments = new Menu(tblDeployments.getControl());
			
			MenuItem mnuAdd = new MenuItem(mnuDeployments, SWT.PUSH);
			mnuAdd.setText("New Deployment...");
			mnuAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			mnuAdd.addListener(SWT.Selection, e-> addDeployment());
					
			MenuItem mnuEdit = new MenuItem(mnuDeployments, SWT.PUSH);
			mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
			mnuEdit.addListener(SWT.Selection, e-> editSelectedDeployments());
			mnuEdit.setText("Edit Deployment...");
			
			MenuItem mnuDelete = new MenuItem(mnuDeployments, SWT.PUSH);
			mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			mnuDelete.addListener(SWT.Selection, e->deleteSelectedDeployments());
			mnuDelete.setText("Delete Deployments...");
			
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
		
		initializePanel(parentEditor.getAsset());
		refreshSummaryStatistics();
		
		return panel;
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
		
		AssetDeploymentDialog dialog = new AssetDeploymentDialog(parentEditor.getSite().getShell(), newDeployment, allDeployments);
		ContextInjectionFactory.inject(dialog, parentContext);
		if (dialog.open() == Window.OK) {
			modifiedDeployments.add(newDeployment);
			allDeployments.add(newDeployment);
			tblDeployments.refresh();
			parentEditor.setDirty(true);
			refreshSummaryStatistics();
		}
		
	}
	
	private void deleteSelectedDeployments() {
		//TODO:		
		if (tblDeployments == null) return;
		List<AssetDeployment> toDelete = new ArrayList<>();
		for (Iterator<?> iterator = ((IStructuredSelection)tblDeployments.getSelection()).iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof AssetDeployment) {
				toDelete.add((AssetDeployment)x);			
			}
		}
		if (toDelete.isEmpty()) return;
		
		if (!MessageDialog.openQuestion(parentEditor.getSite().getShell(), "Delete Deployment Records", 
				MessageFormat.format("Are you sure you want to delete the {0} selected asset deployments? This will delete all data associated with these deployments including all images and observations.", toDelete.size()))){
			return;
		}
		
		//confirm password
		if (!AssetUtils.confirmPassword(parentEditor.getSite().getShell(), "Delete Asset Deployment", "Confirm your password to delete the selected history records.")) {
			return;
		}
		
		toDelete.forEach(x->{
			//currentDeployments.remove(x);
			toDeleteDeployments.add((AssetDeployment) x);
		});
		parentEditor.setDirty(true);
		tblDeployments.refresh();
		refreshSummaryStatistics();
		
	}
	
	private void editSelectedDeployments() {
		if (tblDeployments == null) return;
		Object toEdit = ((IStructuredSelection)tblDeployments.getSelection()).getFirstElement();
		if (toEdit == null) return;
		if (!(toEdit instanceof AssetDeployment)) return;
		
		AssetDeployment toUpdate = (AssetDeployment)toEdit;
		AssetDeploymentDialog dialog = new AssetDeploymentDialog(parentEditor.getSite().getShell(), toUpdate, allDeployments);
		ContextInjectionFactory.inject(dialog, parentContext);
		if (dialog.open() == Window.OK) {
			modifiedDeployments.add(toUpdate);
			parentEditor.setDirty(true);
			tblDeployments.refresh();
			refreshSummaryStatistics();
		}
	}
	
	private Job refreshSummaryStatsJob = new Job("loading asset history data)") {
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
	
	Job loadHistoryDataJob = new Job("loading asset history data)") {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			List<AssetDeploymentTableColumn> tableColumns = new ArrayList<>();
			allDeployments = new ArrayList<>();
			Asset asset = parentEditor.getAsset();
			
			try(Session s = HibernateManager.openSession()){
				
				//compute table columns
				tableColumns.addAll(AssetDeploymentTableColumn.getTableColumns(asset, s));
				
				//TODO: add a date filter to list
				//get deployment data
				List<AssetDeployment> temp = QueryFactory.buildQuery(s,AssetDeployment.class, 
						new Object[] {"asset", asset}).list(); //$NON-NLS-1$
				allDeployments.addAll(temp);
				allDeployments.forEach(d->{
					d.getStationLocation().getId();
					d.getStationLocation().getStation().getId();
					for (AssetDeploymentAttributeValue v : d.getAttributeValues()) {
						v.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS);//TODO: CRS
					}
				});
				
			}
			//sort data
			allDeployments.sort((a,b)->{
				if (a.getEndDate() == null) return -1;
				if (b.getEndDate() == null) return 1;
				return b.getStartDate().compareTo(a.getStartDate());
			});
			//update ui
			Display.getDefault().syncExec(()->{
				for(TableColumn tc : tblDeployments.getTable().getColumns()) tc.dispose();
				for (AssetDeploymentTableColumn column : tableColumns) {
					TableViewerColumn c = new TableViewerColumn(tblDeployments, SWT.NONE);
					c.getColumn().setText(column.getColumnName());
					c.getColumn().setWidth(125);
					c.setLabelProvider(column);
					
				}
				tblDeployments.setInput(allDeployments);
				
				
			});
			return org.eclipse.core.runtime.Status.OK_STATUS;
		}
	};
}
