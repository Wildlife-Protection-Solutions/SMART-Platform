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

import java.util.HashMap;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.connect.dataqueue.internal.ui.TableProgressWidget;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;

/**
 * Data queue monitor for monitoring the state of data queue processing. The application
 * has a single monitor.  
 * 
 * @author Emily
 *
 */
public class DataQueueProcessMonitor implements IProgressMonitor{

	public static final DataQueueProcessMonitor INSTANCE = new DataQueueProcessMonitor();
	
	private int total;
	private int work;
	private String taskName;
	private String subTaskName;
	private double internalWork;
	
	private IProgressMonitor wrapper;
	private boolean done = false;
	private LocalDataQueueItem item;

	private HashMap<UUID, TableProgressWidget> widgets = new HashMap<UUID, TableProgressWidget>();
	
	private DataQueueProcessMonitor(){
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
		progressUpdated(item, taskName, subTaskName, total, work);
	}
	
	private void fireDone(){
		if (item == null) return;
		done(item);
	}
	
	private void fireCancelled(){
		if (item == null) return;
		cancel(item);
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
	
	/**
	 * Registers a widget for displaying progress.  Widgets are automatically
	 * deregistered when disposed.
	 * 
	 * @param newwidgets progress widget
	 * @param item item represented by progress widget
	 */
	public void register(final TableProgressWidget newwidgets, DataQueueItem item){
		newwidgets.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				widgets.remove(item.getUuid());
			}
		});
		this.widgets.put(item.getUuid(), newwidgets);
		fireStatusUpdate();
	}
	
	/**
	 * Updates the progress for a given item to the current state.
	 * 
	 * @param item
	 * @param currentState
	 */
	private TableProgressWidget getProgressWidget(DataQueueItem item){
		TableProgressWidget w = widgets.get(item.getUuid());
		return w;
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
