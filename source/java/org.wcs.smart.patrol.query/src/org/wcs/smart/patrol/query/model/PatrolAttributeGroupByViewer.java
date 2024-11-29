/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.patrol.query.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.ITreeNode;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.query.hibernate.PatrolQueryHibernateManager;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolAttributeGroupBy;
import org.wcs.smart.patrol.query.ui.PatrolOptionData;
import org.wcs.smart.query.model.summary.AbstractGroupByViewer;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ErrorDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;

/**
 * Group by viewer for custom patrol attributes. Supports tree and list attributes.
 * 
 * @author Emily
 *
 */
public class PatrolAttributeGroupByViewer extends AbstractGroupByViewer<PatrolAttributeGroupBy> {
	
	
	public PatrolAttributeGroupByViewer(PatrolAttributeGroupBy gb) {
		super(gb);
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.wp.sum.IGroupBy#getItems(org.hibernate.Session)
	 */
	@Override
	public List<ListItem> getItems(Session session) {
		try {
			PatrolOptionData data = new PatrolOptionData(  getQueryOption(session) );
			if (groupBy.getAttributeType() == Attribute.AttributeType.LIST) {
				if (groupBy.getItems() != null){
					return data.getListValues(session, groupBy.getItems());
				}else {
					return data.getListValues(session);
				}
			}
			if (groupBy.getAttributeType() == Attribute.AttributeType.TREE) {
				
				List<? extends ITreeNode<?>> nodes = data.getValuesTree(session, groupBy.getLevel());
				if (groupBy.getItems() == null || groupBy.getItems().length == 0){
					return nodes.stream().map(e->new ListItem(null, e.getName(), e.getHkey())).collect(Collectors.toList());
				}
				Set<String> keys = new HashSet<>();
				for (String x : groupBy.getItems()) keys.add(x);
				
				return nodes.stream().filter(e->keys.contains(e.getHkey())).
						map(e->new ListItem(null, e.getName(), e.getHkey())).collect(Collectors.toList());				
			}
		}catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		return null;		
	}

	private PatrolAttributeQueryOption getQueryOption(Session session) throws Exception{
		PatrolAttributeGroupBy gb = (PatrolAttributeGroupBy)groupBy;

		PatrolAttribute pa = PatrolQueryHibernateManager.getInstance().getPatrolAttribute(session,  gb.getAttributeKey());
		if (pa == null || (
				pa.getType() != Attribute.AttributeType.LIST &&
				pa.getType() != Attribute.AttributeType.TREE)) 
			throw new Exception(MessageFormat.format(Messages.PatrolAttributeGroupByViewer_PatrolAttributeNotFound2, gb.getAttributeKey()));
		
		return new PatrolAttributeQueryOption(pa);
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asDropItem(org.hibernate.Session)
	 */
	@Override
	public DropItem asDropItem(Session session) throws Exception{
		String[] items = groupBy.getItems();
		try {
		
			PatrolAttributeQueryOption option = getQueryOption(session);
			PatrolOptionData data = new PatrolOptionData(option);
			
			DropItem it = PatrolDropItemFactory.INSTANCE.createPatrolGroupByDropItem(option);

			if (items != null){
				Set<String> keys = new HashSet<>();
				for(String i : items) keys.add(i);
				
				if (option.getPatrolAttribute().getType() == Attribute.AttributeType.LIST) {
					List<ListItem> allItems = new ArrayList<>(data.getListValues(session));
					List<ListItem> inits = allItems.stream().filter(e->keys.contains(e.getKey())).collect(Collectors.toList());
					it.initializeData(new Object[]{data, inits.toArray(new ListItem[inits.size()])});
				}else if (option.getPatrolAttribute().getType() == Attribute.AttributeType.TREE) {
					int level = groupBy.getLevel();
					List<? extends ITreeNode<?>> matched = data.getValuesTree(session, level);
					List<ListItem> inits = matched.stream()
							.filter(e->keys.contains(e.getHkey()))
							.map(e->new ListItem(null, e.getName(),e.getHkey()))
							.collect(Collectors.toList());
					it.initializeData(new Object[]{data, inits.toArray(new ListItem[inits.size()]), level});
				}
			}else {
				if (option.getPatrolAttribute().getType() == Attribute.AttributeType.TREE) {
					it.initializeData(new Object[]{data, null, groupBy.getLevel()});
				}
			}
			return it;
		} catch (Exception ex) {
			return new ErrorDropItem(Messages.PatrolGroupBy_CouldNotParse + ex.getLocalizedMessage());
		}
		
	}

}
