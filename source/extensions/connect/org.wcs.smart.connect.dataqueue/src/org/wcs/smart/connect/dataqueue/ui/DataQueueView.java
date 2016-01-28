package org.wcs.smart.connect.dataqueue.ui;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.dataqueue.ConnectDataQueue;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.hibernate.SmartDB;

public class DataQueueView{ 
	
	public static final String ID = "org.wcs.smart.connect.dataqueue.queueview"; //$NON-NLS-1$
	
	private FormToolkit toolkit = null;
	
	private TableViewer tblProcessing;

	private SmartConnect connect;
	
	@Inject private UISynchronize ui;
	
	private enum Column{
		NAME("Name"),
		TYPE("Type"),
		STATUS("Status");
		
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
			}
			return "";
		}
	}

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
		toolkit = new FormToolkit(parent.getDisplay());
	
		Section dataQueueSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
		dataQueueSection.setText("Data Queue");
		dataQueueSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dataQueueSection.setLayout(new GridLayout());
		
		Composite main = toolkit.createComposite(dataQueueSection);
		main.setLayout(new GridLayout());
		dataQueueSection.setClient(main);
		
		tblProcessing = new TableViewer(main);
		tblProcessing.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblProcessing.setContentProvider(ArrayContentProvider.getInstance());
		
		for (final Column c : Column.values()){
			TableViewerColumn column = new TableViewerColumn(tblProcessing, SWT.LEFT);
			column.setLabelProvider(new ColumnLabelProvider(){
				@Override
				public String getText(Object element){
					if (element instanceof DataQueueItem){
						return c.getLabel((DataQueueItem)element);
					}
					return super.getText(element);
				}
			});
		}
		
		Button btnTest = new Button(main, SWT.PUSH);
		btnTest.setText("TEST");
		btnTest.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshTable();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		
		refreshTable();
	}

	private void refreshTable(){
		if (tblProcessing == null || tblProcessing.getTable().isDisposed()) return;
		if(connect == null){
			ConnectDialog cd = new ConnectDialog(tblProcessing.getTable().getShell());
			if (cd.open() != Window.OK){
				return;
			}
			connect = cd.getConnection();
		}
		
		Job j = new Job("refresh data queue table"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try{
					List<DataQueueItem> items = ConnectDataQueue.INSTANCE.getQueuedItems(connect, SmartDB.getCurrentConservationArea());
					ui.syncExec(new Runnable() {
						
						@Override
						public void run() {
							if (tblProcessing.getTable().isDisposed()) return;
							tblProcessing.setInput(items);
						}
					});
				}catch (Exception ex){
					ConnectPlugIn.displayLog("Error loading items from server", ex);
				}
				return Status.OK_STATUS;
			}
			
		};
		j.schedule();
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