/*
 *    uDig - User Friendly Desktop Internet GIS client
 *    http://udig.refractions.net
 *    (C) 2004, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 *
 */
package org.wcs.smart.paws.udig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.parameter.ParameterGroup;
import org.locationtech.udig.catalog.rasterings.AbstractRasterGeoResource;
import org.locationtech.udig.catalog.rasterings.AbstractRasterGeoResourceInfo;
import org.locationtech.udig.catalog.rasterings.AbstractRasterService;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.parameter.GeneralParameterValue;

/**
 * 
 */
public class PawsTiffGeoResource extends AbstractRasterGeoResource {

	private AbstractGridCoverage2DReader reader;
	private Path file;

	/**
	 * Construct <code>GeoTiffGeoResourceImpl</code>.
	 */
	public PawsTiffGeoResource(AbstractRasterService service, Path file) {
		super(service, file.toString());

	}

	/**
	 * Template method called by findResource that is responsible for loading the
	 * coverage. By default it loads a very small coverage for getting info from but
	 * not using as a real datasource
	 * 
	 * @return
	 * @throws IOException
	 */
	@Override
	protected GridCoverage loadCoverage() throws IOException {
		AbstractGridCoverage2DReader reader = this.service(new NullProgressMonitor()).getReader(null);
		ParameterGroup pvg = getReadParameters();
		List<GeneralParameterValue> list = pvg.values();
		GeneralParameterValue[] values = list.toArray(new GeneralParameterValue[0]);
		GridCoverage gridCoverage = reader.read(values);
		return gridCoverage;
	}

	@Override
	protected AbstractRasterGeoResourceInfo createInfo(IProgressMonitor monitor) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public synchronized AbstractGridCoverage2DReader getReader() throws FileNotFoundException {
		if (this.reader == null) {
			AbstractGridFormat frmt = (AbstractGridFormat) ((PawsService) service).getFormat();
			if (Files.exists(file)) {
				this.reader = (AbstractGridCoverage2DReader) frmt.getReader(file);
			}
			throw new FileNotFoundException(file.toString());

		}
		return this.reader;
	}
}
