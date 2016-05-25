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
package org.wcs.smart.patrol.xml.external;

import java.util.List;

import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.xml.model.ExtraDataType;

/**
 * Contribution for the Patrol Export/Import functionality to handle
 * "extra-data" section of XML file. Contributions are provided via
 * extension point.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public interface IXmlExtraDataContribution {

	/**
	 * Extension id
	 */
	public static final String EXTENSION_ID = "org.wcs.smart.patrol.xml"; //$NON-NLS-1$

	/**
	 * Returns "extra-data" that current extension points wants to save
	 * to XML file for given patrol.
	 * 
	 * @param patrol
	 * @return List<ExtraDataType>
	 * @throws Exception
	 */
	public List<ExtraDataType> exportData(Patrol patrol) throws Exception;

	/**
	 * Converts XML extra-data to some inner structure but do not perform actual save operation.
	 * Extension point is supposed to convert only items that it contributed.
	 * Returned object acts like a wrapper on converted extra-data.
	 * 
	 * @see IConvertedExtraData
	 * 
	 * @param extraDataList
	 * @return
	 */
	public IConvertedExtraData fromXml(List<ExtraDataType> extraDataList);
	
}