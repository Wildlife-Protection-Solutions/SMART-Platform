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
package org.wcs.smart.patrol.internal.ui.importwp.gpsbabel;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Test;
import org.wcs.smart.gpx.GPSBabel;

/**
 * Test for reading gps babel supported formats.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class GPSBabelTest {

	@Test
	public void testGetFormats()  {
		HashMap<String, String> ops = null;
		
		try {
			ops = GPSBabel.getDeviceOptions();
		} catch (IOException e) {
			fail(e.getMessage());
		}
		assertNotNull(ops);
		assertTrue(ops.size() > 0);
		
		boolean garmin = false;
		for (String val : ops.keySet()){
			if (val.toLowerCase().contains("garmin")){
				garmin = true;
				break;
			}
		}
		if (!garmin){
			fail("Garmin device not found");
		}
	}

}
