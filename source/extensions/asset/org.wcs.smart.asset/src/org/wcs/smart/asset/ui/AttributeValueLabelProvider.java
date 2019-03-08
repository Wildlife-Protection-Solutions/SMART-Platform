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
package org.wcs.smart.asset.ui;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttribute.AttributeType;
import org.wcs.smart.asset.model.AssetAttributeValue;
import org.wcs.smart.asset.model.AssetDeploymentAttributeValue;
import org.wcs.smart.asset.model.AssetStationAttributeValue;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;

/**
 * Label provider for attribute values.  Supports AssetAttributeValue,
 * AssetDeploymentAttributeValue, AssetStationAttributeValue
 * 
 * @author Emily
 *
 */
public class AttributeValueLabelProvider extends LabelProvider {
	
	private CoordinateReferenceSystem crs = null;

	private synchronized CoordinateReferenceSystem getCrs(){
		if (crs != null) return crs;
		
		//this opens a session so do in job
		Job j = new Job("load projection"){ //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				CoordinateReferenceSystem  temp = GeometryUtils.SMART_CRS;
				Projection currentProjection = HibernateManager.getCurrentViewProjection();
				if (currentProjection != null ){
					try{
						CoordinateReferenceSystem parsed = ReprojectUtils.stringToCrs(currentProjection.getDefinition());
						if (!CRS.equalsIgnoreMetadata(temp, parsed)){
							temp = parsed;
						}
					}catch (Exception ex){
						AssetPlugIn.log(ex.getMessage(), ex);
					}
				}
				AttributeValueLabelProvider.this.crs = temp;
				return Status.OK_STATUS;
			}
		};
		j.setSystem(true);
		j.schedule();
		try {
			j.join();
		} catch (InterruptedException e) {
			AssetPlugIn.log(e.getMessage(), e);
		}
		return crs;
	}
	
	public AttributeValueLabelProvider(){
		super();
		getCrs();
	}
	public String getText(Object element){
		Object value = null;
		AssetAttribute attribute = null;
		if (element instanceof AssetAttributeValue){
			value = ((AssetAttributeValue) element).getAttributeValue();
			attribute = ((AssetAttributeValue) element).getAttribute();
		}else if (element instanceof AssetDeploymentAttributeValue){
			value = ((AssetDeploymentAttributeValue) element).getAttributeValue();
			attribute = ((AssetDeploymentAttributeValue) element).getAttribute();
		}else if (element instanceof AssetStationAttributeValue){
			value = ((AssetStationAttributeValue) element).getAttributeValue();
			attribute = ((AssetStationAttributeValue) element).getAttribute();
		}else{
			return super.getText(element);
		}
		if (value == null){
			return ""; //$NON-NLS-1$
		}
		
		if (value instanceof String){
			return (String) value;
		}else if (value instanceof Number){
			if (attribute.getType() == AttributeType.BOOLEAN){
				if (((Number)value).doubleValue() >= 0.5){
					return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
				}else{
					return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
				}
			}
			return ((Number)value).toString();
		}else if (value instanceof Date){
			return DateFormat.getDateInstance().format((Date)value);
		}else if (value instanceof NamedItem){
			return ((NamedItem) value).getName();
		}else if (value instanceof Point){
			CoordinateReferenceSystem crs = getCrs();
			if(crs == GeometryUtils.SMART_CRS){
				return ((Point)value).toString(); 
			}else{
				try{
					Coordinate c = ReprojectUtils.reproject(((Point) value).getX(), ((Point) value).getY(), GeometryUtils.SMART_CRS, getCrs());
					return "POINT (" + c.x + " " + c.y + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}catch (Exception ex){
					return "Error: " + ex.getMessage(); //$NON-NLS-1$
				}
			}
		}else{
			return value.toString();
		}
	}
	
	
	public Image getImage(Object element){
		return super.getImage(element);
	}
}
