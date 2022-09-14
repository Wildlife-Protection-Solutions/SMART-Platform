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
package org.wcs.smart.util;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.derby.agg.Aggregator;

/**
 * Apache derby aggregate function for concatenating a set of strings
 * with a comma. Strings are sorted alphabetically
 * 
 * @author Emily
 *
 */
public class StringConcatAggregate implements Aggregator<String, String, StringConcatAggregate> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	List<String> sb = new ArrayList<>();
	
	@Override
	public void accumulate(String b) {
		sb.add(b);
	}


	@Override
	public String terminate() {
		if (sb.isEmpty()) return null;
		Collections.sort(sb, (a,b)->Collator.getInstance().compare(a, b));
		return String.join(", ",sb); //$NON-NLS-1$
	}


	@Override
	public void init() {
	}


	@Override
	public void merge(StringConcatAggregate arg0) {
	}
	
}
