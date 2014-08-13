package org.wcs.smart.er.query.filter.summary;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.util.SmartUtils;

public class MissionIdGroupBy implements IGroupBy {

	public static MissionIdGroupBy createGroupBy(String key){
		String bits[] = key.split(":"); //$NON-NLS-1$
		if (bits.length > 3){
			String items[] = new String[bits.length - 3];
			for (int i = 3; i < bits.length; i ++){
				items[i-3] = bits[i];
			}
			return new MissionIdGroupBy(items);
		}else{
			return new MissionIdGroupBy(null);
		}
	}
	
	private String[] items;
	private List<ListItem> allItems;
	
	private MissionIdGroupBy(String[] items){
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
		return "s:mission:id:"; //$NON-NLS-1$
	}

	@Override
	public GroupByType getType() {
		return GroupByType.BYTE;
	}

	@Override
	public List<ListItem> getItems(Session session) {
		if (allItems != null){
			return allItems;
		}
		
		allItems = new ArrayList<ListItem>();
		if (items != null){
			for (String it : items){
				try{
					Mission m = (Mission) session.load(Mission.class, SmartUtils.decodeHex(it));
					if (m != null){
						allItems.add(new ListItem(m.getUuid(), m.getId()));
					}
				}catch (Exception ex){
					EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
				}
			}
		}
		return allItems;
	}

	@Override
	public DropItem asDropItem(Session session) throws Exception {
		DropItem di = SurveyDropItemFactory.INSTANCE.createMissionIdGroupByDropItem();
		di.initializeData(getItems(session));
		return di;
	}

	@Override
	public void visit(IGroupByVisitor visitor) {
		visitor.visit(this);		
	}

}
