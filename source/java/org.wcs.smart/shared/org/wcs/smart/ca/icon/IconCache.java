package org.wcs.smart.ca.icon;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.wcs.smart.ca.IconItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;

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

	private int iconSize = 32;
	private IconSetOption iconType = IconSetOption.DEFAULT;
	
	/**
	 * Creates a new icon cache that gets disposed when 
	 * ui control gets disposed
	 * 
	 * @param dispose
	 */
	public IconCache(Control dispose) {
		this(dispose, 32);
	}
	
	/**
	 * Creates a new icon cache the user MUST dispose of 
	 * it when finished with it
	 */
	public IconCache() {
		this(null, 32);
	}
	
	public IconCache(Control dispose, int iconSize) {
		if(dispose != null) {
			dispose.addListener(SWT.Dispose, e->dispose());
		}
		this.iconSize = iconSize;
	}
	
	public void setIconSetOption(IconSetOption option) {
		this.iconType = option;
	}
	public Image getImage(Object element) {
		if (iconSize < 0) return null;
		if (element == null) return null;
		if (!(element instanceof IconItem)) return null;
		
		IconItem item = (IconItem)element;
		if (images.containsKey(item)) return images.get(item);
		
		
		Image img = null;
		if (iconType == IconSetOption.DEFAULT) {
			img = SmartUtils.getImage(HibernateManager.loadIcon(item), iconSize);
		}else if (iconType == IconSetOption.ALL) {
			img = SmartUtils.generateImage(HibernateManager.loadIcon(item), iconSize);
		}
		
		images.put(item, img);
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
