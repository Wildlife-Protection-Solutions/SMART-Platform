/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.database;

import org.hibernate.Session;
import org.wcs.smart.connect.model.ConservationAreaInfo;

/**
 * Tools for locking and releasing the database for applying 
 * Conservation Area changes.
 * 
 * If users are writing changes to the filestore they should consider locking the database 
 * for the Conservation Area before writing to the datastore, 
 * otherwise if the database is locked from another process the filestore change will 
 * be made but not recorded in the change log table (and thus never replicated out to other users). 
 * This affects all filestore changes include reports, observation & waypoint attachments etc.
 * 
 * @author Emily
 *
 */
public enum LockManager {

	INSTANCE;
	
	public void lockDatabase(Session session, ConservationAreaInfo info) throws Exception{
		Integer lockKey = info.getLockKey();
		session.createNativeQuery("SELECT cast(pg_advisory_lock(" + lockKey + ") as varchar)").uniqueResult(); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void releaseDatabase(Session session, ConservationAreaInfo info ) throws Exception{
		Integer lockKey = info.getLockKey();
		String wasunlocked = (String) session.createNativeQuery("SELECT cast(pg_advisory_unlock(" + lockKey + ") as varchar)").uniqueResult(); //$NON-NLS-1$ //$NON-NLS-2$
		if (!Boolean.valueOf(wasunlocked)) {
			throw new Exception("The database could not be unlocked.  This will cause deadlock issues.  You must restart the database server."); //$NON-NLS-1$
		}
		
	}
}

