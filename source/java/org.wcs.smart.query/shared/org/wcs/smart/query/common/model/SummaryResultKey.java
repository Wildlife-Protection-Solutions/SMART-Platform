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
package org.wcs.smart.query.common.model;

import java.util.Arrays;

/**
 * A key for mapping summary value to the corresponding
 * group by fields.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SummaryResultKey {

	private String valueKey;
	private String[] groupByKeys;
	
	/**
	 * Creates a new key
	 * @param valueKey the value represented 
	 * @param groupByKeys the group by keys.  The order is important and
	 * must be the same across all classes if this class is
	 * to be used as a key in a map.
	 * 
	 */
	public SummaryResultKey(String valueKey, String[] groupByKeys){
		this.valueKey = valueKey;
		this.groupByKeys = groupByKeys;
	}
	public SummaryResultKey(SummaryResultKey copy){
		this.valueKey = copy.valueKey;
		this.groupByKeys = copy.groupByKeys;
	}
	
	public void setValueKey(String valueKey){
		this.valueKey = valueKey;
	}
	
	/** Keys are equals if the valueKeys are the same
	 * and the groupByKeys are the same
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object other){
		if (!(other instanceof SummaryResultKey)){
			return false;
		}
		SummaryResultKey o = (SummaryResultKey)other;
		if (!o.valueKey.endsWith(this.valueKey)){
			return false;
		}
		if (o.groupByKeys.length != this.groupByKeys.length){
			return false;
		}
		for (int i = 0; i < groupByKeys.length; i ++){
			if (!this.groupByKeys[i].equals(o.groupByKeys[i])){
				return false;
			}
		}
		return true;
	}
	
	@Override
	public int hashCode(){
		int hash = 17;
		hash = hash* 31 + valueKey.hashCode();
		hash = hash * 31 + Arrays.hashCode(groupByKeys);
		return hash;
	}

	/**
	 * @return
	 */
	public String asString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < groupByKeys.length; i ++){
			sb.append(groupByKeys[i] +  " _ "); //$NON-NLS-1$
		}
		sb.append(valueKey);
		return sb.toString();
	}
}
