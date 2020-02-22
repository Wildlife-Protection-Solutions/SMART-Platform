/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.patrol.query.parser.internal.summary;

import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.patrol.ui.IQueryPatrolLabelProvider;

/**
 * ValueItem for patrol value that counts buffers patrol tracks
 * and computes the associated area.
 * 
 * @author egouge
 * @since 7.0.0
 */
public class PatrolValueItemAreaBuffer extends PatrolValueItem {

	protected double bufferValue;
	
	public static final String ERROR_MSG_KEY = "patrolvalueitemareabuffererror"; //$NON-NLS-1$
	/**
	 * Creates a patrol value item from a key of the form
	 * patrol:<aggregation>:patrolkey:bufferValue
	 * 
	 * @param part
	 */
	public PatrolValueItemAreaBuffer(String key){
		super(key);
		
		String[] bits = key.split(":"); //$NON-NLS-1$
		if (bits.length >= 4) {
			bufferValue = Double.parseDouble(bits[3]);
			if (bufferValue <= 0) throw new RuntimeException(SmartContext.INSTANCE.getClass(IQueryPatrolLabelProvider.class).getLabel(ERROR_MSG_KEY, Locale.getDefault()));
		}else {
			throw new RuntimeException(SmartContext.INSTANCE.getClass(IQueryPatrolLabelProvider.class).getLabel(ERROR_MSG_KEY, Locale.getDefault()));
		}
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asString()
	 */
	public String asString(){
		String key = super.asString();
		key += ":" + bufferValue;  //$NON-NLS-1$
		return key;
	}
	
	/**
	 * The user specified area buffer
	 * @return
	 */
	public double getBufferValue() {
		return this.bufferValue;
	}	
}
