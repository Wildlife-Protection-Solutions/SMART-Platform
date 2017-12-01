package org.wcs.smart.asset.ui;

import org.wcs.smart.asset.AssetPlugIn;

public class DataDisplaySettings {

	public enum IconSize{
		SMALL(150, "Small"),
		MEDIUM(250, "Medium"),
		LARGE(350, "Large");
		
		int size;
		String optionName;
		
		IconSize(int size, String optionName){
			this.size = size;
			this.optionName = optionName;
		}
		
		public int getSize() {
			return this.size;
		}
		
		public String getOptionName() {
			return this.optionName;
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
