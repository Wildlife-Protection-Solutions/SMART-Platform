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
package org.wcs.smart.query.ui.model.impl;

import java.text.MessageFormat;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.AreaFilter;
import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.BooleanExpression;
import org.wcs.smart.query.model.filter.BracketFilter;
import org.wcs.smart.query.model.filter.CategoryAttributeFilter;
import org.wcs.smart.query.model.filter.CategoryFilter;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilter.FilterType;
import org.wcs.smart.query.model.filter.NotExpression;
import org.wcs.smart.query.model.filter.ObserverFilter;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.model.filter.date.DateGroupByViewer;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;
import org.wcs.smart.query.model.summary.AreaGroupBy;
import org.wcs.smart.query.model.summary.AreaGroupByViewer;
import org.wcs.smart.query.model.summary.AttributeGroupBy;
import org.wcs.smart.query.model.summary.AttributeGroupByViewer;
import org.wcs.smart.query.model.summary.AttributeValueItem;
import org.wcs.smart.query.model.summary.CategoryGroupBy;
import org.wcs.smart.query.model.summary.CategoryGroupByViewer;
import org.wcs.smart.query.model.summary.CategoryValueItem;
import org.wcs.smart.query.model.summary.CombinedValueItem;
import org.wcs.smart.query.model.summary.ConservationAreaGroupBy;
import org.wcs.smart.query.model.summary.ConservationAreaGroupByViewer;
import org.wcs.smart.query.model.summary.DateGroupBy;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IGroupByViewer;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.ObserverGroupBy;
import org.wcs.smart.query.model.summary.ObserverGroupByViewer;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDropItemFactory;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.BracketDropItem.BracketType;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

public class BasicDropItemFactory implements IDropItemFactory{

	public static ListItem ANY_OPTION = new ListItem(null, Messages.AttributeFilter_AnyListItemOption, AttributeFilter.ANY_OPTION_KEY);
	
	public static BasicDropItemFactory INSTANCE = new BasicDropItemFactory();
	
	protected BasicDropItemFactory(){
		
	}
	
	public String getFilterTypeName(IFilter.FilterType type){
		if (type == FilterType.WAYPOINT){
			return Messages.IFilter_IncidentFilterName;
		}else if (type == FilterType.OBSERVATION){
			return Messages.IFilter_ObservationFilterName;
		}
		throw new IllegalStateException("Invalid filter type."); //$NON-NLS-1$
	}
	
	
	public DropItem createConservationAreaGroupByDropItem(){
		return new ConservationAreaGroupByDropItem();
	}
	
	
	public static DropItem createBooleanOpDropItem(){
		BooleanOpDropItem op = new BooleanOpDropItem();
		return op;
	}
	
	public static DropItem[] createBracketIems(){
		BracketDropItem open = new BracketDropItem(BracketType.OPEN);
		BracketDropItem close = new BracketDropItem(BracketType.CLOSE);
		
		return new DropItem[]{open, close};
	}
	
	public static DropItem createErrorDropItem(String errorMessage){
		return new ErrorDropItem(errorMessage);
	}
	public static DropItem createNotDropItem(){
		return new NotDropItem();
	}
	
	public DropItem createObserverDropItem(){
		return new ObserverDropItem();
	}
	
	public DropItem createObserverGroupByDropItem(){
		return new ObserverGroupByDropItem();
	}
	
	public DropItem createCategoryDropItem(Category c){
		return new CategoryDropItem(c);
	}
	
	public DropItem createAttributeDropItem(CategoryAttribute ca){
		if (ca.getAttribute().getType() == AttributeType.BOOLEAN || 
				ca.getAttribute().getType() == AttributeType.NUMERIC ||
				ca.getAttribute().getType() == AttributeType.TEXT ||
				ca.getAttribute().getType() == AttributeType.DATE ){
			return new AttributeDropItem(ca);
		}else if (ca.getAttribute().getType() == AttributeType.LIST ){
			return new AttributeListDropItem(ca);
		}else if (ca.getAttribute().getType() == AttributeType.TREE ){
			return new AttributeTreeDropItem(ca);
		}
		return null;
	}
	
