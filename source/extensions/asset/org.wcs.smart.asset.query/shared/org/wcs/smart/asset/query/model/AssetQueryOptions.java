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
package org.wcs.smart.asset.query.model;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that defines asset based options for
 * queries.  This includes both values for summary queries
 * and filters for summary and observation queries.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class AssetQueryOptions {
//
//	/**
//	 * Asset group by options for summary queries.
//	 */
//	public final static AssetFilterOption[] ASSET_GROUBY_OPTIONS = {
//			AssetFilterOption.ASSET, 
//			AssetFilterOption.STATION, 
//			AssetFilterOption.ASSETTYPE, 
//			AssetFilterOption.STATIONLOCATION, 
//	};
	
//	
//	/**
//	 * Asset filter options for summary and observation queries
//	 */
//	public final static AssetFilterOption[] ASSET_FILTER_OPTIONS = {
//			AssetFilterOption.ASSET, 
//			AssetFilterOption.STATION, 
//			AssetFilterOption.ASSETTYPE, 
//			AssetFilterOption.STATIONLOCATION, 
//	};
//	
//	
//
//	public static IDateGroupBy[] DATE_GROUBY_OPS = new IDateGroupBy[]{
//		DayDateGroupBy.INSTANCE,
//		MonthDateGroupBy.INSTANCE,
//		YearDateGroupBy.INSTANCE
//	};

	/**
	 * Finds a particular asset filter option based
	 * on the key.
	 *  
	 * @param key the asset filter key
	 * @return
	 */
	public static final AssetFilterOption findAssetQueryOption(String key){
		return keyToColumnMap.get(key);
	}
	
	/*
	 * Maps of asset filter keys to the asset filter option
	 */
	private static Map<String, AssetFilterOption> keyToColumnMap;
	static {
		keyToColumnMap = new HashMap<String, AssetFilterOption>();
		for (int i = 0; i < AssetFilterOption.values().length; i++) {
			AssetFilterOption op = AssetFilterOption.values()[i];
			keyToColumnMap.put(op.getKey(), op);
		}
		keyToColumnMap = Collections.unmodifiableMap(keyToColumnMap);
	}
	
}

