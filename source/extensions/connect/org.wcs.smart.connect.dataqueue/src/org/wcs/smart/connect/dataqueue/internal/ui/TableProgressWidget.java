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

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;

/**
 * Widget to display progress inside a table viewer cell
 * @author Emily
 *
 */
public class TableProgressWidget extends Composite{

	private ProgressBar pbar; 
	private Label lblProgress;

	private LocalDataQueueItem item;
	private TableViewer viewer;
	
	
	
	public TableProgressWidget(Composite parent, LocalDataQueueItem item, TableViewer viewer) {
		super(parent, SWT.TRANSPARENT);
		this.item = item;
		this.viewer = viewer;
		createControl();
	}
	
	public void setBackground(Color color){
		Control[] controls = new Control[]{lblProgress, pbar};
		for (Control c : controls){
			if (!c.isDisposed()) c.setBackground(color);
		}
	}
	private void createControl(){
		setLayout(new GridLayout(2, false));
		((GridLayout)getLayout()).marginHeight = 2;
		((GridLayout)getLayout()).marginWidth = 0;
		
		pbar = new ProgressBar(this, SWT.SMOOTH | SWT.HORIZONTAL);
		GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		gd.widthHint = 100;
		pbar.setLayoutData(gd);
		
		lblProgress = new Label(this,  SWT.NONE);
		lblProgress.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		initStatus();
	}

	private void hideProgress(){
		pbar.dispose();
		layout(true);
	}
	
	public void initStatus(){
		lblProgress.setText(item.getStatus().name());
		if (item.getStatus() == LocalDataQueueItem.Status.COMPLETE ||
			item.getStatus() == LocalDataQueueItem.Status.COMPLETE_WARN ||
			item.getStatus() == LocalDataQueueItem.Status.ERROR){
			hideProgress();
		}
		layout(true);
	}
	
	public void setProgressDone(LocalDataQueueItem update){
		
		this.item.setStatus(update.getStatus());
		this.item.setErrorMessage(update.getErrorMessage());
		this.item.setDateProcessed(update.getDateProcessed());
		
		hideProgress();
		lblProgress.setText(this.item.getStatus().name());
		viewer.refresh(item);
		layout(true);
	}
	
	public void setProgressCancel(LocalDataQueueItem update){
		setProgressDone(update);
	}
	
	public void setProgress(LocalDataQueueItem update, String taskName, String subTask, int totalWork, int initWork){
		this.item.setStatus(update.getStatus());
		this.item.setErrorMessage(update.getErrorMessage());
		this.item.setDateProcessed(update.getDateProcessed());
		viewer.refresh(item);
		if (pbar != null && !pbar.isDisposed()){
			pbar.setMinimum(0);
			pbar.setMaximum(totalWork);
			pbar.setSelection(initWork);
		}
		StringBuilder sb = new StringBuilder();
		if (taskName != null){
			sb.append(taskName);
		}
		if (subTask != null){
			sb.append(": " + subTask); //$NON-NLS-1$
		}
		lblProgress.setText(sb.toString());
		
		getParent().layout(true);
		layout(true);
	}
	
}
