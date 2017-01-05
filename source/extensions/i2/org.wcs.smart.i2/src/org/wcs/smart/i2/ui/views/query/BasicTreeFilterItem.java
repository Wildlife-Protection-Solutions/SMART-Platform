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
package org.wcs.smart.i2.ui.views.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;

/**
 * Basic tree filter item
 * 
 * @author Emily
 *
 */
public class BasicTreeFilterItem extends FilterTreeItem {

	protected List<FilterTreeItem> kids = null;
	protected FilterTreeItem parent;
	protected ImageDescriptor image;
	
	public BasicTreeFilterItem(String name){
		super(name);	
	}
	
	public ImageDescriptor getImage(){
		return image;
	}
	
	public void setImageDescriptor(ImageDescriptor img){
		this.image = img;
	}
	
	public void setParent(BasicTreeFilterItem parent){
		this.parent = parent;
	}
	
	public void addChild(BasicTreeFilterItem kid){
		if (kids == null) kids = new ArrayList<FilterTreeItem>();
		kid.setParent(this);
		kids.add(kid);
	}
	@Override
	public List<FilterTreeItem> getChildren() {
		if (kids == null || kids.isEmpty()) return null;
		return Collections.unmodifiableList(kids);
	}

	@Override
	public FilterTreeItem getParent() {
		return parent;
	}
	
	@Override
	public DropItem[] asDropItem() {
		return null;
	}

}
