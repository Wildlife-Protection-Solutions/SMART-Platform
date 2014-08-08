package org.wcs.smart.er.query.filter;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;
import org.wcs.smart.util.SmartUtils;

public class SamplingUnitFilter implements IFilter {

	public static SamplingUnitFilter createSamplingUnitFilter(String key){
		return new SamplingUnitFilter(key.split(":")[2], Type.SAMPLINGUNIT);
	}
	
	public static SamplingUnitFilter createMissionTrackFilter(String key){
		return new SamplingUnitFilter(key.split(":")[2], Type.TRACK);
	}
	
	private String uuid;
	private Type type;
	
	public enum Type {SAMPLINGUNIT, TRACK};
	
	public SamplingUnitFilter(String uuid, Type type){
		this.uuid = uuid;
		this.type = type;
	}
	
	public Type getType(){
		return this.type;
	}
	
	public String getUuid(){
		return this.uuid;
	}
	@Override
	public String asString() {
		if (type == Type.SAMPLINGUNIT){
			return "s:samplingunit:" + uuid;
		}else if (type == Type.TRACK){
			return "s:samplingunittrack:" + uuid;
		}
		return null;
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
		
	}

	@Override
	public DropItem[] getDropItems(Session session) throws Exception {
		if (type == Type.SAMPLINGUNIT){
			SamplingUnit su = (SamplingUnit) session.load(SamplingUnit.class, SmartUtils.decodeHex(uuid));
			if (su != null){
				return new DropItem[]{SurveyDropItemFactory.INSTANCE.createSamplingUnitDropItem(su)};
			}else{
				return new DropItem[]{new ErrorDropItem(MessageFormat.format("Sampling unit {0} not found.", new Object[]{uuid}))};
			}
		}
		if (type == Type.TRACK){
			MissionTrack su = (MissionTrack) session.load(MissionTrack.class, SmartUtils.decodeHex(uuid));
			if (su != null){
				return new DropItem[]{SurveyDropItemFactory.INSTANCE.createSamplingUnitDropItem(su)};
			}else{
				return new DropItem[]{new ErrorDropItem(MessageFormat.format("Sampling unit (mission track) {0} not found.", new Object[]{uuid}))};
			}
		}
		return null;
	}

}
