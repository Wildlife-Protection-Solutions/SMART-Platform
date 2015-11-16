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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.conversion.csv.handler.ProcessingActionHandler;
import org.wcs.smart.conversion.csv.tool.Csv2DbLoader;
import org.wcs.smart.conversion.csv.tool.CsvExportTool;
import org.wcs.smart.conversion.csv.tool.CsvMergeTool;
import org.wcs.smart.conversion.csv.tool.CsvMergeTool.CsvMergeResult;
import org.wcs.smart.conversion.csv.tool.MatchFileBuilder;
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

	private static final Logger logger = LogManager.getLogger(CsvMatcherDialog.class); 
	
	private Label dbLabel;
	private int dbRecordsCount = 0;
	
	private XmlFileComposite xmlDatamodel;
	private XmlFileComposite xmlMapping;
	
	private Button btnMergeCsv;
	private Button btnClearDb;
	private Button btnExportCsv;
	private Button btnGenMap;
	private Button btnEditMap;
	private Button btnValidateMap;
	private Button btnGenMeta;
	private Button btnGenPatrol;
	private Button btnGenMission;

	private Text txtInfo;
	
	private ProcessingActionHandler handler;
	
	public CsvMatcherDialog(Shell shell) {
		super(shell, SWT.NONE);
		handler = new ProcessingActionHandler(getShell()) {
			@Override
			protected MatchSession createMatchSession() throws JAXBException, IOException {
				MatchSession session = new MatchSession();
				session.setSmartMapping(FileUtil.loadSmartMapping(xmlMapping.getFile()));
				session.setDataModel(FileUtil.loadDataModel(xmlDatamodel.getFile()));
				session.setConnection(ConnectionUtil.getConnection());
				return session;
			}
		};
		
		GridLayout layout = new GridLayout(1, false);
		this.setLayout(layout);

		GridData gridData = new GridData(SWT.FILL,SWT.FILL, true, true);
		this.setLayoutData(gridData);

		this.setSize(840, 640);

		Group dataGroup = new Group(this, SWT.NONE);
		dataGroup.setText("CSV Data Configuration");
		dataGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		dataGroup.setLayout(new GridLayout(1, false));
		
		dbLabel = new Label(dataGroup, SWT.NONE);
		
		Composite btnCmp = new Composite(dataGroup, SWT.NONE);
		btnCmp.setLayoutData(new GridData(SWT.END, SWT.TOP, true, true));
		GridLayout gridLayout = new GridLayout(3, false);
		gridLayout.horizontalSpacing = gridLayout.marginWidth = 0;
		gridLayout.marginLeft = 5;
		btnCmp.setLayout(gridLayout);
		
		Button btnLoadCsv = new Button(btnCmp, SWT.PUSH);
		btnLoadCsv.setText("Load data from CSV");
		btnLoadCsv.setLayoutData(new GridData(SWT.NONE, SWT.TOP, false, true));
		btnLoadCsv.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				loadCsv();
			}
		});

		btnMergeCsv = new Button(btnCmp, SWT.PUSH);
		btnMergeCsv.setText("Merge database with CSV");
		btnMergeCsv.setLayoutData(new GridData(SWT.NONE, SWT.TOP, false, true));
		btnMergeCsv.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				mergeCsv();
			}
		});

		btnClearDb = new Button(btnCmp, SWT.PUSH);
		btnClearDb.setText("Clear database");
		btnClearDb.setLayoutData(new GridData(SWT.NONE, SWT.TOP, false, true));
		btnClearDb.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				clearDb();
			}
		});

    	txtInfo = new Text(dataGroup, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER | SWT.H_SCROLL);
    	txtInfo.setEditable(false);
    	GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
    	gd.heightHint = 80;
    	txtInfo.setLayoutData(gd);
		
		btnExportCsv = new Button(dataGroup, SWT.PUSH);
		btnExportCsv.setText("Export database to CSV");
		btnExportCsv.setLayoutData(new GridData(SWT.END, SWT.TOP, true, true));
		btnExportCsv.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				exportCsv();
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
		generateGroup.setText("Patrol && Mission Generation");
		generateGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		generateGroup.setLayout(new GridLayout(1, false));

		xmlDatamodel = new XmlUpdateFileComposite(generateGroup);
		xmlDatamodel.setLabelText("SMART Datamodel XML:");
		
		xmlMapping = new XmlUpdateFileComposite(generateGroup);
		xmlMapping.setLabelText("Data Mapping XML: ");

		btnEditMap = new Button(generateGroup, SWT.PUSH);
		btnEditMap.setText("Edit mapping");
		btnEditMap.setLayoutData(new GridData(SWT.END, SWT.TOP, true, true));
		btnEditMap.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				editMapping();
			}
		});
		
		btnValidateMap = new Button(generateGroup, SWT.PUSH);
		btnValidateMap.setText("Validate mapping");
		btnValidateMap.setLayoutData(new GridData(SWT.END, SWT.TOP, true, true));
		btnValidateMap.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				handler.validateMapping();
			}
		});

		btnGenMeta = new Button(generateGroup, SWT.PUSH);
		btnGenMeta.setText("Generate metadata");
		btnGenMeta.setLayoutData(new GridData(SWT.END, SWT.TOP, true, true));
		btnGenMeta.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				handler.generateMeta();
			}
		});

		btnGenPatrol = new Button(generateGroup, SWT.PUSH);
		btnGenPatrol.setText("Generate patrols");
		btnGenPatrol.setLayoutData(new GridData(SWT.END, SWT.TOP, true, true));
		btnGenPatrol.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				handler.generatePatrols();
			}
		});

		btnGenMission = new Button(generateGroup, SWT.PUSH);
		btnGenMission.setText("Generate missions");
		btnGenMission.setLayoutData(new GridData(SWT.END, SWT.TOP, true, true));
		btnGenMission.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				handler.generateMissions();
			}
		});
		
		//TODO: remove after testing is complete
