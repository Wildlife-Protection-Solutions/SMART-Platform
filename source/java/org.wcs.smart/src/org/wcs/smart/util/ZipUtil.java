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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

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
			Path[] directories, 
			Path outputZipFile, 
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
			Path[] directories, 
			Path outputZipFile, 
			Set<Path> itemsToExclude,			
			IProgressMonitor monitor) throws IOException{

        SubMonitor progress = SubMonitor.convert(monitor, Messages.ZipUtil_Progress_CreatingZip, 100);
        progress.subTask(Messages.ZipUtil_Progress_CreatingZip);
        try (OutputStream fOut =Files.newOutputStream(outputZipFile);
        	 BufferedOutputStream bOut = new BufferedOutputStream(fOut);
        	ZipOutputStream tOut = new ZipOutputStream(bOut);){
            
            progress.setWorkRemaining(directories.length);
            for (int i = 0; i < directories.length; i ++){
            	Path f = directories[i];
            	if (!itemsToExclude.contains(f)) {
            		addFileToZip(tOut, directories[i], "", itemsToExclude, progress.split(1)); //$NON-NLS-1$
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
			Collection<Path> files,
			Path outputZipFile, 			
			IProgressMonitor monitor) throws IOException{

        SubMonitor progress = SubMonitor.convert(monitor, Messages.ZipUtil_Progress_CreatingZip, 100);
        progress.subTask(Messages.ZipUtil_Progress_CreatingZip);
        try (OutputStream fOut = Files.newOutputStream(outputZipFile);
        	 BufferedOutputStream bOut = new BufferedOutputStream(fOut);
        	ZipOutputStream tOut = new ZipOutputStream(bOut);){
            
            progress.setWorkRemaining(files.size());
            for (Path f : files){
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
    private static boolean addFileToZip(ZipOutputStream zOut, 
    		Path path, 
    		String base, 
			Set<Path> itemsToExclude,
			IProgressMonitor monitor) throws IOException {
    	
    	SubMonitor progress = SubMonitor.convert(monitor, 1);
    	progress.subTask( Messages.ZipUtil_Progress_ProcessingFile + path.toAbsolutePath().toString() );
    	try {
	    	String entryName = base + path.getFileName().toString();
	        progress.checkCanceled();
	       
	        
	        if (!Files.exists(path)) {
	    		//assume empty directory and ignore
	        }else if (!Files.isDirectory(path)) {
	            ZipEntry zipEntry = new ZipEntry(entryName); 
	            zOut.putNextEntry(zipEntry);
	            try(InputStream in = Files.newInputStream(path)){
	            	IOUtils.copy(in, zOut);
	            }
	            zOut.closeEntry();
	            
	        }else {
	        	List<Path> kids = null;
	        	try(Stream<Path> stream = Files.list(path)){
	        		kids = stream.collect(Collectors.toList());
	        	}
	        
	        	if (Files.isDirectory(path) && kids.size() == 0) {
	        		//empty directory
		        	ZipEntry zipEntry = new ZipEntry(entryName + DIR_PATH_SEPERATOR);  
		            zOut.putNextEntry(zipEntry);
		            zOut.closeEntry();
	        	}else {
	        		progress.setWorkRemaining(kids.size());
	        		for (Path child : kids) {
	        			if (!itemsToExclude.contains(child)) {
	        				if (!addFileToZip(zOut, child, entryName + DIR_PATH_SEPERATOR, itemsToExclude, progress.split(1))){
	        					return false;
	        				}
	        			}
	        		}
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
    public static void unzipFolder(Path file,
			Path destinationLocation)
			throws Exception {
    	ZipUtilCommon.unzipFolder(file, destinationLocation);

	}
    
    /**
     * Writes a single to a zip file. If the zip file exists it will add it
     * as an entry to the existing file.
     * 
     * @param zipFile the zip file name
     * @param filecontents the contents to write, each array should contain the {filename, file contents}
     * 
     * @throws IOException
     */
    public static void writeToZip(Path zipFile, String[]... filecontents) throws IOException {
    	 try (OutputStream fOut = Files.newOutputStream(zipFile);
	        	BufferedOutputStream bOut = new BufferedOutputStream(fOut);
	        	ZipOutputStream tOut = new ZipOutputStream(bOut);){
    		 
    		 for (String[] item: filecontents) {
    			 String fname = item[0];
    			 String content = item[1];
        		 ZipEntry zipEntry = new ZipEntry(fname); 
    	         tOut.putNextEntry(zipEntry);
    	         tOut.write(content.getBytes(StandardCharsets.UTF_8));
    	         tOut.closeEntry();
    		 }

    	 }
    }
    
    
    /**
	 * Unzips the content of the zip
	 * file to a temporary directory.
	 * 
	 * @param zipFile the zip file to unzip
	 * @return the location of the files
	 * @throws Exception
	 */
	public static Path unzip(Path zipFile) throws Exception{
		Path tempDir = null;
		try(ZipFile zout = new ZipFile(zipFile.toAbsolutePath().toFile())) {
			tempDir = Files.createTempDirectory("smart_"+Long.toString(System.nanoTime())); //$NON-NLS-1$
			Files.delete(tempDir);
			SmartUtils.createDirectory(tempDir);
		
			Enumeration<? extends ZipEntry> elements = zout.entries();
			while(elements.hasMoreElements()){
				ZipEntry entry = elements.nextElement();
				
				Path fout = tempDir.resolve(entry.getName());
				if (entry.isDirectory()){
					SmartUtils.createDirectory(fout);
				}else{
					SmartUtils.createDirectory(fout.getParent());
					try(InputStream is = zout.getInputStream(entry)){
						Files.copy(is, fout);
					}
				}
			}	
		}
		return tempDir;
	}	
}
