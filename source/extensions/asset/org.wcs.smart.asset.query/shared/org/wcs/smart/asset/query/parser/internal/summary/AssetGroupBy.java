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

import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.asset.query.model.AssetQueryOptionType;
import org.wcs.smart.asset.query.model.AssetQueryOptions;
import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.util.SharedUtils;

/**
 * A asset group by option
 * @author egouge
 * @since 1.0.0
 */
public class AssetGroupBy implements IGroupBy {

	/**
	 * Creates a new asset group by option
	 * @param key asset group by key of the form
	 * "asset:<key>:<uniqueid>,<uniqueid>,<uniqueid>"
	 * 
	 * Where <key> is one of the AssetQueryOption and uniqueid
	 * is a hex encoded uuid or unique string value.
	 * 
	 * @return
	 */
	public static final AssetGroupBy createGroupBy(String key){
		return new AssetGroupBy(key);
	}

	private String[] items;
	private AssetFilterOption option;
	/**
	 * Creates a new asset group by option
	 * @param key asset group by item key of the format "asset:key:<uuid>:<uuid>..."
	 */
	public AssetGroupBy(String key){
		String[] bits = key.split(":"); //$NON-NLS-1$
		option = AssetQueryOptions.findAssetQueryOption(bits[1]);
		if (bits.length > 2){
			items = new String[bits.length - 2];
			for (int i = 2; i < bits.length; i ++){
				if (option.getType() == AssetQueryOptionType.UUID){
					items[i-2] = bits[i];
				}else{
					items[i-2] = SharedUtils.stripQuotes(bits[i]);
				}
			}
		}else{
			items = null;
		}
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getKeyPart()
	 */
	public String getKeyPart(){
		StringBuilder sb = new StringBuilder();
		sb.append("asset:"); //$NON-NLS-1$
		sb.append(option.getKey());
		return sb.toString();
	}
	
	public AssetFilterOption getOption(){
		return this.option;
	}
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asString()
	 */
	@Override
	public String asString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getKeyPart());
		sb.append(":"); //$NON-NLS-1$
		if (items != null) {
			for (int i = 0; i < items.length; i++) {
				if (option.getType() == AssetQueryOptionType.STRING){
					sb.append("\""); //$NON-NLS-1$
					sb.append(items[i]);
					sb.append("\""); //$NON-NLS-1$
				}else{
					sb.append(items[i]);
				}
				if (i < items.length - 1) {
					sb.append(":"); //$NON-NLS-1$
				}
			}
		}
		return sb.toString();
	}
		
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getType()
	 */
	public GroupByType getType(){
		if (option.getType() == AssetQueryOptionType.UUID){
			return GroupByType.BYTE;
		}else if (option.getType() == AssetQueryOptionType.STRING){
			return GroupByType.STRING;
		}else if (option.getType() == AssetQueryOptionType.KEY){
			return GroupByType.KEY;
		}
		return GroupByType.STRING;
	}
	
	
	/**
	 * 
	 * @return the asset group by items
	 */
	public String[] getItems(){
		return this.items;
	}

	@Override
	public void visit(IGroupByVisitor visitor) {
		visitor.visit(this);
	}
}
