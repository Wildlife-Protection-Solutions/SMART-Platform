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

import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IGroupByDropItem;
import org.wcs.smart.query.ui.model.ListItem;

/**
 * Date group by drop item
 * @author egouge
 * @since 1.0.0
 */
public class DateGroupByDropItem extends DropItem implements IGroupByDropItem {

	
	private IDateGroupBy dateOption;
	
	public DateGroupByDropItem(IDateGroupBy dateOption){
		this.dateOption = dateOption;
	}
	/**
	 * @see org.wcs.smart.query.ui.model.formulaDnd.IGroupByDropItem#getListItem()
	 */
	@Override
	public List<ListItem> getListItem() {
		return null;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return dateOption.getGuiName(Locale.getDefault());
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		return "date:" + dateOption.getKey(); //$NON-NLS-1$
	}

	/**
	 * Does nothing.
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#initializeData(java.lang.Object)
	 */
	@Override
	public void initializeData(Object data) {

	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText( formatStringForLabel(dateOption.getGuiName(Locale.getDefault())));
		
		initDrag(lbl);
	}

}
