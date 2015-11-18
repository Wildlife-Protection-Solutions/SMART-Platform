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
package org.wcs.smart.conversion.csv.handler;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.conversion.csv.tool.CsvMetaExtractor;
import org.wcs.smart.conversion.csv.tool.CsvMissionExtractor;
import org.wcs.smart.conversion.csv.tool.CsvPatrolExtractor;
import org.wcs.smart.conversion.csv.tool.MappingValidator;
import org.wcs.smart.conversion.lookup.DataModelLookup;
import org.wcs.smart.conversion.tool.MatchSession;
import org.wcs.smart.conversion.ui.ReportDialog;

/**
 * Class containing all validation and generation action handlers.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public abstract class ProcessingActionHandler {

	private static final Logger logger = LogManager.getLogger(ProcessingActionHandler.class); 
	
	private Shell shell;
	
	private String lastDir = null;
	
	public ProcessingActionHandler(Shell shell) {
		super();
		this.shell = shell;
	}

	protected Shell getShell() {
		return shell;
	}

	protected abstract MatchSession createMatchSession() throws JAXBException, IOException;
	
	public void validateMapping() {
		try {
			MatchSession session = createMatchSession();
			
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
			logger.error("Error during mapping validation.", e); //$NON-NLS-1$
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Error during mapping validation. See log for details.");
		}
	}

	public void generateMissions() {
		DirectoryDialog dd = new DirectoryDialog(getShell(), SWT.SAVE);
		dd.setFilterPath(lastDir);

		String f = dd.open();
		if (f != null) {
			lastDir = f;
			try {
				MatchSession session = createMatchSession();

				DataModelLookup dmLookup = new DataModelLookup(session.getDataModel());
				
				CsvMissionExtractor exporter = new CsvMissionExtractor(session.getConnection());
				exporter.extract(f, session, dmLookup);
				MessageDialog.openInformation(getShell(), "Mission generation", "Mission generation sucessfully completed.");
			} catch (Exception e) {
				logger.error("Error occured while mission generation.", e);
				MessageDialog.openError(getShell(), "Mission generation", "Error occured while mission generation. See log for details.");
			}
		}
	}
	
	public void generatePatrols() {
		DirectoryDialog dd = new DirectoryDialog(getShell(), SWT.SAVE);
		dd.setFilterPath(lastDir);
		
		String f = dd.open();
		if (f != null) {
			lastDir = f;
			try {
				MatchSession session = createMatchSession();

				DataModelLookup dmLookup = new DataModelLookup(session.getDataModel());
				
				CsvPatrolExtractor exporter = new CsvPatrolExtractor(session.getConnection());
				exporter.extract(f, session, dmLookup);
				MessageDialog.openInformation(getShell(), "Patrol generation", "Patrol generation sucessfully completed.");
			} catch (Exception e) {
				logger.error("Error occured while patrol generation.", e);
				MessageDialog.openError(getShell(), "Patrol generation", "Error occured while patrol generation. See log for details.");
			}
		}
	}

	public void generateMeta() {
		DirectoryDialog dd = new DirectoryDialog(getShell(), SWT.SAVE);
		dd.setFilterPath(lastDir);

		String f = dd.open();
		if (f != null) {
			lastDir = f;
			try {
				MatchSession session = createMatchSession();

				boolean isOk = true;
				CsvMetaExtractor metaExtractor = new CsvMetaExtractor(session);
				isOk = metaExtractor.exportMembers(new File(f + "\\" + "members.csv")) && isOk;
				isOk = metaExtractor.exportMandates(new File(f + "\\" + "mandates.csv")) && isOk;
				isOk = metaExtractor.exportTransects(new File(f + "\\" + "transects.csv")) && isOk;
				if (isOk) {
					MessageDialog.openInformation(getShell(), "Metadata generation", "Metadata generation sucessfully completed.");
				} else {
					MessageDialog.openError(getShell(), "Metadata generation", "Errors occured while metadata generation. See console for details.");
				}
			} catch (Exception e) {
				logger.error("Error occured while metadata generation.", e);
				MessageDialog.openError(getShell(), "Metadata generation", "Error occured while metadata generation. See log for details.");
			}
		}
	}

}
