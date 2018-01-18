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
package org.wcs.smart.common.folder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Implementation for {@link IGroupContentBuilder} that do not perform any grouping.
 *
 * @author elitvin
 * @since 6.0.0
 */
public class NoGroupingContentBuilder implements IGroupContentBuilder {
	@Override
	public List<ITreeElement> applyGrouping(Object input) {
		List<ITreeElement> items = new ArrayList<>();
		if (input instanceof Object[]) {
			Object[] objs = (Object[]) input;
			for (Object o : objs) {
				items.add(new TreeElement(o, null));
			}
			return items;
		} else if (input instanceof Collection<?>) {
			Collection<?> col = (Collection<?>) input;
			for (Object o : col) {
				items.add(new TreeElement(o, null));
			}
			return items;
		}
		items.add(new TreeElement(input, null));
		return items;
	}
}