	public DropItem createAttributeDropItem(Attribute attribute){
		if (attribute.getType() == AttributeType.BOOLEAN || 
				attribute.getType() == AttributeType.NUMERIC ||
				attribute.getType() == AttributeType.TEXT ||
				attribute.getType() == AttributeType.DATE ){
			return new AttributeDropItem(attribute);
		}else if (attribute.getType() == AttributeType.LIST ){
			return new AttributeListDropItem(attribute);
		}else if (attribute.getType() == AttributeType.TREE ){
			return new AttributeTreeDropItem(attribute);
		}
		return null;
	}
	
	public DropItem createAreaDropItem(Area source, AreaFilter.AreaFilterGeometryType type){
		return new AreaDropItem(source, type);
	}
	
	public DropItem createAreaGroupByDropItem(AreaType areaType){
		return new AreaGroupByItem(areaType);
	}
	public DropItem createAreaGroupByDropItem(Area area){
		return new AreaGroupByItem(area);
	}
	
	public DropItem createAttributeListGroupByDropItem(CategoryAttribute cat){
		return new AttributeListGroupByDropItem(cat);
	}
	public DropItem createAttributeTreeNodeGroupByDropItem(Attribute att, int treeLevel, Category cat){
		return new AttributeTreeGroupByDropItem(att,treeLevel,cat);
	}
	
	public DropItem createAttributeTreeNodeGroupByDropItem(AttributeTreeNode node){
		return new AttributeTreeGroupByDropItem(node);
	}
	
	public DropItem createAttributeTreeNodeGroupByDropItem(AttributeTreeNode node, Category category){
		return new AttributeTreeGroupByDropItem(node, category);
	}
	
	public DropItem createAttributeListGroupByDropItem(Attribute attribute){
		return new AttributeListGroupByDropItem(attribute);
	}
	public DropItem createAttributeTreeNodeGroupByDropItem(Attribute att, int treeLevel){
		return new AttributeTreeGroupByDropItem(att,treeLevel);
	}

	
	public DropItem createCategoryGroupByDropItem(int treeLevel){
		return new CategoryGroupByDropItem(treeLevel);
	}
	
	public DropItem createCategoryGroupByDropItem(Category category){
		return new CategoryGroupByDropItem(category);
	}
	
	public DropItem createDateGroupByDropItem(IDateGroupBy op){
		return new DateGroupByDropItem(op);
	}
	
	public DropItem createDateGroupByDropItem(DateGroupByViewer op){
		return new DateGroupByDropItem(op.getGroupBy().getOption());
	}

	/**
	 * @return null if cannot create a drop item based on the source
	 * object
	 */
	@Override
	public DropItem[] generateDropItem(Object source, String queryItemPanelId) {
		
		DropItem[] items = null; 
		if (source instanceof Category) {
			items = new DropItem[]{ createCategoryDropItem((Category)source)};
			
		} else if (source instanceof CategoryAttribute) {
			items = new DropItem[]{ createAttributeDropItem((CategoryAttribute)source)};
		
		} else if (source instanceof Attribute) {
			items = new DropItem[]{ createAttributeDropItem((Attribute)source)};
		
		} else if (source instanceof IDateGroupBy) {
			items = new DropItem[]{createDateGroupByDropItem(
					(IDateGroupBy) source)};
		} else if (source instanceof DateGroupByViewer ){
			items = new DropItem[]{createDateGroupByDropItem((DateGroupByViewer)source)};
		}
		
		return items;
	}
	
	
	/**
	 * Creates anew attribute value drop item
	 * @param att
	 * @return
	 */
	public DropItem createAttributeValueDropItem(Attribute att){
		return new AttributeValueDropItem(false, att);
	}
	
	/**
	 * Creates a new category attribute value drop item
	 * @param catatt
	 * @return
	 */
	public DropItem createAttributeValueDropItem(CategoryAttribute catatt){
		return new AttributeValueDropItem(false, catatt);
	}
	
	/**
	 * Creates a new attribute list drop item
	 * @param item
	 * @return
	 */
	public DropItem createAttributeListItemValueDropItem(AttributeListItem item){
		return new AttributeListValueDropItem(false, item);
	}
	
	/**
	 * Creates a new attribute list item associated with a category
	 * @param item
	 * @param cat
	 * @return
	 */
	public DropItem createAttributeListItemValueDropItem(AttributeListItem item, Category cat){
		return new AttributeListValueDropItem(false, item,cat);
	}
	
