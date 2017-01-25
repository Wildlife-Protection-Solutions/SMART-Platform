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
package org.wcs.smart.i2.birt.record;

import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;
import org.wcs.smart.i2.birt.datasource.DataSourceParameter;

/**
 * Entity record parameter metadata
 * 
 * @author Emily
 *
 */
public class RecordParameterMetadata implements IParameterMetaData{
	
	private static DataSourceParameter[] parameters = new DataSourceParameter[]{
		null,
		DataSourceParameter.RECORD_UUID,
	};
	
	private String datasetId;
	
	public RecordParameterMetadata(String datasetId){
		this.datasetId = datasetId;
	}
	
	public int findParameterIndex(String parameterName){
		for (int i = 1; i < parameters.length; i ++){
			if (parameters[i].getName().equalsIgnoreCase(parameterName)){
				return i;
			}
		}
		return -1;
	}
	
	@Override
	public int getParameterCount() throws OdaException {
		return parameters.length-1;
	}

	@Override
	public int getParameterMode(int param) throws OdaException {
		return parameters[param].getParameterMode();
	}

	@Override
	public String getParameterName(int param) throws OdaException {
		return parameters[param].getName();
	}

	@Override
	public int getParameterType(int param) throws OdaException {
		return parameters[param].getType();
	}

	@Override
	public String getParameterTypeName(int param) throws OdaException {
		int nativeTypeCode = getParameterType(param);
		return AbstractIntelBirtConnection.getNativeDataTypeName(nativeTypeCode, datasetId);
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
		if (param == 1) return parameterNoNulls;
		return parameterNullableUnknown;
	}
}
