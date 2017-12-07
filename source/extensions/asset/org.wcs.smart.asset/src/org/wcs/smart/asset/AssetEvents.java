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

/**
 * Asset module events
 * 
 * @author Emily
 *
 */
public class AssetEvents {

	/**
	 * Payload for asset events is a collection of assets
	 */
	public static final String ASSET_ALL = "ASSET/*"; //$NON-NLS-1$
	public static final String ASSET_NEW = "ASSET/NEW"; //$NON-NLS-1$
	public static final String ASSET_DELETE = "ASSET/DELETE"; //$NON-NLS-1$
	public static final String ASSET_MODIFIED = "ASSET/UPDATED"; //$NON-NLS-1$
	

	/**
	 * Payload for asset type events are a collection of asset types
	 */
	public static final String ASSETTYPE_ALL = "ASSETTYPE/*"; //$NON-NLS-1$
	public static final String ASSETTYPE_NEW = "ASSETTYPE/NEW"; //$NON-NLS-1$
	public static final String ASSETTYPE_DELETE = "ASSETTYPE/DELETE"; //$NON-NLS-1$
	public static final String ASSETTYPE_MODIFIED = "ASSETTYPE/UPDATED"; //$NON-NLS-1$
	
	/**
	 * Payload for asset stations are a collection of asset stations
	 */
	public static final String ASSETSTATION_ALL = "ASSETSTATION/*"; //$NON-NLS-1$
	public static final String ASSETSTATION_NEW = "ASSETSTATION/NEW"; //$NON-NLS-1$
	public static final String ASSETSTATION_DELETE = "ASSETSTATION/DELETE"; //$NON-NLS-1$
	public static final String ASSETSTATION_MODIFIED = "ASSETSTATION/UPDATED"; //$NON-NLS-1$
	
	/**
	 * Payload for asset stations location are collection of asset station location
	 */
	public static final String ASSETSTATIONLOCATION_ALL = "ASSETSTATIONLOCATION/*"; //$NON-NLS-1$
	public static final String ASSETSTATIONLOCATION_NEW = "ASSETSTATIONLOCATION/NEW"; //$NON-NLS-1$
	public static final String ASSETSTATIONLOCATION_DELETE = "ASSETSTATIONLOCATION/DELETE"; //$NON-NLS-1$
	public static final String ASSETSTATIONLOCATION_MODIFIED = "ASSETSTATIONLOCATION/UPDATED"; //$NON-NLS-1$
	
	/**
	 * Payload for asset deployment events is collection of asset deployments
	 */
	public static final String ASSETDEPLOYMENT_ALL = "ASSETDEPLOYMENT/*"; //$NON-NLS-1$
	public static final String ASSETDEPLOYMENT_NEW = "ASSETDEPLOYMENT/NEW"; //$NON-NLS-1$
	public static final String ASSETDEPLOYMENT_DELETE = "ASSETDEPLOYMENT/DELETE"; //$NON-NLS-1$
	public static final String ASSETDEPLOYMENT_MODIFIED = "ASSETDEPLOYMENT/UPDATED"; //$NON-NLS-1$
	
	/**
	 * when asset data has modified (waypoints, links etc) payload is null
	 */
	public static final String ASSETDATA = "ASSETDATA"; //$NON-NLS-1$
}