	/**
	 * Creates a new attribute tree node drop item
	 * @param item
	 * @return
	 */
	public DropItem createAttributeTreeNodeValueDropItem(AttributeTreeNode item ){
		return new AttributeTreeValueDropItem(false, item);
	}
	
	/**
	 * Creates a new attribute tree node associated with a category
	 * @param item
	 * @param cat
	 * @return
	 */
	public DropItem createAttributeTreeNodeValueDropItem(AttributeTreeNode item, Category cat){
		return new AttributeTreeValueDropItem(false, item,cat);
	}
	
	/**
	 * Creates a category value drop item
	 * @param cat
	 * @return
	 */
	public DropItem createCategoryValueDropItem(Category cat){
		if (cat == null){
			return new CategoryValueDropItem(false);
		}
		return new CategoryValueDropItem(false, cat);
	}
	
	
	/**
	 * Creates one of the other query drop items
	 * @param other
	 * @return an array of drop items of the associated type
	 */
	public DropItem[] createOtherDropItem(Operator other){
		if (other == Operator.BRACKETS){
			return createBracketIems();
		}else if (other == Operator.NOT){
			return new DropItem[]{ createNotDropItem() };
		}
		return null;
	}
	

	/**
	 * Does nothing; needs to be overwritten
	 */
	@Override
	public void generateDropItems(QueryProxy q, Session session) {
	}
	
	public DropItem[] filterToDropItem(IFilter f, Session session) throws Exception{
		if (f instanceof AreaFilter){
			return createDropItems((AreaFilter)f, session);
		}else if (f instanceof AttributeFilter){
			return createDropItems((AttributeFilter)f, session);
		}else if (f instanceof CategoryAttributeFilter){
			return createDropItems((CategoryAttributeFilter)f, session);
		}else if (f instanceof CategoryFilter){
			return createDropItems((CategoryFilter)f, session);
		}else if (f instanceof BooleanExpression){
			return createDropItems((BooleanExpression)f, session);
		}else if (f instanceof BracketFilter){
			return createDropItems((BracketFilter)f, session);
		}else if (f instanceof NotExpression){
			return createDropItems((NotExpression)f, session);
		}else if (f instanceof ObserverFilter){
			return createDropItems((ObserverFilter)f, session);
		}else if (f instanceof EmptyFilter){
			return new DropItem[]{};
		}
		return null;
		
	}
	
	
	public DropItem valueItemToDropItem(IValueItem item, Session session) throws Exception{
		if (item instanceof CategoryValueItem){
			return createDropItems((CategoryValueItem)item, session);
			
		}else if (item instanceof CombinedValueItem){
			return createDropItems((CombinedValueItem)item, session);
			
		}else if (item instanceof AttributeValueItem){
			return createDropItems((AttributeValueItem)item, session);
		}
		return null;
		
	}
	
	public DropItem[] createDropItems(AreaFilter af, Session session) throws Exception{
		try{
			return new DropItem[]{ new AreaDropItem(loadArea(af, session), af.getGeometryType())};
		}catch(Exception ex){
			return new DropItem[]{new ErrorDropItem(ex.getMessage())};
		}
		
	}
	
	/*
	 * Loads the area for the given type/key.
	 */
	private Area loadArea(AreaFilter af, Session session){
		@SuppressWarnings("unchecked")
		List<Area> areas = session.createCriteria(Area.class)
			.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
			.add(Restrictions.eq("type", af.getType())) //$NON-NLS-1$
			.add(Restrictions.eq("keyId", af.getKey())).list();  //$NON-NLS-1$
		if (areas.size() == 0){
			throw new IllegalStateException(
					MessageFormat.format(
							Messages.AreaFilter_InvalidAreaKey, 
							new Object[]{SmartLabelProvider.getAreaTypeName(af.getType()), af.getKey()}));
		}else{
			return areas.get(0);
		}
		
	}
	
