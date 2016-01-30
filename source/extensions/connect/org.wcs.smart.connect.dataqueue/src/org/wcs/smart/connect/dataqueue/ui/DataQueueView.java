package org.wcs.smart.connect.dataqueue.ui;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.ws.rs.ProcessingException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.hibernate.mapping.Array;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.dataqueue.ConnectDataQueuePlugin;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.dataqueue.process.DataQueueManager;
import org.wcs.smart.connect.dataqueue.process.ProcessorManager;
import org.wcs.smart.connect.dataqueue.server.ConnectDataQueue;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.hibernate.SmartDB;

public class DataQueueView{ 
	
	public static final String ID = "org.wcs.smart.connect.dataqueue.queueview"; //$NON-NLS-1$
	
	private FormToolkit toolkit = null;
	
	private TableViewer tblHistory;
	private CheckboxTableViewer tblServer;
	
	private SashForm sash;
	
	private SmartConnect connect;
	private Composite localDataQueue;
	
	private ScrolledForm  dataQueueScrollForm;
	
	@Inject private Shell shell;
	@Inject private UISynchronize ui;
	
	private enum Column{
		CHECK(""),
		NAME("Name"),
		TYPE("Type"),
		STATUS("Status"),
		DATE("Date Processed"),
		ERROR("Error");
		
		String name;
		
		Column(String name){
			this.name = name;
		}
		
