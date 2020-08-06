/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset;

import java.util.Locale;

import org.wcs.smart.ISharedLabelProvider;

/**
 * Asset label provider for labels shared with Connect 
 * 
 * @author Emily
 *
 */
public interface IAssetLabelProvider extends ISharedLabelProvider {
	
	public static final String ASSET_TABLE_NAME = "asset_table_name"; //$NON-NLS-1$
	public static final String STATION_TABLE_NAME = "asset_station_name"; //$NON-NLS-1$
	public static final String STATIONLOCATION_TABLE_NAME = "asset_stationlocation_name"; //$NON-NLS-1$
	
	public static final String ID_COL_NAME = "asset_id_col_name"; //$NON-NLS-1$
	public static final String ASSET_TYPE_COL_NAME = "asset_type_col_name"; //$NON-NLS-1$
	public static final String ASSET_TYPEKEY_COL_NAME = "asset_type_key_name"; //$NON-NLS-1$
	public static final String STATUS_COL_NAME = "asset_status_col_name"; //$NON-NLS-1$
	public static final String STATUSKEY_COL_NAME = "asset_status_key_col_name"; //$NON-NLS-1$
	public static final String POSITION_COL_NAME = "asset_position"; //$NON-NLS-1$

	/**
	 * Formats the time in seconds to days, hours (ex. 34 days, 34.56 hours)
	 * @param timeInSeconds
	 * @param l
	 * @return
	 */
	public String formatTime(double timeInSeconds, Locale l);
}
