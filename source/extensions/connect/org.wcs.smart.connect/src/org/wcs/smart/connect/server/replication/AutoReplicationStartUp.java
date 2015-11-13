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
package org.wcs.smart.connect.server.replication;

import org.hibernate.Session;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerOption;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Manager to manage auto replication.  This is
 * run on startup and enables auto 
 * replication is required.
 * 
 * @author Emily
 *
 */
public enum AutoReplicationStartUp {

	INSTANCE;
	
	public void onStartUp(){
		Session s = HibernateManager.openSession();
		try{
			ConnectServer cs = ConnectHibernateManager.getConnectServer(s);
			if (cs == null) return;
			
			if (cs.getOptionAsBoolean(ConnectServerOption.Option.SYNC_AUTOMATICALLY)){
				//start auto sync job
				int delay = cs.getOptionAsInt(ConnectServerOption.Option.SYNC_MINUTE);
				enableAutoReplication(delay);
			}
		}finally{
			s.close();
		}
	}
	
	/**
	 * Starts the background auto replication job after the given
	 * delay.
	 * @param delayMinutes delay in mintues
	 */
	public void enableAutoReplication(int delayMinutes){
		long delaysec = delayMinutes * 60 * 1000l;
		AutoReplicationJob autoReplication = new AutoReplicationJob();
		autoReplication.schedule(delaysec);
	}
}
