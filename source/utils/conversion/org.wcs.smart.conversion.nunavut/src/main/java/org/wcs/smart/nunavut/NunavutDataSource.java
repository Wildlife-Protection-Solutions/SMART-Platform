package org.wcs.smart.nunavut;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBWriter;
import org.wcs.smart.nunavut.MysqlObservation.valueType;
import org.wcs.smart.patrol.xml.model.v13.TrackType;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class NunavutDataSource {

	private static final String HOST = "54.196.200.248";
	private static final int PORT = 3306;
	private static final String DB_NAME = "kitikmeot";
	private static final String USERNAME = "devel";
	private static final String PASSWORD = "aertiueSS!99kjpoiwe";
	public static final String FILE_LOCATION = "D:\\Myfiles\\SMART\\Nunavut_data_conversion\\Orig_Attachments\\kitikmeot\\AllInOneFolder\\";
	public static final String MAPPING_FILE = "D:\\Myfiles\\SMART\\Nunavut_data_conversion\\ListItemMapping\\items.csv";

	HashMap<String, String> itemMap = new HashMap<String, String>();
	
	private Connection connection;
	
	public NunavutDataSource() throws SQLException, CsvValidationException, IOException {
		
		StringBuilder sb = new StringBuilder();
		sb.append("jdbc:mysql://");
		sb.append(HOST);
		sb.append(":");
		sb.append(PORT);
		sb.append("/");
		sb.append(DB_NAME);
		sb.append("?user=" + USERNAME);
		sb.append("&password=" + PASSWORD);

		connection = DriverManager.getConnection(sb.toString());
		
		initHashMap(); //reads the csv of list items into a hash
	}

	public List<String> getPatrolsIds() throws SQLException{
		
		String sql = "SELECT MOBILE_DATA_RECORD_TRIP_ID FROM mobile_data_record_trip";
		List<String> patrolIds = new ArrayList<>();
		
		try(Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(sql)){
			
			while(rs.next()) {
				String pid = rs.getString(1);
				patrolIds.add(pid);
			}
		}
		return patrolIds;
		//return Collections.emptyList();
	}
	
//don't think I need this actually...
//	
//	public List<Path> getAttachments(String patrolId)throws SQLException{
//		List<Path> files = new ArrayList<Path>(); 
//		
//		String sql = "SELECT f.file_name FROM mobile_data_file f "
//				+ " JOIN mobile_data_record_entry_form a on a.mobile_data_record_entry_form_id = f.mobile_data_record_entry_form_id "
//				+ " JOIN mobile_data_record_entry b on b.mobile_data_record_entry_id = a.mobile_data_record_entry_id "
//				+ " JOIN mobile_data_record c on c.mobile_data_record_id = b.mobile_data_record_id "
//				+ " JOIN mobile_data_record_trip d on d.mobile_data_record_id = c.mobile_data_record_id "
//				+ " WHERE d.mobile_data_record_trip_id = " + patrolId;
//		
//		try(Statement s = connection.createStatement();
//			ResultSet rs = s.executeQuery(sql)){
//				while(rs.next()) {
//					String filename = rs.getString(1);
//					files.add(Paths.get(filename) );
//				}
//		}
//		
//		return files;
//	}
	
	public HashMap<String, String> getPatrolLeader(String patrolId) throws SQLException{
		HashMap<String, String> user = new HashMap<String, String>();
		
		String sql = "SELECT a.first_name, a.last_name FROM user a JOIN mobile_data_record b ON a.user_id = b.user_id "
				+ " JOIN mobile_data_record_trip c on b.mobile_data_record_id = c.mobile_data_record_id "
				+ " WHERE c.mobile_data_record_trip_id = " + patrolId;
		
		try(Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(sql)){
				while(rs.next()) {

					user.put("first_name", rs.getString(1));
					user.put("last_name", rs.getString(2));
				}
		}
		return user;
	}

	public HashMap<String, String> getPatrolMetaData(String patrolId) throws SQLException {
		
		HashMap<String, String> metadata = new HashMap<String, String>();
		
		String sql = "SELECT a.start_time, a.end_time FROM mobile_data_record_trip a WHERE a.mobile_data_record_trip_id = " + patrolId;
		
		try(Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(sql)){
				while(rs.next()) {
					String startTime = rs.getString(1);
					String endTime = rs.getString(2);
					metadata.put("start", startTime);
					metadata.put("end", endTime);
					metadata.put("station", DB_NAME);
				}
		}
		return metadata;
	}

	
	//Makes a track out of the observation waypoints, this is the original basic track that works. I'll try to get more track details from the DB in another function "getFullTrackOnDay"

	public TrackType getFullTrackOnDay(String patrolId, LocalDate day) throws SQLException, ParseException {
		String sql = "Select coords from mobile_data_geometry where MOBILE_DATA_RECORD_TRIP_ID = " + patrolId;
		LineString line = null;
		
		try(Statement s = connection.createStatement();ResultSet rs = s.executeQuery(sql)){
			int first = 1;
			while(rs.next()) { //should be only 1 row here, I'm not positive the data is perfect though, print something if we get unexpected row 2+
				if(first != 1) {
					System.out.println(patrolId + " has more than one trip coordinates. The last one will be used.");
				}
				String coords = rs.getString(1);
				String[] a = coords.split("\\),\\(");
				
				ArrayList<Coordinate> jtsCoordList = new ArrayList<Coordinate>();
				Coordinate lastYesterday = null; 
				for (String c : a) {

					try {
						String data[] = c.split(",");
						
						DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
						LocalDateTime currentDateTime = LocalDateTime.parse(data[2], formatter);
						if (Double.parseDouble(data[0]) > 180){ 
							//throw an exception...
							Double.parseDouble("invalidDouble");
						}
						
						if(currentDateTime.toLocalDate().equals(day.minusDays(1))) { //kep track of previous days points, so we can use the last one
							lastYesterday = new Coordinate(Double.parseDouble(data[1]), Double.parseDouble(data[0]), currentDateTime.toInstant(ZoneOffset.UTC).toEpochMilli());
						}
						
						
						//only add to the list if it's part of the correct day
						if(currentDateTime.toLocalDate().equals(day)) {
							if(lastYesterday != null && jtsCoordList.size() == 0) {// If we are starting a new day and there is a previous day's track, use the last point as today's first, so the tracks are smooth and connected. 
								jtsCoordList.add(lastYesterday);
							}
							jtsCoordList.add( new Coordinate(Double.parseDouble(data[1]), Double.parseDouble(data[0]), currentDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()) );
						}

					}catch(Exception e){
						//there are some bad coordinate data, a few bad dates in the DB etc, catch any parsing problems and  
						//just skip invalid coordinates/dates/etc
					}
				}
				
				
				if(jtsCoordList.size() > 1) {
					GeometryFactory gf = new GeometryFactory();
					Coordinate[] l = jtsCoordList.toArray(new Coordinate[0]);
					line= gf.createLineString(l);
				}else {
					line = null;
				}
				first++;
			}
			
			
		}
		TrackType track = new TrackType();
		if (line != null ) {  
//			try {
				WKBWriter writer = new WKBWriter(3);
				track.setGeom(encodeGeometry(writer.write(line)));
				double distance = (float)(distanceInMeters(line) / 1000.0);
				track.setDistance(distance);
//			}catch(Exception e){

//			}
		}else {
			return null;
		}

		return track;
		
	}
	public TrackType getObsTrackOnDay(String patrolId, LocalDate day) throws SQLException, ParseException {

		ArrayList<MysqlCoordinate> list  = getWaypointsOnDay(patrolId, day);
		
		if(list.size() == 1 ) {
			//only 1 coordinate found, check if the previous day has anything, if yes, use the last point from yesterday to start this day's track.
			ArrayList<MysqlCoordinate> yesterday;
			yesterday = getWaypointsOnDay(patrolId, day.minusDays(1));
			if (yesterday == null || yesterday.size() == 0) {
				return null;
			}else {
				MysqlCoordinate last = yesterday.get(yesterday.size()-1);
				list.add(0,last);//add the last point from yesterday's track to the start of todays points list
			}
			
		}else if(list.size() <1){
			return null; //0 coordinates found, there is no point in making a track with less than 2 points, so stop here for this leg-day.
			
		}
		
		Coordinate[] jtsCoordList = new Coordinate[list.size()];
		int x=0;
		for (MysqlCoordinate c : list) {
			jtsCoordList[x] = new Coordinate(c.getLon(), c.getLat(),c.getDate().toInstant(ZoneOffset.UTC).toEpochMilli());
			x++;
		}

		GeometryFactory gf = new GeometryFactory();
		LineString line = gf.createLineString(jtsCoordList);
		

		TrackType track1 = new TrackType();
		if (line != null) {
			WKBWriter writer = new WKBWriter(3);
			track1.setGeom(encodeGeometry(writer.write(line)));
			
			double distance = (float)(distanceInMeters(line) / 1000.0);
			track1.setDistance(distance);
		}

		return track1;
	}


	public ArrayList<MysqlCoordinate> getWaypointsOnDay(String patrolId, LocalDate day) throws SQLException {

		String sql = "SELECT c.mobile_data_record_entry_id, max(a.coords)"
				+ " FROM mobile_data_geometry a"
				+ " JOIN mobile_data_record_entry_form b ON a.mobile_data_record_entry_form_id = b.mobile_data_record_entry_form_id "
				+ " JOIN mobile_data_record_entry c ON b.mobile_data_record_entry_id = c.mobile_data_record_entry_id "
				+ " JOIN mobile_data_record d ON c.mobile_data_record_id = d.mobile_data_record_id "
				+ " JOIN mobile_data_record_trip e ON e.mobile_data_record_id = d.mobile_data_record_id "
				+ " WHERE e.mobile_data_record_trip_id = " + patrolId 
				+ " GROUP BY c.mobile_data_record_entry_id";				
				

		
		//		+ " AND DATE(a.time_created) = '" + day.toString() + "' ";

		
		try(Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(sql)){
			ArrayList<MysqlCoordinate> list = new ArrayList<MysqlCoordinate>(); 
			while(rs.next()) {
				String[] data = rs.getString(2).split(",");

				try{
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
					LocalDateTime currentDateTime = LocalDateTime.parse(data[2], formatter);
					LocalDate currentDay = currentDateTime.toLocalDate();
					if(!currentDay.equals(day)) {//if this waypoint is from the requested day, continue, otherwise continue to next loop iteration.
						continue;
					}
					
					MysqlCoordinate c = new MysqlCoordinate();
					c.setId(rs.getInt(1));
					
					c.setDate(currentDateTime);
					c.setLat(Double.parseDouble(data[0]));
					c.setLon(Double.parseDouble(data[1]));
					list.add(c);
				}catch(Exception e){
					//there is bad coordinate data in the DB like: "E_END=20120423222424,0,2012-04-23 22:22:40" Skip these when they fail to parse into lat/long
					//or some blank coordinates cause "data[2]" above to be out of bounds
					
					//skip invalid coordinates 
				}

			}

			list.sort((o1, o2) -> o1.getDate().compareTo(o2.getDate()));
			return list;
		}
	}
	
	
	
	
	//parameter "id" is the column mobile_data_record_id from table mobile_data_record in the Mysql DB
	public ArrayList<String> getAttachmentsForWaypoint(int id) throws SQLException {
		ArrayList<String> list = new ArrayList<String>();
		
		String sql = "SELECT c.file_name FROM mobile_data_record_entry a "
				+ " JOIN mobile_data_record_entry_form b ON a.mobile_data_record_entry_id = b.mobile_data_record_entry_id"
				+ " JOIN mobile_data_file c on b.mobile_data_record_entry_form_id = c.mobile_data_record_entry_form_id"
				+ " WHERE a.mobile_data_record_entry_id = " + id;
		
		try(Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(sql)){
				while(rs.next()) {
					if(!list.contains(FILE_LOCATION + rs.getString(1))) {
						list.add(FILE_LOCATION + rs.getString(1)); //only add if unique
					}
				}
		}
		return list;
	}
	
	//parameter "id" is the column mobile_data_record_id from table mobile_data_record in the Mysql DB
	public ArrayList<MysqlObservation> getObservationsForWaypoint(int id) throws SQLException {

		
		ArrayList<MysqlObservation> obs = new ArrayList<MysqlObservation>();
		
		String sql = "SELECT b.mobile_data_record_entry_form_id, b.content, c.form_title, c.form_question, d.project_title, b.mobile_form_item_id, c.mobile_form_id "
				+ " FROM mobile_data_record_entry_form b "
//				+ " LEFT JOIN mobile_form_item i on i.mobile_form_item_id = b.mobile_form_item_id " don't need this, have the mobile_form_item_id already, that is in our hash to look up from there.
				+ " JOIN mobile_form c on b.mobile_form_id = c.mobile_form_id"
				+ " JOIN mobile_project d on c.mobile_project_id = d.mobile_project_id"
				+ " WHERE b.mobile_data_record_entry_id = " + id  + " order by d.mobile_project_id, c.mobile_form_id";

		//System.out.println("\n\n" + sql);
		try(Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(sql)){
				MysqlObservation o = new MysqlObservation();
				boolean firstrow = true;
				
				while(rs.next()) {
					if(firstrow == true) {
						String cat = rs.getString(5); //project_title
						if(cat.equals("CBMN-OBSERVATION")) {						
							o.setCategoryKey("observationdetails.");
						}else if(cat.equals("CBMN-HARVEST")) {
							o.setCategoryKey("harvestdetails.");
						}else if(cat.equals("CBMN-TRIP-END")) {
							o.setCategoryKey("endtrip.");
						}else if(cat.equals("CBMN-TRIP-START")) {
							o.setCategoryKey("starttrip.");
						}

						firstrow = false;
					}
					
					int attr = rs.getInt(7);		//mobile_form_id
					if(attr == 24) { //PURPOSE
						o.getAttrs().add("trippurpose");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 25) {//LAND SPECIES
						o.getAttrs().add("targettedlandspecies");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 26) { //TRANSPORT
						o.getAttrs().add("transporttype");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 28) { //MARINE SPECIES
						o.getAttrs().add("targettingmarinespecies");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 29) { //BIRDS
						o.getAttrs().add("targettedbirdspecies");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 30) { //FISH
						o.getAttrs().add("targettedfishspecies");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 68) { //OTHER OBSERVATIONS, all values are animals, so mapping to "species"
						o.getAttrs().add("species");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 78) { //TEMPERATURE
						o.getAttrs().add("temperature");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 79) { //VISIBILITY
						o.getAttrs().add("visibility");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 80) { //REASON
						o.getAttrs().add("reasonforharvest");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 81) { //NUMBER OF INDIVIDUALS CAUGHT
						o.getAttrs().add("numberofindividualscaught");
						o.getValueTypes().add(valueType.NUMERIC);
						o.getValues().add(rs.getString(2));	//content
					}else if(attr == 82) { //SPECIES GROUP
						o.getAttrs().add("typeofanimalplantharvested");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 83) { // MAMMAL
						o.getAttrs().add("species");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 84) {//BIRD
						o.getAttrs().add("species");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 85) {//FISH
						o.getAttrs().add("species");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 87) {//CONDITION
						o.getAttrs().add("condition");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 88) {//STAGE OR SIZE
						o.getAttrs().add("lifestage");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 89) {//SEX
						o.getAttrs().add("sex");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 90) {//WEAPON
						o.getAttrs().add("typeofweaponused");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 91) {//NUMBER OF WOUNDED
						o.getAttrs().add("numberofwounded");
						o.getValueTypes().add(valueType.NUMERIC);
						o.getValues().add(rs.getString(2));	//content
					}else if(attr == 92) {//PHOTO AND AUDIO
						o.getAttrs().add("comments");
						o.getValueTypes().add(valueType.TEXT);
						o.getValues().add(rs.getString(2));	//content
					}else if(attr == 93) {//TEMPERATURE
						o.getAttrs().add("temperature");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 94) {//VISIBILITY
						o.getAttrs().add("visibility");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 95) {//DISTANCE
						o.getAttrs().add("distancetoobservedanimal");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 96) {//SPECIES GROUP
						o.getAttrs().add("typeofanimalplantharvested");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 97) {//FISH
						o.getAttrs().add("species");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 98) {//MAMMAL
						o.getAttrs().add("species");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 99) {//BIRD
						o.getAttrs().add("species");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 101) {//NUMBER OF INDIVIDUALS OBSERVERED
						o.getAttrs().add("numberofindividualsobserved");
						o.getValueTypes().add(valueType.NUMERIC);
						o.getValues().add(rs.getString(2));	//content
					}else if(attr == 102) {//CONDITION
						o.getAttrs().add("condition");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 103) {//STAGE OR SIZE
						o.getAttrs().add("lifestage");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 104) {//SEX
						o.getAttrs().add("sex");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}else if(attr == 105) { //PHOTO AND AUDIO
						o.getAttrs().add("comments");
						o.getValueTypes().add(valueType.TEXT);
						o.getValues().add(rs.getString(2));	//content
					}else if(attr == 106) { //ADDITIONAL HARVEST
						//do nothing, this was just a "do you want to enter another thing"  type of question/value
					}else if(attr == 129) { //TO COMPLETE DATA ENTRY
						//do nothing, just a placeholder item
					}else if(attr == 130) { //OTHER OBSERVATIONS
						o.getAttrs().add("species");
						o.getValueTypes().add(valueType.LIST);
						o.getValues().add(  itemMap.get(rs.getString(6))  );
					}	
					
					
				
					
					
										
				}
				obs.add(o);
		}
		
		return obs;
	}


	private void initHashMap() throws CsvValidationException, IOException {
		//get list items hashmap setup
 
		try (CSVReader reader = new CSVReader(new FileReader(MAPPING_FILE))) {
		      String[] lineInArray;
		      while ((lineInArray = reader.readNext()) != null) {
		          itemMap.put(lineInArray[0], lineInArray[1]);
		      }
		  }
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

	public static double distanceInMeters(LineString ls) {
        GeodeticCalculator cal = new GeodeticCalculator();
        double distance = 0;
        for (int i = 1; i < ls.getCoordinates().length; i ++){
                cal.setStartingGeographicPoint(ls.getCoordinateN(i-1).x, ls.getCoordinateN(i-1).y);
                cal.setDestinationGeographicPoint(ls.getCoordinateN(i).x, ls.getCoordinateN(i).y);

                distance +=cal.getOrthodromicDistance();
        }
        return distance;
}

	public void close() throws SQLException{
		connection.close();
	}
}