//		xmlDatamodel.setFileName("e:\\SMART\\CT Conversion - Data From Rich\\V2_nigeria data model.xml");
//		xmlMapping.setFileName("e:\\SMART\\CT Conversion - Data From Rich\\csv2smart\\mapping4_in.xml");

		updateState();
	}

	protected void clearDb() {
		if (MessageDialog.openConfirm(getShell(), "Clear database", "Do you really want to remove all the records from the database?")) {
			try {
				Connection c = ConnectionUtil.getConnection();
				Csv2DbLoader loader = new Csv2DbLoader();
				loader.cleanDb(c);
				updateState();
				txtInfo.setText(""); //$NON-NLS-1$
				MessageDialog.openInformation(getShell(), "Info", "Database is successfully reset");
			} catch (Exception e) {
				logger.error("Error while clearing the database.", e); //$NON-NLS-1$
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Error while clearing the database. See log for more details.\n" + e.toString());
			}
		}
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
				txtInfo.setText(MessageFormat.format("Loaded data from {0}", fn));
				MessageDialog.openInformation(getShell(), "Info", "Data from CSV loaded successfully");
			} catch (Exception e) {
				logger.error("Error loading CSV file.", e); //$NON-NLS-1$
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Error while loading file. See log for more details.\n" + e.toString());
			}
		}
	}

	protected void mergeCsv() {
		FileDialog dlg = new FileDialog(getShell(), SWT.OPEN);
		dlg.setFilterNames(new String[] {"CSV file"});
		dlg.setFilterExtensions(new String[] {"*.csv"});
		String fn = dlg.open();
		if (fn != null) {
			try {
				Connection c = ConnectionUtil.getConnection();
				CsvMergeTool  mergeTool = new CsvMergeTool();
				CsvMergeResult result = mergeTool.merge(new File(fn), c);
				if (result.isCompleted()) {
					List<String> messages = result.getMessages();
					ReportDialog report = new ReportDialog(getShell(), "Merge results", MessageFormat.format("{0} message(s) reported during merge:", messages.size()), messages);
					report.open();
					updateState();
					txtInfo.setText(txtInfo.getText() + "\r\n" + MessageFormat.format("Merged with data from {0}", fn));
				}
			} catch (Exception e) {
				logger.error("Error occured during merge operation.", e); //$NON-NLS-1$
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Error occured during merge operation. See log for details.");
			}
		}
	}

	protected void exportCsv() {
		FileDialog dlg = new FileDialog(getShell(), SWT.SAVE);
		dlg.setFilterNames(new String[] {"CSV file"});
		dlg.setFilterExtensions(new String[] {"*.csv"});
		String fn = dlg.open();
		if (fn != null) {
			try {
				Connection c = ConnectionUtil.getConnection();
				CsvExportTool  exportTool = new CsvExportTool();
				exportTool.export(new File(fn), c);
				MessageDialog.openInformation(getShell(), "Info", "Data exported successfully");
			} catch (Exception e) {
				logger.error("Error occured during csv export.", e); //$NON-NLS-1$
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Error occured during csv export. See log for details.");
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
					logger.error("Error occured while writing data to file.", e); //$NON-NLS-1$
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Error occured while writing data to file. See log for details.");
				}
			}
		} catch (Exception e) {
			logger.error("Error occured while creating mapping.", e); //$NON-NLS-1$
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Error occured while creating mapping. See log for details.");
		}
	}

	protected void editMapping() {
		try {
			MatchSession session = new MatchSession();
			session.setSmartMapping(FileUtil.loadSmartMapping(xmlMapping.getFile()));
			session.setDataModel(FileUtil.loadDataModel(xmlDatamodel.getFile()));
			session.setConnection(ConnectionUtil.getConnection());

			MatcherDialog dialog = new MatcherDialog(getShell(), session);
			dialog.open();
		} catch (JAXBException | IOException e) {
			logger.error("Error while editing mapping.", e); //$NON-NLS-1$
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Error while editing mapping. See log for details.");
		}
	}
	
	private int getDbRowCount() {
		Connection c = ConnectionUtil.getConnection();
		try {
			ResultSet rs = c.createStatement().executeQuery("select count(*) from csv_to_smart.csv"); //$NON-NLS-1$
			if (rs.next()) {
				return rs.getInt(1);
			}
		} catch (SQLException e) {
			logger.error("Failed to get db row count.", e); //$NON-NLS-1$
		} 
		return 0;
	}
	
	private void updateState() {
		dbRecordsCount = getDbRowCount();
		dbLabel.setText(MessageFormat.format("Database contains {0} records", dbRecordsCount));
		dbLabel.getParent().layout(true);
		
		btnGenMap.setEnabled(dbRecordsCount > 0);
		btnMergeCsv.setEnabled(dbRecordsCount > 0);
		btnExportCsv.setEnabled(dbRecordsCount > 0);
		btnEditMap.setEnabled(dbRecordsCount > 0 && !xmlDatamodel.isEmpty() && !xmlMapping.isEmpty());
		btnValidateMap.setEnabled(dbRecordsCount > 0 && !xmlDatamodel.isEmpty() && !xmlMapping.isEmpty());
		btnGenMeta.setEnabled(dbRecordsCount > 0 && !xmlDatamodel.isEmpty() && !xmlMapping.isEmpty());
		btnGenPatrol.setEnabled(dbRecordsCount > 0 && !xmlDatamodel.isEmpty() && !xmlMapping.isEmpty());
		btnGenMission.setEnabled(dbRecordsCount > 0 && !xmlDatamodel.isEmpty() && !xmlMapping.isEmpty());
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
