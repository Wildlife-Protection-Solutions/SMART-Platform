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
package org.wcs.smart.connect.cybertracker.xml.dataentry;

import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.dataentry.model.xml.external.ICmXmlExtraDataImporter;
import org.wcs.smart.dataentry.model.xml.external.IConvertedCmExtraData;
import org.wcs.smart.dataentry.model.xml.generated.CmExtraDataType;

/**
 * Connect Alerts CyberTracker contribution for dataentry module to provide ability to 
 * Export/Import to/from XML file Connect Alerts CyberTraker related data.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConnectCt2CmXmlExtraDataImporter implements ICmXmlExtraDataImporter {

	@Override
	public IConvertedCmExtraData fromXml(List<CmExtraDataType> extraDataList, Map<String, UuidItem> dataMap, Session session) {
		return new ConvertedConnectCt2CmExtraData(extraDataList, dataMap, session);
	}
}
