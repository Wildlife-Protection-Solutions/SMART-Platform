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
package org.wcs.smart.plan.report.oda;

import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.data.oda.smart.impl.ISmartParameterMetadata;

/**
 * SMART Plan target query parameter metadata
 * @author Emily
 * @since 2.0.0
 *
 */
public class PlanTargetParameterMetaData implements ISmartParameterMetadata {

	public static final String PLAN_UUID_PARAM = "PlanUUID"; //$NON-NLS-1$
	
	public Object findParameter(int index){
		if (index == 1) return PLAN_UUID_PARAM;
		return null;
	}

	public Object findParameter(String name){
		if (name.equals(PLAN_UUID_PARAM)) return PLAN_UUID_PARAM;
		return null;
	}
	
	@Override
	public int getParameterCount() throws OdaException {
		return 1;
	}

	@Override
	public int getParameterMode(int param) throws OdaException {
		return IParameterMetaData.parameterModeIn;
	}

	@Override
	public String getParameterName(int param) throws OdaException {
		if (param == 1){
			return PLAN_UUID_PARAM;
		}
		return null;
	}

	@Override
	public int getParameterType(int param) throws OdaException {
		if(param == 1){
			return java.sql.Types.VARCHAR;
		}
		return 0;
	}

	@Override
	public String getParameterTypeName(int param) throws OdaException {
		return SmartPlanDriver.getNativeDataTypeName(getParameterType(param), PlanTargetQuery.SMART_PLAN_TARGET_ID);
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
		return IParameterMetaData.parameterNullableUnknown;
	}
	
	public int getParameterIndex(String name) throws OdaException{
		for (int i = 0; i < getParameterCount(); i ++){
			if (name.equals(getParameterName(i))){
				return i;
			}
		}
		return -1;
	}

}
