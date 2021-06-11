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
package org.wcs.smart.incident.birt;

import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;


/**
 * Entity record parameter metadata
 * 
 * @author Emily
 *
 */
public class IncidentDatasetParameterMetadata implements IParameterMetaData{
	
	public static String UUID_PARAM_NAME = "Incident UUID"; //$NON-NLS-1$
	private static int UUID_PARAM_MODE = IParameterMetaData.parameterModeIn;
	private static int UUID_PARAM_TYPE = java.sql.Types.VARCHAR;
	
	private String datasetName;
	
	public IncidentDatasetParameterMetadata(String datasetName) {
		this.datasetName = datasetName;
	}
	
	public int findParameterIndex(String parameterName){
		if (parameterName.equalsIgnoreCase(UUID_PARAM_NAME)) return 1;
		return -1;
	}
	
	@Override
	public int getParameterCount() throws OdaException {
		return 1;
	}

	@Override
	public int getParameterMode(int param) throws OdaException {
		return UUID_PARAM_MODE;
	}

	@Override
	public String getParameterName(int param) throws OdaException {
		return UUID_PARAM_NAME;
	}

	@Override
	public int getParameterType(int param) throws OdaException {
		return UUID_PARAM_TYPE;
	}

	@Override
	public String getParameterTypeName(int param) throws OdaException {
		int nativeTypeCode = getParameterType(param);
		return SmartConnection.getNativeDataTypeName(nativeTypeCode, datasetName);
	}

	@Override
	public int getPrecision(int param) throws OdaException {
		return -1;
	}

	@Override
	public int getScale(int param) throws OdaException {
		return -1;
	}

	@Override
	public int isNullable(int param) throws OdaException {
		return parameterNullable;
	}
}
