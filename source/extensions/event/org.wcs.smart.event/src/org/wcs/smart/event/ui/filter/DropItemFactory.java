/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.event.ui.filter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.event.filter.AttributeFilter;
import org.wcs.smart.event.filter.BooleanFilter;
import org.wcs.smart.event.filter.BracketFilter;
import org.wcs.smart.event.filter.CategoryAttributeFilter;
import org.wcs.smart.event.filter.CategoryFilter;
import org.wcs.smart.event.filter.IFilter;
import org.wcs.smart.event.filter.NotFilter;
import org.wcs.smart.event.filter.Operator;
import org.wcs.smart.event.internal.Messages;
import org.wcs.smart.event.ui.filter.BracketDropItem.BracketType;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Factory for generating drop items.
 * 
 * @author Emily
 *
 */
public enum DropItemFactory {
	INSTANCE;
	
	public static final AttributeListItem ANY_ATTRIBUTE = new AttributeListItem() {
		private static final long serialVersionUID = 1L;
		@Override
		public String getKeyId() {
			return AttributeFilter.ANY_OPTION_KEY;
		}
		
		@Override
		public String getName() {
			return Messages.DropItemFactory_AnyOptionLabel;
		}
	};
	
	/**
	 * populates the list of drop items with all the drop items associated with the filter
	 * @param filter
	 * @param items
	 * @param session
	 */
	public void createDropItem(IFilter filter, List<DropItem> items, Session session) throws Exception{
		if (filter == null) return;
		if (filter instanceof AttributeFilter) {
			AttributeFilter ff = (AttributeFilter)filter;

			Attribute aa = QueryFactory.buildQuery(session,  Attribute.class, 
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
					new Object[] {"keyId", ff.getAttributeKey()}).uniqueResult(); //$NON-NLS-1$
			if (aa == null) throw new Exception(MessageFormat.format(Messages.DropItemFactory_attributeNotFound, ff.getAttributeKey()));
			DropItem di = createDropItem(aa)[0];
			
			if (aa.getType() == AttributeType.DATE) {
				String[] initData = new String[] {ff.getValue().toString(), ff.getValue2().toString(), ff.getOperator().getGuiValue()};
				di.initializeData(initData);
			}else if (aa.getType() == AttributeType.LIST) {
				String attributeKey = ff.getValue().toString();
				AttributeListItem found = null;
				for (AttributeListItem ii : aa.getAttributeList()) {
					if (ii.getKeyId().equalsIgnoreCase(attributeKey)) {
						found = ii;
						break;
					}
				}
				if (found == null) {
					if (attributeKey.equalsIgnoreCase(ANY_ATTRIBUTE.getKeyId())) {
						found = ANY_ATTRIBUTE;
					}
				}
				if (found == null) {
					throw new Exception(MessageFormat.format(Messages.DropItemFactory_ListItemNotFound, ff.getValue().toString(), aa.getName())) ;
				}
				di.initializeData(found);
			}else if (aa.getType() == AttributeType.MLIST) {
				
				List<AttributeListItem> aitems = new ArrayList<>();
				
				String[] attributeKey = ff.getValue().toString().split(AttributeFilter.MLIST_SEPERATOR);
				for (String liKey : attributeKey) {
					AttributeListItem found = null;
					for (AttributeListItem ii : aa.getAttributeList()) {
						if (ii.getKeyId().equalsIgnoreCase(liKey)) {
							found = ii;
							break;
						}
					}
					if (found == null) {
						throw new Exception(MessageFormat.format(Messages.DropItemFactory_ListItemNotFound, ff.getValue().toString(), aa.getName())) ;	
					}else {
						aitems.add(found);
					}
				}
				di.initializeData(new Object[] {ff.getOperator(), aitems});
			}else if (aa.getType() == AttributeType.TREE) {
				AttributeTreeNode treenode = QueryFactory.buildQuery(session,  AttributeTreeNode.class, 
						new Object[] {"hkey", ff.getValue().toString()}, //$NON-NLS-1$
						new Object[] {"attribute", aa} //$NON-NLS-1$
						).uniqueResult();
				if (treenode == null) {
					throw new Exception(MessageFormat.format(Messages.DropItemFactory_TreeNodeNotFound, ff.getValue().toString(), aa.getName())) ;
				}
				di.initializeData(treenode);
			}else if (aa.getType() == AttributeType.BOOLEAN) {
			}else {
				String[] initData = new String[] {ff.getOperator().getGuiValue(), ff.getValue().toString()};
				di.initializeData(initData);
			}
			items.add(di);
		}else if (filter instanceof BooleanFilter) {
			BooleanFilter ff = (BooleanFilter)filter;
			
			createDropItem(ff.getFilter1(), items, session);
			DropItem[] dis = createDropItem(ff.getOperator());
			for (DropItem di : dis) items.add(di);
			createDropItem(ff.getFilter2(), items, session);
		}else if (filter instanceof BracketFilter) {
			BracketFilter ff = (BracketFilter)filter;
			
			items.add(new BracketDropItem(BracketType.OPEN));
			createDropItem(ff.getFilter(), items, session);
			items.add(new BracketDropItem(BracketType.CLOSE));
			
		}else if (filter instanceof CategoryAttributeFilter) {
			CategoryAttributeFilter ff = (CategoryAttributeFilter)filter;
			
			Category cc = QueryFactory.buildQuery(session,  Category.class, 
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
					new Object[] {"hkey", ff.getCategoryFilter().getCategoryKey()}).uniqueResult(); //$NON-NLS-1$
			if (cc == null) throw new Exception(MessageFormat.format(Messages.DropItemFactory_CategoryNotFound, ff.getCategoryFilter().getCategoryKey()));
			
			Attribute aa = QueryFactory.buildQuery(session,  Attribute.class, 
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
					new Object[] {"keyId", ff.getAttributeFilter().getAttributeKey()}).uniqueResult(); //$NON-NLS-1$
			if (aa == null) throw new Exception(MessageFormat.format(Messages.DropItemFactory_AttributeNotFound, ff.getAttributeFilter().getAttributeKey()));
			
			CategoryAttribute ca = new CategoryAttribute();
			ca.setCategory(cc);
			ca.setAttribute(aa);
			
			DropItem di = createDropItem(ca)[0];
			
			if (aa.getType() == AttributeType.DATE) {
				String[] initData = new String[] {ff.getAttributeFilter().getValue().toString(), ff.getAttributeFilter().getValue2().toString(), ff.getAttributeFilter().getOperator().getGuiValue()};
				di.initializeData(initData);
			}else if (aa.getType() == AttributeType.LIST) {
				String attributeKey = ff.getAttributeFilter().getValue().toString();
				AttributeListItem found = null;
				if (attributeKey.equalsIgnoreCase(AttributeFilter.ANY_OPTION_KEY)) {
					found = DropItemFactory.ANY_ATTRIBUTE;
				}else {
					for (AttributeListItem ii : aa.getAttributeList()) {
						if (ii.getKeyId().equalsIgnoreCase(attributeKey)) {
							found = ii;
							break;
						}
					}
				}
				if (found == null) {
					throw new Exception(MessageFormat.format(Messages.DropItemFactory_AttributeListItemNotFound, ff.getAttributeFilter().getAttributeKey(), aa.getName())) ;
				}
				di.initializeData(found);
			}else if (aa.getType() == AttributeType.MLIST) {
				
				List<AttributeListItem> aitems = new ArrayList<>();
				
				String[] attributeKey = ff.getAttributeFilter().getValue().toString().split(AttributeFilter.MLIST_SEPERATOR);
				for (String liKey : attributeKey) {
					AttributeListItem found = null;
					for (AttributeListItem ii : aa.getAttributeList()) {
						if (ii.getKeyId().equalsIgnoreCase(liKey)) {
							found = ii;
							break;
						}
					}
					if (found == null) {
						throw new Exception(MessageFormat.format(Messages.DropItemFactory_ListItemNotFound, ff.getAttributeFilter().getValue().toString(), aa.getName())) ;	
					}else {
						aitems.add(found);
					}
				}
				di.initializeData(new Object[] {ff.getAttributeFilter().getOperator(), aitems});
			}else if (aa.getType() == AttributeType.TREE) {
				AttributeTreeNode treenode = QueryFactory.buildQuery(session,  AttributeTreeNode.class, 
						new Object[] {"hkey", ff.getAttributeFilter().getValue().toString()}, //$NON-NLS-1$
						new Object[] {"attribute", aa} //$NON-NLS-1$
						).uniqueResult();
				if (treenode == null) {
					throw new Exception(MessageFormat.format(Messages.DropItemFactory_AttributeTreeNodeNotFound, ff.getAttributeFilter().getValue().toString(), aa.getName())) ;
				}
				di.initializeData(treenode);
			}else if (aa.getType() == AttributeType.BOOLEAN) {
			}else {
				String[] initData = new String[] {ff.getAttributeFilter().getOperator().getGuiValue(), ff.getAttributeFilter().getValue().toString()};
				di.initializeData(initData);
			}
			
			items.add(di);
		}else if (filter instanceof CategoryFilter) {
			CategoryFilter ff = (CategoryFilter)filter;
			
			Category cc = QueryFactory.buildQuery(session,  Category.class, 
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
					new Object[] {"hkey", ff.getCategoryKey()}).uniqueResult(); //$NON-NLS-1$
			if (cc == null) throw new Exception(MessageFormat.format(Messages.DropItemFactory_CategoryNotFound, ff.getCategoryKey()));
			DropItem[] dis = createDropItem(cc);
			for (DropItem di : dis) items.add(di);
			
		}else if (filter instanceof NotFilter) {
			NotFilter ff = (NotFilter)filter;
			
			DropItem[] dis = createDropItem(Operator.NOT);
			for (DropItem di : dis) items.add(di);
			createDropItem(ff.getFilter(), items, session);
		}else {
			throw new Exception(Messages.DropItemFactory_FilterExpressionSupported + filter.getClass().toString());
		}
	}
	
	
	public DropItem[] createDropItem(Object element) {
		if (element instanceof Category) {
			return new DropItem[] {new CategoryDropItem((Category)element) };
		}
		if (element instanceof Attribute) {
			Attribute a = (Attribute)element;
			if (a.getType() == Attribute.AttributeType.LIST) {
				return new DropItem[] {new AttributeListDropItem((Attribute)element) };
			}else if (a.getType() == Attribute.AttributeType.MLIST) {
				return new DropItem[] {new AttributeMListDropItem((Attribute)element) };
			}else if (a.getType() == Attribute.AttributeType.TREE) {
				return new DropItem[] {new AttributeTreeDropItem((Attribute)element) };
			}else {
				return new DropItem[] {new AttributeDropItem((Attribute)element) };	
			}
		}
		if (element instanceof CategoryAttribute) {
			CategoryAttribute a = (CategoryAttribute)element;
			if (a.getAttribute().getType() == Attribute.AttributeType.LIST) {
				return new DropItem[] {new AttributeListDropItem((CategoryAttribute)element) };
			}else if (a.getAttribute().getType() == Attribute.AttributeType.MLIST) {
				return new DropItem[] {new AttributeMListDropItem((CategoryAttribute)element) };
			}else if (a.getAttribute().getType() == Attribute.AttributeType.TREE) {
				return new DropItem[] {new AttributeTreeDropItem((CategoryAttribute)element) };
			}else {
				return new DropItem[] {new AttributeDropItem((CategoryAttribute)element) };	
			}
		}
		if (element instanceof Operator) {
			Operator op = (Operator)element;
			if (op == Operator.BRACKETS) {
				return new DropItem[] {new BracketDropItem(BracketType.OPEN), new BracketDropItem(BracketType.CLOSE)}; 
			}
			if (op == Operator.NOT) {
				return new DropItem[] { new NotDropItem() };
			}
			if (op == Operator.AND) {
				BooleanOpDropItem i = new BooleanOpDropItem();
				i.initializeData(Operator.AND.asSmartValue());
				return new DropItem[] { i };
			}
			if (op == Operator.OR) {
				BooleanOpDropItem i = new BooleanOpDropItem();
				i.initializeData(Operator.OR.asSmartValue());
				return new DropItem[] { i };
			}
		}
		
		
		return null;
	}

	public DropItem createAndOrOpterator() {
		return new BooleanOpDropItem();
	}
}
