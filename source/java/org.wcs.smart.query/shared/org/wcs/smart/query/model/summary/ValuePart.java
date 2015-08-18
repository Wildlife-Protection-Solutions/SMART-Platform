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
package org.wcs.smart.query.model.summary;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.wcs.smart.query.model.filter.IValueVisitor;

/**
 * Represents the value part of a summary query. This
 * contains a collection of value items.
 * @author egouge
 * @since 1.0.0
 */
public class ValuePart {

	/**
	 * creates a new value part
	 * @param items collection of items
	 * @return
	 */
	public final static ValuePart createValuePart(List<IValueItem> items) {
		return new ValuePart(items);
	}

	private ArrayList<IValueItem> items;

	protected ValuePart(List<IValueItem> items) {
		this.items = new ArrayList<IValueItem>();
		this.items.addAll(items);
	}

	/**
	 * Converts the value items to the string representation.
	 * @return
	 */
	public String asString() {
		StringBuilder sb = new StringBuilder();
		for (Iterator<IValueItem> iterator = items.iterator(); iterator
				.hasNext();) {
			IValueItem it = (IValueItem) iterator.next();
			sb.append(it.asString());
			if (iterator.hasNext()) {
				sb.append(","); //$NON-NLS-1$
			}
		}
		return sb.toString();
	}

	/**
	 * @return all the value items making up the value part
	 */
	public List<IValueItem> getValueItems() {
		return this.items;
	}


	public void visit(IValueVisitor visitor){
		for (IValueItem item : items){
			item.accept(visitor);
		}
	}
}
