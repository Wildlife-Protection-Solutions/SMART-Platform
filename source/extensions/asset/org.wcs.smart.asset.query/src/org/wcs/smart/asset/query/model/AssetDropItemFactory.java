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
package org.wcs.smart.asset.query.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.query.AssetQueryPlugIn;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.parser.internal.filter.AssetAttributeFilter;
import org.wcs.smart.asset.query.parser.internal.filter.AssetFilter;
import org.wcs.smart.asset.query.parser.internal.summary.AssetAttributeValueItem;
import org.wcs.smart.asset.query.parser.internal.summary.AssetCategoryValueItem;
import org.wcs.smart.asset.query.parser.internal.summary.AssetGroupBy;
import org.wcs.smart.asset.query.parser.internal.summary.AssetValueItem;
import org.wcs.smart.asset.query.ui.AssetOptionData;
import org.wcs.smart.asset.query.ui.definition.AssetFilterPanel;
import org.wcs.smart.asset.query.ui.definition.AssetSummaryGroupByValuePanel;
import org.wcs.smart.asset.query.ui.definition.dropItems.AssetAttributeDropItem;
import org.wcs.smart.asset.query.ui.definition.dropItems.AssetAttributeListDropItem;
import org.wcs.smart.asset.query.ui.definition.dropItems.AssetFillterDropItem;
import org.wcs.smart.asset.query.ui.definition.dropItems.AssetGroupByDropItem;
import org.wcs.smart.asset.query.ui.definition.dropItems.AssetValueDropItem;
import org.wcs.smart.asset.query.ui.itempanel.AttributeWrapper;
import org.wcs.smart.asset.query.ui.itempanel.SummaryDeploymentItemPanel;
import org.wcs.smart.asset.query.ui.itempanel.SummaryItemPanel;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.ui.itempanel.SummaryDataModelContentProvider;
import org.wcs.smart.query.common.ui.itempanel.SummaryDmObject;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.BooleanExpression;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IGroupByViewer;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.SumQueryDefinition;
import org.wcs.smart.query.model.summary.ValuePart;
import org.wcs.smart.query.ui.definition.BasicFilterDefintionPanel;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDropItemFactory;
import org.wcs.smart.query.ui.model.impl.AttributeListValueDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeTreeValueDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeValueDropItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.query.ui.model.impl.CategoryValueDropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

/**
 * Drop item factory for asset queries.
 * @author Emily
 *
 */
public class AssetDropItemFactory extends BasicDropItemFactory implements IDropItemFactory {

	public static AssetDropItemFactory INSTANCE = new AssetDropItemFactory();
	
