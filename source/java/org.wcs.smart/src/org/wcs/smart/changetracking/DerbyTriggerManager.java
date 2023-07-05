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
package org.wcs.smart.changetracking;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

/**
 * Tools for managing DERBY database triggers.
 * 
 * @author Emily
 *
 */
public enum DerbyTriggerManager {
    INSTANCE;
   
	
	/**
	 * Add trigger that removes items from the change log when replication is not enabled for the 
	 * ca.
	 * @param session
	 */
	public void addChangeLogTableTrigger(Session session) {
		session.createNativeMutationQuery("CREATE TRIGGER trg_change_log_insert AFTER INSERT ON smart.connect_change_log REFERENCING NEW AS new FOR EACH ROW WHEN (smart.is_replication_enabled_ca(new.ca_uuid) = false) delete from smart.connect_change_log where uuid = new.uuid"). //$NON-NLS-1$
		executeUpdate();	
	}
	
	/**
	 * Remove the trigger that removes items from the change log when replication is not enabled for the 
	 * ca. We do this in a few select places were data is imported. 
	 * @param session
	 */
	public void removeChangeLogTableTrigger(Session session) {
		session.createNativeMutationQuery("DROP TRIGGER trg_change_log_insert").executeUpdate(); //$NON-NLS-1$
	}
    /**
     * Determines if a given trigger exists by querying in the systriggers table.
     * 
     * @param name trigger name with schema (smart.trigger_abc)
     * @param s current session
     * @return true if trigger exists, false otherwise
     */
	public boolean triggerExists(String name, Session s){
		NativeQuery<Integer> q = s.createNativeQuery("SELECT count(*) FROM SYS.SYSTRIGGERS trj, SYS.SYSSCHEMAS sc WHERE trj.schemaid = sc.schemaid AND sc.schemaname || '.' || UPPER(triggername) = UPPER(:triggerName)", Integer.class); //$NON-NLS-1$
		q.setParameter("triggerName", name); //$NON-NLS-1$
		Integer cnt = (Integer)q.uniqueResult();
		if (cnt.longValue() == 0) return false;
		return true;	
	}
	
	/**
	 * Creates a given trigger if it does not already exist.
	 * 
	 * @param name the trigger name
	 * @param sql the sql to create the trigger
	 * @param s the current session
	 * @return true if created false otherwise
	 */
	public boolean createTriggerIfNotExists(String name, String sql, Session s){
		if (triggerExists(name, s)) return false;
		try {
			s.createNativeMutationQuery(sql).executeUpdate();
		}catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}
		return true;
	}
	
	/**
	 * Drops a given trigger if it exists.
	 * 
	 * @param name the trigger name
	 * @param sql the sql to create the trigger
	 * @param s the current session
	 * @return true if dropped false otherwise
	 */
	public boolean dropIfExists(final String name, Session s){
		if (!triggerExists(name, s)) return false;
		s.createNativeMutationQuery("DROP TRIGGER " + name).executeUpdate(); //$NON-NLS-1$
		return true;
	}
}