	/**
	 * 
	 * @see org.wcs.smart.query.parser.filter.IFilter#getDropItems(org.hibernate.Session)
	 * 
	 * @return {@link AttributeDropItem} or {@link AttributeListDropItem} 
	 * or {@link AttributeTreeDropItem} depending on 
	 * attribute type.
	 */
	public DropItem[] createDropItems(AttributeFilter filter, Session session) throws Exception{
		try{
			Attribute att = getAttribute(filter.getAttributeKey(), session);
			DropItem it = BasicDropItemFactory.INSTANCE.createAttributeDropItem(att);
			initAttributeDropItem(filter, it, session);
			return new DropItem[]{it};
		}catch (Exception ex){
			return new DropItem[]{new ErrorDropItem(ex.getMessage())};
		}
	}
	public void initAttributeDropItem(AttributeFilter filter, DropItem it, Session session){
		AttributeType attributeType = filter.getAttributeType();
		Object value1 = filter.getValue();
		String attributeKey = filter.getAttributeKey();
		if (attributeType == AttributeType.TEXT || 
				attributeType == AttributeType.NUMERIC){
			it.initializeData(new String[]{filter.getOperator().getGuiValue(), String.valueOf(value1)});
		}else if (attributeType == AttributeType.LIST){
			ListItem li = null;
			if (ANY_OPTION.getKey().equals((String)value1)){
				li = ANY_OPTION;
			}else{
				AttributeListItem ali = QueryDataModelManager.getInstance().getAttributeListItem(session, attributeKey, (String)value1);
				if (ali == null){
					throw new IllegalStateException(MessageFormat.format(Messages.AttributeFilter_ListItemNotFound, new Object[]{(String)value1, attributeKey}));
				}
				li = new ListItem(ali.getUuid(), ali.getName(), ali.getKeyId());
			}
			it.initializeData(li);
		}else if (attributeType == AttributeType.TREE){
			AttributeTreeNode ali = QueryDataModelManager.getInstance().getAttributeTreeNode(session, attributeKey, (String)value1);
			if (ali == null){
				throw new IllegalStateException(MessageFormat.format(Messages.AttributeFilter_TreeNodeNotFound, new Object[]{(String)value1, attributeKey}));
			}
			it.initializeData(ali);
		}else if (attributeType == AttributeType.DATE){
			it.initializeData(new String[]{(String)value1, (String)filter.getValue2(), filter.getOperator().getGuiValue()});
		}
	}
	
	/**
	 * Loads the attribute from the database
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public Attribute getAttribute(String attributeKey, Session session) throws Exception{
		Attribute att = QueryDataModelManager.getInstance().getAttribute(session, attributeKey);
		if (att == null){
			throw new Exception(MessageFormat.format(Messages.AttributeFilter_AttributeNotFound, new Object[]{attributeKey}));
		}
		return att;
	}

	public DropItem[] createDropItems(BooleanExpression exp, Session session) throws Exception{
		DropItem[] its1 = filterToDropItem(exp.getFilter1(), session);
		DropItem opDropItem = BasicDropItemFactory.createBooleanOpDropItem();
		opDropItem.initializeData(exp.getOperator().asSmartValue());
		
		DropItem[] its2 = filterToDropItem(exp.getFilter2(), session);
		
		DropItem[] results = new DropItem[its1.length + its2.length + 1];
		for (int i = 0; i < its1.length; i ++){
			results[i] = its1[i];
		}
		results[its1.length] = opDropItem;
		for (int i = 0; i < its2.length; i++){
			results[its1.length + i + 1] = its2[i];
		}
		return results;
	}
	

	public DropItem[] createDropItems(BracketFilter exp, Session session) throws Exception{
		DropItem[] its1 =filterToDropItem(exp.getFilter(), session);
		DropItem[] results = new DropItem[its1.length + 2];
		for (int i = 0; i < its1.length; i ++){
			results[i+1] = its1[i];
		}
		DropItem[] brackets = BasicDropItemFactory.createBracketIems();
		results[0] = brackets[0];
		results[results.length - 1] = brackets[1];
		return results;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#getDropItems(org.hibernate.Session)
	 * 
	 * @return {@link AttributeDropItem} or {@link AttributeListDropItem} 
	 * or {@link AttributeTreeDropItem} depending on 
	 * attribute type.
	 */
	public DropItem[] createDropItems(CategoryAttributeFilter filter, Session session) throws Exception {
		Category c = null;
		Attribute att = null;
		
		try{
			c = getCategory(filter.getCategoryFilter().getCategoryKey(), session);
			att = getAttribute(filter.getAttributeFilter().getAttributeKey(), session);

			boolean found = false;
			for (Attribute a : QueryDataModelManager.getInstance().getAttributes(session, c.getHkey())){
				if (a.getKeyId().equals(att.getKeyId())){
					found = true;
					break;
				}
			}
			if (!found){
				throw new Exception(MessageFormat.format(Messages.CategoryAttributeFilter_MissingCategoryAttribute, new Object[]{c.getKeyId(), att.getKeyId()}));
			}
		
			CategoryAttribute ca = new CategoryAttribute(c,  att);
			DropItem it = BasicDropItemFactory.INSTANCE.createAttributeDropItem(ca);
			initAttributeDropItem(filter.getAttributeFilter(), it, session);
			
			return new DropItem[]{it};
		}catch (Exception ex){
			return new DropItem[]{new ErrorDropItem(ex.getMessage())};
		}
	}
	
