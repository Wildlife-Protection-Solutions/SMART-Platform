package org.wcs.smart.patrol.query.model;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolGroupBy;
import org.wcs.smart.patrol.query.ui.IPatrolOptionData;
import org.wcs.smart.patrol.query.ui.PatrolOptionData;
import org.wcs.smart.query.model.summary.AbstractGroupByViewer;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ErrorDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;
import org.wcs.smart.util.UuidUtils;

public class PatrolGroupByViewer extends AbstractGroupByViewer<PatrolGroupBy> {
	
	private IPatrolOptionData data;
	
	public PatrolGroupByViewer(PatrolGroupBy gb, IPatrolOptionData data) {
		super(gb);
		this.data = data;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.wp.sum.IGroupBy#getItems(org.hibernate.Session)
	 */
	@Override
	public List<ListItem> getItems(Session session) {
		if (groupBy.getItems() != null){
			return data.getListValues(session, groupBy.getItems());
		}
		List<ListItem> items = data.getListValues(session);
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
						if (groupBy.getOption() == PatrolQueryOption.CM && items[i].equals(IFilter.NULL_OP)) {
							String name = groupBy.getOption().getName(session, null, Locale.getDefault());
							initItems[i] = new ListItem(null, name, IFilter.NULL_OP);
						}else {
							UUID uuid = UuidUtils.stringToUuid(items[i]);
							String name = groupBy.getOption().getName(session, uuid, Locale.getDefault());
							if (name == null){
								throw new Exception(MessageFormat.format(Messages.PatrolGroupBy_PatrolOptionParseError2, new Object[]{groupBy.getOption().getGuiName(Locale.getDefault())}));
							}
							initItems[i] = new ListItem(uuid, name);
						}
						
					}else{
						initItems[i] = new ListItem(null, items[i], items[i]);
					}
				}
				it.initializeData(new Object[]{new PatrolOptionData(groupBy.getOption()), initItems});
			}
			return it;
		} catch (Exception ex) {
			return new ErrorDropItem(Messages.PatrolGroupBy_CouldNotParse + ex.getLocalizedMessage());
		}
		
	}

}
