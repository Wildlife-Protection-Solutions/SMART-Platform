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

	private Map<IconItem,Image> images = Collections.synchronizedMap(new HashMap<>());

	private int iconSize = 32;
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
	
	public Image getImage(Object element) {
		if (element == null) return null;
		if (!(element instanceof IconItem)) return null;
		
		IconItem station = (IconItem)element;
		if (images.containsKey(station)) return images.get(station);
		
		Image img = SmartUtils.getImage(HibernateManager.loadIcon(station), iconSize);
		images.put(station, img);
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
