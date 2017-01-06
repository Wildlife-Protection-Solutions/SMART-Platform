package org.wcs.smart.i2.ui.editors.query;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

public class QueryProgressMonitor implements IProgressMonitor {

	private ProgressBar pbar;
	private Label progressLabel;
	
	private String taskName;
	private int totalWork = 0;
	private int worked = 0;
	
	private boolean cancel = false;
	
	public QueryProgressMonitor(ProgressBar pbar, Label progressLabel){
		this.pbar = pbar;
		this.progressLabel = progressLabel;
		Display.getDefault().syncExec(()->{
			pbar.setMinimum(0);
			pbar.setMaximum(100);
		});
	}

	public void updateGui(){
		Display.getDefault().asyncExec(()->{
			progressLabel.setText(taskName);
			
			int progress = Math.round(worked / (float)totalWork);
			pbar.setSelection(progress);
			
			progressLabel.getParent().layout(true);
		});
	}
	
	@Override
	public void beginTask(String name, int totalWork) {
		this.taskName = name;
		this.totalWork = totalWork;
	}

	@Override
	public void done() {
		pbar.setSelection(pbar.getMaximum());
		progressLabel.setText("");
	}

	@Override
	public void internalWorked(double work) {
		worked += work;

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
	}

	@Override
	public void subTask(String name) {
		taskName = taskName + " -  " + name;
		// TODO Auto-generated method stub

	}

	@Override
	public void worked(int work) {
		internalWorked(work);
	}

}
