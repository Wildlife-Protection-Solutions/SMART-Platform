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
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Import engine for generating tracks
 * from waypoints.
 * 
 * @author Emily
 *
 */
public abstract class TrackFromWaypointEngine implements IImportEngine {

	@Override
	public String getName() {
		return Messages.ImportWpTypeWizardPage_GenerateWaypointsOp;
	}

	/**
	 * Only supports track types
	 */
	@Override
	public boolean supportsType(ImportType type) {
		return type == ImportType.TRACK;
	}

	
	/**
	 * @returns null as no data is imported
	 */
	@Override
	public List<Waypoint> getWaypoints(ImportOption options, ImportType type,
			Date currentDate, IProgressMonitor monitor) throws Exception {
		return null;
	}

	
	@Override
	public IImportWizardPage getFirstWizardPage(ImportGpsDataWizard wizard) {
		return new ImportFromWaypointWizardPage(wizard);
	}

}
