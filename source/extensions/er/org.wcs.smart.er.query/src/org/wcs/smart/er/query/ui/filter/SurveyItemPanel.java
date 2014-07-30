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
package org.wcs.smart.er.query.ui.filter;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.query.common.ui.itempanel.IItemTreeNode;

/**
 * Tree node for data model.
 * @author Emily
 *
 */
public class SurveyItemPanel implements IItemTreeNode{

	public static final String KEY = "surveyitem"; //$NON-NLS-1$
	
	private ITreeContentProvider provider;
	private LabelProvider labelprovider;
	
	private String name;
	
	
	/**
	 * type of node
	 * @param type
	 */
	public SurveyItemPanel(){
		provider = new SurveyItemContentProvider();
		labelprovider = new LabelProvider(){
			public String getText(Object element){
				return ((SurveyItemContentProvider.Node)element).guiName;
			}
		};
	}
	@Override
	public String getName() {
		return name;
	}

	@Override
	public ITreeContentProvider getContentProvider() {
		return provider;
	}

	@Override
	public ILabelProvider getLabelProvider() {
		return labelprovider;
	}
	@Override
	public String getKey() {
		return KEY;
	}
	@Override
	public Image getImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DATA_MODEL_ICON);
	}

}
