/*
 * Copyright (C) 2012 Wildlife Conservation Societys
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

import org.hibernate.Session;
import org.wcs.smart.query.model.filter.IValueVisitor;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IValueDropItem;
import org.wcs.smart.query.ui.model.impl.AbstractValueDropItem;

/**
 * Represents an encounter rate value item which currently only supports
 * computing any value divided by a patrolOption value.  
 * 
 * 
 * @author egouge
 * @since 1.0.0
 */
public class CombinedValueItem implements IValueItem {

	public static final String PER_LABEL = AbstractValueDropItem.PER_LABEL;

	/**
	 * Creates a new combined value item from two
	 * other value parts.
	 * 
	 * @param part1
	 * @param part2
	 * @return
	 */
	public static CombinedValueItem createValueItem(IValueItem part1, IValueItem part2){
		return new CombinedValueItem(part1, part2);
	}
	
	private IValueItem part1;
	private IValueItem part2;
	private IValueDropItem part2Item;
	
	/**
	 * Creates a new combined value item from two other value items
	 * 
	 * @param part1
	 * @param part2 the should be a PatrolValueItem
	 */
	protected CombinedValueItem(IValueItem part1, IValueItem part2){
		this.part1 = part1;
		this.part2 = part2;
	}
	
	/**
	 * @return the numerator value item
	 */
	public IValueItem getPart1(){
		return this.part1;
	}
	
	/**
	 * @return the denominator value item
	 */
	public IValueItem getPart2(){
		return this.part2;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asString()
	 */
	@Override
	public String asString() {
		return part1.asString() + "/" + part2.asString(); //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getName(org.hibernate.Session)
	 */
	@Override
	public String getName(Session session) {
		StringBuilder sb = new StringBuilder();
		sb.append(part1.getName(session));
		sb.append(" " + PER_LABEL + " "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(part2.getName(session));
		return sb.toString();
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getFullName(org.hibernate.Session)
	 */
	public String getFullName(Session session){
		StringBuilder sb = new StringBuilder();
		sb.append(part1.getFullName(session));
		sb.append(" " + PER_LABEL + " "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(part2.getFullName(session));
		return sb.toString();
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asDropItem(org.hibernate.Session)
	 */
	@Override
	public DropItem asDropItem(Session session) throws Exception {
		DropItem di = part1.asDropItem(session);
		part2Item = (IValueDropItem) part2.asDropItem(session);
		di.initializeData(getDropItemInitializeData());
		
		return di;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getInitializeData()
	 */
	public Object getDropItemInitializeData(){
		Object[] data = new Object[2];
		data[0] = part1.getDropItemInitializeData();
		data[1] = part2Item;
		return data;
	}
	
	public void accept(IValueVisitor visitor){
		visitor.visit(part1);
		visitor.visit(part2);
		visitor.visit(this);
	}

}
