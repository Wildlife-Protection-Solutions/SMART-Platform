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
package org.wcs.smart.dataentry.model.xml.external;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.xml.generated.CmExtraDataType;

/**
 * Contribution for the Configurable Model Export/Import functionality to handle
 * "extra-data" section of XML file. Contributions are provided via extension point.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public interface IXmlCmExtraDataContribution {

	/**
	 * Extension id
	 */
	public static final String EXTENSION_ID = "org.wcs.smart.dataentry.xml"; //$NON-NLS-1$
	
	/**
	 * Returns "extra-data" that current extension points wants to save
	 * to XML file for given configurable model.
	 * 
	 * @param cm
	 * @param session 
	 * @return List<CmExtraDataType>
	 * @throws Exception
	 */
	public List<CmExtraDataType> exportData(ConfigurableModel cm, Session session);

	/**
	 * Converts XML extra-data to some inner structure but do not perform actual save operation.
	 * Extension point is supposed to convert only items that it contributed.
	 * Returned object acts like a wrapper on converted extra-data.
	 * 
	 * @see IConvertedCmExtraData
	 * 
	 * @param extraDataList
	 * @param session 
	 * @return
	 */
	public IConvertedCmExtraData fromXml(List<CmExtraDataType> extraDataList, Session session);
	
	
}
