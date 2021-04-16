/*
 * Copyright (C) 2020 Wildlife Conservation Society
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

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.CustomArea;
import org.wcs.smart.query.model.filter.AreaFilter;
import org.wcs.smart.query.ui.model.IFilterDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;

/**
 * An drop item for a custom area filter.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class CustomAreaDropItem extends DropItem implements IFilterDropItem {

	private String customArea;
	private AreaFilter.AreaFilterGeometryType geomType;

	/**
	 * Creates a new are drop item.
	 * 
	 * @param area the represented area
	 * @param geomType the type of geometry to apply the filter to
	 */
	public CustomAreaDropItem(String customArea, AreaFilter.AreaFilterGeometryType type){
		this.customArea = customArea;
		this.geomType = type;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return "Custom Area"; //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		return "area:" + geomType.getKey() + ":" + CustomArea.AREA_KEY + ":" + customArea;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

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
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));
		((GridLayout)comp.getLayout()).marginWidth = 0;
		((GridLayout)comp.getLayout()).marginHeight = 0;
		
		Label lbl = new Label(comp, SWT.NONE);
		lbl.setText( formatStringForLabel(Messages.CustomAreaDropItem_CustomAreaLabel) ); 
		initDrag(lbl);
		
		Link edit = new Link(comp, SWT.NONE);
		edit.setText("<a>" + Messages.CustomAreaDropItem_EditLink + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		edit.addListener(SWT.Selection, e->{
			Polygon p = null;
			try {
				p = (Polygon) (new WKBReader()).read( WKBReader.hexToBytes(customArea) );
			} catch (ParseException ex) {
				SmartPlugIn.log(ex.getMessage(), ex);
				return;
			}
			DrawAreaMapDialog dialog = new DrawAreaMapDialog(comp.getShell(), p);
			if (dialog.open() != Window.OK) return;
			
			customArea = WKBWriter.toHex( (new WKBWriter()).write(dialog.getPolygon() ));
			queryChanged();
		});
	}
}
