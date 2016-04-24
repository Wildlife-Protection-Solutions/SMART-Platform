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
package org.wcs.smart.connect.cybertracker.dataentry;

import java.util.Stack;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.connect.cybertracker.model.ConnectAlert;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * LabelProvider that provides label for source of {@link ConnectAlert}.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConnectAlertSourceLabelProvider extends ColumnLabelProvider {

	private ConnectCmTreeContentProvider treeProvider;
	private ConnectCmTreeLabelProvider labelProvider;
	
	
	public ConnectAlertSourceLabelProvider(ConfigurableModel model) {
		treeProvider = new ConnectCmTreeContentProvider(false);
		labelProvider = new ConnectCmTreeLabelProvider(model);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof ConnectAlert) {
			ConnectAlert alert = (ConnectAlert) element;
			Stack<String> stack = new Stack<>();
			CmAttribute attribute = alert.getAttrubute();
			Object obj = attribute != null ? new ConnectCmTreeElement(attribute, alert.getAlertItem()) : alert.getAlertItem();
			while (obj != null) {
				stack.push(labelProvider.getText(obj));
				obj = treeProvider.getParent(obj);
			}
			StringBuilder sb = new StringBuilder();
			while (!stack.isEmpty()) {
				if (sb.length() > 0) {
					sb.append(" -> "); //$NON-NLS-1$
				}
				sb.append(stack.pop());
			}
			return sb.toString();
			
		}
		return super.getText(element);
	}
	
}
