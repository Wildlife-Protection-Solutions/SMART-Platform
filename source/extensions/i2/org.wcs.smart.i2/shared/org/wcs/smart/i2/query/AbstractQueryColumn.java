/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.query;


/**
 * Abstract query column that includes name, key and visibility values
 * 
 * @author Emily
 *
 */
public abstract class AbstractQueryColumn implements IQueryColumn{

	protected String name = null;
	protected String key = null;
	protected boolean isVisible = true;
	
	protected String tooltip = null;
	
	public AbstractQueryColumn(String columnName, String key){
		this.name = columnName;
		this.key = key;
		this.tooltip = columnName;
	}
	
	@Override
	public String getColumnName(){
		return this.name;
	}
	
	@Override
	public String getKey(){
		return this.key;
	}
	
	@Override
	public boolean isVisible(){
		return this.isVisible;
	}
	
	public void setVisible(boolean isVisible){
		this.isVisible = isVisible;
	}
	
	public String getTooltip(){
		return tooltip;
	}
}
