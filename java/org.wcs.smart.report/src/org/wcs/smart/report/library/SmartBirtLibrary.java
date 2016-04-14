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
package org.wcs.smart.report.library;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.wcs.smart.SmartContext;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.util.SmartUtils;

/**
 * Class for managing the smart BIRT LIBRARY.
 * <p>When instantiated this class ensure that the current
 * conservation area has a BIRT Library installed, if not it
 * is added.
 * </p>
 * <p>The default library contains the SMART ODA Datasource
 * and nothing else.
 * <p>
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SmartBirtLibrary {

	private static SmartBirtLibrary instance = null;
	
	/**
	 * Local library directory 
	 */
	public static final String LIBRARY_DIR = "rptlibrary"; //$NON-NLS-1$
	
	/**
	 * Library file name
	 */
	public static final String LIBRARY_FILENAME = "smart.rptlibrary"; //$NON-NLS-1$
	
	/**
	 * Location of default birt library which contains only the smart
	 * oda datasource.
	 */
	public static final String DEFAULT_LIBRARY_FILENAME = "org/wcs/smart/report/library/default_smart.rptlibrary"; //$NON-NLS-1$
	
	
	public static final String DEFAULT_LIBRARY_NAMESPACE = "smart"; //$NON-NLS-1$
	/**
	 * @return the current smart library instance
	 */
	public static SmartBirtLibrary getInstance(){
		if (instance == null){
			instance = new SmartBirtLibrary();
		}
		return instance;
	}
	
	private File libraryLocation = null;
	private File libraryFile = null;

	/**
	 * Creates a new SMART Birt library instance.
	 * <p>If the current conservation are does not have a smart
	 * birt library file associated with it the default one is applied to that conservation
	 * area.
	 * </p>
	 */
	public SmartBirtLibrary(){
		libraryLocation = new File(ReportPlugIn.getReportDirectory(SmartDB.getCurrentConservationArea()), LIBRARY_DIR + File.separator);
		if (!libraryLocation.exists()){
			SmartUtils.createDirectory(libraryLocation);
		}
		
		libraryFile = new File(libraryLocation, LIBRARY_FILENAME);
		if (!libraryFile.exists()){			
			try (InputStream library = ReportPlugIn.class.getClassLoader().getResourceAsStream(DEFAULT_LIBRARY_FILENAME)){
				FileUtils.copyInputStreamToFile(library, libraryFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * 
	 * @return the birt smart library directory location
	 */
	public File getLibraryLocation(){
		return this.libraryLocation;
	}
	/**
	 * 
	 * @return the birt smart library file
	 */
	public File getLibraryFile(){
		return this.libraryFile;
	}
	
	
}
