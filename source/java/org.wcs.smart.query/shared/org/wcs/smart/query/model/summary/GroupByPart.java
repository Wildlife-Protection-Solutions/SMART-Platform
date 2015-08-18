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

import org.wcs.smart.query.model.filter.IGroupByVisitor;

/**
 * Group by part of a summary query.
 * @author egouge
 * @since 1.0.0
 */
public class GroupByPart {

	/**
	 * Creates a new group by part with the collection 
	 * of group bys.
	 * @param groupBy
	 * @return
	 */
	public static final GroupByPart createGroupBy(List<IGroupBy> groupBy) {
		return new GroupByPart(groupBy);
	}

	private ArrayList<IGroupBy> groupBy;

	/**
	 * A new group by part
	 * @param groupBy collection of group bys
	 */
	public GroupByPart(List<IGroupBy> groupBy) {
		this.groupBy = new ArrayList<IGroupBy>();
		this.groupBy.addAll(groupBy);
	}

	/**
	 * @return converts the collection of group bys into
	 * a string representation
	 */
	public String asString() {
		StringBuilder sb = new StringBuilder();
		for (Iterator<IGroupBy> iterator = groupBy.iterator(); iterator
				.hasNext();) {
			IGroupBy it = (IGroupBy) iterator.next();
			sb.append(it.asString());
			if (iterator.hasNext()) {
				sb.append(","); //$NON-NLS-1$
			}
		}
		return sb.toString();
	}

	/**
	 * @return all group bys
	 */
	public List<IGroupBy> getGroupBys() {
		return this.groupBy;
	}
	
	
	public void visit(IGroupByVisitor visitor){
		for (IGroupBy gb : getGroupBys()){
			gb.visit(visitor);
		}
	}

}
