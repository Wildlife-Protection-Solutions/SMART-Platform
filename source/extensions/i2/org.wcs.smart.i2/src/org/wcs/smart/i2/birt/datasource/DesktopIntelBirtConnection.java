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
package org.wcs.smart.i2.birt.datasource;

import java.util.Collection;

import org.hibernate.Session;
import org.wcs.smart.birt.BirtConstants;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * BIRT Connection for intelligence datasets 
 * @author Emily
 *
 */
public class DesktopIntelBirtConnection extends AbstractIntelBirtConnection {
	
	private boolean cleanup;
	
	
	public void openSession(){
		cleanup = false;
		if (appContext != null){
			localSession = (Session) appContext.get(BirtConstants.SESSION_PARAM);
		}
		if (localSession == null){
			cleanup = true;
			localSession = HibernateManager.openSession();
			localSession.beginTransaction();
		}
	}
	
	public void closeSession(){
		if (cleanup){
			localSession.getTransaction().rollback();
			localSession.close();
		}
	}

	public Collection<ConservationArea> getConservationAreas() {
		return SmartDB.getConservationAreaConfiguration().getConservationAreas();
	}

	

}