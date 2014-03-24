/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.wcs.smart.datamodelmatcher.ui;

public class MistItem {

	private String category;
	private String cat1;
	private String cat2;
	private String cat3;
	private String cat4;
	private String cat5;
	private String cat6;
	private String cat7;
	private String cat8;
	private String cat9;
	
	public MistItem(String category){
		this.category = category;
	}
	
	public MistItem() {
		category = new String();
	}

	public MistItem(String string1, String string2, String string3,
			String string4, String string5, String string6, String string7,
			String string8, String string9) {
		if(string1 != null){
			this.cat1 = string1;
		}else{
			this.cat1 = "";
		}
		if(string2 != null){
			this.cat2 = string2;
		}else{
			this.cat2 = "";
		}
		if(string3 != null){
			this.cat3 = string3;
		}else{
			this.cat3 = "";
		}
		if(string4 != null){
			this.cat4 = string4;
		}else{
			this.cat4 = "";
		}
		if(string5 != null){
			this.cat5 = string5;
		}else{
			this.cat5 = "";
		}
		if(string6 != null){
			this.cat6 = string6;
		}else{
			this.cat6 = "";
		}
		if(string7 != null){
			this.cat7 = string7;
		}else{
			this.cat7 = "";
		}
		if(string8 != null){
			this.cat8 = string8;
		}else{
			this.cat8 = "";
		}
		if(string9 != null){
			this.cat9 = string9;
		}else{
			this.cat9 = "";
		}
		String concated = cat1 + (".") + cat2 + (".") + cat3 + (".") + cat5 + (".") + cat6 + (".") + cat7 + (".") + cat8 + (".") + cat9;
		
		//strip all trailing "." 's
		while(concated.substring(concated.length() - 1).equals(".")){
			concated = concated.substring(0, concated.length()-1);
		}
		concated = concated + ".|" + cat4.replaceAll("[.]", ""); //remove period in the item
		//Add one "." back in before the |, FME is expecting this from the way it was processing the data before...
		
		
		this.category = concated.toLowerCase();
	}

	public String getText() {
		//doing all the data processing for special characters etc here, before we save to a file etc so that any old version files will still have it done each time they are re-saved 
		
		//make sure there is a period at the end of the category list, before the | 
		char prebang = category.charAt((category.indexOf("|") - 1));
		if(!(prebang == '.')){
			category = category.replace("|", ".|");
		}
		
		//lowercase
		category = category.toLowerCase();
		
		category = category.replaceAll("\\s+",""); //remove whitespace
		category = category.replaceAll("[-(),_/'><]", "");
		//----
		
		
		return category;
	}
	
	public void setCategory(String category){
		this.category = category;
	}
	
	public String getCat1(){
		return cat1;
	}
	public void setCat1(String s){
		this.cat1 = s;
	}
	
	public String getCat2(){
		return cat2;
	}
	public void setCat2(String s){
		this.cat2 = s;
	}
	
	public String getCat3(){
		return cat3;
	}
	public void setCat3(String s){
		this.cat3 = s;
	}
	
	public String getCat4(){
		return cat4;
	}
	public void setCat4(String s){
		this.cat4 = s;
	}
	
	public String getCat5(){
		return cat5;
	}
	public void setCat5(String s){
		this.cat5 = s;
	}
	
	public String getCat6(){
		return cat6;
	}
	public void setCat6(String s){
		this.cat6 = s;
	}
	
	public String getCat7(){
		return cat7;
	}
	public void setCat7(String s){
		this.cat7 = s;
	}
	
	public String getCat8(){
		return cat8;
	}
	public void setCat8(String s){
		this.cat8 = s;
	}
	
	public String getCat9(){
		return cat9;
	}
	public void setCat9(String s){
		this.cat9 = s;
	}

	public boolean equalTo(MistItem mist) {
		return this.category.equals(mist.getText());
	}
	
}
