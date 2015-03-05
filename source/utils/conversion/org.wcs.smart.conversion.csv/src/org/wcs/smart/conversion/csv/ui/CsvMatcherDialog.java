/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.conversion.csv.ui;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.text.MessageFormat;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.conversion.csv.tool.Csv2DbLoader;
import org.wcs.smart.conversion.csv.tool.MappingValidator;
import org.wcs.smart.conversion.csv.tool.MatchFileBuilder;
import org.wcs.smart.conversion.lookup.DataModelLookup;
import org.wcs.smart.conversion.model.SmartMapping;
import org.wcs.smart.conversion.tool.MatchSession;
import org.wcs.smart.conversion.ui.ReportDialog;
import org.wcs.smart.conversion.ui.XmlFileComposite;
import org.wcs.smart.conversion.util.ConnectionUtil;
import org.wcs.smart.conversion.util.FileUtil;

/**
 * CSV to SMART matching tool main dialog
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class CsvMatcherDialog extends Composite {
	
	private CsvFileComposite csvNewRaw;
	private XmlFileComposite xmlNewDatamodel;
	
	private XmlFileComposite xmlResumeMapping;

	public CsvMatcherDialog(Shell shell) {
		super(shell, SWT.NONE);
		
		GridLayout layout = new GridLayout(1, false);
		this.setLayout(layout);

		GridData gridData = new GridData(SWT.FILL,SWT.FILL, true, true);
		this.setLayoutData(gridData);

		this.setSize(840, 640);

		Group fullGroup = new Group(this, SWT.NONE);
		fullGroup.setText("New matching session");
		fullGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		fullGroup.setLayout(new GridLayout(1, false));

		csvNewRaw = new CsvFileComposite(fullGroup);
		csvNewRaw.setLabelText("CSV File: ");

		xmlNewDatamodel = new XmlFileComposite(fullGroup);
		xmlNewDatamodel.setLabelText("SMART Datamodel XML:");
		
		Button btnStart = new Button(fullGroup, SWT.PUSH);
		btnStart.setText("Load CSV");
		btnStart.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
		btnStart.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				loadCsv();
			}
		});

		Button btnMap = new Button(fullGroup, SWT.PUSH);
		btnMap.setText("Create mapping");
		btnMap.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
		btnMap.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				createMapping();
			}
		});
		
		Group resumeGroup = new Group(this, SWT.NONE);
		resumeGroup.setText("Previous matching session");
		resumeGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		resumeGroup.setLayout(new GridLayout(1, false));

		xmlResumeMapping = new XmlFileComposite(resumeGroup);
		xmlResumeMapping.setLabelText("Data Matching XML: ");

		Button btnValidate = new Button(resumeGroup, SWT.PUSH);
		btnValidate.setText("Validate mapping");
		btnValidate.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
		btnValidate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				validateMapping();
			}
		});

		Button btnResume = new Button(resumeGroup, SWT.PUSH);
		btnResume.setText("Resume");
		btnResume.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
		btnResume.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
//				validateMapping();
			}
		});
		
		//TODO: remove after testing is complete
		csvNewRaw.setFileName("e:\\SMART\\CT Conversion - Data From Rich\\csv2smart\\Afi_gorrila.CSV");
		xmlNewDatamodel.setFileName("e:\\SMART\\CT Conversion - Data From Rich\\V2_nigeria data model.xml");
		xmlResumeMapping.setFileName("e:\\SMART\\CT Conversion - Data From Rich\\csv2smart\\mapping4_in.xml");
	}

	protected void loadCsv() {
		try {
			Connection c = ConnectionUtil.getConnection();
			
			Csv2DbLoader loader = new Csv2DbLoader();
			loader.load(csvNewRaw.getFile(), c);

//			MatchFileBuilder matchBuilder = new MatchFileBuilder();
//			SmartMapping csv2Smart = matchBuilder.create(c);
//			
//			MatchSession session = new MatchSession();
//			session.setCt2Smart(ct2Smart);
//			session.setDataModel(FileUtil.loadDataModel(xmlNewDatamodel.getFile()));
//			
//			launch(session);
			
			MessageDialog.openInformation(getShell(), "Info", "Data from CSV loaded successfully");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void createMapping() {
		try {
			Connection c = ConnectionUtil.getConnection();
			
			MatchFileBuilder matchBuilder = new MatchFileBuilder();
			SmartMapping csv2Smart = matchBuilder.create(c);
			
			FileDialog dlg = new FileDialog(getShell(), SWT.SAVE);
			dlg.setFilterNames(new String[] {"XML file"});
			dlg.setFilterExtensions(new String[] {"*.xml"}); //$NON-NLS-1$
			String fn = dlg.open();
			if (fn != null) {
				if (!fn.endsWith(".xml")) { //$NON-NLS-1$
					fn += ".xml"; //$NON-NLS-1$
				}
				try {
					FileUtil.write(new File(fn), csv2Smart);
				} catch (Exception e) {
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Error occured. See console or log for details.");
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void validateMapping() {
		MatchSession session = new MatchSession();
		try {
			session.setSmartMapping(FileUtil.loadSmartMapping(xmlResumeMapping.getFile()));
			session.setDataModel(FileUtil.loadDataModel(xmlNewDatamodel.getFile()));
			session.setConnection(ConnectionUtil.getConnection());
			
			DataModelLookup dmLookup = new DataModelLookup(session.getDataModel());
			MappingValidator validator = new MappingValidator();
			List<String> errors = validator.validate(session.getSmartMapping(), dmLookup);
			if (errors.isEmpty()) {
				MessageDialog.openInformation(getShell(), "Validation results", "No errors found.");
			} else {
				ReportDialog report = new ReportDialog(getShell(), "Validation results", MessageFormat.format("{0} errors found during validation:", errors.size()), errors);
				report.open();
			}
		} catch (JAXBException | IOException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Error occured. See console or log for details.");
			e.printStackTrace();
		}
	}

}
