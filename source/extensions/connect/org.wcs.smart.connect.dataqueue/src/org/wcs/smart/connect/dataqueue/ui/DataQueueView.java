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

import org.eclipse.core.internal.jobs.JobManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.jobs.ProgressProvider;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.dataqueue.ConnectDataQueuePlugin;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.dataqueue.process.DataQueueItemProcessor;
import org.wcs.smart.connect.dataqueue.process.DataQueueManager;
import org.wcs.smart.connect.dataqueue.server.ConnectDataQueue;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.hibernate.SmartDB;

public class DataQueueView{ 
	
	public static final String ID = "org.wcs.smart.connect.dataqueue.queueview"; //$NON-NLS-1$
	
	private FormToolkit toolkit = null;
	
	private TableViewer tblHistory;

	
	@Inject private Shell shell;
	
	private SmartConnect connect;
	private Composite content;
	@Inject private UISynchronize ui;
	
	private enum Column{
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

	
	Job refreshJob = new Job("refresh data queue table"){

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
				
				List<DataQueueItem> allItems = new ArrayList<DataQueueItem>();
				allItems.addAll(serverItems);
				allItems.addAll(localItems);
				
				ui.syncExec(new Runnable() {
					
					@Override
					public void run() {
						if (content == null || content.isDisposed()) return;
						buildContent(allItems);
						
					}
				});
				monitor.done();
			}catch (Exception ex){
				ConnectDataQueuePlugin.log("Error loading items from server", ex);
				String message = ex.getMessage();
				if (ex instanceof ProcessingException && ex.getCause() != null){
					message = ex.getCause().getMessage();
				}
				final String error = message;
				ui.syncExec(new Runnable() {
					@Override
					public void run() {
						buildContentError(error);		
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
		Section dataQueueSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
		dataQueueSection.setText("Data Queue - Active");
		dataQueueSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dataQueueSection.setLayout(new GridLayout());
		
		Composite main = toolkit.createComposite(dataQueueSection);
		main.setLayout(new GridLayout());
		dataQueueSection.setClient(main);
//		
		this.content = toolkit.createComposite(main);
		content.setLayout(new GridLayout());
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnRefresh = new Button(main, SWT.PUSH);
		btnRefresh.setText("Refresh");
		btnRefresh.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshTable();
			}
		});
		
		Button btnProcess = new Button(main, SWT.PUSH);
		btnProcess.setText("Process Selected Item");
		btnProcess.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				processSelected();
			}
		});
		
		Section dataQueueHistory = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
		dataQueueHistory.setText("Data Queue - History");
		dataQueueHistory.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dataQueueHistory.setLayout(new GridLayout());
		
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
		Composite linkComp = toolkit.createComposite(parent);
		linkComp.setLayout(new GridLayout(4, false));
		((GridLayout)linkComp.getLayout()).marginHeight = 0;
		Hyperlink deleteSel = toolkit.createHyperlink(linkComp, "Remove Selected", SWT.NONE);
		Label sep = toolkit.createLabel(linkComp, "", SWT.SEPARATOR | SWT.VERTICAL);
		sep.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
		((GridData)sep.getLayoutData()).heightHint = 10;
		Hyperlink deleteAll = toolkit.createHyperlink(linkComp, "Remove All" , SWT.NONE);
		Label spacer = toolkit.createLabel(linkComp, "");
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		deleteSel.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				deleteSelected();
				refreshTable();
			}
		});
		deleteAll.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				deleteAll();
				refreshTable();
			}
		});
		
		refreshTable();
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
	private void refreshTable(){
		if (shell == null || shell.isDisposed()) return;
		
		//local history
		tblHistory.setInput(new String[]{"Loading..."});
		refreshHistory.schedule();
		
		//connect info
		if(connect == null){
			ConnectDialog cd = new ConnectDialog(shell);
			if (cd.open() != Window.OK){
				return;
			}
			connect = cd.getConnection();
		}
		for (Control x : content.getChildren()){
			x.dispose();
		}
		toolkit.createLabel(content, "Refreshing . . .");
		content.getParent().layout(true);
		refreshJob.schedule();
	}
	
	private List<Button> serveritems = new ArrayList<Button>();
	
	private void buildContentError(String error){
		for (Control x : content.getChildren()){
			x.dispose();
		}
		serveritems.clear();
		
		Composite part = toolkit.createComposite(content, SWT.BORDER);
		part.setLayout(new GridLayout(3, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lblError = toolkit.createLabel(part, "");
		lblError.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
		toolkit.createLabel(part, "ERROR:");
		toolkit.createLabel(part, error);
		
		content.getParent().layout(true);
	}
	
	private void buildContent(List<DataQueueItem> items){
		for (Control x : content.getChildren()){
			x.dispose();
		}
		serveritems.clear();
		for (DataQueueItem i : items){
			Composite part = toolkit.createComposite(content, SWT.BORDER);
			part.setLayout(new GridLayout(2, false));
			part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Button btnCheck = toolkit.createButton(part, "", SWT.CHECK);
			btnCheck.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
			btnCheck.setData(i);
			if (i instanceof LocalDataQueueItem){
				btnCheck.setVisible(false);
			}else{
				serveritems.add(btnCheck);
			}
			
			Composite info = toolkit.createComposite(part);
			info.setLayout(new GridLayout(2, false));
			((GridLayout)info.getLayout()).marginHeight= 0;
			info.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			Label lblName = toolkit.createLabel(info, "Name:");
			toolkit.createLabel(info, Column.NAME.getLabel(i));
			Label lblType = toolkit.createLabel(info, "Type:");
			toolkit.createLabel(info, Column.TYPE.getLabel(i));
			Label lblStatus = toolkit.createLabel(info, "Status:");
			toolkit.createLabel(info, Column.STATUS.getLabel(i));
			
			toolkit.createLabel(info, "Progress:");
			ProgressBar pbar = new ProgressBar(info, SWT.HORIZONTAL | SWT.SMOOTH);
			pbar.setMinimum(0);
			pbar.setMinimum(100);
			pbar.setSelection(50);
		}
		content.getParent().layout(true);
	}
	
	
//	private void configureProgress(){
//		Job.getJobManager().setProgressProvider(new ProgressProvider() {
//			
//			@Override
//			public IProgressMonitor createMonitor(Job job) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//		});;
//		Job.getJobManager().setPaddJobChangeListener(new JobChangeAdapter() {
//			
//			@Override
//			public void scheduled(IJobChangeEvent event) {
//				if (event.getJob() instanceof DataQueueItemProcessor){
//					event.getJob().
//				}
//			}
//			
//			@Override
//			public void done(IJobChangeEvent event) {
//				// TODO Auto-generated method stub
//				
//			}
//		});
//		
//		
//	}
	private void processSelected(){
		for (Button b : serveritems){
			if (b.isDisposed()) continue;
			if (b.getSelection()){
				try{
					DataQueueManager.INSTANCE.addItemToQueue((DataQueueItem)b.getData());
				}catch (Exception ex){
					ConnectDataQueuePlugin.displayLog("Error adding item to data queue.", ex);
				}
			}
		}
		//TODO: this will start a new one; we want only one ever
		DataQueueItemProcessor.start(connect);
		refreshTable();
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