package org.wcs.smart.observation.udig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.data.DataUtilities;
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
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.UuidUtils;


public class ObservationAttributeFeatureFactory {
	
	public static final String UUID = "uuid";
		
	public static SimpleFeatureType createObservationPolygonSchema(String typeName) throws SchemaException{
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:MultiPolygon:srid=4326,");
		sb.append("uuid:String,");
		sb.append("obs_uuid:String,");
		sb.append("wp_uuid:String,");
		sb.append("attribute:String,");
		sb.append("attribute_key:String,");
		sb.append("perimeter_km:Double,");
		sb.append("area_km2:Double,");
		sb.append("source:String,");
		sb.append("wp_id:String,");
		sb.append("wp_date:java.time.LocalDate,");
		sb.append("wp_time:java.time.LocalTime,");
		sb.append("category:String,");
		sb.append("category_hkey:String");
		SimpleFeatureType type = DataUtilities.createType(typeName, sb.toString());
		return type;
	}
	
	public static SimpleFeatureType createObservationLineStringSchema(String typeName) throws SchemaException{
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:MultiLineString:srid=4326,");
		sb.append("uuid:String,");
		sb.append("obs_uuid:String,");
		sb.append("wp_uuid:String,");
		sb.append("attribute:String,");
		sb.append("attribute_key:String,");
		sb.append("perimeter_km:Double,");
		sb.append("source:String,");
		sb.append("wp_id:String,");
		sb.append("wp_date:java.time.LocalDate,");
		sb.append("wp_time:java.time.LocalTime,");
		sb.append("category:String,");
		sb.append("category_hkey:String");
		SimpleFeatureType type = DataUtilities.createType(typeName, sb.toString());
		return type;
	}
	
	public static SimpleFeature getObservationAttributeAsGeometry(
			SimpleFeatureType ftype, boolean hasArea, WaypointObservationAttribute value) {

		int size = hasArea ? 14 : 13;
		
		Object data[] = new Object[size];
		int i = 0;
		data[i++] = value.getGeometry().getGeometry();
		data[i++] = value.getUuid().toString();
		data[i++] = value.getObservation().getUuid().toString();
		data[i++] = value.getObservation().getWaypoint().getUuid().toString();
		data[i++] = value.getAttribute().getName();
		data[i++] = value.getAttribute().getKeyId();
		data[i++] = value.getGeometry().getPerimeter();
		if (hasArea)  data[i++] = value.getGeometry().getArea();
		data[i++] = value.getGeometry().getSource().name();
		data[i++] = value.getObservation().getWaypoint().getId();
		data[i++] = value.getObservation().getWaypoint().getDateTime().toLocalDate();
		data[i++] = value.getObservation().getWaypoint().getDateTime().toLocalTime();
		data[i++] = value.getObservation().getCategory().getName();
		data[i++] = value.getObservation().getCategory().getHkey();
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
	}
	
	
	public static Object[] findWaypointObservationAttributes(Envelope bounds, IGeoResource[] search) throws IOException{
		
		List<WaypointObservationAttribute> matched = new ArrayList<>();		
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
						WaypointObservationAttribute a = new WaypointObservationAttribute();
						a.setUuid(UuidUtils.stringToUuid(u));
						matched.add(a);
						
						if (c[0] == null) c[0] = ((Geometry)feature.getDefaultGeometryProperty().getValue()).getCentroid().getCoordinate();
					}
			}}, null);
		}
		
		return new Object[] {matched, c[0]};
	}
}
