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
package org.wcs.smart.cybertracker;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.wcs.smart.cybertracker.internal.Messages;

/**
 * Interface for interacting with command line tools for exporting and importing
 * data from cybertracker devices.
 * 
 * @author Emily
 *
 */
public class MobileDeviceUtils {

	private static final int ERROR_BAD_COMMAND_LINE = 100;
	private static final int ERROR_DEVICE_NOT_FOUND = 101;
	private static final int ERROR_DEVICE_NOT_CONNECTED = 102;
	
	private static final String MPTCOPY_EXE = "mtpcopy.exe"; //$NON-NLS-1$
	
	/**
	 * Folder containing SMART Mobile exported data files.
	 */
	public static final String DATA_FOLDER = "SMARTdata"; //$NON-NLS-1$
	
	/**
	 * If exe has been extracted for this session or not
	 */
	private static boolean isExeExtracted = false;
	
	/**
	 * Export application to device.  Installs to root folder
	 * 
	 * @param packageFile
	 * @return
	 * @throws Exception
	 */
	public static void exportAppToDevice(Path packageFile, String targetFilename) throws Exception{
		if (!CyberTrackerPlugIn.getDefault().isWindows()) {
			throw new Exception(Messages.MobileDeviceUtils_OsNotSupported);
		}
		
		Path exe = null;
		try {
			exe = extractExe();
		}catch (Exception ex) {
			throw new Exception(Messages.MobileDeviceUtils_ExtractError, ex);
		}
		
		//auto load works from the root direction not the data folder
		List<String> items = new ArrayList<>();
		items.add(exe.toString());
		items.add("/upload"); //$NON-NLS-1$
		items.add(packageFile.toString());
//		items.add("\\" + DATA_FOLDER + "\\" + targetFilename); //$NON-NLS-1$ //$NON-NLS-2$
		items.add("\\" + targetFilename); //$NON-NLS-1$ 
		
		ProcessBuilder pb = new ProcessBuilder(items);
		Process p = pb.start();
		
		int exitCode = p.waitFor();
		try {
			generateError(exitCode);
		}catch (Exception ex) {
			CyberTrackerPlugIn.log(MessageFormat.format("Failed to copy package to device.  Exit code: {0} Command:{1} ",exitCode,items), null); //$NON-NLS-1$
			throw ex;
		}
	}
	
	/**
	 * Export APK to device
	 * @param apk
	 * @return
	 * @throws Exception
	 */
	public static void exportApkToDevice(Path apk) throws Exception{
		if (!CyberTrackerPlugIn.getDefault().isWindows()) {
			throw new Exception(Messages.MobileDeviceUtils_OsNotSupported);
		}
		
		Path exe = null;
		try {
			exe = extractExe();
		}catch (Exception ex) {
			throw new Exception(Messages.MobileDeviceUtils_ExtractError, ex);
		}
		
		List<String> items = new ArrayList<>();
		items.add(exe.toString());
		items.add("/upload"); //$NON-NLS-1$
		items.add(apk.toString());
		items.add("\\" + apk.getFileName().toString()); //$NON-NLS-1$
		
		ProcessBuilder pb = new ProcessBuilder(items);
		Process p = pb.start();
		
		int exitCode = p.waitFor();
		
		try {
			generateError(exitCode);
		}catch (Exception ex) {
			CyberTrackerPlugIn.log(MessageFormat.format("Failed to copy apk to device.  Exit code: {0} Command:{1} ",exitCode,items), null); //$NON-NLS-1$
			throw ex;
		}
	}
	
	private static void generateError(int exitCode) throws Exception{
		if (exitCode == ERROR_BAD_COMMAND_LINE) {
			throw new Exception(Messages.MobileDeviceUtils_BadCommandErr);
		}else if (exitCode == ERROR_DEVICE_NOT_FOUND) {
			throw new Exception(Messages.MobileDeviceUtils_DeviceNotFoundErr);
		}else if (exitCode == ERROR_DEVICE_NOT_CONNECTED) {
			throw new Exception(Messages.MobileDeviceUtils_DeviceNotConnectedErr);
		}else if (exitCode != 0) {
			throw new Exception(MessageFormat.format(Messages.MobileDeviceUtils_CommunicationError, exitCode));
		}
	}
	
	 /**
	  * Import files from device to given path, deletes data on device after
	  * copied off device.
	  * @param target
	  * @return
	  * @throws Exception
	  */
	public static void importFromDevice(Path target) throws Exception{
		if (!CyberTrackerPlugIn.getDefault().isWindows()) {
			throw new Exception(Messages.MobileDeviceUtils_OsNotSupported);
		}
			
		Path exe = null;
		try {
			exe = extractExe();
		}catch (Exception ex) {
			throw new Exception(Messages.MobileDeviceUtils_ExtractError, ex);
		}
			
		List<String> items = new ArrayList<>();
		items.add(exe.toString());
		items.add("/deleteAfterDownload"); //$NON-NLS-1$
		items.add("/download"); //$NON-NLS-1$
		items.add("\\" + DATA_FOLDER + "\\*.json"); //$NON-NLS-1$ //$NON-NLS-2$
		items.add(target.toString());
			
		ProcessBuilder pb = new ProcessBuilder(items);
		Process p = pb.start();
			
		int exitCode = p.waitFor();
		try {
			generateError(exitCode);
		}catch (Exception ex) {
			CyberTrackerPlugIn.log(MessageFormat.format("Failed to download data from device.  Exit code: {0} Command:{1} ", exitCode, items), null); //$NON-NLS-1$
			throw ex;
		}
	}
	
	private static Path extractExe() throws Exception{
		URL bundleFile = CyberTrackerPlugIn.getDefault().getBundle().getEntry("ext/" + MPTCOPY_EXE); //$NON-NLS-1$
		Path tempFile = CyberTrackerPlugIn.getDefault().getBundle().getDataFile("/ext/" + MPTCOPY_EXE).toPath(); //$NON-NLS-1$
		
		System.out.println(tempFile.toString());
		
		if (isExeExtracted && Files.exists(tempFile)) return tempFile;
		
		//extract exe to temp location
		if (!Files.exists(tempFile.getParent())) Files.createDirectories(tempFile.getParent());
		try(InputStream is = bundleFile.openStream()){
			Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING );
		}
		isExeExtracted = true;
		
		return tempFile;
	}
}
