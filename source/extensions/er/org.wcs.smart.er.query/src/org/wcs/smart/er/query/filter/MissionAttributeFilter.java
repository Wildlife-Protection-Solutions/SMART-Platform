package org.wcs.smart.er.query.filter;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

public class MissionAttributeFilter implements IFilter {

	public static final String QUERY_KEY = "s:missionproperty"; //$NON-NLS-1$
	
	/**
	 * Creates a survey filter.
	 * 
	 * @return
	 */
	public static MissionAttributeFilter createFilter(String key, Operator op, Object value){
		String[] bits = key.split(":");
		return new MissionAttributeFilter(bits[2], bits[3], op, value);
	}

	
	private Operator op;
	private Object value;
	
	private String typeKey;
	private String key;
	
	public MissionAttributeFilter(String typeKey, String key, Operator op, Object value){
		this.typeKey = typeKey;
		this.key = key;
		this.op = op;
		this.value = value;
	}
	
	@Override
	public String asString() {
		String fullIdentifier = QUERY_KEY + ":" + typeKey + ":"+key ;
		if (typeKey.equals(AttributeType.NUMERIC.typeKey)){
			return fullIdentifier + " " + op.asSmartValue() + " " + ((Double)value).toString();  //$NON-NLS-1$  //$NON-NLS-2$
		}else if (typeKey.equals(AttributeType.TEXT.typeKey)){
			return fullIdentifier + " " + op.asSmartValue() + " \"" + ((String)value) + "\"";  //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$ 
		}else if (typeKey.equals(AttributeType.LIST.typeKey)){
			return fullIdentifier + " " + op.asSmartValue() + " " + ((String)value);  //$NON-NLS-1$  //$NON-NLS-2$  
		}
		return null;
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public DropItem[] getDropItems(Session session) throws Exception {
		try{
			MissionAttribute ma = (MissionAttribute) session.createCriteria(MissionAttribute.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.add(Restrictions.eq("keyId", key)).list().get(0);
			DropItem di = SurveyDropItemFactory.INSTANCE.createMissionAttributeDropItem(ma);
			
			if (typeKey.equals(AttributeType.NUMERIC.typeKey) ||
					typeKey.equals(AttributeType.TEXT.typeKey)){
				di.initializeData(new Object[]{op.asSmartValue(), value.toString()}); 
			}else if (typeKey.equals(AttributeType.LIST.typeKey)){
				
				boolean ok = false;
				for (MissionAttributeListItem item : ma.getAttributeList()){
					if (item.getKeyId().equals(value)){
						ok = true;
						di.initializeData(new ListItem(item.getUuid(), item.getName(), item.getKeyId()));
					}
				}
				if (!ok){
					return new DropItem[]{new ErrorDropItem(MessageFormat.format("Mission attribute list item {0} not found.", new Object[]{key}))};		
				}
			}
			return new DropItem[]{di};
		}catch (Exception ex){
			ERQueryPlugIn.log(ex.getMessage(), ex);
			return new DropItem[]{new ErrorDropItem(MessageFormat.format("Mission attribute {0} not found.", new Object[]{key}))};
		}
	}

}
