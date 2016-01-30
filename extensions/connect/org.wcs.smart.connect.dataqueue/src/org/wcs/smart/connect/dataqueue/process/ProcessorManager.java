package org.wcs.smart.connect.dataqueue.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.dataqueue.ui.ProgressWidget;


public enum ProcessorManager {
	
	INSTANCE;
	
	
	private List<DataQueueItemProcessor> jobs = new ArrayList<DataQueueItemProcessor>();
	
	public void startProcessing(SmartConnect connect){
		ProgressMonitorWatcher watcher = new ProgressMonitorWatcher(new IProgressMonitorListener() {
			
			@Override
			public void progressUpdated(final LocalDataQueueItem item, String taskName, String subTask, int totalWork,
					int currentWork) {
				Display.getDefault().asyncExec(new Runnable(){
					@Override
					public void run() {
						ProgressWidget w = getProgressMonitor(item);
						if (w == null || w.isDisposed()) return;
						w.setProgress(item.getStatus(), taskName, subTask, totalWork, currentWork);		
					}});
				
			}

			@Override
			public void done(final LocalDataQueueItem item) {
				
				Display.getDefault().asyncExec(new Runnable(){
					@Override
					public void run() {
						ProgressWidget w = getProgressMonitor(item);
						if (w == null || w.isDisposed()) return;
						w.setProgressDone(item.getStatus());		
					}});
				
			}

			@Override
			public void cancel(final LocalDataQueueItem item) {
				
				Display.getDefault().asyncExec(new Runnable(){
					@Override
					public void run() {
						ProgressWidget w = getProgressMonitor(item);
						if (w == null || w.isDisposed()) return;
						w.setProgressCancel(item.getStatus() );		
					}});
				
			}
		});
		
		
		DataQueueItemProcessor job = new DataQueueItemProcessor(connect, watcher);
		jobs.add(job);
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				jobs.remove(job);
			}
		});
		job.schedule();
	}
	
	private HashMap<UUID, ProgressWidget> widgets = new HashMap<UUID, ProgressWidget>();
	
	/**
	 * Registers a widget for displaying progress.  Widgets are automatically
	 * deregistered when disposed.
	 * 
	 * @param newwidgets progress widget
	 * @param item item represented by progress widget
	 */
	public void register(final ProgressWidget newwidgets, DataQueueItem item){
		newwidgets.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				widgets.remove(item.getUuid());
			}
		});
		this.widgets.put(item.getUuid(), newwidgets);
		
		for(DataQueueItemProcessor job : jobs){
			job.getWatcher().fireStatusUpdate();
		}
	}
	
	/**
	 * Updates the progress for a given item to the current state.
	 * 
	 * @param item
	 * @param currentState
	 */
	public ProgressWidget getProgressMonitor(DataQueueItem item){
		ProgressWidget w = widgets.get(item.getUuid());
		return w;
	}
	
}
