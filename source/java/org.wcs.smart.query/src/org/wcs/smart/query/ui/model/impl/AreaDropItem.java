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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.Area;
import org.wcs.smart.query.model.filter.AreaFilter;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IFilterDropItem;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * An area drop item for area filters.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class AreaDropItem extends DropItem implements IFilterDropItem {

	private Area area = null;
	private AreaFilter.AreaFilterGeometryType geomType;
	
	/**
	 * Creates a new are drop item.
	 * 
	 * @param area the represented area
	 * @param geomType the type of geometry to apply the filter to
	 */
	public AreaDropItem(Area area, AreaFilter.AreaFilterGeometryType geomType){
		this.area = area;
		this.geomType = geomType;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return getLabel(area.getType()) + " = " + area.getName(); //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		return "area:" + geomType.getKey() + ":" + area.getType().name() + ":" + area.getKeyId();  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 *  
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
		lbl.setText( formatStringForLabel(area.getName() + " [" + getLabel(area.getType()) + "]"));  //$NON-NLS-1$//$NON-NLS-2$
		initDrag(lbl);

	}

	protected String getLabel(Area.AreaType area){
		return SmartLabelProvider.getAreaTypeName(area);
	}
}
