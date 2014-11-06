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
package org.wcs.smart.ct2smart.ui;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.ct2smart.matcher.model.CtCategory;
import org.wcs.smart.ct2smart.matcher.model.ExtraAttribute;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class CtCategoryEAComposite extends ExtraAttributeComposite {

	private CtCategory category;

	/**
	 * @param parent
	 * @param lookup
	 */
	public CtCategoryEAComposite(Composite parent, DataModelLookup lookup) {
		super(parent, lookup);
	}

	protected void addExtraAttribute() {
		category.getExtraAttribute().add(new ExtraAttribute());
		getViewer().refresh();
	}
	
	protected void deleteExtraAttribute() {
		Object toDel = ((IStructuredSelection)getViewer().getSelection()).getFirstElement();
		category.getExtraAttribute().remove(toDel);
		getViewer().refresh();
	}

	public void setInput(CtCategory category) {
		this.category = category;
		getViewer().setInput(category.getExtraAttribute());
	}

}
