package org.wcs.smart.connect.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.connect.query.columns.QueryColumnUtils;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public abstract class AbstractDbFeatureResultSet implements IDbTableResultSet {
	
	public static final String POINT_GEOM_TYPE = "Point";
	public static final String MULTI_POINT_GEOM_TYPE = "MultiPoint";
	
	public static final String LINESTRING_GEOM_TYPE = "Linestring";
	
	protected GeometryFactory gf = new GeometryFactory();
	
//	public abstract SimpleFeature toFeature(ResultSet rs, List<QueryColumn> columns, Connection c, SimpleFeatureType ftype) throws Exception;
	
	
	public SimpleFeature toFeature(ResultSet rs, List<QueryColumn> columns, Connection c, SimpleFeatureType ftype)  throws Exception {
		List<Object> data = new ArrayList<Object>();
		data.add(createGeometry(rs));
		data.add(createId(rs));  
		
		int i = 0;
		for (QueryColumn qc : columns){
			if (qc.isVisible()){
				Object x = getValue(rs, qc, c);
				if (x instanceof Boolean){
					if ((Boolean)x){
						x = 0;
					}else{
						x = 1;
					}
				}
				if (qc.getType() == QueryColumn.ColumnType.TIME && 
						ftype.getDescriptor(i++) .getType().getBinding().equals(String.class)
						){
					x = DateFormat.getTimeInstance().format((Date)x);
				}
			}
		}
		return SimpleFeatureBuilder.build(ftype, data, (String)data.get(1));
	}
	

	public abstract Geometry createGeometry(ResultSet rs) throws Exception;
	
	public abstract String createId(ResultSet rs) throws Exception; 
	
	public abstract String getGeometryType();
	
	public String getFeatureSchemaDef(List<QueryColumn> columns, boolean supportsTime){
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:" + getGeometryType() + ":srid=4326,fid:String"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(QueryColumnUtils.createFeatureDefinitionString(columns, supportsTime));
		return sb.toString();
	}
}
