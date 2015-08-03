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
package org.wcs.smart.cybertracker.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerRawData;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.ICyberTrackerData;
import org.wcs.smart.cybertracker.model.data.Data;
import org.wcs.smart.cybertracker.model.data.Data.Elements.E;
import org.wcs.smart.cybertracker.util.PdaUtil;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Importer for CyberTracker application data. 
 * Imports from raw XML to {@link ICyberTrackerData} objects
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerImporter {
	
	public CyberTrackerImportResult importPdaData(IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.CyberTrackerImporter_Task_Download);
		CyberTrackerImportResult result = new CyberTrackerImportResult();
		List<ICyberTrackerData> patrols = new ArrayList<ICyberTrackerData>();
		String appPath = PdaUtil.getCTAppPath();
		if (appPath == null) {
			CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, MessageFormat.format(Messages.CyberTrackerExportDialog_Error_CT_NotFound, ICyberTrackerConstants.DISPLAY_MIN_VERSION), null);
			return result;
		}
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		PdaUtil.updateRegistryKey(ca);
		String[] downloadCommand = {appPath, ICyberTrackerConstants.COMMAND_DOWNLOAD};
		Process proc = Runtime.getRuntime().exec(downloadCommand);
		int code = proc.waitFor();
		result.setReturnCode(code);

		File cxtDataFolder = PdaUtil.getDowloadFolder(ca);
		File xmlTempDir = PdaUtil.createTempDirectory();
		//scan files in this directory and obtain raw xml for them
		monitor.subTask(Messages.CyberTrackerImporter_Task_ExtractRawData);
		try {
			for (final File file : cxtDataFolder.listFiles()) {
				if (file.isFile())
					extractRawXml(appPath, file, xmlTempDir);
			}
			
			//now all raw xml data is in temporary directory, importing it
			for (final File file : xmlTempDir.listFiles()) {
				patrols.addAll(importXmlFileData(file, monitor));
			}

			//move processed files to storage
			File storageFolder = PdaUtil.getStorageFolder(ca);
			for (final File file : cxtDataFolder.listFiles()) {
				if (file.isFile() && file.getName().toLowerCase().endsWith(".ctx")) { //$NON-NLS-1$
					if (patrols.isEmpty()) {
						FileUtils.forceDelete(file);
					} else {
						FileUtils.moveFileToDirectory(file, storageFolder, true);
					}
				}
			}
			
			result.setData(patrols);
			return result;
			
		} finally {
			PdaUtil.deleteTempDirectory(xmlTempDir);
		}
	}

	public List<ICyberTrackerData> importFileData(File file, IProgressMonitor monitor) throws Exception {
		if (file.getName().toLowerCase().endsWith(".ctx")) { //$NON-NLS-1$
			return importCtxFileData(file, monitor);
		}
		return importXmlFileData(file, monitor);
	}
	
	protected List<ICyberTrackerData> importCtxFileData(File file, IProgressMonitor monitor) throws Exception {
		File xmlTempDir = PdaUtil.createTempDirectory();
		try {
			monitor.subTask(Messages.CyberTrackerImporter_Task_ExtractRawData);
			String appPath = PdaUtil.getCTAppPath();
			if (appPath == null) {
				CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, MessageFormat.format(Messages.CyberTrackerExportDialog_Error_CT_NotFound, ICyberTrackerConstants.DISPLAY_MIN_VERSION), null);
				return new ArrayList<ICyberTrackerData>();
			}
			File xmlFile = extractRawXml(appPath, file, xmlTempDir);
			return importXmlFileData(xmlFile, monitor);
		} finally {
			PdaUtil.deleteTempDirectory(xmlTempDir);
		}
	}
	
	protected List<ICyberTrackerData> importXmlFileData(File file, IProgressMonitor monitor) throws Exception {
		Data data = null;
		
		try(FileInputStream in = new FileInputStream(file)) {
			monitor.subTask(MessageFormat.format(Messages.CyberTrackerImporter_Read_Xml, file.getName()));
			data = readDataModel(in);
			monitor.worked(1);
		} catch (Exception e) {
			CyberTrackerPlugIn.log(e.getMessage(), e);
			data = null;
		}
		if (data == null) {
			throw new Exception(MessageFormat.format(Messages.CyberTrackerImporter_Read_Error, file.getName()));
		}
		
		CyberTrackerRawData rawData = new CyberTrackerRawData(data);
		data = null; //we don't need data object anymore

		CyberTrackerDataBuilder dataBuilder = findDataBuilder(rawData);
		List<ICyberTrackerData> records = dataBuilder.buildRecords(rawData);
		return records;
	}


	private CyberTrackerDataBuilder findDataBuilder(CyberTrackerRawData rawData) {
		//TODO: need specific databuilder for each type from extension point (patrol or survey)
		for (E e : rawData.elementsMap.values()) {
			if (ScreensUtil.RESULT_DATATYPE.equals(e.getN())) {
				String datatype = e.getTag0();
				//TODO: return instance for given datatype
				if ("patrol".equals(datatype)) {
					return new PatrolCTDataBuilder();
				}
			}
		}
		return null;
	}

	private File extractRawXml(String ctPath, File ctxFile, File destFolder)  throws Exception {
		if (!ctxFile.isFile())
			return null;
		String xmlFilePath = ctxFile.getName();
		xmlFilePath = xmlFilePath.substring(0, xmlFilePath.toLowerCase().lastIndexOf(".ctx")) + ".xml";  //$NON-NLS-1$//$NON-NLS-2$
		xmlFilePath = destFolder.getAbsolutePath() + "\\" + xmlFilePath; //$NON-NLS-1$
		String[] extractCommand = {ctPath, ICyberTrackerConstants.COMMAND_DATAFILE, ctxFile.getAbsolutePath(), ICyberTrackerConstants.COMMAND_EXPORT, xmlFilePath};
		Process proc = Runtime.getRuntime().exec(extractCommand);
		proc.waitFor();
		File xmlFile = new File(xmlFilePath);
		return xmlFile.exists() ? xmlFile : null;
	}

	/**
	 * Reads data data from an xml file.
	 * <p>
	 * User is required to close input stream.
	 * </p>
	 * 
	 * @param file input stream to read data from
	 * @return
	 * @throws JAXBException
	 */
	public static Data readDataModel(InputStream file) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(Data.class);
		Unmarshaller un = context.createUnmarshaller();	
		Object o = un.unmarshal(file);
		return (Data) o;
	}
	
}
