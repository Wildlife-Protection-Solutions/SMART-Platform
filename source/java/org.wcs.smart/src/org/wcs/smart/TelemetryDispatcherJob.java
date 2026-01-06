/*
 * Copyright (C) 2026 Wildlife Conservation Society
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
package org.wcs.smart;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

import com.ibm.icu.text.MessageFormat;

public class TelemetryDispatcherJob extends Job{

	public static TelemetryDispatcherJob INSTANCE = new TelemetryDispatcherJob();
	
	private static final int RESECHEDULE_HOURS = 48;
	
	private TelemetryDispatcherJob() {
		super("telementry data uploader"); //$NON-NLS-1$
		super.setSystem(true);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if (!TelemetryManager.INSTANCE.isEnabled()) return Status.OK_STATUS;
		
		LocalDateTime last = TelemetryManager.INSTANCE.getLastUploaded();
		
		if (last != null && LocalDateTime.now().minusHours(RESECHEDULE_HOURS).isBefore(last)) {
			//reschedule for 48 hours from now
	        // Reschedule this job to run again after 48 hours
			return reschedule(monitor);
		}
				
		
		//we have to do this as the admin user
		//TODO: lock database?
		SmartPlugIn.log("Computing Telemetry Data", null); //$NON-NLS-1$
		String jsonData = null;
		SmartDB.DbUser currentUser = SmartDB.getCurrentUser();
		try (Session session = 
			HibernateManager.lockDatabase(SmartDB.DbUser.ADMIN.getUserName(), SmartDB.DbUser.ADMIN.getPassword())){
			
			try {
				jsonData = TelemetryManager.INSTANCE.packageData(session, false);
			}catch (Exception ex) {
				SmartPlugIn.log("Unable to compute telemetry data:" + ex.getMessage(), ex);
				return reschedule(monitor);
			}
		
		}catch (Exception ex) {
			//try again in one hour, this is likely a result of something else running in the database
			SmartPlugIn.log("Unable to lock database as admin user for telemetry collection:" + ex.getMessage(), ex);
			return reschedule(1, monitor);
		}finally {
			HibernateManager.unlockDatabase(currentUser.getUserName(), currentUser.getPassword());	
		}
		
		
		//find the end point
		try {
			SmartPlugIn.log("Uploading Telemetry Data", null); //$NON-NLS-1$
			
			//TODO: replace this end point
			URI enpoint = new URI("http://localhost:5000/telemetry");
			HttpURLConnection connection = (HttpURLConnection)enpoint.toURL().openConnection();
			try {
				connection.setRequestMethod("POST"); //$NON-NLS-1$
				connection.setRequestProperty("Content-Type", "application/json; utf-8"); //$NON-NLS-1$ //$NON-NLS-2$
		        connection.setDoOutput(true);
	
		        try (OutputStream os = connection.getOutputStream()) {
		            byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
		            os.write(input, 0, input.length);
		        }
	
		        int code = connection.getResponseCode();
		        if (code == 200) {
		        	TelemetryManager.INSTANCE.setLastUploaded(LocalDateTime.now());    	
		        }else {
		        	SmartPlugIn.log(MessageFormat.format("Could not upload telemetry data. Response code: {0} Message: {1}", code, connection.getResponseMessage()), null); //$NON-NLS-1$
		        }
			}finally {
				connection.disconnect();
			}
	        
		} catch (Exception e) {
			SmartPlugIn.log("Error uploading telemetry data:" + e.getMessage(), e); //$NON-NLS-1$
		}
				
		return reschedule(monitor);		
		
	}
	
	private IStatus reschedule(IProgressMonitor monitor) {		
		return this.reschedule(RESECHEDULE_HOURS, monitor);		
	}
	
	private IStatus reschedule(long delay_hrs, IProgressMonitor monitor) {
		if (monitor.isCanceled()) return Status.CANCEL_STATUS;
		long delay = delay_hrs * 60L * 60L * 1000L; // hours in ms
		this.schedule(delay);
        return Status.OK_STATUS;
	}

}
