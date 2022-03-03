package org.wcs.smart.nunavut;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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

import org.wcs.smart.nunavut.MysqlObservation.valueType;
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

	public static final String OUTPUT_FOLDER = "D:\\Myfiles\\SMART\\Nunavut_data_conversion\\Output";
	public static final String prefix = "kit"; 
	
	public void convert() throws Exception {
		
		Path pOut = Paths.get(OUTPUT_FOLDER);
		if (!Files.exists(pOut)) {
			Files.createDirectories(pOut);
		}
		
		NunavutDataSource source = new NunavutDataSource();
		
		List<String> pids = source.getPatrolsIds();
		int count = 0;
		int completed = 0;
		for (String p : pids) {
			//Devel code only, much faster testing to just do a few patrols
			if(count < 3948 ) {
				count++;
				continue;
			}
			
			
			System.out.println("Processing patrol: " + p);
			convertPatrol(p, source);
			count++;
			completed++;
			
			//Devel code only, much faster testing to just do a few patrols
			//break;
//			if(count > 3650 ) {
//				break;
//			}
		}
		System.out.println("Complete. # of patrols converted: " + completed);
	}
	
	
	private void convertPatrol(String patrolId, NunavutDataSource source) throws Exception {
		
		ArrayList<String> attachmentFiles = new ArrayList<String>(); 
		
		HashMap<String, String> metadata;
		metadata = source.getPatrolMetaData(patrolId);
		
		if(metadata.get("start").equals("0000-00-00 00:00:00") || metadata.get("end").equals("0000-00-00 00:00:00")) {
			System.out.println("Skipping Patrol " + patrolId + " because of 0000/00/00 start or end dates.");
			return;
		}
		
		PatrolType ptype = new PatrolType();
		if(Double.parseDouble(patrolId) < 10) {
			ptype.setId(prefix + "00000" + patrolId);
		}else if(Double.parseDouble(patrolId) < 100) {
			ptype.setId(prefix + "0000" + patrolId);
		}else if(Double.parseDouble(patrolId) < 1000) {
			ptype.setId(prefix + "000" + patrolId);
		}else {
			ptype.setId(prefix + "00" + patrolId);
		}
		ptype.setComment("");
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDate startDate = LocalDate.parse(metadata.get("start"), formatter);
		LocalDate endDate = LocalDate.parse(metadata.get("end"), formatter);
		
		XMLGregorianCalendar xmlStartDate = toXmlDate(startDate);
		XMLGregorianCalendar xmlEndDate = toXmlDate(endDate);
		
		ptype.setStartDate(xmlStartDate);
		ptype.setEndDate(xmlEndDate);
		ptype.setIsArmed(true);
		ptype.setObjective(null);
		ptype.setPatrolType("Ground");
		
		LabelType station = new LabelType();
		station.setLanguageCode("en");
		station.setValue(metadata.get("station"));
		ptype.setStation(station);

		LabelType team = new LabelType();
		team.setLanguageCode("en");
		team.setValue(metadata.get("station"));
		ptype.setStation(team);
		
		//make legs
		PatrolLegType leg1 = new PatrolLegType();
		ptype.getLegs().add(leg1);
		leg1.setStartDate(xmlStartDate);
		leg1.setEndDate(xmlEndDate);
		leg1.setId("1");
		
		
		LabelType mandate = new LabelType();
		mandate.setLanguageCode("en");
		mandate.setValue(metadata.get("station"));
		leg1.setMandate(mandate);
		
		LabelType transporttype = new LabelType();
		transporttype.setLanguageCode("en");
		transporttype.setValue("Not Specified");
		leg1.setTransportType(transporttype);
		
		PatrolMemberType member = new PatrolMemberType();
		member.setIsLeader(true);
		member.setIsPilot(false);
		
		HashMap<String, String>names = source.getPatrolLeader(patrolId);
		
		//special cases for user names. want this maps a couple of misspellings and test users
		if(names != null && names.get("last_name") != null) {
			if(names.get("last_name").equals("TaloTest")) {
				member.setFamilyName("admin");
				member.setGivenName("admin");
			}else if(names.get("last_name").equals("Ukuqtunnuaq")  && names.get("first_name").equals("Johnny")) {
				member.setFamilyName("Uquktunnuaq");
				member.setGivenName("Johnny");
			}else if(names.get("last_name").equals("Sanertanut")  && names.get("first_name").equals("Bobby")) {
				member.setFamilyName("Sanertanaut");
				member.setGivenName("Bobby");
			}else if(names.get("last_name").equals("Idlout")  && names.get("first_name").equals("John Brian")) {
				member.setFamilyName("Idlout");
				member.setGivenName("John Bryan");
			}else if(names.get("last_name").equals("Anguttitauruq Sr")  && names.get("first_name").equals("Andrew ")) {
				member.setFamilyName("Anguttitauruq");
				member.setGivenName("Andrew");
			}else if(names.get("last_name").equals("Anguttitauruq Sr")  && names.get("first_name").equals("Andrew")) {
				member.setFamilyName("Anguttitauruq");
				member.setGivenName("Andrew");
			}else {
				member.setFamilyName(names.get("last_name"));
				member.setGivenName(names.get("first_name"));
			}
		}else {
			//use default if there is no name in the database
			member.setFamilyName("Data");
			member.setGivenName("TrailMark");
		}
		leg1.getMembers().add(member);
		
		//for each leg day
		for (LocalDate d = startDate; d.isBefore(endDate.plusDays(1)); d = d.plusDays(1)){
			//System.out.println("leg day: " + d + "\n");
			PatrolLegDayType day = new PatrolLegDayType();
			leg1.getDays().add(day);
			day.setDate(toXmlDate(d));
			if (d.equals(startDate)){
				
				day.setStartTime(toXmlTime(LocalTime.parse(metadata.get("start").split("\\s+")[1]))); //get the time portion of the start date
			}else{	
				day.setStartTime(toXmlTime(LocalTime.MIDNIGHT));
			}
			if(d.equals(endDate)) {
				day.setEndTime(toXmlTime(LocalTime.parse(metadata.get("end").split("\\s+")[1]))); //get the time portion of the end date
			}else {
				day.setEndTime(toXmlTime(LocalTime.MAX));
			}
			day.setRestMinutes(0.0);
			
				
			
			//track
			
			//TrackType track1 = source.getObsTrackOnDay(patrolId, d);
			//old way, only uses the obs waypoints.
			
			TrackType track1 = source.getFullTrackOnDay(patrolId, d);
			if(track1 != null) {
				day.setTrack(track1);
			}


			
			//waypoints
			ArrayList<MysqlCoordinate> points  = source.getWaypointsOnDay(patrolId, d);
			int waypointcount = 1;
			for(MysqlCoordinate p : points) {
				
				WaypointType waypoint = new WaypointType();
				waypoint.setX(p.getLon());
				waypoint.setY(p.getLat());
				waypoint.setId(String.valueOf(waypointcount));
				waypoint.setTime(toXmlTime(LocalTime.parse(p.getDate().format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
				waypoint.setComment(null);
				waypoint.setDirection(null);
				waypoint.setDistance(null);
				
				day.getWaypoints().add(waypoint);
				waypointcount++;
				
				//waypoint attachments
				ArrayList<String> filenames = source.getAttachmentsForWaypoint(p.getId());
				for(String file: filenames){
					AttachmentType wpattachment = new AttachmentType();
					Path f = Paths.get(file);
					wpattachment.setFilename(f.getFileName().toString());
					waypoint.getAttachments().add(wpattachment);
					if(!attachmentFiles.contains(file)) {
						attachmentFiles.add(file);
					}
				}
						
				//group
				WaypointObservationGroupType wpgroup = new WaypointObservationGroupType();
				waypoint.getGroups().add(wpgroup);
				

				//observations
				ArrayList<MysqlObservation> obs  = source.getObservationsForWaypoint(p.getId());
			
				for(MysqlObservation o: obs) {	
										
					WaypointObservationType observation = new WaypointObservationType();
					observation.setCategoryKey(o.getCategoryKey());
					observation.setObserver(null);
					wpgroup.getObservations().add(observation);
					
					
					ArrayList<String> attrs = o.getAttrs();
					ArrayList<String> values = o.getValues();
					ArrayList<valueType> types = o.getValueTypes();
					
					for (int i = 0; i < attrs.size(); i ++) {

						if(types.get(i).equals(valueType.LIST) && values.get(i) == null ){
							//there are various observations that don't have valid data, don't make attribute entries for these.
							continue;
						}
						if(types.get(i).equals(valueType.LIST) && values.get(i).equals("null") ){
							//similar to above, but can't check for the text, "null" until we know it's not actually null and Nullpointer would come up.
							continue;
						}
						
						//special attributes that don't map to attributes.
						if(attrs.get(i).equals("transporttype")) {//maps to patrol transport type
							LabelType transporttypeoverride = new LabelType();
							transporttypeoverride.setLanguageCode("en");
							transporttypeoverride.setValue(values.get(i));
							if(values.get(i).equals("Boat")){
								ptype.setPatrolType("MARINE");
							}
							leg1.setTransportType(transporttypeoverride);
						}else if(attrs.get(i).equals("trippurpose")) {// maps to Mandate 
							LabelType mandateoverride = new LabelType();
							mandateoverride.setLanguageCode("en");
							mandateoverride.setValue(values.get(i));
							leg1.setMandate(mandate);
							
							//the targeted... type attrs are multi-lists, deal with those specially here.
						}else if(attrs.get(i).equals("targettedlandspecies") || attrs.get(i).equals("targettingmarinespecies") || attrs.get(i).equals("targettedbirdspecies") || attrs.get(i).equals("targettedfishspecies")) {
							String catKey = attrs.get(i);
							
							List<WaypointObservationAttributeType> existingobs = observation.getAttributes();
							boolean found = false;
							for (WaypointObservationAttributeType o1 : existingobs) {
								if(catKey.equals(o1.getAttributeKey()) ){
									if (!o1.getItemKey().contains(values.get(i))) {//don't add duplicates
										o1.getItemKey().add(values.get(i));
									}
									found = true;
								}
							}
							if(!found) { //must be the first value, setup the attribute normally 
								WaypointObservationAttributeType attribute1 = new WaypointObservationAttributeType();
								attribute1.setAttributeKey(attrs.get(i));
								attribute1.getItemKey().add(values.get(i));
								observation.getAttributes().add(attribute1);
							}
							
						}else { //normal attributes
							WaypointObservationAttributeType attribute1 = new WaypointObservationAttributeType();
							attribute1.setAttributeKey(attrs.get(i));
							

							if(types.get(i).equals(valueType.LIST)) {
								attribute1.getItemKey().add(values.get(i));								

							}else if(types.get(i).equals(valueType.NUMERIC)) {
								if( values.get(i).equals("") || values.get(i) == null) {
									attribute1.setDValue(0.0);
								}else {
									attribute1.setDValue(Double.parseDouble(values.get(i)));
								}
							
							}else if(types.get(i).equals(valueType.TEXT)) {
								attribute1.setSValue(values.get(i));
							}
							observation.getAttributes().add(attribute1);
						}
					}//end for each attribute
					
				}//end for each observation
				
			}//end for each waypoint
		
		}
		
		//writes to xml
		ArrayList<Path> a = new ArrayList<Path>();
		for (String file : attachmentFiles) {
			Path p = Paths.get(file);
			a.add(p);
			//System.out.println("Filename found:" + p.getFileName().toString());
		}

		writePatrolToFile(ptype, a);
		
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
				try {
					entry = new ZipEntry("attachments/" + file.getFileName().toString());				
					out.putNextEntry(entry);
					Files.copy(file, out);
				}catch (NoSuchFileException e) {
					//make a copy of our blank file template
					Path blank = Paths.get(NunavutDataSource.FILE_LOCATION + "blank.txt");
					Path dest = Paths.get(NunavutDataSource.FILE_LOCATION + file.getFileName().toString());
					Files.copy(blank, dest);
					//add the blank as the attachment
					Files.copy(file, out);
				}
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
	
	
	public static void main(String[] args) throws Exception{
		Converter converter = new Converter();
		converter.convert();
	}
}
