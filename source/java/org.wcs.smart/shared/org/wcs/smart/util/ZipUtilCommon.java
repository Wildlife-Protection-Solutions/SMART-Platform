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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A collection of zip utilities.
 * 
 * @since 1.0.0
 */
public class ZipUtilCommon {

    
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
    	
		try(ZipFile archiveFile = new ZipFile(file.normalize().toAbsolutePath().toFile())) {
			Enumeration<? extends ZipEntry> entries = archiveFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
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
					try (InputStream in = archiveFile.getInputStream(zipEntry)) {
						Files.copy(in, destinationFile);
					}
				}
			}
		} catch (IOException e) {
			throw new Exception("Unzip Failed:" + e.getLocalizedMessage(), e); //$NON-NLS-1$
		}

	}
	
}
