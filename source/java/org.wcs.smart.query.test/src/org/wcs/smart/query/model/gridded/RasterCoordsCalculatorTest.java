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
package org.wcs.smart.query.model.gridded;

import static org.junit.Assert.*;

import java.awt.Point;

import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.omg.CORBA.Bounds;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;

/**
 * Test method for {@link org.wcs.smart.query.model.gridded.RasterCoordsCalculator#compute(float, float, org.geotools.geometry.jts.ReferencedEnvelope)}.
 * 
 * @author Mauricio Pazos
 *
 */
public class RasterCoordsCalculatorTest {


	/**
	 *    | E1  
	 * ---I---
	 *    | 
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testEnvelope1() throws Exception {
		
		ReferencedEnvelope bounds = new ReferencedEnvelope( 20, 100, 20, 80, CRS.decode("EPSG:4326"));
		{
			Point rasterCoords = RasterCoordsCalculator.compute(20, 80, bounds);

			assertEquals(0, rasterCoords.x);
			assertEquals(0, rasterCoords.y);
		}
		{
			Point rasterCoords = RasterCoordsCalculator.compute(100, 20, bounds);

			assertEquals(80, rasterCoords.x);
			assertEquals(60, rasterCoords.y);
		}
	}
	/**
	 *    |  
	 * ---I---
	 *    | E2
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testEnvelope2() throws Exception {
		
		ReferencedEnvelope bounds = new ReferencedEnvelope(2, 100, -2, -50, CRS.decode("EPSG:4326"));
		{
			Point rasterCoords = RasterCoordsCalculator.compute(2, -2, bounds);

			assertEquals(0, rasterCoords.x);
			assertEquals(0, rasterCoords.y);
		}
		{
			Point rasterCoords = RasterCoordsCalculator.compute(2, -3, bounds);

			assertEquals(0, rasterCoords.x);
			assertEquals(1, rasterCoords.y);
		}
		{
			Point rasterCoords = RasterCoordsCalculator.compute(4, -4, bounds);

			assertEquals(2, rasterCoords.x);
			assertEquals(2, rasterCoords.y);
		}
		{
			Point rasterCoords = RasterCoordsCalculator.compute(100, -50, bounds);

			assertEquals(98, rasterCoords.x);
			assertEquals(48, rasterCoords.y);
		}
	}

	/**
	 *    |  
	 * ---I---
	 * E3 | 
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testEnvelope3() throws Exception {
		
		ReferencedEnvelope bounds = new ReferencedEnvelope(-170, -80, -20, -80, CRS.decode("EPSG:4326"));
		// TODO
	}

	/**
	 * E4 |  
	 * ---I---
	 *    | 
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testEnvelope4() throws Exception {
		
		ReferencedEnvelope bounds = new ReferencedEnvelope(-170, -80, 20, 80, CRS.decode("EPSG:4326"));
		{
			Point rasterCoords = RasterCoordsCalculator.compute(-170, 80, bounds);

			assertEquals(0, rasterCoords.x);
			assertEquals(0, rasterCoords.y);
		}
		{
			Point rasterCoords = RasterCoordsCalculator.compute(-80, 20, bounds);

			assertEquals(90, rasterCoords.x);
			assertEquals(60, rasterCoords.y);
		}
	}
}
