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
package org.wcs.smart.query.parser.filter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.query.parser.internal.filter.AttributeInfo;
import org.wcs.smart.query.ui.formulaDnd.DropItem;

/**
 * A class to represent and empty filter.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class EmptyFilter implements IFilter {

	@Override
	public String asString() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public String asSql(HashMap<Class<?>, String> tableMapping) {
		return ""; //$NON-NLS-1$
	}

	@Override
	public boolean hasCategoryFilter() {
		return false;
	}

	@Override
	public boolean hasAttributeFilter() {
		return false;
	}

	@Override
	public boolean hasEmployeeFilter() {
		return false;
	}

	@Override
	public void getAttributeFilters(HashSet<AttributeInfo> attributes) {
	}
	
	@Override
	public DropItem[] getDropItems(Session session) throws Exception {
		return new DropItem[]{};
	}

	@Override
	public List<IFilter> getChildren() {
		return null;
	}

}
