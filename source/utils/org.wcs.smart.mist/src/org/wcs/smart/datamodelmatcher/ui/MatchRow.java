package org.wcs.smart.datamodelmatcher.ui;


 

public class MatchRow {
	private boolean matched;
	private MistItem mistItem;
	private SmartItem smartItem;
	
	public MatchRow(boolean matched, MistItem mistItem, SmartItem smartItem){
		this.matched = matched;
		this.mistItem = mistItem;
		this.smartItem = smartItem;
	}
	
	public MistItem getMistItem(){
		return mistItem;
	}
	
	public SmartItem getSmartItem(){
		return smartItem;
	}
	
	public boolean getMatched(){
		return matched;
	}
	
}
