package org.wcs.smart.mist.dataReader;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;

import au.com.bytecode.opencsv.CSVWriter;

public class DataReader {

	/**
	 * @param args
	 * @throws ClassNotFoundException
	 */
	public static void main(String[] args) throws Exception {
		
		if (args.length == 0){
			System.err.println("No MIST database file specified. Usage: DataReader PATH_TO_MIST_DB"); //$NON-NLS-1$
			System.exit(1);
		}
		File dbFile = new File(args[0]);
		if (!dbFile.exists()){
			System.err.println("MIST database file not found: '" + dbFile.toString() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			System.exit(1);
		}
		
		
		String lang;
		if(args[1] != null){
			lang = args[1];
		}else{
			lang = "en"; //$NON-NLS-1$
		}

		Connection c = MistDatabase.getConnection(dbFile.getAbsolutePath());

		
		//AGENCIES AND RANKS
		CSVWriter arWriter = new CSVWriter(new FileWriter("agency_and_ranks.csv"), ','); //$NON-NLS-1$
		
		String[] header = {"Agency>" + lang, "Rank>"+ lang}; //$NON-NLS-1$ //$NON-NLS-2$
		arWriter.writeNext(header);
		
		try{
			ResultSet rs = c
				.createStatement()
				.executeQuery(
						"SELECT distinct D.DEPARTMENT_NAME, DU.UNIT_NAME FROM DEPARTMENT_UNITS DU LEFT JOIN DEPARTMENTS D ON D.DEPARTMENT_ID = DU.DEPARTMENT_ID "); //$NON-NLS-1$
			while (rs.next()) {
			    String[] entries = {rs.getString(1), rs.getString(2)};
			    arWriter.writeNext(entries);
			}
		}finally{
			arWriter.close();
		}
		
		
		//EMPLOYEES
		CSVWriter eWriter = new CSVWriter(new FileWriter("employees_no_header.csv"), ','); //$NON-NLS-1$
		
		try{
			ResultSet rs = c
				.createStatement()
				.executeQuery(
						"SELECT Distinct E.EMPLOYEE_ID, E.FIRST_NAME, E.FAMILY_NAME, E.DATE_OF_BIRTH, 'M' as gender, E.DATE_OF_BIRTH, ' ' as end_date, D.DEPARTMENT_NAME, DU.UNIT_NAME " + //$NON-NLS-1$
							"FROM GROUND_PATROL_MEMBERS GPM " + //$NON-NLS-1$
							"LEFT JOIN EMPLOYEES E ON E.EMPLOYEE_ID = GPM.EMPLOYEE_ID " + //$NON-NLS-1$
							"LEFT JOIN DEPARTMENT_UNITS DU ON DU.RECORD_ID = E.DEPARTMENT_ID " + //$NON-NLS-1$
							"LEFT JOIN DEPARTMENTS D ON D.DEPARTMENT_ID = DU.DEPARTMENT_ID " + //$NON-NLS-1$
							"ORDER BY E.EMPLOYEE_ID "); //$NON-NLS-1$
			while (rs.next()) {
			    String[] entries = {rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9)};
			    eWriter.writeNext(entries);
			}
		}finally{
			eWriter.close();
		}
		
		
		//PATROL TYPES
		CSVWriter pWriter = new CSVWriter(new FileWriter("patrol_types.csv"), ','); //$NON-NLS-1$
		String[] pheader = {"Type","Key", "Name>"+ lang}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		pWriter.writeNext(pheader);
		try{
			ResultSet rs = c
				.createStatement()
				.executeQuery(
						"SELECT Distinct LGPT.PATROL_TYPE " + //$NON-NLS-1$
						"FROM GROUND_PATROLS GP "+ //$NON-NLS-1$
						"LEFT JOIN LK_GROUND_PATROL_TYPES LGPT ON LGPT.PATROL_TYPE_ID = GP.PATROL_TYPE_ID "); //$NON-NLS-1$
			while (rs.next()) {
			    String[] entries = {"Ground", "", rs.getString(1)}; //$NON-NLS-1$ //$NON-NLS-2$
			    pWriter.writeNext(entries);
			}
		}finally{
			pWriter.close();
		}
		
		

		//STATIONS
		CSVWriter sWriter = new CSVWriter(new FileWriter("station_names.csv"), ','); //$NON-NLS-1$
		String[] sheader = {"Name>" + lang, "Desc>"+ lang}; //$NON-NLS-1$ //$NON-NLS-2$
		sWriter.writeNext(sheader);
		try{
			ResultSet rs = c
				.createStatement()
				.executeQuery(
						"SELECT STATION_NAME FROM RANGER_STATIONS "); //$NON-NLS-1$
			while (rs.next()) {
			    String[] entries = {rs.getString(1), ""}; //$NON-NLS-1$
			    sWriter.writeNext(entries);
			}
		}finally{
			c.close();
			sWriter.close();
		}
		
	
		
	}


}
