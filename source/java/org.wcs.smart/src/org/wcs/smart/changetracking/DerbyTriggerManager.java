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

import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Tools for managing DERBY database triggers.
 * 
 * @author Emily
 *
 */
public enum DerbyTriggerManager {
    INSTANCE;
   
    /**
     * Determines if a given trigger exists by querying in the systriggers table.
     * 
     * @param name trigger name with schema (smart.trigger_abc)
     * @param s current session
     * @return true if trigger exists, false otherwise
     */
	public boolean triggerExists(String name, Session s){
		Query q = s.createSQLQuery("SELECT count(*) FROM SYS.SYSTRIGGERS trj, SYS.SYSSCHEMAS sc WHERE trj.schemaid = sc.schemaid AND sc.schemaname || '.' || UPPER(triggername) = ?"); //$NON-NLS-1$
		q.setString(0, name.toUpperCase());
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
		s.createSQLQuery(sql).executeUpdate();
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
		s.createSQLQuery("DROP TRIGGER " + name).executeUpdate(); //$NON-NLS-1$
		return true;
	}
}
