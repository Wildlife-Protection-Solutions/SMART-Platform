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
package org.wcs.smart.i2.query.observation.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Group by part which contains a list of group by items
 * 
 * @author Emily
 *
 */
public class GroupByPart {

	public static GroupByPart parse(List<GroupByItem> items) {
		GroupByPart p = new GroupByPart();
		for (GroupByItem i : items)p.addItem(i);
		return p;
	}
	
	private List<GroupByItem> items = new ArrayList<>();
	
	public GroupByPart() {
		
	}
	
	public List<GroupByItem> getItems(){
		return this.items;
	}
	
	public void addItem(GroupByItem item) {
		this.items.add(item);
	}
}
