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

import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.wcs.smart.connect.dataqueue.internal.Messages;
import org.wcs.smart.connect.dataqueue.internal.process.IDataQueueProgressListener;
import org.wcs.smart.connect.dataqueue.internal.process.ProcessorManager;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;

/**
 * Table widget for displaying local data queue items and associated progress.
 * @author Emily
 *
 */
public class DataQueueTable extends Composite{

	private HashMap<UUID, TableProgressWidget> widgets = new HashMap<UUID, TableProgressWidget>();
	

	private Map<Object, TableProgressWidget> progressWidgets = new HashMap<Object, TableProgressWidget>();
	 
	private TableViewer viewer;
	private IDataQueueProgressListener progressListener = new IDataQueueProgressListener() {
		@Override
		public void progressUpdated(LocalDataQueueItem item, String taskName,
				String subTask, int totalWork, int currentWork) {
			DataQueueTable.this.progressUpdated(item, taskName, subTask, totalWork, currentWork);
		}
		
		@Override
		public void done(LocalDataQueueItem item) {
			DataQueueTable.this.done(item);
		}
		
		@Override
		public void cancel(LocalDataQueueItem item) {
			DataQueueTable.this.cancel(item);
		}
	};
	
	private enum Column{
		STATUS(""), //$NON-NLS-1$
		NAME (Messages.DataQueueTable_NameLabel),
		TYPE(Messages.DataQueueTable_TypeLabel),
		PROGRESS(Messages.DataQueueTable_ProgressLabel),
		DATE(Messages.DataQueueTable_DateProcessedLabel),
		MESSAGE(Messages.DataQueueTable_MessagesLabel);
		
		public String guiName;
		
		Column(String name){
			this.guiName = name;
		}
		
		public String getValue(LocalDataQueueItem item){
			if (this == STATUS){
				return ""; //$NON-NLS-1$
			}else if (this == NAME){
				return item.getName();
			}else if (this == TYPE){
				return item.getType().name();
			}else if (this == PROGRESS){
				return item.getStatus().getGuiName();
			}else if (this == DATE){
				if (item.getDateProcessed() == null) return ""; //$NON-NLS-1$
				return DateFormat.getDateTimeInstance().format(item.getDateProcessed());
			}else if (this == MESSAGE){
				return item.getErrorMessage();
			}
			return ""; //$NON-NLS-1$
		}
	}
	
	
	public DataQueueTable(Composite parent) {
		super(parent, SWT.BORDER);
		createTable();
		
		ProcessorManager.INSTANCE.addListener(progressListener);
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				ProcessorManager.INSTANCE.removeListener(progressListener);
			}
		});
	}
	
	public void setInput(Object input){
		viewer.setInput(input);
	}
	
	public IStructuredSelection getSelection(){
		return (IStructuredSelection)viewer.getSelection();
	}
	
	private void createTable(){
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginHeight = 0;
		((GridLayout)getLayout()).marginWidth = 0;
		viewer = new TableViewer(this, SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(true);
		viewer.setContentProvider(new ArrayContentProvider(){
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				for (TableProgressWidget p : progressWidgets.values()){
					p.dispose();
				}
				progressWidgets.clear();
				widgets.clear();
		        super.inputChanged(viewer, oldInput, newInput);
		    }
		});
		ColumnViewerToolTipSupport.enableFor(viewer);
		for (final Column c: Column.values()){
	        TableColumn column = new TableColumn(viewer.getTable(), SWT.NONE);
	        column.setText(c.guiName);
	        column.setWidth(150);
	        
	        TableViewerColumn tvcolumn = new TableViewerColumn(viewer, column);
	        
	        if (c == Column.STATUS){
	        	tvcolumn.setLabelProvider(new ColumnLabelProvider(){
	        		@Override
	        		public String getText(Object element) {
	        			return ""; //$NON-NLS-1$
	        		}
	        		@Override
	        		public Image getImage(Object element) {
	        			if (element instanceof LocalDataQueueItem){
	        				return ((LocalDataQueueItem)element).getStatus().getImage();
	        			}
	        			return null;
	        		}
	        		@Override
	        		public String getToolTipText(Object element){
	        			if (element instanceof LocalDataQueueItem){
	        				return Column.MESSAGE.getValue((LocalDataQueueItem)element);
	        			}
	        			return super.getToolTipText(element);
	        		}
	        	});
	        	column.setWidth(30);
	        }else if (c != Column.PROGRESS){
	        	
	        	tvcolumn.setLabelProvider(new ColumnLabelProvider(){
	        		@Override
	        		public String getText(Object element) {
	        			if (element instanceof LocalDataQueueItem){
	        				return c.getValue((LocalDataQueueItem)element);
	        			}
	        			return super.getText(element);
	        		}
	        	});
	        	if ( c == Column.MESSAGE){
	        		tvcolumn.getColumn().setWidth(240);
	        	}
	        }else if (c == Column.PROGRESS){
	        	column.setWidth(150);
	        	tvcolumn.setLabelProvider(new ColumnLabelProvider(){
	                 //make sure you dispose these buttons when viewer input changes
	                 @Override
	                 public void update(ViewerCell cell) {
	                     TableItem item = (TableItem) cell.getItem();
	                     
	                     if (item.getData() instanceof LocalDataQueueItem){
		                     TableProgressWidget progressWidget;
		                     if(progressWidgets.containsKey(cell.getElement())){
		                    	 progressWidget = progressWidgets.get(cell.getElement());
		                     }else{
		                    	 LocalDataQueueItem i = (LocalDataQueueItem)item.getData();
		                    	 progressWidget = new TableProgressWidget((Composite)cell.getViewerRow().getControl(), i, viewer);
		                    	 progressWidget.setBackground(cell.getBackground());
		                    	 
		                    	 progressWidgets.put(cell.getElement(), progressWidget);
		                    	 widgets.put(i.getUuid(), progressWidget);
		                    	 
		                    	 TableEditor editor = new TableEditor(item.getParent());
			                     editor.grabHorizontal  = true;
			                     editor.grabVertical = true;
			                     editor.setEditor(progressWidget, item, cell.getColumnIndex());
			                     editor.layout();
		                     }
		                     
	                     }else{
	                    	 cell.setText((String)item.getData());
	                     }
	                 }

	             });
	        }
		}
	}
	
	public TableViewer getViewer(){
		return this.viewer;
	}
	
	private TableProgressWidget getProgressWidget(DataQueueItem item){
		return widgets.get(item.getUuid());
	}
	
	public void progressUpdated(final LocalDataQueueItem item, String taskName, String subTask, int totalWork,
			int currentWork) {
		Display.getDefault().asyncExec(new Runnable(){
			@Override
			public void run() {
				TableProgressWidget w = getProgressWidget(item);
				if (w == null || w.isDisposed()) return;
				w.setProgress(item, taskName, subTask, totalWork, currentWork);		
			}});
		
	}

	public void done(final LocalDataQueueItem item) {
		
		Display.getDefault().asyncExec(new Runnable(){
			@Override
			public void run() {
				TableProgressWidget w = getProgressWidget(item);
				if (w == null || w.isDisposed()) return;
				w.setProgressDone(item);		
			}});
		
	}

	public void cancel(final LocalDataQueueItem item) {
		
		Display.getDefault().asyncExec(new Runnable(){
			@Override
			public void run() {
				TableProgressWidget w = getProgressWidget(item);
				if (w == null || w.isDisposed()) return;
				w.setProgressCancel(item );		
			}});
		
	}
}
