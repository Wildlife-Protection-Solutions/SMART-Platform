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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.Area;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author egouge
 * @since 1.0.0
 */
public class AreaDropItem extends DropItem {

	private Area area = null;
	
	public AreaDropItem(Area area){
		this.area = area;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return area.getType().getGuiName() + " = " + area.getId();
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		return "area:" + area.getType().name() + ":" + area.getKeyId();
	}

	/**
	 *  
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#initializeData(java.lang.Object)
	 */
	@Override
	public void initializeData(Object data) {
		//TODO: implement me

	}

	/**
	 * 
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#isValueItem()
	 */
	@Override
	public boolean isValueItem() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#isFilterItem()
	 */
	@Override
	public boolean isFilterItem() {
		return true;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#isGroupByItem()
	 */
	@Override
	public boolean isGroupByItem() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(area.getId() + " [" + area.getType().getGuiName() + "]");
		initDrag(lbl);

	}

}
