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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import org.hibernate.Session;
import org.hibernate.internal.SessionImpl;
import org.hibernate.jdbc.Work;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.UuidUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;


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
		});
	}
	
	/**
	 * Drops all icon table FK constraints. This should be done in the
	 * same transaction as the deleting of icons. However you have to re-create
	 * them in a NEW transaction.
	 * 
	 * Throws an exception if they can't be dropped.
	 * 
	 * @param session
	 */
	public void dropIconFkConstraints(Session session) {
		init(session);
		
		for (String s : dropConstraints) {
			session.createNativeMutationQuery(s).executeUpdate();
		}	
		
	}
	
	/**
	 * Creates all foreign key constraints. 
	 * 
	 * @param session
	 */
	public void createIconFkConstraints(Session session) {
		try {
			session.beginTransaction();
			session.createNativeMutationQuery("alter table smart.icon drop constraint ICON_CAUUID_FK").executeUpdate(); //$NON-NLS-1$
			for (String s : createConstraints) {
				session.createNativeMutationQuery(s).executeUpdate();
			}
			session.createNativeMutationQuery("alter table smart.icon add constraint ICON_CAUUID_FK foreign key (ca_uuid) references smart.conservation_area(uuid) on update restrict on delete cascade deferrable initially immediate").executeUpdate(); //$NON-NLS-1$
			session.getTransaction().commit();
		}catch (Exception ex) {
			StringJoiner sj = new StringJoiner(";\n"); //$NON-NLS-1$
			for (String s : createConstraints) {
				sj.add(s);
			}
			
			SmartPlugIn.log("Constraints that could not to be added:\n" + sj.toString(), null); //$NON-NLS-1$
			SmartPlugIn.log(ex.getMessage(), ex);
			
			String message = Messages.IconFKManager_IconConstraintErrorApp;
			SmartPlugIn.displayLog(message, ex);
			
		}
		
	}
	
	/**
	 * Set all foreign key constraints to null - required when deleting an icon 
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
	
	/**
	 * Validate the icon foreign key constraints exist as expected and 
	 * try to re-create them if they don't
	 * 
	 */
	public void validateIconFk() {
		
		try(Session session = HibernateManager.openSession()){
			
			Set<String> tables = new HashSet<>();
			Set<String> missingIconFk = new HashSet<>();
			
			session.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {

					try (ResultSet rs = c.getMetaData().getExportedKeys(null, "SMART", "ICON")) { //$NON-NLS-1$ //$NON-NLS-2$
						while (rs.next()) {
							String schema = rs.getString(6);
							String table = rs.getString(7);
							tables.add((schema + "." + table).toLowerCase()); //$NON-NLS-1$
						}
					}
				}
			});	
								
			try (EntityManager em = session.getSessionFactory().createEntityManager()){			
				for (EntityType<?> entityType : em.getMetamodel().getEntities()) {
					if (entityType.getBindableJavaType().equals(IconFile.class)) continue;
					for (SingularAttribute<?, ?> sa : entityType.getSingularAttributes()) {
						if(sa.getBindableJavaType().equals(Icon.class)) {
							Object entityExample = null;
							try {
								entityExample = entityType.getBindableJavaType().getConstructor().newInstance();
							} catch (ReflectiveOperationException e) {
								throw new RuntimeException(e);
							}
							EntityPersister p = em.unwrap(SessionImpl.class).getEntityPersister(null, entityExample);
							
							if (p instanceof AbstractEntityPersister) {
								AbstractEntityPersister info = (AbstractEntityPersister) p;
								String tablename = info.getRootTableName().toLowerCase();
								
								if (!tables.contains(tablename)) {
									missingIconFk.add(tablename);
								}
							}
							
						}
						
					}
				}
			}
			
			if (!missingIconFk.isEmpty()) {
				StringJoiner sj = new StringJoiner(";\n"); //$NON-NLS-1$
				for (String s : missingIconFk) sj.add(s);
				
				SmartPlugIn.log("The following tables are missing icon fk constraints, attempting to re-create these constraints now:\n" + sj.toString(), null); //$NON-NLS-1$
				
				try {
					session.beginTransaction();
					
					session.createNativeMutationQuery("alter table smart.icon drop constraint ICON_CAUUID_FK").executeUpdate(); //$NON-NLS-1$
					for (String s : missingIconFk) {
						String tableonly = s.substring(s.indexOf('.')+1);
						String constraint = "alter table " + s + " add constraint " + tableonly + "_iconuuidfk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(UUID)  ON DELETE SET NULL ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						SmartPlugIn.log(constraint, null);
						session.createNativeMutationQuery(constraint).executeUpdate();
					}
					session.createNativeMutationQuery("alter table smart.icon add constraint ICON_CAUUID_FK foreign key (ca_uuid) references smart.conservation_area(uuid) on update restrict on delete cascade deferrable initially immediate").executeUpdate(); //$NON-NLS-1$
					session.getTransaction().commit();
				}catch (Exception ex) {
					SmartPlugIn.log("Icon constarints that could not to be added.", null); //$NON-NLS-1$
					SmartPlugIn.log(ex.getMessage(), ex);
					
					String message = Messages.IconFKManager_IconConstraintErrorRestart;
					SmartPlugIn.displayLog(message, ex);
				}
			}
		}
	}
}
