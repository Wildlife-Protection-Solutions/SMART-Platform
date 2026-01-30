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
import java.util.Properties;

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
	
	private int RESECHEDULE_HOURS = 48;
	private String SERVER = null;
		
	private TelemetryDispatcherJob() {
		super("telementry data uploader"); //$NON-NLS-1$
		super.setSystem(true);
		init();
	}
	
	private void init() {
		Properties prop = new Properties();
		try {
			prop.load(getClass().getResourceAsStream(SmartProperties.TELEMETRY_PROPERTIES));
		} catch (Exception e) {
			SmartPlugIn.log("Error determining telemetry properties.", e); //$NON-NLS-1$
			return;
		}
		try {
			RESECHEDULE_HOURS = Integer.parseInt(prop.getProperty("reschedule_hours")); //$NON-NLS-1$
		}catch (Exception ex) {
			SmartPlugIn.log("Error reading property from telementry file.", ex); //$NON-NLS-1$
		}
		SERVER = prop.getProperty("server"); //$NON-NLS-1$

	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if (!TelemetryManager.INSTANCE.canUploadStats()) return Status.OK_STATUS;
		LocalDateTime last = TelemetryManager.INSTANCE.getLastUploaded();
		
		if (last != null && LocalDateTime.now().minusHours(RESECHEDULE_HOURS).isBefore(last)) {
			//reschedule for 48 hours from now
			return reschedule(monitor);
		}
				
		if (SERVER == null) {
			SmartPlugIn.log("Telementry server not configured. Missing telemetry.properties file?.", null); //$NON-NLS-1$
			return Status.OK_STATUS;
		}
		
		//we have to do this as the admin user
		SmartPlugIn.log("Computing Telemetry Data", null); //$NON-NLS-1$
		String jsonData = null;
		SmartDB.DbUser currentUser = SmartDB.getCurrentUser();
		try (Session session = 
			HibernateManager.lockDatabase(SmartDB.DbUser.ADMIN.getUserName(), SmartDB.DbUser.ADMIN.getPassword())){
			
			try {
				jsonData = TelemetryManager.INSTANCE.packageData(session, false);
			}catch (Exception ex) {
				SmartPlugIn.log("Unable to compute telemetry data:" + ex.getMessage(), ex); //$NON-NLS-1$
				return reschedule(monitor);
			}
		
		}catch (Exception ex) {
			//try again in one hour, this is likely a result of something else running in the database
			SmartPlugIn.log("Unable to lock database as admin user for telemetry collection:" + ex.getMessage(), ex); //$NON-NLS-1$
			return reschedule(1, monitor);
		}finally {
			HibernateManager.unlockDatabase(currentUser.getUserName(), currentUser.getPassword());	
		}
		
		// find the end point
		try {
			SmartPlugIn.log("Uploading Telemetry Data", null); //$NON-NLS-1$

			URI enpoint = new URI(SERVER);
			HttpURLConnection connection = (HttpURLConnection) enpoint.toURL().openConnection();
			try {
				connection.setRequestMethod("POST"); //$NON-NLS-1$
				connection.setRequestProperty("Content-Type", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$
				connection.setDoOutput(true);
				String ua = String.format("org.wcs.smart/%s (%s; Java %s; installKey=%s)", //$NON-NLS-1$
						System.getProperty("org.wcs.smart.version.simple"), //$NON-NLS-1$
						System.getProperty("os.name"), //$NON-NLS-1$
						System.getProperty("java.version"), //$NON-NLS-1$
						TelemetryManager.INSTANCE.getInstallKey());
				connection.setRequestProperty("User-Agent", ua); //$NON-NLS-1$
			        
				byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
				connection.setFixedLengthStreamingMode(input.length);
				try (OutputStream os = connection.getOutputStream()) {
					os.write(input);
				}

				int code = connection.getResponseCode();
				if (code == 200) {
					TelemetryManager.INSTANCE.setLastUploaded(LocalDateTime.now());
				} else {
					SmartPlugIn.log(
							MessageFormat.format("Could not upload telemetry data. Response code: {0} Message: {1}", //$NON-NLS-1$
									code, connection.getResponseMessage()),
							null);
				}
			} finally {
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
