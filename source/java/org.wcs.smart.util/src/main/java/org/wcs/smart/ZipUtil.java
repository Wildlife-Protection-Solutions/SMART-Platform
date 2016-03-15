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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * A collection of zip utilities.
 * 
 * @since 1.0.0
 */
public class ZipUtil {

	/**
	 * Creates a zip file collecting together
	 * all the data in the provided directories.
	 * 
	 * @param directories directories to include in zip
	 * @param outputZipFile output zip file name
	 * @param monitor progress monitor
	 * @return <code>true</code> if successful <code>false</code> if error
	 * @throws IOException
	 */
	public static boolean createZip(
			File[] directories, 
			File outputZipFile) throws IOException{
		
		FileOutputStream fOut = null;
        BufferedOutputStream bOut = null;
        ZipArchiveOutputStream tOut = null;
 
        
        try {
            fOut = new FileOutputStream(outputZipFile);
            bOut = new BufferedOutputStream(fOut);
            tOut = new ZipArchiveOutputStream(bOut);
           
            
            for (int i = 0; i < directories.length; i ++){
            	if (directories[i].isDirectory() && directories[i].list().length == 0){
            		//empty directory; create an empty file as a placeholder
            		ZipArchiveEntry zipEntry = new ZipArchiveEntry(directories[i].getName()); 
                    tOut.putArchiveEntry(zipEntry);
                    //IOUtils.copy(new FileInputStream(path), zOut);
                    tOut.closeArchiveEntry();
            	}else{
            		addFileToZip(tOut,directories[i].getAbsoluteFile(), ""); //$NON-NLS-1$
            	}
            }
            
        } finally {
        	if (tOut != null){
        		tOut.finish();
        		tOut.close();
        	}
            if (bOut != null){
            	bOut.close();
            }
            if (fOut != null){
            	fOut.close();
            }
        }
        return true;
 
	}
	
	/**
     * Creates a zip entry for the path specified with a name built 
     * from the base passed in and the file/directory
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
    		String base) throws IOException {
    	
    	
    	String entryName = base + path.getName();
       
        if (path.isFile()) {
        	
            ZipArchiveEntry zipEntry = new ZipArchiveEntry(path, entryName); 
            zOut.putArchiveEntry(zipEntry);
            FileInputStream in = new FileInputStream(path);
            IOUtils.copy(in, zOut);
            in.close();
            zOut.closeArchiveEntry();
            
        } else {
            File[] children = path.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!addFileToZip(zOut, child, entryName +"/")){ // File.separator
                    	return false;
                    }
                }
            }else{
            	throw new IllegalStateException("Zip error");
            }
        }
        return true;
    }

}
