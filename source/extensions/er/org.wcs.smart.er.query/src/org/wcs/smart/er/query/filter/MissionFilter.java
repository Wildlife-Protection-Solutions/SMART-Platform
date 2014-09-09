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
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;
import org.wcs.smart.util.SmartUtils;

/**
 * Represents both mission id and uuid filters.
 * 
 * @author Emily
 *
 */
public class MissionFilter implements IFilter {

	public static final String ID_QUERY_KEY = "s:mission:id"; //$NON-NLS-1$
	public static final String UUID_QUERY_KEY = "s:mission:uuid"; //$NON-NLS-1$
	
	/**
	 * Creates a mission filter.
	 * 
	 * @return
	 */
	public static MissionFilter createIdFilter(Operator op, String value){
		boolean ok = false;
		for (Operator s : Operator.STRING_OPS){
			if (s.equals(op)){
				ok = true;
				break;
			}
		}
		if (!ok){
			throw new RuntimeException("String operator not supported for survey id filter."); //$NON-NLS-1$
		}
		return new MissionFilter(Type.ID, op, SmartUtils.stripQuotes(value));
	}

	/**
	 * Creates a mission uuid filter 
	 * @param key
	 * @return
	 */
	public static MissionFilter createUuidFilter(String key){
		return new MissionFilter(Type.UUID, null, key.split(":")[3]); //$NON-NLS-1$
	}

	public enum Type {ID, UUID};
	
	private Operator op;
	private String value;
	private Type type;
	
	public MissionFilter(Type type, Operator op, String value){
		this.type = type;
		this.op = op;
		this.value = value;
	}
	
	/**
	 * Get survey filter type
	 * @return
	 */
	public Type getType(){
		return this.type;
	}
	
	/**
	 * get operator
	 * @return
	 */
	public Operator getOperator(){
		return this.op;
	}
	
	/**
	 * get value associated with filter
	 * @return
	 */
	public String getValue(){
		return this.value;
	}
	
	@Override
	public String asString() {
		if (type == Type.ID){
			return ID_QUERY_KEY + " " + op.asSmartValue() + " \"" + value + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else{
			return UUID_QUERY_KEY + ":" + value; //$NON-NLS-1$
		}
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public DropItem[] getDropItems(Session session) throws Exception {
		if (type == Type.ID){
			DropItem di = SurveyDropItemFactory.INSTANCE.createMissionIdDropItem();
			di.initializeData(new String[]{op.asSmartValue(), value});
			return new DropItem[]{di};
		}else if (type == Type.UUID){
			try{
				Mission mission = (Mission) session.load(Mission.class, SmartUtils.decodeHex(value));
				if (mission == null){
					return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.MissionFilter_MissionNotFound, new Object[]{value}))};
				}
				mission.getId();
				mission.getSurvey().getId();
				return new DropItem[]{SurveyDropItemFactory.INSTANCE.createMissionUuidIdDropItem(mission)};
			}catch (Exception ex){
				ERQueryPlugIn.log(ex.getMessage(), ex);
				return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.MissionFilter_MissionNotFound, new Object[]{value}))};
			}
		}
		return null;
	}

}