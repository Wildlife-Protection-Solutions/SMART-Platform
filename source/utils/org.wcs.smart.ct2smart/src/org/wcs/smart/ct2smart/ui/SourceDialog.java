package org.wcs.smart.ct2smart.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public class SourceDialog extends Composite {

	private XmlFileComposite xmlNewRaw;
	private XmlFileComposite xmlNewDatamodel;
	
	public SourceDialog(Composite c) {
		super(c, SWT.NONE);
		
		GridLayout layout = new GridLayout(1, false);
		this.setLayout(layout);

		GridData gridData = new GridData(SWT.FILL,SWT.FILL, true, true);
		this.setLayoutData(gridData);

		this.setSize(840, 640);

		Group fullGroup = new Group(this, SWT.NONE);
		fullGroup.setText("New matching session");
		fullGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		fullGroup.setLayout(new GridLayout(1, false));

		xmlNewRaw = new XmlFileComposite(fullGroup);
		xmlNewRaw.setLabelText("CyberTracker Raw XML: ");

		xmlNewDatamodel = new XmlFileComposite(fullGroup);
		xmlNewDatamodel.setLabelText("SMART Datamodel XML:");
		
		Button btnStart = new Button(fullGroup, SWT.PUSH);
		btnStart.setText("Start Session");
		btnStart.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
		btnStart.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
				super.widgetSelected(arg0);
			}
		});
		
		Group resumeGroup = new Group(this, SWT.NONE);
		resumeGroup.setText("Previous matching session");
		resumeGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		resumeGroup.setLayout(new GridLayout(1, false));

		XmlFileComposite xmlRaw = new XmlFileComposite(resumeGroup);
		xmlRaw.setLabelText("Data Matching XML: ");

		XmlFileComposite xmlDatamodel = new XmlFileComposite(resumeGroup);
		xmlDatamodel.setLabelText("SMART Datamodel XML:");
		
		Button btnResume = new Button(resumeGroup, SWT.PUSH);
		btnResume.setText("Resume Session");
		btnResume.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
		btnResume.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
				super.widgetSelected(arg0);
			}
		});
		
	}	
}
