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
package org.wcs.smart.gpx;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.internal.Messages;

/**
 * Class to interface with GPS babel to read
 * data from GPS devices.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class GPSBabel {

	public static final String DEFAULT_DEVICE_TYPE = "garmin"; //$NON-NLS-1$
	
	private static final String ERROR_MSG_COULD_NOT_IMPORT = Messages.GPSBabel_Error_CouldNotImport;

	private static final String ERROR_MSG_PROCESS_TOLONG = Messages.GPSBabel_Error_ProcessTerminatedTooLong;

	/**
	 * GPS Babel arguments for getting format list
	 */
	private static final String FORMAT_ARGS = " -\"^3\""; //$NON-NLS-1$

	/**
	 * GPS Bable process timeout
	 */
	private static final long IMPORT_TIMEOUT_MSEC = 3 * 60  * 1000;  //3 min
	
	/**
	 * 
	 * 
	 * @return the location of the gps babel program
	 */
	private static String getGpsBabelLocation(){
		String location = SmartProperties.getInstance().getProperty(SmartProperties.PROP_GPS_BABEL);
		return new File(location).getAbsolutePath();
	}
	
	
	/**
	 * Reads the device options from GPS Babbel.
	 * <p>Will only return items that
	 * device type 'serial' and can read both
	 * tracks and waypoints.
	 * </p>
	 * 
	 * @return map of gps babel key to display name
	 * @throws IOException if error occurs
	 */
	public static HashMap<String, String> getDeviceOptions() throws IOException {
		final HashMap<String, String> options = new HashMap<String, String>();

		CommandLine cmdLine = CommandLine.parse("\"" + getGpsBabelLocation() + "\"" + FORMAT_ARGS); //$NON-NLS-1$ //$NON-NLS-2$
		DefaultExecutor exec = new DefaultExecutor();

		exec.setStreamHandler(new ExecuteStreamHandler() {
			private BufferedReader inputReader;

			@Override
			public void stop() {
				try {
					inputReader.close();
				} catch (IOException e) {
					SmartPlugIn.log(Messages.GPSBabel_Error_ReadingBabelOps, e);
				}
			}

			@Override
			public void start() throws IOException {
				String line = null;
				while ((line = inputReader.readLine()) != null) {
					String bits[] = line.split("\t"); //$NON-NLS-1$
					if (bits[0].trim().toLowerCase().equals("serial")) { //$NON-NLS-1$
						if (bits[1].trim().toLowerCase().matches("r.r...")) { //$NON-NLS-1$
							options.put(bits[2], bits[4]);
						}
					}
				}
			}

			@Override
			public void setProcessOutputStream(InputStream arg0)
					throws IOException {
				this.inputReader = new BufferedReader(new InputStreamReader(
						arg0));
			}

			@Override
			public void setProcessInputStream(OutputStream arg0)
					throws IOException {
			}

			@Override
			public void setProcessErrorStream(InputStream arg0)
					throws IOException {
			}
		});

		// ensure the process does not run for more than 30 seconds 
		ExecuteWatchdog watchdog = new ExecuteWatchdog(30000);
		exec.setWatchdog(watchdog);
		
		exec.execute(cmdLine);
		return options;
	}
	
	/**
	 * Trys to read data from the gps device and write it to a temporary file.
	 * Will first try the usb then com1 then fail.
	 * <p>The caller of this function is responsible for deleting the temporary file.</p>
	 * @param deviceType the type of device.
	 * @param types waypoint or tracks
	 * @return A gpx file of the data imported from the device.  May return null if file not created.
	 * @throws IOException
	 */
	public static File getData(String deviceType, Set<GPSDataImport.ImportType> types) throws IOException{
		
		//create temporary file
		File file = File.createTempFile("smart_import", ".gpx"); //$NON-NLS-1$ //$NON-NLS-2$
		
		//CommandLine cmdLine = CommandLine.parse(getGpsBabelLocation());
		CommandLine cmdLine = new CommandLine(getGpsBabelLocation());
		if (types.contains(GPSDataImport.ImportType.WAYPOINT)){
			cmdLine.addArgument("-w"); //$NON-NLS-1$
		}
		if (types.contains(GPSDataImport.ImportType.TRACK)){
			cmdLine.addArgument("-t"); //$NON-NLS-1$
		}
		
		cmdLine.addArgument("-i"); //$NON-NLS-1$
		cmdLine.addArgument(deviceType);
		
		cmdLine.addArgument("-f"); //$NON-NLS-1$
		cmdLine.addArgument("usb:"); //$NON-NLS-1$
		cmdLine.addArgument("-o"); //$NON-NLS-1$
		cmdLine.addArgument("gpx,gpxver=1.1"); //$NON-NLS-1$
		cmdLine.addArgument("-F"); //$NON-NLS-1$
		cmdLine.addArgument(file.getAbsolutePath());
		SmartPlugIn.logInfo("Running: " + cmdLine.toString()); //$NON-NLS-1$
		
		DefaultExecutor exec = new DefaultExecutor();
		// 2 minute timeout
		ExecuteWatchdog watchdog = new ExecuteWatchdog(IMPORT_TIMEOUT_MSEC);
		exec.setWatchdog(watchdog);
		int exitValue = -1;
		try{
			exitValue = exec.execute(cmdLine);
		}catch (Exception ex){
			if (exec.isFailure(exitValue) && watchdog.killedProcess()){
				throw new IllegalStateException(ERROR_MSG_PROCESS_TOLONG);
			}
			throw new RuntimeException(ERROR_MSG_COULD_NOT_IMPORT + ex.getLocalizedMessage() , ex);
		}

		if (!file.exists()){
			//try com1 port
			cmdLine = CommandLine.parse(getGpsBabelLocation());
			if (types.contains(GPSDataImport.ImportType.WAYPOINT)){
				cmdLine.addArgument("-w"); //$NON-NLS-1$
			}
			if (types.contains(GPSDataImport.ImportType.TRACK)){
				cmdLine.addArgument("-t"); //$NON-NLS-1$
			}
			cmdLine.addArgument("-i"); //$NON-NLS-1$
			cmdLine.addArgument(deviceType);
			
			cmdLine.addArgument("-f"); //$NON-NLS-1$
			cmdLine.addArgument("COM1"); //$NON-NLS-1$
			cmdLine.addArgument("-o"); //$NON-NLS-1$
			cmdLine.addArgument("gpx,gpxver=1.1"); //$NON-NLS-1$
			cmdLine.addArgument("-F"); //$NON-NLS-1$
			cmdLine.addArgument(file.getAbsolutePath());
			
			exec = new DefaultExecutor();
			
			watchdog = new ExecuteWatchdog(IMPORT_TIMEOUT_MSEC);
			exec.setWatchdog(watchdog);
			try{
				exitValue = exec.execute(cmdLine);
			}catch (Exception ex){
				if (exec.isFailure(exitValue) && watchdog.killedProcess()){
					throw new IllegalStateException(ERROR_MSG_PROCESS_TOLONG);
				}
				throw new RuntimeException(ERROR_MSG_COULD_NOT_IMPORT + ex.getLocalizedMessage() , ex);
			}
			
			if (!file.exists()){
				//cannot import for whatever reason
				return null;
			}
		}
		
		return file;

	}

}
