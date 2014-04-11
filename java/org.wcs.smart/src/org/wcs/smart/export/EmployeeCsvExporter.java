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
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;

import au.com.bytecode.opencsv.CSVWriter;

import com.ibm.icu.text.SimpleDateFormat;

/**
 * Exporter for exporting agencies and ranks data to csv file.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class EmployeeCsvExporter implements ICsvDataExporter {

	@Override
	public boolean exportCsvFile(File file, char delimiter, ConservationArea ca, boolean headers, IProgressMonitor monitor, Session session) throws Exception {
		CSVWriter writer = null;
		try {
			writer = new CSVWriter(new FileWriter(file), delimiter, '"',SmartUtils.LINE_SEPARATOR);
			if (headers) {
				// WriteHeaders
				//String[] headerCols = {"ID", "Given Name", "Family Name", "Birth Date", "Gender", "Start Employement Date", "End Employement Date", "Agency", "Rank"};
				String[] headerCols = {"ID","GIVEN NAME","FAMILY NAME","BIRTHDATE","GENDER","START EMPLOYMENT","END EMPLOYMENT","AGENCY","RANK"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
				writer.writeNext(headerCols);
			}

			List<Employee> employeeList = getEmployees(ca, session);
			SimpleDateFormat dateFormat = new SimpleDateFormat(EmployeeCsvImporter.DATE_FORMAT);
			for (Employee employee : employeeList) {
				if (monitor.isCanceled()) return false;
				String[] data = new String[9];
				
				data[0] = employee.getId();
				data[1] = employee.getGivenName();
				data[2] = employee.getFamilyName();
				data[3] = dateFormat.format(employee.getBirthDate());
				data[4] = String.valueOf(employee.getGender());
				data[5] = dateFormat.format(employee.getStartEmploymentDate());
				Date endDate = employee.getEndEmploymentDate();
				data[6] = endDate == null ? null : dateFormat.format(endDate);
				Agency agency = employee.getAgency();
				if (agency != null){
					data[7] = agency.findName(ca.getDefaultLanguage());
				}else{
					data[7] = null;
				}
				
				Rank rank = employee.getRank();
				if (rank != null){
					data[8] = rank.findName(ca.getDefaultLanguage());
				}else{
					data[8] = null;
				}
				
				writer.writeNext(data);
			}
			
			writer.close();
			return true;
		} catch (Exception ex) {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				return false;
			}
			return false;
		}
	}

	
	private List<Employee> getEmployees(ConservationArea ca, Session session) {
		session.beginTransaction();
		try{
			return HibernateManager.getAllEmployees(ca, session);
		}finally{
			session.getTransaction().rollback();
		}
	}
	
}
