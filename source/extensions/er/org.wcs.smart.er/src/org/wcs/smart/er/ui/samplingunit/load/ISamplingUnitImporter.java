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
package org.wcs.smart.er.ui.samplingunit.load;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wcs.smart.er.model.SamplingUnit;

/**
 * Sampling unit importer interface.
 * 
 * @author Emily
 *
 */
public interface ISamplingUnitImporter {

	public static final String TYPE_KEY = "SU_TYPE";
	public static final String ID_FIELD_KEY = "ID_FIELD";
	public static final String X1_FIELD_KEY = "X1_FIELD";
	public static final String Y1_FIELD_KEY = "Y1_FIELD";
	public static final String X2_FIELD_KEY = "X2_FIELD";
	public static final String Y2_FIELD_KEY = "Y2_FIELD";
	public static final String PROJECTION_KEY = "PROJECTION";
	
	public static final String BUFFER_KEY = "BUFFER";
	
	public String[] getFieldNames(File f, Map<String, Object> options) throws Exception;
	
	public List<SamplingUnit> importFile(File f, HashMap<Object, Object> options) throws Exception;
	
}
