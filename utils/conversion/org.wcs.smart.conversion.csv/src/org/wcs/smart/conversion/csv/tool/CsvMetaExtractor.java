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
package org.wcs.smart.conversion.csv.tool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.wcs.smart.conversion.model.MappedAttribute;
import org.wcs.smart.conversion.model.MappedAttributeType;
import org.wcs.smart.conversion.model.SmartMapping;
import org.wcs.smart.conversion.tool.MatchSession;
import org.wcs.smart.conversion.tool.PatrolBuilder;
import org.wcs.smart.conversion.tool.TeamMembersParser;
import org.wcs.smart.patrol.xml.model.PatrolMemberType;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class CsvMetaExtractor {
	
	private Connection c;
	private SmartMapping mapping;
	
	public CsvMetaExtractor(MatchSession session) {
		this(session.getConnection(), session.getSmartMapping());
	}
	
	public CsvMetaExtractor(Connection c, SmartMapping mapping) {
		this.c = c;
		this.mapping = mapping;
	}

	public boolean exportMembers(File file) {
		Set<String> members = new HashSet<String>();
		TeamMembersParser membersParser = new TeamMembersParser();
		
		for (MappedAttribute cta : mapping.getMappedAttribute()) {
			if (MappedAttributeType.META_MEMBERS.equals(cta.getType())) {
				try {
					ResultSet rs = c.createStatement().executeQuery("select id from csv_to_smart.ATTRIBUTES where n = '" + cta.getI() + "'");  //$NON-NLS-1$//$NON-NLS-2$
					//NOTE: rs MUST be of size 1
					while (rs.next()) {
						String id = rs.getString(1);
						ResultSet dataSet = c.createStatement().executeQuery("select distinct a"+id+" from csv_to_smart.CSV"); //$NON-NLS-1$ //$NON-NLS-2$
						while (dataSet.next()) {
							String str = dataSet.getString(1);
							if (str != null) {
								members.addAll(membersParser.parseMembers(str));
							}
						}
					}
				} catch (SQLException e) {
					System.err.println("Error extracting employees i=" + cta.getI());
					e.printStackTrace();
					return false;
				}
			}
		}
		
		try (CSVWriter writer = new CSVWriter(
				new OutputStreamWriter(new FileOutputStream(file), "UTF-8"), //$NON-NLS-1$ 
				',', '"',System.getProperty("line.separator"))) {  //$NON-NLS-1$

			// WriteHeaders
			String[] headerColumns = new String[] {"ID","GIVEN NAME","FAMILY NAME","BIRTHDATE","GENDER","START EMPLOYMENT","END EMPLOYMENT","AGENCY","RANK"};
			writer.writeNext(headerColumns);

			//for each row write one record
			for (String emp : members) {
				String csvout[] = new String[headerColumns.length];
				PatrolMemberType member = PatrolBuilder.toMember(emp);
//				csvout[0] = null;
				csvout[1] = member.getGivenName();
				csvout[2] = member.getFamilyName();
				csvout[3] = "1950-01-01";
				csvout[4] = "M";
				csvout[5] = "1970-01-01";
				writer.writeNext(csvout);
			}
			writer.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean exportMandates(File file) {
		Set<String> mandates = new HashSet<String>();
		
		for (MappedAttribute cta : mapping.getMappedAttribute()) {
			if (MappedAttributeType.META_MANDATE.equals(cta.getType())) {
				try {
					ResultSet rs = c.createStatement().executeQuery("select id from csv_to_smart.ATTRIBUTES where n = '" + cta.getI() + "'");  //$NON-NLS-1$//$NON-NLS-2$
					//NOTE: rs MUST be of size 1
					while (rs.next()) {
						String id = rs.getString(1);
						ResultSet dataSet = c.createStatement().executeQuery("select distinct a"+id+" from csv_to_smart.CSV"); //$NON-NLS-1$ //$NON-NLS-2$
						while (dataSet.next()) {
							String str = dataSet.getString(1);
							if (str != null) {
								mandates.add(str);
							}
						}
					}
				} catch (SQLException e) {
					System.err.println("Error extracting mandates i=" + cta.getI());
					e.printStackTrace();
					return false;
				}
			}
		}
		
		try (CSVWriter writer = new CSVWriter(
				new OutputStreamWriter(new FileOutputStream(file), "UTF-8"), //$NON-NLS-1$ 
				',', '"',System.getProperty("line.separator"))) {  //$NON-NLS-1$

			//for each row write one record
			for (String m : mandates) {
				String csvout[] = new String[1];
				csvout[0] = m;
				writer.writeNext(csvout);
			}
			writer.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean exportTransects(File file) {
		List<String[]> transectData = new ArrayList<>();
		
		//lists below must be of a same size as inner array in transectData
		List<MappedAttributeType> attributes = new ArrayList<>();
		List<String> ids = new ArrayList<>();
		List<String> colNames = new ArrayList<>();

		for (MappedAttribute cta : mapping.getMappedAttribute()) {
			MappedAttributeType type = cta.getType();
			if (type == null) {
				continue;
			}
			switch (type) {
			case TRANSECT_ID:
			case TRANSECT_START_LAT:
			case TRANSECT_START_LON:
			case TRANSECT_END_LAT:
			case TRANSECT_END_LON: {
				try {
					if (attributes.contains(type)) {
						System.out.println(MessageFormat.format("WARN: More than one mapping present for {0}. Column {1} will be ignored.", type, cta.getI()));
						break;
					}
					attributes.add(type);
					colNames.add(cta.getN() != null ? cta.getN() : cta.getI());
					ResultSet rs = c.createStatement().executeQuery("select id from csv_to_smart.ATTRIBUTES where n = '" + cta.getI() + "'");  //$NON-NLS-1$//$NON-NLS-2$
					//NOTE: rs MUST be of size 1
					if (rs.next()) {
						String id = rs.getString(1);
						ids.add(id);
					}
				} catch (SQLException e) {
					System.err.println("Error extracting transects i=" + cta.getI());
					e.printStackTrace();
					return false;
				}
				break;
			}
			default:
				break;
			}
		}
		
		int outSize = attributes.size();
//		if (outSize == 0) {
//			//no transects to export
//			return true;
//		}
		
		try {
			StringBuilder whatClause = new StringBuilder();
			for (String id : ids) {
				if (whatClause.length() > 0) {
					whatClause.append(", "); //$NON-NLS-1$
				}
				whatClause.append("a").append(id); //$NON-NLS-1$
			}
			ResultSet dataSet = c.createStatement().executeQuery("select distinct "+whatClause+" from csv_to_smart.CSV"); //$NON-NLS-1$ //$NON-NLS-2$
			while (dataSet.next()) {
				String[] data = new String[outSize];
				for (int i = 0; i < outSize; i++) {
					data[i] = dataSet.getString(i+1);
				}
				transectData.add(data);
			}
		} catch (SQLException e) {
			System.err.println("Error extracting distinct transects data");
			e.printStackTrace();
			return false;
		}

		try (CSVWriter writer = new CSVWriter(
				new OutputStreamWriter(new FileOutputStream(file), "UTF-8"), //$NON-NLS-1$ 
				',', '"',System.getProperty("line.separator"))) {  //$NON-NLS-1$

			// write headers
			writer.writeNext(colNames.toArray(new String[colNames.size()]));
			
			//for each row write one record
			for (String[] data : transectData) {
				writer.writeNext(data);
			}
			writer.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}	

}
