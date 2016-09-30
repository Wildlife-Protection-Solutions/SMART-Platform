package org.wcs.smart.i2.model;

import java.util.UUID;

import org.eclipse.swt.graphics.Image;

public class IntelWorkingSetItem {

	private IntelWorkingSetCategory category;
	private String label;
	private UUID uuid;
	private Image image;
	
	public IntelWorkingSetItem(IntelWorkingSetCategory category, String label, UUID uuid){
		this.category = category;
		this.label = label;
		this.uuid = uuid;
	}
	
	public IntelWorkingSetItem(IntelWorkingSetCategory category, String label, UUID uuid, Image image){
		this.category = category;
		this.label = label;
		this.uuid = uuid;
	}
	
	public IntelWorkingSetCategory getCategory(){
		return this.category;
	}
	
	public String getLabel(){
		return this.label;
	}
	
	public UUID getUuid(){
		return this.uuid;
	}
	
	public Image getImage(){
		return this.image;
	}
}
