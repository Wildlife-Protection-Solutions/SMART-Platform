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
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Observer group by query item.
 * 
 * @author Emily
 *
 */
public class ObserverGroupByViewer extends AbstractGroupByViewer<ObserverGroupBy> {

	public ObserverGroupByViewer(ObserverGroupBy gb) {
		super(gb);
	}

	@Override
	public List<ListItem> getItems(Session session) {
		String[] filterkeys = groupBy.getFilterKeys();
		List<ListItem> items = new ArrayList<ListItem>();
		if (filterkeys != null && filterkeys.length > 0){
			try{
				for (String uuid : filterkeys){
					Employee e = (Employee) session.load(Employee.class, UuidUtils.stringToUuid(uuid));
					items.add(new ListItem(e.getUuid(), SmartLabelProvider.getFullLabel(e)));
				}
			}catch (Exception ex){
				QueryPlugIn.displayLog(Messages.ObserverGroupBy_ErrorLoadingEmployees, ex);
			}
		}else{
			List<Employee> es = HibernateManager.getActiveEmployees(SmartDB.getCurrentConservationArea(), session);
			Collections.sort(es, new Comparator<Employee>() {
				@Override
				public int compare(Employee arg0, Employee arg1) {
					return Collator.getInstance().compare(SmartLabelProvider.getShortLabel(arg0).toUpperCase(), SmartLabelProvider.getFullLabel(arg1).toUpperCase());
				}
			});
			for (Employee e : es){
				items.add(new ListItem(e.getUuid(), SmartLabelProvider.getShortLabel(e)));
			}
		}
		return items;
	}

	@Override
	public DropItem asDropItem(Session session) throws Exception {
		DropItem di = BasicDropItemFactory.INSTANCE.createObserverGroupByDropItem();
		di.initializeData(groupBy.getFilterKeys());
		return di;
	}

}
