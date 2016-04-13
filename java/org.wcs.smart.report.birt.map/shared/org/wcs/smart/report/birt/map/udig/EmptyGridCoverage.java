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
package org.wcs.smart.report.birt.map.udig;

import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.GeneralEnvelope;
import org.opengis.coverage.grid.Format;
import org.opengis.parameter.GeneralParameterValue;

/**
 * Empty grid coverage for styling of raster layers
 * 
 * @author Emily
 *
 */
public class EmptyGridCoverage {

	private static GridCoverage2D coverage;
	private static GridCoverage2DReader reader;
	
	private static Object LOCK = new Object();
	
	private EmptyGridCoverage(){
		
	}

	/**
	 * 
	 * @return empty grid coverage reader
	 */
	public static GridCoverage2D getInstance() {
		if (coverage != null)
			return coverage;

		synchronized (LOCK) {
			if (coverage == null) {
				DataBuffer buffer = new DataBufferFloat(new float[0], 0);
				SampleModel sample = new BandedSampleModel(DataBuffer.TYPE_FLOAT, 0, 0, 1);
				WritableRaster raster = Raster.createWritableRaster(sample, buffer, null);

				Envelope2D envelope = new Envelope2D(null, 0, 0, 0, 0);
				GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
				coverage = factory.create(System.nanoTime() + "smart", raster, envelope); //$NON-NLS-1$
			}
		}
		return coverage;
	}

	/**
	 * 
	 * @return grid coverage reader that is empty instance
	 */
	public static GridCoverage2DReader getReaderInstance(){
		if (reader != null)
			return reader;

		synchronized (LOCK) {
			if (reader == null) {
				reader =  new EmptyGridCoverage2DReader();
			}
		}
		return reader;
	}
	
	private static class EmptyGridCoverage2DReader extends AbstractGridCoverage2DReader{
		public EmptyGridCoverage2DReader(){
			super();
			this.originalEnvelope = new GeneralEnvelope((org.opengis.geometry.Envelope)new Envelope2D(null, 0, 0, 0, 0));
		}
		@Override
		public Format getFormat() {
			return null;
		}

		@Override
		public GridCoverage2D read(GeneralParameterValue[] arg0)
				throws IllegalArgumentException, IOException {
			
			return getInstance();
		}
	}
}
