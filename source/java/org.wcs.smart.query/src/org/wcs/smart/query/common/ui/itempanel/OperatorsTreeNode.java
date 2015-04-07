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
package org.wcs.smart.query.common.ui.itempanel;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.filter.Operator;

/**
 * Item Tree Node to display operators.
 * @author Emily
 *
 */
public class OperatorsTreeNode implements IItemTreeNode{

	public static final String KEY = "filteroperators"; //$NON-NLS-1$
	
	private static final LabelProvider lblProvider = new LabelProvider(){
		public String getText(Object element){
			if (element instanceof Operator){
				return ((Operator) element).getGuiValue();
			}
			return super.getText(element);
		}
	};
	
	private OperatorContentProvider provider = new OperatorContentProvider();
	
	@Override
	public String getName() {
		return Messages.OperatorsTreeNode_OperatorsLabel;
	}

	@Override
	public Image getImage() {
		return null;
	}

	@Override
	public ITreeContentProvider getContentProvider() {
		return provider;
	}

	@Override
	public ILabelProvider getLabelProvider() {
		return lblProvider;
	}

	@Override
	public String getKey() {
		return KEY;
	}

	
	private class OperatorContentProvider implements ITreeContentProvider {

		private List<Operator> ops;
		
		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof List){
				this.ops = (List<Operator>) newInput;
			}else if (newInput instanceof Operator[]){
				this.ops = Arrays.asList(((Operator[])newInput));
			}
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return this.ops.toArray(new Operator[ops.size()]);
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}

	}
}
