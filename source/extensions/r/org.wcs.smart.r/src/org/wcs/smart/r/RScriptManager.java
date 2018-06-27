package org.wcs.smart.r;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.r.model.RScript;
import org.wcs.smart.user.UserLevelManager;

public enum RScriptManager {
	INSTANCE;
	
	public static final String RSCRIPT_DIR = "rscripts";
	
	public Path getScriptPath(RScript script) {
		return Paths.get(script.getConservationArea().getFileDataStoreLocation())
				.resolve(RSCRIPT_DIR)
				.resolve(script.getFilename());
	}
	
	public Path computeScriptFileName(Path inputFile, ConservationArea ca) {
		String filename = inputFile.getFileName().toString();
		
		Path rootPath = Paths.get(ca.getFileDataStoreLocation())
				.resolve(RSCRIPT_DIR);
		
		Path temp = rootPath.resolve(filename);
		
		int index = filename.lastIndexOf('.');
		String prefix = filename;
		String suffix = "";
		if (index >= 0) {
			prefix = filename.substring(0, index);
			suffix = filename.substring(index + 1);
		}
		int cnt = 1;
		while(Files.exists(temp)) {
			String newfilename = prefix + "." + cnt + "." + suffix;
			temp = rootPath.resolve(newfilename);
			if (cnt > 10_000) {
				throw new IllegalStateException("Unable to determine filename in filestore for r script file");
			}
			cnt++;
		}
		return temp;
	}
	
	public boolean canEditScript() {
		return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), UserLevelManager.ADMIN) ||
				UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), UserLevelManager.MANAGER) ||
				UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), UserLevelManager.ANALYST);
	}
}
