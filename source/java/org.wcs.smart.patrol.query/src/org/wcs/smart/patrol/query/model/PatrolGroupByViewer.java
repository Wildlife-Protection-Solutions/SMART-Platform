package org.wcs.smart.patrol.query.model;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolGroupBy;
import org.wcs.smart.patrol.query.ui.IPatrolOptionData;
import org.wcs.smart.patrol.query.ui.PatrolOptionData;
import org.wcs.smart.query.model.summary.AbstractGroupByViewer;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;
import org.wcs.smart.util.UuidUtils;

public class PatrolGroupByViewer extends AbstractGroupByViewer<PatrolGroupBy> {

	private IPatrolOptionData data;
	
	public PatrolGroupByViewer(PatrolGroupBy gb) {
		super(gb);
		data = PatrolOptionData.findData(groupBy.getOption());
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.wp.sum.IGroupBy#getItems(org.hibernate.Session)
	 */
	@Override
	public List<ListItem> getItems(Session session) {
		
		if (groupBy.getItems() != null){
			return data.getValues(session, groupBy.getItems());
		}
		List<ListItem> items = data.getAllActiveValues(session);
		return items;		
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asDropItem(org.hibernate.Session)
	 */
	@Override
	public DropItem asDropItem(Session session) throws Exception{
		String[] items = groupBy.getItems();
		try {
			DropItem it = PatrolDropItemFactory.INSTANCE.createPatrolGroupByDropItem(groupBy.getOption());
			if (items != null){
				ListItem[] initItems = new ListItem[items.length];
				for (int i = 0; i < initItems.length; i++) {
					if (groupBy.getOption().getType() == PatrolQueryOptionType.UUID){
						UUID uuid = UuidUtils.stringToUuid(items[i]);
						String name = groupBy.getOption().getName(session, uuid, Locale.getDefault());
						if (name == null){
							throw new Exception(MessageFormat.format(Messages.PatrolGroupBy_PatrolOptionParseError, new Object[]{groupBy.getOption().getGuiName(Locale.getDefault())}));
						}
						initItems[i] = new ListItem(uuid, name);
					}else{
						initItems[i] = new ListItem(null, items[i], items[i]);
					}
				}
				it.initializeData(initItems);
			}
			return it;
		} catch (Exception ex) {
			return new ErrorDropItem(Messages.PatrolGroupBy_CouldNotParse + ex.getLocalizedMessage());
		}
		
	}

}
