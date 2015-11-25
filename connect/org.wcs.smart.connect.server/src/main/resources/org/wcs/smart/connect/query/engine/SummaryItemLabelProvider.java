/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.engine;

import java.text.Collator;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.connect.i18n.labels.SmartLabelProvider;
import org.wcs.smart.connect.query.WaypointSourceEngine;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeGroupBy;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.filter.summary.MissionAttributeGroupBy;
import org.wcs.smart.er.query.filter.summary.MissionIdGroupBy;
import org.wcs.smart.er.query.filter.summary.MissionValueItem;
import org.wcs.smart.er.query.filter.summary.SamplingUnitAttributeGroupBy;
import org.wcs.smart.er.query.filter.summary.SamplingUnitGroupBy;
import org.wcs.smart.er.query.filter.summary.SurveyIdGroupBy;
import org.wcs.smart.intelligence.query.IntelligencePatrolGroupBy;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.query.model.filter.WaypointSourceGroupBy;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.query.model.PatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolQueryOptionType;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolGroupBy;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolValueItem;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.date.DayDateGroupBy;
import org.wcs.smart.query.model.filter.date.IDateFilter;
import org.wcs.smart.query.model.filter.date.MonthDateGroupBy;
import org.wcs.smart.query.model.filter.date.YearDateGroupBy;
import org.wcs.smart.query.model.summary.AreaGroupBy;
import org.wcs.smart.query.model.summary.AttributeGroupBy;
import org.wcs.smart.query.model.summary.AttributeValueItem;
import org.wcs.smart.query.model.summary.CategoryGroupBy;
import org.wcs.smart.query.model.summary.CategoryValueItem;
import org.wcs.smart.query.model.summary.CombinedValueItem;
import org.wcs.smart.query.model.summary.ConservationAreaGroupBy;
import org.wcs.smart.query.model.summary.DateGroupBy;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.IValueItem.ValueType;
import org.wcs.smart.query.model.summary.ObserverGroupBy;
import org.wcs.smart.util.UuidUtils;

/**
 * Label provider for summary queries.
 * 
 * @author Emily
 *
 */
public class SummaryItemLabelProvider {
	
	private final Logger logger = Logger.getLogger(SummaryItemLabelProvider.class.getName());
	
	public static final String PER_LABEL = "per";
	
	private Locale l;
	private Session s;
	private ConservationAreaFilter caFilter;
	
	public SummaryItemLabelProvider(Locale l ,Session s, ConservationAreaFilter caFilter){
		this.l = l;
		this.s = s;
		this.caFilter = caFilter;
	}
	public String getName(IValueItem item){
		
		if (item instanceof PatrolValueItem){
			return ((PatrolValueItem) item).getPatrolValueOption().getGuiName(l);
		}else if (item instanceof MissionValueItem){
			return ((MissionValueItem)item).getValueItem().getGuiName(l);
		}else if (item  instanceof CombinedValueItem){
			CombinedValueItem citem = (CombinedValueItem)item;
			return getName(citem.getPart1()) + " " + PER_LABEL + " " +  getName(citem.getPart2());
		}else if (item instanceof CategoryValueItem){
			//TODO: support for CCAA
			return getName((CategoryValueItem)item, false);
		}else if (item instanceof  AttributeValueItem){
			//TODO: support for CCAA
			return getName((AttributeValueItem)item, false);
		}
		return MessageFormat.format("Value item {0} not supported", item.asString());
	}
	
	public String getFullName(IValueItem item){
		if (item instanceof PatrolValueItem ||
				item instanceof MissionValueItem){
			return getName(item);
		}
		if (item  instanceof CombinedValueItem){
			CombinedValueItem citem = (CombinedValueItem)item;
			return getFullName(citem.getPart1()) + " " + PER_LABEL + " " +  getName(citem.getPart2());
		}else if (item instanceof CategoryValueItem){
			//TODO: support for CCAA
			getName((CategoryValueItem)item, true);
		}else if (item instanceof  AttributeValueItem){
			//TODO: support for CCAA
			return getName((AttributeValueItem)item, true);
		}
		return MessageFormat.format("Value item {0} not supported", item.asString());
	}
	
