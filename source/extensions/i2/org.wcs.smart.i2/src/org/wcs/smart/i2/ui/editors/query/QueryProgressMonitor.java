/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.editors.query;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;

/**
 * Progress monitor for query results; displaying the progress on
 * the provided progress panel. 
 * 
 * @author Emily
 *
 */
public class QueryProgressMonitor implements IProgressMonitor {

	private ProgressPanel panel;
	
	private String taskName;
	private String subTaskName;
	
	private int totalWork = 0;
	private double worked = 0;
	
	private boolean cancel = false;
	
	public QueryProgressMonitor(ProgressPanel panel){
		this.panel = panel;
		Display.getDefault().syncExec(()->{
			panel.clear();
			panel.setProgressMonitor(QueryProgressMonitor.this);
		});
	}

	public void updateGui(){
		Display.getDefault().asyncExec(()->{
			panel.setLabel(taskName + " - " + subTaskName); //$NON-NLS-1$
			int progress = (int) Math.round( (worked / totalWork) * 100.0 ) ;
			panel.setProgress(progress);
			panel.layout(true);
		});
	}
	
	@Override
	public void beginTask(String name, int totalWork) {
		this.taskName = name;
		this.totalWork = totalWork;
		this.worked = 0;
		updateGui();
	}

	@Override
	public void done() {
		Display.getDefault().asyncExec(()->{
			panel.done();
		});
	}

	@Override
	public void internalWorked(double work) {
		worked += work;
		updateGui();
	}

	@Override
	public boolean isCanceled() {
		return cancel;
	}

	@Override
	public void setCanceled(boolean value) {
		this.cancel = value;
	}

	@Override
	public void setTaskName(String name) {
		taskName = name;
		updateGui();
	}

	@Override
	public void subTask(String name) {
		subTaskName = name;
		updateGui();
	}

	@Override
	public void worked(int work) {
		internalWorked(work);
	}

}
