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
package org.wcs.smart.er.query.ui.panels.item;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.query.common.ui.itempanel.IItemTreeNode;

/**
 * Survey values tree node.
 * 
 * @author Emily
 *
 */
public class SurveyValuesTreeNode implements IItemTreeNode{

	public static final String KEY = "surveyitem"; //$NON-NLS-1$
	
	private ITreeContentProvider provider;
	private LabelProvider labelprovider;
		
	public enum Node {
		MISSION_LENGTH("Mission Length");
		
		private String guiName;
		
		private Node(String name){
			guiName = name;
		}
	};
	/**
	 * type of node
	 * @param type
	 */
	public SurveyValuesTreeNode(){
		provider = new ITreeContentProvider(){

			@Override
			public void dispose() {
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
			}

			@Override
			public Object[] getElements(Object inputElement) {
				return Node.values();
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				return null;
			}

			@Override
			public Object getParent(Object element) {
				return SurveyValuesTreeNode.this;
			}

			@Override
			public boolean hasChildren(Object element) {
				return false;
			}
			
		};
		labelprovider = new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof Node){
					return ((Node)element).guiName;
				}
				return super.getText(element);
			}
			
			@Override
			public Image getImage(Object element){
				return null;
			}
		};
	}
	@Override
	public String getName() {
		return "Survey Values";
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
		return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SURVEY_ICON);
	}

}
