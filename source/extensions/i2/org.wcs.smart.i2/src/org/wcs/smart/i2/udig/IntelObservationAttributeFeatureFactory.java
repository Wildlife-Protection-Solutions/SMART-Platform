package org.wcs.smart.i2.udig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.udig.catalog.IGeoResource;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.wcs.smart.i2.model.IntelObservationAttribute;
import org.wcs.smart.observation.udig.ObservationAttributeFeatureFactory;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.UuidUtils;


public class IntelObservationAttributeFeatureFactory {
	
		
	public static SimpleFeatureType createObservationPolygonSchema(String typeName) throws SchemaException{
		return ObservationAttributeFeatureFactory.createObservationPolygonSchema(typeName);		
	}
	
	public static SimpleFeatureType createObservationLineStringSchema(String typeName) throws SchemaException{
		return ObservationAttributeFeatureFactory.createObservationLineStringSchema(typeName);
	}
	
	public static SimpleFeature getObservationAttributeAsGeometry(
			SimpleFeatureType ftype, boolean hasArea, IntelObservationAttribute value) {

		int size = hasArea ? 14 : 13;
		Object data[] = new Object[size];
		int i = 0;
		data[i++] = value.getGeometry().getGeometry();
		data[i++] = value.getUuid() == null ? UUID.randomUUID().toString() : value.getUuid().toString();
		data[i++] = value.getObservation().getUuid() == null ? "" : value.getObservation().getUuid().toString();
		data[i++] = value.getObservation().getLocation().getUuid() == null ? "" : value.getObservation().getLocation().getUuid().toString();
		data[i++] = value.getAttribute().getName();
		data[i++] = value.getAttribute().getKeyId();
		data[i++] = value.getGeometry().getPerimeter();
		if (hasArea)  data[i++] = value.getGeometry().getArea();
		data[i++] = value.getGeometry().getSource().name();
		data[i++] = value.getObservation().getLocation().getId();
		data[i++] = value.getObservation().getLocation().getDateTime().toLocalDate();
		data[i++] = value.getObservation().getLocation().getDateTime().toLocalTime();
		data[i++] = value.getObservation().getCategory().getName();
		data[i++] = value.getObservation().getCategory().getHkey();
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
	}
	
	
	public static Object[] findWaypointObservationAttributes(Envelope bounds, IGeoResource[] search) throws IOException{
		
		List<IntelObservationAttribute> matched = new ArrayList<>();		
		Polygon envp = GeometryUtils.envelopeToGeometry(bounds);
		Coordinate[] c = {null};
		for (IGeoResource r : search) {
			
			FeatureCollection<SimpleFeatureType, SimpleFeature> fc = 
					r.resolve(FeatureSource.class, new NullProgressMonitor())
					.getFeatures(Filter.INCLUDE);
			fc.accepts(new FeatureVisitor() {

				@Override
				public void visit(Feature feature) {
					if ( ((Geometry)feature.getDefaultGeometryProperty().getValue()).getEnvelopeInternal().intersects(bounds) &&
							((Geometry)feature.getDefaultGeometryProperty().getValue()).intersects(envp)
							) {
						String u = feature.getProperty(ObservationAttributeFeatureFactory.UUID).getValue().toString();
						IntelObservationAttribute a = new IntelObservationAttribute();
						a.setUuid(UuidUtils.stringToUuid(u));
						matched.add(a);
						
						if (c[0] == null) c[0] = ((Geometry)feature.getDefaultGeometryProperty().getValue()).getCentroid().getCoordinate();
					}
			}}, null);
		}
		
		return new Object[] {matched, c[0]};
	}
}
