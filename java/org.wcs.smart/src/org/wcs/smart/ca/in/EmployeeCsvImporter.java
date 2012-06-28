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
package org.wcs.smart.ca.in;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

import au.com.bytecode.opencsv.CSVReader;

import com.ibm.icu.text.SimpleDateFormat;

/**
 * Importer for importing employee data into
 * the current conservation area.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class EmployeeCsvImporter {

	
	/**
	 * FEMALE key for CSV file format 
	 */
	private static final String FEMALE = "F";
	/**
	 * MALE key for CSV file format
	 */
	private static final String MALE = "M";

	/**
	 * Current conservation area agencies
	 */
	private List<Agency> agencies = null;
	
	/**
	 * Imports employees from the CSV file into
	 * the current conservation area.
	 * 
	 * @param file the file to import
	 * @param skipHeader if the first line should be skipped or not
	 * @param monitor progress monitor
	 * @param session database session
	 * 
	 * @return true if data loaded, false if data not loaded (monitor cancelled)
	 * @throws Exception if there are any issues with the data in the csv file
	 */
	public boolean importCsvFile(File file, boolean skipHeader, IProgressMonitor monitor, Session session) throws Exception{
		if (!file.exists()){
			throw new IOException("The file: " + file.toString() + " does not exist.");
		}
		
		CSVReader reader = new CSVReader(new FileReader(file));
		
		int line = 1;
		if (skipHeader){
			reader.readNext();
			line ++;
		}
		String[] data ;
		List<Employee> employees = new ArrayList<Employee>();
		
		while( (data = reader.readNext()) != null ){
			if (monitor.isCanceled()) return false;
			if (data.length != 9){
				throw new Exception("Invalid number of fields on line: " + line + ".  Was " + data.length + " requires 9.");
			}
			Employee e = new Employee();
			int index = 0;
			// id
			String id = data[index++];
			if (id != null && id.trim().length() > 0){
				e.setId(id.trim());
			}
			
			// given name
			String given = data[index++];
			if (given == null || given.trim().length() == 0){
				throw new Exception("Give name required and not provided on line: " + line);
			}
			e.setGivenName(given.trim());
			
			//family name
			String family = data[index++];
			if (family == null || family.trim().length() == 0){
				throw new Exception("Family name required and not provided on line: " + line);
			}
			e.setFamilyName(family.trim());
			
			//birth date
			String birth = data[index++];
			if (birth == null || birth.trim().length() != 10){
				throw new Exception("Birth date must be provided in the format yyyy-mm-dd at line " + line);
			}
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			try{
				Date dt = format.parse(birth);
				e.setBirthDate(dt);
			}catch (ParseException ex){
				throw new Exception("Invalid birth date format on line " + line, ex);
			}
			
			//gender
			String gender = data[index++];
			if (gender == null || gender.trim().length() != 1 || 
					!(gender.trim().toUpperCase().equals(FEMALE) || gender.trim().toUpperCase().equals(MALE))){
				throw new Exception("Gender is required and must be " + FEMALE + " (female) or " + MALE + " male at line " + line);
			}
			if (gender.trim().toUpperCase().equals(FEMALE)){
				e.setGender(Employee.DB_FEMALE);
			}else{
				e.setGender(Employee.DB_MALE);
			}
			
			//start employment date
			String start = data[index++];
			if (start == null || start.trim().length() != 10){
				throw new Exception("Sart employment date must be provided in the format yyyy-mm-dd at line " + line);
			}
			try{
				Date dt = format.parse(start);
				e.setStartEmploymentDate(dt);
			}catch (ParseException ex){
				throw new Exception("Invalid start employment date format on line " + line);
			}
			if (e.getStartEmploymentDate().before(e.getBirthDate())){
				throw new Exception("Invalid start employement date on line " + line + ".  It must be after the employee birth date.");
			}
				
			//start employments
			String end = data[index++];
			if (end == null || end.trim().length() == 0){
				e.setEndEmploymentDate(null);
			}else {
				if (end.trim().length() != 10){
					throw new Exception("End employment date must be provided in the format yyyy-mm-dd at line " + line);
				}
				try{
					Date dt = format.parse(end);
					e.setEndEmploymentDate(dt);
				}catch (ParseException ex){
					throw new Exception("Invalid end employment date format on line " + line);
				}
				if (e.getEndEmploymentDate().before(e.getStartEmploymentDate()) || e.getEndEmploymentDate().before(e.getBirthDate())){
					throw new Exception("Invalid end employment date on line " + line + ".  Date must be after start employement date and after end employment date.");
				}
			}
			
			
			//agency & rank
			String agency = data[index++];
			String rank = data[index++];
			if (agency == null || agency.trim().length() == 0){
				e.setAgency(null);
				e.setRank(null);
			}else{
				//look for matching agency
				Agency ag = findAgency(agency, session);
				if (ag == null){
					//warning or something here
					throw new Exception("Agency " + agency + " not found on line " + line);
				}else{
					e.setAgency(ag);
					//look for matching rank
					if (rank == null || rank.trim().length() == 0){
						e.setRank(null);
					}else{
						Rank r = findRank(ag, rank);
						if (r == null){
							throw new Exception("Rank " + rank + " not found for agency " + agency + " on line " + line);
						}else{
							e.setRank(r);
						}
					}
				}
			}
			line++;
			if (e.getId() == null){
				HibernateManager.generateEmployeeId(e, session);
			}
			e.setConservationArea(SmartDB.getCurrentConservationArea());
		
			employees.add(e);
		}
		if (monitor.isCanceled()) return false;
		try{
			session.beginTransaction();
			for (Employee e : employees){
				session.saveOrUpdate(e);
			}
			session.getTransaction().commit();
		}catch (Exception ex){
			throw new Exception("Failed to save parsed employees: " + ex.getMessage(), ex);
		}
		return true;
	}
	
	
	
	/**
	 * Searches a agency for a rank with a given name.
	 * It will search all languages for the matching name.
	 * 
	 * @param agency the agency to search
	 * @param rank the name of the rank to find
	 * @return null if no rank found or the rank found
	 */
	private Rank findRank(Agency agency, String rank){
		for (Rank r : agency.getRanks()){
			for (Label lbl : r.getNames()){
				if (lbl.getValue().equalsIgnoreCase(rank)){
					return r;
				}
			}
		}
		return null;
	}
	/**
	 * Searches the current conservation area for the given 
	 * agency.
	 * 
	 * @param agency the agency to find
	 * @param session database session
	 * @return null if no matching agency found, or the agency found
	 */
	public Agency findAgency(String agency, Session session){
		if (agencies == null){
			agencies = loadAgencies(session);
		}
		
		for (Agency agt: agencies){
			for (Label lbl : agt.getNames()){
				if (lbl.getValue().equalsIgnoreCase(agency)){
					return agt;
				}
			}
		}
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public List<Agency> loadAgencies(Session session){
		List<Agency> agencies = null;
		session.beginTransaction();
		agencies = session.createCriteria(Agency.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list();
		session.getTransaction().commit();
		return agencies;
	}
}
