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

import org.apache.commons.io.FileUtils;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.util.SmartUtils;

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
	
	public static File createTempDirectory() throws IOException {
		final File temp;
		temp = File.createTempFile("cybertracker", Long.toString(System.nanoTime())); //$NON-NLS-1$
		if(!(temp.delete())) {
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath()); //$NON-NLS-1$
		}
		if(!(temp.mkdir())) {
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath()); //$NON-NLS-1$
		}
		return temp;
	}	

	public static String getRegistryKey(ConservationArea ca) {
		return ICyberTrackerConstants.REG_KEY_SMART + SmartUtils.encodeHex(ca.getUuid());
	}

	public static String getFilestore(ConservationArea ca) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		return WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER,
				ICyberTrackerConstants.REG_KEY_PATH, getRegistryKey(ca));
	}
	
	public static void updateRegistryKey(ConservationArea ca) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, IOException {
		String filestoreStr = SmartProperties.getInstance().getProperty(SmartProperties.PROP_FILESTORE);
		filestoreStr = filestoreStr + File.separator + SmartUtils.getDirectoryPath(ca.getUuid()) + File.separator + ICyberTrackerConstants.SMART_CTX_DOWNLOAD_FOLDER;
		File filestore = new File(filestoreStr);
		if (!filestore.exists())
			filestore.mkdirs();

		WinRegistry.writeStringValue(WinRegistry.HKEY_CURRENT_USER, ICyberTrackerConstants.REG_KEY_PATH,
				getRegistryKey(ca), filestore.getCanonicalPath());
	}

	public static void deleteTempDirectory(File tempDir) {
		if (tempDir == null)
			return;
		try {
			FileUtils.deleteDirectory(tempDir);
		} catch (IOException e) {
			//ignore
			e.printStackTrace();
		}
	}	
	
}
