package org.wcs.smart.i2.ui.editors.query;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

public class ProgressPanel extends Composite {

	private ProgressBar pbar;
	private Label progressLabel;
	
	public ProgressPanel(Composite parent) {
		super(parent, SWT.NONE);
		
		setLayout(new GridLayout());
		
		pbar = new ProgressBar(this, SWT.HORIZONTAL | SWT.SMOOTH);
		pbar.setMinimum(0);
		pbar.setMaximum(100);
		pbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		progressLabel = new Label(this, SWT.NONE);
		progressLabel.setText("");
		progressLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	}

	public void setLabel(String label){
		this.progressLabel.setText(label);
	}
	
	/**
	 * 
	 * @param value value between 0 and 100 representing the progress
	 */
	public void setProgress(int value){
		pbar.setSelection(value);
	}
	
	/**
	 * clears message and sets progres to 0
	 */
	public void clear(){
		setProgress(0);
		setLabel("");
		layout(true);
	}
	
	/**
	 * clears messages and set progress to 100%
	 */
	public void done(){
		setProgress(pbar.getMaximum());
		setLabel("");
		layout(true);
	}
}