	protected AssetDropItemFactory(){
		
	}
	
	
	/**
	 * Generates a drop item for the given source object from
	 * the given query item panel.
	 * @param source source object
	 * @param queryItemPanelId object source location
	 */
	@Override
	public DropItem[] generateDropItem(Object source, String queryItemPanelId) {
		
		DropItem[] items = super.generateDropItem(source, queryItemPanelId);
		if (items != null){
			return items;
		}
		
		if (source instanceof Operator) {
			items = createOtherDropItem((Operator)source);
		}else if (source instanceof AssetType) {
			if (!((AssetType) source).getConservationArea().equals(SmartDB.getCurrentConservationArea())) {
				items = new DropItem[] {new ErrorDropItem(MessageFormat.format(Messages.AssetDropItemFactory_AssetTypeNotFound, ((AssetType)source).getKeyId()))};
			}else {
				items = new DropItem[] {new AssetFillterDropItem((AssetType)source) };
			}
		}else if (source instanceof Asset) {
			if (!((Asset) source).getConservationArea().equals(SmartDB.getCurrentConservationArea())) {
				items = new DropItem[] {new ErrorDropItem(MessageFormat.format(Messages.AssetDropItemFactory_AssetNotFound, ((Asset)source).getId()))};
			}else {
				items = new DropItem[] {new AssetFillterDropItem((Asset)source) };
			}
		}else if (source instanceof AssetStation) {
			if (!((AssetStation) source).getConservationArea().equals(SmartDB.getCurrentConservationArea())) {
				items = new DropItem[] {new ErrorDropItem(MessageFormat.format(Messages.AssetDropItemFactory_AssetNotFound, ((AssetStation)source).getId()))};
			}else {
				items = new DropItem[] {new AssetFillterDropItem((AssetStation)source) };
			}
		}else if (source instanceof AssetStationLocation) {
			if (!((AssetStationLocation) source).getStation().getConservationArea().equals(SmartDB.getCurrentConservationArea())) {
				items = new DropItem[] {new ErrorDropItem(MessageFormat.format(Messages.AssetDropItemFactory_AssetNotFound, ((AssetStationLocation)source).getId()))};
			}else {
				items = new DropItem[] {new AssetFillterDropItem((AssetStationLocation)source) };
			}
		}else if (source instanceof AssetValueOption) {
			items = new DropItem[] {new AssetValueDropItem((AssetValueOption)source, null)};
		} else if (source instanceof SummaryDmObject) {
			items = new DropItem[]{createSummaryDmDropItem((SummaryDmObject)source)};
		} else if (source instanceof AssetFilterOption) {
			if (queryItemPanelId.equals(SummaryItemPanel.ID) ||
					queryItemPanelId.equals(SummaryDeploymentItemPanel.ID)){
				items = new DropItem[]{createAssetGroupByDropItem((AssetFilterOption) source)};
			}
		}else if (source == SummaryDataModelContentProvider.DataModelItem.CATEGORIES_VALUE){
			if (queryItemPanelId.equals(SummaryItemPanel.ID)  ||
					queryItemPanelId.equals(SummaryDeploymentItemPanel.ID)){
				items = new DropItem[]{createCategoryValueDropItem(null)};
			}
		}else if (source instanceof AttributeWrapper) {
			AttributeWrapper w = (AttributeWrapper)source;
			if (w.getAttribute().getType() == AssetAttribute.AttributeType.DATE ||
				w.getAttribute().getType() == AssetAttribute.AttributeType.BOOLEAN ||
				w.getAttribute().getType() == AssetAttribute.AttributeType.NUMERIC ||
				w.getAttribute().getType() == AssetAttribute.AttributeType.TEXT) { 
				
				items = new DropItem[] {new AssetAttributeDropItem(w)};
			}else if (w.getAttribute().getType() == AssetAttribute.AttributeType.LIST) {
				items = new DropItem[] {new AssetAttributeListDropItem(w)};
			}
			
		}

		return items;
		
	}

	/**
	 * Creates a new asset value drop item
	 * @param item
	 * @return
	 */
	public DropItem createAssetValueDropItem(AssetValueOption item, AssetFormatOption format){
		return new AssetValueDropItem(item, format);
	}
	
