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
package org.wcs.smart.er.query.ui.dropitems;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.er.query.filter.summary.MissionValueItem;
import org.wcs.smart.query.ui.model.IValueDropItem;
import org.wcs.smart.query.ui.model.impl.AbstractValueDropItem;

/**
 * Total mission track length drop item.
 * 
 * @author Emily
 *
 */
public class MissionValueDropItem extends AbstractValueDropItem implements IValueDropItem{
	
	private MissionValueItem.ValueItem item;
	
	public MissionValueDropItem(MissionValueItem.ValueItem item){
		super(true);
		this.item = item;
	}
	
	@Override
	protected String getValueQueryPart() {
		return item.key;
	}

	@Override
	protected String getValueText() {
		return item.guiName;
	}

	@Override
	protected void createValueComposite(Composite parent) {
		Label lblText = new Label(parent, SWT.NONE);
		lblText.setText( formatStringForLabel(getText()));
		initDrag(lblText);
	}

	@Override
	protected void initializeValueData(Object data) {

	}
}
