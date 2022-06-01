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

import java.util.HashMap;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.ca.IconItem;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.util.SmartUtils;

/**
 * LabelProvider for {@link NamedItem}
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class NamedIconItemLabelProvider extends NamedItemLabelProvider {

	protected HashMap<Object, Image> images = new HashMap<>();
	
	private int iconSize = 32;
	
	public NamedIconItemLabelProvider() {
		
	}
	
	public NamedIconItemLabelProvider(int iconSize) {
		this();
		this.iconSize = iconSize;
	}
	
	@Override
	public Image getImage(Object element) {
		if (element instanceof IconItem) {
			IconItem team = (IconItem) element;

			if (images.containsKey(team))
				return images.get(team);

			Image img = SmartUtils.getImage(team.getIcon(), iconSize);
			images.put(team, img);
			return img;
		}
		return null;
	}

	@Override
	public void dispose() {
		super.dispose();
		for (Image i : images.values()) {
			if (i != null) i.dispose();
		}
		images.clear();
	}
}
