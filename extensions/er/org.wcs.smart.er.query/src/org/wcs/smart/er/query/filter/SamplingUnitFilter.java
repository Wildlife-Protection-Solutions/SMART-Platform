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
package org.wcs.smart.er.query.filter;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;
import org.wcs.smart.util.SmartUtils;

/**
 * Sampling unit filter.
 * 
 * @author Emily
 *
 */
public class SamplingUnitFilter implements IFilter {

	public static final String NONE_KEY = "none"; //$NON-NLS-1$
	
	public static SamplingUnit NONE = new SamplingUnit();
	static{
		NONE.setId(Messages.SamplingUnitFilter_NoneFilterName);
	}
	
	public static SamplingUnitFilter createSamplingUnitFilter(String key){
		return new SamplingUnitFilter(key.split(":")[2], Type.SAMPLINGUNIT); //$NON-NLS-1$
	}
	
	public static SamplingUnitFilter createMissionTrackFilter(String key){
		return new SamplingUnitFilter(key.split(":")[2], Type.TRACK); //$NON-NLS-1$
	}
	
	private String uuid;
	private Type unitType;
	
	public enum Type {SAMPLINGUNIT, TRACK};
	
	public SamplingUnitFilter(String uuid, Type type){
		this.unitType = type;
		this.uuid = uuid;
	}
	
	public boolean isNone(){
		return this.uuid.equals(NONE_KEY);
	}
	public Type getType(){
		return this.unitType;
	}
	
	public String getUuid(){
		return this.uuid;
	}
	@Override
	public String asString() {
		if (unitType == Type.SAMPLINGUNIT){
			return "s:samplingunit:" + uuid; //$NON-NLS-1$
		}else if (unitType == Type.TRACK){
			return "s:samplingunittrack:" + uuid; //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
		
	}

	@Override
	public DropItem[] getDropItems(Session session) throws Exception {
		if (unitType == Type.SAMPLINGUNIT){
			if (uuid.equals(NONE_KEY)){
				return new DropItem[]{SurveyDropItemFactory.INSTANCE.createSamplingUnitDropItem(NONE)};
			}
			
			SamplingUnit su = (SamplingUnit) session.load(SamplingUnit.class, SmartUtils.decodeHex(uuid));
			if (su != null){
				su.getId();
				return new DropItem[]{SurveyDropItemFactory.INSTANCE.createSamplingUnitDropItem(su)};
			}else{
				return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.SamplingUnitFilter_SamplingUnitNotFound, new Object[]{uuid}))}; 
			}
		}
		if (unitType == Type.TRACK){
			MissionTrack su = (MissionTrack) session.load(MissionTrack.class, SmartUtils.decodeHex(uuid));
			if (su != null){
				su.getId();
				return new DropItem[]{SurveyDropItemFactory.INSTANCE.createSamplingUnitDropItem(su)};
			}else{
				return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.SamplingUnitFilter_MissionTrackNotFound, new Object[]{uuid}))}; 
			}
		}
		return null;
	}

}
