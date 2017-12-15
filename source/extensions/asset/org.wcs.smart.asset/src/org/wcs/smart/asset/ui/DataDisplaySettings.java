package org.wcs.smart.asset.ui;

import org.wcs.smart.asset.AssetPlugIn;

public class DataDisplaySettings {

	private static final String PAGE_SIZE_KEY = "PageSize"; //$NON-NLS-1$
	private static final String ICON_SIZE_KEY = "IconSize"; //$NON-NLS-1$

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
	
	public enum PageSize{
		TEN(10, "10"),
		TWENTYFIVE(25, "25"),
		FIFTY(50, "50");
		
		int size;
		String optionName;
		
		PageSize(int size, String optionName){
			this.size = size;
			this.optionName = optionName;
		}
		
		public int getPageSize() {
			return this.size;
		}
		
		public String getOptionName() {
			return this.optionName;
		}
	}
	
	
	private IconSize iconSize = IconSize.MEDIUM;
	private PageSize pageSize = PageSize.TWENTYFIVE;
	
	private DataDisplaySettings() {
		
	}

	public void setIconSize(IconSize size) {
		this.iconSize = size;
		save();
	}
	
	public IconSize getIconSize() {
		return this.iconSize;
	}
	
	public void setPageSize(PageSize size) {
		this.pageSize = size;
		save();
	}
	
	public PageSize getPageSize() {
		return this.pageSize;
	}
	
	private void save() {
		AssetPlugIn.getDefault().getPreferenceStore().setValue(ICON_SIZE_KEY, iconSize.name());
		AssetPlugIn.getDefault().getPreferenceStore().setValue(PAGE_SIZE_KEY, pageSize.name());
	}
	
	private void load() {
		String v = AssetPlugIn.getDefault().getPreferenceStore().getString(ICON_SIZE_KEY);
		try {
			this.iconSize = IconSize.valueOf(v);
		}catch(Exception ex) {
			
		}
		v = AssetPlugIn.getDefault().getPreferenceStore().getString(PAGE_SIZE_KEY);
		try {
			this.pageSize = PageSize.valueOf(v);
		}catch(Exception ex) {
			
		}
	}
	
	public static DataDisplaySettings getSettings() {
		DataDisplaySettings settings = new DataDisplaySettings();
		settings.load();
		return settings;
	}
}
