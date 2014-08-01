package org.wcs.smart.er.query.filter;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.filter.SurveyFilter.Type;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;
import org.wcs.smart.util.SmartUtils;

public class MissionFilter implements IFilter {

	public static final String ID_QUERY_KEY = "s:mission:id"; //$NON-NLS-1$
	public static final String UUID_QUERY_KEY = "s:mission:uuid"; //$NON-NLS-1$
	
	/**
	 * Creates a survey filter.
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
		return new MissionFilter(Type.ID, op, value);
	}

	/**
	 * Creates a uuid filter in the form s:survey:uuid:<HEXUUID>
	 * @param key
	 * @return
	 */
	public static MissionFilter createUuidFilter(String key){
		return new MissionFilter(Type.UUID, null, key.split(";")[3]);
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
			return ID_QUERY_KEY + " " + op.asSmartValue() + " \"" + value + "\"";
		}else{
			return UUID_QUERY_KEY + ":" + value;
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
			di.initializeData(new Object[]{op.asSmartValue(), value});
			return new DropItem[]{di};
		}else if (type == Type.UUID){
			try{
				Mission mission = (Mission) session.load(Mission.class, SmartUtils.decodeHex(value));
				if (mission == null){
					return new DropItem[]{new ErrorDropItem(MessageFormat.format("Survey {0} not found.", new Object[]{value}))};
				}
				return new DropItem[]{SurveyDropItemFactory.INSTANCE.createMissionUuidIdDropItem(mission)};
			}catch (Exception ex){
				ERQueryPlugIn.log(ex.getMessage(), ex);
				return new DropItem[]{new ErrorDropItem(MessageFormat.format("Survey {0} not found.", new Object[]{value}))};
			}
		}
		return null;
	}

}