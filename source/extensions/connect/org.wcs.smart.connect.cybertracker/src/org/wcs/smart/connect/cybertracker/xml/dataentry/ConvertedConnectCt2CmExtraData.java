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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.xml.external.IConvertedCmExtraData;
import org.wcs.smart.dataentry.model.xml.generated.CmExtraDataType;

/**
 * Wrapper for Connect Alerts CyberTracker extra-data conversion (from XML) result.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConvertedConnectCt2CmExtraData implements IConvertedCmExtraData {

	private List<String> warnings = new ArrayList<String>();
	
	public ConvertedConnectCt2CmExtraData(List<CmExtraDataType> extraDataList, Session session) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<String> getWarnings() {
		return warnings;
	}

	@Override
	public boolean saveInTransaction(Session session, ConfigurableModel cm) {
		// TODO Auto-generated method stub
		return true;
	}

}