	/**
	 * Creates a new asset group by drop item
	 * @param item
	 * @return
	 */
	public DropItem createAssetGroupByDropItem(AssetFilterOption item){
		DropItem di = new AssetGroupByDropItem(item);
		di.initializeData(new Object[]{new AssetOptionData(item)});
		return di;
	}
	
	
	/*
	 * Creates a drop item from a SummaryDmObject
	 */
	private DropItem createSummaryDmDropItem(SummaryDmObject object) {
		if (object.isValue()) {
			if (object.getObject() instanceof Attribute) {
				if (((Attribute)object.getObject()).getType() == AttributeType.NUMERIC){
					return createAttributeValueDropItem(
						(Attribute) object.getObject());
				}
				return null;
			} else if (object.getObject() instanceof CategoryAttribute) {
				if (((CategoryAttribute)object.getObject()).getAttribute().getType() == AttributeType.NUMERIC){
					return createAttributeValueDropItem((CategoryAttribute) object.getObject());
				}
				return null;
			} else if (object.getObject() instanceof Category) {
				return createCategoryValueDropItem(
						(Category) object.getObject());
			} else if (object.getObject() instanceof AttributeListItem){
				if (object.getObject2() != null){
					return createAttributeListItemValueDropItem((AttributeListItem)object.getObject(),(Category)object.getObject2());
				}else{
					return createAttributeListItemValueDropItem((AttributeListItem)object.getObject());
				}
			} else if (object.getObject() instanceof AttributeTreeNode){
				if (object.getObject2() != null){
					return createAttributeTreeNodeValueDropItem((AttributeTreeNode)object.getObject(), (Category)object.getObject2());
				}else{
					return createAttributeTreeNodeValueDropItem((AttributeTreeNode)object.getObject());
				}
			} else if (object.getObject() instanceof AssetValueOption ) {
				return createAssetValueDropItem( (AssetValueOption)object.getObject(), null);
			}
		} else {
			// category
			if (object.getObject() instanceof Category) {
				return createCategoryGroupByDropItem((Category) object.getObject());
			} else if (object.getObject() instanceof Attribute) {
				if (((Attribute)object.getObject()).getType() == AttributeType.LIST ){
					return createAttributeListGroupByDropItem((Attribute) object.getObject());
				}
			} else if (object.getObject() instanceof CategoryAttribute) {
				if ( ((CategoryAttribute)object.getObject()).getAttribute().getType() == AttributeType.LIST ){
					return createAttributeListGroupByDropItem((CategoryAttribute) object.getObject());
				}
			} else if (object.getObject() instanceof AttributeTreeNode) {
				
				if (object.getObject2() != null) {
					return createAttributeTreeNodeGroupByDropItem(
									(AttributeTreeNode) object.getObject(),
									(Category) object.getObject2());
				} else {
					return createAttributeTreeNodeGroupByDropItem(
									(AttributeTreeNode) object.getObject());
				}
			}
		}
		return null;
	}

	
	
	/**
	 * Creates anew attribute value drop item
	 * @param att
	 * @return
	 */
	@Override
	public DropItem createAttributeValueDropItem(Attribute att){
		return new AttributeValueDropItem(false, att);
	}
	
	/**
	 * Creates a new category attribute value drop item
	 * @param catatt
	 * @return
	 */
	@Override
	public DropItem createAttributeValueDropItem(CategoryAttribute catatt){
		return new AttributeValueDropItem(false, catatt);
	}
	
	/**
	 * Creates a new attribute list drop item
	 * @param item
	 * @return
	 */
	@Override
	public DropItem createAttributeListItemValueDropItem(AttributeListItem item){
		return new AttributeListValueDropItem(false, item);
	}
	
	/**
	 * Creates a new attribute list item associated with a category
	 * @param item
	 * @param cat
	 * @return
	 */
	@Override
	public DropItem createAttributeListItemValueDropItem(AttributeListItem item, Category cat){
		return new AttributeListValueDropItem(false, item,cat);
	}
	
	/**
	 * Creates a new attribute tree node drop item
	 * @param item
	 * @return
	 */
	@Override
	public DropItem createAttributeTreeNodeValueDropItem(AttributeTreeNode item ){
		return new AttributeTreeValueDropItem(false, item);
	}
	
	/**
	 * Creates a new attribute tree node associated with a category
	 * @param item
	 * @param cat
	 * @return
	 */
	@Override
	public DropItem createAttributeTreeNodeValueDropItem(AttributeTreeNode item, Category cat){
		return new AttributeTreeValueDropItem(false, item,cat);
	}
	
	/**
	 * Creates a category value drop item
	 * @param cat
	 * @return
	 */
	@Override
	public DropItem createCategoryValueDropItem(Category cat){
		if (cat == null){
			return new CategoryValueDropItem(false);
		}
		return new CategoryValueDropItem(false, cat);
	}
	
