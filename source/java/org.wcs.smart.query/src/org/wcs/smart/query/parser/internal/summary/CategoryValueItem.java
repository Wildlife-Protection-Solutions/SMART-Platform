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
package org.wcs.smart.query.parser.internal.summary;

import org.hibernate.Session;
import org.wcs.smart.query.ui.formulaDnd.DropItem;

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
	
	public String key;
	
	private String categoryHkey;
	
	public CategoryValueItem(String key){
		this.key = key;
		
		this.categoryHkey = key.split(":")[2];
		
	}
	
	public String asString(){
		return this.key;
	}

	
	public String getName(Session session){
		//TODO: need to find the category name from the database
		return this.key;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asDropItem(org.hibernate.Session)
	 */
	@Override
	public DropItem asDropItem(Session session) {
		// TODO Auto-generated method stub
		return null;
	}
}
