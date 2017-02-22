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
package org.wcs.smart.observation.common.importwp;

import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.gpx.GPSDataImport.ImportType;
import org.wcs.smart.observation.common.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.observation.model.Waypoint;

public interface IImportEngine {

	/**
	 * 
	 * @return the gui name of the import option
	 */
	public String getName();
	
	/**
	 * 
	 * @param type
	 * @return <code>true</code> if given type supported 
	 */
	public boolean supportsType(ObservationGPSDataImport.ImportType type);
	
	/**
	 * Loads all waypoints from the source.
	 * @param options import option
	 * @param type import type
	 * @param currentDate current date selected in the UI, necessary from some import option types
	 * @param monitor progress monitor
	 * @return list of imported waypoints
	 * @throws Exception
	 */
	public List<Waypoint> getWaypoints(ImportOption options, ImportType type, Date currentDate, IProgressMonitor monitor) throws Exception;
	
	/**
	 * Updates the object and saves the results to the database.
	 * 
	 * @param options
	 * @param type
	 * @param object - source object to be updated with data
	 * @param currentDate
	 * @param data
	 * @param monitor
	 * @return String success message to display on UI
	 * @throws Exception
	 */
	public String updateSourceObject(ImportOption option, ObservationGPSDataImport.ImportType type, Object object, List<Waypoint> data, IProgressMonitor monitor) throws Exception;

	/**
	 * Fist wizard page to display
	 * @param wizard
	 * @return
	 */
	public IImportWizardPage getFirstWizardPage(ImportGpsDataWizard wizard);
}
