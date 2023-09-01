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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.asset.query.parser.internal.summary.AssetGroupBy;
import org.wcs.smart.asset.query.parser.internal.summary.AssetValueItem;
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
import org.wcs.smart.dataentry.model.ConfigurableModel;
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
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.query.model.filter.WaypointSourceGroupBy;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributeListItem;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.query.model.PatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolQueryOptionType;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolAttributeGroupBy;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolGroupBy;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolValueItem;
import org.wcs.smart.plan.query.PlanPatrolGroupBy;
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
import org.wcs.smart.query.model.summary.WaypointCmGroupBy;
import org.wcs.smart.util.UuidUtils;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

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
	
	private boolean includeUuids;
	
	public SummaryItemLabelProvider(Locale l ,Session s, ConservationAreaFilter caFilter,
			boolean includeUuids,
			SurveyDesignFilter sdFilter){
		this.l = l;
		this.s = s;
		this.sdFilter = sdFilter;
		this.caFilter = caFilter;
		this.includeUuids = includeUuids;
	}
	
	public SummaryItemLabelProvider(Locale l ,Session s, ConservationAreaFilter caFilter,
			boolean includeUuids){
		this(l, s, caFilter, includeUuids, null);
	}
	
	public String getName(IValueItem item){
		if (item instanceof PatrolValueItem){
			PatrolValueItem it = (PatrolValueItem)item;
			String text = it.getPatrolValueOption().getGuiName(l);
			
			if (it.getPatrolValueOption().hasNoDataOption()) {
				if (it.includeNoData()) {
					text = text + Messages.getString("SummaryItemLabelProvider.AllDataOption", l); //$NON-NLS-1$
				}else {
					text = text +Messages.getString("SummaryItemLabelProvider.DataOnlyOption", l); //$NON-NLS-1$
				}
			}
			
			return text;
		}else if (item instanceof MissionValueItem){
			return ((MissionValueItem)item).getValueItem().getGuiName(l);
		}else if (item  instanceof CombinedValueItem){
			CombinedValueItem citem = (CombinedValueItem)item;
			return getName(citem.getPart1()) + " " + Messages.getString(PER_LABEL,l) + " " +  getName(citem.getPart2()); //$NON-NLS-1$ //$NON-NLS-2$
		}else if (item instanceof CategoryValueItem){
			return getName((CategoryValueItem)item, false);
		}else if (item instanceof  AttributeValueItem){
			return getName((AttributeValueItem)item, false);
		}else if (item instanceof AssetValueItem) {
			return ((AssetValueItem)item).getAssetValueOption().getGuiName(l);
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
		}else if (item instanceof AssetValueItem) {
			return getName(item);
		}
		return MessageFormat.format(Messages.getString("SummaryItemLabelProvider.Valuenotsupported", l), item.asString()); //$NON-NLS-1$
	}
	
	private Attribute getAttribute(String key){
		CriteriaBuilder cb = s.getCriteriaBuilder();
		CriteriaQuery<Attribute> c = cb.createQuery(Attribute.class);
		Root<Attribute> from = c.from(Attribute.class);
		c.where(cb.and(
				cb.equal(from.get("keyId"), key), //$NON-NLS-1$
				from.get("conservationArea").get("uuid").in(caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$ //$NON-NLS-2$
				));
		return s.createQuery(c).setMaxResults(1).uniqueResult();
	}
	
	private Category getCategory(String catHkey){
		CriteriaBuilder cb = s.getCriteriaBuilder();
		CriteriaQuery<Category> c = cb.createQuery(Category.class);
		Root<Category> from = c.from(Category.class);
		c.where(cb.and(
				cb.equal(from.get("hkey"), catHkey), //$NON-NLS-1$
				from.get("conservationArea").get("uuid").in(caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$ //$NON-NLS-2$
				));
		return s.createQuery(c).setMaxResults(1).uniqueResult();
	}
	
	private AttributeListItem getAttributeListIem(String key, String attributeKey){
		//find the first attribute list item with the given key for one
		//of the conservation areas; we just pick one
		CriteriaBuilder cb = s.getCriteriaBuilder();
		CriteriaQuery<AttributeListItem> c = cb.createQuery(AttributeListItem.class);
		Root<AttributeListItem> from = c.from(AttributeListItem.class);
		Join<Object, Object> attributefrom = from.join("attribute"); //$NON-NLS-1$
		c.where(cb.and(
				cb.equal(from.get("keyId"), key), //$NON-NLS-1$
				cb.equal(attributefrom.get("keyId"), attributeKey), //$NON-NLS-1$
				attributefrom.get("conservationArea").get("uuid").in(caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$ //$NON-NLS-2$
				));
		return s.createQuery(c).setMaxResults(1).uniqueResult();
	}
	
	private AttributeTreeNode getAttributeTreeItem(String hkey, String attributeKey){
		//find the first attribute list item with the given key for one
		//of the conservation areas; we just pick one
		CriteriaBuilder cb = s.getCriteriaBuilder();
		CriteriaQuery<AttributeTreeNode> c = cb.createQuery(AttributeTreeNode.class);
		Root<AttributeTreeNode> from = c.from(AttributeTreeNode.class);
		Join<Object, Object> attributefrom = from.join("attribute"); //$NON-NLS-1$
		c.where(cb.and(
				cb.equal(from.get("hkey"), hkey), //$NON-NLS-1$
				cb.equal(attributefrom.get("keyId"), attributeKey), //$NON-NLS-1$
				attributefrom.get("conservationArea").get("uuid").in(caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$ //$NON-NLS-2$
				));
		return s.createQuery(c).setMaxResults(1).uniqueResult();
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
		}else if (item instanceof PatrolAttributeGroupBy){
			results = getName((PatrolAttributeGroupBy)item);
		}else if (item instanceof SamplingUnitAttributeGroupBy){
			results = getName((SamplingUnitAttributeGroupBy)item);	
		}else if (item instanceof WaypointSourceGroupBy){
			results = getName((WaypointSourceGroupBy)item);
		}else if (item instanceof PlanPatrolGroupBy) {
			results = getName((PlanPatrolGroupBy)item);
		}else if (item instanceof MissionIdGroupBy){
			results = getName((MissionIdGroupBy)item);
		}else if (item instanceof SurveyIdGroupBy){
			results = getName((SurveyIdGroupBy)item);
		}else if (item instanceof SamplingUnitGroupBy){
			results = getName((SamplingUnitGroupBy)item);
		}else if (item instanceof AssetGroupBy){
			results = getName((AssetGroupBy)item);
		}else if (item instanceof WaypointCmGroupBy) {
			results = getName((WaypointCmGroupBy)item);

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
	
	private List<ListItem> getName(AreaGroupBy item){
		String[] filterkeys = item.getAreaFilterKeys();
		Area.AreaType areaType = item.getAreaType();
		
		CriteriaBuilder cb = s.getCriteriaBuilder();
		List<ListItem> items = new ArrayList<ListItem>();
		if (filterkeys != null && filterkeys.length > 0){
			List<String> filterItems = new ArrayList<>();
			for (String f : filterkeys) filterItems.add(f);
			
			CriteriaQuery<Area> c = cb.createQuery(Area.class);
			Root<Area> from = c.from(Area.class);
			c.where(cb.and(
					from.get("conservationArea").get("uuid").in(caFilter.getConservationAreaFilterIds()), //$NON-NLS-1$ //$NON-NLS-2$
					from.get("keyId").in(filterItems), //$NON-NLS-1$
					cb.equal(from.get("type"), areaType) //$NON-NLS-1$
					));
			List<Area> matching = s.createQuery(c).list();
			
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
			CriteriaQuery<Area> c = cb.createQuery(Area.class);
			Root<Area> from = c.from(Area.class);
			c.where(cb.and(
					from.get("conservationArea").get("uuid").in(caFilter.getConservationAreaFilterIds()), //$NON-NLS-1$ //$NON-NLS-2$
					cb.equal(from.get("type"), areaType) //$NON-NLS-1$
					));
			List<Area> matching = s.createQuery(c).list();
			for (Area a : matching){
				items.add(new ListItem(null, a.getName(), a.getKeyId()));
			}
		}
		sortItems(items);
		return items;
	}
	
	private List<String> getTreeNodes(String attributeKey, int level){
		Query<String> q = s.createQuery("SELECT a.hkey FROM AttributeTreeNode a join a.attribute b " + //$NON-NLS-1$
				"WHERE b.keyId = :att AND (length(a.hkey) - length(replace(a.hkey, '.', '')))-1 = :level" + //$NON-NLS-1$
				" AND b.conservationArea.uuid IN (:cauuids) " + //$NON-NLS-1$
				" GROUP BY a.hkey HAVING count(*) = :cnt ", String.class); //$NON-NLS-1$
				
		q.setParameter("att", attributeKey); //$NON-NLS-1$
		q.setParameter("level", level); //$NON-NLS-1$
		q.setParameter("cnt", Long.valueOf(caFilter.getConservationAreaFilterIds().size())); //$NON-NLS-1$
		q.setParameterList("cauuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
		return q.list();
	}
	
	private List<ListItem> getName(AttributeGroupBy item){
		String[]  filterHkeys = item.getFilterKeys();
		
		List<ListItem> items = new ArrayList<ListItem>();
		if (item.getAttributeType().isList()){
			if (filterHkeys != null) {
				for (String key : filterHkeys){
					AttributeListItem it = getAttributeListIem(key, item.getAttributeKey());
					items.add(new ListItem(null, it.getName(), it.getKeyId()));
				}
			}else{
				//we want list items that are shared with all conservation areas
				String query = "SELECT ali.keyId FROM AttributeListItem ali join ali.attribute a WHERE a.conservationArea.uuid in (:cauuids) and a.keyId = :attributeKey group by ali.keyId HAVING count(*) = :cnt "; //$NON-NLS-1$
				Query<String> attquery = s.createQuery(query, String.class);
				attquery.setParameterList("cauuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
				attquery.setParameter("attributeKey", item.getAttributeKey()); //$NON-NLS-1$
				attquery.setParameter("cnt", Long.valueOf(caFilter.getConservationAreaFilterIds().size())); //$NON-NLS-1$
				
				//this gets the attribute name based on the requested locale name query 
				String nameQueryHql = "SELECT a.value FROM Label a, AttributeListItem c where a.id.element.uuid = c.uuid and c.keyId = :attributeKey ORDER By case when upper(a.id.language.code) = upper(:code1) then 1 else case when upper(a.id.language.code) = upper(:code2) then 2 else case when a.id.language.default = true then 3 else 4 end end end "; //$NON-NLS-1$
				String allLocal = l.toString();
				String local = l.getLanguage();
				Query<String> nameQuery = s.createQuery(nameQueryHql, String.class);
				
				List<String> listitems = attquery.list();
				for (String li: listitems){
					nameQuery.setParameter("attributeKey", li); //$NON-NLS-1$
					nameQuery.setParameter("code1", allLocal); //$NON-NLS-1$
					nameQuery.setParameter("code2", local); //$NON-NLS-1$
					nameQuery.setMaxResults(1);
					String name = nameQuery.uniqueResult();
					
					items.add(new ListItem(null, name, li));
				}
			}
		}else if (item.getAttributeType() == AttributeType.TREE){
			if (filterHkeys != null){
				for (String key : filterHkeys){
					AttributeTreeNode it = getAttributeTreeItem(key, item.getAttributeKey());
					items.add(new ListItem(null, it.getName(), it.getHkey()));
				}
			}else{
				List<String> nodes = getTreeNodes(item.getAttributeKey(), item.getTreeLevel());
				String nameQueryHql = "SELECT a.value FROM Label a, AttributeTreeNode c where a.id.element.uuid = c.uuid and c.hkey = :attributeKey ORDER By case when upper(a.id.language.code) = upper(:code1) then 1 else case when upper(a.id.language.code) = upper(:code2) then 2 else case when a.id.language.default = true then 3 else 4 end end end "; //$NON-NLS-1$
				String allLocal = l.toString();
				String local = l.getLanguage();
				org.hibernate.query.Query<String> nameQuery = s.createQuery(nameQueryHql, String.class);
				for (String hkey : nodes){
					nameQuery.setParameter("attributeKey", hkey); //$NON-NLS-1$
					nameQuery.setParameter("code1", allLocal); //$NON-NLS-1$
					nameQuery.setParameter("code2", local); //$NON-NLS-1$
					nameQuery.setMaxResults(1);
					String name = nameQuery.uniqueResult();
					
					items.add(new ListItem(null, name, hkey));
				}
				
			}
		}
		sortItems(items);
		return items;
	}
	
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
			Query<Category> q = s.createQuery("FROM Category WHERE conservationArea.uuid in (:ca) and  (length(hkey) - length(replace(hkey, '.', ''))) - 1 = :level", Category.class); //$NON-NLS-1$
			q.setParameterList("ca", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
			q.setParameter("level", item.getTreeLevel()); //$NON-NLS-1$
			List<Category> cats = q.list();
			for(Category child : cats){
				items.add(new ListItem(null, child.getFullCategoryName(), child.getHkey()));
			}
		}
		sortItems(items);
		return items;
	}

	private List<ListItem> getName(ConservationAreaGroupBy item){
		String[] filterHkeys = item.getFilterKeys();
		List<ListItem> items = new ArrayList<ListItem>();
		
		List<ConservationArea> cas = QueryFactory.buildQuery(s, ConservationArea.class).list();
		for (ConservationArea ca : cas){
			if (!ca.getIsCcaa()) {
				if (includeUuids) {
					items.add(new ListItem(ca.getUuid(), UuidUtils.uuidToString(ca.getUuid())));
				}else {
					items.add(new ListItem(ca.getUuid(), ca.getNameLabel()));
				}
			}
		}
		
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
		sortItems(items);
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
		LocalDate startdate = LocalDate.now();
		LocalDate enddate = LocalDate.now();
		if (dateFilter == null) {
			throw new IllegalStateException(Messages.getString("SummaryItemLabelProvider.InvalidDateFilter", l)); //$NON-NLS-1$
		} else {
			if (dateFilter.getDates() == null) {
				// all daytes
				String hql = "SELECT min(dateTime) from Waypoint WHERE conservationArea.uuid IN (:ca)"; //$NON-NLS-1$
				Query<LocalDateTime> q = s.createQuery(hql, LocalDateTime.class);
				q.setParameterList("ca", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$

				List<LocalDateTime> data = q.list();
				if (data != null && data.size() >= 1 && data.get(0) != null) {
					startdate =  data.get(0).toLocalDate();
				}
			} else {
				LocalDate[] d = dateFilter.getDates();
				if (d.length >= 1) {
					startdate = d[0];
				}
				if (d.length >= 2) {
					enddate = d[1];
				}
			}
		}


		LocalDate cals = startdate;
		while (cals.isBefore(enddate)
				|| (dateFilter.isEndDateInclusive() && cals.isEqual(enddate))) {
			String key = DateTimeFormatter.ISO_LOCAL_DATE.format(cals);
			items.add(new ListItem(null, key, key));
			
			cals = cals.plusDays(1);
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
		LocalDate startdate = LocalDate.now();
		LocalDate enddate = LocalDate.now();
		if (dateFilter == null) {
			throw new IllegalStateException(Messages.getString("SummaryItemLabelProvider.InvalidDateFilter", l)); //$NON-NLS-1$
		} else {
			if (dateFilter.getDates() == null) {
				// all daytes
				String hql = "SELECT min(dateTime) from Waypoint WHERE conservationArea.uuid IN (:ca)"; //$NON-NLS-1$
				Query<LocalDateTime> q = s.createQuery(hql, LocalDateTime.class);
				q.setParameterList("ca", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$

				List<LocalDateTime> data = q.list();
				if (data != null && data.size() >= 1 && data.get(0) != null) {
					startdate = data.get(0).toLocalDate();
				}
			} else {
				LocalDate[] d = dateFilter.getDates();
				if (d.length >= 1) {
					startdate = d[0];
				}
				if (d.length >= 2) {
					enddate = d[1];
				}
			}
		}
		// each month between start and end of
		// form "m/yyyy"
		DateTimeFormatter nameFormat = DateTimeFormatter.ofPattern("MM/yyyy"); //$NON-NLS-1$
		DateTimeFormatter keyFormat = DateTimeFormatter.ofPattern("M/yyyy", Locale.ENGLISH); //$NON-NLS-1$

		LocalDate s = LocalDate.of(startdate.getYear(),  startdate.getMonth(), 1);
		LocalDate e = LocalDate.of(enddate.getYear(), enddate.getMonth(), YearMonth.from(enddate).atEndOfMonth().getDayOfMonth());
		
		while (s.isBefore(e)) {
			String key = keyFormat.format(s);
			String name = nameFormat.format(s);
			items.add(new ListItem(null, name, key));
			s = s.plusMonths(1);
		}
		return items;
	}

	private List<ListItem> getYearItems(IDateFilter dateFilter) {

		ArrayList<ListItem> items = new ArrayList<ListItem>();
		LocalDate startdate = LocalDate.now();
		LocalDate enddate = LocalDate.now();
		if (dateFilter == null) {
			throw new IllegalStateException(Messages.getString("SummaryItemLabelProvider.InvalidDateFilter", l)); //$NON-NLS-1$
		} else {
			if (dateFilter.getDates() == null) {
				// all daytes
				String hql = "SELECT min(dateTime) from Waypoint WHERE conservationArea.uuid IN (:ca)"; //$NON-NLS-1$
				Query<LocalDateTime> q = s.createQuery(hql, LocalDateTime.class);
				q.setParameterList("ca", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$

				List<LocalDateTime> data = q.list();
				if (data != null && data.size() >= 1 && data.get(0) != null) {
					startdate = data.get(0).toLocalDate();
				}
			} else {
				LocalDate[] d = dateFilter.getDates();
				if (d.length >= 1) {
					startdate = d[0];
				}
				if (d.length >= 2) {
					enddate = d[1];
				}
			}
		}
		// each year in form yyyy
		int year = startdate.getYear();
		int year2 = enddate.getYear();
		while (year <= year2) {
			items.add(new ListItem(null, String.valueOf(year), String.valueOf(year)));
			year++;
		}
		return items;
	}
	
	private List<ListItem> getName(EntityAttributeGroupBy item){
		ArrayList<ListItem> items = new ArrayList<ListItem>();

		Query<String> q = s.createQuery("SELECT et.name from EntityAttribute ea join ea.entityType et WHERE et.conservationArea.uuid in (:cauuids) and ea.keyId = :eaKey and et.keyId = :etKey", String.class); //$NON-NLS-1$
		q.setParameterList("cauuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
		q.setParameter("eaKey", item.getEntityAttributeKey()); //$NON-NLS-1$
		q.setParameter("etKey", item.getEntityKey()); //$NON-NLS-1$
		q.setMaxResults(1);
		String entityTypeName = q.uniqueResult();
		
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
		sortItems(items);
		return items;
		
	}
	
	private List<ListItem> getName(MissionAttributeGroupBy item){
		CriteriaBuilder cb = s.getCriteriaBuilder();
		CriteriaQuery<MissionAttribute> c = cb.createQuery(MissionAttribute.class);
		Root<MissionAttribute> from = c.from(MissionAttribute.class);
		c.where(cb.and(
				cb.equal(from.get("keyId"), item.getAttributeKey()), //$NON-NLS-1$
				from.get("conservationArea").get("uuid").in(caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$ //$NON-NLS-2$
				));
		MissionAttribute ma = s.createQuery(c).uniqueResult();
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
		}
		sortItems(allItems);
		return allItems;
	}
	
	private List<ListItem> getName(MissionIdGroupBy item){
		String[] items = item.getRawItems();
		List<ListItem> allItems = new ArrayList<ListItem>();
		if (items != null && items.length > 0){
			for (String it : items){
				Mission m = (Mission) s.get(Mission.class, UuidUtils.stringToUuid(it));
				if (m != null){
					allItems.add(new ListItem(m.getUuid(), m.getId()));
				}else{
					logger.warning(MessageFormat.format("Mission not found", it)); //$NON-NLS-1$
					allItems.add(new ListItem(UuidUtils.stringToUuid(it), it));
				}
			}
		}else{
			//load all missions
			Query<Tuple> missionQuery = null;
			if (sdFilter.getKey() != null){
				String hql = "SELECT m.uuid, m.id From Mission m where m.survey.surveyDesign.keyId = :sd and m.survey.surveyDesign.conservationArea.uuid in (:uuids)"; //$NON-NLS-1$
				missionQuery = s.createQuery(hql, Tuple.class)
						.setParameter("sd", sdFilter.getKey()) //$NON-NLS-1$
						.setParameterList("uuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
			}else{
				String hql = "SELECT m.uuid, m.id From Mission m where m.survey.surveyDesign.conservationArea.uuid in (:uuids)"; //$NON-NLS-1$
				missionQuery = s.createQuery(hql, Tuple.class)
						.setParameterList("uuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
			}
			List<Tuple> ms = missionQuery.list();
			for(Tuple m : ms){
				allItems.add(new ListItem((UUID)m.get(0), (String)m.get(1)));
			}
		}
		sortItems(allItems);
		return allItems;
	}
	
	private List<ListItem> getName(ObserverGroupBy item){
		String[] filterkeys = item.getFilterKeys();
		List<ListItem> items = new ArrayList<ListItem>();
		if (filterkeys != null && filterkeys.length > 0){
			for (String uuid : filterkeys){
				Employee e = (Employee) s.get(Employee.class, UuidUtils.stringToUuid(uuid));
				items.add(new ListItem(e.getUuid(), SmartLabelProvider.getFullName(e, l)));
			}		
		}else{
			CriteriaBuilder cb = s.getCriteriaBuilder();
			CriteriaQuery<Employee> c = cb.createQuery(Employee.class);
			Root<Employee> from = c.from(Employee.class);
			c.where(cb.and(
					from.get("conservationArea").get("uuid").in(caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$ //$NON-NLS-2$
					));
			List<Employee> es = s.createQuery(c).list();
			
			for (Employee e : es){
				items.add(new ListItem(e.getUuid(), SmartLabelProvider.getFullName(e, l)));
			}
		}
		sortItems(items);
		return items;
	}
	
	
	private List<ListItem> getName(PatrolGroupBy item){
		List<ListItem> results = new ArrayList<ListItem>();
		PatrolQueryOptionType type = item.getOption().getType();
		
		String[] keys = item.getItems();
		
		CriteriaBuilder cb = s.getCriteriaBuilder();
		if (type == PatrolQueryOptionType.UUID){
			
			CriteriaQuery<?> c = cb.createQuery(item.getOption().getSourceClass());
			Root<?> from = c.from(item.getOption().getSourceClass());
			List<Predicate> filters = new ArrayList<>();
			
			if (item.getOption() == PatrolQueryOption.STATION 
					|| item.getOption() == PatrolQueryOption.TEAM
					|| item.getOption() == PatrolQueryOption.MANDATE
					|| item.getOption() == PatrolQueryOption.PATROL_TRANSPORT_TYPE
					|| item.getOption() == PatrolQueryOption.PATROL_TYPE){
				filters.add(cb.equal(from.get("isActive"), true)); //$NON-NLS-1$
			}
			
			int addnull = -1;
			if (keys != null){
				List<UUID> uuidkeys = new ArrayList<>(keys.length);
				for (int i = 0; i < keys.length; i++) {
					if (keys[i].equals(IFilter.NULL_OP)) {
						addnull = i;
					}else {
						uuidkeys.add(UuidUtils.stringToUuid(keys[i]));
					}
				}
				filters.add(from.get("uuid").in(uuidkeys)); //$NON-NLS-1$
			}else{
				//ca filter
				if (item.getOption() == PatrolQueryOption.RANK){		
					filters.add(from.join("agency").get("conservationArea").get("uuid").in(caFilter.getConservationAreaFilterIds()));					 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}else if (item.getOption() == PatrolQueryOption.CONSERVATION_AREA){
					filters.add(from.get("uuid").in(caFilter.getConservationAreaFilterIds())); //$NON-NLS-1$
				}else{
					filters.add(from.get("conservationArea").get("uuid").in(caFilter.getConservationAreaFilterIds())); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
//			c.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			c.where(cb.and(filters.toArray(new Predicate[filters.size()])));
			Collection<?> data = s.createQuery(c).list();
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
					if (includeUuids) {
						results.add(new ListItem(ca.getUuid(), UuidUtils.uuidToString(ca.getUuid())));
					}else {
						results.add(new ListItem(ca.getUuid(), ca.getNameLabel()));
					}
				}else if (object instanceof ListItem){
					results.add((ListItem)object);
				}
			}
			if (addnull >=0 ) {
				String name = item.getOption().getName(s, null, l);
				results.add(addnull, new ListItem(null, name, IFilter.NULL_OP));
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
			
			CriteriaQuery<?> c = cb.createQuery(queryClazz);
			Root<?> from = c.from(queryClazz);
			c.where(cb.and(
					from.get("conservationArea").get("uuid").in(caFilter.getConservationAreaFilterIds()), //$NON-NLS-1$ //$NON-NLS-2$
					cb.equal(from.get("isActive"), true) //$NON-NLS-1$
					));
			
			//unique values based on keys
			List<ListItem> data = new ArrayList<>();
			HashSet<String> existingKeys = new HashSet<String>();
			for (Iterator<?> it =s.createQuery(c).list().iterator(); it.hasNext();){
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
				
				CriteriaQuery<?> c = cb.createQuery(item.getOption().getSourceClass());
				Root<?> from = c.from(item.getOption().getSourceClass());
				Predicate[] filters = new Predicate[keys != null ? 2 : 1];
				filters[0] = from.get("conservationArea").get("uuid").in(caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$ //$NON-NLS-2$
				if (keys != null){
					List<String> keyFilters = new ArrayList<>();
					for (String k : keys) keyFilters.add(k);
					filters[1] = from.get(item.getOption().getColumnName()).in(keyFilters);
				}
				c.where(cb.and(filters));
				
				 List<?> data = s.createQuery(c).list();
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
		}else if (type == PatrolQueryOptionType.BOOLEAN){
			if (keys == null) {
				results.add(new ListItem(null, SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.TRUE, l), Boolean.TRUE.toString()));
				results.add(new ListItem(null, SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE, l), Boolean.FALSE.toString()));
			}else {
				for (String k : keys) {
					if (k.equalsIgnoreCase(Boolean.TRUE.toString())) results.add(new ListItem(null, SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.TRUE, l), Boolean.TRUE.toString()));
					if (k.equalsIgnoreCase(Boolean.FALSE.toString())) results.add(new ListItem(null, SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE, l), Boolean.FALSE.toString()));
				}
			}	
		}
		sortItems(results);
		return results;
	}
	
	private List<ListItem> getName(PatrolAttributeGroupBy item){
		List<ListItem> results = new ArrayList<ListItem>();
		
		Set<String> keys = null;
		if (item.getItems() != null) {
			keys = new HashSet<>();
			for (String i : item.getItems()) keys.add(i);
		}
		
		List<UUID> caUuids = caFilter.getConservationAreaFilterIds();
		if (caUuids.size() == 1) {

			//single ca get patrol attribute and return list items
			PatrolAttribute pa = QueryFactory.buildQuery(s, PatrolAttribute.class,
					new Object[] {"keyId", item.getAttributeKey()}, //$NON-NLS-1$
					new Object[] {"conservationArea.uuid", caUuids.get(0)}).uniqueResult(); //$NON-NLS-1$
			
			if (pa == null || pa.getType() != AttributeType.LIST) return results;
			

			
			for (PatrolAttributeListItem li : pa.getAttributeList()) {
				if (keys == null || keys.contains(li.getKeyId())) {
					results.add(new ListItem(null, li.getName(), li.getKeyId()));
				}	
			}
		}else {
			//merge together items
			
			List<PatrolAttribute> pas = s.createQuery("FROM PatrolAttribute WHERE conservationArea.uuid in (:cas) and keyId = :keyid", PatrolAttribute.class) //$NON-NLS-1$
					.setParameterList("cas", caUuids) //$NON-NLS-1$
					.setParameter("keyid", item.getAttributeKey()) //$NON-NLS-1$
					.list();
			if (pas.isEmpty()) return results;
			
			for (PatrolAttribute pa : pas) if (pa.getType() != AttributeType.LIST) return results;
			
			HashMap<String, ListItem> items = new HashMap<>();
			for (PatrolAttribute pa : pas) {
				for (PatrolAttributeListItem li : pa.getAttributeList()) {
					if ( (keys == null || keys.contains(li.getKeyId())) &&  !items.containsKey(li.getKeyId())) {
						ListItem litem = new ListItem(null, li.getKeyId(), li.getName());
						items.put(li.getKeyId(), litem);
					}
				}
			}
			results.addAll(items.values());
		}
		sortItems(results);
		return results;
	}
	
	private List<ListItem> getName(AssetGroupBy item){
		List<ListItem> results = new ArrayList<ListItem>();
		AssetFilterOption type = item.getOption();
		
		String[] keys = item.getItems();
		Set<UUID> selectedItems = null; 
		if (keys != null){
			selectedItems = new HashSet<>();
			for (String key : keys) {
				selectedItems.add(UuidUtils.stringToUuid(key));
			}
		}
		
		if (type == AssetFilterOption.ASSET) {
			List<Asset> assets = null;
			if (selectedItems == null) {
				assets = s.createQuery("FROM Asset WHERE conservationArea.uuid in (:cas)", Asset.class) //$NON-NLS-1$
					.setParameterList("cas", caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$
					.list();
			}else {
				assets = s.createQuery("FROM Asset WHERE uuid in (:uuids) and conservationArea.uuid in (:cas)", Asset.class) //$NON-NLS-1$
						.setParameterList("cas", caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$
						.setParameterList("uuids", selectedItems) //$NON-NLS-1$
						.list();
			}
			assets.sort((a,b)->a.getId().compareTo(b.getId()));
			for (Asset a : assets) {
				results.add(new ListItem(a.getUuid(), a.getId(), UuidUtils.uuidToString(a.getUuid())));
			}
			
		}else if (type == AssetFilterOption.ASSETTYPE) {
			List<AssetType> assets = null;
			if (selectedItems == null) {
				assets = s.createQuery("FROM AssetType WHERE conservationArea.uuid in (:cas)", AssetType.class) //$NON-NLS-1$
					.setParameterList("cas", caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$
					.list();
			}else {
				assets = s.createQuery("FROM AssetType WHERE uuid in (:uuids) and conservationArea.uuid in (:cas)", AssetType.class) //$NON-NLS-1$
						.setParameterList("cas", caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$
						.setParameterList("uuids", selectedItems) //$NON-NLS-1$
						.list();
			}
			assets.sort((a,b)->a.getKeyId().compareTo(b.getKeyId()));
			for (AssetType a : assets) {
				results.add(new ListItem(a.getUuid(), a.getName(), UuidUtils.uuidToString(a.getUuid())));
			}
			
		}else if (type == AssetFilterOption.STATION) {
			List<AssetStation> stations = null;
			if (selectedItems == null) {
				stations = s.createQuery("FROM AssetStation WHERE conservationArea.uuid in (:cas)", AssetStation.class) //$NON-NLS-1$
					.setParameterList("cas", caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$
					.list();
			}else {
				stations = s.createQuery("FROM AssetStation WHERE uuid in (:uuids) and conservationArea.uuid in (:cas)", AssetStation.class) //$NON-NLS-1$
						.setParameterList("cas", caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$
						.setParameterList("uuids", selectedItems) //$NON-NLS-1$
						.list();
			}
			stations.sort((a,b)->a.getId().compareTo(b.getId()));
			for (AssetStation a : stations) {
				results.add(new ListItem(a.getUuid(), a.getId(), UuidUtils.uuidToString(a.getUuid())));
			}

		}else if (type == AssetFilterOption.STATIONLOCATION) {
			List<AssetStationLocation> locations = null;
			if (selectedItems == null) {
				locations = s.createQuery("SELECT l FROM AssetStationLocation l join l.station s WHERE s.conservationArea.uuid in (:cas)", AssetStationLocation.class) //$NON-NLS-1$
					.setParameterList("cas", caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$
					.list();
			}else {
				locations = s.createQuery("SELECT l FROM AssetStationLocation l join l.station s WHERE l.uuid in (:uuids) and s.conservationArea.uuid in (:cas)", AssetStationLocation.class) //$NON-NLS-1$
						.setParameterList("cas", caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$
						.setParameterList("uuids", selectedItems) //$NON-NLS-1$
						.list();
			}
			locations.sort((a,b)->a.getId().compareTo(b.getId()));
			for (AssetStationLocation a : locations) {
				results.add(new ListItem(a.getUuid(), a.getId(), UuidUtils.uuidToString(a.getUuid())));
			}
			
		}
		sortItems(results);
		return results;
	}
	
	private List<ListItem> getName(SamplingUnitAttributeGroupBy item){
		CriteriaBuilder cb = s.getCriteriaBuilder();
		CriteriaQuery<SamplingUnitAttribute> c = cb.createQuery(SamplingUnitAttribute.class);
		Root<SamplingUnitAttribute> from = c.from(SamplingUnitAttribute.class);
		c.where(cb.and(
				cb.equal(from.get("keyId"), item.getAttributeKey()), //$NON-NLS-1$
				from.get("conservationArea").get("uuid").in(caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$ //$NON-NLS-2$
				));
		SamplingUnitAttribute su = s.createQuery(c).uniqueResult();
		
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
		}
		sortItems(allItems);
		return allItems;
	}
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
			List<Tuple> sus = s.createQuery("SELECT su.uuid, su.id FROM SamplingUnit su WHERE su.surveyDesign.keyId = :keyId", Tuple.class) //$NON-NLS-1$
					.setParameter("keyId", sdFilter.getKey()) //$NON-NLS-1$
					.list();
			for (Tuple su : sus){
				listItems.add(new ListItem((UUID)su.get(0), (String)su.get(1)));
			}
			listItems.add(new ListItem(null, Messages.getString("SummaryItemLabelProvider.NoneSuFilterOpt", l))); //$NON-NLS-1$
			
		}
		sortItems(listItems);
		return listItems;
	}
	
	private List<ListItem> getName(SurveyIdGroupBy item){
		String[] items = item.getRawItems();
		List<ListItem> allItems = new ArrayList<ListItem>();
		if (items != null){
			for (String it : items){
				Survey survey = (Survey) s.get(Survey.class, UuidUtils.stringToUuid(it));
				if (survey != null){
					allItems.add(new ListItem(survey.getUuid(), survey.getId()));
				}else{
					logger.warning(MessageFormat.format("Survey not found {0}", it)); //$NON-NLS-1$
					allItems.add(new ListItem(UuidUtils.stringToUuid(it), it));
				}
			}
		}else{
			//load all surveys
			Query<Tuple> surveyQuery = null;
			if (sdFilter.getKey() != null){
				String hql = "SELECT s.uuid, s.id From Survey s where s.surveyDesign.keyId = :sd and s.surveyDesign.conservationArea.uuid in (:uuids)"; //$NON-NLS-1$
				surveyQuery = s.createQuery(hql, Tuple.class)
						.setParameter("sd", sdFilter.getKey()) //$NON-NLS-1$
						.setParameterList("uuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
			}else{
				String hql = "SELECT s.uuid, s.id From Survey s where s.surveyDesign.conservationArea.uuid in (:uuids)"; //$NON-NLS-1$
				surveyQuery = s.createQuery(hql, Tuple.class)
						.setParameterList("uuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
			}
			List<Tuple> ms = surveyQuery.list();
			for(Tuple m : ms){
				allItems.add(new ListItem((UUID)m.get(0), (String)m.get(1)));
			}
		}
		sortItems(allItems);
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
		sortItems(items);
		return items;
	}
	
	private List<ListItem> getName(WaypointCmGroupBy item){
		String[] keys = item.getKeys();
		
		List<ListItem> items = new ArrayList<ListItem>();
		boolean addnone = false;
		if (keys == null){
			addnone = true;
			
			List<ConfigurableModel> models = s.createQuery("FROM ConfigurableModel WHERE conservationArea.uuid in (:cas)", ConfigurableModel.class) //$NON-NLS-1$
					.setParameterList("cas", caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$
					.list();
			for (ConfigurableModel m : models) items.add(new ListItem(m.getUuid(), m.getName()));
		}else{
			for (String k : keys){
				if (k.equals(IFilter.NULL_OP)) {
					addnone = true;
				}else {
					ConfigurableModel cm = s.get(ConfigurableModel.class, UuidUtils.stringToUuid(k));
					if (cm != null && caFilter.getConservationAreaFilterIds().contains(cm.getConservationArea().getUuid())) {
						items.add(new ListItem(cm.getUuid(), cm.getName()));
					}
				}
			}
		}
		sortItems(items);
		if (addnone)
			items.add(0, new ListItem(null, SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(IFilter.NULL_OP, this.l), IFilter.NULL_OP));
		return items;
	}
	
	private List<ListItem> getName(PlanPatrolGroupBy item){
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		items.add(new ListItem(null, Messages.getString("SummaryItemLabelProvider.PartOfPlanHeader", l), PlanPatrolGroupBy.Options.PARTOF.getKey())); //$NON-NLS-1$
		items.add(new ListItem(null, Messages.getString("SummaryItemLabelProvider.NotPartOfPlanHeader", l), PlanPatrolGroupBy.Options.NOT_PARTOF.getKey()));  //$NON-NLS-1$
		return items;
	}
}