		public String getLabel(DataQueueItem item){
			if (this == NAME){
				return item.getName();
			}else if (this == TYPE){
				return item.getType().toString();
			}else if (this == STATUS){
				if (item instanceof LocalDataQueueItem){
					return ((LocalDataQueueItem) item).getStatus().toString();
				}else{
					return "ON SERVER";
				}
			}else if (this == DATE){
				if (item instanceof LocalDataQueueItem){
					if (((LocalDataQueueItem) item).getDateProcessed() == null) return "";
					return DateFormat.getDateTimeInstance().format(((LocalDataQueueItem) item).getDateProcessed());
				}
			}else if (this == ERROR){
				if (item instanceof LocalDataQueueItem){
					return ((LocalDataQueueItem) item).getErrorMessage();
				}
			}
			return "";
		}
	}
	Job refreshLocalJob = new Job("Refresh Local Data Queue Table"){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try{
				monitor.beginTask("Refreshing Data Queue Items", 2);
				
				monitor.worked(1);
				monitor.subTask("Loading Local Items ...");
				List<LocalDataQueueItem> localItems = DataQueueManager.INSTANCE.getLocalItems(LocalDataQueueItem.Status.QUEUED,
						LocalDataQueueItem.Status.PROCESSING, 
						LocalDataQueueItem.Status.DOWNLOADING,
						LocalDataQueueItem.Status.ERROR);
				Collections.sort(localItems, new Comparator<LocalDataQueueItem>() {

					@Override
					public int compare(LocalDataQueueItem o1,
							LocalDataQueueItem o2) {
						if (o1.getStatus() == LocalDataQueueItem.Status.DOWNLOADING){
							if (o2.getStatus() == LocalDataQueueItem.Status.DOWNLOADING){
								return o1.getDateProcessed().compareTo(o2.getDateProcessed());
							}
							return -1;
						}
						if (o2.getStatus() == LocalDataQueueItem.Status.DOWNLOADING){
							if (o2.getStatus() == LocalDataQueueItem.Status.DOWNLOADING){
								return o2.getDateProcessed().compareTo(o1.getDateProcessed());
							}
							return 1;
						}
						if (o1.getStatus() == LocalDataQueueItem.Status.PROCESSING){
							if (o2.getStatus() == LocalDataQueueItem.Status.PROCESSING){
								return o1.getDateProcessed().compareTo(o2.getDateProcessed());
							}
							return -1;
						}
						if (o2.getStatus() == LocalDataQueueItem.Status.PROCESSING){
							if (o1.getStatus() == LocalDataQueueItem.Status.PROCESSING){
								return o2.getDateProcessed().compareTo(o1.getDateProcessed());
							}
							return 1;
						}	
						
						if (o1.getStatus() == LocalDataQueueItem.Status.QUEUED){
							if (o2.getStatus() == LocalDataQueueItem.Status.QUEUED){
								return o1.getOrder().compareTo(o2.getOrder());
							}
							return -1;
						}
						if (o2.getStatus() == LocalDataQueueItem.Status.QUEUED){
							if (o2.getStatus() == LocalDataQueueItem.Status.QUEUED){
								return o2.getOrder().compareTo(o1.getOrder());
							}
							return 1;
						}	
						return o1.getDateProcessed().compareTo(o2.getDateProcessed());
						
					}
				});
				
				ui.syncExec(new Runnable() {
					
					@Override
					public void run() {
						if (localDataQueue == null || localDataQueue.isDisposed()) return;
						buildLocalContent(localItems);
						
					}
				});
				monitor.done();
			}catch (Exception ex){
				final String message = "Error loading active local data queue items." + ex.getMessage();
				ConnectDataQueuePlugin.log(message, ex);
				ui.syncExec(new Runnable() {					
					@Override
					public void run() {
						buildLocalErrorContent(message);
					}
				});
			}
			return Status.OK_STATUS;
		}
	};
	
	Job refreshServerItemsJob = new Job("refresh server items table"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try{
				monitor.beginTask("Refreshing Data Queue Items", 2);
				monitor.subTask("Loading Items From Connect...");
				List<DataQueueItem> serverItems = ConnectDataQueue.INSTANCE.getQueuedItems(connect, SmartDB.getCurrentConservationArea());
				
				monitor.worked(1);
				monitor.subTask("Loading Local Items ...");
				List<LocalDataQueueItem> localItems = DataQueueManager.INSTANCE.getLocalItems(LocalDataQueueItem.Status.QUEUED, LocalDataQueueItem.Status.PROCESSING, LocalDataQueueItem.Status.DOWNLOADING);

				//remove any items from the serverItems that are in the local Items
				//these have been queued and we do not need to display them twice
				for (LocalDataQueueItem i : localItems){
					for (DataQueueItem server : serverItems){
						if (server.getUuid().equals(i.getServerItemUuid())){
							serverItems.remove(server);
							break;
						}
					}
				}
				
				ui.syncExec(new Runnable() {
					
					@Override
					public void run() {
						if (tblServer == null || tblServer.getTable().isDisposed()) return;
						tblServer.setInput(serverItems);
						tblServer.refresh();
					}
				});
				monitor.done();
			}catch (Exception ex){
				String message = "Error loading items from Connect. " + ex.getMessage();
				ConnectDataQueuePlugin.log(message, ex);
				
				final String error = message;
				ui.syncExec(new Runnable() {
					@Override
					public void run() {
						tblServer.setInput(new String[]{error});
						tblServer.refresh();
					}
				});
				
			}
			return Status.OK_STATUS;
		}
		
	};
	Job refreshHistory = new Job("refresh data queue history "){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try{
				monitor.beginTask("Refreshing Data Queue History", 2);
				
				monitor.subTask("Loading Local Items ...");
				List<LocalDataQueueItem> localItems = DataQueueManager.INSTANCE.getLocalItems(LocalDataQueueItem.Status.ERROR, LocalDataQueueItem.Status.COMPLETE);
				Collections.sort(localItems, new Comparator<LocalDataQueueItem>() {
					@Override
					public int compare(LocalDataQueueItem o1,
							LocalDataQueueItem o2) {
						if (o1.getDateProcessed() == null && o2.getDateProcessed() == null) return 0;
						if (o1.getDateProcessed() != null && o2.getDateProcessed() != null){
							return o2.getDateProcessed().compareTo(o1.getDateProcessed());
						}
						if (o1.getDateProcessed() == null) return 1;
						return -1;
					}
				});
				monitor.worked(1);

				ui.syncExec(new Runnable() {
					@Override
					public void run() {
						if (tblHistory == null || tblHistory.getTable().isDisposed()) return;
						tblHistory.setInput(localItems);
						tblHistory.refresh();
						
					}
				});
				monitor.done();
			}catch (Exception ex){
				ConnectPlugIn.displayLog("Error loading items from server", ex);
			}
			return Status.OK_STATUS;
		}
		
	};
	/**
	 * Creates new view
	 */
	public DataQueueView() {
}

	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@PreDestroy
	public void dispose(){
		toolkit.dispose();
		
	}

	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@PostConstruct
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout());
		
		toolkit = new FormToolkit(parent.getDisplay());
		toolkit.adapt(parent);
		
		sash= new SashForm(parent, SWT.VERTICAL);
		sash.setLayout(new GridLayout());
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(sash);
		
