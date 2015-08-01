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
import java.sql.Time;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.export.PatrolScreensUtilToDel;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.importer.SmartImporter.CoordinateZComparator;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol.PatrolMeta;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.data.Data;
import org.wcs.smart.cybertracker.model.data.Data.Elements.E;
import org.wcs.smart.cybertracker.model.data.Data.Sightings;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.cybertracker.util.PdaUtil;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.PatrolType.Type;
import org.wcs.smart.patrol.model.Team;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Importer for CyberTracker application data. 
 * Imports from raw XML to {@link CyberTrackerPatrol} objects
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerImporter {
	
	public CyberTrackerImportResult importPdaData(IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.CyberTrackerImporter_Task_Download);
		CyberTrackerImportResult result = new CyberTrackerImportResult();
		List<CyberTrackerPatrol> patrols = new ArrayList<CyberTrackerPatrol>();
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
				CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, MessageFormat.format(Messages.CyberTrackerExportDialog_Error_CT_NotFound, ICyberTrackerConstants.DISPLAY_MIN_VERSION), null);
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
		
		Map<String, Data.Elements.E> elementsMap = buildElementsMap(data);
		Map<String, List<Data.Sightings.S>> patrolsMap = buildPatrolsMap(data);
		List<Coordinate> timerTrackList = buildTimerTrackList(data);
		
		data = null; //we don't need data object anymore
		List<CyberTrackerPatrol> patrols = new ArrayList<CyberTrackerPatrol>();
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			for (String id : patrolsMap.keySet()) {
				CyberTrackerPatrol ctPatrol = new CyberTrackerPatrol(elementsMap, patrolsMap.get(id));
				initMetaData(ctPatrol, session);
				List<Coordinate> trackPoints = SmartImporter.listPart(timerTrackList, ctPatrol.getStartDate(), ctPatrol.getEndDate());
				
				S lastS = !ctPatrol.getPatrolData().isEmpty() ? ctPatrol.getPatrolData().get(ctPatrol.getPatrolData().size()-1) : null;
				if (lastS != null && !hasWaypointData(lastS, elementsMap)) {
					//lastS is last point recorded when "End Patrol" was selected
					//do not record it as a separate waypoint but add to end of the track
					ctPatrol.getPatrolData().remove(ctPatrol.getPatrolData().size()-1);
					//adding last waypoint to the track
					//TODO: should all waypoints be part of a track?
					Coordinate coord = toCoordinate(lastS);
					if (coord != null)
						trackPoints.add(coord);
				}
				ctPatrol.setTimerTrackList(trackPoints);
				patrols.add(ctPatrol);
			}
		} finally {
			session.getTransaction().rollback();
			session.close();
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

	private void initMetaData(CyberTrackerPatrol ctPatrol, Session session) {
		List<S> patrolData = ctPatrol.getPatrolData();
		if (patrolData.isEmpty())
			return;
		
		 Map<String, E> eMap = ctPatrol.getElementsMap();
		S s = patrolData.get(0); //init metadata from the first sight (as all other sighs MUST have the same metadata by design)
		Date date = null;
		Time time = null;
		Date start_date = null;
		Time start_time = null;
		DateFormat formatter = SmartImporter.createCyberTrackerDateFormatter();
		
		for (S.A a : s.getA()) {
			String i = a.getI();
			String n = a.getN();
			String v = a.getV();
			
			if (ICyberTrackerConstants.DATE.equals(i)) {
				try {
					date = formatter.parse(v);
				} catch (ParseException e) {
					CyberTrackerPlugIn.log(e.getMessage(), e);
				}
			} else if (ICyberTrackerConstants.TIME.equals(i)) {
				time = Time.valueOf(v);
			} else if (ScreensUtil.RESULT_ID.equals(n)) {
				ctPatrol.setId(v);
			} else if (ScreensUtil.RESULT_START_DATE.equals(n)) {
				try {
					start_date = formatter.parse(v);
				} catch (ParseException e) {
					CyberTrackerPlugIn.log(e.getMessage(), e);
				}
			} else if (ScreensUtil.RESULT_START_TIME.equals(n)) {
				start_time = Time.valueOf(v);
			} else {
				E ei = eMap.get(i);
				if (ei != null) {
					recordPatrolData(ctPatrol, ei, v, eMap, session);
				} else {
					ctPatrol.addMissingKey(i);
				}
			}
		}
		
		date = SmartImporter.combine(date, time);
		start_date = SmartImporter.combine(start_date, start_time);
		if (date != null && start_date != null) {
			if (start_date.before(date)) {
				ctPatrol.setStartDate(start_date);
			} else {
				ctPatrol.setStartDate(date);
			}
		} else {
			if (date == null) {
				ctPatrol.setStartDate(start_date);
			} else {
				ctPatrol.setStartDate(date);
			}
		}
		
		date = null;
		time = null;
		
		s = patrolData.get(patrolData.size()-1); //need to find end date
		for (S.A a : s.getA()) {
			String i = a.getI();
			if (ICyberTrackerConstants.DATE.equals(i)) {
				try {
					date = formatter.parse(a.getV());
					if (time != null)
						break;
				} catch (ParseException e) {
					CyberTrackerPlugIn.log(e.getMessage(), e);
				}
			} else if (ICyberTrackerConstants.TIME.equals(i)) {
				time = Time.valueOf(a.getV());
				if (date != null)
					break;
			}
		}
		ctPatrol.setEndDate(SmartImporter.combine(date, time));
		
		//it is possible that pilot is configured as default value, but pilot doesn't make sense for ground patrols
		if (PatrolType.Type.GROUND.equals(ctPatrol.getPatrolType())) {
			ctPatrol.setCtPilot(null);
			ctPatrol.setPilot(null);
		}
	}

	private void recordPatrolData(CyberTrackerPatrol ctPatrol, E i, String v, Map<String, E> eMap, Session session) {
		String n = i.getN();
		if (PatrolScreensUtilToDel.RESULT_DEFAULT_PATROL_VALUES.equals(n)) {
			String[] ctIdArray = v.split(ICyberTrackerConstants.ATTRIBUTE_DEFAULT_VALUES_SEPATATOR);
			for (String ctid : ctIdArray) {
				E di = eMap.get(ctid); //default "E" element, we need to emulate as if it is set in a.i with a.v = di.tag2 ... ;)
				recordPatrolData(ctPatrol, di, di.getTag2(), eMap, session);
			}
		} else if (ScreensUtil.RESULT_ID.equals(n)) {
			ctPatrol.setId(v);
		}if (PatrolScreensUtilToDel.RESULT_PATROL_TYPE.equals(n)) {
			E e = eMap.get(v);
			String tag0 = e != null ? e.getTag0() : null;
			if (tag0 != null) {
				ctPatrol.setPatrolType(Type.valueOf(e.getTag0()));
			}
		} else if (PatrolScreensUtilToDel.RESULT_TRANSPORT.equals(n)) {
			E e = eMap.get(v);
			PatrolTransportType transportType = fetchFromTag0(PatrolTransportType.class, e, session);
			if (transportType == null)
				ctPatrol.addError(PatrolMeta.TRANSPORT, MessageFormat.format(Messages.CyberTrackerPatrol_Error_Transport, e.getN()));
			ctPatrol.setCtTransport(e.getN());
			ctPatrol.setPatrolTransportType(transportType);
		} else if (PatrolScreensUtilToDel.RESULT_ARMED.equals(n)) {
			E e = eMap.get(v);
			String tag0 = e != null ? e.getTag0() : null;
			if (tag0 != null) {
				ctPatrol.setArmed(ElementsUtil.BOOL_TRUE.equals(tag0.toLowerCase()));
			}				
		} else if (PatrolScreensUtilToDel.RESULT_TEAM.equals(n)) {
			E e = eMap.get(v);
			Team t = fetchFromTag0(Team.class, e, session);
			if (t == null && e.getTag0() != null)
				ctPatrol.addWarning(PatrolMeta.TEAM, MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Team, e.getN()));
			ctPatrol.setCtTeam(e.getN());
			ctPatrol.setTeam(t);
		} else if (PatrolScreensUtilToDel.RESULT_STATION.equals(n)) {
			E e = eMap.get(v);
			Station st = fetchFromTag0(Station.class, e, session);
			if (st == null && e.getTag0() != null)
				ctPatrol.addWarning(PatrolMeta.STATION, MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Station, e.getN()));
			ctPatrol.setCtStation(e.getN());
			ctPatrol.setStation(st);
		} else if (PatrolScreensUtilToDel.RESULT_MANDATE.equals(n)) {
			E e = eMap.get(v);
			PatrolMandate m = fetchFromTag0(PatrolMandate.class, e, session);
			if (m == null && e.getTag0() != null)
				ctPatrol.addWarning(PatrolMeta.MANDATE, MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Mandate, e.getN()));
			ctPatrol.setMandate(m);
		} else if (PatrolScreensUtilToDel.RESULT_OBJECTIVE.equals(n)) {
			ctPatrol.setObjective(v);
		} else if (PatrolScreensUtilToDel.RESULT_COMMENTS.equals(n)) {
			ctPatrol.setComment(v);
		} else if (PatrolScreensUtilToDel.RESULT_LEADER.equals(n)) {
			E e = eMap.get(v);
			Employee emp = fetchFromTag0(Employee.class, e, session);
			if (emp == null && e.getTag0() != null)
				ctPatrol.addError(PatrolMeta.LEADER, MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Leader, e.getN()));
			ctPatrol.setCtLeader(e.getN());
			ctPatrol.setLeader(emp);
		} else if (PatrolScreensUtilToDel.RESULT_PILOT.equals(n)) {
			E e = eMap.get(v);
			Employee emp = fetchFromTag0(Employee.class, e, session);
			if (emp == null && e.getTag0() != null)
				ctPatrol.addError(PatrolMeta.PILOT, MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Pilot, e.getN()));
			ctPatrol.setCtPilot(e.getN());
			ctPatrol.setPilot(emp);
		} else if (ElementsUtil.MEMBER_ELEMENT_TAG.equals(i.getTag1())) { //check that this is a member record
			Employee emp = fetchFromTag0(Employee.class, i, session);
			if (emp == null && i.getTag0() != null)
				ctPatrol.addWarning(PatrolMeta.MEMBERS, MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Member, i.getN()));
			ctPatrol.getCtMembers().add(i.getN());
			if (emp != null) {
				ctPatrol.getMembers().add(emp);
			}
		}		
	}
	
	private <T> T fetchFromTag0(Class<T> clazz, E e, Session session) {
		String tag0 = e != null ? e.getTag0() : null;
		if (tag0 != null) {
			return CyberTrackerHibernateManager.fetchByUuid(clazz, tag0, session);
		}
		return null;
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
				if (ScreensUtil.RESULT_ID.equals(a.getN())) {
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

	private List<Coordinate> buildTimerTrackList(Data data) {
		List<Coordinate> result = new ArrayList<Coordinate>();
		if (data == null || data.getTimerTracks() == null)
			return result;
		DateFormat formatter = SmartImporter.createCyberTrackerDateFormatter();
		for (Data.TimerTracks.T t : data.getTimerTracks().getT()) {
			Date date = null;
			Time time = null;
			double x = 0;
			double y = 0;
			for (Data.TimerTracks.T.A a : t.getA()) {
				String i = a.getI();
				if (ICyberTrackerConstants.DATE.equals(i)) {
					try {
						date = formatter.parse(a.getV());
					} catch (ParseException e) {
						CyberTrackerPlugIn.log(e.getMessage(), e);
					}
				} else if (ICyberTrackerConstants.TIME.equals(i)) {
					time = Time.valueOf(a.getV());
				} else if (ICyberTrackerConstants.LATITUDE.equals(i)) {
					y = Double.valueOf(a.getV());
				} else if (ICyberTrackerConstants.LONGITUDE.equals(i)) {
					x = Double.valueOf(a.getV());
				}
			}
			
			if (date == null || time == null)
				continue;
			
			result.add(new Coordinate(x, y, SmartImporter.combine(date, time).getTime()));
		}
		//sort by date+time
		Collections.sort(result, new CoordinateZComparator());
		return result;
	}

	private boolean hasWaypointData(S s, Map<String, E> eMap) {
		//true if at least one category or attribute was selected
		for (S.A a : s.getA()) {
			E e = eMap.get(a.getI());
			if (e != null) {
				if (ElementsUtil.CATEGORY_ELEMENT_TAG.equals(e.getTag1()) || ElementsUtil.ATTRIBUTE_ELEMENT_TAG.equals(e.getTag1()))
					return true;
			}
		}
		return false;
	}

	private Coordinate toCoordinate(S s) {
		DateFormat formatter = SmartImporter.createCyberTrackerDateFormatter();
		Date date = null;
		Time time = null;
		double x = 0;
		double y = 0;
		for (S.A a : s.getA()) {
			String i = a.getI();
			if (ICyberTrackerConstants.DATE.equals(i)) {
				try {
					date = formatter.parse(a.getV());
				} catch (ParseException e) {
					CyberTrackerPlugIn.log(e.getMessage(), e);
				}
			} else if (ICyberTrackerConstants.TIME.equals(i)) {
				time = Time.valueOf(a.getV());
			} else if (ICyberTrackerConstants.LATITUDE.equals(i)) {
				y = Double.valueOf(a.getV());
			} else if (ICyberTrackerConstants.LONGITUDE.equals(i)) {
				x = Double.valueOf(a.getV());
			}
		}
		
		if (date == null || time == null || x == 0 || y == 0)
			return null;
		
		return new Coordinate(x, y, SmartImporter.combine(date, time).getTime());
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
