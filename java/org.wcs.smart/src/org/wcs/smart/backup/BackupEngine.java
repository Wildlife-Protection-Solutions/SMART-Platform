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
package org.wcs.smart.backup;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.util.ZipUtil;

/**
 * Engine responsible for backing up the SMART system.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class BackupEngine {

	/**
	 * @return the default backup file name based on the current date
	 */
	public static String getDefaultFileName(){
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		return "SMART_" + format.format(new Date()) + ".db.bak.zip"; 
	}
	
	/**
	 * Backs up all SMART data to the given output file.
	 * 
	 * @param outputFile output file
	 * @param monitor progress monitor
	 * @return <code>true</code> if backup successful, <code>false</code> if cancelled or failed
	 * @throws IOException
	 */
	public static boolean  backupSystem(File outputFile, IProgressMonitor monitor) throws IOException{
		if (outputFile.exists()){
			if (!outputFile.delete()){
				throw new IllegalStateException("Output file '" + outputFile.getAbsolutePath() + "' could not be deleted.");
			}
		}
		
		File filestore = new File (SmartProperties.getInstance().getProperty(SmartProperties.FILESTORE_KEY));
		File database = new File (SmartProperties.getInstance().getProperty(SmartProperties.SMART_DB_KEY));
		
		File[] dirsToBackup = new File[]{filestore, database};
		
		monitor.beginTask("Backing Up Database and Files", 2);
		
		return ZipUtil.createZip(dirsToBackup, outputFile, monitor);
	}
}
