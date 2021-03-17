/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.model;

import java.util.ArrayList;
import java.util.List;

public class ReportParameter {

	public enum Type {
		DATE,
		TIME,
		DATETIME,
		DOUBLE,
		INTEGER,
		STRING,
		BOOLEAN,
		GROUP,
		LIST,
		OTHER;
	}
	private String name;
	private String displayText;
	private Object defaultValue;
	private Type type;
	private List<ReportParameter> kids;
	
	private List<String[]> options;
	private boolean required = true;
	
	private boolean isMultiList = false;
	
	public ReportParameter(){
		kids = new ArrayList<ReportParameter>();
		options = new ArrayList<>();
	}
	
	public boolean getIsRequired() {
		return this.required;
	}
	
	public void setIsRequired(boolean isRequired) {
		this.required = isRequired;
	}
	
	public String getName(){
		return this.name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public Type getType(){
		return this.type;
	}
	
	public void setType(Type type){
		this.type = type;
	}
	
	public String getDisplayText(){
		return this.displayText;
	}
	public void setDisplayText(String displayText){
		this.displayText = displayText;
	}
	
	public Object getDefaultValue(){
		return this.defaultValue;
	}
	public void setDefaultValue(Object defaultValue){
		this.defaultValue = defaultValue;
	}
	
	public List<ReportParameter> getChildren(){
		return this.kids;
	}
	public void setChildren(List<ReportParameter> kids){
		this.kids = kids;
	}
	
	public List<String[]> getOptions(){
		return this.options;
	}
	public void addOption(String key, String value) {
		options.add(new String[] {key, value});
	}
	
	public void addOption(int index, String key, String value) {
		options.add(index, new String[] {key, value});
	}
	
	public boolean getIsMultiList() {
		return isMultiList;
	}
	public void setIsMultiList(boolean isMultiList) {
		this.isMultiList = isMultiList;
	}
}
