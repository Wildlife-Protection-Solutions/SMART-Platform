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
package org.wcs.smart.asset.query.parser.internal.summary;

import org.wcs.smart.asset.query.model.AssetValueOption;
import org.wcs.smart.query.model.filter.IValueVisitor;
import org.wcs.smart.query.model.summary.IValueItem;

/**
 * ValueItem for asset values.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class AssetValueItem implements IValueItem {


	/**
	 * Creates a asset value item from a key of the form
	 * asset:<aggregation>:assetkey
	 * 
	 * @param part
	 * @return
	 */
	public static final AssetValueItem createItem(String key){
		return new AssetValueItem(key);
	}
	

	private String key = null;
	private String aggregation = null;
	private AssetValueOption assetOp;
	
	/**
	 * Creates a asset value item from a key of the form
	 * asset:<aggregation>:assetkey
	 * 
	 * @param part
	 */
	public AssetValueItem(String key){
		this.key = key;
		String[] bits = key.split(":"); //$NON-NLS-1$
		for (AssetValueOption op : AssetValueOption.values()) {
			if (bits[2].trim().equalsIgnoreCase(op.getKeyPart())) {
				assetOp = op;
				break;
			}
		}
		this.aggregation = bits[1];		
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asString()
	 */
	public String asString(){
		return this.key;
	}
	
	/**
	 * @return the asset value option
	 */
	public AssetValueOption getAssetValueOption(){
		return assetOp;
	}
	
	public void accept(IValueVisitor visitor){
		visitor.visit(this);
	}
}
