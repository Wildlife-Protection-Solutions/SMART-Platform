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
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.wcs.smart.internal.Messages;

/**
 * A collection of zip utilities.
 * 
 * @since 1.0.0
 */
public class ZipUtil {

	/**
	 * Path seperator for directory paths. Cannot use
	 * Path.sepeartor here or it will not work when export
	 * on Windows and importing on MAC.
	 */
	public static final String DIR_PATH_SEPERATOR = "/"; //$NON-NLS-1$

	/**
	 * Creates a zip file collecting together
	 * all the data in the provided directories.
	 * 
	 * @param directories directories to include in zip
	 * @param outputZipFile output zip file name
	 * @param monitor progress monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility to call done() on the given monitor
	 * @return <code>true</code> if successful <code>false</code> if error
	 * @throws IOException
	 */
	public static boolean createZip(
			File[] directories, 
			File outputZipFile, 
			IProgressMonitor monitor) throws IOException{
		return createZip(directories, outputZipFile, Collections.emptySet(), monitor);
	}
	
	/**
	 * Creates a zip file collecting together
	 * all the data in the provided directories.
	 * 
	 * @param directories directories to include in zip
	 * @param outputZipFile output zip file name
	 * @param itemsToExclude a set of files to exclude from the backup
	 * @param monitor progress monitor the progress monitor to use for reporting 
	 * progress to the user. It is the caller's responsibility to call done() on the given monitor
	 * @return <code>true</code> if successful <code>false</code> if error
	 * @throws IOException
	 */
	public static boolean createZip(
			File[] directories, 
			File outputZipFile, 
			Set<File> itemsToExclude,			
			IProgressMonitor monitor) throws IOException{

        SubMonitor progress = SubMonitor.convert(monitor, Messages.ZipUtil_Progress_CreatingZip, 100);
        progress.subTask(Messages.ZipUtil_Progress_CreatingZip);
        try (FileOutputStream fOut = new FileOutputStream(outputZipFile);
        	 BufferedOutputStream bOut = new BufferedOutputStream(fOut);
        	ZipArchiveOutputStream tOut = new ZipArchiveOutputStream(bOut);){
            
            progress.setWorkRemaining(directories.length);
            for (int i = 0; i < directories.length; i ++){
            	File f = directories[i];
            	if (!itemsToExclude.contains(f)) {
            		addFileToZip(tOut, directories[i].getAbsoluteFile(), "", itemsToExclude, progress.split(1)); //$NON-NLS-1$
            	}
            }
            
        }
        return true;

	}

	/**
	 * Creates a zip file of the set of files.  
	 * 
	 * 
	 * @param directories directories to include in zip
	 * @param outputZipFile output zip file name
	 * @param itemsToExclude a set of files to exclude from the backup
	 * @param monitor progress monitor the progress monitor to use for reporting 
	 * progress to the user. It is the caller's responsibility to call done() on the given monitor
	 * @return <code>true</code> if successful <code>false</code> if error
	 * @throws IOException
	 */
	public static boolean createZip(
			Collection<File> files,
			File outputZipFile, 			
			IProgressMonitor monitor) throws IOException{

        SubMonitor progress = SubMonitor.convert(monitor, Messages.ZipUtil_Progress_CreatingZip, 100);
        progress.subTask(Messages.ZipUtil_Progress_CreatingZip);
        try (FileOutputStream fOut = new FileOutputStream(outputZipFile);
        	 BufferedOutputStream bOut = new BufferedOutputStream(fOut);
        	ZipArchiveOutputStream tOut = new ZipArchiveOutputStream(bOut);){
            
            progress.setWorkRemaining(files.size());
            for (File f : files){
            	addFileToZip(tOut, f, "", Collections.emptySet(), progress.split(1)); //$NON-NLS-1$
            }
            
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
    		String base, 
			Set<File> itemsToExclude,
			IProgressMonitor monitor) throws IOException {
    	
    	SubMonitor progress = SubMonitor.convert(monitor, 1);
    	progress.subTask( Messages.ZipUtil_Progress_ProcessingFile + path.getAbsolutePath() );
    	try {
	    	String entryName = base + path.getName();
	        progress.checkCanceled();
	       
	        
	        if (path.isFile()) {
	            ZipArchiveEntry zipEntry = new ZipArchiveEntry(path, entryName); 
	            zOut.putArchiveEntry(zipEntry);
	            try(FileInputStream in = new FileInputStream(path)){
	            	IOUtils.copy(in, zOut);
	            }
	            zOut.closeArchiveEntry();
	        }else if (path.isDirectory() && path.list().length == 0){
	        	//empty directory
	    		ZipArchiveEntry zipEntry = new ZipArchiveEntry(entryName + DIR_PATH_SEPERATOR);  
	            zOut.putArchiveEntry(zipEntry);
	            zOut.closeArchiveEntry();
	        } else {
	            File[] children = path.listFiles();
	            if (children != null) {
	            	progress.setWorkRemaining(children.length);
	                for (File child : children) {
	                	if (!itemsToExclude.contains(child)) {
	                		if (!addFileToZip(zOut, child, entryName + DIR_PATH_SEPERATOR, itemsToExclude, progress.split(1))){
	                			return false;
	                		}
	                	}
	                }
	            }else{
	            	throw new IllegalStateException(MessageFormat.format(Messages.ZipUtil_BackupError, new Object[]{path.toString()}));
	            }
	        }
    	}catch (OperationCanceledException ex) {
    		return false;
    	}
        return true;
    }
    
    
    /**
     * @param file  the zip file
     * @param destinationLocation the destination for unzipped file
     * @return
     * @throws Exception  
     */
    public static void unzipFolder(File file,
			File destinationLocation)
			throws Exception {
    	
    	String[] outputZipRootFolder = new String[] { "null" }; //$NON-NLS-1$
    	
		try(ZipFile archiveFile = new ZipFile(file)) {
			byte[] buf = new byte[65536];

			Enumeration<ZipArchiveEntry> entries = archiveFile.getEntries();
			while (entries.hasMoreElements()) {
				ZipArchiveEntry zipEntry = entries.nextElement();
				String name = zipEntry.getName();
				name = name.replace('\\', '/');
				int i = name.indexOf('/');
				if (i > 0) {
					outputZipRootFolder[0] = name.substring(0, i);
				}
				// name = name.substring(i + 1);

				File destinationFile = new File(destinationLocation, name);
				if (name.endsWith("/")) { //$NON-NLS-1$
					if (!destinationFile.isDirectory()
							&& !destinationFile.mkdirs()) {
						throw new Exception(
								Messages.ZipUtil_Error_CreatingTempDir
										+ destinationFile.getPath());
					}
					continue;
				} else if (name.indexOf('/') != -1) {
					// Create the the parent directory if it doesn't exist
					File parentFolder = destinationFile.getParentFile();
					if (!parentFolder.isDirectory()) {
						if (!parentFolder.mkdirs()) {
							throw new Exception(
									Messages.ZipUtil_Error_CreatingTempDir
											+ parentFolder.getPath());
						}
					}
				}

				
				try (FileOutputStream fos = new FileOutputStream(destinationFile)) {
					int n;
					InputStream entryContent = archiveFile
							.getInputStream(zipEntry);
					while ((n = entryContent.read(buf)) != -1) {
						if (n > 0) {
							fos.write(buf, 0, n);
						}
					}
				}
			}
		} catch (IOException e) {
			throw new Exception(Messages.ZipUtil_Error_UnzipFailed + e.getLocalizedMessage(), e);
		}

	}
}