	/**
	 * @return {@link CategoryDropItem} or {@link ErrorDropItem} if 
	 * category cannot be found.
	 */
	public DropItem[] createDropItems(CategoryFilter filter, Session session) throws Exception{
		DropItem it = null;
		try{
			Category cat = getCategory(filter.getCategoryKey(), session);
			it = BasicDropItemFactory.INSTANCE.createCategoryDropItem(cat);
		}catch (Exception ex){
			it = new ErrorDropItem(ex.getMessage());
		}
		
		return new DropItem[]{it};
	}
	
	/**
	 * Loads the full category item from the database.
	 * 
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public Category getCategory(String categoryHKey, Session session) throws Exception{
		Category cat = QueryDataModelManager.getInstance().getCategory(session, categoryHKey);
		if (cat == null){
			throw new Exception(MessageFormat.format(Messages.CategoryFilter_CategoryNotFound, new Object[]{categoryHKey}));
		}
		return cat;
	}
	

	public DropItem[] createDropItems(NotExpression filter, Session session) throws Exception{
		DropItem[] its1 =filterToDropItem(filter.getFilter(), session);
		DropItem[] results = new DropItem[its1.length + 1];
		for (int i = 0; i < its1.length; i ++){
			results[i+1] = its1[i];
		}
		results[0] =  BasicDropItemFactory.createNotDropItem();
		
		return results;
	}
	
	public DropItem[] createDropItems(ObserverFilter filter, Session session) throws Exception {
		Employee e = (Employee) session.load(Employee.class, UuidUtils.stringToUuid(filter.getValue()));
		DropItem di;
		if (e == null){
			di = new ErrorDropItem(MessageFormat.format(Messages.ObserverFilter_EmployeeNotFound, new Object[]{filter.getValue()}));
		}else{
			e.getUuid();
			SmartLabelProvider.getShortLabel(e);
			
			di = BasicDropItemFactory.INSTANCE.createObserverDropItem();
			di.initializeData(e);
		}
		return new DropItem[]{di};
	}
	

	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asDropItem(org.hibernate.Session)
	 */
	public DropItem createDropItems(CategoryValueItem item, Session session) throws Exception{
		try{
			String categoryHkey = item.getCategoryHKey();
			DropItem di = null;
			if (categoryHkey == null){
				di = BasicDropItemFactory.INSTANCE.createCategoryValueDropItem(null);
			}else{
				Category category = QueryDataModelManager.getInstance().getCategory(session, categoryHkey);
				if (category == null){
					throw new Exception(MessageFormat.format(Messages.CategoryValueItem_categorynotfound, new Object[]{categoryHkey}));
				}
				category.getFullCategoryName();		//cache this
				di = BasicDropItemFactory.INSTANCE.createCategoryValueDropItem(category);
			}
			di.initializeData(getInitializeData(item));
			return di;
		} catch (Exception ex) {
			return new ErrorDropItem(ex.getMessage());
		}
		
	}
	
