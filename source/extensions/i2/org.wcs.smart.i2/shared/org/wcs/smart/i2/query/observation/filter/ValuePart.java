/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.query.observation.filter;

import java.text.MessageFormat;

/**
 * Summary query value part.
 * 
 * @author Emily
 *
 */
public class ValuePart {
	
	public static ValuePart parse(String option) {
		for (ValueOption op : ValueOption.values()) {
			if (op.getKey().equals(option))
				return new ValuePart(op);
		}
		throw new IllegalStateException(MessageFormat.format("{0} is not a supported value", option)); //$NON-NLS-1$
	}
	
	
	public enum ValueOption{
		NUMBER_ENTITIES("numentities"), //$NON-NLS-1$
		NUMBER_RECORDS("numrecords"); //$NON-NLS-1$
		
		String key;
		
		ValueOption(String key){
			this.key = key;
		}
		
		public String getKey() { return this.key; }
		
	}
	
	private ValueOption op;
	
	public ValuePart(ValueOption op) {
		this.op = op;
	}
	
	public ValueOption getValueOption() {
		return this.op;
	}
	
	public String asString() {
		return op.key;
	}
}
