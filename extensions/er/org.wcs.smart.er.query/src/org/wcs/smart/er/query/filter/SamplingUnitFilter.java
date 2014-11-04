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
 * <p>
 * "s:samplingunit:(obs | trk):(<uuid> | none)
 * </p>
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
		String[] bits = key.split(":"); //$NON-NLS-1$
		return new SamplingUnitFilter(bits[3], Type.SAMPLINGUNIT, keyToSource(bits[2]));
	}
	
	public static SamplingUnitFilter createMissionTrackFilter(String key){
		String[] bits = key.split(":"); //$NON-NLS-1$
		return new SamplingUnitFilter(bits[3], Type.TRACK, keyToSource(bits[2]));
	}
	
	private String uuid;
	private Type unitType;
	
	public enum Type {SAMPLINGUNIT, TRACK};
	
	public enum Source {
		OBSERVATION ("obs", Messages.SamplingUnitFilter_ObservationSuFilterLabel),  //$NON-NLS-1$
		TRACK ("trk", Messages.SamplingUnitFilter_TrackSuFilterLabel); //$NON-NLS-1$
		
		public String queryKey;
		public String guiName;
		
		Source(String queryKey, String guiName){
			this.queryKey = queryKey;
			this.guiName = guiName;
		}
	};
	
	private Source joinTable;
	
	public static Source keyToSource(String key){
		for (Source s : Source.values()){
			if (s.queryKey.equals(key)){
				return s;
			}
		}
		return null;
	}
	
	
	public SamplingUnitFilter(String uuid, Type type, Source source){
		this.unitType = type;
		this.uuid = uuid;
		this.joinTable = source;
	}
	
	public Source getSource(){
		return this.joinTable;
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
	
	public void setUuid(String uuid){
		this.uuid = uuid;
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
				return new DropItem[]{SurveyDropItemFactory.INSTANCE.createSamplingUnitDropItem(NONE, joinTable)};
			}
			
			SamplingUnit su = (SamplingUnit) session.get(SamplingUnit.class, SmartUtils.decodeHex(uuid));
			if (su != null){
				su.getId();
				return new DropItem[]{SurveyDropItemFactory.INSTANCE.createSamplingUnitDropItem(su, joinTable)};
			}else{
				return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.SamplingUnitFilter_SamplingUnitNotFound, new Object[]{uuid}))}; 
			}
		}
		if (unitType == Type.TRACK){
			MissionTrack su = (MissionTrack) session.get(MissionTrack.class, SmartUtils.decodeHex(uuid));
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
