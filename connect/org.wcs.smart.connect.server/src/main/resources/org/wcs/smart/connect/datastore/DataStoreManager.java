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
package org.wcs.smart.connect.datastore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.naming.NamingException;

import org.apache.commons.io.FileUtils;
import org.wcs.smart.connect.apache.EnvironmentVariables;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.util.UuidUtils;

/**
 * Tools for managing the SMART datastore.
 * 
 * @author Emily
 *
 */
public enum DataStoreManager {
	
	INSTANCE;
	
	private String datastoreLocation;
	
	public static final String CA_EXPORT_LOCATION = "caexport"; //$NON-NLS-1$
	
	/**
	 * Initialize the location of the datastore;  This should be called
	 * once on startup of web app.
	 * @param datastoreLocation
	 * @throws NamingException 
	 */
	public void initDatastore() throws NamingException{
		datastoreLocation = (String)EnvironmentVariables.INSTANCE.getEnvironmentVariable(EnvironmentVariables.Variable.DATASTORE_LOCATION);
	}
	
	/**
	 * Any files create in this directory should be considered temporary
	 * and removed after finished.
	 * 
	 * @return the temporary working directory in the filestore.
	 * @throws IOException 
	 */
	public Path getTemporaryDirectory() throws IOException{
		Path f = getRootDirectory().resolve("temp"); //$NON-NLS-1$
		if(!Files.exists(f)){ 
			Files.createDirectories(f);

		}
		return f;
	}
	
	/**
	 * Deletes the directory associated with the given conservation area.
	 * 
	 * @param info
	 * @throws IOException
	 */
	public void deleteDirectory(ConservationAreaInfo info) throws IOException{
		deleteDirectory(info.getUuid());
	}
	
	public void deleteDirectory(UUID caUuid) throws IOException{
		Path f = getRootDirectory().resolve(UuidUtils.uuidToString(caUuid));
		FileUtils.deleteDirectory(f.toAbsolutePath().normalize().toFile());
	}
	
	/**
	 * Return the folder name of the conservation area data folder in the filestore. 
	 * 
	 * @param info
	 * @return
	 */
	public String getConservationAreaFolder(ConservationAreaInfo info){
		return UuidUtils.uuidToString(info.getUuid()); 
	}
	
	/**
	 * Generates a filename that is unique based on the request name.
	 * The requested name should be a relative directory.  If the file does not
	 * exist it will return the request name otherwise it will attempt to create a new
	 * unique name and return the new file name.
	 * @param requestedName
	 * @return
	 */
	public String generateFileName(String requestedName){
		Path f = getRootDirectory().resolve(requestedName);
		if (!Files.exists(f)){
			return requestedName;
		}
		
		int index = requestedName.lastIndexOf('.');
		String name = ""; //$NON-NLS-1$
		String ext = ""; //$NON-NLS-1$
		if (index <= 0){
			name = requestedName;
			ext = ""; //$NON-NLS-1$
		}else{
			name = requestedName.substring(0, index);
			ext = requestedName.substring(index+1);
		}
		int cnt = 0;
		while(Files.exists(f)){
			cnt++;
			f = Paths.get(datastoreLocation, name + "." + cnt + "." + ext); //$NON-NLS-1$ //$NON-NLS-2$
			
		}
		return name + "." + cnt + "." + ext; //$NON-NLS-1$ //$NON-NLS-2$
		
	}

	/**
	 * Combines the file with the root directory to get the absolute
	 * location of a given file.
	 * 
	 * @param fileName
	 * @return
	 */
	public Path getFile(String fileName){
		return Paths.get(datastoreLocation, fileName);
	}
	
	/**
	 * 
	 * @return the root filestore location
	 */
	public Path getRootDirectory(){
		return Paths.get(datastoreLocation);		
	}
	
	/**
	 * The full location of the conservation area folder in the filestore.
	 * @param info
	 * @return
	 */
	public Path getConservationAreaFullPath(ConservationAreaInfo info){
		return getRootDirectory().resolve(getConservationAreaFolder(info));
	}
}
