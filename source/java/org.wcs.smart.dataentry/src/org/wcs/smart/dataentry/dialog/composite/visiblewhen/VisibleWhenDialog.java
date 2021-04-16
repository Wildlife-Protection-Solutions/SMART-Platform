package org.wcs.smart.dataentry.dialog.composite.visiblewhen;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.ui.SmartStyledTitleDialog;

public class VisibleWhenDialog extends SmartStyledTitleDialog {

	private CmNode node;
	private CmAttribute attribute;
	
	public VisibleWhenDialog(Shell parent, CmNode node, CmAttribute attribute) {
		super(parent);
		
		this.node = node;
		this.attribute = attribute;
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite)super.createDialogArea(parent);
		
		Composite main = new Composite(composite, SWT.NONE);
//		main.setLayout(new TableColumnLayout());
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		return composite; 
	}

}
