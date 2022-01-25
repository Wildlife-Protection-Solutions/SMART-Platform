package org.wcs.smart.nunavut;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.wcs.smart.patrol.xml.model.v13.AttachmentType;
import org.wcs.smart.patrol.xml.model.v13.LabelType;
import org.wcs.smart.patrol.xml.model.v13.PatrolLegDayType;
import org.wcs.smart.patrol.xml.model.v13.PatrolLegType;
import org.wcs.smart.patrol.xml.model.v13.PatrolMemberType;
import org.wcs.smart.patrol.xml.model.v13.PatrolType;
import org.wcs.smart.patrol.xml.model.v13.TrackType;
import org.wcs.smart.patrol.xml.model.v13.WaypointObservationAttributeType;
import org.wcs.smart.patrol.xml.model.v13.WaypointObservationGroupType;
import org.wcs.smart.patrol.xml.model.v13.WaypointObservationType;
import org.wcs.smart.patrol.xml.model.v13.WaypointType;

public class Converter {

	public static final String OUTPUT_FOLDER = "C:\\temp\\nunavutconversion\\";
	
	public void convert() throws Exception {
		
		Path pOut = Paths.get(OUTPUT_FOLDER);
		if (!Files.exists(pOut)) {
			Files.createDirectories(pOut);
		}
		
		NunavutDataSource source = new NunavutDataSource();
		
		List<String> pids = source.getPatrolsIds();
		for (String p : pids) {
			System.out.println("Processing patrol: " + p);
			convertPatrol(p, source);
		}
	}
	
	
	private void convertPatrol(String patrolId, NunavutDataSource source) throws Exception {
		
		List<Path> attachmentFiles = new ArrayList<>();
		
		PatrolType ptype = new PatrolType();
		ptype.setId(patrolId);
		ptype.setComment("");
		ptype.setEndDate(toXmlDate(LocalDate.now()));
		ptype.setStartDate(toXmlDate(LocalDate.now()));
		ptype.setIsArmed(null);
		ptype.setObjective(null);
		ptype.setPatrolType("ground/water/air");
		
		LabelType station = new LabelType();
		station.setLanguageCode("en");
		station.setValue("Station A");
		ptype.setStation(station);

		LabelType team = new LabelType();
		team.setLanguageCode("en");
		team.setValue("Team A");
		ptype.setStation(team);
		
		//make legs
		PatrolLegType leg1 = new PatrolLegType();
		ptype.getLegs().add(leg1);
		leg1.setEndDate(null);
		leg1.setStartDate(null);
		
		LabelType mandate = new LabelType();
		mandate.setLanguageCode("en");
		mandate.setValue("Team A");
		leg1.setMandate(mandate);
		
		LabelType transporttype = new LabelType();
		transporttype.setLanguageCode("en");
		transporttype.setValue("Foot");
		leg1.setTransportType(transporttype);
		
		PatrolMemberType member = new PatrolMemberType();
		member.setIsLeader(true);
		member.setIsPilot(false);
		member.setFamilyName("Doe");
		member.setGivenName("John");
		leg1.getMembers().add(member);
		
		//leg days
		PatrolLegDayType day1 = new PatrolLegDayType();
		leg1.getDays().add(day1);
		
		day1.setDate(toXmlDate(LocalDate.now()));
		day1.setEndTime(toXmlTime(LocalTime.now()));
		day1.setStartTime(toXmlTime(LocalTime.now()));
		day1.setRestMinutes(60.0);
		
		//track
		TrackType track1 = new TrackType();
		//track is represented as wkb string representation of multilinestring or linestring
		String ls = "LINESTRING(0 0, 1 1)";
		WKTReader reader = new WKTReader();
		Geometry geom = (Geometry)reader.read(ls);

		WKBWriter writer = new WKBWriter(3);
		track1.setGeom(encodeGeometry(writer.write(geom)));
		day1.setTrack(track1);
		
		//waypoints
		WaypointType waypoint = new WaypointType();
		waypoint.setX(0.0);
		waypoint.setY(0.0);
		waypoint.setId(patrolId);
		waypoint.setTime(toXmlTime(LocalTime.now()));
		waypoint.setComment(null);
		waypoint.setDirection(null);
		waypoint.setDistance(null);
		
		day1.getWaypoints().add(waypoint);
		
		//waypoint attachment
		AttachmentType wpattachment = new AttachmentType();
		wpattachment.setFilename("filename.png");
		waypoint.getAttachments().add(wpattachment);
//		attachmentFiles.add(add file here);
				
		//group
		WaypointObservationGroupType wpgroup = new WaypointObservationGroupType();
		waypoint.getGroups().add(wpgroup);
		
		//observation
		WaypointObservationType observation = new WaypointObservationType();
		observation.setCategoryKey("catagory.hkey");
		observation.setObserver(member);
		wpgroup.getObservations().add(observation);
		
		//attributes
		WaypointObservationAttributeType attribute1 = new WaypointObservationAttributeType();
		attribute1.setAttributeKey("attribute");
		attribute1.setBValue(null);
		attribute1.setDValue(null);
		attribute1.setSValue(null);
		observation.getAttributes().add(attribute1);
		
		WaypointObservationAttributeType attribute2 = new WaypointObservationAttributeType();
		attribute2.setAttributeKey("attribute");
		attribute2.setBValue(null);
		attribute2.setDValue(null);
		attribute2.setSValue(null);
		observation.getAttributes().add(attribute2);
		
		//observation attachment
		AttachmentType woattachment = new AttachmentType();
		woattachment.setFilename("filename.png");
		observation.getAttachments().add(woattachment);
//		attachmentFiles.add(add file here);
		
		//writes to xml
		writePatrolToFile(ptype, attachmentFiles);
	}
	
	
	private void writePatrolToFile(PatrolType patrol, List<Path> attachments) throws Exception {
		//write to xml file and then zip up all files (attachments in attachments dir)
		
		Path patrolxmlfile = Files.createTempFile("smartpatrol", ".xml");
		
		JAXBContext context = JAXBContext.newInstance("org.wcs.smart.patrol.xml.model.v13");
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		
		org.wcs.smart.patrol.xml.model.v13.ObjectFactory objFactor = new org.wcs.smart.patrol.xml.model.v13.ObjectFactory();
		
		JAXBElement<org.wcs.smart.patrol.xml.model.v13.PatrolType> element = objFactor.createPatrol(patrol);
		marshaller.marshal(element, patrolxmlfile.toFile());
		
		
		String fname = patrol.getId();
		Path pOut = Paths.get(OUTPUT_FOLDER).resolve(fname + ".zip");
		
		try(ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(pOut))){
			ZipEntry entry = new ZipEntry("patrol.xml");
			out.putNextEntry(entry);
			Files.copy(patrolxmlfile, out);
			
			ZipEntry zattachments = new ZipEntry("attachments/");
			out.putNextEntry(zattachments);
			
			for (Path file : attachments) {
				entry = new ZipEntry("attachments/" + file.getFileName().toString());				
				out.putNextEntry(entry);
				Files.copy(file, out);
			}
		}
		
	}
	
	private XMLGregorianCalendar toXmlDate(LocalDate d) throws DatatypeConfigurationException {
		if (d == null) return null;
		
		return DatatypeFactory.newInstance().newXMLGregorianCalendar(
				d.getYear(), d.getMonthValue(),d.getDayOfMonth(),
				0,0,0,
				DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED);
	}
	
	private XMLGregorianCalendar toXmlTime(LocalTime d) throws DatatypeConfigurationException{
		
		return DatatypeFactory.newInstance().newXMLGregorianCalendar(
				DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED,
				d.getHour(), d.getMinute(), d.getSecond(),
				DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED);
		
	}
	
	
	private String encodeGeometry(byte[] data) {
		if (data == null) return ""; //$NON-NLS-1$
		char[] toDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'a', 'b', 'c', 'd', 'e', 'f' };
		int l = data.length;
		char[] out = new char[l << 1];
		// two characters form the hex value.
		for (int i = 0, j = 0; i < l; i++) {
			out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
			out[j++] = toDigits[0x0F & data[i]];
		}
		return new String(out);

	}
	
	public static void main(String[] args) throws Exception{
		Converter converter = new Converter();
		converter.convert();
	}
}