	private Attribute getAttribute(String key){
		return (Attribute) s.createCriteria(Attribute.class)
				.add(Restrictions.eq("keyId", key))
				.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds()))
				.uniqueResult();
	}
	
	private Category getCategory(String catHkey){
		return (Category) s.createCriteria(Category.class)
				.add(Restrictions.eq("hkey", catHkey))
				.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds()))
				.uniqueResult();
	}
	
	private AttributeListItem getAttributeListIem(String key, Attribute a){
		//TODO: support ccaa queries 
		return (AttributeListItem) s.createCriteria(AttributeListItem.class)
				.add(Restrictions.eq("keyId", key))
				.add(Restrictions.eq("attribute", a))
				.uniqueResult();
	}
	
	private AttributeTreeNode getAttributeTreeItem(String hkey, Attribute a){
		//TODO: support ccaa queries
		return (AttributeTreeNode) s.createCriteria(AttributeTreeNode.class)
				.add(Restrictions.eq("hkey", hkey))
				.add(Restrictions.eq("attribute", a))
				.uniqueResult();
	}
	
	private String getName(CategoryValueItem item, boolean full){
		String catHkey = ((CategoryValueItem) item).getCategoryHKey();
		if (catHkey == null){
			return getLabel(item.getType()) + " " + "All Categories"; //$NON-NLS-1$
		}
		
		try{
			Category c = getCategory(catHkey);
			if (c == null){
				return catHkey;
			}
			if (full){
				return c.getFullCategoryName();
			}else{
				return c.getName();
			}
		}catch (Exception ex){
			logger.log(Level.WARNING, MessageFormat.format("Category not found {0}", catHkey), ex);
			return catHkey;
		}
	}
	private String getName(AttributeValueItem item, boolean full){
		String attributeKey = item.getAttributeKey();
		String itemKey = item.getItemKey();
		String categoryKey = item.getCategoryKey();
		
		Aggregation agg = (Aggregation) s.get(Aggregation.class, item.getAggregationKey());
				
		Attribute att = getAttribute(attributeKey);
		if (att == null){
			return item.asString();
		}
		String itemName = null;
		if (att.getType() == AttributeType.LIST){
			AttributeListItem it = getAttributeListIem(itemKey, att);
			if (it == null){
				logger.log(Level.WARNING, MessageFormat.format("Attribute list item with key {0} not found for attribute {1}.", itemKey, attributeKey));
				itemName = itemKey;
			}else{
				itemName = it.getName();
			}
		}else if (att.getType() == AttributeType.TREE){
			AttributeTreeNode it = getAttributeTreeItem(itemKey, att);
			if (it == null){
				logger.log(Level.WARNING, MessageFormat.format("Attribute tree node with key {0} not found for attribute {1}.", itemKey, attributeKey));
				itemName = itemKey;
			}else{
				itemName = it.getName();
			}
		}
		StringBuilder name = new StringBuilder();
		if (item.getValueType() != null){
			name.append(getLabel(item.getValueType()));
		}else if (agg != null){
			name.append(Aggregation.getGuiName(agg, s, l));
		}
		name.append(" "); //$NON-NLS-1$
		if (itemName != null){
			name.append(itemName);
			name.append(" [" + att.getName() + "] "); //$NON-NLS-1$ //$NON-NLS-2$
		}else{
			name.append(att.getName());
		}
		
		if (categoryKey != null){
			Category cat = getCategory(categoryKey);
			
			if (cat != null){
				if (full){
					name.append( " (" + cat.getFullCategoryName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				}else{
					name.append( " (" + cat.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}else{
				logger.log(Level.WARNING, MessageFormat.format("Category {0} not found.", categoryKey));
				name.append(" (" + categoryKey + " ) "); //$NON-NLS-1$
			}
		}
		return name.toString();
	}
	
	private String getLabel(ValueType type){
		if (type == ValueType.OBSERVATION){
			return "Count Observations";
		}else if (type == ValueType.WAYPOINT){
			return "Count Incidents";
		}
		return "";
	}
	
	
	
	public List<ListItem> getNames(IGroupBy item){
		List<ListItem> results = null;
		if (item instanceof AreaGroupBy){
			results = getName((AreaGroupBy)item);
		}else if (item instanceof AttributeGroupBy){
			results = getName((AttributeGroupBy)item);
		}else if (item instanceof CategoryGroupBy){
			results = getName((CategoryGroupBy)item);
		}else if (item instanceof ConservationAreaGroupBy){
			results = getName((ConservationAreaGroupBy)item);
		}else if (item instanceof DateGroupBy){
			results = getName((DateGroupBy)item);
		}else if (item instanceof EntityAttributeGroupBy){
			results = getName((EntityAttributeGroupBy)item);
		}else if (item instanceof MissionAttributeGroupBy){
			results = getName((MissionAttributeGroupBy)item);
		}else if (item instanceof MissionIdGroupBy){
			results = getName((MissionIdGroupBy)item);
		}else if (item instanceof ObserverGroupBy){
			results = getName((ObserverGroupBy)item);
		}else if (item instanceof PatrolGroupBy){
			results = getName((PatrolGroupBy)item);
		}else if (item instanceof SamplingUnitAttributeGroupBy){
			results = getName((SamplingUnitAttributeGroupBy)item);
		}else if (item instanceof SamplingUnitGroupBy){
			results = getName((SamplingUnitGroupBy)item);
		}else if (item instanceof SurveyIdGroupBy){
			results = getName((SurveyIdGroupBy)item);
		}else if (item instanceof WaypointSourceGroupBy){
			results = getName((WaypointSourceGroupBy)item);
		}else if (item instanceof IntelligencePatrolGroupBy){
			results = getName((IntelligencePatrolGroupBy)item);
		}
		if (results != null){
			Collections.sort(results, new Comparator<ListItem>() {
				Collator c = Collator.getInstance(l);				
				@Override
				public int compare(ListItem o1, ListItem o2) {
					return c.compare(o1.getName(), o2.getName());
				}
			});
		}
		return results;
	}
	
	@SuppressWarnings("unchecked")
	private List<ListItem> getName(AreaGroupBy item){
		String[] filterkeys = item.getAreaFilterKeys();
		Area.AreaType areaType = item.getAreaType();
		
		List<ListItem> items = new ArrayList<ListItem>();
		if (filterkeys != null && filterkeys.length > 0){
			
			List<Area> matching = s
					.createCriteria(Area.class)
					.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds()))
					.add(Restrictions.in("keyId", filterkeys)) //$NON-NLS-1$
					.add(Restrictions.eq("type", areaType)).list(); //$NON-NLS-1$
			
			for (int i = 0; i < filterkeys.length; i++){
				boolean found = false;
				for (Area a : matching){
					if (a.getKeyId().equals(filterkeys[i])){
						items.add(new ListItem(null, a.getName(), a.getKeyId()));
						found = true;
						break;
					}
				}
				if (!found){
					items.add(new ListItem(null, filterkeys[i], filterkeys[i]));
				}
			}
		}else{
			List<Area> matching = s
					.createCriteria(Area.class)
					.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds()))
					.add(Restrictions.eq("type", areaType)).list(); //$NON-NLS-1$
			
			for (Area a : matching){
				items.add(new ListItem(null, a.getName(), a.getKeyId()));
			}
		}
		
		return items;
	}
	
	@SuppressWarnings("unchecked")
	private List<AttributeTreeNode> getTreeNodes(Attribute att, int level){
		Query q = s.createQuery(" FROM AttributeTreeNode WHERE attribute =:att AND (length(hkey) - length(replace(hkey, '.', '')))-1 = :level");
		q.setParameter("att", att);
		q.setParameter("level", level);
		return q.list();
	}
	
	private List<ListItem> getName(AttributeGroupBy item){
		String[]  filterHkeys = item.getFilterKeys();
		//get children categories
		Attribute att = getAttribute(item.getAttributeKey());
		
		if (att == null){
			return null;
		}
		
		List<ListItem> items = new ArrayList<ListItem>();
		if (att.getType() == AttributeType.LIST){
			if (filterHkeys != null) {
				for (AttributeListItem it : att.getActiveListItems()) {
					for (String key : filterHkeys){
						if (key.equals(it.getKeyId())){
							items.add(new ListItem(null, it.getName(), it.getKeyId()));
							break;
						}
					}
				}
			}else{				
				for (AttributeListItem it : att.getActiveListItems()) {
					if (it.getIsActive()){
						items.add(new ListItem(null, it.getName(), it.getKeyId()));
					}
				}
			}
		}else if (att.getType() == AttributeType.TREE){
			List<AttributeTreeNode> nodes = getTreeNodes(att, item.getTreeLevel());
			if (filterHkeys == null){
				//get all attribute nodes with given hkey length
				for(AttributeTreeNode child : nodes){
					if (child.getIsActive()){
						items.add(new ListItem(null, child.getName(), child.getHkey()));
					}
				}
			}else{
				HashSet<String> keys = new HashSet<String>();
				for (int i = 0; i < filterHkeys.length; i ++){
					keys.add(filterHkeys[i]);
				}
				for(AttributeTreeNode child : nodes){
					if (keys.contains(child.getHkey())){
						items.add(new ListItem(null, child.getName(), child.getHkey()));	
					}
				}
			}
		}
		return items;
	}
	
	@SuppressWarnings("unchecked")
	private List<ListItem> getName(CategoryGroupBy item){
		//get children categories
		String[] filterHkeys = item.getFilterKeys();
		
		//find all categories with treeLevel + 1 . in them  
		List<ListItem> items = new ArrayList<ListItem>();
		if (filterHkeys != null && filterHkeys.length > 0){
			for (int i = 0; i < filterHkeys.length; i++){
				Category cat = getCategory(filterHkeys[i]);
				if (cat == null){
					items.add(new ListItem(null, "Category Not Found", filterHkeys[i]));
				}else{
					items.add( new ListItem(null, cat.getFullCategoryName(), cat.getHkey()) );
				}
			}
		}else{
			Query q = s.createQuery("FROM Category WHERE conservationArea.uuid in (:ca) and  (length(hkey) - length(replace(hkey, '.', ''))) - 1 = :level");
			q.setParameterList("ca", caFilter.getConservationAreaFilterIds());
			q.setParameter("level", item.getTreeLevel());
			List<Category> cats = q.list();
			for(Category child : cats){
				items.add(new ListItem(null, child.getFullCategoryName(), child.getHkey()));
			}
		}
		return items;
	}
	@SuppressWarnings("unchecked")
	private List<ListItem> getName(ConservationAreaGroupBy item){
		String[] filterHkeys = item.getFilterKeys();
		List<ListItem> items = new ArrayList<ListItem>();
		
		List<ConservationArea> cas = s.createCriteria(ConservationArea.class).list();
		for (ConservationArea ca : cas){
			items.add(new ListItem(ca.getUuid(), ca.getNameLabel()));
		}
		Collections.sort(items, new Comparator<ListItem>() {
			@Override
			public int compare(ListItem o1, ListItem o2) {
				return Collator.getInstance(l).compare(o1.getName(), o2.getName());
			}
		});
		
		if (filterHkeys != null){
			HashSet<String> lookup = new HashSet<String>();
			for (String k : filterHkeys){
				lookup.add(k);
			}
			List<ListItem> remove = new ArrayList<ListItem>();
			for (ListItem i : items){
				if (!lookup.contains(UuidUtils.uuidToString(i.getUuid()))){
					remove.add(i);
				}
			}
			items.removeAll(remove);
		}
		
		return items;
	}
	
	private List<ListItem> getName(DateGroupBy item){
		if (item.getOption() instanceof DayDateGroupBy) {
			return getDayItems(item.getDateFilter());
		} else if (item.getOption() instanceof MonthDateGroupBy) {
			return getMonthItems(item.getDateFilter());
		} else if (item.getOption() instanceof YearDateGroupBy) {
			return getYearItems(item.getDateFilter());
		}
		return null;
	}
	private List<ListItem> getDayItems(IDateFilter dateFilter) {

		ArrayList<ListItem> items = new ArrayList<ListItem>();
		Date startdate = new Date();
		Date enddate = new Date();
		if (dateFilter == null) {
			throw new IllegalStateException("Invalid date filter.");
		} else {
			if (dateFilter.getDates() == null) {
				// all daytes
				String hql = "SELECT min(dateTime) from Waypoint WHERE conservationArea.uuid IN (:ca)"; //$NON-NLS-1$
				Query q = s.createQuery(hql);
				q.setParameterList("ca", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$

				List<?> data = q.list();
				if (data != null && data.size() >= 1 && data.get(0) != null) {
					startdate = (java.sql.Timestamp) data.get(0);
				}
			} else {
				Date[] d = dateFilter.getDates();
				if (d.length >= 1) {
					startdate = d[0];
				}
				if (d.length >= 2) {
					enddate = d[1];
				}
			}
		}
		Calendar cals = Calendar.getInstance();
		cals.setTime(startdate);

		Calendar cale = Calendar.getInstance();
		cale.setTime(enddate);

		while (cals.before(cale)
				|| (dateFilter.isEndDateInclusive() && cals.equals(cale))) {
			java.sql.Date dd = new java.sql.Date(cals.getTime().getTime());
			String key = dd.toString();
			items.add(new ListItem(null, key, key));
			cals.add(Calendar.DAY_OF_MONTH, 1);
		}
		return items;
	}

	private List<ListItem> getMonthItems(IDateFilter dateFilter) {

		ArrayList<ListItem> items = new ArrayList<ListItem>();
		Date startdate = new Date();
		Date enddate = new Date();
		if (dateFilter == null) {
			throw new IllegalStateException("Invalid Date Filter.");
		} else {
			if (dateFilter.getDates() == null) {
				// all daytes
				String hql = "SELECT min(dateTime) from Waypoint WHERE conservationArea.uuid IN (:ca)"; //$NON-NLS-1$
				Query q = s.createQuery(hql);
				q.setParameterList("ca", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$

				List<?> data = q.list();
				if (data != null && data.size() >= 1 && data.get(0) != null) {
					startdate = (java.sql.Timestamp) data.get(0);
				}
			} else {
				Date[] d = dateFilter.getDates();
				if (d.length >= 1) {
					startdate = d[0];
				}
				if (d.length >= 2) {
					enddate = d[1];
				}
			}
		}
		Calendar cals = Calendar.getInstance();
		cals.setTime(startdate);

		Calendar cale = Calendar.getInstance();
		cale.setTime(enddate);

		// each month between start and end of
		// form "m/yyyy"
		SimpleDateFormat nameFormat = new SimpleDateFormat("MM/yyyy"); //$NON-NLS-1$
		SimpleDateFormat keyFormat = new SimpleDateFormat(
				"M/yyyy", Locale.ENGLISH); //$NON-NLS-1$

		cals.set(Calendar.DAY_OF_MONTH, 1);
		cale.set(Calendar.DAY_OF_MONTH,
				cale.getActualMaximum(Calendar.DAY_OF_MONTH));
		while (cals.before(cale)) {
			String key = keyFormat.format(cals.getTime());
			String name = nameFormat.format(cals.getTime());
			items.add(new ListItem(null, name, key));
			cals.add(Calendar.MONTH, 1);
		}
		return items;
	}

	private List<ListItem> getYearItems(IDateFilter dateFilter) {

		ArrayList<ListItem> items = new ArrayList<ListItem>();
		Date startdate = new Date();
		Date enddate = new Date();
		if (dateFilter == null) {
			throw new IllegalStateException("Invalid Date Filter.");
		} else {
			if (dateFilter.getDates() == null) {
				// all daytes
				String hql = "SELECT min(dateTime) from Waypoint WHERE conservationArea.uuid IN (:ca)"; //$NON-NLS-1$
				Query q = s.createQuery(hql);
				q.setParameterList("ca", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$

				List<?> data = q.list();
				if (data != null && data.size() >= 1 && data.get(0) != null) {
					startdate = (java.sql.Timestamp) data.get(0);
				}
			} else {
				Date[] d = dateFilter.getDates();
				if (d.length >= 1) {
					startdate = d[0];
				}
				if (d.length >= 2) {
					enddate = d[1];
				}
			}
		}
		Calendar cals = Calendar.getInstance();
		cals.setTime(startdate);

		Calendar cale = Calendar.getInstance();
		cale.setTime(enddate);

		// each year in form yyyy
		int year = cals.get(Calendar.YEAR);
		int year2 = cale.get(Calendar.YEAR);
		GregorianCalendar gcal = new GregorianCalendar();
		while (year <= year2) {
			Calendar c = Calendar.getInstance();
			c.set(year, 0, 1);
			gcal.setTime(c.getTime());

			items.add(new ListItem(null, String.valueOf(year), String
					.valueOf(gcal.get(Calendar.YEAR))));
			year++;
		}
		return items;
	}
	
	private List<ListItem> getName(EntityAttributeGroupBy item){
		//get children categories
		List<ListItem> items = new ArrayList<ListItem>();
		EntityAttribute ea = (EntityAttribute) s.createCriteria(EntityAttribute.class)
				.add(Restrictions.in("entityType.conservationArea.uuid", caFilter.getConservationAreaFilterIds()))
				.add(Restrictions.eq("keyId", item.getEntityAttributeKey()))
				.add(Restrictions.eq("entityType.keyId", item.getEntityAttributeKey()))
				.uniqueResult();
				
		if (ea == null){
			logger.severe(MessageFormat.format("Entity attribute not found {0}.", item.getEntityAttributeKey()));
			return items;
		}
						
		Attribute att = ea.getDmAttribute();
		if (att.getType() == AttributeType.LIST){
			String[] filterHkeys = item.getFilterKeys();
			if (filterHkeys != null) {
				for (AttributeListItem it : att.getAttributeList()) {
					for (int i = 0; i < filterHkeys.length; i++) {
						if (filterHkeys[i].equals(it.getKeyId())) {
							items.add(new ListItem(null, it.getName() + " [" + ea.getEntityType().getName()  + "]", it.getKeyId()));
							break;
						}
					}
				}
			}else{
				for (AttributeListItem it : att.getActiveListItems()) {
					items.add(new ListItem(null, it.getName() + " [" + ea.getEntityType().getName()  + "]", it.getKeyId())); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}else if (att.getType() == AttributeType.TREE){
			String[] filterHkeys = item.getFilterKeys();
			List<AttributeTreeNode> nodes = getTreeNodes(att, item.getTreeLevel());
			if (filterHkeys == null){
				//get all attribute nodes with given hkey length
				for(AttributeTreeNode child : nodes){
					items.add(new ListItem(null, child.getName() + " [" + ea.getEntityType().getName()  + "]", child.getHkey())); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}else{
				HashSet<String> keys = new HashSet<String>();
				for (int i = 0; i < filterHkeys.length; i ++){
					keys.add(filterHkeys[i]);
				}
				for(AttributeTreeNode child : nodes){
					if (keys.contains(child.getHkey())){
						items.add(new ListItem(null, child.getName() + " [" + ea.getEntityType().getName()  + "]", child.getHkey()));	 //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
		}
		return items;
	}
	
	private List<ListItem> getName(MissionAttributeGroupBy item){
		MissionAttribute ma = (MissionAttribute) s.createCriteria(MissionAttribute.class)
				.add(Restrictions.eq("keyId", item.getAttributeKey())) //$NON-NLS-1$
				.add(Restrictions.in("conservationArea", caFilter.getConservationAreaFilterIds())) //$NON-NLS-1$
				.uniqueResult();
		if (ma == null){
			logger.warning(MessageFormat.format("Mission attribute not found {0}.", item.getAttributeKey()));;
		}
		ArrayList<ListItem> allItems = new ArrayList<ListItem>();
		String[] items = item.getRawItems();
		if (items != null && items.length > 0){
			for (String it : items){
				for (MissionAttributeListItem mli : ma.getAttributeList()){
					if (mli.getKeyId().equals(it)){
						allItems.add(new ListItem(null, mli.getName(), mli.getKeyId()));
					}
				}
			}
		}else{
			for (MissionAttributeListItem mli : ma.getAttributeList()){
				allItems.add(new ListItem(null, mli.getName(), mli.getKeyId()));
			}
		}
		return allItems;
	}
	
	private List<ListItem> getName(MissionIdGroupBy item){
		String[] items = item.getRawItems();
		List<ListItem> allItems = new ArrayList<ListItem>();
		if (items != null && items.length > 0){
			for (String it : items){
				Mission m = (Mission) s.load(Mission.class, UuidUtils.stringToUuid(it));
				if (m != null){
					allItems.add(new ListItem(m.getUuid(), m.getId()));
				}else{
					logger.warning(MessageFormat.format("Mission not found", it));
					allItems.add(new ListItem(UuidUtils.stringToUuid(it), it));
				}
				
			}
		}
		return allItems;
	}
	@SuppressWarnings("unchecked")
	private List<ListItem> getName(ObserverGroupBy item){
		String[] filterkeys = item.getFilterKeys();
		List<ListItem> items = new ArrayList<ListItem>();
		if (filterkeys != null && filterkeys.length > 0){
			for (String uuid : filterkeys){
				Employee e = (Employee) s.load(Employee.class, UuidUtils.stringToUuid(uuid));
				items.add(new ListItem(e.getUuid(), SmartLabelProvider.getFullName(e)));
			}		
		}else{
			List<Employee> es = s.createCriteria(Employee.class)
					.add(Restrictions.in("conservationArea", caFilter.getConservationAreaFilterIds()))
					.list();
			Collections.sort(es, new Comparator<Employee>() {
				@Override
				public int compare(Employee arg0, Employee arg1) {
					return Collator.getInstance().compare(SmartLabelProvider.getFullName(arg0).toUpperCase(), 
							SmartLabelProvider.getFullName(arg1).toUpperCase());
				}
			});
			for (Employee e : es){
				items.add(new ListItem(e.getUuid(), SmartLabelProvider.getFullName(e)));
			}
		}
		return items;
	}
	private List<ListItem> getName(PatrolGroupBy item){
		List<ListItem> results = new ArrayList<ListItem>();
		PatrolQueryOptionType type = item.getOption().getType();
		
		String[] keys = item.getItems();
		
		
		if (type == PatrolQueryOptionType.UUID){
			Criteria c = s.createCriteria(item.getOption().getSourceClass());
			if (keys != null){
				UUID[] uuidkeys = new UUID[keys.length];
				for (int i = 0; i < keys.length; i++) {
					uuidkeys[i] = UuidUtils.stringToUuid(keys[i]);
				}
				c = c.add(Restrictions.in("uuid", uuidkeys)); //$NON-NLS-1$
			}else{
				//ca filter
				if (item.getOption() == PatrolQueryOption.RANK){
					c.add(Restrictions.in("agency.conservationArea", caFilter.getConservationAreaFilterIds()));
				}else if (item.getOption() == PatrolQueryOption.CONSERVATION_AREA){
					c.add(Restrictions.in("uuid", caFilter.getConservationAreaFilterIds()));
				}else{
					c.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds()));
				}
			}
			Collection<?> data = c.list();
			List<UUID> caUuids = caFilter.getConservationAreaFilterIds();
			for (Iterator<?> iterator = data.iterator(); iterator.hasNext();) {
				Object object = (Object) iterator.next();
				if (object instanceof NamedItem){
					results.add(new ListItem(((NamedItem) object).getUuid(), ((NamedItem) object).getName()));
				}else if (object instanceof Employee){
					Employee e = (Employee)object;
					if (caUuids.contains(e.getConservationArea().getUuid())){
						results.add(new ListItem(e.getUuid(), SmartLabelProvider.getFullName((Employee) e)));
					}
				}else if (object instanceof ConservationArea){
					ConservationArea ca = (ConservationArea)object;
					results.add(new ListItem(ca.getUuid(), ca.getNameLabel()));
				}else if (object instanceof ListItem){
					results.add((ListItem)object);
				}
			}
		}else if (type == PatrolQueryOptionType.KEY){
			Class<?> queryClazz = null;
			if (item.getOption() == PatrolQueryOption.TEAM_KEY){
				queryClazz = Team.class;
			}else if (item.getOption() == PatrolQueryOption.PATROL_TRANSPORT_TYPE_KEY){
				queryClazz = PatrolTransportType.class;
			}else if (item.getOption() == PatrolQueryOption.MANDATE_KEY){
				queryClazz = PatrolMandate.class;
			}
			if (queryClazz == null){
				throw new UnsupportedOperationException("Patrol Query option not supported: " + item.getOption());	
			}
			List<ListItem> data = new ArrayList<ListItem>();
			Criteria c = s.createCriteria(queryClazz)
					.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds()));
			for (Iterator<?> it = c.list().iterator(); it.hasNext();){
				NamedKeyItem nkitem = (NamedKeyItem)it.next();
				data.add(new ListItem(null, nkitem.getName(),nkitem.getKeyId()));
					
			}
			if (data != null){
				for (Iterator<?> iterator = data.iterator(); iterator.hasNext();) {
					ListItem it = (ListItem) iterator.next();
					if (keys != null && Arrays.asList(keys).contains(it.getKey())){
						results.add(it);
					}
				}
			}
		}else if (type == PatrolQueryOptionType.STRING){
			if (item.getOption() == PatrolQueryOption.ID){
				Criteria c = s.createCriteria(item.getOption().getSourceClass())
						.add(Restrictions.in("conservationArea", caFilter.getConservationAreaFilterIds())); //$NON-NLS-1$
				if (keys != null){
					c = c.add(Restrictions.in(item.getOption().getColumnName(), keys));
				} 
				 List<?> data = c.list();
				 for (Iterator<?> iterator = data.iterator(); iterator.hasNext();) {
					Object object = (Object) iterator.next();
					if (object instanceof Patrol){
						results.add(new ListItem( null, ((Patrol) object).getId(),((Patrol) object).getId() ));
					}
				}
			}else if (item.getOption() == PatrolQueryOption.PATROL_TYPE){
				if (keys == null){
					for (PatrolType.Type t : PatrolType.Type.values()){
						results.add(new ListItem(null, t.getGuiName(l), t.name()));
					}
				}else{
					for (int i = 0; i < keys.length; i ++){
						results.add(new ListItem(null, PatrolType.Type.valueOf(keys[i]).getGuiName(l), keys[i]));
					}
				}
			}
		}
		return results;
	}
	private List<ListItem> getName(SamplingUnitAttributeGroupBy item){
		SamplingUnitAttribute su = (SamplingUnitAttribute) s.createCriteria(SamplingUnitAttribute.class)
				.add(Restrictions.eq("keyId", item.getAttributeKey())) //$NON-NLS-1$
				.add(Restrictions.in("conservationArea", caFilter.getConservationAreaFilterIds())) //$NON-NLS-1$
				.uniqueResult();
		if (su == null){
			logger.warning(MessageFormat.format("Sampling unit attribute not found {0}.", item.getAttributeKey()));
			return null;
		}
		String items[] = item.getRawItems();
		ArrayList<ListItem> allItems = new ArrayList<ListItem>();
		if (items != null && items.length > 0){
			for (String it : items){
				for (SamplingUnitAttributeListItem mli : su.getAttributeList()){
					if (mli.getKeyId().equals(it)){
						allItems.add(new ListItem(null, mli.getName(), mli.getKeyId()));
					}
				}
			}
		}else{
			for (SamplingUnitAttributeListItem mli : su.getAttributeList()){
				allItems.add(new ListItem(null, mli.getName(), mli.getKeyId()));
			}
		}
		return allItems;
	}
	private List<ListItem> getName(SamplingUnitGroupBy item){
		String[] items = item.getRawItems();
		List<ListItem> listItems = new ArrayList<ListItem>();
		if (items != null){
			for (String it : items){
					if (it.equals(SamplingUnitFilter.NONE_KEY)){
						listItems.add(new ListItem(null, "None", null));
					}else{
						SamplingUnit su = (SamplingUnit) s.get(SamplingUnit.class,
								UuidUtils.stringToUuid(it));
						if (su != null){
							listItems.add(new ListItem(su.getUuid(), su.getId()));
						}else{
							logger.warning(MessageFormat.format("SamplingUnit not found {0}", it));
							listItems.add(new ListItem(UuidUtils.stringToUuid(it), it));
						}
					}
			}
		}else{
			//all sampling units for associated design
			return null;
		}
		return listItems;
	}
	
	private List<ListItem> getName(SurveyIdGroupBy item){
		String[] items = item.getRawItems();
		List<ListItem> allItems = new ArrayList<ListItem>();
		if (items != null){
			for (String it : items){
				Survey survey = (Survey) s.load(Survey.class, UuidUtils.stringToUuid(it));
				if (survey != null){
					allItems.add(new ListItem(survey.getUuid(), survey.getId()));
				}else{
					logger.warning(MessageFormat.format("Survey not found {0}", it));
					allItems.add(new ListItem(UuidUtils.stringToUuid(it), it));
				}
			}
		}
		return allItems;
	}
	private List<ListItem> getName(WaypointSourceGroupBy item){
		String[] keys = item.getKeys();
		
		List<ListItem> items = new ArrayList<ListItem>();
		if (keys == null){
			for(IWaypointSource src : WaypointSourceEngine.INSTANCE.getSupportedSources()){
				items.add(new ListItem(null,src.getName(l), src.getKey()));
			}
		}else{
			for (String k : keys){
				IWaypointSource c = WaypointSourceEngine.INSTANCE.getSource(k);
				if (c != null){
					items.add(new ListItem(null, c.getName(l), c.getKey()));
				}
			}
		}
		return items;
	}
	
	
	private List<ListItem> getName(IntelligencePatrolGroupBy item){
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		items.add(new ListItem(null, "Motivated", IntelligencePatrolGroupBy.Options.MOTIVATED.getKey())); 
		items.add(new ListItem(null, "Not Motiviated", IntelligencePatrolGroupBy.Options.NOT_MOTIVATED.getKey()));
				return items;
	}
}
