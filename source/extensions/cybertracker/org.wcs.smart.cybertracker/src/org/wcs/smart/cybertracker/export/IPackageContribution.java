package org.wcs.smart.cybertracker.export;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;

public interface IPackageContribution {

	public Composite createUi(Composite parent);
	
	/**
	 * Returns a configuration describing the options that need
	 * to be added to the package.  Can return null if nothing 
	 * to do.
	 * 
	 * @param monitor
	 * @return
	 */
	public PackageUpdates packageFiles(IProgressMonitor monitor) throws IOException;

	public class PackageUpdates {
		List<Path> filesToAdd = new ArrayList<>();
		
		public void addFile(Path path) {
			filesToAdd.add(path);
		}
		
		public List<Path> getAddedFiles(){
			return this.filesToAdd;
		}
		
	}
}
