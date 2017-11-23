package org.wcs.smart.asset.ui;

import org.wcs.smart.asset.AssetPlugIn;

public class DataDisplaySettings {

	public enum IconSize{
		SMALL(50),
		MEDIUM(100),
		LARGE(200);
		
		int size;
		
		IconSize(int size){
			this.size = size;
		}
		
		public int getSize() {
			return this.size;
		}
	}
	
	public enum DisplayType{
		IMAGES_ONLY,
		OBS_AND_IMAGES
	}
	
	
	
	private IconSize size = IconSize.MEDIUM;
	private DisplayType data = DisplayType.OBS_AND_IMAGES;
	
	private DataDisplaySettings() {
		
	}

	public void setIconsSize(IconSize size) {
		this.size = size;
		save();
	}
	
	public IconSize getIconSize() {
		return this.size;
	}
	
	public void setDisplayType(DisplayType dataType) {
		this.data = dataType;
		save();
	}
	
	public DisplayType getDisplayType() {
		return this.data;
	}
	
	
	private void save() {
		AssetPlugIn.getDefault().getPreferenceStore().setValue("IconSize", size.name());
		AssetPlugIn.getDefault().getPreferenceStore().setValue("DisplayType", data.name());
	}
	
	private void load() {
		String v = AssetPlugIn.getDefault().getPreferenceStore().getString("IconSize");
		try {
			this.size = IconSize.valueOf(v);
		}catch(Exception ex) {
			
		}
		v = AssetPlugIn.getDefault().getPreferenceStore().getString("DisplayType");
		try {
			this.data = DisplayType.valueOf(v);
		}catch(Exception ex) {
			
		}
	}
	
	public static DataDisplaySettings getSettings() {
		DataDisplaySettings settings = new DataDisplaySettings();
		settings.load();
		return settings;
	}
}
