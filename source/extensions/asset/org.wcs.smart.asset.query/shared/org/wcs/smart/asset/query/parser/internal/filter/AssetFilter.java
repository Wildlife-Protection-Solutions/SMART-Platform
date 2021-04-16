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
package org.wcs.smart.asset.query.parser.internal.filter;

import java.util.UUID;

import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.asset.query.model.AssetQueryOptions;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.IFilterVisitor;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * A asset filter.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AssetFilter implements IFilter {

	
	/**
	 * Creates a asset filter of the form:  <assetfield> = \"<UUID>\"
	 * @param key asset filter key
	 * @param op asset filter operator 
	 * @param value asset filter value
	 * @return
	 */
	public static AssetFilter createStringFilter(String assetFieldKey, Operator op, String value){
		if (!op.equals(Operator.EQUALS)) throw new RuntimeException("Operator not supported for asset filter (only equals is supported)."); //$NON-NLS-1$
		return new AssetFilter(assetFieldKey, op, value);
	}

	private Operator op= null;
	private UUID value = null;
	private AssetFilterOption option ;
	
	/**
	 * Creates a new asset filter
	 * @param assetFieldKey asset key
	 * @param op operator
	 * @param value filter value
	 */
	public AssetFilter (String assetFieldKey, Operator op, String value){
		this.op = op;
		try {
			this.value = UuidUtils.stringToUuid( SharedUtils.stripQuotes(value) );
		}catch (Throwable t) {
			throw new RuntimeException("Not a valid UUID: " + value ); //$NON-NLS-1$
		}
		
		String assetItem = assetFieldKey.split(":")[1]; //$NON-NLS-1$
        option = AssetQueryOptions.findAssetQueryOption(assetItem);
	}
	
	/**
	 * 
	 * @return the asset option represented by this filter
	 */
	public AssetFilterOption getAssetOption(){
		return option;
	}
	/**
	 * @see org.wcs.smart.asset.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString(){
		return "asset:" + option.getKey() + " " + op.asSmartValue() + " \"" + UuidUtils.uuidToString(value) + "\"";  //$NON-NLS-1$  //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$	
	}
	
	/**
	 * 
	 * @return the filter uuid value
	 */
	public UUID getValue(){
		return  value;
	}
	
	/**
	 * sets the filter uuid value
	 * @param uuid
	 */
	public void setValue(UUID uuid) {
		this.value = uuid;
	}
	
	public Operator getOperator(){
		return this.op;
	}
	
	
	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);		
	}
	
}
