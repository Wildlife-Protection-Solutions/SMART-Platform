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
package org.wcs.smart.ui;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.ca.IconItem;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.icon.IconCache;

/**
 * LabelProvider for {@link NamedItem}
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class NamedIconItemLabelProvider extends NamedItemLabelProvider {

	protected IconCache images ;
	
	private int iconSize = 32;
	
	/**
	 * Create a new label provide that generates icons with default size (32)
	 * 
	 * @param iconSize
	 */
	public NamedIconItemLabelProvider() {
		images = new IconCache();
	}
	
	/**
	 * Create a new label provide that generates icons of a specific size. If size
	 * is < 0 no icon will be generated.
	 * 
	 * @param iconSize
	 */
	public NamedIconItemLabelProvider(int iconSize) {
		this();
		images = new IconCache(null, iconSize);
	}
	
	@Override
	public Image getImage(Object element) {
		if (iconSize <= 0) return null;
		
		if (element instanceof IconItem) {
			IconItem iconItem = (IconItem) element;
			return images.getImage(iconItem);
		}
		return null;
	}

	@Override
	public void dispose() {
		super.dispose();
		images.dispose();
	}
	
	public void clearCachedImages() {
		images.clearCache();
	}
	
}
