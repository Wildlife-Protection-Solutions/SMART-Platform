/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="smart.paws_simple_class")
public class PawsSimpleClass extends AbstractPawsClass{

	private String categoryKey;
	private String attributeKey;
	private String listItemKey;
	private String treeNodeKey;
	
	@Column(name="category_hkey")
	public String getCategoryHkey() {
		return this.categoryKey;
	}
	
	public void setCategoryHkey(String categoryKey) {
		this.categoryKey = categoryKey;
	}
	
	@Column(name="attribute_key")
	public String getAttributeKey() {
		return this.attributeKey;
	}
	
	public void setAttributeKey(String attributeKey) {
		this.attributeKey = attributeKey;
	}
	
	@Column(name="list_key")
	public String getAttributeListItemKey() {
		return this.listItemKey;
	}
	
	public void setAttributeListItemKey(String listItemKey) {
		this.listItemKey = listItemKey;
	}
	
	@Column(name="tree_hkey")
	public String getAttributeTreeNodeHkey() {
		return this.treeNodeKey;
	}
	
	public void setAttributeTreeNodeHkey(String treeNodeKey) {
		this.treeNodeKey = treeNodeKey;
	}
}
