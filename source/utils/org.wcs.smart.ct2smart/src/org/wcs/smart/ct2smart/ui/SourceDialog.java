package org.wcs.smart.ct2smart.ui;

import java.sql.Connection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ct2smart.dao.ConnectionUtil;
import org.wcs.smart.ct2smart.db.CsvDbLoader;
import org.wcs.smart.ct2smart.matcher.CsvMatchFileBuilder;
import org.wcs.smart.ct2smart.matcher.FileUtil;
import org.wcs.smart.ct2smart.matcher.model.Ct2Smart;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class SourceDialog extends Composite {

	private XmlFileComposite xmlNewRaw;
	private XmlFileComposite xmlNewDatamodel;
	
	private XmlFileComposite xmlResumeMapping;
	private XmlFileComposite xmlResumeDatamodel;
	
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
				startNewSession();
			}
		});
		
		Group resumeGroup = new Group(this, SWT.NONE);
		resumeGroup.setText("Previous matching session");
		resumeGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		resumeGroup.setLayout(new GridLayout(1, false));

		xmlResumeMapping = new XmlFileComposite(resumeGroup);
		xmlResumeMapping.setLabelText("Data Matching XML: ");

		xmlResumeDatamodel = new XmlFileComposite(resumeGroup);
		xmlResumeDatamodel.setLabelText("SMART Datamodel XML:");
		
		Button btnResume = new Button(resumeGroup, SWT.PUSH);
		btnResume.setText("Resume Session");
		btnResume.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
		btnResume.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				resumeSession();
			}
		});

		//TODO: remove after testing is complete
		xmlResumeMapping.setFileName("d:\\dev\\data\\mist\\match_super_x.xml");
		xmlResumeDatamodel.setFileName("d:\\dev\\data\\mist\\datamodel.xml");
	}

	protected void startNewSession() {
		try {
			Connection c = ConnectionUtil.getConnection();
			
			CsvDbLoader.getInstance().load(xmlNewRaw.getFile(), c);

			CsvMatchFileBuilder matchBuilder = new CsvMatchFileBuilder();
			Ct2Smart ct2Smart = matchBuilder.create(c);
			
			MatchSession session = new MatchSession();
			session.setCt2Smart(ct2Smart);
			session.setDataModel(FileUtil.loadDataModel(xmlNewDatamodel.getFile()));
			
			launch(session);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	

	protected void resumeSession() {
		MatchSession session = new MatchSession();
		try {
			session.setConnection(ConnectionUtil.getConnection());
			session.setCt2Smart(FileUtil.loadCt2Smart(xmlResumeMapping.getFile()));
			session.setDataModel(FileUtil.loadDataModel(xmlResumeDatamodel.getFile()));
			launch(session);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void launch(MatchSession session) {
		Shell shell = getShell();
		Display display = shell.getDisplay();
		shell.dispose();
		shell = new Shell(display);
		shell.setText("CyberTracker to SMART - Data Model Matcher");

		GridLayout layout = new GridLayout(1, false);
		shell.setLayout(layout);

		GridData gridData = new GridData(SWT.FILL,SWT.FILL, true, true);
		shell.setLayoutData(gridData);

		shell.setMinimumSize(840, 640);
		
		new DmMatcherDialog(shell, session);

		shell.pack();
		shell.open();
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose();
		
	}
}
