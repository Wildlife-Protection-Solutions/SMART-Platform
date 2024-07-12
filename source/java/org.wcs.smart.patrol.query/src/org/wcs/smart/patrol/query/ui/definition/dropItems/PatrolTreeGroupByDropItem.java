/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.patrol.query.ui.definition.dropItems;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.ITreeNode;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.IPatrolQueryOption;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.ui.model.ICombinableDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;

/**
 * A patrol group by drop item for tree custom attributes
 * 
 * @author egouge
 * @since 8.1.0
 */
public class PatrolTreeGroupByDropItem extends PatrolGroupByDropItem implements ICombinableDropItem{
	
	private int level = -1;
	/**
	 * Creates a new drop item
	 * @param type
	 */
	public PatrolTreeGroupByDropItem(IPatrolQueryOption type, int level){
		super(type);
		this.level = level;
	}
	
	@Override
	public void initializeData(Object data) {
		super.initializeData(data);
		Object[] values = (Object[])data;
		
		if (values.length > 1 && values[1] != null) {
			ListItem[] d = (ListItem[])values[1];
			this.level = Category.hkeyLength(d[0].getKey());
		}
		if (values.length > 2 && values[2] != null) {
			this.level = (Integer)values[2];
		}
	}
	
	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(groupBy.getKey());
		sb.append(":"); //$NON-NLS-1$
		sb.append(level);
		sb.append(":"); //$NON-NLS-1$
		
		for (int i = 0; i < filteredValues.size(); i++) {
			if (i != 0) sb.append(":"); //$NON-NLS-1$ 
			sb.append(filteredValues.get(i).getKey());
		}
		
		return sb.toString();
	}
	
	@Override
	public String getText() {
		String key = groupBy.getGuiName(Locale.getDefault());
		String r = MessageFormat.format(Messages.PatrolTreeGroupByDropItem_TreeLevel, key, level);
		
		StringBuilder sb = new StringBuilder();
		sb.append(r);
		for (int i = 0; i < Math.min(3, filteredValues.size()); i ++) {
			sb.append("\n"); //$NON-NLS-1$
			sb.append("   " + filteredValues.get(i).getName()); //$NON-NLS-1$
		}
		if (filteredValues.size() > 3) {
			sb.append("\n"); //$NON-NLS-1$
			sb.append("..."); //$NON-NLS-1$
		}
		return sb.toString();
	}
	
	/**
	 * Adds another drop item to this
	 * attribute tree drop item. Used to combine drop
	 * items with the same attribute-tree level.
	 * 
	 * @param dropItem
	 * @return
	 */
	@Override
	public boolean addItem(DropItem dItem){
		if (!(dItem instanceof PatrolTreeGroupByDropItem)) return false;
			
		PatrolTreeGroupByDropItem dropItem = (PatrolTreeGroupByDropItem)dItem;
		
		if (this.level != dropItem.level)  return false;
			
		for (ListItem item : dropItem.filteredValues) {
			if (!this.filteredValues.contains(item)) {
				this.filteredValues.add(item);
			}
		}
		
		this.updateToolTipMessage();
		this.updateLabel();
		this.queryChanged();
		
		return true;
	}
	
	/**
	 * Opens and closes a hibernate session so this should
	 * be executed in a separate thread.
	 * 
	 * @see org.wcs.smart.query.ui.formulaDnd.IGroupByDropItem#getListItem()
	 */
	@Override
	public List<ListItem> getListItem() {
		List<ListItem> items = new ArrayList<ListItem>();
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try{
				List<? extends ITreeNode<?>> nodes = data.getValuesTree(s);
				
				//find all tree nodes with a level that matches level
				List<ITreeNode<?>> toProcess = new ArrayList<>();
				toProcess.addAll(nodes);
				while(!toProcess.isEmpty()) {
					ITreeNode<?> x = toProcess.remove(0);
					if (Category.hkeyLength(x.getHkey()) == level) {
						items.add(new ListItem(null, x.getName(), x.getHkey()));
					}
					toProcess.addAll(x.getChildren());
				}
				
			}catch (Exception ex){
				QueryPlugIn.displayLog(Messages.PatrolGroupByDropItem_Error_LoadingListItems, ex);
			}finally {
				s.getTransaction().rollback();
			}
		}
		Collections.sort(items);
		return items;
	}

}
