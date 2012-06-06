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
package org.wcs.smart.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A collection of zip utilities
 * 
 * @since 1.0.0
 */
public class ZipUtil {

	public static boolean createZip(
			File[] directories, 
			File outputZipFile, 
			IProgressMonitor monitor) throws IOException{
		
		FileOutputStream fOut = null;
        BufferedOutputStream bOut = null;
        ZipArchiveOutputStream tOut = null;
 
        
        try {
            fOut = new FileOutputStream(outputZipFile);
            bOut = new BufferedOutputStream(fOut);
            tOut = new ZipArchiveOutputStream(bOut);
            for (int i = 0; i < directories.length; i ++){
            	addFileToZip(tOut, directories[i], directories[i].getName(), monitor);	
            }
            
        } finally {
            tOut.finish();
            tOut.close();
            bOut.close();
            fOut.close();
        }
        return true;
 
	}
	
	/**
     * Creates a zip entry for the path specified with a name built from the base passed in and the file/directory
     * name. If the path is a directory, a recursive call is made such that the full directory is added to the zip.
     *
     * @param zOut The zip file's output stream
     * @param path The filesystem path of the file/directory being added
     * @param base The base prefix to for the name of the zip file entry
     *
     * @throws IOException If anything goes wrong
     */
    private static boolean addFileToZip(ZipArchiveOutputStream zOut, 
    		File path, 
    		String base, IProgressMonitor monitor) throws IOException {
        
    	monitor.subTask("processing: " + path.getAbsolutePath());
    	
        String entryName = base + path.getName();
        ZipArchiveEntry zipEntry = new ZipArchiveEntry(path, entryName);
 
        zOut.putArchiveEntry(zipEntry);
        
        if(monitor.isCanceled()){
        	return false;
        }
        if (path.isFile()) {
            IOUtils.copy(new FileInputStream(path), zOut);
            zOut.closeArchiveEntry();
        } else {
            zOut.closeArchiveEntry();
            File[] children = path.listFiles();
 
            if (children != null) {
                for (File child : children) {
                    if (!addFileToZip(zOut, child, entryName + "/", monitor)){
                    	return false;
                    }
                    if (monitor.isCanceled()){
                    	return false;
                    }
                }
            }
        }
        return true;
    }
}
