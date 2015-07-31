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
package org.wcs.smart.entity.query;

import java.util.UUID;

import org.eclipse.core.runtime.IAdapterFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.map.EntityQueryDataSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;
/**
 * Adapter factory for adapting an EntityFeature or a SightingResultItem
 * to a Waypoint object.
 * <p>This is mainly for use in the WaypointInfo view</p>
 * 
 * @author Emily
 *
 */
public class SightingResultItemAdapterFactory implements IAdapterFactory {

	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType == Waypoint.class){
			if (adaptableObject instanceof SightingResultItem){
				SightingResultItem it = (SightingResultItem)adaptableObject;
				Waypoint wp = new Waypoint();
				wp.setUuid(it.getWaypointUuid());
				return wp;
			}else if (adaptableObject instanceof SimpleFeature){
				SimpleFeature sf = (SimpleFeature)adaptableObject;
				if (sf.getFeatureType().getTypeName().equals(EntityQueryDataSource.TYPENAME)){
					String key = sf.getID();
					String uuid = key.substring(key.lastIndexOf('.')+1);
					UUID wpuuid;
					try {
						wpuuid = UuidUtils.stringToUuid(uuid);
						Waypoint wp = new Waypoint();
						wp.setUuid(wpuuid);
						return wp;	
					} catch (Exception e) {
						EntityPlugIn.log("Cannot adapt entity sighting feature to waypoint.", e); //$NON-NLS-1$
					}
					
					
				}
			}
		}
		return null;
	}

	@Override
	public Class[] getAdapterList() {
		return new Class[]{Waypoint.class};
	}

}
