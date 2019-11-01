package org.wcs.smart.i2.ui;

import java.util.HashMap;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecordSource;

public enum Resources {
	
	INSTANCE;
	
	private HashMap<IntelProfile, Color> colorCache = new HashMap<>();
	private HashMap<IntelProfile, Image> profileImages = new HashMap<>();
	private HashMap<IntelEntityType, Image> typeImages = new HashMap<>();
	private HashMap<IntelRecordSource, Image> recordImages = new HashMap<>();
	
	public void dispose() {
		for (Color c : colorCache.values()) c.dispose();
		colorCache.clear();
		
		clearEntityTypeImageCache();
		clearRecordSourceImageCache();
	}
	
	public void clearEntityTypeImageCache() {
		for (Image i : typeImages.values()) i.dispose();
		typeImages.clear();
	}
	
	public void clearRecordSourceImageCache() {
		for (Image i : recordImages.values()) i.dispose();
		recordImages.clear();
	}
	
	public Color getColor(IntelProfile profile) {
		if (profile.getColor() == null) {
			if (colorCache.containsKey(profile)) colorCache.get(profile).dispose();
			colorCache.remove(profile);
			
			Image i = profileImages.get(profile);
			if (i != null) i.dispose();
			profileImages.remove(profile);
			
			return null;
		}
		
		if (!colorCache.containsKey(profile)) {
			Color cc = new Color(Display.getDefault(), profile.getColorObj().getRed(), profile.getColorObj().getGreen(), profile.getColorObj().getBlue());
			colorCache.put(profile, cc);
			return cc;
		}
		
		Color cc = colorCache.get(profile);
		if (cc.getRed() == profile.getColorObj().getRed() &&
				cc.getGreen() == profile.getColorObj().getGreen() &&
						cc.getBlue() == profile.getColorObj().getBlue()) return cc;
		
		cc.dispose();
		cc = new Color(Display.getDefault(), profile.getColorObj().getRed(), profile.getColorObj().getGreen(), profile.getColorObj().getBlue());
		colorCache.put(profile, cc);
		
		Image i = profileImages.get(profile);
		if (i != null) i.dispose();
		profileImages.remove(profile);
		
		return cc;
		
	}

	public Image getImage(IntelProfile profile) {
		Color c = getColor(profile);
		if (profileImages.containsKey(profile)) return profileImages.get(profile);
		if (c == null) return null;
		
		int size = 16;
		Image img = new Image(Display.getDefault(), size, size);
		GC gc = new GC(img);
		try {
			gc.setBackground(c);
			gc.setForeground(c);
			gc.fillRoundRectangle(0, 0, size-1, size-1, 3, 3);
		}finally {
			gc.dispose();
		}
		profileImages.put(profile, img);
		return img;
	}
	public Image getImage(IntelEntityType entity) {
		if (typeImages.containsKey(entity)) return typeImages.get(entity);
		if (entity.getIcon() == null) return null;
		try {
			Image i =  AWTSWTImageUtils.createImageDescriptor(entity.getIconAsImage()).createImage();
			typeImages.put(entity, i);
			return i;
		}catch (Exception ex) {
			Intelligence2PlugIn.log(ex.getMessage(),ex);
			return null;
		}
	}
	
	public Image getImage(IntelRecordSource source) {
		if (source == null) return null;
		if (recordImages.containsKey(source)) return recordImages.get(source);
		if (source.getIcon() == null) return null;
		try {
			Image i =  AWTSWTImageUtils.createImageDescriptor(source.getIconAsImage()).createImage();
			recordImages.put(source, i);
			return i;
		}catch (Exception ex) {
			Intelligence2PlugIn.log(ex.getMessage(),ex);
			return null;
		}
	}
}
