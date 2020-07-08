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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.wcs.smart.SmartPlugIn;
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
import org.wcs.smart.util.SmartFileUtils;
import org.wcs.smart.util.SmartUtils;

/**
 * Importer for CyberTracker application data. 
 * Imports from raw XML to {@link ICyberTrackerData} objects
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerImporter {
	
	public static final String PREFIX_BEFORE_401 = "#"; //$NON-NLS-1$
	
	//this key should be mapped to patrol to support backward compatibility with versions 3.2.1 or less
	private static final String NO_DATTYPE_NAME = "null"; //$NON-NLS-1$
	
	private Map<String, CyberTrackerDataBuilder> dataBuilderMap = new HashMap<String, CyberTrackerDataBuilder>();
	
	public CyberTrackerImporter() {
		if (Platform.getExtensionRegistry() != null) {
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(CyberTrackerPlugIn.DATASOURCE_EXTENSION_ID);
			try {
				for (IConfigurationElement e : config) {
					String name = e.getAttribute("name"); //$NON-NLS-1$
					CyberTrackerDataBuilder builder = (CyberTrackerDataBuilder) e.createExecutableExtension("dataBuilder"); //$NON-NLS-1$
					dataBuilderMap.put(name, builder);
				}
			}catch (Exception ex){
				SmartPlugIn.displayLog(Messages.CyberTrackerImportComposite_ExtensionsError, ex);
			}
		}
	}
	/**
	 * 
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility to call done() on the given monitor
	 * @return
	 * @throws Exception
	 */
	public CyberTrackerImportResult importPdaData(IProgressMonitor monitor) throws Exception {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.CyberTrackerImporter_Task_Download, 2);
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

		Path cxtDataFolder = ICyberTrackerConstants.getDowloadFolder(ca);
		Path xmlTempDir = PdaUtil.createTempDirectory();
		//scan files in this directory and obtain raw xml for them
		progress.subTask(Messages.CyberTrackerImporter_Task_ExtractRawData);
		try {
			
			for (final Path file : Files.list(cxtDataFolder).collect(Collectors.toList())) {
				if (!Files.isDirectory(file))
					extractRawXml(appPath, file, xmlTempDir);
			}
			
			List<Path> items = Files.list(xmlTempDir).collect(Collectors.toList());
			
			progress.setWorkRemaining(items.size()+1);
			//now all raw xml data is in temporary directory, importing it
			for (final Path file : items) {
				patrols.addAll(importXmlFileData(file, progress.split(1)));
			}

			//move processed files to storage
			Path storageFolder = ICyberTrackerConstants.getStorageFolder(ca);
			
			for (final Path file : Files.list(cxtDataFolder).collect(Collectors.toList())) {
				if (!Files.isDirectory(file) && file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ctx")) { //$NON-NLS-1$
					if (patrols.isEmpty()) {
						Files.delete(file);
						
					} else {
						if (!Files.exists(storageFolder)) SmartUtils.createDirectory(storageFolder);
						Path target = storageFolder.resolve(file.getFileName().toString());
						Files.move(file, target);
					}
				}
			}
			progress.setWorkRemaining(0);
			result.setData(patrols);
			return result;
			
		} finally {
			SmartFileUtils.deleteTempDirectory(xmlTempDir);
		}
	}

	public List<ICyberTrackerData> importFileData(Path file, IProgressMonitor monitor) throws Exception {
		if (file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ctx")) { //$NON-NLS-1$
			return importCtxFileData(file, monitor);
		}
		return importXmlFileData(file, monitor);
	}
	
	protected List<ICyberTrackerData> importCtxFileData(Path file, IProgressMonitor monitor) throws Exception {
		SubMonitor progress = SubMonitor.convert(monitor, "", 1); //$NON-NLS-1$
		Path xmlTempDir = PdaUtil.createTempDirectory();
		try {
			progress.subTask(Messages.CyberTrackerImporter_Task_ExtractRawData);
			String appPath = PdaUtil.getCTAppPath();
			if (appPath == null) {
				CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, MessageFormat.format(Messages.CyberTrackerExportDialog_Error_CT_NotFound, ICyberTrackerConstants.DISPLAY_MIN_VERSION), null);
				return new ArrayList<ICyberTrackerData>();
			}
			Path xmlFile = extractRawXml(appPath, file, xmlTempDir);
			return importXmlFileData(xmlFile, progress.split(1));
		} finally {
			SmartFileUtils.deleteTempDirectory(xmlTempDir);
		}
	}
	
	protected List<ICyberTrackerData> importXmlFileData(Path file, IProgressMonitor monitor) throws Exception {
		SubMonitor progress = SubMonitor.convert(monitor, "", 2); //$NON-NLS-1$
		Data data = null;

		progress.split(1);
		progress.subTask(MessageFormat.format(Messages.CyberTrackerImporter_Read_Xml, file.getFileName().toString()));
		try(InputStream in = Files.newInputStream(file)) {
			data = readDataModel(in);
		} catch (Exception e) {
			CyberTrackerPlugIn.log(e.getMessage(), e);
			data = null;
		}
		if (data == null) {
			throw new Exception(MessageFormat.format(Messages.CyberTrackerImporter_Read_Error, file.getFileName().toString()));
		}
		
		preserveCompatibility400(data);
		
		CyberTrackerRawData rawData = new CyberTrackerRawData(data);
		data = null; //we don't need data object anymore

		progress.split(1);
		final String datatype = getDataType(rawData);
		CyberTrackerDataBuilder dataBuilder = dataBuilderMap.get(datatype);
		if (dataBuilder == null) {
			throw new Exception(MessageFormat.format(Messages.CyberTrackerImporter_DataType_Error, file.getFileName().toString(), datatype));
		}
		List<ICyberTrackerData> records = dataBuilder.buildRecords(rawData);
		return records;
	}

	/**
	 * After 4.0.0 prefix for meta records was changed from "#" to "SMART_".
	 * This was needed because SMART Connect cannot parse '#' in json.
	 * Here we need to replace all old prefixes to new one in case data from older versions is imported.
	 * @param data
	 */
	private void preserveCompatibility400(Data data) {
		if (data == null || data.getElements() == null)
			return;
		for (Data.Elements.E e : data.getElements().getE()) {
			if (e.getN() != null && e.getN().startsWith(PREFIX_BEFORE_401)) {
				e.setN(getCompatibleName400(e.getN()));
			}
		}
		
		if (data.getSightings() == null)
			return;
		for (Data.Sightings.S s : data.getSightings().getS()) {
			for (Data.Sightings.S.A a : s.getA()) {
				if (a.getN() != null && a.getN().startsWith(PREFIX_BEFORE_401)) {
					a.setN(getCompatibleName400(a.getN()));
				}
			}
		}
	}

	private String getCompatibleName400(String oldName) {
		String name = oldName.replaceFirst(PREFIX_BEFORE_401, ScreensUtil.COMMON_PREFIX);
		return name.replace('#', '_');
	}
	
	private String getDataType(CyberTrackerRawData rawData) {
		for (E e : rawData.elementsMap.values()) {
			if (ScreensUtil.RESULT_DATATYPE.equals(e.getN())) {
				return e.getTag0();
			}
		}
		return NO_DATTYPE_NAME;
	}

	private Path extractRawXml(String ctPath, Path ctxFile, Path destFolder)  throws Exception {
		if (Files.isDirectory(ctxFile))
			return null;
		String xmlFilePath = ctxFile.getFileName().toString();
		xmlFilePath = xmlFilePath.substring(0, xmlFilePath.toLowerCase(Locale.ROOT).lastIndexOf(".ctx")) + ".xml";  //$NON-NLS-1$//$NON-NLS-2$
		xmlFilePath = destFolder.toAbsolutePath().toString() + "\\" + xmlFilePath; //$NON-NLS-1$
		String[] extractCommand = {ctPath, ICyberTrackerConstants.COMMAND_DATAFILE, ctxFile.toAbsolutePath().toString(), ICyberTrackerConstants.COMMAND_EXPORT, xmlFilePath};
		Process proc = Runtime.getRuntime().exec(extractCommand);
		proc.waitFor();
		Path xmlFile = Paths.get(xmlFilePath);
		return Files.exists(xmlFile) ? xmlFile : null;
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
