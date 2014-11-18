package org.wcs.smart.ct2smart.patrol;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.wcs.smart.ct2smart.matcher.CsvMatchFileBuilder;
import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeType;
import org.wcs.smart.ct2smart.parser.TeamMembersParser;
import org.wcs.smart.ct2smart.ui.MatchSession;
import org.wcs.smart.patrol.xml.model.PatrolMemberType;
import org.wcs.smart.util.SmartUtils;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class CsvMetaExtractor {
	
	private MatchSession session;
	
	public CsvMetaExtractor(MatchSession session) {
		this.session = session;
	}

	public boolean exportMembers(File file) {
		Set<String> members = new HashSet<String>();
		TeamMembersParser membersParser = new TeamMembersParser();
		Connection c = session.getConnection();
		
		for (Ct2Attribute cta : session.getCt2Smart().getCt2Attribute()) {
			if (Ct2AttributeType.META_MEMBERS.equals(cta.getType())) {
				try {
					ResultSet rs = c.createStatement().executeQuery("select id from ct_to_smart.ATTRIBUTES where i = '" + cta.getI() + "'");  //$NON-NLS-1$//$NON-NLS-2$
					//NOTE: rs MUST be of size 1
					while (rs.next()) {
						String id = rs.getString(1);
						ResultSet dataSet = c.createStatement().executeQuery("select distinct a"+id+" from ct_to_smart.CSV"); //$NON-NLS-1$ //$NON-NLS-2$
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
		
		CSVWriter writer = null;
		try {
			writer = new CSVWriter(
					new OutputStreamWriter(new FileOutputStream(file), "UTF-8"), //$NON-NLS-1$ 
					',', '"',SmartUtils.LINE_SEPARATOR); 

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
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean exportMandates(File file) {
		Set<String> mandates = new HashSet<String>();
		Connection c = session.getConnection();
		
		for (Ct2Attribute cta : session.getCt2Smart().getCt2Attribute()) {
			if (Ct2AttributeType.META_MANDATE.equals(cta.getType())) {
				try {
					ResultSet rs = c.createStatement().executeQuery("select id from ct_to_smart.ATTRIBUTES where i = '" + cta.getI() + "'");  //$NON-NLS-1$//$NON-NLS-2$
					//NOTE: rs MUST be of size 1
					while (rs.next()) {
						String id = rs.getString(1);
						ResultSet dataSet = c.createStatement().executeQuery("select distinct a"+id+" from ct_to_smart.CSV"); //$NON-NLS-1$ //$NON-NLS-2$
						while (dataSet.next()) {
							String str = dataSet.getString(1);
							if (str != null) {
								if (CsvMatchFileBuilder.isCtId(str)) {
									ResultSet elSet = c.createStatement().executeQuery("select n from ct_to_smart.ELEMENT where i = '" + str + "'"); //$NON-NLS-1$ //$NON-NLS-2$
									//NOTE: elSet MUST be of size 1
									while (elSet.next()) {
										String n = elSet.getString(1);
										mandates.add(n);
									}									
								} else {
									mandates.add(str);
								}
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
		
		CSVWriter writer = null;
		try {
			writer = new CSVWriter(
					new OutputStreamWriter(new FileOutputStream(file), "UTF-8"), //$NON-NLS-1$ 
					',', '"',SmartUtils.LINE_SEPARATOR); 

			//for each row write one record
			for (String m : mandates) {
				String csvout[] = new String[1];
				csvout[0] = m;
				writer.writeNext(csvout);
			}
			writer.close();
		} catch (IOException ex) {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
}
