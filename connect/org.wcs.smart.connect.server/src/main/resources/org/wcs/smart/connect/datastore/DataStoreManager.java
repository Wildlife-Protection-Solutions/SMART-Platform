package org.wcs.smart.connect.datastore;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.wcs.smart.connect.model.ConservationAreaInfo;

public enum DataStoreManager {
	INSTANCE;
	
	private static final String DATASTORE_LOCATION = "C:\\data\\SMART\\Connect\\datastore\\";
	
	public File getTemporaryDirectory(){
		File f = new File(getRootDirectory(), "temp"); //$NON-NLS-1$
		if(!f.exists()){
			f.mkdir();
		}
		return f;
	}
	public void deleteDirectory(ConservationAreaInfo info) throws IOException{
		FileUtils.deleteDirectory(getConservationAreaFullPath(info));
	}
	
	public String getConservationAreaFolder(ConservationAreaInfo info){
		return info.getUuid().toString().replaceAll("-", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public String generateFileName(String requestedName){
		
		File f = new File(getRootDirectory(), requestedName);
		if (!f.exists()){
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
		while(f.exists()){
			cnt++;
			f = new File(DATASTORE_LOCATION, name + "." + cnt + "." + ext); //$NON-NLS-1$ //$NON-NLS-2$
			
		}
		return name + "." + cnt + "." + ext; //$NON-NLS-1$ //$NON-NLS-2$
		
	}

	
	public File getFile(String fileName){
		System.out.println( (new File(DATASTORE_LOCATION)).getAbsolutePath());
		return new File(DATASTORE_LOCATION, fileName);
	}
	
	private File getRootDirectory(){
		return new File(DATASTORE_LOCATION);
	}
	
	public File getConservationAreaFullPath(ConservationAreaInfo info){
		File f = new File(getRootDirectory() + File.separator + getConservationAreaFolder(info));
		return f;
	}
}
