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
package org.wcs.smart.asset.model;

import java.io.File;
import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.asset.IAssetLabelProvider;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.UuidUtils;

/**
 * Asset waypoint source.
 * 
 * @author Emily
 *
 */
public class AssetWaypointSource implements IWaypointSource{

	public static final String KEY = "ASSET"; //$NON-NLS-1$
	
	public static final String FILESTORE_LOC = "assets"; //$NON-NLS-1$
	
	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getName(Locale l) {
		return SmartContext.INSTANCE.getClass(IAssetLabelProvider.class).getLabel(this, l);
	}

	@Override
	public String getDatastoreFileLocation(Object object, Session session) throws Exception {
		if (object instanceof Waypoint){
			StringBuilder sb = new StringBuilder();
			sb.append(FILESTORE_LOC);
			sb.append(File.separator);
			sb.append(UuidUtils.uuidToString(((Waypoint)object).getUuid()));
			sb.append(File.separator);
			return sb.toString();
		}else{
			throw new Exception("Object type " + object.getClass().getName() + " not supported for asset waypoint source types."); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}


}