	public Object getInitializeData(IValueItem item){
		if (item instanceof CategoryValueItem){
			return ((CategoryValueItem) item).getType().key;
		}else if (item instanceof AttributeValueItem){
			if (((AttributeValueItem) item).getAttributeType() == AttributeType.NUMERIC){
				return DataModel.getAggregation(((AttributeValueItem)item).getAggregationKey());
			}else{
				return ((AttributeValueItem)item).getValueType().key;
			}
		}
		return null;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asDropItem(org.hibernate.Session)
	 */
	public DropItem createDropItems(CombinedValueItem item, Session session) throws Exception {
		DropItem d1 = valueItemToDropItem(item.getPart1(), session);
		DropItem d2 = valueItemToDropItem(item.getPart2(), session);
		
		Object[] data = new Object[2];
		data[0] = getInitializeData(item.getPart1());
		data[1] = d2;
		d1.initializeData(data);
		return d1;
	}
	
	public DropItem createDropItems(AttributeValueItem item, Session session) throws Exception{
		try{
			String attributeKey = item.getAttributeKey();
			String categoryKey = item.getCategoryKey();
			String itemKey = item.getItemKey();
			Attribute.AttributeType attributeType = item.getAttributeType();
			Attribute att = QueryDataModelManager.getInstance().getAttribute(session,attributeKey);
			if (att == null){
				throw new Exception(MessageFormat.format(Messages.AttributeValueItem_attributenotfound, new Object[]{attributeKey}));
			}
			DropItem di = null;
			Category cat = null;
			if (categoryKey != null){
				cat = QueryDataModelManager.getInstance().getCategory(session, categoryKey);
				if (cat == null){
					throw new Exception(MessageFormat.format(Messages.AttributeValueItem_categorynotfound, new Object[]{categoryKey}));
				}
				cat.getFullCategoryName();			
			}
			if (attributeType == AttributeType.NUMERIC){
				if (cat == null){
					di = BasicDropItemFactory.INSTANCE.createAttributeValueDropItem(att);
				}else{
					di = BasicDropItemFactory.INSTANCE.createAttributeValueDropItem(new CategoryAttribute(cat, att));
				}
			}else if (attributeType == AttributeType.LIST){
				AttributeListItem ali = QueryDataModelManager.getInstance().getAttributeListItem(session, attributeKey, itemKey);
				if (ali == null){
					throw new Exception(MessageFormat.format(Messages.AttributeValueItem_listitemnotfound, new Object[]{attributeKey, itemKey}));		
				}
				if (cat == null){
					di = BasicDropItemFactory.INSTANCE.createAttributeListItemValueDropItem(ali);
				}else{
					di = BasicDropItemFactory.INSTANCE.createAttributeListItemValueDropItem(ali,cat);
				}
			
			}else if (attributeType == AttributeType.TREE){
				AttributeTreeNode atn = QueryDataModelManager.getInstance().getAttributeTreeNode(session, attributeKey, itemKey);
				if (atn == null){
					throw new Exception(MessageFormat.format(Messages.AttributeValueItem_treenodenotfound, new Object[]{attributeKey, itemKey}));		
				}
				if (cat == null){
					di = BasicDropItemFactory.INSTANCE.createAttributeTreeNodeValueDropItem(atn);
				}else{
					di = BasicDropItemFactory.INSTANCE.createAttributeTreeNodeValueDropItem(atn,cat);
				}
			}
			if (di != null){
				di.initializeData(getInitializeData(item));
			}
			return di;
		} catch (Exception ex) {
			return new ErrorDropItem(ex.getMessage());
		}
	}
	
	public DropItem groupByToDropItem(IGroupBy gb, Session session) throws Exception{
		return findViewer(gb).asDropItem(session);
	}
	
	public IGroupByViewer<?> findViewer(IGroupBy groupBy){
		if (groupBy instanceof AreaGroupBy){
			return  new AreaGroupByViewer((AreaGroupBy) groupBy);
		}else if(groupBy instanceof AttributeGroupBy){
			return new AttributeGroupByViewer((AttributeGroupBy)groupBy);
		}else if (groupBy instanceof CategoryGroupBy){
			return new CategoryGroupByViewer((CategoryGroupBy)groupBy);
		}else if (groupBy instanceof ConservationAreaGroupBy){
			return new ConservationAreaGroupByViewer((ConservationAreaGroupBy)groupBy);
		}else if (groupBy instanceof ObserverGroupBy){
			return new ObserverGroupByViewer((ObserverGroupBy)groupBy);
		}else if (groupBy instanceof DateGroupBy){
			return new DateGroupByViewer((DateGroupBy) groupBy);
		}
		return null;
	}
}
