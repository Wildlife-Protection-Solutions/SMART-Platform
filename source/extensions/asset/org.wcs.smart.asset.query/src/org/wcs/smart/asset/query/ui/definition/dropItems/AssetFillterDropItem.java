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
package org.wcs.smart.asset.query.ui.definition.dropItems;

import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.query.ui.model.IFilterDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Asset filter drop item 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AssetFillterDropItem extends DropItem implements IFilterDropItem{

	private Label lbl;
	
	private String text;
	private AssetFilterOption filter;
	private UUID itemUuid;
	
	public AssetFillterDropItem(Asset asset) {
			this(AssetFilterOption.ASSET, asset.getUuid(), asset.getId());
	}
	
	public AssetFillterDropItem(AssetStation station) {
		this(AssetFilterOption.STATION, station.getUuid(), station.getId());
	}
	
	public AssetFillterDropItem(AssetStationLocation location) {
		this(AssetFilterOption.STATIONLOCATION, location.getUuid(), location.getId());
	}
	
	public AssetFillterDropItem(AssetType type) {
		this(AssetFilterOption.ASSETTYPE, type.getUuid(), type.getName());
	}
	/**
	 * Creates new drop item
	 * @param filter asset filter
	 */
	protected AssetFillterDropItem(AssetFilterOption filter, UUID itemUuid, String textLabel) {
		this.text = textLabel;
		this.itemUuid = itemUuid;
		this.filter = filter;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return this.text;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		return "asset:" + filter.getKey() + " " + Operator.EQUALS.asSmartValue() + " \"" + UuidUtils.uuidToString(itemUuid) + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		lbl = new Label(parent, SWT.NONE);
		lbl.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));		
		initDrag(lbl);
		
		this.lbl.setText( formatStringForLabel(this.text)) ;
	}

	@Override
	public void initializeData(Object data) {
		
	}

}
