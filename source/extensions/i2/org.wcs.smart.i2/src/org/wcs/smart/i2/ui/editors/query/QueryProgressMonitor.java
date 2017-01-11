package org.wcs.smart.i2.ui.editors.query;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

public class QueryProgressMonitor implements IProgressMonitor {

	private ProgressPanel panel;
	
	private String taskName;
	private String subTaskName;
	
	private int totalWork = 0;
	private double worked = 0;
	
	private boolean cancel = false;
	
	public QueryProgressMonitor(ProgressPanel panel){
		this.panel = panel;
		Display.getDefault().asyncExec(()->panel.clear());
	}

	public void updateGui(){
		Display.getDefault().asyncExec(()->{
			panel.setLabel(taskName + " - " + subTaskName);
			int progress = (int) Math.round( (worked / totalWork) * 100.0 ) ;
			panel.setProgress(progress);
			panel.layout(true);
		});
	}
	
	@Override
	public void beginTask(String name, int totalWork) {
		this.taskName = name;
		this.totalWork = totalWork;
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
