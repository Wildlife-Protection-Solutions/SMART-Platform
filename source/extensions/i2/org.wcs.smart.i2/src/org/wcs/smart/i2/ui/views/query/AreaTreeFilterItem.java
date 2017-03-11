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
package org.wcs.smart.i2.ui.views.query;

import org.wcs.smart.ca.Area;
import org.wcs.smart.i2.query.IntelQueryColumnProvider;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextDropItem;

/**
 * Tree filter item for a specific Conservation Area area
 * 
 * @author Emily
 *
 */
public class AreaTreeFilterItem extends BasicTreeFilterItem{

	private String key;
	private String type;
	private String name;
	
	public AreaTreeFilterItem(Area area) {
		super(area.getName());
		this.key = area.getKeyId();
		this.type = area.getType().name();
		this.name = IntelQueryColumnProvider.generateName(area);
	}
	
	@Override
	public DropItem[] asDropItem() {
		String queryKey = "area:" + type + ":" + key; //$NON-NLS-1$ //$NON-NLS-2$
		return new DropItem[]{new TextDropItem(name, queryKey)};
	}
}
