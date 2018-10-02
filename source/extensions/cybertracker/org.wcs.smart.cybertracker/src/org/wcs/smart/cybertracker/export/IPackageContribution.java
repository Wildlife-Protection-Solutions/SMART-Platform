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
package org.wcs.smart.cybertracker.export;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.json.simple.JSONObject;

/**
 * Extension for contributing additional items to the Cybertracker export
 * packages.  This includes the ability to add items to the ui (package export
 * dialog) and to the export package.  This applies to both patrols
 * and surveys.
 * 
 * @author Emily
 *
 */
public interface IPackageContribution {

	/**
	 * Create the ui component to add to the export dialog.  Can return null
	 * if no ui component.
	 * 
	 * @param parent
	 * @param prefKey a string used for storing preferences to database, same dialog type (patrols vs surveys)
	 * should provide the same preferenece key
	 * @return
	 */
	public Composite createUi(Composite parent, String prefKey);
	
	/**
	 * Returns a configuration describing the options that need
	 * to be added to the package.  Can return null if nothing 
	 * to do.
	 * 
	 * @param monitor
	 * @return
	 */
	public PackageContribution packageFiles(IProgressMonitor monitor) throws IOException;

	/**
	 * The contribution to add to the patrol export package.  This
	 * includes a list of files to include in the package and a 
	 * key/value pair to add to the project.json file.
	 * 
	 */
	public class PackageContribution {
		
		private List<Path> filesToAdd = new ArrayList<>();
		private String metadataKey = null;
		private Object metadataValue = null;
		
		/**
		 * Can be file or a directory. If a directory all subdirectories will
		 * also be copied
		 * @param path
		 */
		public void addFile(Path path) {
			if (!filesToAdd.contains(path)) filesToAdd.add(path);
		}
		
		/**
		 * Paths can be files or directories.  If directory should copy
		 * all sub directory/files
		 * @return
		 */
		public List<Path> getAddedFiles(){
			return this.filesToAdd;
		}
			
		public String getProjectMetadataKey(){
			return metadataKey;
		}
		
		/**
		 * 
		 * @return either JSONObject or String
		 */
		public Object getProjectMetdata() {
			return metadataValue;
		}
		
		public void setProjectMetadata(String key, JSONObject value) {
			this.metadataKey = key;
			this.metadataValue = value;
		}
		
		public void setProjectMetadata(String key, String value) {
			this.metadataKey = key;
			this.metadataValue = value;
		}
		
		/**
		 * Should be overwritten by user. This should cleanup
		 * and temporary resources created 
		 */
		public void cleanUp() throws IOException{
			
		}
	}
}