//		Composite sashForm = new Composite(parent, SWT.VERTICAL);
//		sashForm.setLayout(new GridLayout());
//		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		toolkit.adapt(sashForm);
		
		createLocalSection(sash);
		createServerSection(sash);
		createLocalHistorySection(sash);
		
		refreshLocalTable();
		refreshServerTable();
		refreshHistory();
	}

	private void createLocalSection(Composite parent){
		Section dataQueueSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
		dataQueueSection.setText("Local Data Queue [Active Items]");
		dataQueueSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dataQueueSection.setLayout(new GridLayout());
		addExpansionHandler(dataQueueSection);
		
		Composite main = toolkit.createComposite(dataQueueSection);
		main.setLayout(new GridLayout());
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		dataQueueSection.setClient(main);
		
		dataQueueScrollForm = toolkit.createScrolledForm(main);
		dataQueueScrollForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dataQueueScrollForm.getBody().setLayout(new GridLayout());
		((GridLayout)dataQueueScrollForm.getBody().getLayout()).marginWidth = 0;
		((GridLayout)dataQueueScrollForm.getBody().getLayout()).marginHeight = 0;
		
//		Composite innerSc = toolkit.createComposite(sc, SWT.NONE);
//		innerSc.setLayout(new GridLayout());
//		sc.setContent(innerSc);
//		sc.setExpandHorizontal(true);
//		sc.setExpandVertical(true);
//		sc.setMinSize(innerSc.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		this.localDataQueue = toolkit.createComposite(dataQueueScrollForm.getBody());
		localDataQueue.setLayout(new GridLayout());
		((GridLayout)localDataQueue.getLayout()).marginWidth = 0;
		((GridLayout)localDataQueue.getLayout()).marginHeight = 0;
		localDataQueue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Hyperlink link = toolkit.createHyperlink(main, "Refresh", SWT.NONE);
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				refreshLocalTable();
			}
		});
	}
	
	private void createServerSection(Composite parent){
		Section dataQueueSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
		dataQueueSection.setText("SMART Connect Server - Data Queue Items");
		dataQueueSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dataQueueSection.setLayout(new GridLayout());
		addExpansionHandler(dataQueueSection);
		
		Composite main = toolkit.createComposite(dataQueueSection);
		main.setLayout(new GridLayout());
		dataQueueSection.setClient(main);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		

		tblServer = CheckboxTableViewer.newCheckList(main, SWT.FULL_SELECTION | SWT.BORDER);
		tblServer.getTable().setLinesVisible(true);
		tblServer.getTable().setHeaderVisible(true);
		tblServer.setContentProvider(ArrayContentProvider.getInstance());
		tblServer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		for (final Column c : new Column[]{Column.CHECK, Column.NAME, Column.TYPE, Column.STATUS}){
			TableViewerColumn tc = new TableViewerColumn(tblServer, SWT.DEFAULT);
			tc.getColumn().setWidth(100);
			tc.getColumn().setText(c.name);
			tc.setLabelProvider(new ColumnLabelProvider(){
				@Override
				public String getText(Object element){
					if (element instanceof DataQueueItem){
						return c.getLabel((DataQueueItem)element);
					}
					return super.getText(element);
				}
			});
		}
		Hyperlink link = toolkit.createHyperlink(main, "Refresh", SWT.NONE);
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				refreshServerTable();
			}
		});
		Composite c = toolkit.createComposite(main, SWT.NONE);
		c.setLayout(new GridLayout(2, false));
		Button btn = toolkit.createButton(c, "Process All", SWT.PUSH);
		btn.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				processAll();
			}
		});
		Button btn2 = toolkit.createButton(c, "Process Selected", SWT.PUSH);
		btn2.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				processSelected();
			}
		});
	}
	
	private void addExpansionHandler(final Section section){
		section.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				int initHeight = 0;
				if (section.isExpanded()){
					
//					((SashForm)section.getParent()).setWeights(new int[]{2,50,50});
//					((SashFormData)section.getLayoutData()).heightHint = 200;
					section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
					initHeight = 200;
				}else{
//					((SashForm)section.getParent()).setWeights(new int[]{20,40,40});
//					((GridData)section.getLayoutData()).heightHint = 0;
					section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					Point p = section.computeSize(SWT.DEFAULT, SWT.DEFAULT);
					initHeight = p.y;
				}
				
				Point sashsize = sash.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				int[] weights = sash.getWeights();
				int index = 0;
				int total = 0;
				
				for (Control c : sash.getChildren()){
					if (c == section){
					break;	
					}
					index++;
				}
				for (int i = 0; i < weights.length; i ++){
					if(i==index) continue;
					total+=weights[i];
				}
				int value =initHeight;
				
				for (int i = 0; i < weights.length; i ++){
					if (i == index){
						weights[i] = value;
					}else{
						weights[i] = (int)((weights[i] / (1.0*total)) * (sashsize.y - value)); 
					}
				}
				sash.setWeights(weights);
				section.getParent().layout(true, true);
			}
		});
	}
	private void createLocalHistorySection(Composite parent){
		Section dataQueueHistory = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		dataQueueHistory.setText("Data Queue - History");
		dataQueueHistory.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dataQueueHistory.setLayout(new GridLayout());
		addExpansionHandler(dataQueueHistory);
		
		Composite historyMain = toolkit.createComposite(dataQueueHistory);
		historyMain.setLayout(new GridLayout());
		dataQueueHistory.setClient(historyMain);
		
		tblHistory = new TableViewer(historyMain, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		tblHistory.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tblHistory.getTable().getLayoutData()).heightHint = 200;
		tblHistory.setContentProvider(ArrayContentProvider.getInstance());
		tblHistory.getTable().setHeaderVisible(true);
		tblHistory.getTable().setLinesVisible(true);
		for (final Column c : Column.values()){
			if (c == Column.CHECK) continue;
			TableViewerColumn column = new TableViewerColumn(tblHistory, SWT.LEFT);
			column.getColumn().setText(c.name);
			column.setLabelProvider(new ColumnLabelProvider(){
				@Override
				public String getText(Object element){
					if (element instanceof DataQueueItem){
						return c.getLabel((DataQueueItem)element);
					}
					return super.getText(element);
				}
			});
			column.getColumn().setWidth(100);
		}
		
		Composite linkComp = toolkit.createComposite(historyMain);
		linkComp.setLayout(new GridLayout(6, false));
		((GridLayout)linkComp.getLayout()).marginHeight = 0;
		
		Hyperlink deleteRefresh = toolkit.createHyperlink(linkComp, "Refresh", SWT.NONE);
		Label sep = toolkit.createLabel(linkComp, "", SWT.SEPARATOR | SWT.VERTICAL);
		sep.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
		((GridData)sep.getLayoutData()).heightHint = 10;
		
		Hyperlink deleteSel = toolkit.createHyperlink(linkComp, "Remove Selected", SWT.NONE);
		sep = toolkit.createLabel(linkComp, "", SWT.SEPARATOR | SWT.VERTICAL);
		sep.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
		((GridData)sep.getLayoutData()).heightHint = 10;
		
		Hyperlink deleteAll = toolkit.createHyperlink(linkComp, "Remove All" , SWT.NONE);
		Label spacer = toolkit.createLabel(linkComp, "");
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		deleteRefresh.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				refreshHistory();
			}
		});
		deleteSel.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				deleteSelected();
				refreshHistory();
			}
		});
		deleteAll.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				deleteAll();
				refreshHistory();
			}
		});
	}
	
	private void deleteSelected(){
		List<LocalDataQueueItem> items = new ArrayList<LocalDataQueueItem>();
		IStructuredSelection sel = (IStructuredSelection) tblHistory.getSelection();
		for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
			Object item = iterator.next();
			if (item instanceof LocalDataQueueItem){
				items.add((LocalDataQueueItem)item);
			}
		}
		DataQueueManager.INSTANCE.deleteItems(items);
	}
	private void deleteAll(){
		DataQueueManager.INSTANCE.deleteAllHistory();
	}
	
	private void refreshHistory(){
		if (shell == null || shell.isDisposed()) return;
		//local history
		tblHistory.setInput(new String[]{"Loading..."});
		tblHistory.refresh();
		refreshHistory.schedule();
	}
	private void refreshServerTable(){
		tblServer.setInput(new String[]{"Loading..."});
		tblServer.refresh();
		if(connect == null){
			ConnectDialog cd = new ConnectDialog(shell);
			if (cd.open() != Window.OK){
				return;
			}
			connect = cd.getConnection();
		}
		
		refreshServerItemsJob.schedule();
	}
	
	private void refreshLocalTable(){
		if (shell == null || shell.isDisposed()) return;
		
		//local history
		tblHistory.setInput(new String[]{"Loading..."});
		refreshHistory.schedule();

		for (Control x : localDataQueue.getChildren()){
			x.dispose();
		}
		toolkit.createLabel(localDataQueue, "Refreshing . . .");
		localDataQueue.getParent().layout(true);
		refreshLocalJob.schedule();
	}
	
	private void buildLocalErrorContent(String message){
		for (Control x : localDataQueue.getChildren()){
			x.dispose();
		}
		
		Composite info = toolkit.createComposite(localDataQueue, SWT.BORDER);
		info.setLayout(new GridLayout(2, false));
		((GridLayout)info.getLayout()).marginHeight= 0;
		info.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Label lblError = toolkit.createLabel(info, "");
		lblError.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
		toolkit.createLabel(info, message);
		
		
	}
	private void buildLocalContent(List<LocalDataQueueItem> items){
		for (Control x : localDataQueue.getChildren()){
			x.dispose();
		}
		if (items.isEmpty()){
			Composite part = toolkit.createComposite(localDataQueue, SWT.BORDER);
			part.setLayout(new GridLayout(2, false));
			part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Label lblimage = toolkit.createLabel(part, "");
			lblimage.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.WARN_ICON));
			toolkit.createLabel(part, "No local items in the queue.");
		}
		
		for (DataQueueItem i : items){
			Composite part = toolkit.createComposite(localDataQueue, SWT.BORDER);
			part.setLayout(new GridLayout(4, true));
//			((GridLayout)part.getLayout()).marginWidth= 0;
			((GridLayout)part.getLayout()).marginHeight= 2;
			part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Label lblStatusImage = toolkit.createLabel(part, "");
			
			Composite info = toolkit.createComposite(part);
			info.setLayout(new GridLayout(2, false));
			((GridLayout)info.getLayout()).marginHeight= 0;
			((GridLayout)info.getLayout()).marginWidth= 0;
			info.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			Label lblName = toolkit.createLabel(info, "Name:");
			toolkit.createLabel(info, Column.NAME.getLabel(i));
			
			Composite info2 = toolkit.createComposite(part);
			info2.setLayout(new GridLayout(2, false));
			((GridLayout)info2.getLayout()).marginWidth= 0;
			((GridLayout)info2.getLayout()).marginHeight= 0;
			info2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			Label lblType = toolkit.createLabel(info2, "Type:");
			toolkit.createLabel(info2, Column.TYPE.getLabel(i));
			
			Composite info3 = toolkit.createComposite(part);
			info3.setLayout(new GridLayout(2, false));
			((GridLayout)info3.getLayout()).marginWidth= 0;
			((GridLayout)info3.getLayout()).marginHeight= 0;
			info3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			Label lblStatus = toolkit.createLabel(info3, "Status:");
			Label sLabel = toolkit.createLabel(info3, Column.STATUS.getLabel(i));
			sLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			ProgressWidget widget = new ProgressWidget(part, sLabel, lblStatusImage);
			widget.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
			widget.initStatus(((LocalDataQueueItem)i).getStatus());
			toolkit.adapt(widget);
			
			ProcessorManager.INSTANCE.register(widget, i);
		}
		localDataQueue.getParent().getParent().layout(true);	
		localDataQueue.getParent().layout(true);	
		dataQueueScrollForm.reflow(true);
	}
	
	private void processAll(){
		List<DataQueueItem> items = (List<DataQueueItem>) tblServer.getInput();
		processItems(items);
	}
	
	private void processSelected(){
		List<DataQueueItem> items = new ArrayList<DataQueueItem>();
		for (Object x : tblServer.getCheckedElements()){
			if (x instanceof DataQueueItem){
				items.add((DataQueueItem)x);
			}
		}
		processItems(items);
	}
	
	private void processItems(List<DataQueueItem> items){
		for (DataQueueItem i : items){
			try{
				DataQueueManager.INSTANCE.addItemToQueue(i);
			}catch (Exception ex){
				ConnectDataQueuePlugin.displayLog("Error adding item to data queue.", ex);
			}
		}
		ProcessorManager.INSTANCE.startProcessing(connect);
		refreshLocalTable();
		refreshServerTable();
	}
	
	@Focus
	public void setFocus() {
		
	}

	public static class DataQueueViewWrapper extends DIViewPart<DataQueueView>{
		public DataQueueViewWrapper(){
			super(DataQueueView.class);
		}
	}
}