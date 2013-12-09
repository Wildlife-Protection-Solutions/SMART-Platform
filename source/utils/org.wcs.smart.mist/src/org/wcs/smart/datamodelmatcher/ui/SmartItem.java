package org.wcs.smart.datamodelmatcher.ui;

public class SmartItem {

	private String smartConcatKey;
	private String categoryKey;
	
	private String attr1key;
	private String attr2key;
	private String attr3key;
	private String attr4key;
	private String attr5key;
	
	private boolean attr1BooleanValue;
	private String attr1TextValue;
	private String attr1ListValue;
	private String attr1TreeValue;
	
	private boolean attr2BooleanValue;
	private String attr2TextValue;
	private String attr2ListValue;
	private String attr2TreeValue;
	
	private boolean attr3BooleanValue;
	private String attr3TextValue;
	private String attr3ListValue;
	private String attr3TreeValue;
	
	private boolean attr4BooleanValue;
	private String attr4TextValue;
	private String attr4ListValue;
	private String attr4TreeValue;
	
	private boolean attr5BooleanValue;
	private String attr5TextValue;
	private String attr5ListValue;
	private String attr5TreeValue;
		
	
	public SmartItem(String smartConcatKey){
		this.smartConcatKey = smartConcatKey;
	}
	public SmartItem() {
		smartConcatKey = new String();
	}
	public String getText(){
		return smartConcatKey;
	}
	
	public void setConcatKey(String s){
		this.smartConcatKey = s;
	}
	
	public void updateItem(String a1, String a2, String a3, String a4, String a5, boolean b1, String text1, String list1, String tree1,
			boolean b2, String text2, String list2, String tree2,
			boolean b3, String text3, String list3, String tree3,
			boolean b4, String text4, String list4, String tree4,
			boolean b5, String text5, String list5, String tree5){
		
			attr1key = a1;
			attr2key = a2;
			attr3key = a3;
			attr4key = a4;
			attr5key = a5;
		
			attr1BooleanValue = b1;
			attr1TextValue = text1;
			attr1ListValue = list1;
			attr1TreeValue = tree1;
		
			attr2BooleanValue = b2;
			attr2TextValue = text2;
			attr2ListValue = list2;
			attr2TreeValue = tree2;
		
			attr3BooleanValue = b3;
			attr3TextValue = text3;
			attr3ListValue = list3;
			attr3TreeValue = tree3;
		
			attr4BooleanValue = b4;
			attr4TextValue = text4;
			attr4ListValue = list4;
			attr4TreeValue = tree4;
		
			attr5BooleanValue = b5;
			attr5TextValue = text5;
			attr5ListValue = list5;
			attr5TreeValue = tree5;
	}
	public String getAttr1key() {
		return attr1key;
	}
	public String getAttr2key() {
		return attr2key;
	}
	public String getAttr3key() {
		return attr3key;
	}
	public String getAttr4key() {
		return attr4key;
	}
	public String getAttr5key() {
		return attr5key;
	}
	
	public boolean getB1() {
		return attr1BooleanValue;
	}
	public boolean getB2() {
		return attr2BooleanValue;
	}
	public boolean getB3() {
		return attr3BooleanValue;
	}
	public boolean getB4() {
		return attr4BooleanValue;
	}
	public boolean getB5() {
		return attr5BooleanValue;
	}
	
	public String getText1() {
		return attr1TextValue;
	}
	public String getText2() {
		return attr2TextValue;
	}
	public String getText3() {
		return attr3TextValue;
	}
	public String getText4() {
		return attr4TextValue;
	}
	public String getText5() {
		return attr5TextValue;
	}
	
	public String getTree1() {
		return attr1TreeValue;
	}
	public String getTree2() {
		return attr2TreeValue;
	}
	public String getTree3() {
		return attr3TreeValue;
	}
	public String getTree4() {
		return attr4TreeValue;
	}
	public String getTree5() {
		return attr5TreeValue;
	}
	
	public String getList1() {
		return attr1ListValue;
	}
	public String getList2() {
		return attr2ListValue;
	}
	public String getList3() {
		return attr3ListValue;
	}
	public String getList4() {
		return attr4ListValue;
	}
	public String getList5() {
		return attr5ListValue;
	}
	
	
	public void setCategoryKey(String c) {
		this.categoryKey = c;;
	}
	public String getCategoryKey() {
		return categoryKey;
	}

	
}
