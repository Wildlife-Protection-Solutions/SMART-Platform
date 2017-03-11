/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2;

import java.util.UUID;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;

/**
 * Query manager for intelligence queries.
 * 
 * 
 * @author Emily
 *
 */
public enum QueryManager {

	INSTANCE;
	
	/**
	 * Returns the query deleted from the database if a query is deleted; otherwise
	 * returns null
	 * @param queryUuid
	 * @return
	 */
	public IntelRecordObservationQuery deleteQuery(UUID queryUuid){
		IntelRecordObservationQuery removed = null;
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			removed = (IntelRecordObservationQuery) s.get(IntelRecordObservationQuery.class, queryUuid);
			if (removed == null) throw new Exception(Messages.QueryManager_NotFoundError);
			
			Query wsQuery = s.createQuery("DELETE FROM IntelWorkingSetQuery WHERE id.query = :query"); //$NON-NLS-1$
			wsQuery.setParameter("query", removed); //$NON-NLS-1$
			wsQuery.executeUpdate();
			
			s.delete(removed);
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog(Messages.QueryManager_DeleteError + ex.getMessage(), ex);
			return null;
		}finally{
			s.close();
		}
		
		return removed;
	}
}
