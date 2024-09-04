/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.ca;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.util.UuidUtils;


/**
 * This class implements a work around for dealing with a 
 * foreign-key constraint error with derby. When a table has more than 
 * 16 foreign keys linked to it, derby throws a nested trigger
 * error when an item is deleted from the table.
 * 
 * This work around drops all the triggers then re-creates
 * them after the work is done. The dropping and re-creating have
 * to be done in separate transactions so there is a risk here that
 * the triggers can't be re-created and the database ends up with 
 * these fk links.
 * 
 * There are four cases when these are dropped and re-created
 *  1) Icon Property Page (where icons are created/modified/deleted)
 *  2) When a Conservation Area is deleted
 *  3) When a CA is syn'c with Connect
 *  4) When a CA is "recovered" from Connect. 
 * 
 * @author Emily
 * @since 8.0.0
 *
 */
public enum IconFKManager {
	
	INSTANCE;
	
	private List<String> createConstraints = null;
	private List<String> dropConstraints = null;
	private List<String> updateConstraints = null;
	
	/**
	 * Find and store all icon fk information
	 */
	public synchronized void init(Session session) {
		if (createConstraints != null) return;
			
		createConstraints = new ArrayList<>();
		dropConstraints = new ArrayList<>();
		updateConstraints = new ArrayList<>();

		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {

				try (ResultSet rs = c.getMetaData().getExportedKeys(null, "SMART", "ICON")) { //$NON-NLS-1$ //$NON-NLS-2$
					while (rs.next()) {
						while (rs.next()) {
							String schema = rs.getString(6);
							String table = rs.getString(7);
							String field = rs.getString(8);
							String fkname = rs.getString(12);

							if (table.equalsIgnoreCase("iconfile")) //$NON-NLS-1$
								continue;

							String query = "UPDATE " + schema + "." + table + " SET " + field + " = null where " + field //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
									+ " in (:icons)"; //$NON-NLS-1$
							updateConstraints.add(query);

							String create = "ALTER TABLE " + schema + "." + table + " ADD CONSTRAINT " + fkname //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
									+ " FOREIGN KEY (icon_uuid) REFERENCES smart.icon(UUID)  ON DELETE SET NULL ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE"; //$NON-NLS-1$
							createConstraints.add(create);

							String drop = "ALTER TABLE " + schema + "." + table + " DROP CONSTRAINT " + fkname; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							dropConstraints.add(drop);
						}
					}
				}
			}
		});
	}
	
	/**
	 * Drops all icon table FK constraints.
	 * Throws an exception if they can't be dropped.
	 * 
	 * @param session
	 */
	public void dropIconFkConstraints(Session session) {
		init(session);
		
		session.beginTransaction();
		for (String s : dropConstraints) {
			session.createNativeMutationQuery(s).executeUpdate();
		}	
		session.getTransaction().commit();
	}
	
	/**
	 * Creates all foreign key constraints. 
	 * 
	 * @param session
	 */
	public void createIconFkConstraints(Session session) {
		try {
			session.beginTransaction();
			for (String s : createConstraints) {
				session.createNativeMutationQuery(s).executeUpdate();
			}
			session.getTransaction().commit();
		}catch (Exception ex) {
			//TODO: figure out what to do in this case
			
			StringJoiner sj = new StringJoiner(";\n");
			for (String s : createConstraints) {
				sj.add(s);
			}
			
			SmartPlugIn.log("Constarints that could not to be added:\n" + sj.toString(), null);
			SmartPlugIn.log(ex.getMessage(), ex);
			
			String message = """
			Icon constraints could not be re-created in the database. 
			Restore a backup or contact your SMART Administrator.
			If you continue to use this version of SMART your data may become corrupted.   
			""";
			SmartPlugIn.displayLog(message, ex);
			
		}
		
	}
	
	/**
	 * Set all forign key constraints to null - required when deleting an icon 
	 * @param session
	 * @param icons
	 */
	public void setIconFksToNull(Session session, Collection<Icon> icons) {
		
		List<byte[]> iconuuids = new ArrayList<>();
		for (Icon icon : icons) {
			iconuuids.add(UuidUtils.uuidToByte(icon.getUuid()));
		}

		//update all fk references to null;
		for (String update : updateConstraints) {
			session.createNativeMutationQuery(update)
				.setParameterList("icons", iconuuids) //$NON-NLS-1$
				.executeUpdate();					
		}
		
	}
}
