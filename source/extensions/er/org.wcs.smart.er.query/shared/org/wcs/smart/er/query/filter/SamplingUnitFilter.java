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

import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.er.query.ISurveyQueryLabelProvider;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;

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
	
	public static Source keyToSource(String key){
		for (Source s : Source.values()){
			if (s.queryKey.equals(key)){
				return s;
			}
		}
		return null;
	}
	
	public static SamplingUnitFilter createSamplingUnitFilter(String key){
		String[] bits = key.split(":"); //$NON-NLS-1$
		return new SamplingUnitFilter(bits[3], Type.SAMPLINGUNIT, keyToSource(bits[2]));
	}
	
	public static SamplingUnitFilter createMissionTrackFilter(String key){
		String[] bits = key.split(":"); //$NON-NLS-1$
		return new SamplingUnitFilter(bits[3], Type.TRACK, keyToSource(bits[2]));
	}
	
	public enum Type {SAMPLINGUNIT, TRACK};
	
	public enum Source {
		OBSERVATION ("obs"),  //$NON-NLS-1$
		TRACK ("trk");
		
		public String queryKey;
		
		Source(String queryKey){
			this.queryKey = queryKey;
		}
		
		public String getGuiName(Locale l){
			return SmartContext.INSTANCE.getClass(ISurveyQueryLabelProvider.class).getLabel(this, l);
		}
	};
	
	
	

	
	private String uuid;
	private Type unitType;
	private Source joinTable;
	
	
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
			if (isNone()){
				return "s:samplingunit:" + joinTable.queryKey + ":" + SamplingUnitFilter.NONE_KEY; //$NON-NLS-1$ //$NON-NLS-2$
			}else{
				return "s:samplingunit:" + joinTable.queryKey + ":" + uuid; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}else if (unitType == Type.TRACK){
			return "s:samplingunittrack:" + joinTable.queryKey + ":" + uuid; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
		
	}


}
