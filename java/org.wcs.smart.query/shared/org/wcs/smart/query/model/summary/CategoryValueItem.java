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
package org.wcs.smart.query.model.summary;

import org.wcs.smart.query.model.filter.IValueVisitor;

/**
 * A category value item that represents computing
 * the total number of observations with the given
 * category.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class CategoryValueItem implements IValueItem {
	
	/**
	 * Creates a new category value item of the form
	 * "category:sum:<hkey>"
	 * 
	 * @param key
	 * @return
	 */
	public static CategoryValueItem createItem(String key){
		return new CategoryValueItem(key);
	}
	
	private String key;
	protected String categoryHkey;
	protected IValueItem.ValueType type;
	
	/**
	 * Creates a new category value item.
	 * 
	 * @param key category value key
	 */
	public CategoryValueItem(String key){
		this.key = key;
		String[] bits = key.split(":"); //$NON-NLS-1$
		this.type = ValueType.OBSERVATION;
		for (ValueType vt : ValueType.values()){
			if (vt.key.endsWith(bits[2])){
				this.type = vt;
				break;
			}
		}
		if (bits.length >= 4){
			this.categoryHkey = bits[3];
		}else{
			this.categoryHkey = null;
		}
	}
	
	/**
	 * @return the category hkey
	 */
	public String getCategoryHKey(){
		return this.categoryHkey;
	}
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asString()
	 */
	@Override
	public String asString(){
		return this.key;
	}
	
	/**
	 * 
	 * @return the type of category value
	 */
	public ValueType getType(){
		return this.type;
	}
	
	public void accept(IValueVisitor visitor){
		visitor.visit(this);
	}
	
}
