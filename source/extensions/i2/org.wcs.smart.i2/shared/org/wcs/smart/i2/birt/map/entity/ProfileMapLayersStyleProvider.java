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
package org.wcs.smart.i2.birt.map.entity;

import org.hibernate.Session;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.i2.StyleUtil;
import org.wcs.smart.i2.birt.entity.EntityLocationAttributeDataset;
import org.wcs.smart.i2.birt.record.location.RecordLocationObservationDetailsDataset;
import org.wcs.smart.report.birt.map.IBirtLayerStyleProvider;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.MapLayerInfo.LayerType;
import org.wcs.smart.udig.style.StyleManager;

/**
 * Map layer for entity location attributes
 * 
 * @author Emily
 *
 */
public class ProfileMapLayersStyleProvider implements IBirtLayerStyleProvider {

	public ProfileMapLayersStyleProvider() {
	}


	@Override
	public StyleBlackboard getStyle(String extensionId, String queryText, MapLayerInfo info, 
			ConservationArea ca, Session s) {
		if (extensionId.equals(EntityLocationAttributeDataset.DATASET_TYPE)){
			//return red star style
			StyleBlackboard sb = ProjectFactory.eINSTANCE.createStyleBlackboard();
			sb.put("org.locationtech.udig.style.sld", StyleUtil.INSTANCE.buildRedStarStyle()); //$NON-NLS-1$
			return sb;
		}
		
		if (extensionId.equals(RecordLocationObservationDetailsDataset.DATASET_TYPE)){
			//return red star style
			//style based on attribute
			
			//get all geometry attributes
			Attribute.AttributeType type = null;
			if (info.getLayerType() == LayerType.MULTILINE || info.getLayerType() == LayerType.LINE) {
				type = Attribute.AttributeType.LINE;
			}
			if (info.getLayerType() == LayerType.MULTIPOLYGON || info.getLayerType() == LayerType.POLYGON) {
				type = Attribute.AttributeType.POLYGON;
			}
			if (type == null) return null;

			//TODO: locale here
			return StyleManager.INSTANCE.buildThemedGeometryAttributeStyle(
					"Attribute Key", 
					type, s, ca, true);					
		}
		return null;
	}

}
