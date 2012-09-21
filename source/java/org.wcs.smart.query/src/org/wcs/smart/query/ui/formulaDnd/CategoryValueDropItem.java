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
package org.wcs.smart.query.ui.formulaDnd;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.datamodel.Category;

/**
 * A drop item for a category value item.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class CategoryValueDropItem extends AbstractValueDropItem {

	private Category category = null;

	public CategoryValueDropItem(Category category) {
		this.category = category;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.AbstractValueDropItem#getValueText()
	 */
	@Override
	public String getValueText() {
		return "Count " + category.getFullCategoryName();
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.AbstractValueDropItem#getValueQueryPart()
	 */
	@Override
	public String getValueQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append("category:sum:");
		sb.append(category.getHkey());
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse
	 * .swt.widgets.Composite)
	 */
	@Override
	protected void createValueComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);

		GridLayout gl = new GridLayout(1, false);
		gl.marginTop = 0;
		gl.marginBottom = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));

		Label lblText = new Label(main, SWT.NONE);
		StringBuilder sb = new StringBuilder();
		sb.append("Count " + category.getFullCategoryName());
		lblText.setText( formatStringForLabel(sb.toString()));

		initDrag(main);
		initDrag(lblText);

	}

	/** Does nothing
	 * @see org.wcs.smart.query.ui.formulaDnd.AbstractValueDropItem#initializeValueData(java.lang.Object)
	 */
	@Override
	protected void initializeValueData(Object data) {
		
	}

}