package org.wcs.smart.er.query.filter.summary;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

public class MissionAttributeGroupBy implements IGroupBy{
	
	/**
	 * s:missionproperty:l:" < DM_KEY > ":" ( < DM_KEY > )? (":" < DM_KEY > )* ) 
	 * @param key
	 * @return
	 */
	public static MissionAttributeGroupBy createGroupBy(String key){
		String bits[] = key.split(":"); //$NON-NLS-1$
		String attributeKey = bits[3];
		
		if (bits.length > 4){
			String items[] = new String[bits.length - 4];
			for (int i = 4; i < bits.length; i ++){
				items[i-4] = bits[i];
			}
			return new MissionAttributeGroupBy(attributeKey, items);
		}else{
			return new MissionAttributeGroupBy(attributeKey, null);
		}
	}
	
	private List<ListItem> allItems;
	
	private String attributeKey; 
	private String[] items;
	
	private MissionAttributeGroupBy(String attributeKey, String[] items){
		this.attributeKey = attributeKey;
		this.items = items;
	}
	
	@Override
	public String asString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getKeyPart());
		if (items != null){
			for (String it : items){
				sb.append(it);
				sb.append(":"); //$NON-NLS-1$
			}
			if (items.length > 0){
				sb.deleteCharAt(sb.length() - 1);
			}
		}
		return sb.toString();
	}

	@Override
	public String getKeyPart() {
		return "s:missionproperty:l:"; //$NON-NLS-1$
	}

	@Override
	public GroupByType getType() {
		return GroupByType.KEY;
	}

	@Override
	public List<ListItem> getItems(Session session) {
		if (allItems != null){
			return allItems;
		}
		
		MissionAttribute ma = null;
		try{
			ma = getMissionAttribute(session);
		}catch (Exception ex){
			throw new RuntimeException(ex.getMessage());
		}
		allItems = new ArrayList<ListItem>();
		if (items != null){
			for (String it : items){
				for (MissionAttributeListItem mli : ma.getAttributeList()){
					if (mli.getKeyId().equals(it)){
						allItems.add(new ListItem(null, mli.getName(), mli.getKeyId()));
					}
				}
			}
		}
		return allItems;
	}
	
	private MissionAttribute getMissionAttribute(Session session) throws Exception{
		List<?> items = session.createCriteria(MissionAttribute.class)
				.add(Restrictions.eq("keyId", attributeKey)) //$NON-NLS-1$
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.list();
		if (items.size() == 0){
			throw new Exception(MessageFormat.format("Mission attribute with key {0} not found.", new Object[]{attributeKey}));
		}else if (items.size() > 1){
			throw new Exception(MessageFormat.format("Mission attribute key {0} invalid.", new Object[]{attributeKey}));
		}
		MissionAttribute ma = (MissionAttribute) items.get(0);
		return ma;
	}

	@Override
	public DropItem asDropItem(Session session) throws Exception {
		MissionAttribute ma = null;
		try{
			ma = getMissionAttribute(session);
		}catch (Exception ex){
			return new ErrorDropItem(ex.getMessage());
		}
		DropItem di = SurveyDropItemFactory.INSTANCE.createMissionAttributeDropItem(ma);
		di.initializeData(getItems(session));
		return di;
	}

	@Override
	public void visit(IGroupByVisitor visitor) {
		visitor.visit(this);		
	}

}
