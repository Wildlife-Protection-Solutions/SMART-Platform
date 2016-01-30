package org.wcs.smart.connect.dataqueue.process;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;

public class ProgressMonitorWatcher implements IProgressMonitor{

	private int total;
	private int work;
	private String taskName;
	private String subTaskName;
	private double internalWork;
	
	private IProgressMonitor wrapper;
	private boolean done = false;
	private IProgressMonitorListener listener;
	private LocalDataQueueItem item;
	
	public ProgressMonitorWatcher(IProgressMonitorListener listener){
		this.listener = listener;
	}
	
	public void setDataQueueItem(LocalDataQueueItem item){
		this.item = item;
	}
	
	public void fireStatusUpdate(){
		if (wrapper == null) return;
		if (wrapper.isCanceled()) fireCancelled();
		if (done) fireDone();
		fireUpdate();
	}
	private void fireUpdate(){
		if (item == null) return;
		listener.progressUpdated(item, taskName, subTaskName, total, work);
	}
	
	private void fireDone(){
		if (item == null) return;
		listener.done(item);
	}
	
	private void fireCancelled(){
		if (item == null) return;
		listener.cancel(item);
	}
	
	public IProgressMonitor setProgressMonitor(IProgressMonitor wrapper){
		this.wrapper = wrapper;
		return this;
	}
	
	@Override
	public void beginTask(String name, int totalWork) {
		wrapper.beginTask(name, totalWork);
		this.taskName = name;
		this.total = totalWork;
		fireUpdate();
	}

	@Override
	public void done() {
		this.done = true;
		wrapper.done();
		fireDone();
	}

	@Override
	public void internalWorked(double work) {
		wrapper.internalWorked(work);
		this.internalWork = this.internalWork + work;
		int update = (int)Math.floor(internalWork);
		
		this.work = this.work + update;
		this.internalWork = this.internalWork - update;
		
		fireUpdate();
	}

	@Override
	public boolean isCanceled() {
		return wrapper.isCanceled();
	}

	@Override
	public void setCanceled(boolean value) {
		wrapper.setCanceled(value);
		if (value) fireCancelled();
	}

	@Override
	public void setTaskName(String name) {
		wrapper.setTaskName(name);
		this.taskName = name;
		fireUpdate();
	}

	@Override
	public void subTask(String name) {
		wrapper.subTask(name);
		this.subTaskName = name;
		fireUpdate();
	}

	@Override
	public void worked(int work) {
		wrapper.worked(work);
		this.work = this.work + work;
		fireUpdate();
	}
		
}
