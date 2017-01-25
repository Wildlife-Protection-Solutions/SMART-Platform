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
package org.wcs.smart.connect.dataqueue.internal.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.transaction.Synchronization;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.ConnectServerManager;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.dataqueue.ConnectDataQueuePlugin;
import org.wcs.smart.connect.dataqueue.internal.Messages;
import org.wcs.smart.connect.dataqueue.internal.process.DataQueueManager;
import org.wcs.smart.connect.dataqueue.internal.process.IDataQueueListener;
import org.wcs.smart.connect.dataqueue.internal.process.ProcessorManager;
import org.wcs.smart.connect.dataqueue.internal.server.ConnectDataQueue;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * View for displaying data queue processing information and allowing users to manually
 * control the data queue.
 * 
 * @author Emily
 *
 */
public class DataQueueView{ 
	
	public static final String ID = "org.wcs.smart.connect.dataqueue.queueview"; //$NON-NLS-1$
	
	private FormToolkit toolkit = null;

	private CheckboxTableViewer tblServer;
	private SashForm sash;
	private DataQueueTable dataQueueTable;
	
	private SmartConnect connect;

	@Inject private Shell shell;
	@Inject private UISynchronize ui;
	
	private IDataQueueListener listener = new IDataQueueListener() {
		@Override
		public void dataQueueModified() {
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					refreshLocalTable();		
				}});
		}
	};
	
	private ConnectServerManager.IConnectServerEventHandler handler = new ConnectServerManager.IConnectServerEventHandler() {
		
		@Override
		public void beforeDelete(Session session) throws Exception {
			session.getTransaction().registerSynchronization(new Synchronization() {
				@Override
				public void beforeCompletion() {
				}
				
				@Override
				public void afterCompletion(int status) {
					if (status == javax.transaction.Status.STATUS_COMMITTED){
						connect = null;
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								tblServer.setInput(new Object[]{});
								dataQueueTable.setInput(new Object[]{});
							}
						});
					}
				}
			});
			connect = null;
		}
	};
	
	private enum ServerColumn{
		CHECK("", 30), //$NON-NLS-1$
		NAME(Messages.DataQueueView_NameColumnName, 150),
		TYPE(Messages.DataQueueView_TypeColumName, 100),
		STATUS(Messages.DataQueueView_StatusColumnName, 100);
		
		String guiName;
		int width;
		
		ServerColumn(String name, int width){
			this.guiName = name;
			this.width = width;
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
					return Messages.DataQueueView_OnServerStatus;
				}
			}
			return ""; //$NON-NLS-1$
		}
	}
	Job refreshLocalJob = new Job(Messages.DataQueueView_RefreshLocalJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try{
				monitor.beginTask(Messages.DataQueueView_RefreshLocalTask1, 2);
				
				monitor.worked(1);
				monitor.subTask(Messages.DataQueueView_RefreshLocalTask2);
				List<LocalDataQueueItem> localItems = DataQueueManager.INSTANCE.getLocalItems();
				sort(localItems);				
				
				ui.syncExec(new Runnable() {
					@Override
					public void run() {
						if (dataQueueTable == null || dataQueueTable.isDisposed()) return;
						buildLocalContent(localItems);
					}
				});
				monitor.done();
			}catch (Exception ex){
				final String message = Messages.DataQueueView_RefreshLocalError1 + ex.getMessage();
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
	
	Job refreshServerItemsJob = new Job(Messages.DataQueueView_ServerRefreshJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try{
				monitor.beginTask(Messages.DataQueueView_ServerRefreshTask1, 2);
				monitor.subTask(Messages.DataQueueView_ServerRefreshTask2);
				
				if(connect == null){
					ConnectServer server = null;
					ConnectUser user = null;
					
					Session s = HibernateManager.openSession();
					try{
						server = ConnectHibernateManager.getConnectServer(s);
						user = ConnectHibernateManager.getConnectUser(SmartDB.getCurrentEmployee(), s);
					}finally{
						s.close();
					}
					
					if (server != null && user != null){
						if (user.getConnectUsername() != null && user.getConnectPassword() != null){
							connect = SmartConnect.findInstance(server, user.getConnectUsername(), ConnectPlugIn.decryptPassword(user));
						}
						
					}
					if (connect == null){
						ui.syncExec(new Runnable(){
							@Override
							public void run() {
								DataQueueServerDialog cd = new DataQueueServerDialog(shell);
								if (cd.open() != Window.OK){
									tblServer.setInput(new String[]{Messages.DataQueueView_ServerRefreshError1});
									return;
								}
								connect = cd.getConnection();		
							}	
						});
					}
					
				}
				if (connect == null){
					return Status.OK_STATUS;
				}
				
				List<DataQueueItem> serverItems = ConnectDataQueue.INSTANCE.getQueuedItems(connect, SmartDB.getCurrentConservationArea());
				
				monitor.worked(1);
				monitor.subTask(Messages.DataQueueView_ServerRefreshTask3);
				List<LocalDataQueueItem> localItems = DataQueueManager.INSTANCE.getLocalItems(
						LocalDataQueueItem.Status.QUEUED, 
						LocalDataQueueItem.Status.REQUEUED,
						LocalDataQueueItem.Status.PROCESSING, 
						LocalDataQueueItem.Status.DOWNLOADING);

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
				String message = Messages.DataQueueView_ServerRefreshError2 + ex.getMessage();
				ConnectDataQueuePlugin.log(message, ex);
				
				final String error = message;
				ui.syncExec(new Runnable() {
					@Override
					public void run() {
						if (tblServer.getTable().isDisposed()) return;
						tblServer.setInput(new String[]{error});
						tblServer.refresh();
					}
				});
				
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
		
		DataQueueManager.INSTANCE.removeListener(listener);
		ConnectServerManager.INSTANCE.removeHandler(handler);
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
		
		createServerSection(sash);
		createLocalSection(sash);
	
		sash.setWeights(new int[]{40, 60});
		
		refreshLocalTable();
		refreshServerTable();
		
		DataQueueManager.INSTANCE.addListener(listener);
		ConnectServerManager.INSTANCE.addHandler(handler);
	}

	private void sort(List<LocalDataQueueItem> items){
		Collections.sort(items, new Comparator<LocalDataQueueItem>() {

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
					if (o1.getStatus() == LocalDataQueueItem.Status.DOWNLOADING){
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
				
				if (o1.getStatus() == LocalDataQueueItem.Status.QUEUED || o1.getStatus() == LocalDataQueueItem.Status.REQUEUED){
					if (o2.getStatus() == LocalDataQueueItem.Status.QUEUED || o2.getStatus() == LocalDataQueueItem.Status.REQUEUED){
						return o1.getOrder().compareTo(o2.getOrder());
					}
					return -1;
				}
				if (o2.getStatus() == LocalDataQueueItem.Status.QUEUED || o2.getStatus() == LocalDataQueueItem.Status.REQUEUED){
					if (o1.getStatus() == LocalDataQueueItem.Status.QUEUED|| o1.getStatus() == LocalDataQueueItem.Status.REQUEUED){
						return o2.getOrder().compareTo(o1.getOrder());
					}
					return 1;
				}	
				return  o2.getDateProcessed().compareTo(o1.getDateProcessed());
				
			}
		});
	}
	private void createLocalSection(Composite parent){
		Section dataQueueSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
		dataQueueSection.setText(Messages.DataQueueView_LocalDataQueueLabel);
		dataQueueSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dataQueueSection.setLayout(new GridLayout());
	
		Composite main = toolkit.createComposite(dataQueueSection);
		main.setLayout(new GridLayout());
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		dataQueueSection.setClient(main);
		
		dataQueueTable = new DataQueueTable(main);
		dataQueueTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		

		Menu dataQueueMenu = new Menu(dataQueueTable.getViewer().getControl());
		dataQueueTable.getViewer().getTable().setMenu(dataQueueMenu);
		
		MenuItem menuDelete = new MenuItem(dataQueueMenu, SWT.NONE);
		menuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		menuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		menuDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteSelected();
			}
		});
		MenuItem menuReprocess = new MenuItem(dataQueueMenu, SWT.NONE);
		menuReprocess.setText(Messages.DataQueueView_ReprocessLabel);
		menuReprocess.setImage(ConnectDataQueuePlugin.getDefault().getImageRegistry().get(ConnectDataQueuePlugin.PROCESSING_ICON));
		menuReprocess.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				List<LocalDataQueueItem> items = getDataQueueSelection();
				if (!MessageDialog.openQuestion(shell, Messages.DataQueueView_ReprocessDialogTitle, 
						MessageFormat.format(Messages.DataQueueView_ReprocessDialogMessage, items.size()))){
					return;
				}
				DataQueueManager.INSTANCE.reprocessItems(items);
				ProcessorManager.INSTANCE.processDataQueue(connect);
			}
		});
		
		
		Composite linkComp = toolkit.createComposite(main);
		linkComp.setLayout(new GridLayout(5, false));
		((GridLayout)linkComp.getLayout()).marginHeight = 0;
		
		Hyperlink link = toolkit.createHyperlink(linkComp, Messages.DataQueueView_RefreshLabel, SWT.NONE);
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				refreshLocalTable();
			}
		});
		
		Hyperlink deleteSel = toolkit.createHyperlink(linkComp, Messages.DataQueueView_RemovedSelectedLabel, SWT.NONE);
		Hyperlink deleteAll = toolkit.createHyperlink(linkComp, Messages.DataQueueView_RemoveAllLabel , SWT.NONE);
		
		Hyperlink restart = toolkit.createHyperlink(linkComp, Messages.DataQueueView_RestartLabel , SWT.NONE);
		restart.setToolTipText(Messages.DataQueueView_RestartTooltip);
		restart.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				ProcessorManager.INSTANCE.processDataQueue(connect);		
			}
		});
		
		
		Label spacer = toolkit.createLabel(linkComp, ""); //$NON-NLS-1$
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
			deleteSel.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				deleteSelected();
			}
		});
		deleteAll.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				deleteAll();
				refreshLocalTable();
			}
		});
	}
	
	private void createServerSection(Composite parent){
		Section dataQueueSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED );
		dataQueueSection.setText(Messages.DataQueueView_ServerQueueSectionLabel);
		dataQueueSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dataQueueSection.setLayout(new GridLayout());
	
		
		Composite main = toolkit.createComposite(dataQueueSection);
		main.setLayout(new GridLayout());
		dataQueueSection.setClient(main);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite buttonComp = toolkit.createComposite(main, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		buttonComp.setLayout(gl);
		Button btn = toolkit.createButton(buttonComp, Messages.DataQueueView_ProcessAllLabel, SWT.PUSH);
		btn.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				processAll();
			}
		});
		Button btn2 = toolkit.createButton(buttonComp, Messages.DataQueueView_ProcessCheckedLabel, SWT.PUSH);
		btn2.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				processSelected();
			}
		});
		Hyperlink link = toolkit.createHyperlink(main, Messages.DataQueueView_RefreshLabel, SWT.NONE);
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				refreshServerTable();
			}
		});
		tblServer = CheckboxTableViewer.newCheckList(main, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		tblServer.getTable().setLinesVisible(true);
		tblServer.getTable().setHeaderVisible(true);
		tblServer.setContentProvider(ArrayContentProvider.getInstance());
		tblServer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		for (final ServerColumn c : ServerColumn.values()){
			TableViewerColumn tc = new TableViewerColumn(tblServer, SWT.DEFAULT);
			tc.getColumn().setWidth(c.width);
			tc.getColumn().setText(c.guiName);
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
	}
	
	private List<LocalDataQueueItem> getDataQueueSelection(){
		List<LocalDataQueueItem> items = new ArrayList<LocalDataQueueItem>();
		IStructuredSelection sel = (IStructuredSelection) dataQueueTable.getSelection();
		for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
			Object item = iterator.next();
			if (item instanceof LocalDataQueueItem){
				items.add((LocalDataQueueItem)item);
			}
		}
		return items;
		
	}
	private void deleteSelected(){
		List<LocalDataQueueItem> items = getDataQueueSelection();
		boolean isProcessing = false;
		for (Iterator<LocalDataQueueItem> iterator = items.iterator(); iterator.hasNext();) {
			LocalDataQueueItem localDataQueueItem = (LocalDataQueueItem) iterator.next();
			if (localDataQueueItem.getStatus() == LocalDataQueueItem.Status.PROCESSING || 
					localDataQueueItem.getStatus() == LocalDataQueueItem.Status.DOWNLOADING ){
					//cannot delete processing or downloading items
					isProcessing = true;
					iterator.remove();
			}
		}

		if (items.isEmpty()){
			if (isProcessing){
				MessageDialog.openWarning(shell,Messages.DataQueueView_DeleteItemsTitle, Messages.DataQueueView_DeleteItemsInfoMessage);
			}
			return;
		}
		
		String message = MessageFormat.format(Messages.DataQueueView_DeleteItemsConfirmMessage, items.size());
		if (isProcessing){
			message = MessageFormat.format(Messages.DataQueueView_DeleteItemsConfirmMessage2, items.size());
		}
			
		if (!MessageDialog.openQuestion(shell, Messages.DataQueueView_DeleteItemsTitle, message)){
			return;
		}

		DataQueueManager.INSTANCE.deleteItems(items);
	}

	private void deleteAll(){
		if (!MessageDialog.openQuestion(shell, Messages.DataQueueView_ClearQueueTitle, Messages.DataQueueView_ClearQueueMessage + "\n\n" + Messages.DataQueueView_ClearQueueMessage2)){ //$NON-NLS-1$
			return;
		}
		DataQueueManager.INSTANCE.deleteAllHistory();
	}
	

	private synchronized void refreshServerTable(){
		tblServer.setInput(new String[]{Messages.DataQueueView_LoadingLabel});
		tblServer.refresh();
		refreshServerItemsJob.schedule();
	}
	
	private synchronized void refreshLocalTable(){
		if (shell == null || shell.isDisposed()) return;
		dataQueueTable.setInput(new String[]{Messages.DataQueueView_LoadingLabel});
		refreshLocalJob.schedule();
	}
	
	private void buildLocalErrorContent(String message){
		MessageDialog.openWarning(shell,  Messages.DataQueueView_ErrorDialogTitle, message);	
	}
	
	private void buildLocalContent(List<LocalDataQueueItem> items){
		dataQueueTable.setInput(items);
	}
	
	@SuppressWarnings("unchecked")
	private void processAll(){
		if (!(tblServer.getInput() instanceof List)) return;
		List<DataQueueItem> items = (List<DataQueueItem>) tblServer.getInput();
		if (items.isEmpty()){
			MessageDialog.openInformation(shell, Messages.DataQueueView_ProcessorDialogTitle, Messages.DataQueueView_ProcessorDialogMessage);
			return;
		}
		processItems(items);
	}
	
	private void processSelected(){
		List<DataQueueItem> items = new ArrayList<DataQueueItem>();
		for (Object x : tblServer.getCheckedElements()){
			if (x instanceof DataQueueItem){
				items.add((DataQueueItem)x);
			}
		}
		if (items.isEmpty()) return;
		processItems(items);
	}
	
	private void processItems(List<DataQueueItem> items){
		for (DataQueueItem i : items){
			try{
				DataQueueManager.INSTANCE.addItemToQueue(i);
			}catch (Exception ex){
				ConnectDataQueuePlugin.displayLog(Messages.DataQueueView_AddItemError, ex);
			}
		}
		ProcessorManager.INSTANCE.processDataQueue(connect);
		refreshLocalTable();
		refreshServerTable();
	}
	
	@Focus
	public void setFocus() {
		tblServer.getTable().setFocus();
	}

	public static class DataQueueViewWrapper extends DIViewPart<DataQueueView>{
		public DataQueueViewWrapper(){
			super(DataQueueView.class);
		}
	}
}