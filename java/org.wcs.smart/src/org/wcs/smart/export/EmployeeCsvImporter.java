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
package org.wcs.smart.export;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import org.wcs.smart.export.config.ICsvDataImporter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Importer for importing employee data into
 * the current conservation area.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class EmployeeCsvImporter implements ICsvDataImporter {
	
	public static final String DATE_FORMAT = "yyyy-MM-dd"; //$NON-NLS-1$

	/* to export employees from derby in correct format:
	 * 	SELECT ID || ',' || givenname ||',' || familyname || ',' || 
	 * cast( birthdate as varchar(10))  || ',' || gender ||','
	 * || 
	 * cast( startemployementdate as varchar(10)) 
 	 * ||',' || case when endemployementdate is null then '' else  cast(endemployementdate as varchar(10))  end || ',,'
	 * FROM SMART.EMPLOYEE;
	 */
	
	/**
	 * Current conservation area agencies
	 */
	private List<Agency> agencies = null;
	
	private List<String> warnings = null;
	
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
	@Override
	public boolean importCsvFile(File file, char delimiter, boolean skipHeader, IProgressMonitor monitor, Session session) throws Exception {
		warnings = new ArrayList<String>();
		
		if (!file.exists()){
			throw new IOException(MessageFormat.format(Messages.EmployeeCsvImporter_Error_InputFileDoesNotExist1, new Object[]{file.toString()}));
		}
		
		List<Employee> employees = new ArrayList<Employee>();
		try(CSVReader reader = new CSVReader(
				new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), delimiter)){
			int line = 1;
			if (skipHeader){
				reader.readNext();
				line ++;
			}
			String[] data ;
			
			while( (data = reader.readNext()) != null ){
				if (monitor.isCanceled()) return false;
				if (data.length != 9){
					throw new Exception(MessageFormat.format(Messages.EmployeeCsvImporter_Error_InvalidNumberField, new Object[]{line, data.length}));
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
					throw new Exception(MessageFormat.format(Messages.EmployeeCsvImporter_Error_NoGivenName, new Object[]{line}));
				}
				e.setGivenName(given.trim());
				
				//family name
				String family = data[index++];
				if (family == null || family.trim().length() == 0){
					throw new Exception(MessageFormat.format(Messages.EmployeeCsvImporter_Error_NoFamilyName, new Object[]{ line}));
				}
				e.setFamilyName(family.trim());
				
				//birth date
				String birth = data[index++];
				if (birth == null || birth.trim().length() != 10){
					throw new Exception(MessageFormat.format(Messages.EmployeeCsvImporter_Error_BirthDateFormat, new Object[]{DATE_FORMAT,line}));
				}
				SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
				format.setLenient(false);
				try{
					Date dt = format.parse(birth);
					e.setBirthDate(dt);
				}catch (ParseException ex){
					throw new Exception(MessageFormat.format(Messages.EmployeeCsvImporter_Error_BirthDate, new Object[]{line}), ex);
				}
				
				//gender
				String gender = data[index++];
				if (gender == null || gender.trim().length() != 1 || 
						!(gender.trim().toUpperCase().charAt(0) == Employee.DB_FEMALE || gender.trim().toUpperCase().charAt(0) == Employee.DB_MALE)){
					throw new Exception(MessageFormat.format(Messages.EmployeeCsvImporter_Error_Gender, Employee.DB_FEMALE, Employee.DB_MALE, line));
				}
				if (gender.trim().toUpperCase().charAt(0) == Employee.DB_FEMALE){
					e.setGender(Employee.DB_FEMALE);
				}else{
					e.setGender(Employee.DB_MALE);
				}
				
				//start employment date
				String start = data[index++];
				if (start == null || start.trim().length() != 10){
					throw new Exception( MessageFormat.format(Messages.EmployeeCsvImporter_Error_StartDateFormat, new Object[]{ DATE_FORMAT, line }));
				}
				try{
					Date dt = format.parse(start);
					e.setStartEmploymentDate(dt);
				}catch (ParseException ex){
					throw new Exception( MessageFormat.format(Messages.EmployeeCsvImporter_Error_StartDateFormat, new Object[]{ DATE_FORMAT, line }));
				}
				if (e.getStartEmploymentDate().before(e.getBirthDate())){
					throw new Exception(MessageFormat.format(Messages.EmployeeCsvImporter_Error_StartAfterEmployeeBirthDate, new Object[]{line}));
				}
					
				//start employments
				String end = data[index++];
				if (end == null || end.trim().length() == 0){
					e.setEndEmploymentDate(null);
				}else {
					if (end.trim().length() != 10){
						throw new Exception(MessageFormat.format(Messages.EmployeeCsvImporter_Error_EndEmployementDateFormat, new Object[]{DATE_FORMAT,line}));
					}
					try{
						Date dt = format.parse(end);
						e.setEndEmploymentDate(dt);
					}catch (ParseException ex){
						throw new Exception(MessageFormat.format(Messages.EmployeeCsvImporter_Error_EndEmployementDateFormat, new Object[]{DATE_FORMAT,line}));
					}
					if (e.getEndEmploymentDate().before(e.getStartEmploymentDate()) || e.getEndEmploymentDate().before(e.getBirthDate())){
						throw new Exception(MessageFormat.format(Messages.EmployeeCsvImporter_Error_EndAfterStartDate, new Object[]{line})); 
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
						warnings.add(MessageFormat.format(Messages.EmployeeCsvImporter_Error_AgencyNotFound, new Object[]{agency,line}));
						e.setAgency(null);
					}else{
						e.setAgency(ag);
						//look for matching rank
						if (rank == null || rank.trim().length() == 0){
							e.setRank(null);
						}else{
							Rank r = findRank(ag, rank);
							if (r == null){
								warnings.add(MessageFormat.format(Messages.EmployeeCsvImporter_Error_RankNotFound, new Object[]{rank, agency,line}));
								e.setRank(null);
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
		}
		
		if (monitor.isCanceled()) return false;
		try{
			session.beginTransaction();
			for (Employee e : employees){
				session.saveOrUpdate(e);
			}
			session.getTransaction().commit();
		}catch (Exception ex){
			throw new Exception(Messages.EmployeeCsvImporter_Error_ParseError + ex.getLocalizedMessage(), ex);
		}
		return true;
	}
	
	@Override
	public List<String> getWarnings(){
		return this.warnings;
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
		try{
			agencies = session.createCriteria(Agency.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.list(); 
		}finally{
			session.getTransaction().commit();
		}
		return agencies;
	}
}
