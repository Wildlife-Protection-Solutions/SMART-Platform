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
import java.sql.ResultSet;
import java.sql.SQLException;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.conversion.csv.tool.Csv2DbLoader;
import org.wcs.smart.conversion.csv.tool.CsvMetaExtractor;
import org.wcs.smart.conversion.csv.tool.CsvPatrolExtractor;
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
	
	private Label dbLabel;
	private int dbRecordsCount = 0;
	
	private XmlFileComposite xmlDatamodel;
	private XmlFileComposite xmlMapping;
	
	private Button btnGenMap;
	private Button btnValidateMap;
	private Button btnGenMeta;
	private Button btnGenPatrol;

	public CsvMatcherDialog(Shell shell) {
		super(shell, SWT.NONE);
		
		GridLayout layout = new GridLayout(1, false);
		this.setLayout(layout);

		GridData gridData = new GridData(SWT.FILL,SWT.FILL, true, true);
		this.setLayoutData(gridData);

		this.setSize(840, 640);

		Group dataGroup = new Group(this, SWT.NONE);
		dataGroup.setText("CSV data configuration");
		dataGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		dataGroup.setLayout(new GridLayout(1, false));
		
		dbLabel = new Label(dataGroup, SWT.NONE);
		
		Button btnLoadCsv = new Button(dataGroup, SWT.PUSH);
		btnLoadCsv.setText("Load data from CSV");
		btnLoadCsv.setLayoutData(new GridData(SWT.END, SWT.TOP, true, true));
		btnLoadCsv.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				loadCsv();
			}
		});

		btnGenMap = new Button(dataGroup, SWT.PUSH);
		btnGenMap.setText("Generate mapping template");
		btnGenMap.setLayoutData(new GridData(SWT.END, SWT.TOP, true, true));
		btnGenMap.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				createMapping();
			}
		});
		
		Group generateGroup = new Group(this, SWT.NONE);
		generateGroup.setText("Patrol generation");
		generateGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		generateGroup.setLayout(new GridLayout(1, false));

		xmlDatamodel = new XmlUpdateFileComposite(generateGroup);
		xmlDatamodel.setLabelText("SMART Datamodel XML:");
		
		xmlMapping = new XmlUpdateFileComposite(generateGroup);
		xmlMapping.setLabelText("Data Matching XML: ");

		btnValidateMap = new Button(generateGroup, SWT.PUSH);
		btnValidateMap.setText("Validate mapping");
		btnValidateMap.setLayoutData(new GridData(SWT.END, SWT.TOP, true, true));
		btnValidateMap.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				validateMapping();
			}
		});

		btnGenMeta = new Button(generateGroup, SWT.PUSH);
		btnGenMeta.setText("Generate metadata");
		btnGenMeta.setLayoutData(new GridData(SWT.END, SWT.TOP, true, true));
		btnGenMeta.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				generateMeta();
			}
		});

		btnGenPatrol = new Button(generateGroup, SWT.PUSH);
		btnGenPatrol.setText("Generate patrols");
		btnGenPatrol.setLayoutData(new GridData(SWT.END, SWT.TOP, true, true));
		btnGenPatrol.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				generatePatrols();
			}
		});
		
		//TODO: remove after testing is complete
		xmlDatamodel.setFileName("e:\\SMART\\CT Conversion - Data From Rich\\V2_nigeria data model.xml");
		xmlMapping.setFileName("e:\\SMART\\CT Conversion - Data From Rich\\csv2smart\\mapping4_in.xml");

		updateState();
	}

	protected void loadCsv() {
		FileDialog dlg = new FileDialog(getShell(), SWT.OPEN);
		dlg.setFilterNames(new String[] {"CSV file"});
		dlg.setFilterExtensions(new String[] {"*.csv"});
		String fn = dlg.open();
		if (fn != null) {
			try {
				Connection c = ConnectionUtil.getConnection();
				Csv2DbLoader loader = new Csv2DbLoader();
				loader.load(new File(fn), c);
				updateState();
				MessageDialog.openInformation(getShell(), "Info", "Data from CSV loaded successfully");
			} catch (Exception e) {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Error occured. See console or log for details.");
				e.printStackTrace();
			}
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
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Error occured. See console or log for details.");
			e.printStackTrace();
		}
	}
	
	protected void validateMapping() {
		try {
			MatchSession session = new MatchSession();
			session.setSmartMapping(FileUtil.loadSmartMapping(xmlMapping.getFile()));
			session.setDataModel(FileUtil.loadDataModel(xmlDatamodel.getFile()));
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

	protected void generatePatrols() {
		DirectoryDialog dd = new DirectoryDialog(getShell(), SWT.SAVE);
		String f = dd.open();
		if (f != null) {
			try {
				MatchSession session = new MatchSession();
				session.setSmartMapping(FileUtil.loadSmartMapping(xmlMapping.getFile()));
				session.setDataModel(FileUtil.loadDataModel(xmlDatamodel.getFile()));
				session.setConnection(ConnectionUtil.getConnection());

				DataModelLookup dmLookup = new DataModelLookup(session.getDataModel());
				
				CsvPatrolExtractor exporter = new CsvPatrolExtractor(session.getConnection());
				exporter.extract(f, session, dmLookup);
				MessageDialog.openInformation(getShell(), "Patrol generation", "Patrol generation sucessfully completed.");
			} catch (Exception e) {
				MessageDialog.openError(getShell(), "Patrol generation", "Errors occured while patrol generation. See console for details.");
				e.printStackTrace();
			}
		}
	}

	protected void generateMeta() {
		DirectoryDialog dd = new DirectoryDialog(getShell(), SWT.SAVE);
		String f = dd.open();
		if (f != null) {
			try {
				MatchSession session = new MatchSession();
				session.setSmartMapping(FileUtil.loadSmartMapping(xmlMapping.getFile()));
				session.setDataModel(FileUtil.loadDataModel(xmlDatamodel.getFile()));
				session.setConnection(ConnectionUtil.getConnection());

				boolean isOk = true;
				CsvMetaExtractor metaExtractor = new CsvMetaExtractor(session);
				isOk = metaExtractor.exportMembers(new File(f + "\\" + "members.csv")) && isOk;
				isOk = metaExtractor.exportMandates(new File(f + "\\" + "mandates.csv")) && isOk;
				if (isOk) {
					MessageDialog.openInformation(getShell(), "Metadata generation", "Metadata generation sucessfully completed.");
				} else {
					MessageDialog.openError(getShell(), "Metadata generation", "Errors occured while metadata generation. See console for details.");
				}
			} catch (Exception e) {
				MessageDialog.openError(getShell(), "Metadata generation", "Errors occured while metadata generation session creation. See console for details.");
				e.printStackTrace();
			}
		}
	}
	
	private int getDbRowCount() {
		Connection c = ConnectionUtil.getConnection();
		try {
			ResultSet rs = c.createStatement().executeQuery("select count(*) from csv_to_smart.csv");
			if (rs.next()) {
				return rs.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		return 0;
	}
	
	private void updateState() {
		dbRecordsCount = getDbRowCount();
		dbLabel.setText(MessageFormat.format("Database contains {0} records", dbRecordsCount));
		dbLabel.getParent().layout(true);
		
		btnGenMap.setEnabled(dbRecordsCount > 0);
		btnValidateMap.setEnabled(dbRecordsCount > 0 && !xmlDatamodel.isEmpty() && !xmlMapping.isEmpty());
	}
	
	private class XmlUpdateFileComposite extends XmlFileComposite {

		public XmlUpdateFileComposite(Composite parent) {
			super(parent);
			setEditable(false);
		}
		
		@Override
		public void newFileSelected(String fn) {
			super.newFileSelected(fn);
			updateState();
		}
	}
}
