/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.wcomm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.patrol.PatrolIdGenerator;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.wcomm.WcommMapping.AttributeMapping;
import org.wcs.smart.wcomm.WcommMapping.IncidentMapping;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Class for loading various wcomm data files.
 * @author Emily
 *
 */
public class DataLoader {

	private DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"); //$NON-NLS-1$
	private DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm"); //$NON-NLS-1$
	
	private static final String SPECIES_COL = "Species"; //$NON-NLS-1$
	private static final String DATE_COL = "Date"; //$NON-NLS-1$
	private static final String NUMINDIVIUAL_COL = "NumIndiv"; //$NON-NLS-1$
	private static final String UTMX_COL = "UTMX"; //$NON-NLS-1$
	private static final String UTMY_COL = "UTMY"; //$NON-NLS-1$
	private static final String TIME_COL = "Time"; //$NON-NLS-1$
	private static final String COMMENTS_COL = "Comments"; //$NON-NLS-1$
	private static final String SPOORSCAT_COL = "Spoor_scat"; //$NON-NLS-1$
	private static final String CARCASSAGE_COL = "CarcassAge"; //$NON-NLS-1$
	private static final String SEX_COL = "Sex"; //$NON-NLS-1$
	private static final String AGECLASS_COL = "AgeClass"; //$NON-NLS-1$
	private static final String NUMBER_COL = "Number"; //$NON-NLS-1$
	private static final String CAUSEOFDEATH_COL = "CauseOfDeath"; //$NON-NLS-1$
	private static final String MEANSOFDEATH_COL = "MeansOfDeath"; //$NON-NLS-1$
	private static final String LEFTTUSK_COL = "LeftTusk"; //$NON-NLS-1$
	private static final String RIGHTUSK_COL = "RightTusk"; //$NON-NLS-1$
	private static final String WSPECIES_COL = "WildlifeSpecies"; //$NON-NLS-1$
	private static final String LIVESTOCK_COL = "Livestock"; //$NON-NLS-1$
	private static final String INCIDENTTYPE_COL = "IncidentType"; //$NON-NLS-1$
	
	private List<Patrol> addedpatrols;
	private List<Waypoint> waypoints; 
	private WcommMapping mapping;
	
	private List<String> warnings;
	private CoordinateReferenceSystem srcCrs;
	
	private Path wopath, empath, ocpath, hwcpath, incidentpath;
	private PatrolTransportType tt;
	

	
	public DataLoader(Path wopath, Path empath, Path ocpath, Path hwcpath, Path incidentpath, PatrolTransportType tt, CoordinateReferenceSystem srcCrs) {
		this.srcCrs = srcCrs;
		this.wopath = wopath;
		this.empath = empath;
		this.hwcpath = hwcpath;
		this.ocpath = ocpath;
		this.incidentpath = incidentpath;
		this.tt = tt;
	}
	
	public List<String> getWarnings(){
		return warnings;
	}
	
	public List<Patrol> getPatrols(){
		return addedpatrols;
	}
	
