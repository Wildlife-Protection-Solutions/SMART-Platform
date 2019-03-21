/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.ctpackage.ui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.model.ICtPackage;

/**
 * Manager for processing ctpackages
 * 
 * @author Emily
 *
 */
public interface ICtPackageManager {

	public static final String EXT_NAME_ID = "ctpackagemanager"; //$NON-NLS-1$
	
	/**
	 * The package type identifier
	 * 
	 * @return
	 */
	public String getTypeIdentifier();
	
	/**
	 * The package type name
	 * 
	 * @return
	 */
	public String getTypeName();
	
	/**
	 * 
	 * @return the package type image or null
	 */
	public default Image getTypeImage() {
		return null;
	}
	
	/**
	 * Loads all packages from the database
	 * 
	 * @param session
	 * @return
	 */
	public List<? extends ICtPackage> getPackages(Session session);
	
	/**
	 * Creates a new package and initializes any default values.  Does
	 * not presist the object.
	 * 
	 * @return
	 */
	public ICtPackage createPackage();
	
	/**
	 * Create the package configurator for managing the ui elements
	 * associated with the package
	 * 
	 * @return
	 */
	public ICtPackageConfigurator createConfigurator();

	/**
	 * Deletes the selected package
	 * @param ctpackage
	 * @param session
	 * @return
	 */
	public default void deletePackage(ICtPackage ctpackage, Session session) {
		session.delete(ctpackage);
	}
	
	
	/**
	 * Builds the package and write the results to the output file.
	 * 
	 * @param ctpackage
	 * @param output
	 * @throws IOException
	 */
	public void buildPackage(ICtPackage ctpackage, Path output) throws IOException;
}
