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
package org.wcs.smart.er.query.filter.summary;

import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.model.summary.IGroupBy;

/**
 * Sampling unit group option.
 * 
 * @author Emily
 *
 */
public class SamplingUnitGroupBy implements IGroupBy{
	
	/**
	 *  s:samplingunit:" (< UUID >)? (":" < UUID > )* > 
	 * @param key
	 * @return
	 */
	public static SamplingUnitGroupBy createGroupBy(String key){
		String bits[] = key.split(":"); //$NON-NLS-1$
		if (bits.length > 2){
			String items[] = new String[bits.length - 2];
			for (int i = 2; i < bits.length; i ++){
				items[i-2] = bits[i];
			}
			return new SamplingUnitGroupBy(items);
		}else{
			return new SamplingUnitGroupBy(null);
		}
	}
		
	private String[] items;
	
	private SamplingUnitGroupBy(String[] items){
		this.items = items;
	}
	
	@Override
	public String asString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getKeyPart());
		if (items != null){
			for (String it : items){
				sb.append(it);
				sb.append(":"); //$NON-NLS-1$
			}
			if (items.length > 0){
				sb.deleteCharAt(sb.length() - 1);
			}
		}
		return sb.toString();
	}

	@Override
	public String getKeyPart() {
		return "sgb:samplingunit:"; //$NON-NLS-1$
	}
	
	/**
	 * 
	 * @return the raw group by items
	 */
	public String[] getRawItems(){
		return this.items;
	}

	@Override
	public GroupByType getType() {
		return GroupByType.BYTE;
	}

	@Override
	public void visit(IGroupByVisitor visitor) {
		visitor.visit(this);		
	}

}
