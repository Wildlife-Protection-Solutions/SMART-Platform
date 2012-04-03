package org.wcs.smart.query.ui.formulaDnd;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class ProxyItem extends Composite {

	private Label lbl = null;
	public ProxyItem(Composite parent) {
		super(parent, SWT.NONE);
		super.setLayout(new GridLayout(1, false));
		GridData gd = new GridData();
		gd.horizontalIndent = 5;
		gd.verticalIndent = 0;
		
		super.setData(gd);
		
		Composite inner = new Composite(this, SWT.BORDER);
		inner.setLayout(new GridLayout(1, false));
		lbl = new Label(inner, SWT.NONE);
		lbl.setText("");
		lbl.setVisible(false);
	}
	
	public void setLabelText(String text){
		this.lbl.setText(text);
	}
}