	public void loadData(IProgressMonitor monitor) throws Exception {
		
		monitor.beginTask(Messages.DataLoader_taskname, 110);
		
		waypoints = new ArrayList<>();
		warnings = new ArrayList<>();
		
		monitor.subTask(Messages.DataLoader_loadingtask);
		mapping = WcommMapping.load();
		monitor.worked(1);
		
		
		try(Session session = HibernateManager.openSession()){
			
			monitor.subTask(Messages.DataLoader_wotask);
			if (wopath != null) processWildlifeObservations(wopath, session);
			monitor.worked(9);
			monitor.subTask(Messages.DataLoader_emtask);
			if (empath != null) processElephantMortality(empath, session);
			monitor.worked(10);
			monitor.subTask(Messages.DataLoader_octask);
			if (ocpath != null) processOtherCarcass(ocpath, session);
			monitor.worked(10);
			monitor.subTask(Messages.DataLoader_hwctask);
			if (hwcpath != null) processHWC(hwcpath, session);
			monitor.worked(10);
			monitor.subTask(Messages.DataLoader_inctask);
			if (incidentpath != null) processIncidents(incidentpath, session);
			monitor.worked(10);
		}
		
		if (!warnings.isEmpty()) {
			int[] ret = new int[] {0};
			Display.getDefault().syncExec(()->{
				WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), Messages.DataLoader_warningsTitle,
					Messages.DataLoader_WarningsMsg,
					warnings, new String[] {IDialogConstants.NO_LABEL, IDialogConstants.YES_LABEL}, 0);
				ret[0] = wd.open();
			});
			if (ret[0] == 0) {
				return;
			}
		}
		
		monitor.subTask(Messages.DataLoader_savetask);
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				addedpatrols = createPatrols(waypoints, tt, session, SubMonitor.convert(monitor, 50));
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				WCommPlugIn.displayLog(Messages.DataLoader_SaveError + ex.getMessage(), ex);
				return;
				
			}
		}
		Display.getDefault().syncExec(()->{
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.DataLoader_CompleteTitle, Messages.DataLoader_CompleteMsg);
		});

	}
	
	private List<Patrol> createPatrols(List<Waypoint> waypoints, PatrolTransportType tt, Session session, IProgressMonitor monitor) throws Exception {
		HashMap<LocalDate, List<Waypoint>> groups = new HashMap<>();
		
		for (Waypoint wp : waypoints) {
			LocalDate day = wp.getDateTime().toLocalDate();
			if (!groups.containsKey(day)) groups.put(day, new ArrayList<>());
			groups.get(day).add(wp);
		}
		
		MathTransform transform = CRS.findMathTransform(srcCrs, SmartDB.DATABASE_CRS);
		GeometryFactory gf = GeometryFactoryProvider.getFactory();
		
		monitor.beginTask(Messages.DataLoader_patrolstask, groups.size());
		
		List<Patrol> addedPatrols = new ArrayList<>();
		
		List<LocalDate> pdates = new ArrayList<>();
		pdates.addAll(groups.keySet());
		pdates.sort((a,b)->a.compareTo(b));
		
		for(LocalDate localdate : pdates) {
			Patrol p = new Patrol();
			p.setStartDate(localdate);
			p.setEndDate(localdate);
			p.setConservationArea(SmartDB.getCurrentConservationArea());
			p.setLegs(new ArrayList<>());
			p.setPatrolType(tt.getPatrolType());
			addedPatrols.add(p);
			
			PatrolLeg pl = new PatrolLeg();
			pl.setPatrol(p);
			p.getLegs().add(pl);
			
			PatrolLegMember m = new PatrolLegMember();
			m.setIsLeader(true);
			m.setMember(SmartDB.getCurrentEmployee());
			m.setPatrolLeg(pl);
			
			p.setId(PatrolIdGenerator.INSTANCE.generatePatrolId(p, session));

			pl.setStartDate(localdate);
			pl.setEndDate(localdate);
			pl.setId("Leg 1"); //$NON-NLS-1$
			pl.setLeader(m);
			pl.setMembers(new ArrayList<>());
			pl.getMembers().add(m);
			pl.setPatrolLegDays(new ArrayList<>());
			pl.setType(tt);
			
			PatrolLegDay pld = new PatrolLegDay();
			pld.setPatrolLeg(pl);
			pl.getPatrolLegDays().add(pld);
			pld.setDate(localdate);
			
			pld.setWaypoints(new ArrayList<>());
			
			List<Waypoint> wps = groups.get(localdate); 
			LocalDateTime start = wps.get(0).getDateTime();
			LocalDateTime end = wps.get(0).getDateTime();
			
			session.saveOrUpdate(p);
			
			for (Waypoint wp : wps) {
				
				Point pnt = (Point) JTS.transform(gf.createPoint(new Coordinate(wp.getX(), wp.getY())), transform);
				
				wp.setRawX(pnt.getX());
				wp.setRawY(pnt.getY());
				
				session.saveOrUpdate(wp);
				
				PatrolWaypoint pw = new PatrolWaypoint();
				pw.setWaypoint(wp);
				pld.getWaypoints().add(pw);
				pw.setPatrolLegDay(pld);
				
				if (wp.getDateTime().isAfter(end)) end = wp.getDateTime();
				if (wp.getDateTime().isBefore(start)) start = wp.getDateTime();
				
				session.saveOrUpdate(pw);
			}
			pld.setStartTime(start.toLocalTime());
			pld.setEndTime(end.toLocalTime());
			session.flush();
			
			monitor.worked(1);
		}
		return addedPatrols;
	}
	
	private Attribute findAttribute(WcommMapping.Field field, String categoryKey, List<Attribute> allatts)  {
		if (mapping.getValue(field) == null) return null;
		String v = mapping.getValue(field);
		for (Attribute a : allatts) {
			if (a.getKeyId().equals(v)) {
				return a;
			}
		}
		addWarning(MessageFormat.format(Messages.DataLoader_AttributeNotFound, v, categoryKey));
		return null;
	}
	
	private void addWarning(String msg) {
		if (warnings.contains(msg)) return;
		warnings.add(msg);
	}
	
	private String findMappedValue(String value, String attributekey) {
		for (AttributeMapping m : mapping.getAttributeMapping()) {
			if (m.attribute.equals(attributekey)) {
				if (m.value.equals(value)) return m.item;
			}
		}
		return null;
	}
	
	private void processWildlifeObservations(Path csv, Session session) throws IOException {
		
		try(CSVReader reader = new CSVReader(Files.newBufferedReader(csv))){
			
			String categoryKey = mapping.getValue(WcommMapping.Field.WO_CATEGORY);
			
			Category c = QueryFactory.buildQuery(session, Category.class,
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
					new Object[] {"hkey", categoryKey}).uniqueResult(); //$NON-NLS-1$
			if (c == null) {
				addWarning(MessageFormat.format(Messages.DataLoader_AttributeNotFoundWo, categoryKey));
				return;
			}
			
			List<Attribute> allatts = new ArrayList<>();
			c.getAllAttribute(allatts, null);
			
			
			List<Attribute> attributes = new ArrayList<>();
			
			Attribute speciesAttribute = findAttribute(WcommMapping.Field.WO_SPECIES, categoryKey, allatts);
			Attribute numindiv = findAttribute(WcommMapping.Field.WO_NUMINDIV, categoryKey, allatts);
			Attribute numspoor = findAttribute(WcommMapping.Field.WO_SPOORSCT, categoryKey, allatts);
			
			if (speciesAttribute != null) attributes.add(speciesAttribute);
			if (numindiv != null) attributes.add(numindiv);
			if (numspoor != null) attributes.add(numspoor);
			
			int datecol = -1;
			int utmxcol = -1;
			int utmycol = -1;
			int timecol = -1;
			
			HashMap<Attribute, Integer> att2col = new HashMap<>();
			
			String[] data = reader.readNext(); 
			for (int i = 0; i < data.length; i ++) {
				String value = data[i];
				
				if (value.equals(SPECIES_COL)) {
					if (speciesAttribute != null) att2col.put(speciesAttribute, i);
				}else if (value.equals(DATE_COL)) {
					datecol = i;
				}else if (value.equals(NUMINDIVIUAL_COL)) {
					if (numindiv != null) att2col.put(numindiv, i);
				}else if (value.equals(UTMX_COL)) {
					utmxcol = i;
				}else if (value.equals(UTMY_COL)) {
					utmycol = i;
				}else if (value.equals(TIME_COL)) {
					timecol = i;
				}else if (value.equals(SPOORSCAT_COL)) {
					if (numspoor != null) att2col.put(numspoor, i);
				}
			};
			
			while(true) {
				data = reader.readNext();
				if (data == null) break;
				if (data.length == 1 && data[0].trim().isEmpty()) continue;
				double x = Double.valueOf( data[utmxcol] );
				double y = Double.valueOf( data[utmycol] );
				
				LocalDate d = LocalDate.parse(data[datecol], dateFormat);
				LocalTime lt = LocalTime.NOON;
				if (timecol >= 0) {
					lt = LocalTime.parse(data[timecol], timeFormat);
				}
				LocalDateTime dt = d.atTime(lt);
				Waypoint wp = new Waypoint();
				wp.setConservationArea(SmartDB.getCurrentConservationArea());
				wp.setDateTime( dt );
				wp.setRawX(x);
				wp.setRawY(y);
				wp.setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
				wp.setObservationGroups(new ArrayList<>());
				waypoints.add(wp);
				
				WaypointObservationGroup group = new WaypointObservationGroup();
				wp.getObservationGroups().add(group);
				group.setWaypoint(wp);;
				group.setObservations(new ArrayList<>());
				
				WaypointObservation wo = new WaypointObservation();
				wo.setCategory(c);
				wo.setObservationGroup(group);
				wo.setAttributes(new ArrayList<>());
				group.getObservations().add(wo);
				
				for (Attribute a : attributes) {
					if (att2col.get(a) == null) continue;
					WaypointObservationAttribute woa = new WaypointObservationAttribute();
					woa.setAttribute(a);
					woa.setObservation(wo);
					
					processAttribute(a, woa, data, att2col.get(a), session, Messages.DataLoader_WildlifeObsDatasetname);
					
				}
			}
		}
	}
	
	private void processElephantMortality(Path csv, Session session) throws IOException {
		
		try(CSVReader reader = new CSVReader(Files.newBufferedReader(csv))){
			String categoryKey = mapping.getValue(WcommMapping.Field.EM_CATEGORY);
			
			Category c = QueryFactory.buildQuery(session, Category.class,
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
					new Object[] {"hkey", categoryKey}).uniqueResult(); //$NON-NLS-1$
			if (c == null) {
				addWarning(MessageFormat.format(Messages.DataLoader_AttributeNotFoundEm, categoryKey));
				return;
			}
			
			List<Attribute> allatts = new ArrayList<>();
			c.getAllAttribute(allatts, null);
			
			HashMap<WcommMapping.Field, Attribute> field2attribute = new HashMap<>();
	
			HashMap<String,WcommMapping.Field> col2field = new HashMap<>();
			col2field.put(SPECIES_COL, WcommMapping.Field.EM_SEPCIES);
			col2field.put(AGECLASS_COL, WcommMapping.Field.EM_AGE);
			col2field.put(CARCASSAGE_COL, WcommMapping.Field.EM_CARCASSAGE);
			col2field.put(CAUSEOFDEATH_COL, WcommMapping.Field.EM_CAUSEDEATH);
			col2field.put(LEFTTUSK_COL, WcommMapping.Field.EM_LEFTTUSK);
			col2field.put(MEANSOFDEATH_COL, WcommMapping.Field.EM_MEANSDEATH);
			col2field.put(RIGHTUSK_COL, WcommMapping.Field.EM_RIGHTTUSK);
			col2field.put(SEX_COL, WcommMapping.Field.EM_SEX);
			
			for (WcommMapping.Field f : col2field.values()) {
				Attribute attribute = findAttribute(f, categoryKey, allatts);
				if (attribute != null) field2attribute.put(f, attribute);
	
			}
			
			String[] data = reader.readNext();
			
			int datecol = -1;
			int timecol = -1;
			int utmxcol = -1;
			int utmycol = -1;
			int commentscol = -1;
			
			HashMap<Attribute, Integer> att2col = new HashMap<>();
			
			for(int i = 0; i < data.length; i ++) {
				String value = data[i];
				
				if (value.equals(DATE_COL)) {
					datecol = i;
				}else if (value.equals(UTMX_COL)) {
					utmxcol = i;
				}else if (value.equals(UTMY_COL)) {
					utmycol = i;
				}else if (value.equals(TIME_COL)) {
					timecol = i;
				}else if (value.equals(COMMENTS_COL)) {
					commentscol = i;
				}else if (col2field.containsKey(value)  ) {
					WcommMapping.Field f = col2field.get(value);
					att2col.put(field2attribute.get(f), i);
				}
			};
			
			while(true) {
				data = reader.readNext();
				if (data == null) break;
				if (data.length == 1 && data[0].trim().isEmpty()) continue;
				String comment = null;
				if (commentscol >= 0) comment = data[commentscol];
				double x = Double.valueOf(data[utmxcol]);
				double y = Double.valueOf(data[utmycol]);

				LocalDate d = LocalDate.parse(data[datecol], dateFormat);
				LocalTime lt = LocalTime.NOON;
				if (timecol >= 0) {
					lt = LocalTime.parse(data[timecol], timeFormat);
				}
				LocalDateTime dt = d.atTime(lt);
				
				Waypoint wp = new Waypoint();
				wp.setConservationArea(SmartDB.getCurrentConservationArea());
				wp.setDateTime(dt);
				wp.setRawX(x);
				wp.setRawY(y);
				wp.setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
				wp.setComment(comment);
				wp.setObservationGroups(new ArrayList<>());
				waypoints.add(wp);
				
				WaypointObservationGroup group = new WaypointObservationGroup();
				wp.getObservationGroups().add(group);
				group.setWaypoint(wp);;
				group.setObservations(new ArrayList<>());
				
				WaypointObservation wo = new WaypointObservation();
				wo.setCategory(c);
				wo.setObservationGroup(group);
				wo.setAttributes(new ArrayList<>());
				group.getObservations().add(wo);
				
				for (Attribute a : field2attribute.values()) {
					if (att2col.get(a) == null) continue;
					WaypointObservationAttribute woa = new WaypointObservationAttribute();
					woa.setAttribute(a);
					woa.setObservation(wo);
					
					processAttribute(a, woa, data, att2col.get(a), session, Messages.DataLoader_ElephantMortalityDatasetname);
					
				}
			}
		}
	}
	
	private void processOtherCarcass(Path csv, Session session) throws IOException {
		
		try(CSVReader reader = new CSVReader(Files.newBufferedReader(csv))){
			String categoryKey = mapping.getValue(WcommMapping.Field.OC_CATEGORY);
			
			Category c = QueryFactory.buildQuery(session, Category.class,
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
					new Object[] {"hkey", categoryKey}).uniqueResult(); //$NON-NLS-1$
			if (c == null) {
				addWarning(MessageFormat.format(Messages.DataLoader_AttributeNotFoundOc, categoryKey));
				return;
			}
			
			List<Attribute> allatts = new ArrayList<>();
			c.getAllAttribute(allatts, null);
			
			HashMap<WcommMapping.Field, Attribute> field2attribute = new HashMap<>();
	
			HashMap<String,WcommMapping.Field> col2field = new HashMap<>();
			col2field.put(SPECIES_COL, WcommMapping.Field.OC_SEPCIES);
			col2field.put(AGECLASS_COL, WcommMapping.Field.OC_AGE);
			col2field.put(CARCASSAGE_COL, WcommMapping.Field.OC_CARCAASSAGE);
			col2field.put(CAUSEOFDEATH_COL, WcommMapping.Field.OC_CAUSEDEATH);
			col2field.put(MEANSOFDEATH_COL, WcommMapping.Field.OC_MEANSDEATH);
			col2field.put(SEX_COL, WcommMapping.Field.OC_SEX);
			col2field.put(NUMBER_COL, WcommMapping.Field.OC_NUMBER);
			
			for (WcommMapping.Field f : col2field.values()) {
				Attribute attribute = findAttribute(f, categoryKey, allatts);
				if (attribute != null) field2attribute.put(f, attribute);
			}
			
			String[] data = reader.readNext();
			
			int datecol = -1;
			int timecol = -1;
			int utmxcol = -1;
			int utmycol = -1;
			int commentscol = -1;
			
			HashMap<Attribute, Integer> att2col = new HashMap<>();
			
			for(int i = 0; i < data.length; i ++) {
				String value = data[i];
				
				if (value.equals(DATE_COL)) {
					datecol = i;
				}else if (value.equals(UTMX_COL)) {
					utmxcol = i;
				}else if (value.equals(UTMY_COL)) {
					utmycol = i;
				}else if (value.equals(TIME_COL)) {
					timecol = i;
				}else if (value.equals(COMMENTS_COL)) {
					commentscol = i;
				}else if (col2field.containsKey(value)  ) {
					WcommMapping.Field f = col2field.get(value);
					att2col.put(field2attribute.get(f), i);
				}
			};
			
			while(true) {
				data = reader.readNext();
				if (data == null) break;
				if (data.length == 1 && data[0].trim().isEmpty()) continue;
				String comment = null;
				if (commentscol >= 0) comment = data[commentscol];
				double x = Double.valueOf(data[utmxcol]);
				double y = Double.valueOf(data[utmycol]);

				LocalDate d = LocalDate.parse(data[datecol], dateFormat);
				LocalTime lt = LocalTime.NOON;
				if (timecol >= 0) {
					lt = LocalTime.parse(data[timecol], timeFormat);
				}
				LocalDateTime dt = d.atTime(lt);
				
				Waypoint wp = new Waypoint();
				wp.setConservationArea(SmartDB.getCurrentConservationArea());
				wp.setDateTime(dt);
				wp.setRawX(x);
				wp.setRawY(y);
				wp.setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
				wp.setComment(comment);
				wp.setObservationGroups(new ArrayList<>());
				waypoints.add(wp);
				
				WaypointObservationGroup group = new WaypointObservationGroup();
				wp.getObservationGroups().add(group);
				group.setWaypoint(wp);;
				group.setObservations(new ArrayList<>());
				
				WaypointObservation wo = new WaypointObservation();
				wo.setCategory(c);
				wo.setObservationGroup(group);
				wo.setAttributes(new ArrayList<>());
				group.getObservations().add(wo);
				
				for (Attribute a : field2attribute.values()) {
					if (att2col.get(a) == null) continue;
					WaypointObservationAttribute woa = new WaypointObservationAttribute();
					woa.setAttribute(a);
					woa.setObservation(wo);
					
					processAttribute(a, woa, data, att2col.get(a), session, Messages.DataLoader_OtherCaracssesDatasetName);
					
				}
			}
		}
	}

	
	private void processHWC(Path csv, Session session) throws IOException {
		
		try(CSVReader reader = new CSVReader(Files.newBufferedReader(csv))){
			String categoryKey = mapping.getValue(WcommMapping.Field.HWC_CATEGORY);
			
			Category c = QueryFactory.buildQuery(session, Category.class,
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
					new Object[] {"hkey", categoryKey}).uniqueResult(); //$NON-NLS-1$
			if (c == null) {
				addWarning(MessageFormat.format(Messages.DataLoader_AttributeNotFoundHwc, categoryKey));
				return;
			}
			
			List<Attribute> allatts = new ArrayList<>();
			c.getAllAttribute(allatts, null);
			
			HashMap<WcommMapping.Field, Attribute> field2attribute = new HashMap<>();
			HashMap<String,WcommMapping.Field> col2field = new HashMap<>();
			col2field.put(LIVESTOCK_COL, WcommMapping.Field.HWC_LIVESTOCK);
			col2field.put(WSPECIES_COL, WcommMapping.Field.HWC_SPECIES);
			col2field.put(TIME_COL, WcommMapping.Field.HWC_TIME);
			
			for (WcommMapping.Field f : col2field.values()) {
				Attribute attribute = findAttribute(f, categoryKey, allatts);
				if (attribute != null) field2attribute.put(f, attribute);
			}
			
			String[] data = reader.readNext();
			
			int datecol = -1;
			int timecol = -1;
			int utmxcol = -1;
			int utmycol = -1;
			int commentscol = -1;
			
			HashMap<Attribute, Integer> att2col = new HashMap<>();
			
			for(int i = 0; i < data.length; i ++) {
				String value = data[i];
				
				if (value.equals(DATE_COL)) {
					datecol = i;
				}else if (value.equals(UTMX_COL)) {
					utmxcol = i;
				}else if (value.equals(UTMY_COL)) {
					utmycol = i;
				}else if (value.equals(TIME_COL)) {
					timecol = i;
				}else if (value.equals(COMMENTS_COL)) {
					commentscol = i;
				}else if (col2field.containsKey(value)  ) {
					WcommMapping.Field f = col2field.get(value);
					att2col.put(field2attribute.get(f), i);
				}
			};
			
			while(true) {
				data = reader.readNext();
				if (data == null) break;
				if (data.length == 1 && data[0].trim().isEmpty()) continue;
				String comment = null;
				if (commentscol >= 0) comment = data[commentscol];
				double x = Double.valueOf(data[utmxcol]);
				double y = Double.valueOf(data[utmycol]);

				LocalDate d = LocalDate.parse(data[datecol], dateFormat);
				LocalTime lt = LocalTime.NOON;
				if (timecol >= 0) {
					String v = data[timecol];
					if (v.equalsIgnoreCase("Day")) { //$NON-NLS-1$
						lt = LocalTime.NOON;
					}else if (v.equalsIgnoreCase("Night")) { //$NON-NLS-1$
						lt = LocalTime.MIDNIGHT;
					}else {
						lt = LocalTime.parse(data[timecol], timeFormat);
					}
				}
				LocalDateTime dt = d.atTime(lt);
				
				Waypoint wp = new Waypoint();
				wp.setConservationArea(SmartDB.getCurrentConservationArea());
				wp.setDateTime(dt);
				wp.setRawX(x);
				wp.setRawY(y);
				wp.setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
				wp.setObservationGroups(new ArrayList<>());
				wp.setComment(comment);
				waypoints.add(wp);
				
				WaypointObservationGroup group = new WaypointObservationGroup();
				wp.getObservationGroups().add(group);
				group.setWaypoint(wp);;
				group.setObservations(new ArrayList<>());
				
				WaypointObservation wo = new WaypointObservation();
				wo.setCategory(c);
				wo.setObservationGroup(group);
				wo.setAttributes(new ArrayList<>());
				group.getObservations().add(wo);
				
				for (Attribute a : field2attribute.values()) {
					if (att2col.get(a) == null) continue;
					WaypointObservationAttribute woa = new WaypointObservationAttribute();
					woa.setAttribute(a);
					woa.setObservation(wo);
					
					processAttribute(a, woa, data, att2col.get(a), session, Messages.DataLoader_HWCDatasetName);
					
				}
			}
		}
	}
	
	
	private void processIncidents(Path csv, Session session) throws IOException {
		
		try(CSVReader reader = new CSVReader(Files.newBufferedReader(csv))){
	
			String[] data = reader.readNext();
			
			int datecol = -1;
			int timecol = -1;
			int utmxcol = -1;
			int utmycol = -1;
			int incidenttypecol = -1;
			
			for(int i = 0; i < data.length; i ++) {
				String value = data[i];
				
				if (value.equals(DATE_COL)) {
					datecol = i;
				}else if (value.equals(UTMX_COL)) {
					utmxcol = i;
				}else if (value.equals(UTMY_COL)) {
					utmycol = i;
				}else if (value.equals(TIME_COL)) {
					timecol = i;
				}else if (value.equals(INCIDENTTYPE_COL)) {
					incidenttypecol = i;
				}
			};
			
			while(true) {
				data = reader.readNext();
				if (data == null) break;
				
				String comment = null;

				double x = Double.valueOf(data[utmxcol]);
				double y = Double.valueOf(data[utmycol]);

				LocalDate d = LocalDate.parse(data[datecol], dateFormat);
				LocalTime lt = LocalTime.NOON;
				if (timecol >= 0) {
					lt = LocalTime.parse(data[timecol], timeFormat);
				}
				LocalDateTime dt = d.atTime(lt);
				
				Waypoint wp = new Waypoint();
				wp.setConservationArea(SmartDB.getCurrentConservationArea());
				wp.setDateTime(dt);
				wp.setRawX(x);
				wp.setRawY(y);
				wp.setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
				wp.setObservationGroups(new ArrayList<>());
				wp.setComment(comment);
				waypoints.add(wp);
				
				String value = data[incidenttypecol];
				IncidentMapping im = null;
				for (IncidentMapping m : mapping.getIncidentMapping()) {
					if (m.value.equals(value)) {
						im = m;
						break;
					}
				}
				if (im == null) {
					addWarning(MessageFormat.format(Messages.DataLoader_MappingNotFoundIncident, value));
					continue;
				}
				
				Category c = QueryFactory.buildQuery(session, Category.class,
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
						new Object[] {"hkey", im.category}).uniqueResult(); //$NON-NLS-1$
				if (c == null) {
					addWarning(MessageFormat.format(Messages.DataLoader_Hkeynotfound, im.category));
					continue;
				}
				
				WaypointObservationGroup group = new WaypointObservationGroup();
				wp.getObservationGroups().add(group);
				group.setWaypoint(wp);;
				group.setObservations(new ArrayList<>());
				
				WaypointObservation wo = new WaypointObservation();
				wo.setCategory(c);
				wo.setObservationGroup(group);
				wo.setAttributes(new ArrayList<>());
				group.getObservations().add(wo);
				
				Attribute found = null;
				if (im.attribute != null) {
					List<Attribute> all = new ArrayList<>();
					c.getAllAttribute(all, null);
					for (Attribute a : all) {
						if (a.getKeyId().equals(im.attribute)) {
							found = a;
							break;
						}
					}
					if (found == null) {
						addWarning(MessageFormat.format(Messages.DataLoader_attributenotfound, im.attribute, im.category));
						continue;
					}
				}
				
				AttributeListItem li = null;
				if (found != null && im.item != null) {
					for (AttributeListItem lli : found.getAttributeList()) {
						if (lli.getKeyId().equals(im.item)) {
							li = lli;
							break;
						}
					}
					if (li == null) {
						addWarning(MessageFormat.format(Messages.DataLoader_listitemnotfound, im.item, im.attribute, im.category));
						continue;
					}
				}
				
				if (found != null && li == null) {
					addWarning(MessageFormat.format(Messages.DataLoader_missinglistitem, found.getKeyId()));
				}
								
				if (found != null && li != null) {
					WaypointObservationAttribute woa = new WaypointObservationAttribute();
					woa.setAttribute(found);
					woa.setAttributeListItem(li);
					woa.setObservation(wo);
					wo.getAttributes().add(woa);
				}
			}
		}
	}

	private void processAttribute(Attribute a, WaypointObservationAttribute woa, String[] data, int col, Session session, String page) {
		if (a.getType() == Attribute.AttributeType.NUMERIC) {
			Double v = Double.valueOf(data[col]);
			woa.setNumberValue(v);
			woa.getObservation().getAttributes().add(woa);
			
		}else if (a.getType() == Attribute.AttributeType.TEXT) {
			woa.setStringValue(data[col]);
			woa.getObservation().getAttributes().add(woa);
		}else if (a.getType() == Attribute.AttributeType.LIST) {
			String value = data[col];
			
			String dmitem = findMappedValue(value, a.getKeyId());
			if (dmitem == null) {
				addWarning(MessageFormat.format(Messages.DataLoader_missingmapping, a.getName(), value, page));
			}else {
				AttributeListItem item = QueryFactory.buildQuery(session, AttributeListItem.class, 
						new Object[] {"attribute", a}, //$NON-NLS-1$
						new Object[] {"keyId", dmitem}).uniqueResult(); //$NON-NLS-1$
				if (item == null) {
					addWarning(MessageFormat.format(Messages.DataLoader_listitemnotfoundforattribute, dmitem, a.getName(), page));
				}
				if (item != null) {
					woa.setAttributeListItem(item);
					woa.getObservation().getAttributes().add(woa);
				}
			}
		}else if (a.getType() == Attribute.AttributeType.TREE) {
			String value = data[col];
			
			String dmitem = findMappedValue(value, a.getKeyId());
			if (dmitem == null) {
				addWarning(MessageFormat.format(Messages.DataLoader_mappingmissing, a.getName(), value, page));
			}else {
				AttributeTreeNode node = QueryFactory.buildQuery(session, AttributeTreeNode.class, 
						new Object[] {"attribute", a}, //$NON-NLS-1$
						new Object[] {"hkey", dmitem}).uniqueResult(); //$NON-NLS-1$
				if (node == null) {
					addWarning(MessageFormat.format(Messages.DataLoader_treenodenotfound, dmitem, a.getName(), page));
				}
				if (node != null) {
					woa.setAttributeTreeNode(node);
					woa.getObservation().getAttributes().add(woa);
				}
			}
		}
	}
}
