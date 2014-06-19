package org.wcs.smart.ct2smart.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class SourceDialog extends Composite {

	public SourceDialog(Composite c) {
		super(c, SWT.NONE);
		
		GridLayout layout = new GridLayout(1, false);
		this.setLayout(layout);

		GridData gridData = new GridData(SWT.FILL,SWT.FILL, true, true);
		this.setLayoutData(gridData);

		this.setSize(840, 640);

		//main composite and layout
		final Composite main = new Composite(this, SWT.NONE);
		GridLayout mlayout = new GridLayout(2, true);
		main.setLayout(mlayout);
		GridData mainGridData = new GridData(SWT.FILL,SWT.FILL, true, true);
		main.setLayoutData(mainGridData);

		XmlFileComposite xml = new XmlFileComposite(main);
		
	}	
}
