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
package org.wcs.smart;

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IResolve;
import net.refractions.udig.catalog.IService;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class SmartPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart"; //$NON-NLS-1$

	// The shared instance
	private static SmartPlugIn plugin;
	
	/**
	 * Image descriptor key for smart employee
	 */
	public static final String SMART_EMPLOYEE_ICON = "org.wsc.smart.SMART_EMPLOYEE"; //$NON-NLS-1$

	/**
	 * Image descriptor key for non-smart user employee
	 */
	public static final String EMPLOYEE_ICON = "org.wsc.smart.EMPLOYEE"; //$NON-NLS-1$

	static {

	}

	
	/**
	 * The constructor
	 */
	public SmartPlugIn() {
		

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		try {
			//TODO: review this requirement
			// clean out the catalog
			List<IResolve> members = CatalogPlugin.getDefault().getLocalCatalog().members(null);
			for (IResolve member : members) {
				CatalogPlugin.getDefault().getLocalCatalog().remove((IService) member.resolve(IService.class, null));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static SmartPlugIn getDefault() {
		return plugin;
	}

	public static void log(String message, Throwable t){
		int status = t instanceof Exception || message != null ? IStatus.ERROR : IStatus.WARNING;
        getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, message, t));
	}
	
	public static void logInfo(String message){
		getDefault().getLog().log(new Status(IStatus.OK, PLUGIN_ID, IStatus.INFO, message, null));
	}
	

	
	/**
	 * Displays an error message to the user and logs the
	 * message.
	 * 
	 * @param message  Error message to display
	 * @param t exception to log
	 */
	public static void displayLog(Shell currentShell, String message, Throwable t){
		log(message, t);
		if (currentShell == null){
			if (Display.getCurrent() != null && Display.getCurrent().getActiveShell() != null){
				currentShell = Display.getCurrent().getActiveShell();
			}
		}
		MessageDialog.openError(currentShell, "Error", message);
	}
	
	/**
	 * Displays an error message to the user, logs the error and
	 * exits the program with an error code of 1.
	 * 
	 * @param message the message to display to the user
	 * @param t optionally exception
	 */
	public static void displayLogExit(String message, Throwable t){
		log(message, t);
		MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", message);
		System.exit(1);
	}
	
	
	public static String lookupLocaleName(String nl){
		String[] bits = nl.split("_");
		for (int i = 0; i < Locale.getAvailableLocales().length;i++){
			Locale l  = Locale.getAvailableLocales()[i];
			if (bits[0].equals(l.getLanguage()) && 
					bits[1].equals(l.getCountry())){
				return l.getDisplayName();
			}
		}
		return null;
		
	}
	
	/**
	 * Converts a datetime widget to a date object only setting year, month and day.
	 * The hour, minute, second and millisecond are all set to 0;
	 * 
	 */
	public static Date getDate(DateTime dt){
		Calendar calendar = new GregorianCalendar();
		calendar.set(Calendar.YEAR, dt.getYear());
		calendar.set(Calendar.MONTH, dt.getMonth());
		calendar.set(Calendar.DAY_OF_MONTH, dt.getDay());
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		return calendar.getTime();
	}
	
	
	/**
	 * Gets only the date part of a given date.  Sets the time to 0 is not endOfDay; sets
	 * the time to 23:59:59 if end of day.
	 * @param dt
	 * @return date only date
	 */
	public static Date getDatePart(Date date, boolean endOfDay){
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		if (!endOfDay){
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
		}else{
			calendar.set(Calendar.HOUR_OF_DAY, 23);
			calendar.set(Calendar.MINUTE, 59);
			calendar.set(Calendar.SECOND, 59);
			calendar.set(Calendar.MILLISECOND, 0);
		}
		
		return calendar.getTime();
	}
	
	/**
	 * Gets only the date part of a given date.  Sets the time to 0.
	 * @param dt
	 * @return date only date
	 */
	public static Date combineDateTime(Date date, Time time){
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		Calendar calendar2 = new GregorianCalendar();
		calendar2.setTime(time);
		
		calendar.set(Calendar.HOUR_OF_DAY, calendar2.get(Calendar.HOUR_OF_DAY));
		calendar.set(Calendar.MINUTE, calendar2.get(Calendar.MINUTE));
		calendar.set(Calendar.SECOND, calendar2.get(Calendar.SECOND));
		calendar.set(Calendar.MILLISECOND, calendar2.get(Calendar.MILLISECOND));
		
		return calendar.getTime();
	}
	
	/**
	 * Converts a datetime widget to a date object only setting hour, minute, seconde
	 */
	public static Date getTime (DateTime dt){
		Calendar calendar  = new GregorianCalendar();
		calendar.setTimeInMillis(0);
		calendar.set(Calendar.HOUR_OF_DAY, dt.getHours());
		calendar.set(Calendar.MINUTE, dt.getMinutes());
		calendar.set(Calendar.SECOND, dt.getSeconds());
		return calendar.getTime();
	}
	
	public static Date getMidnight ( ){
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis(0);
		cal.set(Calendar.MILLISECOND,0);
		cal.set(Calendar.SECOND,0);
		cal.set(Calendar.HOUR_OF_DAY,0);
		cal.set(Calendar.MINUTE,0);
		return cal.getTime();
	}
	/**
	 * converts a Date to a Calendar object
	 */
	public static GregorianCalendar convertDate(Date d){
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(d);
		return calendar;
		
	}
	
	public static String getDirectoryPath(byte[] uuid){
		return Arrays.toString(uuid).replaceAll(", ", "").replace("[", "").replace("]", "");
	}
	
	public static boolean createDirectory(File dir){
		try{
			FileUtils.forceMkdir(dir);
			return true;
		}catch (IOException ex){
			SmartPlugIn.displayLog(null, "Could not create directory.  Attachments will not be imported.", ex);
		}
		return false;
	}
	
	public static boolean copyFile(File from, File to){
		//if (true) return false;
		try {
			FileUtils.copyFile(from, to);
			return true;
		} catch (IOException e) {
			SmartPlugIn.displayLog(null, "Could not copy file " + from.getAbsolutePath() + " to " + to.getAbsolutePath() + ". ", e);
		}
		return false;
	}
}
