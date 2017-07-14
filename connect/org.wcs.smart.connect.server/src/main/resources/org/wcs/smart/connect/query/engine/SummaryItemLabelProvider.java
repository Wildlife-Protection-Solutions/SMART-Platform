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
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.i18n.labels.SmartLabelProvider;
import org.wcs.smart.connect.query.WaypointSourceEngine;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeGroupBy;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
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
import org.wcs.smart.query.model.filter.date.EndHourGroupBy;
import org.wcs.smart.query.model.filter.date.IDateFilter;
import org.wcs.smart.query.model.filter.date.MonthDateGroupBy;
import org.wcs.smart.query.model.filter.date.StartHourGroupBy;
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
	
	private static final String PER_LABEL = "SummaryItemLabelProvider.perLabel"; //$NON-NLS-1$
	
	private Locale l;
	private Session s;
	private ConservationAreaFilter caFilter;
	private SurveyDesignFilter sdFilter;
	
	public SummaryItemLabelProvider(Locale l ,Session s, ConservationAreaFilter caFilter, SurveyDesignFilter sdFilter){
		this.l = l;
		this.s = s;
		this.sdFilter = sdFilter;
		this.caFilter = caFilter;
	}
	public SummaryItemLabelProvider(Locale l ,Session s, ConservationAreaFilter caFilter){
		this(l, s, caFilter, null);
	}
	
	public String getName(IValueItem item){
		if (item instanceof PatrolValueItem){
			return ((PatrolValueItem) item).getPatrolValueOption().getGuiName(l);
		}else if (item instanceof MissionValueItem){
			return ((MissionValueItem)item).getValueItem().getGuiName(l);
		}else if (item  instanceof CombinedValueItem){
			CombinedValueItem citem = (CombinedValueItem)item;
			return getName(citem.getPart1()) + " " + Messages.getString(PER_LABEL,l) + " " +  getName(citem.getPart2()); //$NON-NLS-1$ //$NON-NLS-2$
		}else if (item instanceof CategoryValueItem){
			return getName((CategoryValueItem)item, false);
		}else if (item instanceof  AttributeValueItem){
			return getName((AttributeValueItem)item, false);
		}
		return MessageFormat.format(Messages.getString("SummaryItemLabelProvider.Valuenotsupported", l), item.asString()); //$NON-NLS-1$
	}
	
	public String getFullName(IValueItem item){
		if (item instanceof PatrolValueItem ||
				item instanceof MissionValueItem){
			return getName(item);
		}
		if (item  instanceof CombinedValueItem){
			CombinedValueItem citem = (CombinedValueItem)item;
			return getFullName(citem.getPart1()) + " " + Messages.getString(PER_LABEL, l) + " " +  getName(citem.getPart2()); //$NON-NLS-1$ //$NON-NLS-2$
		}else if (item instanceof CategoryValueItem){
			getName((CategoryValueItem)item, true);
		}else if (item instanceof  AttributeValueItem){
			return getName((AttributeValueItem)item, true);
		}
		return MessageFormat.format(Messages.getString("SummaryItemLabelProvider.Valuenotsupported", l), item.asString()); //$NON-NLS-1$
	}
	
	private Attribute getAttribute(String key){
		//find an attribute with the given key in 
		//any of the conservation areas
		return (Attribute) s.createCriteria(Attribute.class)
				.add(Restrictions.eq("keyId", key)) //$NON-NLS-1$
				.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds())) //$NON-NLS-1$
				.setMaxResults(1)
				.uniqueResult();
	}
	
	private Category getCategory(String catHkey){
		//find a category with the given given
		//in any of the conservation areas
		return (Category) s.createCriteria(Category.class)
				.add(Restrictions.eq("hkey", catHkey)) //$NON-NLS-1$
				.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds())) //$NON-NLS-1$
				.setMaxResults(1)
				.uniqueResult();
	}
	
	private AttributeListItem getAttributeListIem(String key, String attributeKey){
		//find the first attribute list item with the given key for one
		//of the conservation areas; we just pick one
		return (AttributeListItem) s.createCriteria(AttributeListItem.class, "li") //$NON-NLS-1$
				.createCriteria("attribute", "a") //$NON-NLS-1$ //$NON-NLS-2$
				.add(Restrictions.eq("li.keyId", key)) //$NON-NLS-1$
				.add(Restrictions.eq("a.keyId", attributeKey)) //$NON-NLS-1$
				.add(Restrictions.in("a.conservationArea.uuid", caFilter.getConservationAreaFilterIds())) //$NON-NLS-1$
				.setMaxResults(1)
				.uniqueResult();
	}
	
	private AttributeTreeNode getAttributeTreeItem(String hkey, String attributeKey){
		//find the first attribute tree node with the given key for one
		//of the conservation areas; we just pick one
		return (AttributeTreeNode) s.createCriteria(AttributeTreeNode.class)
				.add(Restrictions.eq("hkey", hkey)) //$NON-NLS-1$
				.createCriteria("attribute", "a") //$NON-NLS-1$ //$NON-NLS-2$
				.add(Restrictions.eq("a.keyId", attributeKey)) //$NON-NLS-1$
				.add(Restrictions.in("a.conservationArea.uuid", caFilter.getConservationAreaFilterIds())) //$NON-NLS-1$
				.setMaxResults(1)
				.uniqueResult();
	}
	
	private String getName(CategoryValueItem item, boolean full){
		String catHkey = ((CategoryValueItem) item).getCategoryHKey();
		if (catHkey == null){
			return getLabel(item.getType()) + " " + Messages.getString("SummaryItemLabelProvider.AllCategoriesLabel", l); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		try{
			Category c = getCategory(catHkey);
			if (c == null){
				return catHkey;
			}
			if (full){
				return getLabel(item.getType()) + " " + c.getFullCategoryName(); //$NON-NLS-1$
			}else{
				return getLabel(item.getType()) + " " + c.getName(); //$NON-NLS-1$
			}
		}catch (Exception ex){
			logger.log(Level.WARNING, MessageFormat.format("Category not found {0}", catHkey), ex); //$NON-NLS-1$
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
			AttributeListItem it = getAttributeListIem(itemKey, attributeKey);
			if (it == null){
				logger.log(Level.WARNING, MessageFormat.format("Attribute list item with key {0} not found for attribute {1}.", itemKey, attributeKey)); //$NON-NLS-1$
				itemName = itemKey;
			}else{
				itemName = it.getName();
			}
		}else if (att.getType() == AttributeType.TREE){
			AttributeTreeNode it = getAttributeTreeItem(itemKey, attributeKey);
			if (it == null){
				logger.log(Level.WARNING, MessageFormat.format("Attribute tree node with key {0} not found for attribute {1}.", itemKey, attributeKey)); //$NON-NLS-1$
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
//			name.append(" [" + att.getName() + "] "); //$NON-NLS-1$ //$NON-NLS-2$
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
				logger.log(Level.WARNING, MessageFormat.format("Category {0} not found.", categoryKey)); //$NON-NLS-1$
				name.append(" (" + categoryKey + " ) "); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return name.toString();
	}
	
	private String getLabel(ValueType type){
		if (type == ValueType.OBSERVATION){
			return Messages.getString("SummaryItemLabelProvider.CountObservationLabel", l); //$NON-NLS-1$
		}else if (type == ValueType.WAYPOINT){
			return Messages.getString("SummaryItemLabelProvider.CountIncidentsLabel", l); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
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
		}else if (item instanceof ObserverGroupBy){
			results = getName((ObserverGroupBy)item);
		}else if (item instanceof PatrolGroupBy){
			results = getName((PatrolGroupBy)item);
		}else if (item instanceof SamplingUnitAttributeGroupBy){
			results = getName((SamplingUnitAttributeGroupBy)item);	
		}else if (item instanceof WaypointSourceGroupBy){
			results = getName((WaypointSourceGroupBy)item);
		}else if (item instanceof IntelligencePatrolGroupBy){
			results = getName((IntelligencePatrolGroupBy)item);
		}else if (item instanceof MissionIdGroupBy){
			results = getName((MissionIdGroupBy)item);
		}else if (item instanceof SurveyIdGroupBy){
			results = getName((SurveyIdGroupBy)item);
		}else if (item instanceof SamplingUnitGroupBy){
			results = getName((SamplingUnitGroupBy)item);
		}
		return results;
	}
	
	private void sortItems(List<ListItem> items){
		Collections.sort(items, new Comparator<ListItem>() {
			Collator c = Collator.getInstance(l);				
			@Override
			public int compare(ListItem o1, ListItem o2) {
				return c.compare(o1.getName(), o2.getName());
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	private List<ListItem> getName(AreaGroupBy item){
		String[] filterkeys = item.getAreaFilterKeys();
		Area.AreaType areaType = item.getAreaType();
		
		List<ListItem> items = new ArrayList<ListItem>();
		if (filterkeys != null && filterkeys.length > 0){
			
			List<Area> matching = s
					.createCriteria(Area.class)
					.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds())) //$NON-NLS-1$
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
					.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds())) //$NON-NLS-1$
					.add(Restrictions.eq("type", areaType)).list(); //$NON-NLS-1$
			
			for (Area a : matching){
				items.add(new ListItem(null, a.getName(), a.getKeyId()));
			}
			sortItems(items);
		}
		
		return items;
	}
	
	@SuppressWarnings("unchecked")
	private List<String> getTreeNodes(String attributeKey, int level){
		Query q = s.createQuery("SELECT a.hkey FROM AttributeTreeNode a join a.attribute b " + //$NON-NLS-1$
				"WHERE b.keyId = :att AND (length(a.hkey) - length(replace(a.hkey, '.', '')))-1 = :level" + //$NON-NLS-1$
				" AND b.conservationArea.uuid IN (:cauuids) " + //$NON-NLS-1$
				" GROUP BY a.hkey HAVING count(*) = :cnt "); //$NON-NLS-1$
				
		q.setParameter("att", attributeKey); //$NON-NLS-1$
		q.setParameter("level", level); //$NON-NLS-1$
		q.setParameter("cnt", new Long(caFilter.getConservationAreaFilterIds().size())); //$NON-NLS-1$
		q.setParameterList("cauuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
		return q.list();
	}
	
	@SuppressWarnings("unchecked")
	private List<ListItem> getName(AttributeGroupBy item){
		String[]  filterHkeys = item.getFilterKeys();
		
		List<ListItem> items = new ArrayList<ListItem>();
		if (item.getAttributeType() == AttributeType.LIST){
			if (filterHkeys != null) {
				for (String key : filterHkeys){
					AttributeListItem it = getAttributeListIem(key, item.getAttributeKey());
					items.add(new ListItem(null, it.getName(), it.getKeyId()));
				}
			}else{
				//we want list items that are shared with all conservation areas
				String query = "SELECT ali.keyId FROM AttributeListItem ali join ali.attribute a WHERE a.conservationArea.uuid in (:cauuids) and a.keyId = :attributeKey group by ali.keyId HAVING count(*) = :cnt "; //$NON-NLS-1$
				org.hibernate.Query attquery = s.createQuery(query);
				attquery.setParameterList("cauuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
				attquery.setParameter("attributeKey", item.getAttributeKey()); //$NON-NLS-1$
				attquery.setParameter("cnt", new Long(caFilter.getConservationAreaFilterIds().size())); //$NON-NLS-1$
				
				//this gets the attribute name based on the requested locale name query 
				String nameQueryHql = "SELECT a.value FROM Label a, AttributeListItem c where a.id.element = c.uuid and c.keyId = :attributeKey ORDER By case when upper(a.id.language.code) = :code1 then 1 else case when upper(a.id.language.code) = :code2 then 2 else case when a.id.language.default = true then 3 else 4 end end end "; //$NON-NLS-1$
				String allLocal = l.toString().toUpperCase();
				String local = l.getLanguage().toUpperCase();
				org.hibernate.Query nameQuery = s.createQuery(nameQueryHql);
				
				List<String> listitems = attquery.list();
				for (String li: listitems){
					nameQuery.setParameter("attributeKey", li); //$NON-NLS-1$
					nameQuery.setParameter("code1", allLocal); //$NON-NLS-1$
					nameQuery.setParameter("code2", local); //$NON-NLS-1$
					nameQuery.setMaxResults(1);
					String name = (String) nameQuery.uniqueResult();
					
					items.add(new ListItem(null, name, li));
				}
				sortItems(items);
			}
		}else if (item.getAttributeType() == AttributeType.TREE){
			if (filterHkeys != null){
				for (String key : filterHkeys){
					AttributeTreeNode it = getAttributeTreeItem(key, item.getAttributeKey());
					items.add(new ListItem(null, it.getName(), it.getHkey()));
				}
			}else{
				List<String> nodes = getTreeNodes(item.getAttributeKey(), item.getTreeLevel());
				String nameQueryHql = "SELECT a.value FROM Label a, AttributeTreeNode c where a.id.element = c.uuid and c.hkey = :attributeKey ORDER By case when upper(a.id.language.code) = :code1 then 1 else case when upper(a.id.language.code) = :code2 then 2 else case when a.id.language.default = true then 3 else 4 end end end "; //$NON-NLS-1$
				String allLocal = l.toString().toUpperCase();
				String local = l.getLanguage().toUpperCase();
				org.hibernate.Query nameQuery = s.createQuery(nameQueryHql);
				for (String hkey : nodes){
					nameQuery.setParameter("attributeKey", hkey); //$NON-NLS-1$
					nameQuery.setParameter("code1", allLocal); //$NON-NLS-1$
					nameQuery.setParameter("code2", local); //$NON-NLS-1$
					nameQuery.setMaxResults(1);
					String name = (String) nameQuery.uniqueResult();
					
					items.add(new ListItem(null, name, hkey));
				}
				sortItems(items);
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
					items.add(new ListItem(null, Messages.getString("SummaryItemLabelProvider.CategoryNotFoundItemLabel", l), filterHkeys[i])); //$NON-NLS-1$
				}else{
					items.add( new ListItem(null, cat.getFullCategoryName(), cat.getHkey()) );
				}
			}
		}else{
			Query q = s.createQuery("FROM Category WHERE conservationArea.uuid in (:ca) and  (length(hkey) - length(replace(hkey, '.', ''))) - 1 = :level"); //$NON-NLS-1$
			q.setParameterList("ca", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
			q.setParameter("level", item.getTreeLevel()); //$NON-NLS-1$
			List<Category> cats = q.list();
			for(Category child : cats){
				items.add(new ListItem(null, child.getFullCategoryName(), child.getHkey()));
			}
			sortItems(items);
		}
		return items;
	}
	@SuppressWarnings("unchecked")
	private List<ListItem> getName(ConservationAreaGroupBy item){
		String[] filterHkeys = item.getFilterKeys();
		List<ListItem> items = new ArrayList<ListItem>();
		
		List<ConservationArea> cas = s.createCriteria(ConservationArea.class)
				.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
				.list();
		for (ConservationArea ca : cas){
			if (!ca.getIsCcaa()) items.add(new ListItem(ca.getUuid(), ca.getNameLabel()));
		}
		sortItems(items);
		
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
		} else if (item.getOption() instanceof StartHourGroupBy){
			return getHourItems();
		} else if (item.getOption() instanceof EndHourGroupBy){
			return getHourItems();
		}
		return null;
	}
	private List<ListItem> getDayItems(IDateFilter dateFilter) {

		ArrayList<ListItem> items = new ArrayList<ListItem>();
		Date startdate = new Date();
		Date enddate = new Date();
		if (dateFilter == null) {
			throw new IllegalStateException(Messages.getString("SummaryItemLabelProvider.InvalidDateFilter", l)); //$NON-NLS-1$
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

	public List<ListItem> getHourItems() {
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		for (int i = 0 ; i < 48; i ++){
			String key = String.valueOf(i/2);
			if (i % 2 == 0){
				key += ":00"; //$NON-NLS-1$
			}else{
				key += ":30"; //$NON-NLS-1$
			}
			items.add(new ListItem(null, key, key ));
		}
		return items;
		
	}
	private List<ListItem> getMonthItems(IDateFilter dateFilter) {

		ArrayList<ListItem> items = new ArrayList<ListItem>();
		Date startdate = new Date();
		Date enddate = new Date();
		if (dateFilter == null) {
			throw new IllegalStateException(Messages.getString("SummaryItemLabelProvider.InvalidDateFilter", l)); //$NON-NLS-1$
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
			throw new IllegalStateException(Messages.getString("SummaryItemLabelProvider.InvalidDateFilter", l)); //$NON-NLS-1$
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
		ArrayList<ListItem> items = new ArrayList<ListItem>();

		Query q = s.createQuery("SELECT et.name from EntityAttribute ea join ea.entityType et WHERE et.conservationArea.uuid in (:cauuids) and ea.keyId = :eaKey and et.keyId = :etKey"); //$NON-NLS-1$
		q.setParameterList("cauuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
		q.setString("eaKey", item.getEntityAttributeKey()); //$NON-NLS-1$
		q.setString("etKey", item.getEntityKey()); //$NON-NLS-1$
		q.setMaxResults(1);
		String entityTypeName = (String) q.uniqueResult();
		
		if (entityTypeName == null){
			logger.severe(MessageFormat.format("Entity attribute not found {0}.", item.getEntityAttributeKey())); //$NON-NLS-1$
			return items;
		}
	
		String key = item.asString();
		String attributePart = key.substring(key.indexOf("attribute:")); //$NON-NLS-1$
		AttributeGroupBy ag = AttributeGroupBy.createAttributeGroupBy(attributePart);
		List<ListItem> aitems = getName(ag);
		
		for (ListItem i : aitems){
			items.add(new ListItem(null, i.getName() + " [" + entityTypeName + "]", i.getKey())); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return items;
		
	}
	
	private List<ListItem> getName(MissionAttributeGroupBy item){
		MissionAttribute ma = (MissionAttribute) s.createCriteria(MissionAttribute.class)
				.add(Restrictions.eq("keyId", item.getAttributeKey())) //$NON-NLS-1$
				.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds())) //$NON-NLS-1$
				.uniqueResult();
		if (ma == null){
			logger.warning(MessageFormat.format("Mission attribute not found {0}.", item.getAttributeKey()));; //$NON-NLS-1$
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
			sortItems(allItems);
		}
		return allItems;
	}
	
	@SuppressWarnings("unchecked")
	private List<ListItem> getName(MissionIdGroupBy item){
		String[] items = item.getRawItems();
		List<ListItem> allItems = new ArrayList<ListItem>();
		if (items != null && items.length > 0){
			for (String it : items){
				Mission m = (Mission) s.load(Mission.class, UuidUtils.stringToUuid(it));
				if (m != null){
					allItems.add(new ListItem(m.getUuid(), m.getId()));
				}else{
					logger.warning(MessageFormat.format("Mission not found", it)); //$NON-NLS-1$
					allItems.add(new ListItem(UuidUtils.stringToUuid(it), it));
				}
			}
		}else{
			//load all missions
			Query missionQuery = null;
			if (sdFilter.getKey() != null){
				String hql = "SELECT m.uuid, m.id From Mission m where m.survey.surveyDesign.keyId = :sd and m.survey.surveyDesign.conservationArea.uuid in (:uuids)"; //$NON-NLS-1$
				missionQuery = s.createQuery(hql)
						.setString("sd", sdFilter.getKey()) //$NON-NLS-1$
						.setParameterList("uuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
			}else{
				String hql = "SELECT m.uuid, m.id From Mission m where m.survey.surveyDesign.conservationArea.uuid in (:uuids)"; //$NON-NLS-1$
				missionQuery = s.createQuery(hql)
						.setParameterList("uuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
			}
			List<Object[]> ms = missionQuery.list();
			for(Object[] m : ms){
				allItems.add(new ListItem((UUID)m[0], (String)m[1]));
			}
			sortItems(allItems);
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
				items.add(new ListItem(e.getUuid(), SmartLabelProvider.getFullName(e, l)));
			}		
		}else{
			List<Employee> es = s.createCriteria(Employee.class)
					.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds())) //$NON-NLS-1$
					.list();
			for (Employee e : es){
				items.add(new ListItem(e.getUuid(), SmartLabelProvider.getFullName(e, l)));
			}
			sortItems(items);
		}
		return items;
	}
	private List<ListItem> getName(PatrolGroupBy item){
		List<ListItem> results = new ArrayList<ListItem>();
		PatrolQueryOptionType type = item.getOption().getType();
		
		String[] keys = item.getItems();
		boolean sort = keys == null;
		
		if (type == PatrolQueryOptionType.UUID){
			Criteria c = s.createCriteria(item.getOption().getSourceClass());
			if (item.getOption() == PatrolQueryOption.STATION 
					|| item.getOption() == PatrolQueryOption.TEAM
					|| item.getOption() == PatrolQueryOption.MANDATE
					|| item.getOption() == PatrolQueryOption.PATROL_TRANSPORT_TYPE
					|| item.getOption() == PatrolQueryOption.PATROL_TYPE){
				c.add(Restrictions.eq("isActive", true)); //$NON-NLS-1$
			}
												
			if (keys != null){
				UUID[] uuidkeys = new UUID[keys.length];
				for (int i = 0; i < keys.length; i++) {
					uuidkeys[i] = UuidUtils.stringToUuid(keys[i]);
				}
				c = c.add(Restrictions.in("uuid", uuidkeys)); //$NON-NLS-1$
			}else{
				//ca filter
				if (item.getOption() == PatrolQueryOption.RANK){
					c.add(Restrictions.in("agency.conservationArea.uuid", caFilter.getConservationAreaFilterIds())); //$NON-NLS-1$
				}else if (item.getOption() == PatrolQueryOption.CONSERVATION_AREA){
					c.add(Restrictions.in("uuid", caFilter.getConservationAreaFilterIds())); //$NON-NLS-1$
				}else{
					c.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds())); //$NON-NLS-1$
				}
			}
			c.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			Collection<?> data = c.list();
			List<UUID> caUuids = caFilter.getConservationAreaFilterIds();
			for (Iterator<?> iterator = data.iterator(); iterator.hasNext();) {
				Object object = (Object) iterator.next();
				if (object instanceof NamedItem){
					results.add(new ListItem(((NamedItem) object).getUuid(), ((NamedItem) object).getName()));
				}else if (object instanceof Employee){
					Employee e = (Employee)object;
					if (caUuids.contains(e.getConservationArea().getUuid())){
						results.add(new ListItem(e.getUuid(), SmartLabelProvider.getFullName((Employee) e, l)));
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
				throw new UnsupportedOperationException(Messages.getString("SummaryItemLabelProvider.PatrolQueryOptionNotSupported", l) + item.getOption());	 //$NON-NLS-1$
			}
			List<ListItem> data = new ArrayList<ListItem>();
			Criteria c = s.createCriteria(queryClazz)
					.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds())) //$NON-NLS-1$
					.add(Restrictions.eq("isActive", true)); //$NON-NLS-1$
			
			//unique values based on keys
			HashSet<String> existingKeys = new HashSet<String>();
			for (Iterator<?> it = c.list().iterator(); it.hasNext();){
				NamedKeyItem nkitem = (NamedKeyItem)it.next();
				if (!existingKeys.contains(nkitem.getKeyId())){
					data.add(new ListItem(null, nkitem.getName(),nkitem.getKeyId()));
					existingKeys.add(nkitem.getKeyId());
				}
			}
			
			if (data != null){
				if (keys != null){
					HashSet<String> allKeys = new HashSet<>();
					for (String a : keys) allKeys.add(a);
					
					for (Iterator<?> iterator = data.iterator(); iterator.hasNext();) {
						ListItem it = (ListItem) iterator.next();
						if (allKeys.contains(it.getKey())){
							results.add(it);
						}
					}
				}else{
					results.addAll(data);
				}
			}
		}else if (type == PatrolQueryOptionType.STRING){
			if (item.getOption() == PatrolQueryOption.ID){
				Criteria c = s.createCriteria(item.getOption().getSourceClass())
						.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds())); //$NON-NLS-1$
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
		if (sort) sortItems(results);
		return results;
	}
	private List<ListItem> getName(SamplingUnitAttributeGroupBy item){
		SamplingUnitAttribute su = (SamplingUnitAttribute) s.createCriteria(SamplingUnitAttribute.class)
				.add(Restrictions.eq("keyId", item.getAttributeKey())) //$NON-NLS-1$
				.add(Restrictions.in("conservationArea.uuid", caFilter.getConservationAreaFilterIds())) //$NON-NLS-1$
				.uniqueResult();
		if (su == null){
			logger.warning(MessageFormat.format("Sampling unit attribute not found {0}.", item.getAttributeKey())); //$NON-NLS-1$
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
			sortItems(allItems);
		}
		return allItems;
	}
	@SuppressWarnings("unchecked")
	private List<ListItem> getName(SamplingUnitGroupBy item){
		String[] items = item.getRawItems();
		List<ListItem> listItems = new ArrayList<ListItem>();
		if (items != null){
			for (String it : items){
				if (it.equals(SamplingUnitFilter.NONE_KEY)){
					listItems.add(new ListItem(null, Messages.getString("SummaryItemLabelProvider.NoneSuFilterOpt", l), null)); //$NON-NLS-1$
				}else{
					SamplingUnit su = (SamplingUnit) s.get(SamplingUnit.class,
							UuidUtils.stringToUuid(it));
					if (su != null){
						listItems.add(new ListItem(su.getUuid(), su.getId()));
					}else{
						logger.warning(MessageFormat.format("SamplingUnit not found {0}", it)); //$NON-NLS-1$
						listItems.add(new ListItem(UuidUtils.stringToUuid(it), it));
					}
				}
			}
		}else{
			if (sdFilter.getKey() == null) return null;
			
			//all sampling units for associated design
			List<Object[]> sus = s.createQuery("SELECT su.uuid, su.id FROM SamplingUnit su WHERE su.surveyDesign.keyId = :keyId") //$NON-NLS-1$
					.setString("keyId", sdFilter.getKey()) //$NON-NLS-1$
					.list();
			for (Object[] su : sus){
				listItems.add(new ListItem((UUID)su[0], (String)su[1]));
			}
			sortItems(listItems);
			listItems.add(new ListItem(null, Messages.getString("SummaryItemLabelProvider.NoneSuFilterOpt", l))); //$NON-NLS-1$
			
		}
		return listItems;
	}
	
	@SuppressWarnings("unchecked")
	private List<ListItem> getName(SurveyIdGroupBy item){
		String[] items = item.getRawItems();
		List<ListItem> allItems = new ArrayList<ListItem>();
		if (items != null){
			for (String it : items){
				Survey survey = (Survey) s.load(Survey.class, UuidUtils.stringToUuid(it));
				if (survey != null){
					allItems.add(new ListItem(survey.getUuid(), survey.getId()));
				}else{
					logger.warning(MessageFormat.format("Survey not found {0}", it)); //$NON-NLS-1$
					allItems.add(new ListItem(UuidUtils.stringToUuid(it), it));
				}
			}
		}else{
			//load all surveys
			Query surveyQuery = null;
			if (sdFilter.getKey() != null){
				String hql = "SELECT s.uuid, s.id From Survey s where s.surveyDesign.keyId = :sd and s.surveyDesign.conservationArea.uuid in (:uuids)"; //$NON-NLS-1$
				surveyQuery = s.createQuery(hql)
						.setString("sd", sdFilter.getKey()) //$NON-NLS-1$
						.setParameterList("uuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
			}else{
				String hql = "SELECT s.uuid, s.id From Survey s where s.surveyDesign.conservationArea.uuid in (:uuids)"; //$NON-NLS-1$
				surveyQuery = s.createQuery(hql)
						.setParameterList("uuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
			}
			List<Object[]> ms = surveyQuery.list();
			for(Object[] m : ms){
				allItems.add(new ListItem((UUID)m[0], (String)m[1]));
			}
			sortItems(allItems);
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
			sortItems(items);
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
		items.add(new ListItem(null, Messages.getString("SummaryItemLabelProvider.MotivateIntelOp", l), IntelligencePatrolGroupBy.Options.MOTIVATED.getKey()));  //$NON-NLS-1$
		items.add(new ListItem(null, Messages.getString("SummaryItemLabelProvider.NotMotivatedIntlOp", l), IntelligencePatrolGroupBy.Options.NOT_MOTIVATED.getKey())); //$NON-NLS-1$
		return items;
	}
}
