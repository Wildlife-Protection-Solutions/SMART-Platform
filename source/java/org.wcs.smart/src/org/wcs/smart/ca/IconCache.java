/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.ca;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.hibernate.HibernateManager;
/**
 * 
 * @author Emily
 * @since 8.0.0
 *
 */
public class IconCache {
	
	/**
	 * Type of icon to generate for cache 
	 * 
	 */
	public enum IconSetOption{
		/**
		 * Generates a single icon that concatenates all icons
		 */
		ALL,
		/**
		 * Generates a single icon using the default icon set
		 */
		DEFAULT
	}
	
	private Map<IconItem,Image> images = Collections.synchronizedMap(new HashMap<>());

	private IconManager.Size iconSize = IconManager.Size.SMALL;
	private IconSetOption iconType = IconSetOption.DEFAULT;
	
	/**
	 * Creates a new icon cache that gets disposed when 
	 * ui control gets disposed
	 * 
	 * @param dispose
	 */
	public IconCache(Control dispose) {
		this(dispose, IconManager.Size.SMALL);
	}
	
	/**
	 * Creates a new icon cache the user MUST dispose of 
	 * it when finished with it
	 */
	public IconCache() {
		this(null, IconManager.Size.SMALL);
	}
	
	public IconCache(Control dispose, IconManager.Size size) {
		if(dispose != null) {
			dispose.addListener(SWT.Dispose, e->dispose());
		}
		this.iconSize = size;
	}
	
	public void setIconSetOption(IconSetOption option) {
		this.iconType = option;
	}
	
	
	public Image getImage(IconItem element) {
		if (element == null) return null;
		
		if (images.containsKey(element)) return images.get(element);
				
		Image img = null;
		Icon icon = HibernateManager.loadIcon(element.getIcon());
		if (iconType == IconSetOption.DEFAULT) {			
			img = IconManager.INSTANCE.getThumbnail(icon, iconSize);
		}else if (iconType == IconSetOption.ALL) {
			img = IconManager.INSTANCE.generateImage(icon, iconSize);
		}
		
		synchronized (images) {
			Image x = images.remove(element);
			if (x != null && !x.isDisposed()) x.dispose();
			
			images.put(element, img);
		}
		return img;
	}
	
	public void dispose() {
		clearCache();
	}
	
	public void clearCache() {
		for (Image i : images.values()) {
			if (i != null) i.dispose();
		}
		images.clear();
	}
	
	public void clearCache(IconItem element) {
		Image value = images.remove(element);
		if (value != null) value.dispose();
	}
}
