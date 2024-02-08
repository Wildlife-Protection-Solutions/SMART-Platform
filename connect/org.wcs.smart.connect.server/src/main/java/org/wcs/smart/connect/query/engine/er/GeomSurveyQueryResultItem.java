package org.wcs.smart.connect.query.engine.er;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;

public class GeomSurveyQueryResultItem extends SurveyQueryResultItem{

	private static final WKBReader reader = new WKBReader();
	
	private byte[] geom;
	
	public void setGeometry(byte[] geom){
		this.geom = geom;
	}
	
	public byte[] getGeometry(){
		return this.geom;
	}
	
	@Override
	public Geometry asGeometry() {
		
		if (getGeometry() == null) return null;
		try{
			return reader.read(getGeometry());
		}catch (Exception ex){
			Logger.getLogger(GeomSurveyQueryResultItem.class.getName()).log(Level.WARNING, "Error parsing geometry.", ex); //$NON-NLS-1$
		}
		return null;
	}
	
}
