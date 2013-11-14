package org.wcs.smart.datamodelmatcher.ui;

public class MistItem {

	private String category;
	private String cat2;
	private String cat3;
	private String cat4;
	private String cat5;
	private String cat6;
	private String cat7;
	
	private String value;

	public MistItem(String category){
		this.category = category;
	}
	
	public String getText() {
		return category;
		//cat1.concat(".").concat(cat2).concat(".").concat(cat3).concat(".").concat(cat4).concat(".").concat(cat5).concat(".").concat(cat6).concat(".").concat(cat7).concat(".").concat(value);
	}
	
	
	
}
