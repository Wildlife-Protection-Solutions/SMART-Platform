package org.wcs.smart.query.ui.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;

public class ProgressMonitorWrapper implements IProgressMonitor {

	private IProgressMonitor wrapper;
	private ProgressBar pbar;
	
	private int worked = 0;
	
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
				pbar.setSelection(worked);		
			}
			
		});

	}

}
