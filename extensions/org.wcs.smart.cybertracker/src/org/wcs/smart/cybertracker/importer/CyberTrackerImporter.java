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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.PatrolScreensUtil;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.data.Data;
import org.wcs.smart.cybertracker.model.data.Data.Sightings;
import org.wcs.smart.cybertracker.util.PdaUtil;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Importer for CyberTracker application data. 
 * Imports from raw XML to {@link CyberTrackerPatrol} objects
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerImporter {
	
	private Map<String, Data.Elements.E> elementsMap;
	private Map<String, List<Data.Sightings.S>> patrolsMap;
	
	public List<CyberTrackerPatrol> importPdaData(IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.CyberTrackerImporter_Task_Download);
		List<CyberTrackerPatrol> patrols = new ArrayList<CyberTrackerPatrol>();
		String appPath = PdaUtil.getCTAppPath();
		if (appPath == null) {
			CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, MessageFormat.format(Messages.CyberTrackerExportDialog_Error_CT_NotFound, ICyberTrackerConstants.MIN_VERSION));
			return patrols;
		}
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		PdaUtil.updateRegistryKey(ca);
		String[] downloadCommand = {appPath, ICyberTrackerConstants.COMMAND_DOWNLOAD};
		Process proc = Runtime.getRuntime().exec(downloadCommand);
		proc.waitFor();

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
				if (file.isFile())
					FileUtils.moveFileToDirectory(file, storageFolder, true);
			}
			
			return patrols;
			
		} finally {
			PdaUtil.deleteTempDirectory(xmlTempDir);
		}
	}

	protected List<CyberTrackerPatrol> importFileData(File file, IProgressMonitor monitor) throws Exception {
		if (file.getName().toLowerCase().endsWith(".ctx")) { //$NON-NLS-1$
			return importCtxFileData(file, monitor);
		}
		return importXmlFileData(file, monitor);
	}
	
	protected List<CyberTrackerPatrol> importCtxFileData(File file, IProgressMonitor monitor) throws Exception {
		File xmlTempDir = PdaUtil.createTempDirectory();
		try {
			monitor.subTask(Messages.CyberTrackerImporter_Task_ExtractRawData);
			String appPath = PdaUtil.getCTAppPath();
			if (appPath == null) {
				CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, MessageFormat.format(Messages.CyberTrackerExportDialog_Error_CT_NotFound, ICyberTrackerConstants.MIN_VERSION));
				return new ArrayList<CyberTrackerPatrol>();
			}
			File xmlFile = extractRawXml(appPath, file, xmlTempDir);
			return importXmlFileData(xmlFile, monitor);
		} finally {
			PdaUtil.deleteTempDirectory(xmlTempDir);
		}
	}
	
	protected List<CyberTrackerPatrol> importXmlFileData(File file, IProgressMonitor monitor) throws Exception {
		Data data = null;
		FileInputStream in = new FileInputStream(file);
		try {
			monitor.subTask(MessageFormat.format(Messages.CyberTrackerImporter_Read_Xml, file.getName()));
			data = readDataModel(in);
			monitor.worked(1);
		} catch (Exception e) {
			e.printStackTrace();
			data = null;
		} finally {
			in.close();
		}
		if (data == null) {
			throw new Exception(MessageFormat.format(Messages.CyberTrackerImporter_Read_Error, file.getName()));
		}
		
		elementsMap = buildElementsMap(data);
		patrolsMap = buildPatrolsMap(data);
		
		data = null; //we don't need data object anymore
		List<CyberTrackerPatrol> patrols = new ArrayList<CyberTrackerPatrol>();
		for (String id : patrolsMap.keySet()) {
			patrols.add(new CyberTrackerPatrol(elementsMap, patrolsMap.get(id)));
		}
		//sort patrols based on there CT id before returning
		Collections.sort(patrols, new Comparator<CyberTrackerPatrol>() {
			@Override
			public int compare(CyberTrackerPatrol p1, CyberTrackerPatrol p2) {
				if (p1.getStartDate() == null)
					return -1;
				if (p2.getStartDate() == null)
					return 1;
				return p1.getStartDate().compareTo(p2.getStartDate());
			}});
		return patrols;
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
	
	private Map<String, Data.Elements.E> buildElementsMap(Data data) {
		Map<String, Data.Elements.E> result = new HashMap<String, Data.Elements.E>();
		if (data == null || data.getElements() == null)
			return result;
		for (Data.Elements.E e : data.getElements().getE()) {
			result.put(e.getI(), e);
		}
		return result;
		
	}

	/**
	 * Returns mapped patrol_id to list of {@link Sightings.S} where each sighting is related to one patrol
	 * 
	 * @param data
	 * @return mapped patrol_id to list of {@link Sightings.S}

	 */
	private Map<String, List<Data.Sightings.S>> buildPatrolsMap(Data data) {
		Map<String, List<Data.Sightings.S>> result = new HashMap<String, List<Data.Sightings.S>>();
		if (data == null || data.getSightings() == null)
			return result;
		for (Data.Sightings.S s : data.getSightings().getS()) {
			//fetch patrol id value
			String patrolId = null;
			for (Data.Sightings.S.A a : s.getA()) {
				if (PatrolScreensUtil.RESULT_PATROL_ID.equals(a.getN())) {
					patrolId = a.getV();
				}
			}
			if (patrolId == null)
				continue;
			
			List<Data.Sightings.S> lst = result.get(patrolId);
			if (lst == null) {
				lst = new ArrayList<Data.Sightings.S>();
				result.put(patrolId, lst);
			}
			lst.add(s);
		}
		return result;
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
