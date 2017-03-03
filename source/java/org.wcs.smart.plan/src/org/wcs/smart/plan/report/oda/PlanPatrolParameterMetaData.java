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
import org.wcs.smart.data.oda.smart.impl.SmartParameterMetaData;
import org.wcs.smart.data.oda.smart.impl.SmartParameterMetaData.Parameter;
/**
 * Combined query and plan parameter metadata for
 * Patrol Plan Target Query.
 * 
 * @author Emily
 *
 */
public class PlanPatrolParameterMetaData implements IParameterMetaData {

	private PlanTargetParameterMetaData plan;
	private SmartParameterMetaData query;
	
	public PlanPatrolParameterMetaData(){
		plan = new PlanTargetParameterMetaData(true, false);
		query = new SmartParameterMetaData();
	}

	/**
	 * Finds a parameter at a given index
	 * 
	 * @param index
	 *            the parameter index
	 * @return the parameter of null if index not found
	 * @throws OdaException 
	 */
	public Object findParameter(int index) throws OdaException {
		if (isQuery(index)){
			return query.findParameter(index);
		}else{
			return plan.getParameterName(index - query.getParameterCount());	
		}
	}

	/**
	 * Finds a parameter by name
	 * 
	 * @param name
	 *            the parameter name
	 * @return the parameter or null if not found
	 */
	public Object findParameter(String name) {
		Parameter p = query.findParameter(name);
		if (p != null) return p;
		return name;
	}
	
	@Override
	public int getParameterCount() throws OdaException {
		return plan.getParameterCount() + query.getParameterCount();
	}

	private boolean isQuery(int index) throws OdaException{
		if (index <= query.getParameterCount()){
			return true;
		}
		return false;
	}
	
	@Override
	public int getParameterMode(int arg0) throws OdaException {
		if (isQuery(arg0)){
			return query.getParameterMode(arg0);
		}else{
			return plan.getParameterMode(arg0 - query.getParameterCount());
		}
	}

	@Override
	public String getParameterName(int arg0) throws OdaException {
		if (isQuery(arg0)){
			return query.getParameterName(arg0);
		}else{
			return plan.getParameterName(arg0 - query.getParameterCount());
		}
	}

	@Override
	public int getParameterType(int arg0) throws OdaException {
		if (isQuery(arg0)){
			return query.getParameterType(arg0);
		}else{
			return plan.getParameterType(arg0 - query.getParameterCount());
		}
	}

	@Override
	public String getParameterTypeName(int arg0) throws OdaException {
		if (isQuery(arg0)){
			return query.getParameterTypeName(arg0);
		}else{
			return plan.getParameterTypeName(arg0 - query.getParameterCount());
		}
	}

	@Override
	public int getPrecision(int arg0) throws OdaException {
		if (isQuery(arg0)){
			return query.getPrecision(arg0);
		}else{
			return plan.getPrecision(arg0 - query.getParameterCount());
		}
	}

	@Override
	public int getScale(int arg0) throws OdaException {
		if (isQuery(arg0)){
			return query.getScale(arg0);
		}else{
			return plan.getScale(arg0 - query.getParameterCount());
		}
	}

	@Override
	public int isNullable(int arg0) throws OdaException {
		if (isQuery(arg0)){
			return query.isNullable(arg0);
		}else{
			return plan.isNullable(arg0 - query.getParameterCount());
		}
	}
	
}
