package org.wcs.smart.connect.query.engine.er;

import org.wcs.smart.er.query.model.SurveyQueryResultItem;

public class GeomSurveyQueryResultItem extends SurveyQueryResultItem{


	private byte[] geom;
	
	public void setGeometry(byte[] geom){
		this.geom = geom;
	}
	
	public byte[] getGeometry(){
		return this.geom;
	}
}
