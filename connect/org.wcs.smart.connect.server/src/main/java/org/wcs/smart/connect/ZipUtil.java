/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;
import org.wcs.smart.connect.datastore.DataStoreManager;

/**
 * A collection of zip utilities.
 * 
 * @since 1.0.0
 */
public class ZipUtil {

	public static Path createTemporaryDirectory(){
		try {
			Path baseDir = DataStoreManager.INSTANCE.getTemporaryDirectory();
			String basename = "smart_" + Long.toString(System.nanoTime()); //$NON-NLS-1$
			
			for (int i = 0; i < 1000; i ++){
				Path tempDir = baseDir.resolve(basename + "_"+ i); //$NON-NLS-1$
				if (!Files.exists(tempDir)) {
					Files.createDirectories(tempDir);
					return tempDir;
				}
			}
		}catch(IOException ex) {
			throw new IllegalStateException("Could not create temporary directory", ex); //$NON-NLS-1$
		}
		throw new IllegalStateException("Could not create temporary directory"); //$NON-NLS-1$
		
	}
	
	public static boolean createZip(List<Path> inputs, Path outputZipFile) throws IOException{
		  try (FileOutputStream fOut = new FileOutputStream(outputZipFile.toFile());
				  BufferedOutputStream bOut = new BufferedOutputStream(fOut);
		       	ZipArchiveOutputStream tOut = new ZipArchiveOutputStream(bOut);){
		       
			for (Path p : inputs) {
				addFileToZip(tOut, p, ""); //$NON-NLS-1$
			}
		            
		  }
		   return true;
	}
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
			Path[] directories, 
			Path outputZipFile) throws IOException{

        
        try (OutputStream fOut = Files.newOutputStream(outputZipFile);
        	 BufferedOutputStream bOut = new BufferedOutputStream(fOut);
        	ZipArchiveOutputStream tOut = new ZipArchiveOutputStream(bOut);){
            
            for (int i = 0; i < directories.length; i ++){
            	addFileToZip(tOut,directories[i], ""); //$NON-NLS-1$
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
    		Path path, 
    		String base) throws IOException {
    	
    	String entryName = base + path.getFileName().toString();
    	if (!Files.exists(path)) {
    		//assume empty directory and ignore
    	}else if (!Files.isDirectory(path)) {
            ZipArchiveEntry zipEntry = new ZipArchiveEntry(path.toAbsolutePath().normalize().toFile(), entryName); 
            zOut.putArchiveEntry(zipEntry);
            try(InputStream in = Files.newInputStream(path)){
            	IOUtils.copy(in, zOut);
            }
            zOut.closeArchiveEntry();
        }else if (Files.isDirectory(path) && Files.list(path).count() == 0){
        	//empty directory
    		ZipArchiveEntry zipEntry = new ZipArchiveEntry(entryName + File.separator); 
            zOut.putArchiveEntry(zipEntry);
            zOut.closeArchiveEntry();
        } else {
        	List<Path> kids = Files.list(path).collect(Collectors.toList());
            for (Path child : kids) {
				if (!addFileToZip(zOut, child, entryName + File.separator)) {
					return false;
				}
            }

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
    	
    	
    	String[] outputZipRootFolder = new String[] { "null" }; //$NON-NLS-1$
    	
		try (ZipFile archiveFile = new ZipFile(file.toAbsolutePath().normalize().toFile())){
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

				Path destinationFile = destinationLocation.resolve(name);
				Path parent = destinationFile.getParent();
				if (name.endsWith("/")) { //$NON-NLS-1$
					parent = destinationFile;
				}
				Files.createDirectories(parent);

				if (!Files.isDirectory(destinationFile)) {
					try (InputStream is = archiveFile.getInputStream(zipEntry)){
						Files.copy(is, destinationFile);
					}
				}
			}
		} catch (IOException e) {
			throw new Exception("Unzip failed: " + e.getLocalizedMessage(), e); //$NON-NLS-1$
		} 

	}
}
