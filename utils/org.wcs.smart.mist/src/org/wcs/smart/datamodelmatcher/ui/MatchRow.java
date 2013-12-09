package org.wcs.smart.datamodelmatcher.ui;


 

public class MatchRow {
	private boolean matched = false;
	private MistItem mistItem;
	private SmartItem smartItem;
	
	public MatchRow(){
		mistItem = new MistItem();
		smartItem = new SmartItem();
		matched = false;
	}
	
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
	
	public void setSmartItem(String s){
		smartItem.setConcatKey(s);
		matched = true;
	}
	
	public void setMistItem(String s){
		mistItem.setCategory(s);
	}
	
	public boolean getMatched(){
		return matched;
	}
	
}
