package org.wcs.smart.connect.dataqueue.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;

public class ProgressWidget extends Composite{

	private ProgressBar pbar; 
//	private Label lblStatus, lblProgress, lbl, lblItemStatus;
	private Label lblProgress, lblItemStatus, lbl, lblStatusImage;
//	private Composite progressSection;
	
	public ProgressWidget(Composite parent, Label sLabel, Label lblStatusImage) {
		super(parent, SWT.NONE);
		createControl();
		this.lblItemStatus = sLabel;
		this.lblStatusImage = lblStatusImage;
		
	}
	
	public void setBackground(Color color){
		super.setBackground(color);
//		Control[] controls = new Control[]{lblStatus, lblProgress, lblItemStatus, lbl, pbar, progressSection};
		Control[] controls = new Control[]{lbl, lblProgress, pbar};
		for (Control c : controls){
			if (!c.isDisposed()) c.setBackground(color);
		}
	}
	private void createControl(){
		setLayout(new GridLayout(3, false));
		((GridLayout)getLayout()).marginHeight = 0;
		((GridLayout)getLayout()).marginWidth = 0;
		
		lblProgress = new Label(this, SWT.NONE);
		lblProgress.setText("Progress:");
		lblProgress.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		pbar = new ProgressBar(this, SWT.SMOOTH | SWT.HORIZONTAL);
		lbl = new Label(this, SWT.NONE);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	}

	private void hideProgress(){
		((GridData)getLayoutData()).heightHint = 0;
		getParent().getParent().getParent().layout(true , true);
	}
	
	public void initStatus(LocalDataQueueItem.Status status){
		lblItemStatus.setText(status.name());
		updateImage(status);
		if (status == LocalDataQueueItem.Status.COMPLETE ||
				status == LocalDataQueueItem.Status.ERROR){
			hideProgress();
		}
	}
	
	private void updateImage(LocalDataQueueItem.Status status){
		Image x = status.getImage();
		if (x != null){
			lblStatusImage.setImage(x);
		}
	}
	public void setProgressDone(LocalDataQueueItem.Status status){
		lblItemStatus.setText(status.name());
		hideProgress();
		updateImage(status);
		
	}
	
	public void setProgressCancel(LocalDataQueueItem.Status status){
		lblItemStatus.setText(status.name());
		lbl.setText("CANCELLED.");
		updateImage(status);
	}
	
	public void setProgress(LocalDataQueueItem.Status status, String taskName, String subTask, int totalWork, int initWork){
		updateImage(status);
		lblItemStatus.setText(status.name());
		pbar.setMinimum(0);
		pbar.setMaximum(totalWork);
		pbar.setSelection(initWork);
		StringBuilder sb = new StringBuilder();
		if (taskName != null){
			sb.append(taskName);
		}
		if (subTask != null){
			sb.append(": " + subTask);
		}
		lbl.setText(sb.toString());
		
		getParent().layout(true);
	}
	
}
