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
package org.wcs.smart.cybertracker.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.util.SmartFileUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Class containing utility methods to work with PDA + CyberTracker application.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PdaUtil {

	public static String getCTAppPath() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		return WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER,
				ICyberTrackerConstants.REG_KEY_PATH, ICyberTrackerConstants.REG_KEY_NAME);
	}

	public static String getCTMediaFolder() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
//		String path = System.getProperty("user.home"); //$NON-NLS-1$
//		return path + "\\Documents\\CyberTracker\\ExportMedia\\"; //$NON-NLS-1$
		return WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER,
				ICyberTrackerConstants.REG_KEY_PATH, ICyberTrackerConstants.REG_KEY_EXPORT_MEDIA);
	}
	
	public static File createTempDirectory() throws IOException {
		return SmartFileUtils.createTempDirectory("cybertracker"); //$NON-NLS-1$
	}	

	public static String getRegistryKey(ConservationArea ca) {
		return ICyberTrackerConstants.REG_KEY_SMART + UuidUtils.uuidToString(ca.getUuid());
	}

//	public static String getFilestoreFromRegistry(ConservationArea ca) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
//		return WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER,
//				ICyberTrackerConstants.REG_KEY_PATH, getRegistryKey(ca));
//	}

	private static String getCyberTrackerFolder(ConservationArea ca) {
		return ca.getFileDataStoreLocation() + File.separator + ICyberTrackerConstants.SMART_CTX_DOWNLOAD_FOLDER;
	}
	
	public static File getDowloadFolder(ConservationArea ca) {
		return new File(getCyberTrackerFolder(ca));
	}

	public static File getStorageFolder(ConservationArea ca) {
		String dir = getCyberTrackerFolder(ca) + File.separator + ICyberTrackerConstants.SMART_CTX_STORAGE_FOLDER;
		return new File(dir);
	}
	
	public static void updateRegistryKey(ConservationArea ca) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, IOException {
		File folder = getDowloadFolder(ca);
		if (!folder.exists())
			folder.mkdirs();

		WinRegistry.writeStringValue(WinRegistry.HKEY_CURRENT_USER, ICyberTrackerConstants.REG_KEY_PATH,
				getRegistryKey(ca), folder.getCanonicalPath());
	}

	public static void deleteRegistryKey(ConservationArea ca) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		WinRegistry.deleteValue(WinRegistry.HKEY_CURRENT_USER, ICyberTrackerConstants.REG_KEY_PATH, getRegistryKey(ca));
	}
	
	public static int uploadPda(File file) throws Exception {
		String appPath = getCTAppPath();
		String[] uploadCommands = {appPath, ICyberTrackerConstants.COMMAND_SILENT, ICyberTrackerConstants.COMMAND_UPLOAD, file.getAbsolutePath()};
		Process proc = Runtime.getRuntime().exec(uploadCommands);
		int code = proc.waitFor();
		return code;
	}
	
}