	/**
	 * Generates drop items for the given query proxy.
	 * 
	 * @see org.wcs.smart.query.ui.model.impl.BasicDropItemFactory#generateDropItems(org.wcs.smart.query.model.QueryProxy, org.hibernate.Session)
	 */
	@Override
	public void generateDropItems(QueryProxy proxy, Session session) {
		try{
			if (proxy.getQuery() instanceof SimpleQuery){
				IFilter queryFilter = ((SimpleQuery)proxy.getQuery()).getFilter().getFilter();
				proxy.setDropItems(BasicFilterDefintionPanel.ID, asDropItems(queryFilter, session));
	
			}else if (AssetSummaryQuery.isAssetSummary(proxy.getQuery().getTypeKey())) { 
				
				AssetSummaryQuery q = (AssetSummaryQuery) proxy.getQuery();
				SumQueryDefinition def = q.getQueryDefinition();

				//value filter panel
				proxy.setDropItems(AssetFilterPanel.ID, def == null || def.getValueFilter() == null ? null : asDropItems(def.getValueFilter().getFilter(), session));
 
				//column group by
				proxy.setDropItems(AssetSummaryGroupByValuePanel.ID + "." + AssetSummaryGroupByValuePanel.ListTargetType.COLUMN.name(), //$NON-NLS-1$
						def == null || def.getColumnGroupByPart() == null ? null :
							groupByToDropItems(def.getColumnGroupByPart(), session));
							
				//row group by
				proxy.setDropItems(AssetSummaryGroupByValuePanel.ID + "." + AssetSummaryGroupByValuePanel.ListTargetType.ROW.name(), //$NON-NLS-1$
						def == null || def.getRowGroupByPart() == null ? null : 
							groupByToDropItems(def.getRowGroupByPart(), session));
				//values
				List<DropItem> items = null;
				if (def != null && def.getValuePart() != null){
					items = valuePartToDropItems(def.getValuePart(),session);
				}
				proxy.setDropItems(AssetSummaryGroupByValuePanel.ID + "." + AssetSummaryGroupByValuePanel.ListTargetType.VALUE.name(), items);//$NON-NLS-1$
			}
		}catch (Exception ex){
			AssetQueryPlugIn.log(ex.getMessage(), ex);
		}
	}
	
	/*
	 * Converts a filter to a set of drop items
	 */
	private List<DropItem> asDropItems(IFilter filter, Session session){
		List<DropItem> items = new ArrayList<DropItem>();
		try{
			DropItem[] filterItems = filterToDropItem(filter, session);
			for(DropItem i : filterItems){
				items.add(i);
			}
			
		}catch (Exception ex){
			items.add(new ErrorDropItem(MessageFormat.format(Messages.SimpleQuery_DropItemParseError, new Object[]{ex.getMessage()})));
		}
		return items;
	}
	
	@Override
	public DropItem[] filterToDropItem(IFilter f, Session session) throws Exception{
		if (f instanceof AssetFilter){
			return createDropItems((AssetFilter)f, session);
		}else if (f instanceof BooleanExpression){
			return createDropItems((BooleanExpression)f, session);
		}else if (f instanceof AssetAttributeFilter) {
			return createDropItems((AssetAttributeFilter)f, session);
		}
		return super.filterToDropItem(f, session);
		
	}
	
	public DropItem[] createDropItems(AssetAttributeFilter filter, Session session) throws Exception{
		try{
			AssetAttribute attribute = getAssetAttribute(filter.getAttributeKey(), session);
			AttributeWrapper wrapper = new AttributeWrapper(attribute, filter.getSource());
			
			DropItem it = null;
			if (attribute.getType() == AssetAttribute.AttributeType.LIST) {
				it = new AssetAttributeListDropItem(wrapper);
			}else {
				it = new AssetAttributeDropItem(wrapper);
			}
			
			initAttributeDropItem(filter, it, session);
			return new DropItem[]{it};
		}catch (Exception ex){
			return new DropItem[]{new ErrorDropItem(ex.getMessage())};
		}
	}
	
