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

package org.wcs.smart.ui.internal.userlog;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.LoginLogEntry;
import org.wcs.smart.hibernate.HibernateManager;

public enum LoginLogModelProvider {
    INSTANCE;

    private List<LoginLogEntry> entries;

    private LoginLogModelProvider() {
    	entries = new ArrayList<LoginLogEntry>();
        // get the login entries from the database
    	try(Session s = HibernateManager.openSession()){	
			Query<LoginLogEntry> query = s.createQuery("FROM LoginLogEntry ORDER BY loginTimestamp", LoginLogEntry.class); //$NON-NLS-1$
			entries = query.getResultList();
		}

    }

    public List<LoginLogEntry> getLog() {
        return entries;
    }

}
