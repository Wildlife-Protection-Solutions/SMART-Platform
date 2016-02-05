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
package org.wcs.smart.connect.dataqueue.internal.process;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;

/**
 * Data queue monitor for monitoring the state of data queue processing. The application
 * has a single monitor.  
 * 
 * @author Emily
 *
 */
public class DataQueueProcessMonitor implements IProgressMonitor{

	private int total;
	private int work;
	private String taskName;
	private String subTaskName;
	private double internalWork;
	
	private IProgressMonitor wrapper;
	private boolean done = false;
	private LocalDataQueueItem item;

	
	DataQueueProcessMonitor(){
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
		ProcessorManager.INSTANCE.progressUpdated(item, taskName, subTaskName, total, work);
	}
	
	private void fireDone(){
		if (item == null) return;
		ProcessorManager.INSTANCE.done(item);
	}
	
	private void fireCancelled(){
		if (item == null) return;
		ProcessorManager.INSTANCE.cancel(item);
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