	public AssetAttribute getAssetAttribute(String attributeKey, Session session) throws Exception{
		//TODO: ccaa support is currently not implemented for any asset related features
		AssetAttribute att = session.createQuery("FROM AssetAttribute WHERE keyid = :keyid and conservationArea = :ca", AssetAttribute.class) //$NON-NLS-1$
				.setParameter("keyid", attributeKey) //$NON-NLS-1$
				.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
				.uniqueResult();
		
		if (att == null){
			throw new Exception(MessageFormat.format(Messages.AssetDropItemFactory_AssetAttributeNotFound, new Object[]{attributeKey}));
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
	
	/**
	 * Converts all value items to drop items.
	 * @param session
	 * @return
	 */
	public List<DropItem> valuePartToDropItems(ValuePart part, Session session) {
		ArrayList<DropItem> item = new ArrayList<DropItem>();
		try{
			for (IValueItem valueItem : part.getValueItems()){
				item.add(valueItemToDropItem(valueItem, session));
			}
		}catch (Exception ex){
			QueryPlugIn.log(ex.getMessage(), ex);
			item.clear();
			item.add(new ErrorDropItem(ex.getMessage()));
		}
		return item;

	}
	@Override
	public DropItem valueItemToDropItem(IValueItem item, Session session) throws Exception{
		if (item instanceof AssetAttributeValueItem){
			return asDropItem((AssetAttributeValueItem) item, session);
		}else if (item instanceof AssetCategoryValueItem){
			return asDropItem((AssetCategoryValueItem) item, session);
		}else if (item instanceof AssetValueItem) {
			return asDropItem ((AssetValueItem)item);
		}
		return super.valueItemToDropItem(item, session);
	}
	
	
	public DropItem asDropItem(AssetValueItem item) {
		return createAssetValueDropItem(item.getAssetValueOption(), item.getAssetFormatOption());
	}
	
	public DropItem asDropItem(AssetCategoryValueItem item, Session session) throws Exception{
		try{
			String categoryHkey = item.getCategoryHKey();
			DropItem di = null;
			if (categoryHkey == null){
				di = AssetDropItemFactory.INSTANCE.createCategoryValueDropItem(null);
			}else{
				Category category = QueryDataModelManager.getInstance().getCategory(session, categoryHkey);
				if (category == null){
					throw new Exception(MessageFormat.format(Messages.AssetCategoryValueItem_CategoryNotFound, new Object[]{categoryHkey}));
				}
				category.getFullCategoryName();		//cache this
				di = AssetDropItemFactory.INSTANCE.createCategoryValueDropItem(category);
			}
			
			di.initializeData(new Object[]{getInitializeData(item), null});
			return di;
		} catch (Exception ex) {
			return new ErrorDropItem(ex.getMessage());
		}
		
	}
	public DropItem asDropItem(AssetAttributeValueItem item, Session session) throws Exception{
		String attributeKey = item.getAttributeKey();
		String categoryKey = item.getCategoryKey();
		String itemKey = item.getItemKey();
		
		Attribute.AttributeType attributeType = item.getAttributeType();
		
		try{
			Attribute att = QueryDataModelManager.getInstance().getAttribute(session,attributeKey);
			if (att == null){
				throw new Exception(MessageFormat.format(Messages.AssetAttributeValueItem_AttributeNotFound, new Object[]{attributeKey}));
			}
			DropItem di = null;
			Category cat = null;
			if (categoryKey != null){
				cat = QueryDataModelManager.getInstance().getCategory(session, categoryKey);
				if (cat == null){
					throw new Exception(MessageFormat.format(Messages.AssetAttributeValueItem_CategoryNotFound, new Object[]{categoryKey}));
				}
				cat.getFullCategoryName();			
			}
			if (attributeType == AttributeType.NUMERIC){
				if (cat == null){
					di = AssetDropItemFactory.INSTANCE.createAttributeValueDropItem(att);
				}else{
					di = AssetDropItemFactory.INSTANCE.createAttributeValueDropItem(new CategoryAttribute(cat, att));
				}
			}else if (attributeType == AttributeType.LIST){
				AttributeListItem ali = QueryDataModelManager.getInstance().getAttributeListItem(session, attributeKey, itemKey);
				if (ali == null){
					throw new Exception(MessageFormat.format(Messages.AssetAttributeValueItem_ListItemNotFound, new Object[]{attributeKey, itemKey}));		
				}
				if (cat == null){
					di = AssetDropItemFactory.INSTANCE.createAttributeListItemValueDropItem(ali);
				}else{
					di = AssetDropItemFactory.INSTANCE.createAttributeListItemValueDropItem(ali,cat);
				}
			
			}else if (attributeType == AttributeType.TREE){
				AttributeTreeNode atn = QueryDataModelManager.getInstance().getAttributeTreeNode(session, attributeKey, itemKey);
				if (atn == null){
					throw new Exception(MessageFormat.format(Messages.AssetAttributeValueItem_TreeNodeNotFound, new Object[]{attributeKey, itemKey}));		
				}
				if (cat == null){
					di = AssetDropItemFactory.INSTANCE.createAttributeTreeNodeValueDropItem(atn);
				}else{
					di = AssetDropItemFactory.INSTANCE.createAttributeTreeNodeValueDropItem(atn,cat);
				}
			}
			if (di != null){
				di.initializeData(new Object[]{getInitializeData(item), null});
			}
			return di;
		} catch (Exception ex) {
			return new ErrorDropItem(ex.getMessage());
		}
	}
	

	private DropItem[] createDropItems(AssetFilter f, Session session) throws Exception {
		AssetFilterOption option = f.getAssetOption();
		UUID uuid = f.getValue();
		DropItem it = null;
		if (option == AssetFilterOption.ASSET) {
			Asset asset = session.get(Asset.class, uuid);
			if (asset == null || !asset.getConservationArea().equals(SmartDB.getCurrentConservationArea())) {
				it = new ErrorDropItem(Messages.AssetDropItemFactory_AssetNotFound2);
			}else {
				it = new AssetFillterDropItem(asset);
			}
		}else if (option == AssetFilterOption.ASSETTYPE) {
			AssetType asset = session.get(AssetType.class, uuid);
			if (asset == null || !asset.getConservationArea().equals(SmartDB.getCurrentConservationArea())) {
				it = new ErrorDropItem(Messages.AssetDropItemFactory_AssetTypeNotFound2);
			}else {
				it = new AssetFillterDropItem(asset);
			}
		}else if (option == AssetFilterOption.STATION) {
			AssetStation asset = session.get(AssetStation.class, uuid);
			if (asset == null || !asset.getConservationArea().equals(SmartDB.getCurrentConservationArea())) {
				it = new ErrorDropItem(Messages.AssetDropItemFactory_AssetStationNotFound);
			}else {
				it = new AssetFillterDropItem(asset);
			}
		}else if (option == AssetFilterOption.STATIONLOCATION) {
			AssetStationLocation asset = session.get(AssetStationLocation.class, uuid);
			if (asset == null || !asset.getStation().getConservationArea().equals(SmartDB.getCurrentConservationArea())) {
				it = new ErrorDropItem(Messages.AssetDropItemFactory_AssetLocationNotFound);
			}else {
				it = new AssetFillterDropItem(asset);
			}
		}
		 
		return new DropItem[] { it };
	}
	
	/**
	 * Converts the group by part into a collection of
	 * drop items.
	 * 
	 * @param session
	 * @return
	 */
	public List<DropItem> groupByToDropItems(GroupByPart groupBy, Session session) {
		ArrayList<DropItem> item = new ArrayList<DropItem>();
		try{
			for (IGroupBy groupByItem : groupBy.getGroupBys()){
				item.add(groupByToDropItem(groupByItem, session));
			}
		}catch (Exception ex){
			QueryPlugIn.log(ex.getMessage(), ex);
			item.clear();
			item.add(new ErrorDropItem(ex.getMessage()));
		}
		return item;
	}
	
	@Override
	public IGroupByViewer<?> findViewer(IGroupBy groupBy){
		if (groupBy instanceof AssetGroupBy){
			return new AssetGroupByViewer((AssetGroupBy) groupBy, new AssetOptionData(((AssetGroupBy)groupBy).getOption()));
		}
		return super.findViewer(groupBy);
	}
	
	@Override
	public DropItem groupByToDropItem(IGroupBy item, Session session) throws Exception{
		return findViewer(item).asDropItem(session);
	}
}
