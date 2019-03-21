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
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.cybertracker.model.ICtPackage;

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
	 * Returns the ui controller for this contribution.
	 * @return
	 */
	public IPackageUiContribution getUiController();
	
	/**
	 * Returns a configuration describing the options that need
	 * to be added to the package.  Can return null if nothing 
	 * to do.
	 * 
	 * @param ctpackage
	 * @param monitor
	 * @return
	 */
	public PackageContribution packageFiles( ICtPackage ctpackage, IProgressMonitor monitor) throws IOException;

	
	/**
	 * Create the contents of the details panel for the contribution.
	 * @param parent the parent composite with a grid layout with 1 column
	 * @return
	 */
	public void createDetails(Composite parent, ICtPackage ctpackage, Session session);
	
	/**
	 * The contribution to add to the patrol export package.  This
	 * includes a list of files to include in the package and a 
	 * key/value pair to add to the project.json file.
	 * 
	 */
	public class PackageContribution {
		
		private List<Path> filesToAdd = new ArrayList<>();
		private HashMap<String, Object> metadata = new HashMap<>();
		
		
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
		
		/**
		 * Get project metadata additions
		 * @return
		 */
		public HashMap<String, Object> getProjectMetadata(){
			return this.metadata;
		}
		
		/**
		 * Adds a value to the project metadata
		 * @param key
		 * @param value
		 */
		public void addProjectMetadata(String key, JSONObject value) {
			metadata.put(key, value);
		}
		
		/**
		 * Adds a string value to the project metadata
		 * @param key
		 * @param value
		 */
		public void setProjectMetadata(String key, String value) {
			metadata.put(key, value);
		}
		
		/**
		 * Should be overwritten by user. This should cleanup
		 * and temporary resources created 
		 */
		public void cleanUp() throws IOException{
			
		}
	}
}
