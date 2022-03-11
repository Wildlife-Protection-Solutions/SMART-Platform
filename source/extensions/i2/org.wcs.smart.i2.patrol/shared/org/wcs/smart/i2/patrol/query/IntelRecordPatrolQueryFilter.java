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
package org.wcs.smart.i2.patrol.query;

import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.IFilterVisitor;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.patrol.query.ext.IExtensionFilter;
import org.wcs.smart.patrol.query.model.IPatrolQueryOption;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Intelligence filter extension for patrol queries. 
 * 
 * @author elitvin
 * @author Emily
 * @since 1.0.0
 */
public class IntelRecordPatrolQueryFilter implements IExtensionFilter {
	
	private IPatrolQueryOption option;
	private Operator op;	
	private Object value;

	public IntelRecordPatrolQueryFilter(){}
	
	public IntelRecordPatrolQueryFilter(IPatrolQueryOption option, Operator op, Object value) {
		this.option = option;
		this.op = op;
		this.value = value;
	}

	public IPatrolQueryOption getPatrolQueryOption(){
		return this.option;
	}
	
	@Override
	public String asString() {
		return option.getKey() + " " + op.asSmartValue() + " " + value; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public boolean isAnyRecord() {
		if (value == null) return true;
		if (!(value instanceof String)) return false;
		
		String obj = SharedUtils.stripQuotes((String)value);
		if (obj.isEmpty() || obj.equals(UuidUtils.ZERO_UUID_STR)) return true;
		return false;
	}
	
	public Object getValue(){
		return this.value;
	}
	

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * not supported
	 */
	@Override
	public IFilter createFilter(String key) {
		return null;
	}

	@Override
	public IFilter createFilter(String key, Operator op, Object value) {
		if (key.equalsIgnoreCase(IntelRecordPatrolQueryOption.KEY)){
			return new IntelRecordPatrolQueryFilter(new IntelRecordPatrolQueryOption(), op, value);
		}
		return null;
	}
		
}
