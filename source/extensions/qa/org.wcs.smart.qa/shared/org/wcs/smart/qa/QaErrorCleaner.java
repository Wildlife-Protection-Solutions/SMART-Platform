/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.qa.model.QaError;

/**
 * Tools for cleaning results from the QaError table
 * @author Emily
 *
 */
public enum QaErrorCleaner {
	
	INSTANCE;

	/**
	 * Deletes all QaErrors from the database that are not 
	 * of ERROR state
	 */
	public void cleanItems(ConservationArea ca) throws Exception{
		Session s = HibernateManager.openSession();
		try{
			s.getTransaction().begin();
			Query q = s.createQuery("DELETE FROM QaError WHERE conservationArea = :ca and status != :status"); //$NON-NLS-1$
			q.setParameter("ca", ca); //$NON-NLS-1$
			q.setParameter("status", QaError.Status.NEW); //$NON-NLS-1$
			q.executeUpdate();
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			throw ex;
		}finally{
			s.close();
		}
	}
}
