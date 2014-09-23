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
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.query.model.summary.IValueItem;

/**
 * Drop item that represents counting items from
 * a list attributes.
 * 
 * @author Emily
 *
 */
public class AttributeListValueDropItem extends AbstractValueDropItem {

	private AttributeListItem item = null;
	private Category category = null; 
	
	private Combo combo = null;
	private Font smallerFont = null;
	private int defaultSelection = 0;
	
	public AttributeListValueDropItem(boolean hasEncounter,
			AttributeListItem item, Category category){
		super(hasEncounter);
		this.item = item;
		this.category = category;
	}
	public AttributeListValueDropItem(boolean hasEncounter,AttributeListItem item){
		this(hasEncounter, item, null);
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
	
	@Override
	protected String getValueQueryPart() {
		StringBuilder sb = new StringBuilder();
		if (category != null){
			sb.append("category:"); //$NON-NLS-1$
			sb.append(category.getHkey());
			sb.append(":"); //$NON-NLS-1$
		}
		sb.append("attribute:"); //$NON-NLS-1$
		sb.append(item.getAttribute().getType().typeKey);
		sb.append(":sum:"); //$NON-NLS-1$
		IValueItem.ValueType[] values = IValueItem.ValueType.values();
		for (int i = 0; i < values.length; i++){
			if (values[i].guiLabel.equals(combo.getItem(combo.getSelectionIndex()))){
				sb.append(values[i].key);
				break;
			}
		}
		sb.append(":"); //$NON-NLS-1$
		sb.append(item.getAttribute().getKeyId());
		sb.append("."); //$NON-NLS-1$
		sb.append(item.getKeyId());
		return sb.toString();
	}

	@Override
	protected String getValueText() {
		StringBuilder sb = new StringBuilder();
		sb.append(combo.getItem(combo.getSelectionIndex()));
		sb.append(" "); //$NON-NLS-1$
		sb.append(item.getName());
		sb.append(" ("); //$NON-NLS-1$
		sb.append(item.getAttribute().getName());
		if (category != null){
			sb.append(" - "); //$NON-NLS-1$
			sb.append(category.getFullCategoryName());
		}
		sb.append(")"); //$NON-NLS-1$
		return sb.toString();
	}

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
			combo.add(values[i].guiLabel);
		}
		combo.select(defaultSelection);
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				defaultSelection = combo.getSelectionIndex();
				AttributeListValueDropItem.this.queryChanged();
			}
		});
		
		FontData fd = (combo.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		combo.setFont(smallerFont);
		
		Label lblText = new Label(main, SWT.NONE);
		StringBuilder tooltip = new StringBuilder();
		
		StringBuilder sb = new StringBuilder();
		sb.append(item.getName());
		sb.append(" ("); //$NON-NLS-1$
		sb.append(item.getAttribute().getName());
		tooltip.append(item.getAttribute().getName());
		if (category != null){
			sb.append(" - "); //$NON-NLS-1$
			sb.append(category.getName());
			tooltip.append(" - "); //$NON-NLS-1$
			tooltip.append(category.getFullCategoryName());
		}
		sb.append(")"); //$NON-NLS-1$
		lblText.setText( formatStringForLabel(sb.toString()));
		lblText.setToolTipText(tooltip.toString());
		
		initDrag(main);
		initDrag(lblText);


	}

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
