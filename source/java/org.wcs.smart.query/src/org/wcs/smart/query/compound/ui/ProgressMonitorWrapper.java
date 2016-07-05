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
package org.wcs.smart.query.compound.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;

/**
 * Progress monitor wrapper that updates a progress
 * bar as work is completed.
 * 
 * @author Emily
 *
 */
public class ProgressMonitorWrapper implements IProgressMonitor {

	private IProgressMonitor wrapper;
	private ProgressBar pbar;
	
	private int worked = 0;
	
	/**
	 * Creates a new wrapper
	 * @param wrapped progress monitor to wrap
	 * @param pbar progress bar to update
	 */
	public ProgressMonitorWrapper(IProgressMonitor wrapped, ProgressBar pbar){
		this.wrapper = wrapped;
		this.pbar = pbar;
	}
	
	@Override
	public void beginTask(String name, int totalWork) {
		wrapper.beginTask(name, totalWork);
		Display.getDefault().asyncExec(new Runnable(){

			@Override
			public void run() {
				if(pbar.isDisposed()) return;
				pbar.setMaximum(totalWork);		
			}
			
		});
		
		worked = 0;
	}

	@Override
	public void done() {
		Display.getDefault().asyncExec(new Runnable(){
			@Override
			public void run() {
				if (pbar.isDisposed()) return;
				pbar.setSelection(pbar.getMaximum());		
			}
		});
	}

	@Override
	public void internalWorked(double work) {
		wrapper.internalWorked(work);
	}

	@Override
	public boolean isCanceled() {
		return wrapper.isCanceled();
	}

	@Override
	public void setCanceled(boolean value) {
		wrapper.setCanceled(value);
	}

	@Override
	public void setTaskName(String name) {
		wrapper.setTaskName(name);
	}

	@Override
	public void subTask(String name) {
		wrapper.subTask(name);
	}

	@Override
	public void worked(int work) {
		wrapper.worked(work);
		worked += work;
		
		Display.getDefault().asyncExec(new Runnable(){
			@Override
			public void run() {
				if (pbar.isDisposed()) return;
				pbar.setSelection(worked);		
			}
		});
	}
}
