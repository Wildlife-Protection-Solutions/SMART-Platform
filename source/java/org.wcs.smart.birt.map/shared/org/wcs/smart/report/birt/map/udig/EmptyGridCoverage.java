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

import java.awt.Rectangle;
import java.io.IOException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.parameter.ParameterGroup;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Empty grid coverage for styling of raster layers
 * 
 * @author Emily
 *
 */
public class EmptyGridCoverage {

	private static volatile GridCoverage2DReader reader;
	
	private static Object LOCK = new Object();
	
	private EmptyGridCoverage(){
		
	}

	/**
	 * 
	 * @return empty grid coverage reader
	 */
	public static GridCoverage2D getInstance() {
		return null;
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
		private static Format emptyFormat = new Format(){
			
			GridEnvelope2D gridRange = new GridEnvelope2D(new Rectangle(0,0,100,100));
			ReferencedEnvelope env = new ReferencedEnvelope(-180.0, 180.0,-90.0, 90.0, DefaultGeographicCRS.WGS84);
			GridGeometry2D world = new GridGeometry2D(gridRange, env);
			DefaultParameterDescriptor<GridGeometry> gridGeometryDescriptor = new DefaultParameterDescriptor<GridGeometry>(
					AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString(),
					GridGeometry.class, null, world); 
			
			ParameterValueGroup empty =  new ParameterGroup(new DefaultParameterDescriptorGroup("", new GeneralParameterDescriptor[]{gridGeometryDescriptor})); //$NON-NLS-1$
			
			@Override
			public String getDescription() {
				return null;
			}

			@Override
			public String getDocURL() {
				return null;
			}

			@Override
			public String getName() {
				return null;
			}

			@Override
			public ParameterValueGroup getReadParameters() {
				return empty;
			}

			@Override
			public String getVendor() {
				return null;
			}

			@Override
			public String getVersion() {
				return null;
			}

			@Override
			public ParameterValueGroup getWriteParameters() {
				return empty;
			}
			
		};
		
		public EmptyGridCoverage2DReader(){
			super();
			this.originalEnvelope = new GeneralEnvelope(2);
			this.originalEnvelope.setToNull();
		}
		@Override
		public Format getFormat() {
			return emptyFormat;
		}

		@Override
		public GridCoverage2D read(GeneralParameterValue[] arg0)
				throws IllegalArgumentException, IOException {
			
			return getInstance();
		}
	}
}
