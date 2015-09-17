package org.wcs.smart.query.model.summary;

import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.AllCategory;
import org.wcs.smart.query.model.summary.IValueItem.ValueType;
import org.wcs.smart.query.ui.model.impl.AbstractValueDropItem;

public class ValueItemLabelProvider {

	public static final String PER_LABEL = AbstractValueDropItem.PER_LABEL;
	
	public static ValueItemLabelProvider INSTANCE = new ValueItemLabelProvider();
	
	protected ValueItemLabelProvider(){
		
	}
	
	
	public String getName(IValueItem item, Session session){
		if (item instanceof AttributeValueItem){
			return getName((AttributeValueItem)item, session);
		}else if (item instanceof CategoryValueItem){
			return getName((CategoryValueItem)item, session);
		}else if (item instanceof CombinedValueItem){
			return getFullName((CombinedValueItem)item, session);
		}
		return ""; //$NON-NLS-1$
	}
	
	public String getFullName(IValueItem item, Session session){
		if (item instanceof AttributeValueItem){
			return getFullName((AttributeValueItem)item, session);
		}else if (item instanceof CategoryValueItem){
			return getFullName((CategoryValueItem)item, session);
		}else if (item instanceof CombinedValueItem){
			return getFullName((CombinedValueItem)item, session);
		}
		return "";		 //$NON-NLS-1$
	}
	
	public String getLabel(ValueType type){
		if (type == ValueType.OBSERVATION){
			return Messages.CategoryValueItem_CountObservationLabel;
		}else if (type == ValueType.WAYPOINT){
			return Messages.CategoryValueItem_CountIncidentLabel;
		}
		return ""; //$NON-NLS-1$
	}
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getName(org.hibernate.Session)
	 */
	public String getName(AttributeValueItem item, Session session){
		String attributeKey = item.getAttributeKey();
		String itemKey = item.getItemKey();
		String categoryKey = item.getCategoryKey();
		Aggregation agg = DataModel.getAggregation(item.getAggregationKey());
		Attribute att = QueryDataModelManager.getInstance().getAttribute(session,attributeKey);
		if (att == null){
			return ""; //$NON-NLS-1$
		}
		String itemName = null;
		if (att.getType() == AttributeType.LIST){
			AttributeListItem it = QueryDataModelManager.getInstance().getAttributeListItem(session, attributeKey, itemKey);
			itemName = it.getName();
		}else if (att.getType() == AttributeType.TREE){
			AttributeTreeNode it = QueryDataModelManager.getInstance().getAttributeTreeNode(session, attributeKey, itemKey);
			itemName = it.getName();
		}
		StringBuilder name = new StringBuilder();
		if (item.getValueType() != null){
			name.append(getLabel(item.getValueType()));
		}else if (agg != null){
			name.append(Aggregation.getGuiName(agg, session, Locale.getDefault()));
		}
		name.append(" "); //$NON-NLS-1$
		if (itemName != null){
			name.append(itemName);
		}else{
			name.append(att.getName());
		}
		
		if (categoryKey != null){
			Category cat = QueryDataModelManager.getInstance().getCategory(session, categoryKey);
			if (cat != null){
				name.append( " (" + cat.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}else{
				name.append(" (not found) "); //$NON-NLS-1$
			}
		}
		return name.toString();
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getFullName(org.hibernate.Session)
	 */
	public String getFullName(AttributeValueItem item, Session session){
		String attributeKey = item.getAttributeKey();
		String itemKey = item.getItemKey();
		String categoryKey = item.getCategoryKey();
		Aggregation agg = DataModel.getAggregation(item.getAggregationKey());
		
		Attribute att = QueryDataModelManager.getInstance().getAttribute(session,attributeKey);
		if (att == null){
			return ""; //$NON-NLS-1$
		}
		String itemName = null;
		if (att.getType() == AttributeType.LIST){
			AttributeListItem it = QueryDataModelManager.getInstance().getAttributeListItem(session, attributeKey, itemKey);
			itemName = it.getName();
		}else if (att.getType() == AttributeType.TREE){
			AttributeTreeNode it = QueryDataModelManager.getInstance().getAttributeTreeNode(session, attributeKey, itemKey);
			itemName = it.getName();
		}
		StringBuilder name = new StringBuilder();
		if (item.getValueType() != null){
			name.append(getLabel(item.getValueType()));
		}else if (agg != null){
			name.append(Aggregation.getGuiName(agg, session, Locale.getDefault()));
		}
		name.append(" "); //$NON-NLS-1$
		if (itemName != null){
			name.append(itemName);
			name.append(" [" + att.getName() + "] "); //$NON-NLS-1$ //$NON-NLS-2$
		}else{
			name.append(att.getName());
		}
		
		if (categoryKey != null){
			Category cat = QueryDataModelManager.getInstance().getCategory(session, categoryKey);
			if (cat != null){
				name.append( " (" + cat.getFullCategoryName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}else{
				name.append(" (not found) "); //$NON-NLS-1$
			}
		}
		return name.toString();
		
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getName(org.hibernate.Session)
	 */
	public String getName(CategoryValueItem item, Session session){
		String categoryHkey = item.getCategoryHKey();
		if (categoryHkey == null){
			return getLabel(item.getType()) + " " + AllCategory.INSTANCE.getName(); //$NON-NLS-1$
		}
		Category c = QueryDataModelManager.getInstance().getCategory(session, categoryHkey);
		if (c == null){
			return item.getCategoryHKey();
		}
		return getLabel(item.getType()) + " " + c.getName(); //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getFullName(org.hibernate.Session)
	 */
	public String getFullName(CategoryValueItem item, Session session){
		String categoryHkey = item.getCategoryHKey();
		if (categoryHkey == null){
			return getLabel(item.getType()) + " " + AllCategory.INSTANCE.getName(); //$NON-NLS-1$
		}
		Category c = QueryDataModelManager.getInstance().getCategory(session, categoryHkey);
		if (c == null){
			return item.getCategoryHKey();
		}
		return getLabel(item.getType()) + " " + c.getName(); //$NON-NLS-1$
	}


	public String getName(CombinedValueItem item, Session session) {
		StringBuilder sb = new StringBuilder();
		sb.append(getName(item.getPart1(), session));
		sb.append(" " + PER_LABEL + " "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(getName(item.getPart2(), session));
		return sb.toString();
	}

	public String getFullName(CombinedValueItem item, Session session){
		StringBuilder sb = new StringBuilder();
		sb.append(getFullName(item.getPart1(), session));
		sb.append(" " + PER_LABEL + " "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(getFullName(item.getPart2(), session));
		return sb.toString();
	}
}
