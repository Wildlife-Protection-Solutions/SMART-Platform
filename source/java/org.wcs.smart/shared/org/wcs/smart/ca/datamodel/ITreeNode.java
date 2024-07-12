/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.ca.datamodel;

import java.util.List;

import org.wcs.smart.ca.IconItem;

/**
 * An tree node element
 * 
 * @author Emily
 * @since 8.1.0
 */
public interface ITreeNode<T extends ITreeNode<T>> extends HkeyObject, IconItem{

	public static final String FULL_NAME_SEPARATOR = " - "; //$NON-NLS-1$

	public int getNodeOrder();
	public void setNodeOrder(int nodeOrder);

	public String getKeyId();
	public void setKeyId(String key);
	
	public String getHkey();
	public void setHkey(String hkey);
	
	public List<T> getChildren();
	public List<T> getActiveChildren();
	public void setChildren(List<T> children);
	
	public ITreeNode<T> getParent();
	public void setParent(T parent);

	public void setIsActive(boolean isActive);
	public boolean getIsActive();
	
	public String getName();
	
	/**
	 * Updates the hkey of this object
	 * and children tree nodes.
	 */
	public default void updateHkey(){
		setHkey(computeHkey());
		
		if (getChildren() != null){
			for (T child : getChildren()){
				child.updateHkey();
			}
		}
	}
	
	
	/**
	 * Computes the hkey for the given category.
	 * 
	 * @return the hkey for this category.
	 */
	public default String computeHkey(){
		if (getParent() == null){
			return this.getKeyId() + HkeyObject.HKEY_SEPERATOR;
		}
		return getParent().computeHkey() + this.getKeyId() + HkeyObject.HKEY_SEPERATOR;
	}
	
	
	
	/**
	 * 
	 * @return the attribute name concatenated with
	 * all parent attribute names.
	 */
	public default String getFullName(){
		if (getParent() == null){
			return getName();
		}else{
			return getName() + FULL_NAME_SEPARATOR + getParent().getFullName(); 
		}
	}
	
	
	public default void accept(ITreeNodeVisitor<T> visitor) {
		if (!visitor.visit(this)) return;
		if (getChildren() == null) return;
		for (ITreeNode<T> kid : getChildren()) {
			kid.accept(visitor);
		}
	}
}
