package org.wcs.smart.connect.dataqueue.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;

public class DataQueueProgressMonitor implements IProgressMonitor{

	private DataQueueItem item;
	
	public void setDataQueueItem(DataQueueItem item){
		this.item = item;
		
		IJobManager manager = Job.getJobManager();
		manager.addJobChangeListener(new IJobChangeListener() {
			
			@Override
			public void sleeping(IJobChangeEvent event) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void scheduled(IJobChangeEvent event) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void running(IJobChangeEvent event) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void done(IJobChangeEvent event) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void awake(IJobChangeEvent event) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void aboutToRun(IJobChangeEvent event) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	
	@Override
	public void beginTask(String name, int totalWork) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void done() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void internalWorked(double work) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isCanceled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setCanceled(boolean value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTaskName(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void subTask(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void worked(int work) {
		// TODO Auto-generated method stub
		
	}

	
}
