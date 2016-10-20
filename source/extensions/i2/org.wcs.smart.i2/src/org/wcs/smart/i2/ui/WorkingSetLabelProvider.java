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
package org.wcs.smart.i2.ui;

import java.util.HashMap;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.i2.model.IntelWorkingSetCategory;
import org.wcs.smart.i2.model.IntelWorkingSetItem;

/**
 * Working set label provider providing labels for working sets, working
 * set categories, and working set item
 * 
 * @author Emily
 *
 */
public class WorkingSetLabelProvider extends LabelProvider {

	private HashMap<IntelWorkingSetItem, Image> images = new HashMap<>();
	
	public String getText(Object element){
		if (element instanceof IntelWorkingSet){
			return ((IntelWorkingSet) element).getName();
		}else if (element instanceof IntelWorkingSetCategory){
			return ((IntelWorkingSetCategory) element).getGuiName();
		}else if (element instanceof IntelWorkingSetItem){
			return ((IntelWorkingSetItem) element).getLabel();
		}
		return super.getText(element);
	}
	
	public Image getImage(Object element){
		if (element instanceof IntelWorkingSetCategory){
			return ((IntelWorkingSetCategory) element).getImage();
		}else if (element instanceof IntelWorkingSetItem){
			IntelWorkingSetItem wi = (IntelWorkingSetItem)element;
			Image img = images.get(wi);
			if (img != null) return img;
			img = wi.getImageDescriptor().createImage();
			images.put(wi, img);
			return img;
			
		}
		return super.getImage(element);
	}
	
	@Override
	public void dispose(){
		super.dispose();
		images.values().forEach(i -> i.dispose());
	}
}
