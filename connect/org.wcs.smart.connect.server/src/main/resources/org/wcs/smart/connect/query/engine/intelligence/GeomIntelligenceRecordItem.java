package org.wcs.smart.connect.query.engine.intelligence;

import org.wcs.smart.intelligence.query.model.IntelligenceRecordResultItem;

public class GeomIntelligenceRecordItem extends IntelligenceRecordResultItem {

	private byte[] geom;
	
	public void setGeometry(byte[] geom){
		this.geom = geom;
	}
	
	public byte[] getGeometry(){
		return this.geom;
	}
}
