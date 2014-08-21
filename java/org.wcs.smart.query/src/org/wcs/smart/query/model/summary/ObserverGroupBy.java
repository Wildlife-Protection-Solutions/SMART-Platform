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
package org.wcs.smart.query.model.summary;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.util.SmartUtils;

/**
 * Observer group by query item.
 * 
 * @author Emily
 *
 */
public class ObserverGroupBy implements IGroupBy {

	/**
	 * Creates new group by item
	 * 
	 * @param key
	 * @return
	 */
	public final static ObserverGroupBy createGroupBy(String key){
		return new ObserverGroupBy(key);
	}
	
	private String[] filterkeys = null;
	
	protected ObserverGroupBy(String key){
		String bits[] = key.split(":"); //$NON-NLS-1$
		if (bits.length - 2 > 0){
			filterkeys = new String[bits.length-2];
			for (int i = 2; i < bits.length; i ++){
				filterkeys[i-2] = bits[i];
			}
		}
	}
	
	@Override
	public String asString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getKeyPart());
		sb.append(":"); //$NON-NLS-1$
		if (filterkeys != null){
			for (int i =0; i < filterkeys.length; i ++){
				sb.append(filterkeys[i]);
				if (i < filterkeys.length-1){
					sb.append(":"); //$NON-NLS-1$
				}
			}
		}
		return sb.toString();
	}

	@Override
	public String getKeyPart() {
		return "wpnobs:observer"; //$NON-NLS-1$
	}

	@Override
	public GroupByType getType() {
		return GroupByType.BYTE;
	}

	@Override
	public List<ListItem> getItems(Session session) {
		List<ListItem> items = new ArrayList<ListItem>();
		if (filterkeys != null && filterkeys.length > 0){
			try{
				for (String uuid : filterkeys){
					Employee e = (Employee) session.load(Employee.class, SmartUtils.decodeHex(uuid));
					items.add(new ListItem(e.getUuid(), e.getFullLabel()));
				}
			}catch (Exception ex){
				QueryPlugIn.displayLog(Messages.ObserverGroupBy_ErrorLoadingEmployees, ex);
			}
		}else{
			List<Employee> es = HibernateManager.getActiveEmployees(SmartDB.getCurrentConservationArea(), session);
			Collections.sort(es, new Comparator<Employee>() {
				@Override
				public int compare(Employee arg0, Employee arg1) {
					return Collator.getInstance().compare(arg0.getFullLabel().toUpperCase(), arg1.getFullLabel().toUpperCase());
				}
			});
			for (Employee e : es){
				items.add(new ListItem(e.getUuid(), e.getFullLabel()));
			}
		}
		return items;
	}

	@Override
	public DropItem asDropItem(Session session) throws Exception {
		DropItem di = BasicDropItemFactory.INSTANCE.createObserverGroupByDropItem();
		di.initializeData(filterkeys);
		return di;
	}

	@Override
	public void visit(IGroupByVisitor visitor) {
		visitor.visit(this);
	}

}
