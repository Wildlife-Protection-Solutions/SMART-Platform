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
package org.wcs.smart.er.ui.samplingunit.load;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.data.FeatureReader;
import org.geotools.data.shapefile.ng.ShapefileDataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnit.SamplingUnitType;
import org.wcs.smart.er.model.SamplingUnit.State;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

/**
 * Shapefile sampling unit importer.
 * 
 * @author Emily
 *
 */
public class ShpSamplingUnitImporter implements ISamplingUnitImporter{

	/**
	 * Gets the field names.  This also validates the geometry type against
	 * the sampling unit type.
	 * <br>
	 * options must include mapping from TYPE_KEY to SamplingUnitType. 
	 *
	 */
	@Override
	public String[] getFieldNames(File f, Map<String, Object> options)
			throws Exception {
		ArrayList<String> attributes = new ArrayList<String>();
		ShapefileDataStore store = new ShapefileDataStore(f.toURI().toURL());
		
		try{
			SamplingUnit.SamplingUnitType suType = (SamplingUnitType) options.get(TYPE_KEY);
			SimpleFeatureType type = store.getSchema();
			
			//validate geometry types
			boolean error = false;
			if (suType == SamplingUnitType.PLOT){
				//only point geometry types supported
				if (!Point.class.isAssignableFrom(type.getGeometryDescriptor().getType().getBinding())){
					error = true;
				}
			}else if (suType == SamplingUnitType.TRANSECT ){
				if (!LineString.class.isAssignableFrom(type.getGeometryDescriptor().getType().getBinding()) && 
					!MultiLineString.class.isAssignableFrom(type.getGeometryDescriptor().getType().getBinding())){
					error = true;
				}
			}
			if (error){
				throw new Exception(
						MessageFormat.format(Messages.ShpSamplingUnitImporter_InvalidGeometryType,
								new Object[]{type.getGeometryDescriptor().getType().getBinding().getName(), suType.getGuiName()}));
			}
			
			
			for (AttributeDescriptor d : type.getAttributeDescriptors()){
				if (!Geometry.class.isAssignableFrom(d.getType().getBinding())){
					attributes.add(d.getLocalName());	
				}			
			}
		}finally{
			store.dispose();
		}
		return attributes.toArray(new String[attributes.size()]);
	}

	
	
	@Override
	public List<SamplingUnit> importFile(File f, HashMap<Object, Object> options, IProgressMonitor monitor)
			throws Exception {

		monitor.beginTask(MessageFormat.format(Messages.ShpSamplingUnitImporter_Progress1, new Object[]{f.getAbsolutePath()}), 2);
		List<SamplingUnit> units = new ArrayList<SamplingUnit>();
		ShapefileDataStore store = new ShapefileDataStore(f.toURI().toURL());
		try {
			SamplingUnit.SamplingUnitType type = (SamplingUnitType) options.get(TYPE_KEY);
			if (type == null) {
				throw new Exception(Messages.ShpSamplingUnitImporter_SamplingUnitTypeError);
			}

			Double bufferValue = (Double) options.get(BUFFER_KEY);

			List<SamplingUnitAttribute> attributes = new ArrayList<SamplingUnitAttribute>();
			for (Object key : options.keySet()) {
				if (key instanceof SamplingUnitAttribute) {
					attributes.add((SamplingUnitAttribute) key);
				}
			}

			for (SamplingUnitAttribute att : attributes) {
				String field = (String) options.get(att);
				if (field == null)
					continue;

				// validate attribute types
				AttributeDescriptor shapeAtt = store.getSchema().getDescriptor(field);
				boolean error = false;
				if (att.getType() == AttributeType.TEXT) {
					if (!shapeAtt.getType().getBinding().equals(String.class)) {
						error = true;
					}
				} else if (att.getType() == AttributeType.NUMERIC) {
					if (!Number.class.isAssignableFrom(shapeAtt.getType()
							.getBinding())) {
						error = true;
					}
				}

				if (error) {
					throw new Exception(
							MessageFormat
									.format(Messages.ShpSamplingUnitImporter_AttributeMappingError,
											new Object[] {shapeAtt.getLocalName(),att.getName(),shapeAtt.getType().getBinding().getName(),att.getType() }));
				}
			}
			monitor.worked(1);

			String idField = (String) options.get(ID_FIELD_KEY);
			FeatureReader<SimpleFeatureType, SimpleFeature> reader = store
					.getFeatureReader();
			int cnt = 0;
			try {
				while (reader.hasNext()) {
					SimpleFeature sf = reader.next();
					String id = null;
					cnt++;
					if (idField != null) {
						id = sf.getAttribute(idField).toString();
					} else {
						id = AUTO_GENERATE_KEY_PREFIX + " " + cnt; //$NON-NLS-1$
					}

					SamplingUnit su = new SamplingUnit();
					su.setAttributes(new ArrayList<SamplingUnitAttributeValue>());
					su.setBuffer(bufferValue);

					Geometry g = (Geometry) sf.getDefaultGeometry();
					if (type == SamplingUnitType.PLOT) {
						if (!(g instanceof Point)) {
							throw new Exception(
									MessageFormat
											.format(Messages.ShpSamplingUnitImporter_GeomTypeNotSupported,
													new Object[] { g.getClass().getName() }));
						}
					} else if (type == SamplingUnitType.TRANSECT) {
						if (g instanceof MultiLineString) {
							if (((MultiLineString) g).getNumGeometries() > 1) {
								throw new Exception(
										MessageFormat
												.format(Messages.ShpSamplingUnitImporter_GeomTypeNotSupported2,
														new Object[] { g.getClass().getName() }));
							} else {
								g = g.getGeometryN(0);
							}
						}
						if (!(g instanceof LineString)) {
							throw new Exception(
									MessageFormat
											.format(Messages.ShpSamplingUnitImporter_GeomTypeNotSupported3,
													new Object[] { g.getClass().getName() }));
						}
					}
					su.setGeometry(g);
					su.setId(id);
					su.setState(State.ACTIVE);
					su.setType(type);

					for (SamplingUnitAttribute att : attributes) {
						String field = (String) options.get(att);
						if (field == null)
							continue;

						SamplingUnitAttributeValue sv = new SamplingUnitAttributeValue();
						sv.setSamplingUnitAttribute(att);
						sv.setSamplingUnit(su);

						boolean add = false;
						if (att.getType() == AttributeType.NUMERIC) {

							Number d = (Number) sf.getAttribute(field);
							if (d != null) {
								add = true;
							}
							sv.setNumberValue(d.doubleValue());
						} else if (att.getType() == AttributeType.TEXT) {
							String s = sf.getAttribute(field).toString();
							if (s != null) {
								add = true;
							}
							sv.setStringValue(s);
						} else if (att.getType() == AttributeType.LIST){
							SamplingUnitAttributeListItem listValue = ImportAttributes.findMatch(att, (String)sf.getAttribute(field));
							if (listValue != null){
								add = true;
								sv.setAttributeListItem(listValue);
							}
						}
						if (add) {
							su.getAttributes().add(sv);
						}
					}
					units.add(su);
				}
				monitor.worked(1);
			} finally {
				reader.close();
			}
		} finally {
			store.dispose();
			monitor.done();
		}
		return units;
	}

}
