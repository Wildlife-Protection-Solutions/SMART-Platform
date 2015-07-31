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
package org.wcs.smart.query.ui.model.impl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.query.model.AllCategory;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.ValueItemLabelProvider;
import org.wcs.smart.query.model.summary.IValueItem.ValueType;

/**
 * A drop item for a category value item.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class CategoryValueDropItem extends AbstractValueDropItem {

	private Category category = null;

	private Combo combo = null;
	private Font smallerFont = null;
	private int defaultSelection = 0;
	
	public CategoryValueDropItem(boolean hasEnounter, 
			Category category) {
		super(hasEnounter);
		this.category = category;
	}
	
	/**
	 * In this case no category is supplied so it
	 * is assume all categories are to be included
	 */
	public CategoryValueDropItem(boolean hasEncounter){
		this(hasEncounter, null);
	}

	/**
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
		if (smallerFont != null){
			smallerFont.dispose();
		}
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.AbstractValueDropItem#getValueText()
	 */
	@Override
	public String getValueText() {
		String x = combo.getItem(combo.getSelectionIndex()) + " "; //$NON-NLS-1$
		if (category != null){
			x += category.getFullCategoryName();
		}else{
			x += AllCategory.INSTANCE.getName();
		}
		return x;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.AbstractValueDropItem#getValueQueryPart()
	 */
	@Override
	public String getValueQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append("category:sum:"); //$NON-NLS-1$
		ValueType[] values = ValueType.values();
		for (int i = 0; i < values.length; i++){
			if (ValueItemLabelProvider.INSTANCE.getLabel(values[i]).equals(combo.getItem(combo.getSelectionIndex()))){
				sb.append(values[i].key + ":"); //$NON-NLS-1$
				break;
			}
		}
		if (category != null){
			sb.append(category.getHkey());
		}
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

		GridLayout gl = new GridLayout(2, false);
		gl.marginTop = 0;
		gl.marginBottom = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));

		combo = new Combo(main, SWT.READ_ONLY);
		IValueItem.ValueType[] values = IValueItem.ValueType.values();
		for (int i = 0; i < values.length; i++){
			combo.add(ValueItemLabelProvider.INSTANCE.getLabel(values[i]));
		}
		combo.select(defaultSelection);
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				defaultSelection = combo.getSelectionIndex();
				CategoryValueDropItem.this.queryChanged();
			}
		});
		
		FontData fd = (combo.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		combo.setFont(smallerFont);
		
		Label lblText = new Label(main, SWT.NONE);
		StringBuilder sb = new StringBuilder();
		if (category != null){
			sb.append(category.getFullCategoryName());
		}else{
			sb.append(AllCategory.INSTANCE.getName());
		}
		lblText.setText( formatStringForLabel(sb.toString()));

		initDrag(main);
		initDrag(lblText);

	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.AbstractValueDropItem#initializeValueData(java.lang.Object)
	 */
	@Override
	protected void initializeValueData(Object data) {
		
		IValueItem.ValueType[] values = IValueItem.ValueType.values();
		for (int i = 0; i < values.length; i++){
			if (((String)data).equals(values[i].key)){
				defaultSelection = i;
				break;
			}		
		}
	}

}